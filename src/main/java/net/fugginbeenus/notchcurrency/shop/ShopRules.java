package net.fugginbeenus.notchcurrency.shop;

import net.fugginbeenus.notchcurrency.config.NotchConfig;

public final class ShopRules {

    private ShopRules() {}

    public static boolean adminShops = true;
    public static boolean restock = true;
    public static boolean buyLimits = true;
    public static boolean sellToShops = true;
    public static boolean dynamicPricing = true;

    public static void applyConfig(NotchConfig cfg) {
        NotchConfig.Shops s = cfg.shops;
        adminShops = s.adminShops;
        restock = s.restock;
        buyLimits = s.buyLimits;
        sellToShops = s.sellToShops;
        dynamicPricing = s.dynamicPricing;

        DynamicPrice.ELASTICITY = Math.max(1, Math.min(100, s.priceSensitivity)) / 1000.0;
        DynamicPrice.MIN_MULT = Math.max(10, Math.min(100, s.priceFloorPercent)) / 100.0;
        DynamicPrice.MAX_MULT = Math.max(100, Math.min(1000, s.priceCeilingPercent)) / 100.0;
        DynamicPrice.DECAY = Math.max(1, Math.min(50, s.priceSettlePercent)) / 100.0;
    }

    public static void exportConfig(NotchConfig cfg) {
        NotchConfig.Shops s = cfg.shops;
        s.adminShops = adminShops;
        s.restock = restock;
        s.buyLimits = buyLimits;
        s.sellToShops = sellToShops;
        s.dynamicPricing = dynamicPricing;
    }
}
