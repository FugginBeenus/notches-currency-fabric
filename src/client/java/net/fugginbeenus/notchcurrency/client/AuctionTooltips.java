package net.fugginbeenus.notchcurrency.client;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
//? if <1.21 {
import net.minecraft.client.item.TooltipContext;
//?}
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

public final class AuctionTooltips {

    private AuctionTooltips() {}

    public static void init() {
        ItemTooltipCallback.EVENT.register(AuctionTooltips::onTooltip);
    }

    //? if >=1.21 {
    /*private static void onTooltip(ItemStack stack,
                                  net.minecraft.item.Item.TooltipContext context,
                                  net.minecraft.item.tooltip.TooltipType type,
                                  List<Text> lines) {
    *///?} else {
    private static void onTooltip(ItemStack stack,
                                  TooltipContext context,
                                  List<Text> lines) {
    //?}

        NbtCompound tag = StackData.getData(stack);
        if (!tag.contains("nc_price")) {
            return; // not an auction-tagged item
        }

        long price = tag.getLong("nc_price");
        String seller = tag.contains("nc_seller") ? tag.getString("nc_seller") : "Unknown";

        // Keep the first line as name if present; otherwise use stack name
        Text name = lines.isEmpty() ? stack.getName() : lines.get(0);
        lines.clear();

        // Name line
        lines.add(name.copy().formatted(Formatting.WHITE));

        // Price line: "Price: 45 ⛁"
        Text priceLine = Text.empty()
                .append(Text.literal("Price: ").formatted(Formatting.GOLD))
                .append(Text.literal(String.valueOf(price) + " ").formatted(Formatting.YELLOW))
                .append(NotchCurrency.coinIcon());

        lines.add(priceLine);

        // Seller line
        lines.add(
                Text.literal("Seller: " + seller)
                        .formatted(Formatting.GRAY)
        );

        // Hint line
        lines.add(
                Text.literal("Click to buy")
                        .formatted(Formatting.YELLOW)
        );
    }
}
