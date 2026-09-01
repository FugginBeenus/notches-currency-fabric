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

    private static String allowanceText(AdminShopEntry e, ServerPlayer player) {
        StringBuilder out = new StringBuilder();
        if (e.getBuyLimit() > 0) {
            out.append(e.remainingBuy(player.getUUID())).append(" buy left");
        }
        if (e.getSellLimit() > 0) {
            if (out.length() > 0) out.append(", ");
            out.append(e.remainingSell(player.getUUID())).append(" sell left");
        }
        return out.toString();
    }

    public static void sendListing(ServerPlayer player, AdminShop shop) {
        net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("═══ " + shop.getName() + " ═══").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));

        if (shop.getEntries().isEmpty()) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("This shop has no items yet.").withStyle(ChatFormatting.GRAY));
            return;
        }

        for (AdminShopEntry e : shop.getEntries()) {
            e.maybeReset(player.serverLevel());
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

            String left = allowanceText(e, player);
            if (!left.isEmpty()) {
                line.append(Component.literal("  " + left).withStyle(ChatFormatting.DARK_GRAY));
            }
            if (e.isDynamic()) {
                line.append(Component.literal(" ~").withStyle(ChatFormatting.AQUA)
                        .withStyle(s -> s.withHoverEvent(net.fugginbeenus.notchcurrency.compat.Chat.showText(Component.literal("Dynamic price - moves with supply & demand")))));
            }
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, line);
        }
        net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("(Prices in ").withStyle(ChatFormatting.DARK_GRAY)
                .append(NotchCurrency.coinIcon())
                .append(Component.literal(". Click a button to trade 1 at a time.)").withStyle(ChatFormatting.DARK_GRAY)));
    }

    private static MutableComponent button(String label, ChatFormatting color, String command, String hover) {
        return Component.literal("[" + label + "]").withStyle(color)
                .withStyle(s -> s
                        .withClickEvent(net.fugginbeenus.notchcurrency.compat.Chat.runCommand(command))
                        .withHoverEvent(net.fugginbeenus.notchcurrency.compat.Chat.showText(Component.literal(hover))));
    }
}
