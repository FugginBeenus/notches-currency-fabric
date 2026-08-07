package net.fugginbeenus.notchcurrency.economy.adminshop;

import net.fugginbeenus.notchcurrency.compat.StackData;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

public final class AdminShopManager {

    private AdminShopManager() {}

    public enum Result {
        SUCCESS, ENTRY_NOT_FOUND, NOT_BUYABLE, NOT_SELLABLE,
        INVALID_QUANTITY, INSUFFICIENT_FUNDS, INSUFFICIENT_ITEMS
    }

    public static Result buy(ServerPlayer buyer, AdminShop shop, AdminShopEntry entry, int qty) {
        if (entry == null) return Result.ENTRY_NOT_FOUND;
        if (qty <= 0 || qty > 256) return Result.INVALID_QUANTITY;
        if (!entry.isBuyable()) return Result.NOT_BUYABLE;

        long unitPrice = entry.currentBuyPrice();
        long total = unitPrice * qty;

        if (CurrencyApi.getBalance(buyer) < total) {
            return Result.INSUFFICIENT_FUNDS;
        }
        if (!CurrencyApi.withdraw(buyer, total, TransactionReason.SINK,
                "admin shop buy: " + shop.getName())) {
            return Result.INSUFFICIENT_FUNDS;
        }

        int perBundle = entry.getUnit();
        ItemStack template = entry.getItem();
        giveItems(buyer, template, qty * perBundle);
        entry.recordBuy(qty);

        //? if >=1.21 {
        /*buyer.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.2F);
        *///?} else {
        buyer.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.2F);
        //?}
        buyer.displayClientMessage(Component.literal("Bought ")
                .append(Component.literal((qty * perBundle) + "x ").withStyle(ChatFormatting.WHITE))
                .append(template.getHoverName())
                .append(Component.literal(" for ").withStyle(ChatFormatting.GREEN))
                .append(NotchCurrency.coins(total)), false);
        return Result.SUCCESS;
    }

    public static Result sell(ServerPlayer seller, AdminShop shop, AdminShopEntry entry, int qty) {
        if (entry == null) return Result.ENTRY_NOT_FOUND;
        if (qty <= 0 || qty > 256) return Result.INVALID_QUANTITY;
        if (!entry.isSellable()) return Result.NOT_SELLABLE;

        ItemStack template = entry.getItem();
        int perBundle = entry.getUnit();
        int needed = qty * perBundle;

        if (countItems(seller, template) < needed) {
            return Result.INSUFFICIENT_ITEMS;
        }

        long unitPrice = entry.currentSellPrice();
        long total = unitPrice * qty;

        removeItems(seller, template, needed);
        if (total > 0) {
            CurrencyApi.deposit(seller, total, TransactionReason.FAUCET,
                    "admin shop sell: " + shop.getName());
        }
        entry.recordSell(qty);

        //? if >=1.21 {
        /*seller.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
        *///?} else {
        seller.playNotifySound(SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 1.0F, 1.2F);
        //?}
        seller.displayClientMessage(Component.literal("Sold ")
                .append(Component.literal(needed + "x ").withStyle(ChatFormatting.WHITE))
                .append(template.getHoverName())
                .append(Component.literal(" for ").withStyle(ChatFormatting.GREEN))
                .append(NotchCurrency.coins(total)), false);
        return Result.SUCCESS;
    }

    // ---- inventory helpers ----

    private static int countItems(ServerPlayer player, ItemStack template) {
        int count = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (StackData.canCombine(template, s)) count += s.getCount();
        }
        return count;
    }

    private static void removeItems(ServerPlayer player, ItemStack template, int amount) {
        int remaining = amount;
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
            ItemStack s = inv.getItem(i);
            if (StackData.canCombine(template, s)) {
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }
    }

    private static void giveItems(ServerPlayer player, ItemStack template, int amount) {
        int remaining = amount;
        int max = template.getMaxStackSize();
        while (remaining > 0) {
            int give = Math.min(remaining, max);
            ItemStack stack = template.copy();
            stack.setCount(give);
            if (!player.getInventory().add(stack)) {
                player.drop(stack, false);
            }
            remaining -= give;
        }
    }
}
