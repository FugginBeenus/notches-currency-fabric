package net.fugginbeenus.notchcurrency.config.ui;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.fugginbeenus.notchcurrency.auction.AuctionConfig;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.fugginbeenus.notchcurrency.crate.DailyCrateManager;
import net.fugginbeenus.notchcurrency.crate.GoldenCacheManager;
import net.fugginbeenus.notchcurrency.economy.ShopRent;
import net.fugginbeenus.notchcurrency.economy.WealthTax;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyManager;
import net.fugginbeenus.notchcurrency.economy.crate.CrateManager;
import net.fugginbeenus.notchcurrency.economy.raffle.RaffleManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Builds the Cloth Config settings screen for Notch Currency, opened from ModMenu.
 *
 * Edits the live {@link NotchConfig} instance and persists it on save. Economy knobs
 * (auction fees) re-apply immediately; world-bound knobs (balloon/cache) re-apply to the
 * static managers and fully take effect on the next world load.
 *
 * Note: on a multiplayer client this edits the *client's* local config file, which has no
 * effect on a remote server — server admins edit the server's config or use commands.
 */
public final class NotchConfigScreen {

    private NotchConfigScreen() {}

    public static Screen create(Screen parent) {
        NotchConfig cfg = NotchConfigIO.get();

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Notch Currency"))
                .setSavingRunnable(() -> {
                    NotchConfigIO.save(cfg);
                    AuctionConfig.apply(cfg);
                    DailyCrateManager.applyConfig(cfg);
                    GoldenCacheManager.applyConfig(cfg);
                    WealthTax.applyConfig(cfg);
                    ShopRent.applyConfig(cfg);
                    RaffleManager.applyConfig(cfg);
                    BountyManager.applyConfig(cfg);
                    CrateManager.applyConfig(cfg);
                    net.fugginbeenus.notchcurrency.economy.loan.LoanManager.applyConfig(cfg);
                    net.fugginbeenus.notchcurrency.economy.enchanter.EnchanterManager.applyConfig(cfg);
                    net.fugginbeenus.notchcurrency.economy.cosmetic.CosmeticManager.applyConfig(cfg);
                    net.fugginbeenus.notchcurrency.integration.WaystoneFeeHandler.applyConfig(cfg);
                    net.fugginbeenus.notchcurrency.economy.villager.VillagerCoinTrades.applyConfig(cfg);
                    // Rebuild the custom-currency pack so a name change takes effect on the next start.
                    net.fugginbeenus.notchcurrency.client.CurrencyPackGenerator.generate();
                });

        ConfigEntryBuilder eb = builder.entryBuilder();

        // ===== Economy =====
        ConfigCategory economy = builder.getOrCreateCategory(Text.literal("Economy"));
        economy.addEntry(eb.startIntField(Text.literal("Auction listing fee — flat (coins)"), cfg.auctionListingFeeFlat)
                .setDefaultValue(0).setMin(0)
                .setTooltip(Text.literal("Flat coin fee to create an auction listing, on top of the percent fee."),
                        Text.literal("A money SINK. 0 = no flat fee."))
                .setSaveConsumer(v -> cfg.auctionListingFeeFlat = v).build());
        economy.addEntry(eb.startIntField(Text.literal("Auction listing fee — percent of price"), cfg.auctionListingFeePercent)
                .setDefaultValue(0).setMin(0)
                .setTooltip(Text.literal("Listing fee as a percent of the asking price — scales with the listing."),
                        Text.literal("A money SINK. 0 = no percent fee."))
                .setSaveConsumer(v -> cfg.auctionListingFeePercent = v).build());
        economy.addEntry(eb.startIntField(Text.literal("Auction listing fee — max (coins)"), cfg.auctionListingFeeMax)
                .setDefaultValue(0).setMin(0)
                .setTooltip(Text.literal("Cap on the total listing fee. 0 = uncapped."))
                .setSaveConsumer(v -> cfg.auctionListingFeeMax = v).build());
        economy.addEntry(eb.startIntField(Text.literal("Auction sale tax (%)"), cfg.auctionSaleTaxPercent)
                .setDefaultValue(0).setMin(0).setMax(100)
                .setTooltip(Text.literal("Percent taken from the seller's payout on a sale."),
                        Text.literal("A money SINK. 0 = no tax."))
                .setSaveConsumer(v -> cfg.auctionSaleTaxPercent = v).build());
        economy.addEntry(eb.startIntField(Text.literal("Auction sale tax — max (coins)"), cfg.auctionSaleTaxMax)
                .setDefaultValue(0).setMin(0)
                .setTooltip(Text.literal("Cap on the sale tax per sale. 0 = uncapped."))
                .setSaveConsumer(v -> cfg.auctionSaleTaxMax = v).build());

        // ===== Currency (the maker — kept as the 2nd tab so it's easy to find) =====
        ConfigCategory currency = builder.getOrCreateCategory(Text.literal("Currency"));
        currency.addEntry(eb.startStrField(Text.literal("Coin name"), cfg.currency.itemName)
                .setDefaultValue("")
                .setTooltip(Text.literal("Rename the coin everywhere — the item AND messages/GUIs (\"You won 50 Rupees\")."),
                        Text.literal("Pick a name that reads well after a number. Blank keeps \"Notch Coin\"/\"coins\"."),
                        Text.literal("Drop coin.png in config/notchcurrency/currency/ to reskin the art."),
                        Text.literal("A resource pack is generated on save; enable it in Options → Resource Packs."))
                .setSaveConsumer(v -> cfg.currency.itemName = v).build());

        // ===== Wealth Tax =====
        ConfigCategory tax = builder.getOrCreateCategory(Text.literal("Wealth Tax"));
        tax.addEntry(eb.startBooleanToggle(Text.literal("Enabled"), cfg.wealthTax.enabled)
                .setDefaultValue(false)
                .setTooltip(Text.literal("A periodic SINK that taxes only the wealthy."),
                        Text.literal("Off by default — it's an aggressive lever."))
                .setSaveConsumer(v -> cfg.wealthTax.enabled = v).build());
        tax.addEntry(eb.startLongField(Text.literal("Threshold"), cfg.wealthTax.threshold)
                .setDefaultValue(100_000L).setMin(0L)
                .setTooltip(Text.literal("Only the balance ABOVE this is taxed."),
                        Text.literal("Players below it pay nothing."))
                .setSaveConsumer(v -> cfg.wealthTax.threshold = v).build());
        tax.addEntry(eb.startIntField(Text.literal("Rate (% of excess)"), cfg.wealthTax.ratePercent)
                .setDefaultValue(1).setMin(0).setMax(100)
                .setTooltip(Text.literal("Percent of the above-threshold amount removed each cycle."))
                .setSaveConsumer(v -> cfg.wealthTax.ratePercent = v).build());
        tax.addEntry(eb.startIntField(Text.literal("Interval (minutes)"), cfg.wealthTax.intervalMinutes)
                .setDefaultValue(1440).setMin(1)
                .setTooltip(Text.literal("How often the tax runs. 1440 = once a day."))
                .setSaveConsumer(v -> cfg.wealthTax.intervalMinutes = v).build());
        tax.addEntry(eb.startBooleanToggle(Text.literal("Announce to taxed players"), cfg.wealthTax.announce)
                .setDefaultValue(true)
                .setSaveConsumer(v -> cfg.wealthTax.announce = v).build());

        // ===== Shop Rent =====
        ConfigCategory rent = builder.getOrCreateCategory(Text.literal("Shop Rent"));
        rent.addEntry(eb.startBooleanToggle(Text.literal("Enabled"), cfg.shopRent.enabled)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Charge open player-shops rent each cycle (a SINK)."),
                        Text.literal("Paid from shop earnings first, then the owner's balance."))
                .setSaveConsumer(v -> cfg.shopRent.enabled = v).build());
        rent.addEntry(eb.startLongField(Text.literal("Base rent per shop"), cfg.shopRent.baseRent)
                .setDefaultValue(100L).setMin(0L)
                .setSaveConsumer(v -> cfg.shopRent.baseRent = v).build());
        rent.addEntry(eb.startLongField(Text.literal("Rent per listing"), cfg.shopRent.perListing)
                .setDefaultValue(0L).setMin(0L)
                .setTooltip(Text.literal("Extra rent per active listing, scaling with shop size."))
                .setSaveConsumer(v -> cfg.shopRent.perListing = v).build());
        rent.addEntry(eb.startIntField(Text.literal("Interval (minutes)"), cfg.shopRent.intervalMinutes)
                .setDefaultValue(1440).setMin(1)
                .setTooltip(Text.literal("How often rent is charged. 1440 = once a day."))
                .setSaveConsumer(v -> cfg.shopRent.intervalMinutes = v).build());
        rent.addEntry(eb.startIntField(Text.literal("Grace cycles before close"), cfg.shopRent.graceCycles)
                .setDefaultValue(3).setMin(0)
                .setTooltip(Text.literal("Cycles a frozen shop survives before auto-closing."))
                .setSaveConsumer(v -> cfg.shopRent.graceCycles = v).build());
        rent.addEntry(eb.startBooleanToggle(Text.literal("Announce to owners"), cfg.shopRent.announce)
                .setDefaultValue(true)
                .setSaveConsumer(v -> cfg.shopRent.announce = v).build());

        // ===== Raffle =====
        ConfigCategory raffle = builder.getOrCreateCategory(Text.literal("Raffle"));
        raffle.addEntry(eb.startBooleanToggle(Text.literal("Enabled"), cfg.raffle.enabled)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Players buy tickets into a shared pot; one weighted winner takes it all."),
                        Text.literal("The house cut on each ticket is a SINK; the pot is redistributed."))
                .setSaveConsumer(v -> cfg.raffle.enabled = v).build());
        raffle.addEntry(eb.startLongField(Text.literal("Ticket price"), cfg.raffle.ticketPrice)
                .setDefaultValue(100L).setMin(1L)
                .setSaveConsumer(v -> cfg.raffle.ticketPrice = v).build());
        raffle.addEntry(eb.startIntField(Text.literal("House cut (%)"), cfg.raffle.houseCutPercent)
                .setDefaultValue(20).setMin(0).setMax(100)
                .setTooltip(Text.literal("Percent of each ticket destroyed as the house cut (a SINK)."),
                        Text.literal("The rest funds the prize pot."))
                .setSaveConsumer(v -> cfg.raffle.houseCutPercent = v).build());
        raffle.addEntry(eb.startIntField(Text.literal("Max tickets per player"), cfg.raffle.maxTicketsPerPlayer)
                .setDefaultValue(0).setMin(0)
                .setTooltip(Text.literal("Cap per player per round. 0 = unlimited."))
                .setSaveConsumer(v -> cfg.raffle.maxTicketsPerPlayer = v).build());
        raffle.addEntry(eb.startIntField(Text.literal("Auto-draw interval (minutes)"), cfg.raffle.drawIntervalMinutes)
                .setDefaultValue(1440).setMin(0)
                .setTooltip(Text.literal("How often a winner is drawn automatically. 1440 = once a day."),
                        Text.literal("0 = manual only (/raffle draw)."))
                .setSaveConsumer(v -> cfg.raffle.drawIntervalMinutes = v).build());
        raffle.addEntry(eb.startBooleanToggle(Text.literal("Announce entries & winner"), cfg.raffle.announce)
                .setDefaultValue(true)
                .setSaveConsumer(v -> cfg.raffle.announce = v).build());
        raffle.addEntry(eb.startBooleanToggle(Text.literal("Allow ticket redemption"), cfg.raffle.redeemEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Let players turn in an old losing ticket for a few free entries."),
                        Text.literal("<5 entries → 1, <10 → 5, else 10; once per player per round."))
                .setSaveConsumer(v -> cfg.raffle.redeemEnabled = v).build());

        // ===== Bounty Board =====
        ConfigCategory bounty = builder.getOrCreateCategory(Text.literal("Bounty Board"));
        bounty.addEntry(eb.startBooleanToggle(Text.literal("Enabled"), cfg.bounty.enabled)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Auto-generate rotating kill/deliver bounties from datapack pools."),
                        Text.literal("A coin/item FAUCET. Rewards are created money."))
                .setSaveConsumer(v -> cfg.bounty.enabled = v).build());
        bounty.addEntry(eb.startIntField(Text.literal("Live bounties on the board"), cfg.bounty.activeCount)
                .setDefaultValue(5).setMin(0).setMax(20)
                .setTooltip(Text.literal("How many generated bounties are available at once."))
                .setSaveConsumer(v -> cfg.bounty.activeCount = v).build());
        bounty.addEntry(eb.startIntField(Text.literal("Bounties a player can take at once"), cfg.bounty.takeLimit)
                .setDefaultValue(3).setMin(1).setMax(5)
                .setTooltip(Text.literal("How many bounties a player may have in progress at a time."))
                .setSaveConsumer(v -> cfg.bounty.takeLimit = v).build());
        bounty.addEntry(eb.startIntField(Text.literal("Bounty duration (minutes)"), cfg.bounty.durationMinutes)
                .setDefaultValue(30).setMin(1)
                .setTooltip(Text.literal("How long each bounty lasts before it rotates out."))
                .setSaveConsumer(v -> cfg.bounty.durationMinutes = v).build());
        bounty.addEntry(eb.startIntField(Text.literal("Coin reward scale (%)"), cfg.bounty.rewardMultiplierPercent)
                .setDefaultValue(100).setMin(0).setMax(1000)
                .setTooltip(Text.literal("Scales every coin reward. 100 = unchanged, 50 = half, 0 = no coins."))
                .setSaveConsumer(v -> cfg.bounty.rewardMultiplierPercent = v).build());
        bounty.addEntry(eb.startLongField(Text.literal("Max coin reward (0 = no cap)"), cfg.bounty.maxCoinReward)
                .setDefaultValue(250L).setMin(0L)
                .setTooltip(Text.literal("Hard cap on the coins one bounty can pay (after scaling)."))
                .setSaveConsumer(v -> cfg.bounty.maxCoinReward = v).build());

        // ===== Crates =====
        ConfigCategory crate = builder.getOrCreateCategory(Text.literal("Crates"));
        crate.addEntry(eb.startBooleanToggle(Text.literal("Enabled"), cfg.crate.enabled)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Crate blocks opened with coin-bought keys."),
                        Text.literal("Buying keys is a SINK; loot is defined per-crate in datapacks."))
                .setSaveConsumer(v -> cfg.crate.enabled = v).build());
        crate.addEntry(eb.startLongField(Text.literal("Crate key price (coins)"), cfg.crate.keyPrice)
                .setDefaultValue(500L).setMin(0L)
                .setTooltip(Text.literal("Coin cost of one Crate Key. Higher-tier crates need more keys."))
                .setSaveConsumer(v -> cfg.crate.keyPrice = v).build());

        // ===== Loans =====
        ConfigCategory loan = builder.getOrCreateCategory(Text.literal("Loans"));
        loan.addEntry(eb.startBooleanToggle(Text.literal("Enabled"), cfg.loan.enabled)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Players borrow coins (created) up to a cap and repay with interest."),
                        Text.literal("Interest is a SINK. Off by default — it creates money."))
                .setSaveConsumer(v -> cfg.loan.enabled = v).build());
        loan.addEntry(eb.startLongField(Text.literal("Borrowing limit (max debt)"), cfg.loan.maxDebt)
                .setDefaultValue(10_000L).setMin(0L)
                .setSaveConsumer(v -> cfg.loan.maxDebt = v).build());
        loan.addEntry(eb.startIntField(Text.literal("Interest per cycle (%)"), cfg.loan.interestPercentPerCycle)
                .setDefaultValue(5).setMin(0).setMax(100)
                .setSaveConsumer(v -> cfg.loan.interestPercentPerCycle = v).build());
        loan.addEntry(eb.startIntField(Text.literal("Interest interval (minutes)"), cfg.loan.intervalMinutes)
                .setDefaultValue(1440).setMin(1)
                .setTooltip(Text.literal("How often interest is applied. 1440 = once a day."))
                .setSaveConsumer(v -> cfg.loan.intervalMinutes = v).build());
        loan.addEntry(eb.startBooleanToggle(Text.literal("Auto-collect from balance"), cfg.loan.autoCollect)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Each cycle, pull any spare balance toward the debt before charging interest."))
                .setSaveConsumer(v -> cfg.loan.autoCollect = v).build());
        loan.addEntry(eb.startIntField(Text.literal("Loan term (days)"), cfg.loan.termDays)
                .setDefaultValue(7).setMin(1)
                .setTooltip(Text.literal("How long a borrower has to repay before the loan goes overdue."))
                .setSaveConsumer(v -> cfg.loan.termDays = v).build());
        loan.addEntry(eb.startIntField(Text.literal("Late fee (%)"), cfg.loan.lateFeePercent)
                .setDefaultValue(10).setMin(0).setMax(100)
                .setTooltip(Text.literal("One-time penalty added to the debt the first cycle it is overdue."))
                .setSaveConsumer(v -> cfg.loan.lateFeePercent = v).build());
        loan.addEntry(eb.startIntField(Text.literal("Overdue interest (%)"), cfg.loan.overdueInterestPercent)
                .setDefaultValue(20).setMin(0).setMax(100)
                .setTooltip(Text.literal("Interest rate charged each cycle while a loan is past due (replaces the normal rate)."))
                .setSaveConsumer(v -> cfg.loan.overdueInterestPercent = v).build());

        // ===== Gambling =====
        ConfigCategory gambling = builder.getOrCreateCategory(Text.literal("Gambling"));
        gambling.addEntry(eb.startBooleanToggle(Text.literal("Enabled"), cfg.gambling.enabled)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Slots + coin flip. Bets are a SINK, winnings a FAUCET; the house edge nets a sink."))
                .setSaveConsumer(v -> cfg.gambling.enabled = v).build());
        gambling.addEntry(eb.startLongField(Text.literal("Minimum bet"), cfg.gambling.minBet)
                .setDefaultValue(10L).setMin(1L)
                .setSaveConsumer(v -> cfg.gambling.minBet = v).build());
        gambling.addEntry(eb.startLongField(Text.literal("Maximum bet"), cfg.gambling.maxBet)
                .setDefaultValue(1_000L).setMin(1L)
                .setSaveConsumer(v -> cfg.gambling.maxBet = v).build());
        gambling.addEntry(eb.startIntField(Text.literal("Slots house edge (%)"), cfg.gambling.slotsHouseEdgePercent)
                .setDefaultValue(22).setMin(0).setMax(90)
                .setTooltip(Text.literal("The cut the house keeps. 22 = players get ~78% back over time."),
                        Text.literal("Payouts auto-scale to hit this edge no matter the reel odds."))
                .setSaveConsumer(v -> cfg.gambling.slotsHouseEdgePercent = v).build());
        gambling.addEntry(eb.startIntField(Text.literal("Coin flip payout (%)"), cfg.gambling.coinFlipPayoutPercent)
                .setDefaultValue(195).setMin(100).setMax(300)
                .setTooltip(Text.literal("Percent of the bet returned on a win. 200 = fair double; below 200 is the house edge."))
                .setSaveConsumer(v -> cfg.gambling.coinFlipPayoutPercent = v).build());
        gambling.addEntry(eb.startIntField(Text.literal("Coin flip reveal (ticks)"), cfg.gambling.coinFlipRevealTicks)
                .setDefaultValue(30).setMin(0).setMax(200)
                .setTooltip(Text.literal("How long the coin-flip block spins before the result shows. 20 ticks = 1 second."))
                .setSaveConsumer(v -> cfg.gambling.coinFlipRevealTicks = v).build());

        // ===== Enchanter =====
        ConfigCategory enchanter = builder.getOrCreateCategory(Text.literal("Enchanter"));
        enchanter.addEntry(eb.startBooleanToggle(Text.literal("Enabled"), cfg.enchanter.enabled)
                .setDefaultValue(true)
                .setTooltip(Text.literal("The enchant/repair/extract service NPC. Every payment is a SINK."))
                .setSaveConsumer(v -> cfg.enchanter.enabled = v).build());
        enchanter.addEntry(eb.startIntField(Text.literal("Full repair cost"), cfg.enchanter.repairFullCost)
                .setDefaultValue(60).setMin(0)
                .setTooltip(Text.literal("Coins to fully repair a 100%-damaged item; scales down with less damage."))
                .setSaveConsumer(v -> cfg.enchanter.repairFullCost = v).build());
        enchanter.addEntry(eb.startIntField(Text.literal("Enchant price multiplier (%)"), cfg.enchanter.costMultiplierPercent)
                .setDefaultValue(100).setMin(1).setMax(1000)
                .setTooltip(Text.literal("Scales all enchantment purchase prices. 200 = double."))
                .setSaveConsumer(v -> cfg.enchanter.costMultiplierPercent = v).build());
        enchanter.addEntry(eb.startIntField(Text.literal("Extract cost"), cfg.enchanter.extractCost)
                .setDefaultValue(25).setMin(0)
                .setTooltip(Text.literal("Coins to pull one enchantment off an item onto a book."))
                .setSaveConsumer(v -> cfg.enchanter.extractCost = v).build());
        enchanter.addEntry(eb.startBooleanToggle(Text.literal("Sell treasure enchants"), cfg.enchanter.allowTreasure)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Whether Mending and other treasure enchantments can be bought (at double price)."))
                .setSaveConsumer(v -> cfg.enchanter.allowTreasure = v).build());

        // ===== Cosmetics =====
        ConfigCategory cosmetic = builder.getOrCreateCategory(Text.literal("Cosmetics"));
        cosmetic.addEntry(eb.startBooleanToggle(Text.literal("Enabled"), cfg.cosmetic.enabled)
                .setDefaultValue(true)
                .setTooltip(Text.literal("The cosmetics shop NPC. Offers are datapack-driven — see"),
                        Text.literal("data/notchcurrency/cosmetics/*.json. Buying is a coin SINK."))
                .setSaveConsumer(v -> cfg.cosmetic.enabled = v).build());

        // ===== Villager Trades =====
        ConfigCategory villager = builder.getOrCreateCategory(Text.literal("Villager Trades"));
        villager.addEntry(eb.startBooleanToggle(Text.literal("Enabled"), cfg.villagerTrades.enabled)
                .setDefaultValue(true)
                .setTooltip(Text.literal("When villagers roll new trades, some may be priced in coins instead of"),
                        Text.literal("emeralds — a rare find that makes currency spendable at villagers (a SINK)."))
                .setSaveConsumer(v -> cfg.villagerTrades.enabled = v).build());
        villager.addEntry(eb.startIntSlider(Text.literal("Conversion chance"), cfg.villagerTrades.chancePercent, 0, 100)
                .setDefaultValue(10)
                .setTextGetter(v -> Text.literal(v + "%"))
                .setTooltip(Text.literal("Chance per new emerald trade. High-value trades (8+ emeralds) get double."))
                .setSaveConsumer(v -> cfg.villagerTrades.chancePercent = v).build());
        villager.addEntry(eb.startIntField(Text.literal("Coins per emerald"), cfg.villagerTrades.coinsPerEmerald)
                .setDefaultValue(3).setMin(1)
                .setTooltip(Text.literal("Coin price for each emerald of the original trade."),
                        Text.literal("Trades too pricey to fit the two buy slots stay emerald-priced."))
                .setSaveConsumer(v -> cfg.villagerTrades.coinsPerEmerald = v).build());

        // ===== HUD =====
        ConfigCategory hud = builder.getOrCreateCategory(Text.literal("HUD"));
        hud.addEntry(eb.startSelector(Text.literal("Bounty tracker position"),
                        new String[]{"TOP_LEFT", "TOP_CENTER", "TOP_RIGHT",
                                "BOTTOM_LEFT", "BOTTOM_CENTER", "BOTTOM_RIGHT"},
                        cfg.hud.bountyTrackerCorner)
                .setDefaultValue("TOP_RIGHT")
                .setTooltip(Text.literal("Screen anchor for the bounty tracker pills."),
                        Text.literal("Client-side: on a server each player sets their own."))
                .setSaveConsumer(v -> cfg.hud.bountyTrackerCorner = v).build());
        hud.addEntry(eb.startIntField(Text.literal("Bounty tracker X offset"), cfg.hud.bountyTrackerX)
                .setDefaultValue(6)
                .setTooltip(Text.literal("Pixels inward from the anchor (left/right nudge on CENTER)."))
                .setSaveConsumer(v -> cfg.hud.bountyTrackerX = v).build());
        hud.addEntry(eb.startIntField(Text.literal("Bounty tracker Y offset"), cfg.hud.bountyTrackerY)
                .setDefaultValue(6)
                .setTooltip(Text.literal("Pixels inward from the top or bottom edge."))
                .setSaveConsumer(v -> cfg.hud.bountyTrackerY = v).build());
        hud.addEntry(eb.startIntSlider(Text.literal("Bounty tracker scale"), cfg.hud.bountyTrackerScale, 50, 200)
                .setDefaultValue(100)
                .setTextGetter(v -> Text.literal(v + "%"))
                .setTooltip(Text.literal("Shrink or grow the whole tracker."))
                .setSaveConsumer(v -> cfg.hud.bountyTrackerScale = v).build());
        hud.addEntry(eb.startIntSlider(Text.literal("Bounty tracker opacity"), cfg.hud.bountyTrackerOpacity, 0, 100)
                .setDefaultValue(85)
                .setTextGetter(v -> Text.literal(v + "%"))
                .setTooltip(Text.literal("Background darkness of the pills."))
                .setSaveConsumer(v -> cfg.hud.bountyTrackerOpacity = v).build());

        // ===== Waystone Fee =====
        ConfigCategory waystone = builder.getOrCreateCategory(Text.literal("Waystone Fee"));
        waystone.addEntry(eb.startBooleanToggle(Text.literal("Enabled"), cfg.waystone.enabled)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Charge a coin SINK to teleport via a Waystone. Needs the Waystones mod installed."))
                .setSaveConsumer(v -> cfg.waystone.enabled = v).build());
        waystone.addEntry(eb.startIntField(Text.literal("Teleport fee"), cfg.waystone.fee)
                .setDefaultValue(50).setMin(0)
                .setTooltip(Text.literal("Coins for a normal same-dimension waystone teleport."))
                .setSaveConsumer(v -> cfg.waystone.fee = v).build());
        waystone.addEntry(eb.startIntField(Text.literal("Dimensional fee"), cfg.waystone.dimensionalFee)
                .setDefaultValue(200).setMin(0)
                .setTooltip(Text.literal("Coins for a teleport that crosses dimensions."))
                .setSaveConsumer(v -> cfg.waystone.dimensionalFee = v).build());
        waystone.addEntry(eb.startBooleanToggle(Text.literal("Announce fee"), cfg.waystone.announce)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Tell the player how much they paid after each teleport."))
                .setSaveConsumer(v -> cfg.waystone.announce = v).build());

        // ===== Audit Log =====
        ConfigCategory ledger = builder.getOrCreateCategory(Text.literal("Audit Log"));
        ledger.addEntry(eb.startBooleanToggle(Text.literal("Write audit file"), cfg.ledger.fileLogEnabled)
                .setDefaultValue(true)
                .setTooltip(Text.literal("Log every transaction to <world>/notchcurrency/ledger/."))
                .setSaveConsumer(v -> cfg.ledger.fileLogEnabled = v).build());
        ledger.addEntry(eb.startBooleanToggle(Text.literal("Enable Discord webhook"), cfg.ledger.webhookEnabled)
                .setDefaultValue(false)
                .setTooltip(Text.literal("Mirror admin-relevant events to a Discord webhook."))
                .setSaveConsumer(v -> cfg.ledger.webhookEnabled = v).build());
        ledger.addEntry(eb.startStrField(Text.literal("Discord webhook URL"), cfg.ledger.webhookUrl)
                .setDefaultValue("")
                .setTooltip(Text.literal("Paste a Discord channel webhook URL. Leave blank to disable."))
                .setSaveConsumer(v -> cfg.ledger.webhookUrl = v).build());
        ledger.addEntry(eb.startLongField(Text.literal("Webhook large-txn threshold"), cfg.ledger.webhookLargeTxnThreshold)
                .setDefaultValue(10_000L).setMin(0L)
                .setTooltip(Text.literal("Transactions at or above this amount also post to the webhook."),
                        Text.literal("0 = never post by size (admin actions still post)."))
                .setSaveConsumer(v -> cfg.ledger.webhookLargeTxnThreshold = v).build());

        // ===== Balloon Crates =====
        ConfigCategory balloon = builder.getOrCreateCategory(Text.literal("Balloon Crates"));
        balloon.addEntry(eb.startBooleanToggle(Text.literal("Announce spawns"), cfg.balloon.announce)
                .setDefaultValue(true).setSaveConsumer(v -> cfg.balloon.announce = v).build());
        balloon.addEntry(eb.startIntField(Text.literal("Spawn count per wave"), cfg.balloon.perDay)
                .setDefaultValue(3).setMin(0)
                .setTooltip(Text.literal("How many balloon crates spawn each wave."))
                .setSaveConsumer(v -> cfg.balloon.perDay = v).build());
        balloon.addEntry(eb.startIntField(Text.literal("Center X"), cfg.balloon.centerX)
                .setDefaultValue(0).setSaveConsumer(v -> cfg.balloon.centerX = v).build());
        balloon.addEntry(eb.startIntField(Text.literal("Center Y"), cfg.balloon.centerY)
                .setDefaultValue(80).setSaveConsumer(v -> cfg.balloon.centerY = v).build());
        balloon.addEntry(eb.startIntField(Text.literal("Center Z"), cfg.balloon.centerZ)
                .setDefaultValue(0).setSaveConsumer(v -> cfg.balloon.centerZ = v).build());
        balloon.addEntry(eb.startIntField(Text.literal("Spawn radius"), cfg.balloon.radius)
                .setDefaultValue(25).setMin(1).setSaveConsumer(v -> cfg.balloon.radius = v).build());
        balloon.addEntry(eb.startIntField(Text.literal("Min Y"), cfg.balloon.minY)
                .setDefaultValue(110).setSaveConsumer(v -> cfg.balloon.minY = v).build());
        balloon.addEntry(eb.startIntField(Text.literal("Max Y"), cfg.balloon.maxY)
                .setDefaultValue(150).setSaveConsumer(v -> cfg.balloon.maxY = v).build());

        // ===== Golden Cache =====
        ConfigCategory cache = builder.getOrCreateCategory(Text.literal("Golden Cache"));
        cache.addEntry(eb.startBooleanToggle(Text.literal("Announce spawns"), cfg.cache.announce)
                .setDefaultValue(true).setSaveConsumer(v -> cfg.cache.announce = v).build());
        cache.addEntry(eb.startIntField(Text.literal("Cooldown (minutes)"), cfg.cache.cooldownMinutes)
                .setDefaultValue(60).setMin(0).setSaveConsumer(v -> cfg.cache.cooldownMinutes = v).build());
        cache.addEntry(eb.startIntField(Text.literal("Currency stacks (min)"), cfg.cache.currencyStacksMin)
                .setDefaultValue(1).setMin(0).setSaveConsumer(v -> cfg.cache.currencyStacksMin = v).build());
        cache.addEntry(eb.startIntField(Text.literal("Currency stacks (max)"), cfg.cache.currencyStacksMax)
                .setDefaultValue(3).setMin(0).setSaveConsumer(v -> cfg.cache.currencyStacksMax = v).build());
        cache.addEntry(eb.startIntField(Text.literal("Coins per stack (min)"), cfg.cache.currencyPerStackMin)
                .setDefaultValue(100).setMin(1).setSaveConsumer(v -> cfg.cache.currencyPerStackMin = v).build());
        cache.addEntry(eb.startIntField(Text.literal("Coins per stack (max)"), cfg.cache.currencyPerStackMax)
                .setDefaultValue(250).setMin(1).setSaveConsumer(v -> cfg.cache.currencyPerStackMax = v).build());

        return builder.build();
    }
}
