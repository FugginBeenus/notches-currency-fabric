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

    /**
     * Cosmetics shop: sell cosmetics from any mod for coins (a SINK). Offers are datapack-driven
     * (config/... data/notchcurrency/cosmetics/*.json); each gives an item or runs an unlock command.
     */
    public static final class Cosmetic {
        /** Master toggle for the cosmetics shop. */
        public boolean enabled = true;
    }

    /**
     * Waystone fee: an optional coin SINK charged when a player teleports via a Waystone (soft
     * integration — only active when the Waystones mod is installed). Dimensional jumps can cost
     * more. Broke players are politely denied the teleport.
     */
    public static final class Waystone {
        /** Master toggle. Off by default (opt-in sink; needs the Waystones mod). */
        public boolean enabled = false;

        /** Coins charged for a normal same-dimension waystone teleport. */
        public int fee = 50;

        /** Coins charged for a teleport that crosses dimensions. */
        public int dimensionalFee = 200;

        /** Tell the player how much they paid after each teleport. */
        public boolean announce = true;
    }

    /**
     * Custom currency: the client generates a resource pack ("NotchCurrencyCustom") that reskins
     * the coin everywhere — item, HUD and the chat glyph all share one texture. Drop a square PNG
     * at config/notchcurrency/currency/coin.png (optional coin_tails.png) and/or set a name here;
     * the pack is rebuilt on every game start and deleted when nothing is customized.
     */
    public static final class Currency {
        /** Display name for the coin item ("" = keep "Notch Coin"). */
        public String itemName = "";
    }

    /**
     * Enchanter NPC: pay coins to repair gear, buy specific enchantment levels, or extract an
     * enchantment onto a book. Every payment is a SINK.
     */
    public static final class Enchanter {
        /** Master toggle for the enchanter service. */
        public boolean enabled = true;

        /** Coins to fully repair a 100%-damaged item (scales down with less damage). */
        public int repairFullCost = 60;

        /** Scales all enchantment purchase prices (percent; 200 = double price). */
        public int costMultiplierPercent = 100;

        /** Coins to extract one enchantment onto a book. */
        public int extractCost = 25;

        /** Whether treasure enchantments (Mending etc.) can be bought — at double price. */
        public boolean allowTreasure = true;
    }

    /**
     * Gambling: slot machine + coin flip, bet directly with coins. Bets are a SINK; winnings are a
     * FAUCET. A configurable house edge means more is destroyed than paid over time (net sink). The
     * slot payouts are auto-scaled to hit the target RTP (100% - house edge) no matter the reel odds.
     */
    public static final class Gambling {
        /** Master toggle for all gambling (slots + coin flip). */
        public boolean enabled = true;

        /** Smallest coin bet allowed. */
        public long minBet = 10L;

        /** Largest coin bet allowed. */
        public long maxBet = 1_000L;

        /** Slot machine house edge (percent). 22 = players get ~78% back over time. */
        public int slotsHouseEdgePercent = 22;

        /** Coin flip payout as a percent of the bet on a win. 200 = fair double; &lt;200 = house edge. */
        public int coinFlipPayoutPercent = 195;

        /** How long the coin-flip block "spins" before the result shows, in ticks (20 = 1s). */
        public int coinFlipRevealTicks = 30;
    }

    /**
     * Loans: players borrow coins (created) up to a cap and repay with interest (the interest is
     * a SINK). Interest compounds each cycle; optionally the loan auto-collects from the player's
     * balance so debts don't linger. Off by default — it creates money and is an economy lever.
     */
    public static final class Loan {
        /** Master toggle. */
        public boolean enabled = false;

        /** Most a single player may owe at once. */
        public long maxDebt = 10_000L;

        /** Interest added to the outstanding debt each cycle, as a percent. */
        public int interestPercentPerCycle = 5;

        /** How often interest is applied / auto-collection runs, in minutes (default daily). */
        public int intervalMinutes = 1440;

        /** Each cycle, pull any available balance toward the debt before charging interest. */
        public boolean autoCollect = true;

        /** Days a player has to repay before the loan goes overdue and penalties kick in. */
        public int termDays = 7;

        /** One-time fee added to the debt (percent) the first time a loan goes overdue. */
        public int lateFeePercent = 10;

        /** Interest rate (percent per cycle) applied once a loan is overdue — the consequence. */
        public int overdueInterestPercent = 20;
    }

    /**
     * Crates &amp; keys: placeable crate blocks opened with coin-bought keys. Buying keys is a
     * money SINK; the crate loot (items, some coins) is defined per-crate in datapacks with open
     * odds. A fun, transparent coin drain.
     */
    public static final class Crate {
        /** Master toggle. */
        public boolean enabled = true;

        /** Coin cost of one Crate Key (a SINK). Higher-tier crates need more keys. */
        public long keyPrice = 500L;
    }

    /**
     * Bounty board: auto-generates a rotating set of time-limited bounties (kill/deliver tasks)
     * from datapack pools, paying coin/item rewards — a controllable money/item FAUCET. Admins
     * do no per-bounty work; the board keeps itself topped up.
     */
    public static final class Bounty {
        /** Master toggle for auto-generated bounties. */
        public boolean enabled = true;

        /** How many generated bounties to keep live on the board at once. */
        public int activeCount = 5;

        /** How many bounties a player may have taken (in progress) at once. */
        public int takeLimit = 3;

        /** How long a taken bounty lasts before it expires / rotates, in minutes. */
        public int durationMinutes = 30;

        /** Scales every coin reward, as a percent (100 = unchanged, 50 = half, 0 = no coins). */
        public int rewardMultiplierPercent = 100;

        /** Hard cap on the coins a single bounty can pay after scaling. 0 = no cap. */
        public long maxCoinReward = 250;
    }

    /**
     * Server raffle: players buy tickets into a shared prize pot, then one ticket is drawn
     * (weighted by how many each player bought) and that player wins the whole pot. The
     * house takes a cut of every ticket as a SINK; the rest is pure redistribution, so the
     * raffle is an opt-in sink that also concentrates coins into one lucky payout.
     */
    public static final class Raffle {
        /** Master toggle. */
        public boolean enabled = false;

        /** Coin cost of a single ticket. */
        public long ticketPrice = 100L;

        /** Percent of each ticket destroyed as the house cut (0–100). The rest funds the pot. */
        public int houseCutPercent = 20;

        /** Max tickets one player may hold per round. 0 = unlimited. */
        public int maxTicketsPerPlayer = 0;

        /** Auto-draw interval in minutes. 0 = manual draw only (/raffle draw). */
        public int drawIntervalMinutes = 1440;

        /** Broadcast ticket-buy nudges and the winner announcement to everyone. */
        public boolean announce = true;

        /**
         * Allow turning in an old losing ticket for a few free entries into the new raffle
         * (a consolation: fewer entries than you bought — &lt;5 → 1, &lt;10 → 5, else 10 — and
         * only one redemption per player per round).
         */
        public boolean redeemEnabled = true;
    }

    /**
     * Player-shop rent: a periodic sink charged to open shops. Rent is taken from the
     * shop's own pending earnings first, then the owner's balance. A shop that can't pay
     * is frozen (paused) for a grace period, then auto-closed.
     */
    public static final class ShopRent {
        /** Master toggle. */
        public boolean enabled = false;

        /** Flat rent per open shop per cycle. */
        public long baseRent = 100L;

        /** Extra rent per active listing per cycle (scales rent with shop size). */
        public long perListing = 0L;

        /** How often rent is charged, in minutes (default daily). */
        public int intervalMinutes = 1440;

        /** Cycles a shop may stay frozen for unpaid rent before it auto-closes. */
        public int graceCycles = 3;

        /** Tell owners about rent charges, freezes, and closures. */
        public boolean announce = true;
    }

    /**
     * Progressive wealth tax: a periodic sink that taxes only the portion of a balance
     * ABOVE a threshold, so hoarders are throttled but ordinary players are untouched.
     * Off by default — it's an aggressive lever.
     */
    public static final class WealthTax {
        /** Master toggle. */
        public boolean enabled = false;

        /** Only the balance ABOVE this amount is taxed. */
        public long threshold = 100_000L;

        /** Percent of the excess removed each cycle (0–100). */
        public int ratePercent = 1;

        /** How often the tax runs, in minutes (default daily). */
        public int intervalMinutes = 1440;

        /** Tell taxed players how much they paid. */
        public boolean announce = true;
    }

    /**
     * Economy ledger / audit-log settings. The ledger records every balance change to a
     * rotating JSON-lines file inside the world save, and can optionally mirror
     * admin-relevant events to a Discord webhook.
     */
    public static final class Ledger {
        /** Write a per-day JSON-lines audit file under &lt;world&gt;/notchcurrency/ledger/. */
        public boolean fileLogEnabled = true;

        /** Post admin-relevant events (admin actions + large transactions) to a Discord webhook. */
        public boolean webhookEnabled = false;

        /** Discord webhook URL. Leave blank to disable regardless of webhookEnabled. */
        public String webhookUrl = "";

        /** Transactions whose absolute amount is >= this also post to the webhook. 0 = never by size. */
        public long webhookLargeTxnThreshold = 10_000L;
    }

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

    /** Flat fee (in coins) to create a listing, added on top of the percent fee. 0 = none. */
    public int auctionListingFeeFlat = 0;

    /** Listing fee as a percent of the asking price (scales with the listing). 0 = none. */
    public int auctionListingFeePercent = 0;

    /** Cap on the total listing fee in coins. 0 = uncapped. */
    public int auctionListingFeeMax = 0;

    /** Percent tax (0–100) taken from the seller’s payout on successful sales. */
    public int auctionSaleTaxPercent = 0;

    /** Cap on the sale tax in coins. 0 = uncapped. */
    public int auctionSaleTaxMax = 0;
}
