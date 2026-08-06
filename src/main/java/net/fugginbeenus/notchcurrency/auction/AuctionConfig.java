package net.fugginbeenus.notchcurrency.auction;

import net.fugginbeenus.notchcurrency.config.NotchConfig;

public final class AuctionConfig {

    public static int LISTING_FEE_FLAT = 0;

    public static int LISTING_FEE_PERCENT = 0;

    public static int LISTING_FEE_MAX = 0;

    public static int SALE_TAX_PERCENT = 0;

    public static int SALE_TAX_MAX = 0;

    private AuctionConfig() {}

    public static void apply(NotchConfig cfg) {
        LISTING_FEE_FLAT = Math.max(0, cfg.auctionListingFeeFlat);
        LISTING_FEE_PERCENT = Math.max(0, cfg.auctionListingFeePercent);
        LISTING_FEE_MAX = Math.max(0, cfg.auctionListingFeeMax);
        SALE_TAX_PERCENT = Math.max(0, Math.min(100, cfg.auctionSaleTaxPercent));
        SALE_TAX_MAX = Math.max(0, cfg.auctionSaleTaxMax);
    }

    public static long listingFee(long price) {
        long fee = LISTING_FEE_FLAT;
        if (LISTING_FEE_PERCENT > 0 && price > 0) {
            fee += (long) Math.floor(price * (LISTING_FEE_PERCENT / 100.0));
        }
        if (LISTING_FEE_MAX > 0) fee = Math.min(fee, LISTING_FEE_MAX);
        return Math.max(0, fee);
    }

    public static long saleTax(long gross) {
        if (SALE_TAX_PERCENT <= 0 || gross <= 0) return 0L;
        long tax = (long) Math.floor(gross * (SALE_TAX_PERCENT / 100.0));
        if (SALE_TAX_MAX > 0) tax = Math.min(tax, SALE_TAX_MAX);
        return Math.max(0, Math.min(tax, gross));
    }

    public static long applySaleTax(long gross) {
        return Math.max(0L, gross - saleTax(gross));
    }
}
