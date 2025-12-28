package net.fugginbeenus.notchcurrency.auction;

import net.fugginbeenus.notchcurrency.config.NotchConfig;

/**
 * Static, runtime-tunable auction economy knobs.
 * Values are loaded once from NotchConfig on server start / reload.
 */
public final class AuctionConfig {

    /** Flat coin fee per listing (0 = none). */
    public static int LISTING_FEE_FLAT = 0;

    /** Percent taken from seller payout (0–100). */
    public static int SALE_TAX_PERCENT = 0;

    private AuctionConfig() {}

    public static void apply(NotchConfig cfg) {
        LISTING_FEE_FLAT = Math.max(0, cfg.auctionListingFeeFlat);
        SALE_TAX_PERCENT = Math.max(0, Math.min(100, cfg.auctionSaleTaxPercent));
    }

    /** Convenience: compute net payout after tax from a gross amount. */
    public static int applySaleTax(int gross) {
        if (SALE_TAX_PERCENT <= 0) return gross;
        int tax = (int) Math.floor(gross * (SALE_TAX_PERCENT / 100.0));
        int net = gross - tax;
        return Math.max(net, 0);
    }
}
