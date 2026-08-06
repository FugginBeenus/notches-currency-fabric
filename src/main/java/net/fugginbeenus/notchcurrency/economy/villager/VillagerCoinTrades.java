package net.fugginbeenus.notchcurrency.economy.villager;

import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

public final class VillagerCoinTrades {

    private static boolean enabled = true;
    private static int chancePercent = 10;
    private static int coinsPerEmerald = 3;

    private VillagerCoinTrades() {}

    public static void applyConfig(NotchConfig cfg) {
        enabled = cfg.villagerTrades.enabled;
        chancePercent = Math.max(0, Math.min(100, cfg.villagerTrades.chancePercent));
        coinsPerEmerald = Math.max(1, cfg.villagerTrades.coinsPerEmerald);
    }

    public static void convert(VillagerEntity villager) {
        if (!enabled || chancePercent <= 0 || villager.getWorld().isClient()) return;

        TradeOfferList offers = villager.getOffers();
        Random random = villager.getRandom();
        for (int i = 0; i < offers.size(); i++) {
            TradeOffer offer = offers.get(i);
            ItemStack first = offer.getOriginalFirstBuyItem();
            if (!first.isOf(Items.EMERALD)) continue;

            int emeralds = first.getCount();
            int cost = emeralds * coinsPerEmerald;
            ItemStack coins = new ItemStack(ModItems.NOTCH_COIN);
            int max = coins.getMaxCount();

            // The price has to fit the two buy slots: one coin stack, or an overflow split into the
            // second slot when it's free. Anything pricier stays an emerald trade.
            //? if >=1.21 {
            /*java.util.Optional<net.minecraft.village.TradedItem> second = offer.getSecondBuyItem();
            java.util.Optional<net.minecraft.village.TradedItem> secondOut;
            if (cost <= max) {
                coins.setCount(cost);
                secondOut = second;
            } else if (second.isEmpty() && cost <= max * 2) {
                coins.setCount(max);
                secondOut = java.util.Optional.of(
                        new net.minecraft.village.TradedItem(ModItems.NOTCH_COIN, cost - max));
            } else {
                continue;
            }

            int chance = emeralds >= 8 ? Math.min(100, chancePercent * 2) : chancePercent;
            if (random.nextInt(100) >= chance) continue;

            offers.set(i, new TradeOffer(
                    new net.minecraft.village.TradedItem(ModItems.NOTCH_COIN, coins.getCount()),
                    secondOut, offer.getSellItem(),
                    offer.getUses(), offer.getMaxUses(), offer.getMerchantExperience(),
                    offer.getPriceMultiplier()));
            *///?} else {
            ItemStack second = offer.getSecondBuyItem();
            ItemStack secondOut;
            if (cost <= max) {
                coins.setCount(cost);
                secondOut = second;
            } else if (second.isEmpty() && cost <= max * 2) {
                coins.setCount(max);
                secondOut = new ItemStack(ModItems.NOTCH_COIN, cost - max);
            } else {
                continue;
            }

            int chance = emeralds >= 8 ? Math.min(100, chancePercent * 2) : chancePercent;
            if (random.nextInt(100) >= chance) continue;

            offers.set(i, new TradeOffer(coins, secondOut, offer.getSellItem(),
                    offer.getUses(), offer.getMaxUses(), offer.getMerchantExperience(),
                    offer.getPriceMultiplier()));
            //?}
        }
    }
}
