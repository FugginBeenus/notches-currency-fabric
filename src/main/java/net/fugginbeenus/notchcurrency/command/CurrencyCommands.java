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
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.Collection;
import java.util.UUID;

import static net.fugginbeenus.notchcurrency.core.NotchCurrency.coinIcon;
import static net.fugginbeenus.notchcurrency.core.NotchCurrency.coins;

public final class CurrencyCommands {

    private CurrencyCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // ===== /givnotches <amount> (admin only) =====
        dispatcher.register(
                Commands.literal("givnotches")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayer();
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    player.addItem(new ItemStack(ModItems.NOTCH_COIN, amount));
                                    player.displayClientMessage(Component.literal("Given " + amount + " Notch Coins!"), false);
                                    return 1;
                                }))
        );

        // ===== /balance + /bal =====
        dispatcher.register(
                Commands.literal("balance")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            long bal = BalanceStore.get(p);
                            p.displayClientMessage(
                                    Component.literal("Balance: " + bal + " ")
                                            .append(NotchCurrency.coinIcon()),
                                    true
                            );
                            NotchPackets.sendBalance(p, bal);
                            return 1;
                        })
        );
        dispatcher.register(
                Commands.literal("bal")
                        .executes(ctx -> {
                            // Block form: executeWithPrefix stopped returning an int in 1.21.
                            ctx.getSource().getServer().getCommands()
                                    .performPrefixedCommand(ctx.getSource(), "balance");
                            return 1;
                        })
        );

        // ===== /pay <player> <amount> =====
        dispatcher.register(
                Commands.literal("pay")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            ServerPlayer from = ctx.getSource().getPlayer();
                                            ServerPlayer to = EntityArgument.getPlayer(ctx, "target");
                                            int amt = IntegerArgumentType.getInteger(ctx, "amount");

                                            if (from == to) {
                                                from.displayClientMessage(
                                                        Component.literal("You can’t pay yourself.")
                                                                .withStyle(ChatFormatting.RED),
                                                        false);
                                                return 0;
                                            }

                                            long bal = BalanceStore.get(from);
                                            if (bal < amt) {
                                                from.displayClientMessage(
                                                        Component.literal("Insufficient funds.")
                                                                .withStyle(ChatFormatting.RED),
                                                        false);
                                                return 0;
                                            }

                                            BalanceStore.subtract(from, amt, net.fugginbeenus.notchcurrency.economy.TransactionReason.PAY, "paid " + to.getName().getString());
                                            BalanceStore.add(to, amt, net.fugginbeenus.notchcurrency.economy.TransactionReason.PAY, "from " + from.getName().getString());

                                            NotchPackets.sendBalance(from, BalanceStore.get(from));
                                            NotchPackets.sendBalance(to, BalanceStore.get(to));

                                            from.displayClientMessage(
                                                    Component.literal("Paid " + amt + " ")
                                                            .append(NotchCurrency.coinIcon())
                                                            .append(Component.literal(" to " + to.getName().getString()))
                                                            .withStyle(ChatFormatting.GREEN),
                                                    false
                                            );

                                            to.displayClientMessage(
                                                    Component.literal(from.getName().getString() + " paid you " + amt + " ")
                                                            .append(NotchCurrency.coinIcon())
                                                            .withStyle(ChatFormatting.GREEN),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )
        );

    }
}
