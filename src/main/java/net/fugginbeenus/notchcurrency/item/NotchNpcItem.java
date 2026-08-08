package net.fugginbeenus.notchcurrency.item;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.economy.npc.NpcRole;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.npc.NotchNpcManager;
import net.fugginbeenus.notchcurrency.registry.ModEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class NotchNpcItem extends Item {

    public NotchNpcItem(Properties settings) {
        super(settings.stacksTo(16));
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level world = context.getLevel();
        if (world.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        ServerLevel sw = (ServerLevel) world;
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Player player = context.getPlayer();

        NotchNpcEntity npc = new NotchNpcEntity(ModEntities.NOTCH_NPC, sw);
        float yaw = player != null ? player.getYRot() + 180f : 0f;
        npc.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0f);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);

        ItemStack stack = context.getItemInHand();
        if (StackData.has(stack, NotchNpcManager.ITEM_TAG)) {
            // Restore a packed NPC (from "Pick up").
            npc.readFromItem(StackData.getCompound(stack, NotchNpcManager.ITEM_TAG));
        } else if (player != null) {
            // Fresh blank NPC: the placer owns it.
            npc.setOwner(player.getUUID(), player.getName().getString());
            npc.setCustomName(Component.literal("NPC"));
            npc.setCustomNameVisible(true);
        }

        // Home (the wander leash point) is wherever the NPC is placed.
        npc.setHome(pos);

        sw.addFreshEntity(npc);

        // Re-establish the shop link if this NPC carries the SHOP role.
        if (npc.getRole() == NpcRole.SHOP && player instanceof ServerPlayer sp) {
            NotchNpcManager.ensureShopForNpc(sw, npc, sp);
        }

        if (player != null && !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (player instanceof ServerPlayer sp) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("NPC placed. ").withStyle(ChatFormatting.GREEN)
                    .append(Component.literal("Sneak + right-click").withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" it to configure.").withStyle(ChatFormatting.GREEN)));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    // 1.21.11 feeds the lines to a consumer rather than filling a list. The body below still builds
    // a list, which is handed over in one go, ahead of whatever the superclass adds.
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
        if (StackData.has(stack, NotchNpcManager.ITEM_TAG)) {
            CompoundTag tag = StackData.getCompound(stack, NotchNpcManager.ITEM_TAG);
            String name = tag.contains("Name") ? tag.getString("Name") : "NPC";
            String role = tag.contains("Role") ? tag.getString("Role") : "NONE";
            tooltip.add(Component.literal("Packed NPC: " + name).withStyle(ChatFormatting.AQUA));
            tooltip.add(Component.literal("Role: " + role).withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Place to set it down.").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltip.add(Component.literal("Places a blank NPC you own.").withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.literal("Sneak + right-click it to configure").withStyle(ChatFormatting.YELLOW));
            tooltip.add(Component.literal("appearance, role, name and more.").withStyle(ChatFormatting.YELLOW));
        }
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
