package net.fugginbeenus.notchcurrency.shop;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Merchant License - Use on a ShopkeeperEntity to turn them into your shopkeeper.
 * Consumed on use.
 */
public class MerchantLicenseItem extends Item {

    public MerchantLicenseItem(Settings settings) {
        super(settings.maxCount(1));
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("Right-click an NPC to make them").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("your personal shopkeeper!").formatted(Formatting.GRAY));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("The NPC will sell items for you").formatted(Formatting.YELLOW));
        tooltip.add(Text.literal("while you're away.").formatted(Formatting.YELLOW));
        super.appendTooltip(stack, world, tooltip, context);
    }
}