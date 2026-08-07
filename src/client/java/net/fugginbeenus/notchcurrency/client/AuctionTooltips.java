package net.fugginbeenus.notchcurrency.client;

import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.List;

public final class AuctionTooltips {

    private AuctionTooltips() {}

    public static void init() {
        ItemTooltipCallback.EVENT.register(AuctionTooltips::onTooltip);
    }

    //? if >=1.21 {
    /*private static void onTooltip(ItemStack stack,
                                  net.minecraft.world.item.Item.TooltipContext context,
                                  net.minecraft.world.item.TooltipFlag type,
                                  List<Component> lines) {
    *///?} else {
    private static void onTooltip(ItemStack stack,
                                  TooltipFlag context,
                                  List<Component> lines) {
    //?}

        CompoundTag tag = StackData.getData(stack);
        if (!tag.contains("nc_price")) {
            return; // not an auction-tagged item
        }

        long price = tag.getLong("nc_price");
        String seller = tag.contains("nc_seller") ? tag.getString("nc_seller") : "Unknown";

        // Keep the first line as name if present; otherwise use stack name
        Component name = lines.isEmpty() ? stack.getHoverName() : lines.get(0);
        lines.clear();

        // Name line
        lines.add(name.copy().withStyle(ChatFormatting.WHITE));

        // Price line: "Price: 45 ⛁"
        Component priceLine = Component.empty()
                .append(Component.literal("Price: ").withStyle(ChatFormatting.GOLD))
                .append(Component.literal(String.valueOf(price) + " ").withStyle(ChatFormatting.YELLOW))
                .append(NotchCurrency.coinIcon());

        lines.add(priceLine);

        // Seller line
        lines.add(
                Component.literal("Seller: " + seller)
                        .withStyle(ChatFormatting.GRAY)
        );

        // Hint line
        lines.add(
                Component.literal("Click to buy")
                        .withStyle(ChatFormatting.YELLOW)
        );
    }
}
