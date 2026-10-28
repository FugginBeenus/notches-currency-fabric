package net.fugginbeenus.notchcurrency.config;

import net.fugginbeenus.notchcurrency.auction.AuctionConfig;
import net.fugginbeenus.notchcurrency.crate.GoldenCacheManager;
import net.fugginbeenus.notchcurrency.economy.SavingsInterest;
import net.fugginbeenus.notchcurrency.economy.ShopRent;
import net.fugginbeenus.notchcurrency.economy.WealthTax;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyManager;
import net.fugginbeenus.notchcurrency.economy.cosmetic.CosmeticManager;
import net.fugginbeenus.notchcurrency.economy.crate.CrateManager;
import net.fugginbeenus.notchcurrency.economy.enchanter.EnchanterManager;
import net.fugginbeenus.notchcurrency.economy.gambling.GamblingManager;
import net.fugginbeenus.notchcurrency.economy.loan.LoanManager;
import net.fugginbeenus.notchcurrency.economy.raffle.RaffleManager;
import net.fugginbeenus.notchcurrency.economy.villager.VillagerCoinTrades;
import net.fugginbeenus.notchcurrency.integration.WaystoneFeeHandler;
import net.fugginbeenus.notchcurrency.shop.ShopRules;

public final class NotchConfigApply {

    private NotchConfigApply() {}

    public static void applyAll(NotchConfig cfg) {
        AuctionConfig.apply(cfg);
        GoldenCacheManager.applyConfig(cfg);
        WealthTax.applyConfig(cfg);
        SavingsInterest.applyConfig(cfg);
        ShopRent.applyConfig(cfg);
        ShopRules.applyConfig(cfg);
        RaffleManager.applyConfig(cfg);
        BountyManager.applyConfig(cfg);
        CrateManager.applyConfig(cfg);
        LoanManager.applyConfig(cfg);
        GamblingManager.applyConfig(cfg);
        EnchanterManager.applyConfig(cfg);
        CosmeticManager.applyConfig(cfg);
        WaystoneFeeHandler.applyConfig(cfg);
        VillagerCoinTrades.applyConfig(cfg);
    }
}
