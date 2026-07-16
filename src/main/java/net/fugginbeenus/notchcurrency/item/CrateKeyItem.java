package net.fugginbeenus.notchcurrency.item;

//? if <1.21 {
import net.minecraft.client.item.TooltipContext;
//?}
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A universal Crate Key — bought with coins (a sink) or earned. Right-click a crate block while
 * holding keys to open it; higher-tier crates cost more keys.
 */
public class CrateKeyItem extends Item {

    public CrateKeyItem(Settings settings) {
        super(settings);
    }

    @Override
    //? if >=1.21 {
    /*public void appendTooltip(ItemStack stack, net.minecraft.item.Item.TooltipContext context, List<Text> tooltip, net.minecraft.item.tooltip.TooltipType type) {
    *///?} else {
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
    //?}
        tooltip.add(Text.literal("Right-click a crate to open it.").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("Sneak-right-click a crate to see its odds.").formatted(Formatting.DARK_GRAY));
        //? if >=1.21 {
        /*super.appendTooltip(stack, context, tooltip, type);
        *///?} else {
        super.appendTooltip(stack, world, tooltip, context);
        //?}
    }
}
