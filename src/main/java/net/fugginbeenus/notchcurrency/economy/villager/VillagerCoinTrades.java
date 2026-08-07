package net.fugginbeenus.notchcurrency.economy.villager;

import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

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

    public static void convert(Villager villager) {
        if (!enabled || chancePercent <= 0 || villager.level().isClientSide()) return;

        MerchantOffers offers = villager.getOffers();
        RandomSource random = villager.getRandom();
        for (int i = 0; i < offers.size(); i++) {
            MerchantOffer offer = offers.get(i);
            ItemStack first = offer.getBaseCostA();
            if (!first.is(Items.EMERALD)) continue;

            int emeralds = first.getCount();
            int cost = emeralds * coinsPerEmerald;
            ItemStack coins = new ItemStack(ModItems.NOTCH_COIN);
            int max = coins.getMaxStackSize();

            // The price has to fit the two buy slots: one coin stack, or an overflow split into the
            // second slot when it's free. Anything pricier stays an emerald trade.
            //? if >=1.21 {
            /*java.util.Optional<net.minecraft.world.item.trading.ItemCost> second = offer.getCostB();
            java.util.Optional<net.minecraft.world.item.trading.ItemCost> secondOut;
            if (cost <= max) {
                coins.setCount(cost);
                secondOut = second;
            } else if (second.isEmpty() && cost <= max * 2) {
                coins.setCount(max);
                secondOut = java.util.Optional.of(
                        new net.minecraft.world.item.trading.ItemCost(ModItems.NOTCH_COIN, cost - max));
            } else {
                continue;
            }

            int chance = emeralds >= 8 ? Math.min(100, chancePercent * 2) : chancePercent;
            if (random.nextInt(100) >= chance) continue;

            offers.set(i, new MerchantOffer(
                    new net.minecraft.world.item.trading.ItemCost(ModItems.NOTCH_COIN, coins.getCount()),
                    secondOut, offer.getResult(),
                    offer.getUses(), offer.getMaxUses(), offer.getXp(),
                    offer.getPriceMultiplier()));
            *///?} else {
            ItemStack second = offer.getCostB();
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

            offers.set(i, new MerchantOffer(coins, secondOut, offer.getResult(),
                    offer.getUses(), offer.getMaxUses(), offer.getXp(),
                    offer.getPriceMultiplier()));
            //?}
        }
    }
}
