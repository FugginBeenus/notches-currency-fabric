package net.fugginbeenus.notchcurrency.item;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.npc.NotchNpcManager;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
//? if <1.21.11 {
import net.minecraft.world.InteractionResultHolder;
//?}
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class RoutePlannerItem extends Item {

    public static final String NPC_KEY = "RouteNpc";
    public static final String NPC_NAME_KEY = "RouteNpcName";
    public static final String COUNT_KEY = "RouteCount";
    public static final String ENTRY_KEY = "ScheduleEntry";

    public RoutePlannerItem(Properties settings) {
        super(settings.stacksTo(1));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(context.getPlayer() instanceof ServerPlayer sp)) {
            return InteractionResult.PASS;
        }

        NotchNpcEntity npc = boundNpc(context.getItemInHand(), (ServerLevel) world, sp);
        if (npc == null) return InteractionResult.CONSUME;

        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        if (sp.isShiftKeyDown()) {
            NotchNpcManager.removeLastWaypoint(sp, npc);
        } else {
            NotchNpcManager.addWaypointAt(sp, npc, pos);
        }
        return InteractionResult.CONSUME;
    }

    //? if >=1.21.11 {
    /*@Override
    public InteractionResult use(Level world, Player user, InteractionHand hand) {
    *///?} else {
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player user, InteractionHand hand) {
    //?}
        ItemStack stack = user.getItemInHand(hand);
        if (!world.isClientSide() && user instanceof ServerPlayer sp) {
            if (StackData.has(stack, ENTRY_KEY)) {
                consume(sp, stack);
                net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Spot unchanged.").withStyle(ChatFormatting.GRAY));
                NotchNpcManager.reopenScheduleFor(sp, stack);
            } else {
                NotchNpcEntity npc = boundNpc(stack, (ServerLevel) world, sp);
                if (npc != null) {
                    NotchNpcManager.confirmRoute(sp, npc);
                }
            }
        }
        //? if >=1.21.11 {
        /*return InteractionResult.SUCCESS;
        *///?} else {
        return InteractionResultHolder.success(stack);
        //?}
    }

    //? if >=1.21.11 {
    /*@Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity,
                              net.minecraft.world.entity.EquipmentSlot slot) {
        if (!(entity instanceof ServerPlayer sp)) return;
        boolean held = slot == net.minecraft.world.entity.EquipmentSlot.MAINHAND
                || slot == net.minecraft.world.entity.EquipmentSlot.OFFHAND;
    *///?} else {
    @Override
    public void inventoryTick(ItemStack stack, Level world, Entity entity, int slot, boolean selected) {
        if (world.isClientSide() || !(entity instanceof ServerPlayer sp)) return;
        boolean held = selected || sp.getOffhandItem() == stack;
    //?}
        if (!held || world.getGameTime() % 10 != 0) return;

        if (StackData.has(stack, ENTRY_KEY)) return;
        UUID npcId = StackData.getUuid(stack, NPC_KEY);
        if (npcId == null) return;
        if (!(((ServerLevel) world).getEntity(npcId) instanceof NotchNpcEntity npc)) return;

        List<BlockPos> route = npc.getWaypoints();
        if (StackData.getInt(stack, COUNT_KEY) != route.size()) {
            StackData.putInt(stack, COUNT_KEY, route.size());
        }
        ServerLevel sw = (ServerLevel) world;
        for (BlockPos wp : route) {
            for (int i = 0; i < 3; i++) {
                //? if >=1.21.11 {
                /*sw.sendParticles(sp, ParticleTypes.END_ROD, true, false,
                        wp.getX() + 0.5, wp.getY() + 0.3 + i * 0.55, wp.getZ() + 0.5, 1, 0, 0, 0, 0);
                *///?} else {
                sw.sendParticles(sp, ParticleTypes.END_ROD, true,
                        wp.getX() + 0.5, wp.getY() + 0.3 + i * 0.55, wp.getZ() + 0.5, 1, 0, 0, 0, 0);
                //?}
            }
        }
    }

    public static void markScheduleSpot(ServerPlayer sp, ItemStack stack, BlockPos clicked,
                                        net.minecraft.core.Direction side) {
        ServerLevel world = sp.serverLevel();
        NotchNpcEntity npc = boundNpc(stack, world, sp);
        if (npc == null) {
            consume(sp, stack);
            return;
        }
        net.minecraft.world.level.block.state.BlockState state = world.getBlockState(clicked);
        BlockPos target;
        if (state.getBlock() instanceof net.minecraft.world.level.block.BedBlock) {
            target = state.getValue(net.minecraft.world.level.block.BedBlock.PART) == net.minecraft.world.level.block.state.properties.BedPart.FOOT
                    ? clicked.relative(state.getValue(net.minecraft.world.level.block.BedBlock.FACING))
                    : clicked;
        } else {
            target = clicked.relative(side);
        }
        NotchNpcManager.setScheduleAnchor(sp, npc, StackData.getInt(stack, ENTRY_KEY), target,
                sp.getYRot() + 180f);
        consume(sp, stack);
    }

    public static void consume(ServerPlayer sp, ItemStack stack) {
        stack.shrink(1);
        sp.getInventory().setChanged();
        sp.containerMenu.broadcastChanges();
    }

    @Nullable
    private static NotchNpcEntity boundNpc(ItemStack stack, ServerLevel world, ServerPlayer sp) {
        UUID npcId = StackData.getUuid(stack, NPC_KEY);
        if (npcId == null) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("This route tool isn't bound to an NPC - get one from the NPC editor.")
                    .withStyle(ChatFormatting.RED));
            return null;
        }
        if (world.getEntity(npcId) instanceof NotchNpcEntity npc) {
            return npc;
        }
        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Can't reach that NPC (unloaded or removed).").withStyle(ChatFormatting.RED));
        return null;
    }

    @Override
    //? if >=1.21.11 {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context,
                                net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> lines,
                                net.minecraft.world.item.TooltipFlag type) {
        List<Component> tooltip = new java.util.ArrayList<>();
    *///?} elif >=1.21 {
    /*public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, net.minecraft.world.item.TooltipFlag type) {
    *///?} else {
    public void appendHoverText(ItemStack stack, @Nullable Level world, List<Component> tooltip, TooltipFlag context) {
    //?}
        String bound = StackData.has(stack, NPC_NAME_KEY) ? StackData.getString(stack, NPC_NAME_KEY) : null;
        tooltip.add(Component.literal(bound == null ? "Not bound to an NPC." : "Route for: " + bound)
                .withStyle(ChatFormatting.AQUA));
        tooltip.add(Component.literal("Right-click ground: add waypoint").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Sneak + right-click: undo last").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Right-click the air: confirm route").withStyle(ChatFormatting.GRAY));
        //? if >=1.21.11 {
        /*tooltip.forEach(lines);
        super.appendHoverText(stack, context, display, lines, type);
        *///?} elif >=1.21 {
        /*super.appendHoverText(stack, context, tooltip, type);
        *///?} else {
        super.appendHoverText(stack, world, tooltip, context);
        //?}
    }
}
