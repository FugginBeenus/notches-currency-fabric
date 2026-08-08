package net.fugginbeenus.notchcurrency.item;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class CrateKeyItem extends Item {

    public CrateKeyItem(Properties settings) {
        super(settings);
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
        tooltip.add(Component.literal("Right-click a crate to open it.").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("Sneak-right-click a crate to see its odds.").withStyle(ChatFormatting.DARK_GRAY));
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
