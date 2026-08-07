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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import java.util.Collection;
import java.util.UUID;

import static net.fugginbeenus.notchcurrency.core.NotchCurrency.coinIcon;
import static net.fugginbeenus.notchcurrency.core.NotchCurrency.coins;

public final class TradeCommands {

    private TradeCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // ===== /trade =====
        dispatcher.register(
                Commands.literal("trade")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> {
                                    ServerPlayer from = ctx.getSource().getPlayer();
                                    ServerPlayer to = EntityArgument.getPlayer(ctx, "player");
                                    TradeManager.invite(from, to);
                                    return 1;
                                })
                        )
                        .then(Commands.literal("accept")
                                .then(Commands.argument("inviter", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayer();
                                            String inviter = StringArgumentType.getString(ctx, "inviter");
                                            TradeManager.accept(p, inviter);
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("decline")
                                .then(Commands.argument("inviter", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayer();
                                            String inviter = StringArgumentType.getString(ctx, "inviter");
                                            TradeManager.decline(p, inviter);
                                            return 1;
                                        })
                                )
                        )
                        // /trade offer: open the create-an-offline-offer screen
                        .then(Commands.literal("offer")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null) net.fugginbeenus.notchcurrency.trade.TradeOfferCreateScreenHandler.open(p);
                                    return 1;
                                })
                        )
                        // /trade offers: open the board of offers for you + your open offers
                        .then(Commands.literal("offers")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p != null) net.fugginbeenus.notchcurrency.trade.TradeOffersScreenHandler.open(p);
                                    return 1;
                                })
                        )
        );

    }
}
