package net.fugginbeenus.notchcurrency.auction;

import net.fugginbeenus.notchcurrency.compat.Chat;
import net.fugginbeenus.notchcurrency.compat.Msg;
import net.fugginbeenus.notchcurrency.compat.StateData;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.HashMap;

public final class AuctionState extends SavedData implements net.fugginbeenus.notchcurrency.compat.NbtState {

    // UUID → listing
    private final Map<UUID, AuctionListing> listings = new LinkedHashMap<>();

    // UUID (listingId) → pending winnings / returns / payouts
    private final Map<UUID, PendingWinnings> pendingWinnings = new LinkedHashMap<>();

    // UUID → worldTime tick when we should remind them about mailbox
    private final Map<UUID, Long> loginReminders = new HashMap<>();

    public AuctionState() {
    }

    // ----- SavedData plumbing -----

    public static AuctionState get(ServerLevel world) {
        return get(world.getServer());
    }

    public static AuctionState get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage mgr = overworld.getDataStorage();
        return StateData.getOrCreate(mgr, AuctionState::new, AuctionState::fromNbt, "notchcurrency_auctions");
    }

    public static AuctionState fromNbt(CompoundTag tag) {
        AuctionState s = new AuctionState();

        // Listings
        ListTag list = tag.getList("Listings", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            AuctionListing l = AuctionListing.fromNbt(e);
            s.listings.put(l.id, l);
        }

        // Pending winnings
        ListTag pendingList = tag.getList("PendingWinnings", Tag.TAG_COMPOUND);
        for (int i = 0; i < pendingList.size(); i++) {
            CompoundTag e = pendingList.getCompound(i);
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

        public CompoundTag toNbt() {
            CompoundTag tag = new CompoundTag();
            net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(tag, "Id", listingId);
            net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(tag, "Seller", sellerUuid);
            net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(tag, "Winner", winnerUuid);
            tag.putString("SellerName", sellerName);
            tag.putString("WinnerName", winnerName);
            tag.putLong("FinalPrice", finalPrice);
            tag.put("Stack", StackData.writeStack(stack));
            return tag;
        }

        public static PendingWinnings fromNbt(CompoundTag tag) {
            UUID id = net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(tag, "Id");
            UUID seller = net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(tag, "Seller");
            UUID winner = net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(tag, "Winner");
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

    // Only the older versions call this. 1.21.11 hands writeNbt to a codec instead, so there is
    // nothing on SavedData left to override there.
    //? if >=1.21.11 {
    /*
    *///?} elif >=1.21 {
    /*@Override
    public CompoundTag save(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        return writeNbt(tag);
    }
    *///?} else {
    @Override
    public CompoundTag save(CompoundTag tag) {
        return writeNbt(tag);
    }
    //?}

    @Override
    public CompoundTag writeNbt(CompoundTag tag) {
        // Listings
        ListTag list = new ListTag();
        for (AuctionListing l : listings.values()) {
            list.add(l.toNbt());
        }
        tag.put("Listings", list);

        // Pending winnings
        ListTag pendingList = new ListTag();
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
            setDirty();
        }
    }

    public void addPending(PendingWinnings pw) {
        pendingWinnings.put(pw.listingId, pw);
        setDirty();
    }

    // ----- Helper to strip auction NBT from items -----

    private static void stripAuctionTags(ItemStack stack) {
        if (StackData.hasData(stack)) {
            CompoundTag tag = StackData.editData(stack);
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

    public void onPlayerJoin(ServerPlayer player) {
        long now = player.level().getGameTime();
        // 45 seconds = 45 * 20 = 900 ticks
        scheduleReminder(player.getUUID(), now + 900L);
    }

    public void checkLoginReminders(ServerLevel world) {
        if (loginReminders.isEmpty()) return;

        long now = world.getGameTime();
        Iterator<Map.Entry<UUID, Long>> it = loginReminders.entrySet().iterator();

        while (it.hasNext()) {
            Map.Entry<UUID, Long> entry = it.next();
            UUID uuid = entry.getKey();
            long trigger = entry.getValue();

            if (now < trigger) {
                continue;
            }

            ServerPlayer p = world.getServer().getPlayerList().getPlayer(uuid);
            if (p == null) {
                // Player went offline again; drop this reminder
                it.remove();
                continue;
            }

            // Only notify if they actually have pending winnings/returns
            boolean hasPending = pendingWinnings.values().stream()
                    .anyMatch(pw -> pw.winnerUuid.equals(uuid) || pw.sellerUuid.equals(uuid));

            if (hasPending) {
                MutableComponent claim = Component.literal("[Claim All]")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                        .withStyle(style -> style
                                .withClickEvent(Chat.runCommand("/ah claim"))
                                .withHoverEvent(Chat.showText(Component.literal("Click to claim all pending auction " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " & items"))));

                Msg.chat(p, Component.literal("You have unclaimed auction rewards! ")
                                .withStyle(ChatFormatting.YELLOW)
                                .append(claim));
            }

            it.remove(); // Fire once per login
        }
    }

    // ----- API used by commands / GUIs -----

    public Collection<AuctionListing> getListings() {
        return Collections.unmodifiableCollection(listings.values());
    }

    public AuctionListing getListing(UUID id) {
        return listings.get(id);
    }

    // default addListing (3-day timed listing, can be overridden)
    public AuctionListing addListing(ServerLevel world,
                                     ServerPlayer seller,
                                     ItemStack stack,
                                     long price,
                                     String category) {
        long defaultDurationTicks = 3L * 24L * 60L * 60L * 20L;  // 3 real days
        return addListing(world, seller, stack, price, category, defaultDurationTicks);
    }

    public AuctionListing addListing(ServerLevel world,
                                     ServerPlayer seller,
                                     ItemStack stack,
                                     long price,
                                     String category,
                                     long durationTicks) {

        UUID id = UUID.randomUUID();
        long now = world.getGameTime();  // global tick time, never wraps

        long expires = (durationTicks <= 0L) ? 0L : now + durationTicks;

        // Tag the stack so client tooltip can read everything
        ItemStack listingStack = stack.copy();
        CompoundTag tag = StackData.editData(listingStack);
        tag.putLong("nc_price", price);
        tag.putString("nc_seller", seller.getName().getString());
        net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(tag, "nc_listing_id", id);
        tag.putLong("nc_created", now);
        tag.putLong("nc_expires", expires);
        StackData.commitData(listingStack, tag);

        AuctionListing listing = new AuctionListing(
                id,
                seller.getUUID(),
                seller.getName().getString(),
                listingStack,
                price,
                now,
                expires,
                category
        );

        listings.put(id, listing);
        setDirty();
        return listing;
    }

    public void buyListing(ServerPlayer buyer, UUID id) {
        AuctionListing listing = listings.get(id);
        if (listing == null) {
            Msg.chat(buyer, Component.literal("No listing with that id."));
            return;
        }

        if (buyer.getUUID().equals(listing.sellerUuid)) {
            Msg.chat(buyer, Component.literal("You cannot buy your own listing."));
            return;
        }

        // Expiry check: expiresGameTime <= 0 => no time limit (buy-now)
        ServerLevel world = buyer.serverLevel();
        long now = world.getGameTime();  // use global tick time
        if (listing.expiresGameTime > 0L && now >= listing.expiresGameTime) {
            Msg.chat(buyer, Component.literal("This listing has expired.").withStyle(ChatFormatting.RED));
            listings.remove(id);
            setDirty();
            return;
        }

        // If this is a timed auction with any bids, force /ah bid instead
        if (listing.expiresGameTime > 0L && listing.highestBid > 0L) {
            Msg.chat(buyer, Component.literal("This is a timed auction. Use /ah bid instead.")
                            .withStyle(ChatFormatting.RED));
            return;
        }

        long bal = BalanceStore.get(buyer);
        long price = listing.price;

        if (bal < price) {
            Msg.chat(buyer, Component.literal("You don't have enough " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + "."));
            return;
        }

        // Withdraw from buyer
        BalanceStore.subtract(buyer, price, TransactionReason.AUCTION, "auction buy-now");
        NotchPackets.sendBalance(buyer, BalanceStore.get(buyer));

        // Pay seller if online; if offline, store coins in mailbox
        ServerPlayer sellerPlayer =
                buyer.level().getServer().getPlayerList().getPlayer(listing.sellerUuid);

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
            Component itemName = listing.stack.getHoverName().copy().withStyle(ChatFormatting.WHITE);
            String buyerName = buyer.getName().getString();

            MutableComponent sellerMsg = Component.literal("Sold ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(Component.literal(count + "x ").withStyle(ChatFormatting.GREEN))
                    .append(itemName)
                    .append(Component.literal(" to " + buyerName + " for ").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(String.valueOf(gross) + " ").withStyle(ChatFormatting.GREEN))
                    .append(NotchCurrency.coinIcon());

            if (tax > 0) {
                sellerMsg.append(Component.literal(" (")
                        .append(Component.literal(String.valueOf(tax) + " ")
                                .withStyle(ChatFormatting.RED))
                        .append(NotchCurrency.coinIcon())
                        .append(Component.literal(" auction fee)").withStyle(ChatFormatting.RED)));
            }

            sellerMsg.append(Component.literal("!").withStyle(ChatFormatting.GREEN));

            Msg.chat(sellerPlayer, sellerMsg);
            sellerPlayer.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
            sellerPaidNow = true;
        }

        // Give item; if inventory full, into mailbox
        ItemStack prize = listing.stack.copy();

        // Strip auction NBT tags so the item's normal tooltip returns
        stripAuctionTags(prize);

        ItemStack toGive = prize.copy();
        boolean inserted = buyer.getInventory().add(toGive);
        if (!inserted && !toGive.isEmpty()) {
            // Both sides might be partially offline -> mailbox holds obligations.
            PendingWinnings pw = new PendingWinnings(
                    listing.id,
                    listing.sellerUuid,
                    buyer.getUUID(),
                    listing.sellerName,
                    buyer.getName().getString(),
                    prize.copy(),
                    sellerPaidNow ? 0L : net
            );
            addPending(pw);

            MutableComponent claim = Component.literal("[Claim Item]")
                    .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                    .withStyle(style -> style
                            .withClickEvent(
                                    Chat.runCommand("/ah claim " + listing.id.toString())
                            )
                            .withHoverEvent(
                                    Chat.showText(Component.literal("Click to claim your purchased item"))
                            ));

            Msg.chat(buyer, Component.literal("Your inventory was full. ")
                            .withStyle(ChatFormatting.YELLOW)
                            .append(claim)
                            .append(Component.literal(" to receive your item from the mailbox.")));
        }

        listings.remove(id);
        setDirty();

        int count = listing.stack.getCount();
        Component itemName = listing.stack.getHoverName().copy().withStyle(ChatFormatting.WHITE);

        Component buyerMsg = Component.literal("Purchased ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(count + "x ").withStyle(ChatFormatting.GREEN))
                .append(itemName)
                .append(Component.literal(" for ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(String.valueOf(price) + " ").withStyle(ChatFormatting.GREEN))
                .append(NotchCurrency.coinIcon())
                .append(Component.literal("!").withStyle(ChatFormatting.GREEN));

        Msg.chat(buyer, buyerMsg);
        buyer.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.0F);

        // If seller was offline, their coins are in mailbox and will be claimable via /ah claim
        if (!sellerPaidNow && sellerPlayer == null) {
            // Nothing else to do; claimAll will handle payout.
        }
    }

    public void placeBid(ServerLevel world,
                         ServerPlayer bidder,
                         UUID id,
                         long amount) {

        AuctionListing listing = listings.get(id);
        if (listing == null) {
            Msg.chat(bidder, Component.literal("No listing with that id.").withStyle(ChatFormatting.RED));
            return;
        }

        long now = world.getGameTime();
        if (listing.expiresGameTime > 0L && now >= listing.expiresGameTime) {
            Msg.chat(bidder, Component.literal("This auction has expired.").withStyle(ChatFormatting.RED));
            return;
        }

        if (listing.expiresGameTime <= 0L) {
            Msg.chat(bidder, Component.literal("This is a buy-now listing. Click to purchase.")
                            .withStyle(ChatFormatting.RED));
            return;
        }

        if (bidder.getUUID().equals(listing.sellerUuid)) {
            Msg.chat(bidder, Component.literal("You can't bid on your own listing.")
                            .withStyle(ChatFormatting.RED));
            return;
        }

        long baseline = listing.highestBid > 0 ? listing.highestBid : listing.price;
        long minBid = baseline + 1;

        if (amount < minBid) {
            Msg.chat(bidder, Component.literal("Minimum bid is " + minBid + " ")
                            .append(NotchCurrency.coinIcon())
                            .withStyle(ChatFormatting.RED));
            return;
        }

        long bal = BalanceStore.get(bidder);
        if (bal < amount) {
            Msg.chat(bidder, Component.literal("Insufficient funds for that bid.")
                            .withStyle(ChatFormatting.RED));
            return;
        }

        // Refund previous highest bidder (if any). Offline-safe: an offline bidder is still credited
        // by UUID, otherwise their reserved coins would be destroyed when outbid while away.
        if (listing.highestBidderUuid != null && listing.highestBid > 0) {
            ServerPlayer prevTop = world.getServer().getPlayerList()
                    .getPlayer(listing.highestBidderUuid);
            if (prevTop != null) {
                BalanceStore.add(prevTop, listing.highestBid, TransactionReason.AUCTION_REFUND, "outbid refund");
                NotchPackets.sendBalance(prevTop, BalanceStore.get(prevTop));
                Msg.chat(prevTop, Component.literal("Your bid was refunded on ")
                                .append(listing.stack.getHoverName().copy())
                                .withStyle(ChatFormatting.YELLOW));
                prevTop.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1.0F, 0.8F);
            } else {
                BalanceStore.add(world.getServer(), listing.highestBidderUuid, listing.highestBid,
                        TransactionReason.AUCTION_REFUND, "outbid refund (offline)");
            }
        }

        // Reserve bidder's coins
        BalanceStore.subtract(bidder, amount, TransactionReason.AUCTION_BID, "bid reserve");
        NotchPackets.sendBalance(bidder, BalanceStore.get(bidder));

        listing.highestBid = amount;
        listing.highestBidderUuid = bidder.getUUID();
        listing.highestBidderName = bidder.getName().getString();

        // Sync bid info into the listing's stack NBT for client tooltip
        CompoundTag tag = StackData.editData(listing.stack);
        tag.putLong("nc_highest_bid", listing.highestBid);
        tag.putString("nc_highest_bidder", listing.highestBidderName);
        StackData.commitData(listing.stack, tag);

        Msg.chat(bidder, Component.literal("You bid " + amount + " ")
                        .append(NotchCurrency.coinIcon())
                        .append(Component.literal(" on "))
                        .append(listing.stack.getHoverName().copy())
                        .withStyle(ChatFormatting.GREEN));
        bidder.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 1.2F);

        // Notify seller if online
        ServerPlayer seller = world.getServer().getPlayerList()
                .getPlayer(listing.sellerUuid);
        if (seller != null) {
            Msg.chat(seller, Component.literal(listing.highestBidderName + " bid " + amount + " ")
                            .append(NotchCurrency.coinIcon())
                            .append(Component.literal(" on your listing."))
                            .withStyle(ChatFormatting.YELLOW));
            seller.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
        }

        setDirty();
    }

    public void removeListing(UUID id) {
        listings.remove(id);
        setDirty();
    }

    public long refundHighestBid(ServerLevel world, AuctionListing listing) {
        if (listing == null || listing.highestBidderUuid == null || listing.highestBid <= 0) {
            return 0L;
        }
        long amount = listing.highestBid;
        MinecraftServer server = world.getServer();
        ServerPlayer bidder = server.getPlayerList().getPlayer(listing.highestBidderUuid);
        if (bidder != null) {
            BalanceStore.add(bidder, amount, TransactionReason.AUCTION_REFUND, "auction cancelled - bid refunded");
            NotchPackets.sendBalance(bidder, BalanceStore.get(bidder));
            Msg.chat(bidder, Component.literal("Your bid was refunded - ")
                    .append(listing.stack.getHoverName().copy().withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" was cancelled by the seller.").withStyle(ChatFormatting.YELLOW)));
        } else {
            BalanceStore.add(server, listing.highestBidderUuid, amount,
                    TransactionReason.AUCTION_REFUND, "auction cancelled - bid refunded (offline)");
        }
        // Clear so no later path double-refunds.
        listing.highestBid = 0L;
        listing.highestBidderUuid = null;
        listing.highestBidderName = null;
        return amount;
    }

    private int tickCounter = 0;
    private static final int TICK_INTERVAL = 20; // Check once per second instead of every tick

    public void tick(ServerLevel world) {
        // Only process every TICK_INTERVAL ticks (1 second) - auctions don't need tick-precise expiry
        tickCounter++;
        if (tickCounter % TICK_INTERVAL != 0) {
            return;
        }

        if (listings.isEmpty()) {
            return;
        }

        long now = world.getGameTime();
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
            ServerPlayer seller = world.getServer().getPlayerList()
                    .getPlayer(listing.sellerUuid);

            if (listing.highestBid > 0L && listing.highestBidderUuid != null) {
                // There is a winning bidder
                ServerPlayer winner = world.getServer().getPlayerList()
                        .getPlayer(listing.highestBidderUuid);

                long finalPrice = listing.highestBid;
                long gross = finalPrice;
                long net = AuctionConfig.applySaleTax(gross);
                long tax = gross - net;

                ItemStack prize = listing.stack.copy();
                stripAuctionTags(prize);
                Component itemName = listing.stack.getHoverName().copy().withStyle(ChatFormatting.WHITE);

                boolean sellerPaidNow = false;

                // Pay seller if online; otherwise they'll claim coins later
                if (seller != null) {
                    if (net > 0) {
                        BalanceStore.add(seller, net, TransactionReason.AUCTION, "auction win payout");
                        NotchPackets.sendBalance(seller, BalanceStore.get(seller));
                    }

                    MutableComponent sellerMsg = Component.literal("Your auction for ")
                            .withStyle(ChatFormatting.GREEN)
                            .append(itemName)
                            .append(Component.literal(" was won by " +
                                    (listing.highestBidderName != null ? listing.highestBidderName : "Unknown") +
                                    " for ").withStyle(ChatFormatting.GREEN))
                            .append(NotchCurrency.coins(finalPrice));

                    if (tax > 0) {
                        sellerMsg.append(Component.literal(" (")
                                .append(Component.literal(String.valueOf(tax) + " ")
                                        .withStyle(ChatFormatting.RED))
                                .append(NotchCurrency.coinIcon())
                                .append(Component.literal(" auction fee)").withStyle(ChatFormatting.RED)));
                    }

                    sellerMsg.append(Component.literal("!").withStyle(ChatFormatting.GREEN));

                    Msg.chat(seller, sellerMsg);
                    seller.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);
                    sellerPaidNow = true;
                }

                if (winner != null) {
                    ItemStack toGive = prize.copy();
                    boolean inserted = winner.getInventory().add(toGive);

                    if (inserted || toGive.isEmpty()) {
                        // Fully delivered now
                        Component winMsg = Component.literal("You won the auction for ")
                                .withStyle(ChatFormatting.GREEN)
                                .append(itemName)
                                .append(Component.literal(" for ").withStyle(ChatFormatting.GREEN))
                                .append(NotchCurrency.coins(finalPrice))
                                .append(Component.literal("!").withStyle(ChatFormatting.GREEN));

                        Msg.chat(winner, winMsg);
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

                        MutableComponent claim = Component.literal("[Claim Item]")
                                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                                .withStyle(style -> style
                                        .withClickEvent(
                                                Chat.runCommand("/ah claim " + listing.id.toString())
                                        )
                                        .withHoverEvent(
                                                Chat.showText(Component.literal("Click to claim your winnings"))
                                        ));

                        Component msg = Component.literal("Inventory full! ")
                                .withStyle(ChatFormatting.YELLOW)
                                .append(claim)
                                .append(Component.literal(" to receive ")
                                        .withStyle(ChatFormatting.YELLOW))
                                .append(prize.getHoverName().copy().withStyle(ChatFormatting.WHITE))
                                .append(Component.literal(".").withStyle(ChatFormatting.YELLOW));

                        Msg.chat(winner, msg);
                        winner.playSound(SoundEvents.NOTE_BLOCK_PLING.value(), 1.0F, 1.0F);
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
                setDirty();
            } else {
                // No bids: return item to seller or mailbox, then remove listing.
                if (seller != null) {
                    ItemStack toReturn = listing.stack.copy();
                    stripAuctionTags(toReturn);
                    boolean inserted = seller.getInventory().add(toReturn);
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

                        MutableComponent claim = Component.literal("[Claim Item]")
                                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                                .withStyle(style -> style
                                        .withClickEvent(
                                                Chat.runCommand("/ah claim " + listing.id.toString())
                                        )
                                        .withHoverEvent(
                                                Chat.showText(Component.literal("Click to claim your returned item"))
                                        ));

                        Msg.chat(seller, Component.literal("Your auction for ")
                                        .append(listing.stack.getHoverName().copy())
                                        .append(Component.literal(" expired with no bids. "))
                                        .append(claim)
                                        .append(Component.literal(" to retrieve your item from the mailbox."))
                                        .withStyle(ChatFormatting.YELLOW));
                        seller.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1.0F, 0.8F);

                    } else {
                        Msg.chat(seller, Component.literal("Your auction for ")
                                        .append(listing.stack.getHoverName().copy())
                                        .append(Component.literal(" expired with no bids. Item returned."))
                                        .withStyle(ChatFormatting.YELLOW));
                        seller.playSound(SoundEvents.NOTE_BLOCK_BASS.value(), 1.0F, 0.8F);

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
                setDirty();
            }
        }
    }

    public void claimAll(ServerLevel world, ServerPlayer player) {
        if (pendingWinnings.isEmpty()) {
            Msg.chat(player, Component.literal("You have no pending auction rewards.")
                            .withStyle(ChatFormatting.GRAY));
            return;
        }

        UUID uuid = player.getUUID();
        boolean claimedSomething = false;

        Iterator<PendingWinnings> it = pendingWinnings.values().iterator();
        while (it.hasNext()) {
            PendingWinnings pw = it.next();

            // Claim coins as seller
            if (pw.sellerUuid.equals(uuid) && pw.finalPrice > 0L) {
                long amt = pw.finalPrice;
                BalanceStore.add(player, amt, TransactionReason.AUCTION, "claimed auction winnings");
                NotchPackets.sendBalance(player, BalanceStore.get(player));

                Msg.chat(player, Component.literal("Claimed ")
                                .append(Component.literal(String.valueOf(amt) + " ").withStyle(ChatFormatting.GOLD))
                                .append(NotchCurrency.coinIcon())
                                .append(Component.literal(" from auction winnings.").withStyle(ChatFormatting.GREEN)));
                player.playSound(SoundEvents.PLAYER_LEVELUP, 1.0F, 1.0F);

                pw.finalPrice = 0L;
                claimedSomething = true;
            }

            // Claim item as winner (or seller in no-bid return)
            if (pw.winnerUuid.equals(uuid) && !pw.stack.isEmpty()) {
                ItemStack toGive = pw.stack.copy();
                stripAuctionTags(toGive); // Safety strip in case of old pending data
                boolean inserted = player.getInventory().add(toGive);

                if (!inserted && !toGive.isEmpty()) {
                    // Still no room; keep in mailbox
                    Msg.chat(player, Component.literal("Your inventory is full. "
                                            + "Free up space and run /ah claim again.")
                                    .withStyle(ChatFormatting.RED));
                } else {
                    Msg.chat(player, Component.literal("Claimed auction item: ")
                                    .append(pw.stack.getHoverName().copy())
                                    .withStyle(ChatFormatting.GREEN));
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
            Msg.chat(player, Component.literal("You have no claimable " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " or items right now.")
                            .withStyle(ChatFormatting.GRAY));
        }

        setDirty();
    }
}
