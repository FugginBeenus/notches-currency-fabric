package net.fugginbeenus.notchcurrency.economy.adminshop;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class AdminShopMenu {

    private AdminShopMenu() {}

    public static void sendListing(ServerPlayerEntity player, AdminShop shop) {
        player.sendMessage(Text.literal("═══ " + shop.getName() + " ═══").formatted(Formatting.GOLD, Formatting.BOLD), false);

        if (shop.getEntries().isEmpty()) {
            player.sendMessage(Text.literal("This shop has no items yet.").formatted(Formatting.GRAY), false);
            return;
        }

        for (AdminShopEntry e : shop.getEntries()) {
            int unit = e.getUnit();
            MutableText line = Text.literal(" • ").formatted(Formatting.DARK_GRAY)
                    .append(e.getItem().getName().copy().formatted(Formatting.WHITE));
            if (unit > 1) line.append(Text.literal(" x" + unit).formatted(Formatting.GRAY));
            line.append(Text.literal("  "));

            if (e.isBuyable()) {
                line.append(button("Buy " + e.currentBuyPrice(), Formatting.GREEN,
                        "/adminshop buy " + shop.getId() + " " + e.getId() + " 1",
                        "Buy " + unit + "x for " + e.currentBuyPrice() + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word()));
                line.append(Text.literal(" "));
            }
            if (e.isSellable()) {
                line.append(button("Sell " + e.currentSellPrice(), Formatting.YELLOW,
                        "/adminshop sell " + shop.getId() + " " + e.getId() + " 1",
                        "Sell " + unit + "x for " + e.currentSellPrice() + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word()));
            }
            if (e.isDynamic()) {
                line.append(Text.literal(" ~").formatted(Formatting.AQUA)
                        .styled(s -> s.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                                Text.literal("Dynamic price - moves with supply & demand")))));
            }
            player.sendMessage(line, false);
        }
        player.sendMessage(Text.literal("(Prices in ").formatted(Formatting.DARK_GRAY)
                .append(NotchCurrency.coinIcon())
                .append(Text.literal(". Click a button to trade 1 at a time.)").formatted(Formatting.DARK_GRAY)), false);
    }

    private static MutableText button(String label, Formatting color, String command, String hover) {
        return Text.literal("[" + label + "]").formatted(color)
                .styled(s -> s
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(hover))));
    }
}
