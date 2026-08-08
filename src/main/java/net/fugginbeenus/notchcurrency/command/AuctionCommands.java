package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fugginbeenus.notchcurrency.auction.AuctionCategories;
import net.fugginbeenus.notchcurrency.auction.AuctionConfig;
import net.fugginbeenus.notchcurrency.auction.AuctionHouseScreenHandler;
import net.fugginbeenus.notchcurrency.auction.AuctionListing;
import net.fugginbeenus.notchcurrency.auction.AuctionState;
import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.core.CoinEconomy;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.crate.BalloonEntity;
import net.fugginbeenus.notchcurrency.crate.DailyCrateManager;
import net.fugginbeenus.notchcurrency.crate.GoldenCacheManager;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.fugginbeenus.notchcurrency.trade.TradeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import java.util.Collection;
import java.util.UUID;

import static net.fugginbeenus.notchcurrency.core.NotchCurrency.coinIcon;
import static net.fugginbeenus.notchcurrency.core.NotchCurrency.coins;

public final class AuctionCommands {

    private AuctionCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // ===== /ah (Auction House) =====
        dispatcher.register(
                Commands.literal("ah")

                        // /ah -> open GUI
                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayer();
                            player.openMenu(new SimpleMenuProvider(
                                    (containerId, inv, p) -> new AuctionHouseScreenHandler(containerId, inv),
                                    Component.literal("Auction House")
                            ));
                            return 1;
                        })

                        // /ah browse  -> simple text listing
                        .then(Commands.literal("browse")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    var world = ctx.getSource().getLevel();
                                    AuctionState state = AuctionState.get((ServerLevel) world);

                                    p.displayClientMessage(Component.literal("=== Auction Listings ==="), false);
                                    if (state.getListings().isEmpty()) {
                                        p.displayClientMessage(Component.literal("No active listings."), false);
                                        return 1;
                                    }

                                    for (AuctionListing l : state.getListings()) {
                                        Component line = Component.literal(l.id.toString())
                                                .append(Component.literal(" | " + l.stack.getCount() + "× " + l.stack.getHoverName().getString() + " | " + l.price + " "))
                                                .append(NotchCurrency.coinIcon())
                                                .append(Component.literal(" | Seller: " + l.sellerName));

                                        p.displayClientMessage(line, false);
                                    }
                                    p.displayClientMessage(Component.literal("Use /ah buy <id> to purchase or /ah bid <id> <amount> to bid."), false);

                                    return 1;
                                })
                        )

                        // /ah list <price> [days]
                        .then(Commands.literal("list")
                                // /ah list  (no args) -> open the visual listing screen
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p == null) { ctx.getSource().sendFailure(Component.literal("Run as a player.")); return 0; }
                                    net.fugginbeenus.notchcurrency.auction.AuctionListingScreenHandler.open(p);
                                    return 1;
                                })
                                .then(Commands.argument("price", IntegerArgumentType.integer(1))
                                        // /ah list <price>  -> regular, NO time limit (instant buy)
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayer();
                                            ServerLevel world = (ServerLevel) ctx.getSource().getLevel();
                                            AuctionState state = AuctionState.get(world);

                                            int price = IntegerArgumentType.getInteger(ctx, "price");
                                            // days = 0 => no timer
                                            return handleListCommand(p, world, state, price, 0, false);
                                        })
                                        // /ah list <price> <days> -> TIMED AUCTION
                                        .then(Commands.argument("days", IntegerArgumentType.integer(1, 7))
                                                .executes(ctx -> {
                                                    ServerPlayer p = ctx.getSource().getPlayer();
                                                    ServerLevel world = (ServerLevel) ctx.getSource().getLevel();
                                                    AuctionState state = AuctionState.get(world);

                                                    int price = IntegerArgumentType.getInteger(ctx, "price");
                                                    int days  = IntegerArgumentType.getInteger(ctx, "days");
                                                    return handleListCommand(p, world, state, price, days, true);
                                                })
                                        )
                                )
                        )

                        // /ah buy <id>
                        .then(Commands.literal("buy")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayer();
                                            var world = ctx.getSource().getLevel();
                                            AuctionState state = AuctionState.get((ServerLevel) world);

                                            String raw = StringArgumentType.getString(ctx, "id");
                                            UUID id;
                                            try {
                                                id = UUID.fromString(raw);
                                            } catch (IllegalArgumentException e) {
                                                p.displayClientMessage(Component.literal(
                                                        "Invalid listing id (must be a UUID)."
                                                ), false);
                                                return 0;
                                            }

                                            state.buyListing(p, id);
                                            return 1;
                                        })
                                )
                        )

                        // /ah claim <id> – winner-only, pulls from pending winnings
                        .then(Commands.literal("claim")
                                // /ah claim
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    ServerLevel world = (ServerLevel) ctx.getSource().getLevel();
                                    AuctionState state = AuctionState.get(world);
                                    state.claimAll(world, p);
                                    return 1;
                                })
                                // /ah claim <id>
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayer();
                                            ServerLevel world = (ServerLevel) ctx.getSource().getLevel();
                                            AuctionState state = AuctionState.get(world);

                                            String raw = StringArgumentType.getString(ctx, "id");
                                            UUID id;
                                            try {
                                                id = UUID.fromString(raw);
                                            } catch (IllegalArgumentException e) {
                                                p.displayClientMessage(
                                                        Component.literal("Invalid listing id (must be a UUID).")
                                                                .withStyle(ChatFormatting.RED),
                                                        false
                                                );
                                                return 0;
                                            }

                                            AuctionState.PendingWinnings pw = state.getPending(id);
                                            if (pw == null) {
                                                p.displayClientMessage(
                                                        Component.literal("No pending winnings for that id.")
                                                                .withStyle(ChatFormatting.RED),
                                                        false
                                                );
                                                return 0;
                                            }

                                            boolean claimedSomething = false;

                                            // Claim coins if you are the seller
                                            if (p.getUUID().equals(pw.sellerUuid) && pw.finalPrice > 0L) {
                                                long amt = pw.finalPrice;
                                                BalanceStore.add(p, amt);
                                                NotchPackets.sendBalance(p, BalanceStore.get(p));

                                                p.displayClientMessage(
                                                        Component.literal("Claimed ")
                                                                .append(Component.literal(String.valueOf(amt) + " ").withStyle(ChatFormatting.GOLD))
                                                                .append(NotchCurrency.coinIcon())
                                                                .append(Component.literal(" from auction winnings.").withStyle(ChatFormatting.GREEN)),
                                                        false
                                                );
                                                pw.finalPrice = 0L;
                                                claimedSomething = true;
                                            }

                                            // Claim item if you are the winner (or owner of returned item)
                                            if (p.getUUID().equals(pw.winnerUuid) && !pw.stack.isEmpty()) {
                                                ItemStack toGive = pw.stack.copy();
                                                // Strip auction NBT tags so the item's normal tooltip returns
                                                if (StackData.hasData(toGive)) {
                                                    CompoundTag tag = StackData.editData(toGive);
                                                    tag.remove("nc_price");
                                                    tag.remove("nc_seller");
                                                    tag.remove("nc_created");
                                                    tag.remove("nc_expires");
                                                    tag.remove("nc_highest_bid");
                                                    tag.remove("nc_highest_bidder");
                                                    tag.remove("nc_listing_id");
                                                    if (tag.isEmpty()) {
                                                        StackData.clearData(toGive);
                                                    } else {
                                                        StackData.commitData(toGive, tag);
                                                    }
                                                }
                                                boolean inserted = p.getInventory().add(toGive);
                                                if (!inserted && !toGive.isEmpty()) {
                                                    // Explicit claim: worst case drop near them
                                                    p.drop(toGive, false);
                                                }

                                                p.displayClientMessage(
                                                        Component.literal("Claimed item: ")
                                                                .append(pw.stack.getHoverName().copy())
                                                                .withStyle(ChatFormatting.GREEN),
                                                        false
                                                );
                                                pw.stack = ItemStack.EMPTY;
                                                claimedSomething = true;
                                            }

                                            // If you don't match either role
                                            if (!claimedSomething) {
                                                p.displayClientMessage(
                                                        Component.literal("You have nothing to claim for that listing.")
                                                                .withStyle(ChatFormatting.RED),
                                                        false
                                                );
                                                return 0;
                                            }

                                            if (pw.isFullyClaimed()) {
                                                state.removePending(id);
                                            } else {
                                                state.addPending(pw); // markDirty is inside
                                            }

                                            return 1;
                                        })
                                )
                        )

                        // /ah bid <id> <amount>
                        .then(Commands.literal("bid")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    ServerPlayer p = ctx.getSource().getPlayer();
                                                    ServerLevel world = (ServerLevel) ctx.getSource().getLevel();
                                                    AuctionState state = AuctionState.get(world);

                                                    String raw = StringArgumentType.getString(ctx, "id");
                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");

                                                    UUID id;
                                                    try {
                                                        id = UUID.fromString(raw);
                                                    } catch (IllegalArgumentException e) {
                                                        p.displayClientMessage(Component.literal(
                                                                "Invalid listing id (must be a UUID)."
                                                        ), false);
                                                        return 0;
                                                    }

                                                    state.placeBid(world, p, id, amount);
                                                    return 1;
                                                })
                                        )
                                )
                        )

                        // /ah cancel <id> (seller only; returns item)
                        .then(Commands.literal("cancel")
                                // /ah cancel (no args) - show your listings with clickable cancel buttons
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    var world = ctx.getSource().getLevel();
                                    AuctionState state = AuctionState.get((ServerLevel) world);

                                    Collection<AuctionListing> allListings = state.getListings();
                                    java.util.List<AuctionListing> myListings = new java.util.ArrayList<>();

                                    for (AuctionListing l : allListings) {
                                        if (l.sellerUuid.equals(p.getUUID())) {
                                            myListings.add(l);
                                        }
                                    }

                                    if (myListings.isEmpty()) {
                                        p.displayClientMessage(Component.literal("You have no active auction listings.")
                                                .withStyle(ChatFormatting.GRAY), false);
                                        return 0;
                                    }

                                    p.displayClientMessage(Component.literal("─── Your Auction Listings ───")
                                            .withStyle(ChatFormatting.GOLD), false);

                                    for (AuctionListing l : myListings) {
                                        int count = l.stack.getCount();
                                        String itemName = l.stack.getHoverName().getString();
                                        long price = l.price;

                                        // Build the listing line
                                        MutableComponent line = Component.literal(" • ")
                                                .withStyle(ChatFormatting.GRAY)
                                                .append(Component.literal(count + "x ")
                                                        .withStyle(ChatFormatting.WHITE))
                                                .append(Component.literal(itemName)
                                                        .withStyle(ChatFormatting.YELLOW))
                                                .append(Component.literal(" - ")
                                                        .withStyle(ChatFormatting.GRAY))
                                                .append(Component.literal(String.valueOf(price) + " ")
                                                        .withStyle(ChatFormatting.GOLD))
                                                .append(coinIcon())
                                                .append(Component.literal(" "));

                                        // Add clickable [Cancel] button
                                        MutableComponent cancelBtn = Component.literal("[Cancel]")
                                                .withStyle(ChatFormatting.RED)
                                                .withStyle(style -> style
                                                        .withClickEvent(net.fugginbeenus.notchcurrency.compat.Chat.runCommand("/ah cancel " + l.id.toString()))
                                                        .withHoverEvent(net.fugginbeenus.notchcurrency.compat.Chat.showText(Component.literal("Click to cancel this listing")))
                                                );

                                        line.append(cancelBtn);
                                        p.displayClientMessage(line, false);
                                    }

                                    p.displayClientMessage(Component.literal("────────────────────")
                                            .withStyle(ChatFormatting.GOLD), false);
                                    return 1;
                                })
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayer();
                                            var world = ctx.getSource().getLevel();
                                            AuctionState state = AuctionState.get((ServerLevel) world);

                                            String raw = StringArgumentType.getString(ctx, "id");
                                            UUID id;
                                            try {
                                                id = UUID.fromString(raw);
                                            } catch (IllegalArgumentException e) {
                                                p.displayClientMessage(Component.literal(
                                                        "Invalid listing id (must be a UUID)."
                                                ), false);
                                                return 0;
                                            }

                                            AuctionListing l = state.getListing(id);
                                            if (l == null) {
                                                p.displayClientMessage(Component.literal(
                                                        "No listing with that id."
                                                ), false);
                                                return 0;
                                            }
                                            if (!p.getUUID().equals(l.sellerUuid)) {
                                                p.displayClientMessage(Component.literal(
                                                        "Only the seller can cancel this listing."
                                                ), false);
                                                return 0;
                                            }

                                            // Refund any standing bid before removing: bids escrow
                                            // coins, so cancelling without this destroys them.
                                            long refunded = state.refundHighestBid((ServerLevel) world, l);
                                            state.removeListing(id);
                                            if (refunded > 0) {
                                                p.displayClientMessage(Component.literal("The high bidder was refunded "
                                                        + refunded + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".").withStyle(ChatFormatting.GRAY), false);
                                            }

                                            ItemStack toReturn = l.stack.copy();
                                            // Strip auction NBT tags so the item's normal tooltip returns
                                            if (StackData.hasData(toReturn)) {
                                                CompoundTag tag = StackData.editData(toReturn);
                                                tag.remove("nc_price");
                                                tag.remove("nc_seller");
                                                tag.remove("nc_created");
                                                tag.remove("nc_expires");
                                                tag.remove("nc_highest_bid");
                                                tag.remove("nc_highest_bidder");
                                                tag.remove("nc_listing_id");
                                                if (tag.isEmpty()) {
                                                    StackData.clearData(toReturn);
                                                } else {
                                                    StackData.commitData(toReturn, tag);
                                                }
                                            }
                                            boolean inserted = p.getInventory().add(toReturn);
                                            if (!inserted && !toReturn.isEmpty()) {
                                                p.drop(toReturn, false);
                                            }

                                            p.displayClientMessage(Component.literal("Cancelled listing for ")
                                                    .withStyle(ChatFormatting.GREEN)
                                                    .append(l.stack.getHoverName().copy().withStyle(ChatFormatting.YELLOW))
                                                    .append(Component.literal(".").withStyle(ChatFormatting.GREEN)), false);
                                            return 1;
                                        })
                                )
                        )
        );

    }

    private static int handleListCommand(ServerPlayer p,
                                  ServerLevel world,
                                  AuctionState state,
                                  int price,
                                  int days,
                                  boolean timed) {

        ItemStack hand = p.getMainHandItem();

        if (hand.isEmpty()) {
            p.displayClientMessage(Component.literal(
                    "Hold the item you want to list in your main hand."
            ), false);
            return 0;
        }

        // --- Auction listing fee (optional, scales with price) ---
        long fee = AuctionConfig.listingFee(price);
        if (fee > 0) {
            long bal = BalanceStore.get(p);
            if (bal < fee) {
                p.displayClientMessage(
                        Component.literal("You need ")
                                .append(Component.literal(String.valueOf(fee) + " ").withStyle(ChatFormatting.GOLD))
                                .append(NotchCurrency.coinIcon())
                                .append(Component.literal(" to pay the auction listing fee.")
                                        .withStyle(ChatFormatting.RED)),
                        false
                );
                return 0;
            }

            BalanceStore.subtract(p, fee, net.fugginbeenus.notchcurrency.economy.TransactionReason.SINK, "auction listing fee");
            NotchPackets.sendBalance(p, BalanceStore.get(p));

            p.displayClientMessage(
                    Component.literal("Paid ")
                            .append(Component.literal(String.valueOf(fee) + " ").withStyle(ChatFormatting.GOLD))
                            .append(NotchCurrency.coinIcon())
                            .append(Component.literal(" as auction listing fee.")
                                    .withStyle(ChatFormatting.GRAY)),
                    false
            );
        }
        // --- end listing fee ---

        // Copy stack, remove from player
        ItemStack listingStack = hand.copy();
        hand.shrink(listingStack.getCount());

        long durationTicks = 0L;
        int clampedDays = 0;

        if (timed) {
            clampedDays = Math.max(1, Math.min(7, days));

            // real-time days: 24h * 60m * 60s * 20 ticks/s
            durationTicks = clampedDays * 24L * 60L * 60L * 20L;
        }

        String category = AuctionCategories.classify(listingStack);

        AuctionListing listing = state.addListing(
                world, p, listingStack, price, category, durationTicks);

        // Common part of the message
        MutableComponent listedMsg = Component.literal("Listed ")
                .append(Component.literal("x" + listingStack.getCount() + " ")
                        .withStyle(ChatFormatting.GREEN))
                .append(listingStack.getHoverName().copy().withStyle(ChatFormatting.GREEN))
                .append(Component.literal(" for " + price + " ")
                        .withStyle(ChatFormatting.GOLD))
                .append(NotchCurrency.coinIcon());

        if (timed) {
            listedMsg.append(
                    Component.literal(" (" + clampedDays + " day" + (clampedDays > 1 ? "s" : "") + " auction)")
                            .withStyle(ChatFormatting.GRAY)
            );
        } else {
            listedMsg.append(
                    Component.literal(" (no time limit)")
                            .withStyle(ChatFormatting.GRAY)
            );
        }

        p.displayClientMessage(listedMsg, false);

        return 1;
    }
}
