package net.fugginbeenus.notchcurrency.economy.villager;

import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.math.random.Random;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.TradeOfferList;

/**
 * The "lootable" villager currency integration: whenever a villager rolls new trades (level-up),
 * each emerald-priced trade has a small chance to be re-priced in coins instead — a rare find that
 * lets players spend currency at vanilla villagers. Higher-value trades (more emeralds) get double
 * the chance, so the lucky finds skew toward the trades worth paying coins for. Conversion is a
 * coin SINK (coins handed to the villager leave the economy).
 *
 * Called from {@code VillagerEntityMixin} after vanilla's fillRecipes; converted offers persist
 * with the villager's normal Offers NBT. Config: {@code villagerTrades} in notchcurrency.json.
 */
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

    /** Rolls every emerald-priced offer for coin conversion. Runs right after vanilla fills trades. */
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
        }
    }
}
