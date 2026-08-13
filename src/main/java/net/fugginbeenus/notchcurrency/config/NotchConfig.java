package net.fugginbeenus.notchcurrency.config;

public final class NotchConfig {
    public Balloon balloon = new Balloon();
    public Cache   cache   = new Cache();
    public Ledger  ledger  = new Ledger();
    public WealthTax wealthTax = new WealthTax();
    public ShopRent shopRent = new ShopRent();
    public Raffle raffle = new Raffle();
    public Bounty bounty = new Bounty();
    public Crate crate = new Crate();
    public Loan loan = new Loan();
    public Gambling gambling = new Gambling();
    public Enchanter enchanter = new Enchanter();
    public Currency currency = new Currency();
    public Waystone waystone = new Waystone();
    public Cosmetic cosmetic = new Cosmetic();
    public Hud hud = new Hud();
    public VillagerTrades villagerTrades = new VillagerTrades();

    // NOTE: currency.itemName + config/notchcurrency/currency/*.png are also read SERVER-side and
    // pushed to every joining player (CurrencyServerSync), so admins customize the coin once.

    public static final class VillagerTrades {
        public boolean enabled = true;

        public int chancePercent = 10;

        public int coinsPerEmerald = 3;
    }

    public static final class Hud {
        public String bountyTrackerCorner = "TOP_RIGHT";

        public int bountyTrackerX = 6;
        public int bountyTrackerY = 6;

        public int bountyTrackerScale = 100;

        public int bountyTrackerOpacity = 85;
    }

    public static final class Cosmetic {
        public boolean enabled = true;
    }

    public static final class Waystone {
        public boolean enabled = false;

        public int fee = 50;

        public int dimensionalFee = 200;

        public boolean announce = true;
    }

    public static final class Currency {
        public String itemName = "";
    }

    public static final class Enchanter {
        public boolean enabled = true;

        public int repairFullCost = 60;

        public int costMultiplierPercent = 100;

        public int costCommon = 15;
        public int costUncommon = 25;
        public int costRare = 45;
        public int costVeryRare = 80;

        public int treasureMultiplierPercent = 200;

        public int extractCost = 25;

        public int extractValuePercent = 100;

        public boolean allowTreasure = true;

        public int uncraftCost = 30;
    }

    public static final class Gambling {
        public boolean enabled = true;

        public long minBet = 10L;

        public long maxBet = 1_000L;

        public int slotsHouseEdgePercent = 22;

        public int coinFlipPayoutPercent = 195;

        public int coinFlipRevealTicks = 30;
    }

    public static final class Loan {
        public boolean enabled = false;

        public long maxDebt = 10_000L;

        public int interestPercentPerCycle = 5;

        public int intervalMinutes = 1440;

        public boolean autoCollect = true;

        public int termDays = 7;

        public int lateFeePercent = 10;

        public int overdueInterestPercent = 20;
    }

    public static final class Crate {
        public boolean enabled = true;

        public long keyPrice = 500L;
    }

    public static final class Bounty {
        public boolean enabled = true;

        public int activeCount = 5;

        public int takeLimit = 3;

        public int durationMinutes = 30;

        public int rewardMultiplierPercent = 100;

        public long maxCoinReward = 250;
    }

    public static final class Raffle {
        public boolean enabled = false;

        public long ticketPrice = 100L;

        public int houseCutPercent = 20;

        public int maxTicketsPerPlayer = 0;

        public int drawIntervalMinutes = 1440;

        public boolean announce = true;

        public boolean redeemEnabled = true;
    }

    public static final class ShopRent {
        public boolean enabled = false;

        public long baseRent = 100L;

        public long perListing = 0L;

        public int intervalMinutes = 1440;

        public int graceCycles = 3;

        public boolean announce = true;
    }

    public static final class WealthTax {
        public boolean enabled = false;

        public long threshold = 100_000L;

        public int ratePercent = 1;

        public int intervalMinutes = 1440;

        public boolean announce = true;
    }

    public static final class Ledger {
        public boolean fileLogEnabled = true;

        public boolean webhookEnabled = false;

        public String webhookUrl = "";

        public long webhookLargeTxnThreshold = 10_000L;
    }

    public static final class Balloon {
        /**
         * Whether weekly waves happen at all.
         *
         * <p>Off until somebody says otherwise, so a world nobody has set up does not get balloons
         * dropped on its origin. This is the same switch /balloon setArea throws.
         */
        public boolean enabled = false;

        public int centerX = 0;
        public int centerY = 80;
        public int centerZ = 0;
        public int radius  = 25;

        public int minY = 110;
        public int maxY = 150;

        public int perDay = 3;
        public boolean announce = true;

        /** One each for everybody online when a wave fires, on top of the wave over the area. */
        public boolean perPlayer = false;
        public boolean playerInAreaOnly = false;
        public int playerHeight = 40;
        public int playerSpread = 12;

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

    public int auctionListingFeeFlat = 0;

    public int auctionListingFeePercent = 0;

    public int auctionListingFeeMax = 0;

    public int auctionSaleTaxPercent = 0;

    public int auctionSaleTaxMax = 0;
}
