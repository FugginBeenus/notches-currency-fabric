package net.fugginbeenus.notchcurrency.item;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.economy.npc.NpcRole;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.npc.NotchNpcManager;
import net.fugginbeenus.notchcurrency.registry.ModEntities;
//? if <1.21 {
import net.minecraft.client.item.TooltipContext;
//?}
import net.minecraft.entity.player.PlayerEntity;
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

/**
 * The single Notch NPC item. Placing it spawns an NPC: a fresh, owned, blank-slate NPC if the item is
 * plain, or a fully-restored NPC if the item was created by the editor's "Pick up" (carries the config
 * under the {@link NotchNpcManager#ITEM_TAG} tag). Everything else is configured in the editor,
 * deliberately no per-mob spawn eggs or separate mover/editor items.
 */
public class NotchNpcItem extends Item {

    public NotchNpcItem(Settings settings) {
        super(settings.maxCount(16));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }
        ServerWorld sw = (ServerWorld) world;
        BlockPos pos = context.getBlockPos().offset(context.getSide());
        PlayerEntity player = context.getPlayer();

        NotchNpcEntity npc = new NotchNpcEntity(ModEntities.NOTCH_NPC, sw);
        float yaw = player != null ? player.getYaw() + 180f : 0f;
        npc.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0f);
        npc.setHeadYaw(yaw);
        npc.setBodyYaw(yaw);

        ItemStack stack = context.getStack();
        if (StackData.has(stack, NotchNpcManager.ITEM_TAG)) {
            // Restore a packed NPC (from "Pick up").
            npc.readFromItem(StackData.getCompound(stack, NotchNpcManager.ITEM_TAG));
        } else if (player != null) {
            // Fresh blank NPC: the placer owns it.
            npc.setOwner(player.getUuid(), player.getName().getString());
            npc.setCustomName(Text.literal("NPC"));
            npc.setCustomNameVisible(true);
        }

        // Home (the wander leash point) is wherever the NPC is placed.
        npc.setHome(pos);

        sw.spawnEntity(npc);

        // Re-establish the shop link if this NPC carries the SHOP role.
        if (npc.getRole() == NpcRole.SHOP && player instanceof ServerPlayerEntity sp) {
            NotchNpcManager.ensureShopForNpc(sw, npc, sp);
        }

        if (player != null && !player.getAbilities().creativeMode) {
            stack.decrement(1);
        }
        if (player instanceof ServerPlayerEntity sp) {
            sp.sendMessage(Text.literal("NPC placed. ").formatted(Formatting.GREEN)
                    .append(Text.literal("Sneak + right-click").formatted(Formatting.YELLOW))
                    .append(Text.literal(" it to configure.").formatted(Formatting.GREEN)), false);
        }
        return ActionResult.CONSUME;
    }

    @Override
    //? if >=1.21 {
    /*public void appendTooltip(ItemStack stack, net.minecraft.item.Item.TooltipContext context, List<Text> tooltip, net.minecraft.item.tooltip.TooltipType type) {
    *///?} else {
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
    //?}
        if (StackData.has(stack, NotchNpcManager.ITEM_TAG)) {
            NbtCompound tag = StackData.getCompound(stack, NotchNpcManager.ITEM_TAG);
            String name = tag.contains("Name") ? tag.getString("Name") : "NPC";
            String role = tag.contains("Role") ? tag.getString("Role") : "NONE";
            tooltip.add(Text.literal("Packed NPC: " + name).formatted(Formatting.AQUA));
            tooltip.add(Text.literal("Role: " + role).formatted(Formatting.GRAY));
            tooltip.add(Text.literal("Place to set it down.").formatted(Formatting.DARK_GRAY));
        } else {
            tooltip.add(Text.literal("Places a blank NPC you own.").formatted(Formatting.GRAY));
            tooltip.add(Text.literal("Sneak + right-click it to configure").formatted(Formatting.YELLOW));
            tooltip.add(Text.literal("appearance, role, name and more.").formatted(Formatting.YELLOW));
        }
        //? if >=1.21 {
        /*super.appendTooltip(stack, context, tooltip, type);
        *///?} else {
        super.appendTooltip(stack, world, tooltip, context);
        //?}
    }
}
