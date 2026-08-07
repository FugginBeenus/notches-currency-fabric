package net.fugginbeenus.notchcurrency.economy.adminshop;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;

public final class AdminShopMenu {

    private AdminShopMenu() {}

    public static void sendListing(ServerPlayer player, AdminShop shop) {
        player.displayClientMessage(Component.literal("═══ " + shop.getName() + " ═══").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);

        if (shop.getEntries().isEmpty()) {
            player.displayClientMessage(Component.literal("This shop has no items yet.").withStyle(ChatFormatting.GRAY), false);
            return;
        }

        for (AdminShopEntry e : shop.getEntries()) {
            int unit = e.getUnit();
            MutableComponent line = Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(e.getItem().getHoverName().copy().withStyle(ChatFormatting.WHITE));
            if (unit > 1) line.append(Component.literal(" x" + unit).withStyle(ChatFormatting.GRAY));
            line.append(Component.literal("  "));

            if (e.isBuyable()) {
                line.append(button("Buy " + e.currentBuyPrice(), ChatFormatting.GREEN,
                        "/adminshop buy " + shop.getId() + " " + e.getId() + " 1",
                        "Buy " + unit + "x for " + e.currentBuyPrice() + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word()));
                line.append(Component.literal(" "));
            }
            if (e.isSellable()) {
                line.append(button("Sell " + e.currentSellPrice(), ChatFormatting.YELLOW,
                        "/adminshop sell " + shop.getId() + " " + e.getId() + " 1",
                        "Sell " + unit + "x for " + e.currentSellPrice() + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word()));
            }
            if (e.isDynamic()) {
                line.append(Component.literal(" ~").withStyle(ChatFormatting.AQUA)
                        .withStyle(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Component.literal("Dynamic price - moves with supply & demand")))));
            }
            player.displayClientMessage(line, false);
        }
        player.displayClientMessage(Component.literal("(Prices in ").withStyle(ChatFormatting.DARK_GRAY)
                .append(NotchCurrency.coinIcon())
                .append(Component.literal(". Click a button to trade 1 at a time.)").withStyle(ChatFormatting.DARK_GRAY)), false);
    }

    private static MutableComponent button(String label, ChatFormatting color, String command, String hover) {
        return Component.literal("[" + label + "]").withStyle(color)
                .withStyle(s -> s
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Component.literal(hover))));
    }
}
