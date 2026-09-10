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

    private record PendingPay(UUID to, long amount, long expiresAt) {}

    private static final java.util.Map<UUID, PendingPay> PENDING_PAYS = new java.util.HashMap<>();
    private static final long PAY_CONFIRM_WINDOW_MS = 30_000L;

    private static boolean needsPayConfirm(ServerPlayer from, ServerPlayer to, long amt) {
        long threshold = net.fugginbeenus.notchcurrency.config.NotchConfigIO.get().currency.payConfirmAbove;
        if (threshold <= 0 || amt < threshold) return false;
        long now = System.currentTimeMillis();
        PendingPay pending = PENDING_PAYS.get(from.getUUID());
        if (pending != null && pending.expiresAt() > now
                && pending.to().equals(to.getUUID()) && pending.amount() == amt) {
            PENDING_PAYS.remove(from.getUUID());
            return false;
        }
        PENDING_PAYS.put(from.getUUID(), new PendingPay(to.getUUID(), amt, now + PAY_CONFIRM_WINDOW_MS));
        String again = "/pay " + to.getName().getString() + " " + amt;
        Component confirm = Component.literal("[Confirm]")
                .withStyle(ChatFormatting.GREEN, ChatFormatting.BOLD)
                .withStyle(style -> style
                        .withClickEvent(net.fugginbeenus.notchcurrency.compat.Chat.runCommand(again))
                        .withHoverEvent(net.fugginbeenus.notchcurrency.compat.Chat.showText(Component.literal(again))));
        net.fugginbeenus.notchcurrency.compat.Msg.chat(from, Component.literal("That is a big payment: ")
                .withStyle(ChatFormatting.YELLOW)
                .append(coins(amt))
                .append(Component.literal(" to " + to.getName().getString() + ". ").withStyle(ChatFormatting.YELLOW))
                .append(confirm)
                .append(Component.literal(" or run the same command again within 30 seconds.").withStyle(ChatFormatting.GRAY)));
        return true;
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("givnotches")
                        .requires(net.fugginbeenus.notchcurrency.compat.Perms::isOperator)
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    ServerPlayer player = ctx.getSource().getPlayer();
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    player.addItem(new ItemStack(ModItems.NOTCH_COIN, amount));
                                    net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("Given " + amount + " Notch Coins!"));
                                    return 1;
                                }))
        );

        dispatcher.register(
                Commands.literal("balance")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            long bal = BalanceStore.get(p);
                            net.fugginbeenus.notchcurrency.compat.Msg.actionBar(p, Component.literal("Balance: " + bal + " ")
                                            .append(NotchCurrency.coinIcon()));
                            NotchPackets.sendBalance(p, bal);
                            return 1;
                        })
        );
        dispatcher.register(
                Commands.literal("bal")
                        .executes(ctx -> {
                            ctx.getSource().getServer().getCommands()
                                    .performPrefixedCommand(ctx.getSource(), "balance");
                            return 1;
                        })
        );

        dispatcher.register(
                Commands.literal("pay")
                        .then(Commands.argument("target", EntityArgument.player())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            ServerPlayer from = ctx.getSource().getPlayer();
                                            ServerPlayer to = EntityArgument.getPlayer(ctx, "target");
                                            int amt = IntegerArgumentType.getInteger(ctx, "amount");

                                            if (from == to) {
                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(from, Component.literal("You can’t pay yourself.")
                                                                .withStyle(ChatFormatting.RED));
                                                return 0;
                                            }

                                            long bal = BalanceStore.get(from);
                                            if (bal < amt) {
                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(from, Component.literal("Insufficient funds.")
                                                                .withStyle(ChatFormatting.RED));
                                                return 0;
                                            }

                                            if (needsPayConfirm(from, to, amt)) return 1;

                                            BalanceStore.subtract(from, amt, net.fugginbeenus.notchcurrency.economy.TransactionReason.PAY, "paid " + to.getName().getString());
                                            BalanceStore.add(to, amt, net.fugginbeenus.notchcurrency.economy.TransactionReason.PAY, "from " + from.getName().getString());

                                            NotchPackets.sendBalance(from, BalanceStore.get(from));
                                            NotchPackets.sendBalance(to, BalanceStore.get(to));

                                            net.fugginbeenus.notchcurrency.compat.Msg.chat(from, Component.literal("Paid " + amt + " ")
                                                            .append(NotchCurrency.coinIcon())
                                                            .append(Component.literal(" to " + to.getName().getString()))
                                                            .withStyle(ChatFormatting.GREEN));

                                            net.fugginbeenus.notchcurrency.compat.Msg.chat(to, Component.literal(from.getName().getString() + " paid you " + amt + " ")
                                                            .append(NotchCurrency.coinIcon())
                                                            .withStyle(ChatFormatting.GREEN));
                                            return 1;
                                        })
                                )
                        )
        );

    }
}
