package net.fugginbeenus.notchcurrency.config.ui;

import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.config.ui.ConfigEntry.BoolEntry;
import net.fugginbeenus.notchcurrency.config.ui.ConfigEntry.NumberEntry;
import net.fugginbeenus.notchcurrency.config.ui.ConfigEntry.SelectEntry;
import net.fugginbeenus.notchcurrency.config.ui.ConfigEntry.SliderEntry;
import net.fugginbeenus.notchcurrency.config.ui.ConfigEntry.StringEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Every setting shown by the config screen, bound to a live {@link NotchConfig}. Categories appear
 * in this order; Currency stays near the top so the maker is easy to find. Compared to the old
 * Cloth screen this also exposes the per-rarity enchant prices, treasure multiplier, extract value
 * charge and uncraft fee that were config-file-only before.
 */
final class ConfigEntries {

    private ConfigEntries() {}

    static List<ConfigEntry> build(NotchConfig cfg) {
        List<ConfigEntry> e = new ArrayList<>();

        // ===== Economy =====
        String c = "Economy";
        e.add(new NumberEntry(c, "Auction listing fee — flat", cfg.auctionListingFeeFlat, 0, 0, 1_000_000,
                v -> cfg.auctionListingFeeFlat = v.intValue(),
                "Flat coin fee to create an auction listing, on top of the percent fee.",
                "A money SINK. 0 = no flat fee."));
        e.add(new NumberEntry(c, "Auction listing fee — % of price", cfg.auctionListingFeePercent, 0, 0, 100,
                v -> cfg.auctionListingFeePercent = v.intValue(),
                "Listing fee as a percent of the asking price — scales with the listing.",
                "A money SINK. 0 = no percent fee."));
        e.add(new NumberEntry(c, "Auction listing fee — max", cfg.auctionListingFeeMax, 0, 0, 10_000_000,
                v -> cfg.auctionListingFeeMax = v.intValue(),
                "Cap on the total listing fee. 0 = uncapped."));
        e.add(new NumberEntry(c, "Auction sale tax (%)", cfg.auctionSaleTaxPercent, 0, 0, 100,
                v -> cfg.auctionSaleTaxPercent = v.intValue(),
                "Percent taken from the seller's payout on a sale.", "A money SINK. 0 = no tax."));
        e.add(new NumberEntry(c, "Auction sale tax — max", cfg.auctionSaleTaxMax, 0, 0, 10_000_000,
                v -> cfg.auctionSaleTaxMax = v.intValue(),
                "Cap on the sale tax per sale. 0 = uncapped."));

        // ===== Currency =====
        c = "Currency";
        e.add(new StringEntry(c, "Coin name", cfg.currency.itemName, "", 64,
                v -> cfg.currency.itemName = v,
                "Rename the coin everywhere — the item AND messages/GUIs (\"You won 50 Rupees\").",
                "Pick a name that reads well after a number. Blank keeps \"Notch Coin\"/\"coins\".",
                "Drop coin.png in config/notchcurrency/currency/ to reskin the art.",
                "A resource pack is generated on save; servers push it to every player."));

        // ===== Villager Trades =====
        c = "Villager Trades";
        e.add(new BoolEntry(c, "Coin-priced villager trades", cfg.villagerTrades.enabled, true,
                v -> cfg.villagerTrades.enabled = v,
                "When villagers roll new trades, some may be priced in coins instead of",
                "emeralds — a rare find that makes currency spendable at villagers (a SINK)."));
        e.add(new SliderEntry(c, "Conversion chance", cfg.villagerTrades.chancePercent, 10, 0, 100, "%",
                v -> cfg.villagerTrades.chancePercent = v,
                "Chance per new emerald trade. High-value trades (8+ emeralds) get double."));
        e.add(new NumberEntry(c, "Coins per emerald", cfg.villagerTrades.coinsPerEmerald, 3, 1, 1000,
                v -> cfg.villagerTrades.coinsPerEmerald = v.intValue(),
                "Coin price for each emerald of the original trade.",
                "Trades too pricey to fit the two buy slots stay emerald-priced."));

        // ===== HUD =====
        c = "HUD";
        e.add(new SelectEntry(c, "Bounty tracker position",
                new String[]{"TOP_LEFT", "TOP_CENTER", "TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_CENTER", "BOTTOM_RIGHT"},
                cfg.hud.bountyTrackerCorner, "TOP_RIGHT",
                v -> cfg.hud.bountyTrackerCorner = v,
                "Screen anchor for the bounty tracker pills.",
                "Client-side: on a server each player sets their own."));
        e.add(new NumberEntry(c, "Bounty tracker X offset", cfg.hud.bountyTrackerX, 6, -500, 500,
                v -> cfg.hud.bountyTrackerX = v.intValue(),
                "Pixels inward from the anchor (left/right nudge on CENTER)."));
        e.add(new NumberEntry(c, "Bounty tracker Y offset", cfg.hud.bountyTrackerY, 6, -500, 500,
                v -> cfg.hud.bountyTrackerY = v.intValue(),
                "Pixels inward from the top or bottom edge."));
        e.add(new SliderEntry(c, "Bounty tracker scale", cfg.hud.bountyTrackerScale, 100, 50, 200, "%",
                v -> cfg.hud.bountyTrackerScale = v,
                "Shrink or grow the whole tracker."));
        e.add(new SliderEntry(c, "Bounty tracker opacity", cfg.hud.bountyTrackerOpacity, 85, 0, 100, "%",
                v -> cfg.hud.bountyTrackerOpacity = v,
                "Background darkness of the pills."));

        // ===== Bounty Board =====
        c = "Bounty Board";
        e.add(new BoolEntry(c, "Bounties enabled", cfg.bounty.enabled, true,
                v -> cfg.bounty.enabled = v,
                "Auto-generate rotating kill/deliver bounties from datapack pools.",
                "A coin/item FAUCET. Rewards are created money."));
        e.add(new NumberEntry(c, "Live bounties on the board", cfg.bounty.activeCount, 5, 0, 20,
                v -> cfg.bounty.activeCount = v.intValue(),
                "How many generated bounties are available at once."));
        e.add(new NumberEntry(c, "Bounties per player", cfg.bounty.takeLimit, 3, 1, 5,
                v -> cfg.bounty.takeLimit = v.intValue(),
                "How many bounties a player may have in progress at a time."));
        e.add(new NumberEntry(c, "Bounty duration (minutes)", cfg.bounty.durationMinutes, 30, 1, 10_080,
                v -> cfg.bounty.durationMinutes = v.intValue(),
                "How long each bounty lasts before it rotates out."));
        e.add(new SliderEntry(c, "Coin reward scale", cfg.bounty.rewardMultiplierPercent, 100, 0, 1000, "%",
                v -> cfg.bounty.rewardMultiplierPercent = v,
                "Scales every coin reward. 100 = unchanged, 50 = half, 0 = no coins."));
        e.add(new NumberEntry(c, "Max coin reward", cfg.bounty.maxCoinReward, 250, 0, 10_000_000,
                v -> cfg.bounty.maxCoinReward = v,
                "Hard cap on the coins one bounty can pay (after scaling). 0 = no cap."));

        // ===== Enchanter =====
        c = "Enchanter";
        e.add(new BoolEntry(c, "Enchanter enabled", cfg.enchanter.enabled, true,
                v -> cfg.enchanter.enabled = v,
                "The enchant/repair/extract/uncraft service NPC. Every payment is a SINK."));
        e.add(new NumberEntry(c, "Full repair cost", cfg.enchanter.repairFullCost, 60, 0, 1_000_000,
                v -> cfg.enchanter.repairFullCost = v.intValue(),
                "Coins to fully repair a 100%-damaged item; scales down with less damage."));
        e.add(new NumberEntry(c, "Enchant price — Common (per level)", cfg.enchanter.costCommon, 15, 0, 100_000,
                v -> cfg.enchanter.costCommon = v.intValue(),
                "Per-level price for common enchantments (Sharpness, Protection...)."));
        e.add(new NumberEntry(c, "Enchant price — Uncommon (per level)", cfg.enchanter.costUncommon, 25, 0, 100_000,
                v -> cfg.enchanter.costUncommon = v.intValue(),
                "Per-level price for uncommon enchantments."));
        e.add(new NumberEntry(c, "Enchant price — Rare (per level)", cfg.enchanter.costRare, 45, 0, 100_000,
                v -> cfg.enchanter.costRare = v.intValue(),
                "Per-level price for rare enchantments."));
        e.add(new NumberEntry(c, "Enchant price — Very Rare (per level)", cfg.enchanter.costVeryRare, 80, 0, 100_000,
                v -> cfg.enchanter.costVeryRare = v.intValue(),
                "Per-level price for very rare enchantments (Mending, Infinity...)."));
        e.add(new SliderEntry(c, "Global price multiplier", cfg.enchanter.costMultiplierPercent, 100, 1, 1000, "%",
                v -> cfg.enchanter.costMultiplierPercent = v,
                "Scales all enchantment purchase prices. 200 = double."));
        e.add(new SliderEntry(c, "Treasure price multiplier", cfg.enchanter.treasureMultiplierPercent, 200, 100, 1000, "%",
                v -> cfg.enchanter.treasureMultiplierPercent = v,
                "Extra multiplier on treasure enchantments (Mending etc.)."));
        e.add(new BoolEntry(c, "Sell treasure enchants", cfg.enchanter.allowTreasure, true,
                v -> cfg.enchanter.allowTreasure = v,
                "Whether Mending and other treasure enchantments can be bought at all."));
        e.add(new NumberEntry(c, "Extract base cost", cfg.enchanter.extractCost, 25, 0, 1_000_000,
                v -> cfg.enchanter.extractCost = v.intValue(),
                "Base coins to pull one enchantment off an item onto a book."));
        e.add(new SliderEntry(c, "Extract value charge", cfg.enchanter.extractValuePercent, 100, 0, 1000, "%",
                v -> cfg.enchanter.extractValuePercent = v,
                "Extract price = base + the enchant's purchase value × this percent.",
                "Keeps extraction from undercutting enchant prices (anti book-farm)."));
        e.add(new NumberEntry(c, "Uncraft fee", cfg.enchanter.uncraftCost, 30, 0, 1_000_000,
                v -> cfg.enchanter.uncraftCost = v.intValue(),
                "Coins to break an undamaged item back into its crafting parts."));

        // ===== Gambling =====
        c = "Gambling";
        e.add(new BoolEntry(c, "Gambling enabled", cfg.gambling.enabled, true,
                v -> cfg.gambling.enabled = v,
                "Slots + coin flip. Bets are a SINK, winnings a FAUCET; the house edge nets a sink."));
        e.add(new NumberEntry(c, "Minimum bet", cfg.gambling.minBet, 10, 1, 1_000_000_000,
                v -> cfg.gambling.minBet = v));
        e.add(new NumberEntry(c, "Maximum bet", cfg.gambling.maxBet, 1_000, 1, 1_000_000_000,
                v -> cfg.gambling.maxBet = v));
        e.add(new SliderEntry(c, "Slots house edge", cfg.gambling.slotsHouseEdgePercent, 22, 0, 90, "%",
                v -> cfg.gambling.slotsHouseEdgePercent = v,
                "The cut the house keeps. 22 = players get ~78% back over time.",
                "Payouts auto-scale to hit this edge no matter the reel odds."));
        e.add(new SliderEntry(c, "Coin flip payout", cfg.gambling.coinFlipPayoutPercent, 195, 100, 300, "%",
                v -> cfg.gambling.coinFlipPayoutPercent = v,
                "Percent of the bet returned on a win. 200 = fair double; below 200 is the house edge."));
        e.add(new NumberEntry(c, "Coin flip reveal (ticks)", cfg.gambling.coinFlipRevealTicks, 30, 0, 200,
                v -> cfg.gambling.coinFlipRevealTicks = v.intValue(),
                "How long the coin-flip block spins before the result shows. 20 ticks = 1 second."));

        // ===== Raffle =====
        c = "Raffle";
        e.add(new BoolEntry(c, "Raffle enabled", cfg.raffle.enabled, false,
                v -> cfg.raffle.enabled = v,
                "Players buy tickets into a shared pot; one weighted winner takes it all.",
                "The house cut on each ticket is a SINK; the pot is redistributed."));
        e.add(new NumberEntry(c, "Ticket price", cfg.raffle.ticketPrice, 100, 1, 1_000_000_000,
                v -> cfg.raffle.ticketPrice = v));
        e.add(new SliderEntry(c, "House cut", cfg.raffle.houseCutPercent, 20, 0, 100, "%",
                v -> cfg.raffle.houseCutPercent = v,
                "Percent of each ticket destroyed as the house cut (a SINK).",
                "The rest funds the prize pot."));
        e.add(new NumberEntry(c, "Max tickets per player", cfg.raffle.maxTicketsPerPlayer, 0, 0, 100_000,
                v -> cfg.raffle.maxTicketsPerPlayer = v.intValue(),
                "Cap per player per round. 0 = unlimited."));
        e.add(new NumberEntry(c, "Auto-draw interval (minutes)", cfg.raffle.drawIntervalMinutes, 1440, 0, 100_000,
                v -> cfg.raffle.drawIntervalMinutes = v.intValue(),
                "How often a winner is drawn automatically. 1440 = once a day.",
                "0 = manual only (/raffle draw)."));
        e.add(new BoolEntry(c, "Announce entries & winner", cfg.raffle.announce, true,
                v -> cfg.raffle.announce = v));
        e.add(new BoolEntry(c, "Allow ticket redemption", cfg.raffle.redeemEnabled, true,
                v -> cfg.raffle.redeemEnabled = v,
                "Let players turn in an old losing ticket for a few free entries.",
                "<5 entries → 1, <10 → 5, else 10; once per player per round."));

        // ===== Crates =====
        c = "Crates";
        e.add(new BoolEntry(c, "Crates enabled", cfg.crate.enabled, true,
                v -> cfg.crate.enabled = v,
                "Crate blocks opened with coin-bought keys.",
                "Buying keys is a SINK; loot is defined per-crate in datapacks."));
        e.add(new NumberEntry(c, "Crate key price", cfg.crate.keyPrice, 500, 0, 1_000_000_000,
                v -> cfg.crate.keyPrice = v,
                "Coin cost of one Crate Key. Higher-tier crates need more keys."));

        // ===== Loans =====
        c = "Loans";
        e.add(new BoolEntry(c, "Loans enabled", cfg.loan.enabled, false,
                v -> cfg.loan.enabled = v,
                "Players borrow coins (created) up to a cap and repay with interest.",
                "Interest is a SINK. Off by default — it creates money."));
        e.add(new NumberEntry(c, "Borrowing limit (max debt)", cfg.loan.maxDebt, 10_000, 0, 1_000_000_000,
                v -> cfg.loan.maxDebt = v));
        e.add(new SliderEntry(c, "Interest per cycle", cfg.loan.interestPercentPerCycle, 5, 0, 100, "%",
                v -> cfg.loan.interestPercentPerCycle = v));
        e.add(new NumberEntry(c, "Interest interval (minutes)", cfg.loan.intervalMinutes, 1440, 1, 100_000,
                v -> cfg.loan.intervalMinutes = v.intValue(),
                "How often interest is applied. 1440 = once a day."));
        e.add(new BoolEntry(c, "Auto-collect from balance", cfg.loan.autoCollect, true,
                v -> cfg.loan.autoCollect = v,
                "Each cycle, pull any spare balance toward the debt before charging interest."));
        e.add(new NumberEntry(c, "Loan term (days)", cfg.loan.termDays, 7, 1, 365,
                v -> cfg.loan.termDays = v.intValue(),
                "How long a borrower has to repay before the loan goes overdue."));
        e.add(new SliderEntry(c, "Late fee", cfg.loan.lateFeePercent, 10, 0, 100, "%",
                v -> cfg.loan.lateFeePercent = v,
                "One-time penalty added to the debt the first cycle it is overdue."));
        e.add(new SliderEntry(c, "Overdue interest", cfg.loan.overdueInterestPercent, 20, 0, 100, "%",
                v -> cfg.loan.overdueInterestPercent = v,
                "Interest rate charged each cycle while a loan is past due (replaces the normal rate)."));

        // ===== Shop Rent =====
        c = "Shop Rent";
        e.add(new BoolEntry(c, "Shop rent enabled", cfg.shopRent.enabled, false,
                v -> cfg.shopRent.enabled = v,
                "Charge open player-shops rent each cycle (a SINK).",
                "Paid from shop earnings first, then the owner's balance."));
        e.add(new NumberEntry(c, "Base rent per shop", cfg.shopRent.baseRent, 100, 0, 1_000_000_000,
                v -> cfg.shopRent.baseRent = v));
        e.add(new NumberEntry(c, "Rent per listing", cfg.shopRent.perListing, 0, 0, 1_000_000_000,
                v -> cfg.shopRent.perListing = v,
                "Extra rent per active listing, scaling with shop size."));
        e.add(new NumberEntry(c, "Interval (minutes)", cfg.shopRent.intervalMinutes, 1440, 1, 100_000,
                v -> cfg.shopRent.intervalMinutes = v.intValue(),
                "How often rent is charged. 1440 = once a day."));
        e.add(new NumberEntry(c, "Grace cycles before close", cfg.shopRent.graceCycles, 3, 0, 100,
                v -> cfg.shopRent.graceCycles = v.intValue(),
                "Cycles a frozen shop survives before auto-closing."));
        e.add(new BoolEntry(c, "Announce to owners", cfg.shopRent.announce, true,
                v -> cfg.shopRent.announce = v));

        // ===== Wealth Tax =====
        c = "Wealth Tax";
        e.add(new BoolEntry(c, "Wealth tax enabled", cfg.wealthTax.enabled, false,
                v -> cfg.wealthTax.enabled = v,
                "A periodic SINK that taxes only the wealthy.",
                "Off by default — it's an aggressive lever."));
        e.add(new NumberEntry(c, "Threshold", cfg.wealthTax.threshold, 100_000, 0, Long.MAX_VALUE / 2,
                v -> cfg.wealthTax.threshold = v,
                "Only the balance ABOVE this is taxed.", "Players below it pay nothing."));
        e.add(new SliderEntry(c, "Rate (of excess)", cfg.wealthTax.ratePercent, 1, 0, 100, "%",
                v -> cfg.wealthTax.ratePercent = v,
                "Percent of the above-threshold amount removed each cycle."));
        e.add(new NumberEntry(c, "Interval (minutes)", cfg.wealthTax.intervalMinutes, 1440, 1, 100_000,
                v -> cfg.wealthTax.intervalMinutes = v.intValue(),
                "How often the tax runs. 1440 = once a day."));
        e.add(new BoolEntry(c, "Announce to taxed players", cfg.wealthTax.announce, true,
                v -> cfg.wealthTax.announce = v));

        // ===== Cosmetics =====
        c = "Cosmetics";
        e.add(new BoolEntry(c, "Cosmetics shop enabled", cfg.cosmetic.enabled, true,
                v -> cfg.cosmetic.enabled = v,
                "The cosmetics shop NPC. Offers are datapack-driven — see",
                "data/notchcurrency/cosmetics/*.json. Buying is a coin SINK."));

        // ===== Waystone Fee =====
        c = "Waystone Fee";
        e.add(new BoolEntry(c, "Waystone fee enabled", cfg.waystone.enabled, false,
                v -> cfg.waystone.enabled = v,
                "Charge a coin SINK to teleport via a Waystone. Needs the Waystones mod installed."));
        e.add(new NumberEntry(c, "Teleport fee", cfg.waystone.fee, 50, 0, 1_000_000,
                v -> cfg.waystone.fee = v.intValue(),
                "Coins for a normal same-dimension waystone teleport."));
        e.add(new NumberEntry(c, "Dimensional fee", cfg.waystone.dimensionalFee, 200, 0, 1_000_000,
                v -> cfg.waystone.dimensionalFee = v.intValue(),
                "Coins for a teleport that crosses dimensions."));
        e.add(new BoolEntry(c, "Announce fee", cfg.waystone.announce, true,
                v -> cfg.waystone.announce = v,
                "Tell the player how much they paid after each teleport."));

        // ===== Audit Log =====
        c = "Audit Log";
        e.add(new BoolEntry(c, "Write audit file", cfg.ledger.fileLogEnabled, true,
                v -> cfg.ledger.fileLogEnabled = v,
                "Log every transaction to <world>/notchcurrency/ledger/."));
        e.add(new BoolEntry(c, "Discord webhook", cfg.ledger.webhookEnabled, false,
                v -> cfg.ledger.webhookEnabled = v,
                "Mirror admin-relevant events to a Discord webhook."));
        e.add(new StringEntry(c, "Webhook URL", cfg.ledger.webhookUrl, "", 256,
                v -> cfg.ledger.webhookUrl = v,
                "Paste a Discord channel webhook URL. Leave blank to disable."));
        e.add(new NumberEntry(c, "Webhook large-txn threshold", cfg.ledger.webhookLargeTxnThreshold,
                10_000, 0, Long.MAX_VALUE / 2,
                v -> cfg.ledger.webhookLargeTxnThreshold = v,
                "Transactions at or above this amount also post to the webhook.",
                "0 = never post by size (admin actions still post)."));

        // ===== Balloon Crates =====
        c = "Balloon Crates";
        e.add(new BoolEntry(c, "Announce spawns", cfg.balloon.announce, true,
                v -> cfg.balloon.announce = v));
        e.add(new NumberEntry(c, "Spawn count per wave", cfg.balloon.perDay, 3, 0, 100,
                v -> cfg.balloon.perDay = v.intValue(),
                "How many balloon crates spawn each wave."));
        e.add(new NumberEntry(c, "Center X", cfg.balloon.centerX, 0, -30_000_000, 30_000_000,
                v -> cfg.balloon.centerX = v.intValue()));
        e.add(new NumberEntry(c, "Center Y", cfg.balloon.centerY, 80, -64, 320,
                v -> cfg.balloon.centerY = v.intValue()));
        e.add(new NumberEntry(c, "Center Z", cfg.balloon.centerZ, 0, -30_000_000, 30_000_000,
                v -> cfg.balloon.centerZ = v.intValue()));
        e.add(new NumberEntry(c, "Spawn radius", cfg.balloon.radius, 25, 1, 10_000,
                v -> cfg.balloon.radius = v.intValue()));
        e.add(new NumberEntry(c, "Min Y", cfg.balloon.minY, 110, -64, 320,
                v -> cfg.balloon.minY = v.intValue()));
        e.add(new NumberEntry(c, "Max Y", cfg.balloon.maxY, 150, -64, 320,
                v -> cfg.balloon.maxY = v.intValue()));

        // ===== Golden Cache =====
        c = "Golden Cache";
        e.add(new BoolEntry(c, "Announce spawns", cfg.cache.announce, true,
                v -> cfg.cache.announce = v));
        e.add(new NumberEntry(c, "Cooldown (minutes)", cfg.cache.cooldownMinutes, 60, 0, 100_000,
                v -> cfg.cache.cooldownMinutes = v.intValue()));
        e.add(new NumberEntry(c, "Currency stacks (min)", cfg.cache.currencyStacksMin, 1, 0, 27,
                v -> cfg.cache.currencyStacksMin = v.intValue()));
        e.add(new NumberEntry(c, "Currency stacks (max)", cfg.cache.currencyStacksMax, 3, 0, 27,
                v -> cfg.cache.currencyStacksMax = v.intValue()));
        e.add(new NumberEntry(c, "Coins per stack (min)", cfg.cache.currencyPerStackMin, 100, 1, 1_000_000,
                v -> cfg.cache.currencyPerStackMin = v.intValue()));
        e.add(new NumberEntry(c, "Coins per stack (max)", cfg.cache.currencyPerStackMax, 250, 1, 1_000_000,
                v -> cfg.cache.currencyPerStackMax = v.intValue()));

        return e;
    }
}
