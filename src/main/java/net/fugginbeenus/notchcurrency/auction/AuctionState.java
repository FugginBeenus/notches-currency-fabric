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

    private final Map<UUID, AuctionListing> listings = new LinkedHashMap<>();
    private final Map<UUID, PendingWinnings> pendingWinnings = new LinkedHashMap<>();
    private final Map<UUID, Long> loginReminders = new HashMap<>();

    public AuctionState() {
    }

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

        ListTag list = tag.getList("Listings", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompound(i);
            AuctionListing l = AuctionListing.fromNbt(e);
            s.listings.put(l.id, l);
        }

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
        public ItemStack stack;
        public long finalPrice;

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
        ListTag list = new ListTag();
        for (AuctionListing l : listings.values()) {
            list.add(l.toNbt());
        }
        tag.put("Listings", list);

        ListTag pendingList = new ListTag();
        for (PendingWinnings pw : pendingWinnings.values()) {
            pendingList.add(pw.toNbt());
        }
        tag.put("PendingWinnings", pendingList);

        return tag;
    }

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
            if (tag.isEmpty()) {
                StackData.clearData(stack);
            } else {
                StackData.commitData(stack, tag);
            }
        }
    }

    public void scheduleReminder(UUID playerUuid, long triggerTime) {
        loginReminders.put(playerUuid, triggerTime);
    }

    public void onPlayerJoin(ServerPlayer player) {
        long now = player.level().getGameTime();
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
                it.remove();
                continue;
            }

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

            it.remove();
        }
    }

    public Collection<AuctionListing> getListings() {
        return Collections.unmodifiableCollection(listings.values());
    }

    public AuctionListing getListing(UUID id) {
        return listings.get(id);
    }

    public AuctionListing addListing(ServerLevel world,
                                     ServerPlayer seller,
                                     ItemStack stack,
                                     long price,
                                     String category) {
        long defaultDurationTicks = 3L * 24L * 60L * 60L * 20L;
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

        ServerLevel world = buyer.serverLevel();
        long now = world.getGameTime();
        if (listing.expiresGameTime > 0L && now >= listing.expiresGameTime) {
            Msg.chat(buyer, Component.literal("This listing has expired.").withStyle(ChatFormatting.RED));
            listings.remove(id);
            setDirty();
            return;
        }

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

        BalanceStore.subtract(buyer, price, TransactionReason.AUCTION, "auction buy-now");
        NotchPackets.sendBalance(buyer, BalanceStore.get(buyer));
        ServerPlayer sellerPlayer =
                buyer.level().getServer().getPlayerList().getPlayer(listing.sellerUuid);

        boolean sellerPaidNow = false;
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

        ItemStack prize = listing.stack.copy();
        stripAuctionTags(prize);

        ItemStack toGive = prize.copy();
        boolean inserted = buyer.getInventory().add(toGive);
        if (!inserted && !toGive.isEmpty()) {
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

        if (!sellerPaidNow && sellerPlayer == null) {
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

        BalanceStore.subtract(bidder, amount, TransactionReason.AUCTION_BID, "bid reserve");
        NotchPackets.sendBalance(bidder, BalanceStore.get(bidder));

        listing.highestBid = amount;
        listing.highestBidderUuid = bidder.getUUID();
        listing.highestBidderName = bidder.getName().getString();

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
        listing.highestBid = 0L;
        listing.highestBidderUuid = null;
        listing.highestBidderName = null;
        return amount;
    }

    private int tickCounter = 0;
    private static final int TICK_INTERVAL = 20;

    public void tick(ServerLevel world) {
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

            if (listing.expiresGameTime <= 0L) {
                continue;
            }
            if (now < listing.expiresGameTime) {
                continue;
            }

            ServerPlayer seller = world.getServer().getPlayerList()
                    .getPlayer(listing.sellerUuid);

            if (listing.highestBid > 0L && listing.highestBidderUuid != null) {
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
                        Component winMsg = Component.literal("You won the auction for ")
                                .withStyle(ChatFormatting.GREEN)
                                .append(itemName)
                                .append(Component.literal(" for ").withStyle(ChatFormatting.GREEN))
                                .append(NotchCurrency.coins(finalPrice))
                                .append(Component.literal("!").withStyle(ChatFormatting.GREEN));

                        Msg.chat(winner, winMsg);
                        winner.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 1.0F, 1.0F);
                    } else {
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
                if (seller != null) {
                    ItemStack toReturn = listing.stack.copy();
                    stripAuctionTags(toReturn);
                    boolean inserted = seller.getInventory().add(toReturn);
                    if (!inserted && !toReturn.isEmpty()) {
                        PendingWinnings pw = new PendingWinnings(
                                listing.id,
                                listing.sellerUuid,
                                listing.sellerUuid,
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

    public int drainIntoMail(net.minecraft.server.MinecraftServer server) {
        if (pendingWinnings.isEmpty()) return 0;
        int posted = 0;
        Iterator<PendingWinnings> it = pendingWinnings.values().iterator();
        while (it.hasNext()) {
            PendingWinnings pw = it.next();
            boolean allPosted = true;

            if (pw.finalPrice > 0L) {
                var coins = net.fugginbeenus.notchcurrency.mail.MailItem.payout(
                        "Auction House", "Sale of " + pw.stack.getHoverName().getString(), pw.finalPrice);
                if (net.fugginbeenus.notchcurrency.mail.MailManager.post(server, pw.sellerUuid, coins)) {
                    pw.finalPrice = 0L;
                    posted++;
                } else {
                    allPosted = false;
                }
            }
            if (!pw.stack.isEmpty()) {
                var parcel = net.fugginbeenus.notchcurrency.mail.MailItem.parcel(
                        "Auction House", "You won this listing", pw.stack);
                if (net.fugginbeenus.notchcurrency.mail.MailManager.post(server, pw.winnerUuid, parcel)) {
                    pw.stack = ItemStack.EMPTY;
                    posted++;
                } else {
                    allPosted = false;
                }
            }
            if (allPosted) it.remove();
        }
        if (posted > 0) setDirty();
        return posted;
    }

    public void claimAll(ServerLevel world, ServerPlayer player) {
        var server = world.getServer();
        net.fugginbeenus.notchcurrency.mail.MailSweep.run(server);
        int taken = net.fugginbeenus.notchcurrency.mail.MailManager.collectAll(player);
        if (taken == 0) {
            Msg.chat(player, Component.literal("You have nothing waiting to collect.")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

}
