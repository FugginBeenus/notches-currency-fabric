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
        dispatcher.register(
                Commands.literal("ah")

                        .executes(ctx -> {
                            ServerPlayer player = ctx.getSource().getPlayer();
                            player.openMenu(new SimpleMenuProvider(
                                    (containerId, inv, p) -> new AuctionHouseScreenHandler(containerId, inv),
                                    Component.literal("Auction House")
                            ));
                            return 1;
                        })

                        .then(Commands.literal("browse")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    var world = ctx.getSource().getLevel();
                                    AuctionState state = AuctionState.get((ServerLevel) world);

                                    net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("=== Auction Listings ==="));
                                    if (state.getListings().isEmpty()) {
                                        net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("No active listings."));
                                        return 1;
                                    }

                                    for (AuctionListing l : state.getListings()) {
                                        Component line = Component.literal(l.id.toString())
                                                .append(Component.literal(" | " + l.stack.getCount() + "× " + l.stack.getHoverName().getString() + " | " + l.price + " "))
                                                .append(NotchCurrency.coinIcon())
                                                .append(Component.literal(" | Seller: " + l.sellerName));

                                        net.fugginbeenus.notchcurrency.compat.Msg.chat(p, line);
                                    }
                                    net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Use /ah buy <id> to purchase or /ah bid <id> <amount> to bid."));

                                    return 1;
                                })
                        )

                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p == null) { ctx.getSource().sendFailure(Component.literal("Run as a player.")); return 0; }
                                    net.fugginbeenus.notchcurrency.auction.AuctionListingScreenHandler.open(p);
                                    return 1;
                                })
                                .then(Commands.argument("price", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayer();
                                            ServerLevel world = (ServerLevel) ctx.getSource().getLevel();
                                            AuctionState state = AuctionState.get(world);

                                            int price = IntegerArgumentType.getInteger(ctx, "price");
                                            return handleListCommand(p, world, state, price, 0, false);
                                        })
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
                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal(
                                                        "Invalid listing id (must be a UUID)."
                                                ));
                                                return 0;
                                            }

                                            state.buyListing(p, id);
                                            return 1;
                                        })
                                )
                        )

                        .then(Commands.literal("claim")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    ServerLevel world = (ServerLevel) ctx.getSource().getLevel();
                                    AuctionState state = AuctionState.get(world);
                                    state.claimAll(world, p);
                                    return 1;
                                })
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
                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Invalid listing id (must be a UUID).")
                                                                .withStyle(ChatFormatting.RED));
                                                return 0;
                                            }

                                            AuctionState.PendingWinnings pw = state.getPending(id);
                                            if (pw == null) {
                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("No pending winnings for that id.")
                                                                .withStyle(ChatFormatting.RED));
                                                return 0;
                                            }

                                            boolean claimedSomething = false;
                                            if (p.getUUID().equals(pw.sellerUuid) && pw.finalPrice > 0L) {
                                                long amt = pw.finalPrice;
                                                BalanceStore.add(p, amt);
                                                NotchPackets.sendBalance(p, BalanceStore.get(p));

                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Claimed ")
                                                                .append(Component.literal(String.valueOf(amt) + " ").withStyle(ChatFormatting.GOLD))
                                                                .append(NotchCurrency.coinIcon())
                                                                .append(Component.literal(" from auction winnings.").withStyle(ChatFormatting.GREEN)));
                                                pw.finalPrice = 0L;
                                                claimedSomething = true;
                                            }

                                            if (p.getUUID().equals(pw.winnerUuid) && !pw.stack.isEmpty()) {
                                                ItemStack toGive = pw.stack.copy();
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
                                                    p.drop(toGive, false);
                                                }

                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Claimed item: ")
                                                                .append(pw.stack.getHoverName().copy())
                                                                .withStyle(ChatFormatting.GREEN));
                                                pw.stack = ItemStack.EMPTY;
                                                claimedSomething = true;
                                            }

                                            if (!claimedSomething) {
                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("You have nothing to claim for that listing.")
                                                                .withStyle(ChatFormatting.RED));
                                                return 0;
                                            }

                                            if (pw.isFullyClaimed()) {
                                                state.removePending(id);
                                            } else {
                                                state.addPending(pw);
                                            }

                                            return 1;
                                        })
                                )
                        )

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
                                                        net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal(
                                                                "Invalid listing id (must be a UUID)."
                                                        ));
                                                        return 0;
                                                    }

                                                    state.placeBid(world, p, id, amount);
                                                    return 1;
                                                })
                                        )
                                )
                        )

                        .then(Commands.literal("cancel")
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
                                        net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("You have no active auction listings.")
                                                .withStyle(ChatFormatting.GRAY));
                                        return 0;
                                    }

                                    net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("─── Your Auction Listings ───")
                                            .withStyle(ChatFormatting.GOLD));

                                    for (AuctionListing l : myListings) {
                                        int count = l.stack.getCount();
                                        String itemName = l.stack.getHoverName().getString();
                                        long price = l.price;

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

                                        MutableComponent cancelBtn = Component.literal("[Cancel]")
                                                .withStyle(ChatFormatting.RED)
                                                .withStyle(style -> style
                                                        .withClickEvent(net.fugginbeenus.notchcurrency.compat.Chat.runCommand("/ah cancel " + l.id.toString()))
                                                        .withHoverEvent(net.fugginbeenus.notchcurrency.compat.Chat.showText(Component.literal("Click to cancel this listing")))
                                                );

                                        line.append(cancelBtn);
                                        net.fugginbeenus.notchcurrency.compat.Msg.chat(p, line);
                                    }

                                    net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("────────────────────")
                                            .withStyle(ChatFormatting.GOLD));
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
                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal(
                                                        "Invalid listing id (must be a UUID)."
                                                ));
                                                return 0;
                                            }

                                            AuctionListing l = state.getListing(id);
                                            if (l == null) {
                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal(
                                                        "No listing with that id."
                                                ));
                                                return 0;
                                            }
                                            if (!p.getUUID().equals(l.sellerUuid)) {
                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal(
                                                        "Only the seller can cancel this listing."
                                                ));
                                                return 0;
                                            }

                                            long refunded = state.refundHighestBid((ServerLevel) world, l);
                                            state.removeListing(id);
                                            if (refunded > 0) {
                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("The high bidder was refunded "
                                                        + refunded + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".").withStyle(ChatFormatting.GRAY));
                                            }

                                            ItemStack toReturn = l.stack.copy();
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

                                            net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Cancelled listing for ")
                                                    .withStyle(ChatFormatting.GREEN)
                                                    .append(l.stack.getHoverName().copy().withStyle(ChatFormatting.YELLOW))
                                                    .append(Component.literal(".").withStyle(ChatFormatting.GREEN)));
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
            net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal(
                    "Hold the item you want to list in your main hand."
            ));
            return 0;
        }

        long fee = AuctionConfig.listingFee(price);
        if (fee > 0) {
            long bal = BalanceStore.get(p);
            if (bal < fee) {
                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("You need ")
                                .append(Component.literal(String.valueOf(fee) + " ").withStyle(ChatFormatting.GOLD))
                                .append(NotchCurrency.coinIcon())
                                .append(Component.literal(" to pay the auction listing fee.")
                                        .withStyle(ChatFormatting.RED)));
                return 0;
            }

            BalanceStore.subtract(p, fee, net.fugginbeenus.notchcurrency.economy.TransactionReason.SINK, "auction listing fee");
            NotchPackets.sendBalance(p, BalanceStore.get(p));

            net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Paid ")
                            .append(Component.literal(String.valueOf(fee) + " ").withStyle(ChatFormatting.GOLD))
                            .append(NotchCurrency.coinIcon())
                            .append(Component.literal(" as auction listing fee.")
                                    .withStyle(ChatFormatting.GRAY)));
        }

        ItemStack listingStack = hand.copy();
        hand.shrink(listingStack.getCount());

        long durationTicks = 0L;
        int clampedDays = 0;

        if (timed) {
            clampedDays = Math.max(1, Math.min(7, days));
            durationTicks = clampedDays * 24L * 60L * 60L * 20L;
        }

        String category = AuctionCategories.classify(listingStack);

        AuctionListing listing = state.addListing(
                world, p, listingStack, price, category, durationTicks);

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

        net.fugginbeenus.notchcurrency.compat.Msg.chat(p, listedMsg);

        return 1;
    }
}
