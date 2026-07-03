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
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.core.CoinEconomy;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.crate.BalloonEntity;
import net.fugginbeenus.notchcurrency.crate.DailyCrateManager;
import net.fugginbeenus.notchcurrency.crate.GoldenCacheManager;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.fugginbeenus.notchcurrency.trade.TradeManager;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.UUID;

import static net.fugginbeenus.notchcurrency.core.NotchCurrency.coinIcon;
import static net.fugginbeenus.notchcurrency.core.NotchCurrency.coins;

/** Extracted from the mod initializer; registered from NotchCurrency.onInitialize(). */
public final class AuctionCommands {

    private AuctionCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // ===== /ah (Auction House) =====
        dispatcher.register(
                CommandManager.literal("ah")

                        // /ah -> open GUI
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                                    (syncId, inv, p) -> new AuctionHouseScreenHandler(syncId, inv),
                                    Text.literal("Auction House")
                            ));
                            return 1;
                        })

                        // /ah browse  -> simple text listing
                        .then(CommandManager.literal("browse")
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    var world = ctx.getSource().getWorld();
                                    AuctionState state = AuctionState.get((ServerWorld) world);

                                    p.sendMessage(Text.literal("=== Auction Listings ==="), false);
                                    if (state.getListings().isEmpty()) {
                                        p.sendMessage(Text.literal("No active listings."), false);
                                        return 1;
                                    }

                                    for (AuctionListing l : state.getListings()) {
                                        Text line = Text.literal(l.id.toString())
                                                .append(Text.literal(" | " + l.stack.getCount() + "× " + l.stack.getName().getString() + " | " + l.price + " "))
                                                .append(NotchCurrency.coinIcon())
                                                .append(Text.literal(" | Seller: " + l.sellerName));

                                        p.sendMessage(line, false);
                                    }
                                    p.sendMessage(Text.literal("Use /ah buy <id> to purchase or /ah bid <id> <amount> to bid."), false);

                                    return 1;
                                })
                        )

                        // /ah list <price> [days]
                        .then(CommandManager.literal("list")
                                // /ah list  (no args) -> open the visual listing screen
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    if (p == null) { ctx.getSource().sendError(Text.literal("Run as a player.")); return 0; }
                                    net.fugginbeenus.notchcurrency.auction.AuctionListingScreenHandler.open(p);
                                    return 1;
                                })
                                .then(CommandManager.argument("price", IntegerArgumentType.integer(1))
                                        // /ah list <price>  -> regular, NO time limit (instant buy)
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            ServerWorld world = (ServerWorld) ctx.getSource().getWorld();
                                            AuctionState state = AuctionState.get(world);

                                            int price = IntegerArgumentType.getInteger(ctx, "price");
                                            // days = 0 => no timer
                                            return handleListCommand(p, world, state, price, 0, false);
                                        })
                                        // /ah list <price> <days> -> TIMED AUCTION
                                        .then(CommandManager.argument("days", IntegerArgumentType.integer(1, 7))
                                                .executes(ctx -> {
                                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                                    ServerWorld world = (ServerWorld) ctx.getSource().getWorld();
                                                    AuctionState state = AuctionState.get(world);

                                                    int price = IntegerArgumentType.getInteger(ctx, "price");
                                                    int days  = IntegerArgumentType.getInteger(ctx, "days");
                                                    return handleListCommand(p, world, state, price, days, true);
                                                })
                                        )
                                )
                        )

                        // /ah buy <id>
                        .then(CommandManager.literal("buy")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            var world = ctx.getSource().getWorld();
                                            AuctionState state = AuctionState.get((ServerWorld) world);

                                            String raw = StringArgumentType.getString(ctx, "id");
                                            UUID id;
                                            try {
                                                id = UUID.fromString(raw);
                                            } catch (IllegalArgumentException e) {
                                                p.sendMessage(Text.literal(
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
                        .then(CommandManager.literal("claim")
                                // /ah claim
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    ServerWorld world = (ServerWorld) ctx.getSource().getWorld();
                                    AuctionState state = AuctionState.get(world);
                                    state.claimAll(world, p);
                                    return 1;
                                })
                                // /ah claim <id>
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            ServerWorld world = (ServerWorld) ctx.getSource().getWorld();
                                            AuctionState state = AuctionState.get(world);

                                            String raw = StringArgumentType.getString(ctx, "id");
                                            UUID id;
                                            try {
                                                id = UUID.fromString(raw);
                                            } catch (IllegalArgumentException e) {
                                                p.sendMessage(
                                                        Text.literal("Invalid listing id (must be a UUID).")
                                                                .formatted(Formatting.RED),
                                                        false
                                                );
                                                return 0;
                                            }

                                            AuctionState.PendingWinnings pw = state.getPending(id);
                                            if (pw == null) {
                                                p.sendMessage(
                                                        Text.literal("No pending winnings for that id.")
                                                                .formatted(Formatting.RED),
                                                        false
                                                );
                                                return 0;
                                            }

                                            boolean claimedSomething = false;

                                            // Claim coins if you are the seller
                                            if (p.getUuid().equals(pw.sellerUuid) && pw.finalPrice > 0L) {
                                                long amt = pw.finalPrice;
                                                BalanceStore.add(p, amt);
                                                NotchPackets.sendBalance(p, BalanceStore.get(p));

                                                p.sendMessage(
                                                        Text.literal("Claimed ")
                                                                .append(Text.literal(String.valueOf(amt) + " ").formatted(Formatting.GOLD))
                                                                .append(NotchCurrency.coinIcon())
                                                                .append(Text.literal(" from auction winnings.").formatted(Formatting.GREEN)),
                                                        false
                                                );
                                                pw.finalPrice = 0L;
                                                claimedSomething = true;
                                            }

                                            // Claim item if you are the winner (or owner of returned item)
                                            if (p.getUuid().equals(pw.winnerUuid) && !pw.stack.isEmpty()) {
                                                ItemStack toGive = pw.stack.copy();
                                                // Strip auction NBT tags so the item's normal tooltip returns
                                                if (toGive.hasNbt()) {
                                                    NbtCompound tag = toGive.getNbt();
                                                    tag.remove("nc_price");
                                                    tag.remove("nc_seller");
                                                    tag.remove("nc_created");
                                                    tag.remove("nc_expires");
                                                    tag.remove("nc_highest_bid");
                                                    tag.remove("nc_highest_bidder");
                                                    tag.remove("nc_listing_id");
                                                    if (tag.isEmpty()) {
                                                        toGive.setNbt(null);
                                                    }
                                                }
                                                boolean inserted = p.getInventory().insertStack(toGive);
                                                if (!inserted && !toGive.isEmpty()) {
                                                    // Explicit claim: worst case drop near them
                                                    p.dropItem(toGive, false);
                                                }

                                                p.sendMessage(
                                                        Text.literal("Claimed item: ")
                                                                .append(pw.stack.getName().copy())
                                                                .formatted(Formatting.GREEN),
                                                        false
                                                );
                                                pw.stack = ItemStack.EMPTY;
                                                claimedSomething = true;
                                            }

                                            // If you don't match either role
                                            if (!claimedSomething) {
                                                p.sendMessage(
                                                        Text.literal("You have nothing to claim for that listing.")
                                                                .formatted(Formatting.RED),
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
                        .then(CommandManager.literal("bid")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                                    ServerWorld world = (ServerWorld) ctx.getSource().getWorld();
                                                    AuctionState state = AuctionState.get(world);

                                                    String raw = StringArgumentType.getString(ctx, "id");
                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");

                                                    UUID id;
                                                    try {
                                                        id = UUID.fromString(raw);
                                                    } catch (IllegalArgumentException e) {
                                                        p.sendMessage(Text.literal(
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
                        .then(CommandManager.literal("cancel")
                                // /ah cancel (no args) - show your listings with clickable cancel buttons
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    var world = ctx.getSource().getWorld();
                                    AuctionState state = AuctionState.get((ServerWorld) world);

                                    Collection<AuctionListing> allListings = state.getListings();
                                    java.util.List<AuctionListing> myListings = new java.util.ArrayList<>();

                                    for (AuctionListing l : allListings) {
                                        if (l.sellerUuid.equals(p.getUuid())) {
                                            myListings.add(l);
                                        }
                                    }

                                    if (myListings.isEmpty()) {
                                        p.sendMessage(Text.literal("You have no active auction listings.")
                                                .formatted(Formatting.GRAY), false);
                                        return 0;
                                    }

                                    p.sendMessage(Text.literal("─── Your Auction Listings ───")
                                            .formatted(Formatting.GOLD), false);

                                    for (AuctionListing l : myListings) {
                                        int count = l.stack.getCount();
                                        String itemName = l.stack.getName().getString();
                                        long price = l.price;

                                        // Build the listing line
                                        MutableText line = Text.literal(" • ")
                                                .formatted(Formatting.GRAY)
                                                .append(Text.literal(count + "x ")
                                                        .formatted(Formatting.WHITE))
                                                .append(Text.literal(itemName)
                                                        .formatted(Formatting.YELLOW))
                                                .append(Text.literal(" - ")
                                                        .formatted(Formatting.GRAY))
                                                .append(Text.literal(String.valueOf(price) + " ")
                                                        .formatted(Formatting.GOLD))
                                                .append(coinIcon())
                                                .append(Text.literal(" "));

                                        // Add clickable [Cancel] button
                                        MutableText cancelBtn = Text.literal("[Cancel]")
                                                .formatted(Formatting.RED)
                                                .styled(style -> style
                                                        .withClickEvent(new net.minecraft.text.ClickEvent(
                                                                net.minecraft.text.ClickEvent.Action.RUN_COMMAND,
                                                                "/ah cancel " + l.id.toString()
                                                        ))
                                                        .withHoverEvent(new net.minecraft.text.HoverEvent(
                                                                net.minecraft.text.HoverEvent.Action.SHOW_TEXT,
                                                                Text.literal("Click to cancel this listing")
                                                        ))
                                                );

                                        line.append(cancelBtn);
                                        p.sendMessage(line, false);
                                    }

                                    p.sendMessage(Text.literal("────────────────────")
                                            .formatted(Formatting.GOLD), false);
                                    return 1;
                                })
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            var world = ctx.getSource().getWorld();
                                            AuctionState state = AuctionState.get((ServerWorld) world);

                                            String raw = StringArgumentType.getString(ctx, "id");
                                            UUID id;
                                            try {
                                                id = UUID.fromString(raw);
                                            } catch (IllegalArgumentException e) {
                                                p.sendMessage(Text.literal(
                                                        "Invalid listing id (must be a UUID)."
                                                ), false);
                                                return 0;
                                            }

                                            AuctionListing l = state.getListing(id);
                                            if (l == null) {
                                                p.sendMessage(Text.literal(
                                                        "No listing with that id."
                                                ), false);
                                                return 0;
                                            }
                                            if (!p.getUuid().equals(l.sellerUuid)) {
                                                p.sendMessage(Text.literal(
                                                        "Only the seller can cancel this listing."
                                                ), false);
                                                return 0;
                                            }

                                            // Refund any standing bid before removing — bids escrow
                                            // coins, so cancelling without this destroys them.
                                            long refunded = state.refundHighestBid((ServerWorld) world, l);
                                            state.removeListing(id);
                                            if (refunded > 0) {
                                                p.sendMessage(Text.literal("The high bidder was refunded "
                                                        + refunded + " coins.").formatted(Formatting.GRAY), false);
                                            }

                                            ItemStack toReturn = l.stack.copy();
                                            // Strip auction NBT tags so the item's normal tooltip returns
                                            if (toReturn.hasNbt()) {
                                                NbtCompound tag = toReturn.getNbt();
                                                tag.remove("nc_price");
                                                tag.remove("nc_seller");
                                                tag.remove("nc_created");
                                                tag.remove("nc_expires");
                                                tag.remove("nc_highest_bid");
                                                tag.remove("nc_highest_bidder");
                                                tag.remove("nc_listing_id");
                                                if (tag.isEmpty()) {
                                                    toReturn.setNbt(null);
                                                }
                                            }
                                            boolean inserted = p.getInventory().insertStack(toReturn);
                                            if (!inserted && !toReturn.isEmpty()) {
                                                p.dropItem(toReturn, false);
                                            }

                                            p.sendMessage(Text.literal("Cancelled listing for ")
                                                    .formatted(Formatting.GREEN)
                                                    .append(l.stack.getName().copy().formatted(Formatting.YELLOW))
                                                    .append(Text.literal(".").formatted(Formatting.GREEN)), false);
                                            return 1;
                                        })
                                )
                        )
        );

    }

    private static int handleListCommand(ServerPlayerEntity p,
                                  ServerWorld world,
                                  AuctionState state,
                                  int price,
                                  int days,
                                  boolean timed) {

        ItemStack hand = p.getMainHandStack();

        if (hand.isEmpty()) {
            p.sendMessage(Text.literal(
                    "Hold the item you want to list in your main hand."
            ), false);
            return 0;
        }

        // --- Auction listing fee (optional, scales with price) ---
        long fee = AuctionConfig.listingFee(price);
        if (fee > 0) {
            long bal = BalanceStore.get(p);
            if (bal < fee) {
                p.sendMessage(
                        Text.literal("You need ")
                                .append(Text.literal(String.valueOf(fee) + " ").formatted(Formatting.GOLD))
                                .append(NotchCurrency.coinIcon())
                                .append(Text.literal(" to pay the auction listing fee.")
                                        .formatted(Formatting.RED)),
                        false
                );
                return 0;
            }

            BalanceStore.subtract(p, fee, net.fugginbeenus.notchcurrency.economy.TransactionReason.SINK, "auction listing fee");
            NotchPackets.sendBalance(p, BalanceStore.get(p));

            p.sendMessage(
                    Text.literal("Paid ")
                            .append(Text.literal(String.valueOf(fee) + " ").formatted(Formatting.GOLD))
                            .append(NotchCurrency.coinIcon())
                            .append(Text.literal(" as auction listing fee.")
                                    .formatted(Formatting.GRAY)),
                    false
            );
        }
        // --- end listing fee ---

        // Copy stack, remove from player
        ItemStack listingStack = hand.copy();
        hand.decrement(listingStack.getCount());

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
        MutableText listedMsg = Text.literal("Listed ")
                .append(Text.literal("x" + listingStack.getCount() + " ")
                        .formatted(Formatting.GREEN))
                .append(listingStack.getName().copy().formatted(Formatting.GREEN))
                .append(Text.literal(" for " + price + " ")
                        .formatted(Formatting.GOLD))
                .append(NotchCurrency.coinIcon());

        if (timed) {
            listedMsg.append(
                    Text.literal(" (" + clampedDays + " day" + (clampedDays > 1 ? "s" : "") + " auction)")
                            .formatted(Formatting.GRAY)
            );
        } else {
            listedMsg.append(
                    Text.literal(" (no time limit)")
                            .formatted(Formatting.GRAY)
            );
        }

        p.sendMessage(listedMsg, false);

        return 1;
    }
}
