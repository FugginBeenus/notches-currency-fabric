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

public final class TradeCommands {

    private TradeCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // ===== /trade =====
        dispatcher.register(
                CommandManager.literal("trade")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(ctx -> {
                                    ServerPlayerEntity from = ctx.getSource().getPlayer();
                                    ServerPlayerEntity to = EntityArgumentType.getPlayer(ctx, "player");
                                    TradeManager.invite(from, to);
                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("accept")
                                .then(CommandManager.argument("inviter", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            String inviter = StringArgumentType.getString(ctx, "inviter");
                                            TradeManager.accept(p, inviter);
                                            return 1;
                                        })
                                )
                        )
                        .then(CommandManager.literal("decline")
                                .then(CommandManager.argument("inviter", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            String inviter = StringArgumentType.getString(ctx, "inviter");
                                            TradeManager.decline(p, inviter);
                                            return 1;
                                        })
                                )
                        )
                        // /trade offer: open the create-an-offline-offer screen
                        .then(CommandManager.literal("offer")
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    if (p != null) net.fugginbeenus.notchcurrency.trade.TradeOfferCreateScreenHandler.open(p);
                                    return 1;
                                })
                        )
                        // /trade offers: open the board of offers for you + your open offers
                        .then(CommandManager.literal("offers")
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    if (p != null) net.fugginbeenus.notchcurrency.trade.TradeOffersScreenHandler.open(p);
                                    return 1;
                                })
                        )
        );

    }
}
