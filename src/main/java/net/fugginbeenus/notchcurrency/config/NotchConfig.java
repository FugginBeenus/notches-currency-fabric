package net.fugginbeenus.notchcurrency.config;

public final class NotchConfig {
    public Balloon balloon = new Balloon();
    public Cache   cache   = new Cache();

    public static final class Balloon {
        public int centerX = 0;
        public int centerY = 80;
        public int centerZ = 0;
        public int radius  = 25;

        public int minY = 110;
        public int maxY = 150;

        public int perDay = 3;
        public boolean announce = true;

        // Optional spawn window (ticks-of-day) if you want to use it
        public long windowStart = 1000;
        public long windowEnd   = 2000;
    }

    public static final class Cache {
        // super-rare cache knobs
        public boolean announce = true;
        public int     cooldownMinutes = 60; // global cooldown suggestion
        // Loot tuning examples (only used if your manager supports them)
        public int currencyStacksMin = 1;
        public int currencyStacksMax = 3;
        public int currencyPerStackMin = 100;
        public int currencyPerStackMax = 250;
    }

    // Auction / economy tuning

    /** Flat fee (in coins) to create a listing. 0 = free. */
    public int auctionListingFeeFlat = 0;

    /** Percent tax (0–100) taken from the seller’s payout on successful sales. */
    public int auctionSaleTaxPercent = 0;
}
