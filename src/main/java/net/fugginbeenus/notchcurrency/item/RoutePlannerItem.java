package net.fugginbeenus.notchcurrency.item;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.npc.NotchNpcManager;
//? if <1.21 {
import net.minecraft.client.item.TooltipContext;
//?}
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * The patrol route tool: handed out by the NPC editor's Behavior tab, bound to one NPC. Walk the
 * route and right-click the ground to drop waypoints; sneak + right-click undoes the last one;
 * right-click the AIR to confirm: the tool vanishes and the NPC starts patrolling. While held, the
 * route HUD overlay shows the live count and each waypoint is marked with a particle beacon.
 */
public class RoutePlannerItem extends Item {

    public static final String NPC_KEY = "RouteNpc";
    public static final String NPC_NAME_KEY = "RouteNpcName";
    public static final String COUNT_KEY = "RouteCount"; // synced for the HUD overlay
    /** Present when this tool is marking one schedule entry's spot instead of walking a route.
     *  Same tool and the same right-click, because it is the same job: point at a block. */
    public static final String ENTRY_KEY = "ScheduleEntry";

    public RoutePlannerItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayerEntity sp)) {
            return ActionResult.PASS;
        }

        NotchNpcEntity npc = boundNpc(context.getStack(), (ServerWorld) world, sp);
        if (npc == null) return ActionResult.CONSUME;

        BlockPos pos = context.getBlockPos().offset(context.getSide());
        // Spot marking never arrives here: UseBlockCallback takes it first, because some blocks
        // (beds above all) swallow the click before an item is ever asked about it.
        if (sp.isSneaking()) {
            NotchNpcManager.removeLastWaypoint(sp, npc);
        } else {
            NotchNpcManager.addWaypointAt(sp, npc, pos);
        }
        return ActionResult.CONSUME;
    }

    /** Right-click the air to confirm the route. The tool disappears and the patrol starts. */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient()) {
            return TypedActionResult.success(stack);
        }
        if (user instanceof ServerPlayerEntity sp) {
            if (StackData.has(stack, ENTRY_KEY)) {
                // Nothing to confirm when marking a single spot: this is the way to back out.
                consume(sp, stack);
                sp.sendMessage(Text.literal("Spot unchanged.").formatted(Formatting.GRAY), false);
                NotchNpcManager.reopenScheduleFor(sp, stack);
                return TypedActionResult.success(stack);
            }
            NotchNpcEntity npc = boundNpc(stack, (ServerWorld) world, sp);
            if (npc != null) {
                NotchNpcManager.confirmRoute(sp, npc);
            }
        }
        return TypedActionResult.success(stack);
    }

    /** While held: keep the HUD's waypoint count synced and mark each waypoint with a particle
     *  beacon (shown only to the holder). */
    @Override
    public void inventoryTick(ItemStack stack, World world, Entity entity, int slot, boolean selected) {
        if (world.isClient() || !(entity instanceof ServerPlayerEntity sp)) return;
        boolean held = selected || sp.getOffHandStack() == stack;
        if (!held || world.getTime() % 10 != 0) return;

        if (StackData.has(stack, ENTRY_KEY)) return; // nothing to beacon: no route being built
        UUID npcId = StackData.getUuid(stack, NPC_KEY);
        if (npcId == null) return;
        if (!(((ServerWorld) world).getEntity(npcId) instanceof NotchNpcEntity npc)) return;

        List<BlockPos> route = npc.getWaypoints();
        if (StackData.getInt(stack, COUNT_KEY) != route.size()) {
            StackData.putInt(stack, COUNT_KEY, route.size());
        }
        ServerWorld sw = (ServerWorld) world;
        for (BlockPos wp : route) {
            for (int i = 0; i < 3; i++) {
                sw.spawnParticles(sp, ParticleTypes.END_ROD, true,
                        wp.getX() + 0.5, wp.getY() + 0.3 + i * 0.55, wp.getZ() + 0.5, 1, 0, 0, 0, 0);
            }
        }
    }

    /**
     * Record the clicked block as one schedule entry's spot, then take the tool back.
     *
     * <p>A bed is taken as clicked rather than the block beside it: sleeping means being in it. For
     * anything else the block above is the spot, since that is where a body stands.
     */
    public static void markScheduleSpot(ServerPlayerEntity sp, ItemStack stack, BlockPos clicked,
                                        net.minecraft.util.math.Direction side) {
        ServerWorld world = sp.getServerWorld();
        NotchNpcEntity npc = boundNpc(stack, world, sp);
        if (npc == null) {
            consume(sp, stack); // bound to nothing reachable: don't leave a dead tool behind
            return;
        }
        net.minecraft.block.BlockState state = world.getBlockState(clicked);
        BlockPos target;
        if (state.getBlock() instanceof net.minecraft.block.BedBlock) {
            // A bed is two blocks and the sleeper belongs in the head half: that is the end vanilla
            // lies a player at, and the end the renderer measures its offset from. Storing the foot
            // instead leaves the body shifted a block down the bed, hanging off the end of it.
            target = state.get(net.minecraft.block.BedBlock.PART) == net.minecraft.block.enums.BedPart.FOOT
                    ? clicked.offset(state.get(net.minecraft.block.BedBlock.FACING))
                    : clicked;
        } else {
            // Step off the face that was clicked rather than always upward, so clicking the side of a
            // wall puts the spot in front of it instead of inside it.
            target = clicked.offset(side);
        }
        NotchNpcManager.setScheduleAnchor(sp, npc, StackData.getInt(stack, ENTRY_KEY), target, sp.getYaw());
        consume(sp, stack);
    }

    /** Take the tool out of the player's hands. Its whole job was one click. */
    public static void consume(ServerPlayerEntity sp, ItemStack stack) {
        stack.decrement(1);
        sp.getInventory().markDirty();
        sp.currentScreenHandler.sendContentUpdates();
    }

    @Nullable
    private static NotchNpcEntity boundNpc(ItemStack stack, ServerWorld world, ServerPlayerEntity sp) {
        UUID npcId = StackData.getUuid(stack, NPC_KEY);
        if (npcId == null) {
            sp.sendMessage(Text.literal("This route tool isn't bound to an NPC - get one from the NPC editor.")
                    .formatted(Formatting.RED), false);
            return null;
        }
        if (world.getEntity(npcId) instanceof NotchNpcEntity npc) {
            return npc;
        }
        sp.sendMessage(Text.literal("Can't reach that NPC (unloaded or removed).").formatted(Formatting.RED), false);
        return null;
    }

    @Override
    //? if >=1.21 {
    /*public void appendTooltip(ItemStack stack, net.minecraft.item.Item.TooltipContext context, List<Text> tooltip, net.minecraft.item.tooltip.TooltipType type) {
    *///?} else {
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
    //?}
        String bound = StackData.has(stack, NPC_NAME_KEY) ? StackData.getString(stack, NPC_NAME_KEY) : null;
        tooltip.add(Text.literal(bound == null ? "Not bound to an NPC." : "Route for: " + bound)
                .formatted(Formatting.AQUA));
        tooltip.add(Text.literal("Right-click ground: add waypoint").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Sneak + right-click: undo last").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Right-click the air: confirm route").formatted(Formatting.GRAY));
        //? if >=1.21 {
        /*super.appendTooltip(stack, context, tooltip, type);
        *///?} else {
        super.appendTooltip(stack, world, tooltip, context);
        //?}
    }
}
