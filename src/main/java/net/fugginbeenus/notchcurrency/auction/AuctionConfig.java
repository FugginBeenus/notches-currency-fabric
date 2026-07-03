package net.fugginbeenus.notchcurrency.auction;

import net.fugginbeenus.notchcurrency.config.NotchConfig;

/**
 * Static, runtime-tunable auction economy knobs.
 * Values are loaded once from NotchConfig on server start / reload.
 *
 * Both fees SCALE with the listing price: the listing fee is a flat amount plus a percent of the
 * asking price (optionally capped), and the sale tax is a percent of the sale (optionally capped).
 */
public final class AuctionConfig {

    /** Flat coin fee per listing, added on top of the percent fee (0 = none). */
    public static int LISTING_FEE_FLAT = 0;

    /** Listing fee as a percent of the asking price (0 = none). */
    public static int LISTING_FEE_PERCENT = 0;

    /** Cap on the total listing fee in coins (0 = uncapped). */
    public static int LISTING_FEE_MAX = 0;

    /** Percent taken from seller payout (0–100). */
    public static int SALE_TAX_PERCENT = 0;

    /** Cap on the sale tax in coins (0 = uncapped). */
    public static int SALE_TAX_MAX = 0;

    private AuctionConfig() {}

    public static void apply(NotchConfig cfg) {
        LISTING_FEE_FLAT = Math.max(0, cfg.auctionListingFeeFlat);
        LISTING_FEE_PERCENT = Math.max(0, cfg.auctionListingFeePercent);
        LISTING_FEE_MAX = Math.max(0, cfg.auctionListingFeeMax);
        SALE_TAX_PERCENT = Math.max(0, Math.min(100, cfg.auctionSaleTaxPercent));
        SALE_TAX_MAX = Math.max(0, cfg.auctionSaleTaxMax);
    }

    /** Total listing fee for an item asked at {@code price}: flat + percent, capped. */
    public static long listingFee(long price) {
        long fee = LISTING_FEE_FLAT;
        if (LISTING_FEE_PERCENT > 0 && price > 0) {
            fee += (long) Math.floor(price * (LISTING_FEE_PERCENT / 100.0));
        }
        if (LISTING_FEE_MAX > 0) fee = Math.min(fee, LISTING_FEE_MAX);
        return Math.max(0, fee);
    }

    /** The sale tax coins taken from a gross sale amount (percent, capped). */
    public static long saleTax(long gross) {
        if (SALE_TAX_PERCENT <= 0 || gross <= 0) return 0L;
        long tax = (long) Math.floor(gross * (SALE_TAX_PERCENT / 100.0));
        if (SALE_TAX_MAX > 0) tax = Math.min(tax, SALE_TAX_MAX);
        return Math.max(0, Math.min(tax, gross));
    }

    /** Convenience: compute net payout after tax from a gross amount. */
    public static long applySaleTax(long gross) {
        return Math.max(0L, gross - saleTax(gross));
    }
}
