package net.fugginbeenus.notchcurrency.economy.adminshop;

import net.fugginbeenus.notchcurrency.compat.StackData;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class AdminShopManager {

    private AdminShopManager() {}

    public enum Result {
        SUCCESS, ENTRY_NOT_FOUND, NOT_BUYABLE, NOT_SELLABLE,
        INVALID_QUANTITY, INSUFFICIENT_FUNDS, INSUFFICIENT_ITEMS
    }

    public static Result buy(ServerPlayerEntity buyer, AdminShop shop, AdminShopEntry entry, int qty) {
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
        /*buyer.playSoundToPlayer(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.2F);
        *///?} else {
        buyer.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, SoundCategory.PLAYERS, 1.0F, 1.2F);
        //?}
        buyer.sendMessage(Text.literal("Bought ")
                .append(Text.literal((qty * perBundle) + "x ").formatted(Formatting.WHITE))
                .append(template.getName())
                .append(Text.literal(" for ").formatted(Formatting.GREEN))
                .append(NotchCurrency.coins(total)), false);
        return Result.SUCCESS;
    }

    public static Result sell(ServerPlayerEntity seller, AdminShop shop, AdminShopEntry entry, int qty) {
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
        /*seller.playSoundToPlayer(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.PLAYERS, 1.0F, 1.2F);
        *///?} else {
        seller.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), SoundCategory.PLAYERS, 1.0F, 1.2F);
        //?}
        seller.sendMessage(Text.literal("Sold ")
                .append(Text.literal(needed + "x ").formatted(Formatting.WHITE))
                .append(template.getName())
                .append(Text.literal(" for ").formatted(Formatting.GREEN))
                .append(NotchCurrency.coins(total)), false);
        return Result.SUCCESS;
    }

    // ---- inventory helpers ----

    private static int countItems(ServerPlayerEntity player, ItemStack template) {
        int count = 0;
        var inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (StackData.canCombine(template, s)) count += s.getCount();
        }
        return count;
    }

    private static void removeItems(ServerPlayerEntity player, ItemStack template, int amount) {
        int remaining = amount;
        var inv = player.getInventory();
        for (int i = 0; i < inv.size() && remaining > 0; i++) {
            ItemStack s = inv.getStack(i);
            if (StackData.canCombine(template, s)) {
                int take = Math.min(remaining, s.getCount());
                s.decrement(take);
                remaining -= take;
            }
        }
    }

    private static void giveItems(ServerPlayerEntity player, ItemStack template, int amount) {
        int remaining = amount;
        int max = template.getMaxCount();
        while (remaining > 0) {
            int give = Math.min(remaining, max);
            ItemStack stack = template.copy();
            stack.setCount(give);
            if (!player.getInventory().insertStack(stack)) {
                player.dropItem(stack, false);
            }
            remaining -= give;
        }
    }
}
