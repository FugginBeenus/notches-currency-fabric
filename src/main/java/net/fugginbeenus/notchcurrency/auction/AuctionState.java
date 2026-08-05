package net.fugginbeenus.notchcurrency.auction;

import net.fugginbeenus.notchcurrency.compat.StateData;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

public final class AuctionState extends PersistentState {

    // UUID → listing
    private final Map<UUID, AuctionListing> listings = new LinkedHashMap<>();

    // UUID (listingId) → pending winnings / returns / payouts
    private final Map<UUID, PendingWinnings> pendingWinnings = new LinkedHashMap<>();

    // UUID → worldTime tick when we should remind them about mailbox
    private final Map<UUID, Long> loginReminders = new HashMap<>();

    public AuctionState() {
    }

    // ----- PersistentState plumbing -----

    public static AuctionState get(ServerWorld world) {
        return get(world.getServer());
    }

    /**
     * Auctions are global: always stored in the overworld's persistent state, so a
     * listing is visible regardless of which dimension the seller or buyer is in.
     * (Balances and player shops are overworld-only too.)
     */
    public static AuctionState get(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        PersistentStateManager mgr = overworld.getPersistentStateManager();
        return StateData.getOrCreate(mgr, AuctionState::new, AuctionState::fromNbt, "notchcurrency_auctions");
    }

    public static AuctionState fromNbt(NbtCompound tag) {
        AuctionState s = new AuctionState();

        // Listings
        NbtList list = tag.getList("Listings", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound e = list.getCompound(i);
            AuctionListing l = AuctionListing.fromNbt(e);
            s.listings.put(l.id, l);
        }

        // Pending winnings
        NbtList pendingList = tag.getList("PendingWinnings", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < pendingList.size(); i++) {
            NbtCompound e = pendingList.getCompound(i);
            PendingWinnings pw = PendingWinnings.fromNbt(e);
            s.pendingWinnings.put(pw.listingId, pw);
        }

        return s;
    }

    public static final class PendingWinnings {
        public final UUID listingId;
        public final UUID sellerUuid;
        public final UUID winnerUuid;
        public final String sellerName;
        public final String winnerName;

        // mutable so we can partially claim
        public ItemStack stack;    // item owed to winner (or returned item)
        public long finalPrice;    // coins owed to seller (0 if none / already paid)

        public PendingWinnings(UUID listingId,
                               UUID sellerUuid,
                               UUID winnerUuid,
                               String sellerName,
                               String winnerName,
                               ItemStack stack,
                               long finalPrice) {
            this.listingId = listingId;
            this.sellerUuid = sellerUuid;
            this.winnerUuid = winnerUuid;
            this.sellerName = sellerName;
            this.winnerName = winnerName;
            this.stack = stack;
            this.finalPrice = finalPrice;
        }

        public NbtCompound toNbt() {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("Id", listingId);
            tag.putUuid("Seller", sellerUuid);
            tag.putUuid("Winner", winnerUuid);
            tag.putString("SellerName", sellerName);
            tag.putString("WinnerName", winnerName);
            tag.putLong("FinalPrice", finalPrice);
            tag.put("Stack", StackData.writeStack(stack));
            return tag;
        }

        public static PendingWinnings fromNbt(NbtCompound tag) {
            UUID id = tag.getUuid("Id");
            UUID seller = tag.getUuid("Seller");
            UUID winner = tag.getUuid("Winner");
            String sellerName = tag.getString("SellerName");
            String winnerName = tag.getString("WinnerName");
            long price = tag.getLong("FinalPrice");
            ItemStack stack = StackData.readStack(tag.getCompound("Stack"));
            return new PendingWinnings(id, seller, winner, sellerName, winnerName, stack, price);
        }

        public boolean isFullyClaimed() {
            return finalPrice <= 0L && stack.isEmpty();
        }
    }

    @Override
    //? if >=1.21 {
    /*public NbtCompound writeNbt(NbtCompound tag, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
    *///?} else {
    public NbtCompound writeNbt(NbtCompound tag) {
    //?}
        // Listings
        NbtList list = new NbtList();
        for (AuctionListing l : listings.values()) {
            list.add(l.toNbt());
        }
        tag.put("Listings", list);

        // Pending winnings
        NbtList pendingList = new NbtList();
        for (PendingWinnings pw : pendingWinnings.values()) {
            pendingList.add(pw.toNbt());
        }
        tag.put("PendingWinnings", pendingList);

        return tag;
    }

    // ----- Pending winnings helpers -----

    public PendingWinnings getPending(UUID id) {
        return pendingWinnings.get(id);
    }

    public void removePending(UUID id) {
        if (pendingWinnings.remove(id) != null) {
            markDirty();
        }
    }

    public void addPending(PendingWinnings pw) {
        pendingWinnings.put(pw.listingId, pw);
        markDirty();
    }

    // ----- Helper to strip auction NBT from items -----

    /**
     * Removes all auction-related NBT tags from an item so its normal tooltip returns.
     * Call this before giving purchased/returned items to players.
     */
    private static void stripAuctionTags(ItemStack stack) {
        if (StackData.hasData(stack)) {
            NbtCompound tag = StackData.editData(stack);
            tag.remove("nc_price");
            tag.remove("nc_seller");
            tag.remove("nc_created");
            tag.remove("nc_expires");
            tag.remove("nc_highest_bid");
            tag.remove("nc_highest_bidder");
            tag.remove("nc_listing_id");
            // If the tag is now empty, remove it entirely
            if (tag.isEmpty()) {
                StackData.clearData(stack);
            } else {
                StackData.commitData(stack, tag);
            }
        }
    }

    // ----- Login reminder helpers -----

    public void scheduleReminder(UUID playerUuid, long triggerTime) {
        loginReminders.put(playerUuid, triggerTime);
    }

    public void onPlayerJoin(ServerPlayerEntity player) {
        long now = player.getWorld().getTime();
        // 45 seconds = 45 * 20 = 900 ticks
        scheduleReminder(player.getUuid(), now + 900L);
    }

    /**
     * Called from NotchCurrency each tick to send login reminders
     * when a player has pending winnings.
     */
    public void checkLoginReminders(ServerWorld world) {
        if (loginReminders.isEmpty()) return;

        long now = world.getTime();
        Iterator<Map.Entry<UUID, Long>> it = loginReminders.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            UUID uuid = entry.getKey();
            long trigger = entry.getValue();

            if (now < trigger) {
                continue;
            }

            ServerPlayerEntity p = world.getServer().getPlayerManager().getPlayer(uuid);
            if (p == null) {
                // Player went offline again; drop this reminder
                it.remove();
                continue;
            }

            // Only notify if they actually have pending winnings/returns
            boolean hasPending = pendingWinnings.values().stream()
                    .anyMatch(pw -> pw.winnerUuid.equals(uuid) || pw.sellerUuid.equals(uuid));

            if (hasPending) {
                MutableText claim = Text.literal("[Claim All]")
                        .formatted(Formatting.GOLD, Formatting.BOLD)
                        .styled(style -> style
                                .withClickEvent(new ClickEvent(
                                        ClickEvent.Action.RUN_COMMAND,
                                        "/ah claim"
                                ))
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Text.literal("Click to claim all pending auction " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " & items")
                                )));

                p.sendMessage(
                        Text.literal("You have unclaimed auction rewards! ")
                                .formatted(Formatting.YELLOW)
                                .append(claim),
                        false
                );
            }

            it.remove(); // Fire once per login
        }
    }

    // ----- API used by commands / GUIs -----

    /** All current listings (backed by PersistentState). */
    public Collection<AuctionListing> getListings() {
        return Collections.unmodifiableCollection(listings.values());
    }

    public AuctionListing getListing(UUID id) {
        return listings.get(id);
    }

    // default addListing (3-day timed listing, can be overridden)
    public AuctionListing addListing(ServerWorld world,
                                     ServerPlayerEntity seller,
                                     ItemStack stack,
                                     long price,
                                     String category) {
        long defaultDurationTicks = 3L * 24L * 60L * 60L * 20L;  // 3 real days
        return addListing(world, seller, stack, price, category, defaultDurationTicks);
    }

    /**
     * Overload that supports a custom duration.
     * durationTicks <= 0  => no time limit (buy-now listing).
     */
    public AuctionListing addListing(ServerWorld world,
                                     ServerPlayerEntity seller,
                                     ItemStack stack,
                                     long price,
                                     String category,
                                     long durationTicks) {

        UUID id = UUID.randomUUID();
        long now = world.getTime();  // global tick time, never wraps

        long expires = (durationTicks <= 0L) ? 0L : now + durationTicks;

        // Tag the stack so client tooltip can read everything
        ItemStack listingStack = stack.copy();
        NbtCompound tag = StackData.editData(listingStack);
        tag.putLong("nc_price", price);
        tag.putString("nc_seller", seller.getName().getString());
        tag.putUuid("nc_listing_id", id);
        tag.putLong("nc_created", now);
        tag.putLong("nc_expires", expires);
        StackData.commitData(listingStack, tag);

        AuctionListing listing = new AuctionListing(
                id,
                seller.getUuid(),
                seller.getName().getString(),
                listingStack,
                price,
                now,
                expires,
                category
        );

        listings.put(id, listing);
        markDirty();
        return listing;
    }

    /**
     * Attempt to buy a listing at its fixed price.
     * - Fails if expired (for timed auctions).
     * - Fails if there are bids (use /ah bid instead).
     * Supports offline seller & full-inventory buyer via mailbox.
     */
    public void buyListing(ServerPlayerEntity buyer, UUID id) {
        AuctionListing listing = listings.get(id);
        if (listing == null) {
            buyer.sendMessage(Text.literal("No listing with that id."), false);
            return;
        }

        if (buyer.getUuid().equals(listing.sellerUuid)) {
            buyer.sendMessage(Text.literal("You cannot buy your own listing."), false);
            return;
        }

        // Expiry check: expiresGameTime <= 0 => no time limit (buy-now)
        ServerWorld world = buyer.getServerWorld();
        long now = world.getTime();  // use global tick time
        if (listing.expiresGameTime > 0L && now >= listing.expiresGameTime) {
            buyer.sendMessage(
                    Text.literal("This listing has expired.").formatted(Formatting.RED),
                    false
            );
            listings.remove(id);
            markDirty();
            return;
        }

        // If this is a timed auction with any bids, force /ah bid instead
        if (listing.expiresGameTime > 0L && listing.highestBid > 0L) {
            buyer.sendMessage(
                    Text.literal("This is a timed auction. Use /ah bid instead.")
                            .formatted(Formatting.RED),
                    false
            );
            return;
        }

        long bal = BalanceStore.get(buyer);
        long price = listing.price;

        if (bal < price) {
            buyer.sendMessage(Text.literal("You don't have enough " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + "."), false);
            return;
        }

        // Withdraw from buyer
        BalanceStore.subtract(buyer, price, TransactionReason.AUCTION, "auction buy-now");
        NotchPackets.sendBalance(buyer, BalanceStore.get(buyer));

        // Pay seller if online; if offline, store coins in mailbox
        ServerPlayerEntity sellerPlayer =
                buyer.getServer().getPlayerManager().getPlayer(listing.sellerUuid);

        boolean sellerPaidNow = false;

        // Apply sale tax on the seller's payout
        long gross = price;
        long net = AuctionConfig.applySaleTax(gross);
        long tax = gross - net;

        if (sellerPlayer != null) {
            if (net > 0) {
                BalanceStore.add(sellerPlayer, net, TransactionReason.AUCTION, "auction sale payout");
                NotchPackets.sendBalance(sellerPlayer, BalanceStore.get(sellerPlayer));
            }

            int count = listing.stack.getCount();
            Text itemName = listing.stack.getName().copy().formatted(Formatting.WHITE);
            String buyerName = buyer.getName().getString();

            MutableText sellerMsg = Text.literal("Sold ")
                    .formatted(Formatting.GREEN)
                    .append(Text.literal(count + "x ").formatted(Formatting.GREEN))
                    .append(itemName)
                    .append(Text.literal(" to " + buyerName + " for ").formatted(Formatting.GREEN))
                    .append(Text.literal(String.valueOf(gross) + " ").formatted(Formatting.GREEN))
                    .append(NotchCurrency.coinIcon());

            if (tax > 0) {
                sellerMsg.append(Text.literal(" (")
                        .append(Text.literal(String.valueOf(tax) + " ")
                                .formatted(Formatting.RED))
                        .append(NotchCurrency.coinIcon())
                        .append(Text.literal(" auction fee)").formatted(Formatting.RED)));
            }

            sellerMsg.append(Text.literal("!").formatted(Formatting.GREEN));

            sellerPlayer.sendMessage(sellerMsg, false);
            sellerPlayer.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
            sellerPaidNow = true;
        }

        // Give item; if inventory full, into mailbox
        ItemStack prize = listing.stack.copy();

        // Strip auction NBT tags so the item's normal tooltip returns
        stripAuctionTags(prize);

        ItemStack toGive = prize.copy();
        boolean inserted = buyer.getInventory().insertStack(toGive);
        if (!inserted && !toGive.isEmpty()) {
            // Both sides might be partially offline -> mailbox holds obligations.
            PendingWinnings pw = new PendingWinnings(
                    listing.id,
                    listing.sellerUuid,
                    buyer.getUuid(),
                    listing.sellerName,
                    buyer.getName().getString(),
                    prize.copy(),
                    sellerPaidNow ? 0L : net
            );
            addPending(pw);

            MutableText claim = Text.literal("[Claim Item]")
                    .formatted(Formatting.GREEN, Formatting.BOLD)
                    .styled(style -> style
                            .withClickEvent(
                                    new ClickEvent(
                                            ClickEvent.Action.RUN_COMMAND,
                                            "/ah claim " + listing.id.toString()
                                    )
                            )
                            .withHoverEvent(
                                    new HoverEvent(
                                            HoverEvent.Action.SHOW_TEXT,
                                            Text.literal("Click to claim your purchased item")
                                    )
                            ));

            buyer.sendMessage(
                    Text.literal("Your inventory was full. ")
                            .formatted(Formatting.YELLOW)
                            .append(claim)
                            .append(Text.literal(" to receive your item from the mailbox.")),
                    false
            );
        }

        listings.remove(id);
        markDirty();

        int count = listing.stack.getCount();
        Text itemName = listing.stack.getName().copy().formatted(Formatting.WHITE);

        Text buyerMsg = Text.literal("Purchased ")
                .formatted(Formatting.GREEN)
                .append(Text.literal(count + "x ").formatted(Formatting.GREEN))
                .append(itemName)
                .append(Text.literal(" for ").formatted(Formatting.GREEN))
                .append(Text.literal(String.valueOf(price) + " ").formatted(Formatting.GREEN))
                .append(NotchCurrency.coinIcon())
                .append(Text.literal("!").formatted(Formatting.GREEN));

        buyer.sendMessage(buyerMsg, false);
        buyer.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);

        // If seller was offline, their coins are in mailbox and will be claimable via /ah claim
        if (!sellerPaidNow && sellerPlayer == null) {
            // Nothing else to do; claimAll will handle payout.
        }
    }

    public void placeBid(ServerWorld world,
                         ServerPlayerEntity bidder,
                         UUID id,
                         long amount) {

        AuctionListing listing = listings.get(id);
        if (listing == null) {
            bidder.sendMessage(
                    Text.literal("No listing with that id.").formatted(Formatting.RED),
                    false
            );
            return;
        }

        long now = world.getTime();
        if (listing.expiresGameTime > 0L && now >= listing.expiresGameTime) {
            bidder.sendMessage(
                    Text.literal("This auction has expired.").formatted(Formatting.RED),
                    false
            );
            return;
        }

        if (listing.expiresGameTime <= 0L) {
            bidder.sendMessage(
                    Text.literal("This is a buy-now listing. Click to purchase.")
                            .formatted(Formatting.RED),
                    false
            );
            return;
        }

        if (bidder.getUuid().equals(listing.sellerUuid)) {
            bidder.sendMessage(
                    Text.literal("You can't bid on your own listing.")
                            .formatted(Formatting.RED),
                    false
            );
            return;
        }

        long baseline = listing.highestBid > 0 ? listing.highestBid : listing.price;
        long minBid = baseline + 1;

        if (amount < minBid) {
            bidder.sendMessage(
                    Text.literal("Minimum bid is " + minBid + " ")
                            .append(NotchCurrency.coinIcon())
                            .formatted(Formatting.RED),
                    false
            );
            return;
        }

        long bal = BalanceStore.get(bidder);
        if (bal < amount) {
            bidder.sendMessage(
                    Text.literal("Insufficient funds for that bid.")
                            .formatted(Formatting.RED),
                    false
            );
            return;
        }

        // Refund previous highest bidder (if any). Offline-safe: an offline bidder is still credited
        // by UUID, otherwise their reserved coins would be destroyed when outbid while away.
        if (listing.highestBidderUuid != null && listing.highestBid > 0) {
            ServerPlayerEntity prevTop = world.getServer().getPlayerManager()
                    .getPlayer(listing.highestBidderUuid);
            if (prevTop != null) {
                BalanceStore.add(prevTop, listing.highestBid, TransactionReason.AUCTION_REFUND, "outbid refund");
                NotchPackets.sendBalance(prevTop, BalanceStore.get(prevTop));
                prevTop.sendMessage(
                        Text.literal("Your bid was refunded on ")
                                .append(listing.stack.getName().copy())
                                .formatted(Formatting.YELLOW),
                        false
                );
                prevTop.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.8F);
            } else {
                BalanceStore.add(world.getServer(), listing.highestBidderUuid, listing.highestBid,
                        TransactionReason.AUCTION_REFUND, "outbid refund (offline)");
            }
        }

        // Reserve bidder's coins
        BalanceStore.subtract(bidder, amount, TransactionReason.AUCTION_BID, "bid reserve");
        NotchPackets.sendBalance(bidder, BalanceStore.get(bidder));

        listing.highestBid = amount;
        listing.highestBidderUuid = bidder.getUuid();
        listing.highestBidderName = bidder.getName().getString();

        // Sync bid info into the listing's stack NBT for client tooltip
        NbtCompound tag = StackData.editData(listing.stack);
        tag.putLong("nc_highest_bid", listing.highestBid);
        tag.putString("nc_highest_bidder", listing.highestBidderName);
        StackData.commitData(listing.stack, tag);

        bidder.sendMessage(
                Text.literal("You bid " + amount + " ")
                        .append(NotchCurrency.coinIcon())
                        .append(Text.literal(" on "))
                        .append(listing.stack.getName().copy())
                        .formatted(Formatting.GREEN),
                false
        );
        bidder.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0F, 1.2F);

        // Notify seller if online
        ServerPlayerEntity seller = world.getServer().getPlayerManager()
                .getPlayer(listing.sellerUuid);
        if (seller != null) {
            seller.sendMessage(
                    Text.literal(listing.highestBidderName + " bid " + amount + " ")
                            .append(NotchCurrency.coinIcon())
                            .append(Text.literal(" on your listing."))
                            .formatted(Formatting.YELLOW),
                    false
            );
            seller.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
        }

        markDirty();
    }

    public void removeListing(UUID id) {
        listings.remove(id);
        markDirty();
    }

    /**
     * Refund the reserved coins of a timed auction's highest bidder, if any. Bids escrow the coins
     * at bid time (see placeBid), so any path that removes a still-live listing with a standing bid
     * MUST call this or those coins are destroyed. Offline-safe: credits by UUID. Returns the amount
     * refunded (0 if there was no bid).
     */
    public long refundHighestBid(ServerWorld world, AuctionListing listing) {
        if (listing == null || listing.highestBidderUuid == null || listing.highestBid <= 0) {
            return 0L;
        }
        long amount = listing.highestBid;
        MinecraftServer server = world.getServer();
        ServerPlayerEntity bidder = server.getPlayerManager().getPlayer(listing.highestBidderUuid);
        if (bidder != null) {
            BalanceStore.add(bidder, amount, TransactionReason.AUCTION_REFUND, "auction cancelled, bid refunded");
            NotchPackets.sendBalance(bidder, BalanceStore.get(bidder));
            bidder.sendMessage(Text.literal("Your bid was refunded: ")
                    .append(listing.stack.getName().copy().formatted(Formatting.YELLOW))
                    .append(Text.literal(" was cancelled by the seller.").formatted(Formatting.YELLOW)), false);
        } else {
            BalanceStore.add(server, listing.highestBidderUuid, amount,
                    TransactionReason.AUCTION_REFUND, "auction cancelled, bid refunded (offline)");
        }
        // Clear so no later path double-refunds.
        listing.highestBid = 0L;
        listing.highestBidderUuid = null;
        listing.highestBidderName = null;
        return amount;
    }

    /**
     * Called once per world tick from NotchCurrency.
     * Handles:
     *  - Timed auction expiry (payout to seller + item to winner / mailbox)
     *  - Returning unsold items on expired auctions with no bids (mailbox-safe)
     *
     * Coins are already reserved at bid time and refunded on outbid,
     * so here we only CREDIT the seller for the winning bid, or store
     * those coins into mailbox if needed.
     */
    private int tickCounter = 0;
    private static final int TICK_INTERVAL = 20; // Check once per second instead of every tick

    public void tick(ServerWorld world) {
        // Only process every TICK_INTERVAL ticks (1 second) - auctions don't need tick-precise expiry
        tickCounter++;
        if (tickCounter % TICK_INTERVAL != 0) {
            return;
        }

        if (listings.isEmpty()) {
            return;
        }

        long now = world.getTime();
        Iterator<AuctionListing> it = listings.values().iterator();

        while (it.hasNext()) {
            AuctionListing listing = it.next();

            // Only timed auctions (expiresGameTime > 0) are handled here
            if (listing.expiresGameTime <= 0L) {
                continue;
            }
            if (now < listing.expiresGameTime) {
                continue;
            }

            // Auction is expired here
            ServerPlayerEntity seller = world.getServer().getPlayerManager()
                    .getPlayer(listing.sellerUuid);

            if (listing.highestBid > 0L && listing.highestBidderUuid != null) {
                // There is a winning bidder
                ServerPlayerEntity winner = world.getServer().getPlayerManager()
                        .getPlayer(listing.highestBidderUuid);

                long finalPrice = listing.highestBid;
                long gross = finalPrice;
                long net = AuctionConfig.applySaleTax(gross);
                long tax = gross - net;

                ItemStack prize = listing.stack.copy();
                stripAuctionTags(prize);
                Text itemName = listing.stack.getName().copy().formatted(Formatting.WHITE);

                boolean sellerPaidNow = false;

                // Pay seller if online; otherwise they'll claim coins later
                if (seller != null) {
                    if (net > 0) {
                        BalanceStore.add(seller, net, TransactionReason.AUCTION, "auction win payout");
                        NotchPackets.sendBalance(seller, BalanceStore.get(seller));
                    }

                    MutableText sellerMsg = Text.literal("Your auction for ")
                            .formatted(Formatting.GREEN)
                            .append(itemName)
                            .append(Text.literal(" was won by " +
                                    (listing.highestBidderName != null ? listing.highestBidderName : "Unknown") +
                                    " for ").formatted(Formatting.GREEN))
                            .append(NotchCurrency.coins(finalPrice));

                    if (tax > 0) {
                        sellerMsg.append(Text.literal(" (")
                                .append(Text.literal(String.valueOf(tax) + " ")
                                        .formatted(Formatting.RED))
                                .append(NotchCurrency.coinIcon())
                                .append(Text.literal(" auction fee)").formatted(Formatting.RED)));
                    }

                    sellerMsg.append(Text.literal("!").formatted(Formatting.GREEN));

                    seller.sendMessage(sellerMsg, false);
                    seller.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);
                    sellerPaidNow = true;
                }

                if (winner != null) {
                    ItemStack toGive = prize.copy();
                    boolean inserted = winner.getInventory().insertStack(toGive);

                    if (inserted || toGive.isEmpty()) {
                        // Fully delivered now
                        Text winMsg = Text.literal("You won the auction for ")
                                .formatted(Formatting.GREEN)
                                .append(itemName)
                                .append(Text.literal(" for ").formatted(Formatting.GREEN))
                                .append(NotchCurrency.coins(finalPrice))
                                .append(Text.literal("!").formatted(Formatting.GREEN));

                        winner.sendMessage(winMsg, false);
                        winner.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
                    } else {
                        // Inventory full – store pending & send clickable "Claim Item"
                        PendingWinnings pw = new PendingWinnings(
                                listing.id,
                                listing.sellerUuid,
                                listing.highestBidderUuid,
                                listing.sellerName,
                                listing.highestBidderName != null
                                        ? listing.highestBidderName
                                        : winner.getName().getString(),
                                prize,
                                sellerPaidNow ? 0L : net
                        );
                        addPending(pw);

                        MutableText claim = Text.literal("[Claim Item]")
                                .formatted(Formatting.GREEN, Formatting.BOLD)
                                .styled(style -> style
                                        .withClickEvent(
                                                new ClickEvent(
                                                        ClickEvent.Action.RUN_COMMAND,
                                                        "/ah claim " + listing.id.toString()
                                                )
                                        )
                                        .withHoverEvent(
                                                new HoverEvent(
                                                        HoverEvent.Action.SHOW_TEXT,
                                                        Text.literal("Click to claim your winnings")
                                                )
                                        ));

                        Text msg = Text.literal("Inventory full! ")
                                .formatted(Formatting.YELLOW)
                                .append(claim)
                                .append(Text.literal(" to receive ")
                                        .formatted(Formatting.YELLOW))
                                .append(prize.getName().copy().formatted(Formatting.WHITE))
                                .append(Text.literal(".").formatted(Formatting.YELLOW));

                        winner.sendMessage(msg, false);
                        winner.playSound(SoundEvents.BLOCK_NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
                    }
                } else {
                    // Winner offline: store pending so they can /ah claim later
                    PendingWinnings pw = new PendingWinnings(
                            listing.id,
                            listing.sellerUuid,
                            listing.highestBidderUuid,
                            listing.sellerName,
                            listing.highestBidderName != null
                                    ? listing.highestBidderName
                                    : "Unknown",
                            prize,
                            sellerPaidNow ? 0L : net
                    );
                    addPending(pw);
                }

                it.remove();
                markDirty();
            } else {
                // No bids: return item to seller or mailbox, then remove listing.
                if (seller != null) {
                    ItemStack toReturn = listing.stack.copy();
                    stripAuctionTags(toReturn);
                    boolean inserted = seller.getInventory().insertStack(toReturn);
                    if (!inserted && !toReturn.isEmpty()) {
                        // Inventory full -> mailbox instead of drop
                        PendingWinnings pw = new PendingWinnings(
                                listing.id,
                                listing.sellerUuid,
                                listing.sellerUuid, // winner = seller for returns
                                listing.sellerName,
                                listing.sellerName,
                                toReturn.copy(),
                                0L
                        );
                        addPending(pw);

                        MutableText claim = Text.literal("[Claim Item]")
                                .formatted(Formatting.GOLD, Formatting.BOLD)
                                .styled(style -> style
                                        .withClickEvent(
                                                new ClickEvent(
                                                        ClickEvent.Action.RUN_COMMAND,
                                                        "/ah claim " + listing.id.toString()
                                                )
                                        )
                                        .withHoverEvent(
                                                new HoverEvent(
                                                        HoverEvent.Action.SHOW_TEXT,
                                                        Text.literal("Click to claim your returned item")
                                                )
                                        ));

                        seller.sendMessage(
                                Text.literal("Your auction for ")
                                        .append(listing.stack.getName().copy())
                                        .append(Text.literal(" expired with no bids. "))
                                        .append(claim)
                                        .append(Text.literal(" to retrieve your item from the mailbox."))
                                        .formatted(Formatting.YELLOW),
                                false
                        );
                        seller.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.8F);

                    } else {
                        seller.sendMessage(
                                Text.literal("Your auction for ")
                                        .append(listing.stack.getName().copy())
                                        .append(Text.literal(" expired with no bids. Item returned."))
                                        .formatted(Formatting.YELLOW),
                                false
                        );
                        seller.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASS.value(), 1.0F, 0.8F);

                    }
                } else {
                    // Seller offline: put return item into mailbox
                    ItemStack returnStack = listing.stack.copy();
                    stripAuctionTags(returnStack);
                    PendingWinnings pw = new PendingWinnings(
                            listing.id,
                            listing.sellerUuid,
                            listing.sellerUuid,
                            listing.sellerName,
                            listing.sellerName,
                            returnStack,
                            0L
                    );
                    addPending(pw);
                }

                it.remove();
                markDirty();
            }
        }
    }

    /**
     * Claim all pending coins + items for this player.
     * Used by /ah claim (no args).
     */
    public void claimAll(ServerWorld world, ServerPlayerEntity player) {
        if (pendingWinnings.isEmpty()) {
            player.sendMessage(
                    Text.literal("You have no pending auction rewards.")
                            .formatted(Formatting.GRAY),
                    false
            );
            return;
        }

        UUID uuid = player.getUuid();
        boolean claimedSomething = false;

        Iterator<PendingWinnings> it = pendingWinnings.values().iterator();
        while (it.hasNext()) {
            PendingWinnings pw = it.next();

            // Claim coins as seller
            if (pw.sellerUuid.equals(uuid) && pw.finalPrice > 0L) {
                long amt = pw.finalPrice;
                BalanceStore.add(player, amt, TransactionReason.AUCTION, "claimed auction winnings");
                NotchPackets.sendBalance(player, BalanceStore.get(player));

                player.sendMessage(
                        Text.literal("Claimed ")
                                .append(Text.literal(String.valueOf(amt) + " ").formatted(Formatting.GOLD))
                                .append(NotchCurrency.coinIcon())
                                .append(Text.literal(" from auction winnings.").formatted(Formatting.GREEN)),
                        false
                );
                player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 1.0F, 1.0F);

                pw.finalPrice = 0L;
                claimedSomething = true;
            }

            // Claim item as winner (or seller in no-bid return)
            if (pw.winnerUuid.equals(uuid) && !pw.stack.isEmpty()) {
                ItemStack toGive = pw.stack.copy();
                stripAuctionTags(toGive); // Safety strip in case of old pending data
                boolean inserted = player.getInventory().insertStack(toGive);

                if (!inserted && !toGive.isEmpty()) {
                    // Still no room; keep in mailbox
                    player.sendMessage(
                            Text.literal("Your inventory is full. "
                                            + "Free up space and run /ah claim again.")
                                    .formatted(Formatting.RED),
                            false
                    );
                } else {
                    player.sendMessage(
                            Text.literal("Claimed auction item: ")
                                    .append(pw.stack.getName().copy())
                                    .formatted(Formatting.GREEN),
                            false
                    );
                    player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);

                    pw.stack = ItemStack.EMPTY;
                    claimedSomething = true;
                }
            }

            if (pw.isFullyClaimed()) {
                it.remove();
            }
        }

        if (!claimedSomething) {
            player.sendMessage(
                    Text.literal("You have no claimable " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " or items right now.")
                            .formatted(Formatting.GRAY),
                    false
            );
        }

        markDirty();
    }
}