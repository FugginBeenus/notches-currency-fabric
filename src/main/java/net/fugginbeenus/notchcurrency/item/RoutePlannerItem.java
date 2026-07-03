package net.fugginbeenus.notchcurrency.item;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.npc.NotchNpcManager;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * The patrol route tool: handed out by the NPC editor's Behavior tab, bound to one NPC. Walk the
 * route and right-click the ground to drop waypoints where you stand-in; sneak + right-click undoes
 * the last one. The editor's Clear button wipes the whole route.
 */
public class RoutePlannerItem extends Item {

    public static final String NPC_KEY = "RouteNpc";
    public static final String NPC_NAME_KEY = "RouteNpcName";

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

        NbtCompound nbt = context.getStack().getNbt();
        if (nbt == null || !nbt.containsUuid(NPC_KEY)) {
            sp.sendMessage(Text.literal("This route tool isn't bound to an NPC — get one from the NPC editor.")
                    .formatted(Formatting.RED), false);
            return ActionResult.CONSUME;
        }
        UUID npcId = nbt.getUuid(NPC_KEY);
        if (!(((ServerWorld) world).getEntity(npcId) instanceof NotchNpcEntity npc)) {
            sp.sendMessage(Text.literal("Can't reach that NPC (unloaded or removed).").formatted(Formatting.RED), false);
            return ActionResult.CONSUME;
        }

        BlockPos pos = context.getBlockPos().offset(context.getSide());
        if (sp.isSneaking()) {
            NotchNpcManager.removeLastWaypoint(sp, npc);
        } else {
            NotchNpcManager.addWaypointAt(sp, npc, pos);
        }
        return ActionResult.CONSUME;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        NbtCompound nbt = stack.getNbt();
        String bound = (nbt != null && nbt.contains(NPC_NAME_KEY)) ? nbt.getString(NPC_NAME_KEY) : null;
        tooltip.add(Text.literal(bound == null ? "Not bound to an NPC." : "Route for: " + bound)
                .formatted(Formatting.AQUA));
        tooltip.add(Text.literal("Right-click ground: add waypoint").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Sneak + right-click: undo last").formatted(Formatting.GRAY));
        super.appendTooltip(stack, world, tooltip, context);
    }
}
