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
public final class CurrencyCommands {

    private CurrencyCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // ===== /givnotches <amount> (admin only) =====
        dispatcher.register(
                CommandManager.literal("givnotches")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    player.giveItemStack(new ItemStack(ModItems.NOTCH_COIN, amount));
                                    player.sendMessage(Text.literal("Given " + amount + " Notch Coins!"), false);
                                    return 1;
                                }))
        );

        // ===== /balance + /bal =====
        dispatcher.register(
                CommandManager.literal("balance")
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            long bal = BalanceStore.get(p);
                            p.sendMessage(
                                    Text.literal("Balance: " + bal + " ")
                                            .append(NotchCurrency.coinIcon()),
                                    true
                            );
                            NotchPackets.sendBalance(p, bal);
                            return 1;
                        })
        );
        dispatcher.register(
                CommandManager.literal("bal")
                        .executes(ctx -> {
                            // Block form: executeWithPrefix stopped returning an int in 1.21.
                            ctx.getSource().getServer().getCommandManager()
                                    .executeWithPrefix(ctx.getSource(), "balance");
                            return 1;
                        })
        );

        // ===== /pay <player> <amount> =====
        dispatcher.register(
                CommandManager.literal("pay")
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            ServerPlayerEntity from = ctx.getSource().getPlayer();
                                            ServerPlayerEntity to = EntityArgumentType.getPlayer(ctx, "target");
                                            int amt = IntegerArgumentType.getInteger(ctx, "amount");

                                            if (from == to) {
                                                from.sendMessage(
                                                        Text.literal("You can’t pay yourself.")
                                                                .formatted(Formatting.RED),
                                                        false);
                                                return 0;
                                            }

                                            long bal = BalanceStore.get(from);
                                            if (bal < amt) {
                                                from.sendMessage(
                                                        Text.literal("Insufficient funds.")
                                                                .formatted(Formatting.RED),
                                                        false);
                                                return 0;
                                            }

                                            BalanceStore.subtract(from, amt, net.fugginbeenus.notchcurrency.economy.TransactionReason.PAY, "paid " + to.getName().getString());
                                            BalanceStore.add(to, amt, net.fugginbeenus.notchcurrency.economy.TransactionReason.PAY, "from " + from.getName().getString());

                                            NotchPackets.sendBalance(from, BalanceStore.get(from));
                                            NotchPackets.sendBalance(to, BalanceStore.get(to));

                                            from.sendMessage(
                                                    Text.literal("Paid " + amt + " ")
                                                            .append(NotchCurrency.coinIcon())
                                                            .append(Text.literal(" to " + to.getName().getString()))
                                                            .formatted(Formatting.GREEN),
                                                    false
                                            );

                                            to.sendMessage(
                                                    Text.literal(from.getName().getString() + " paid you " + amt + " ")
                                                            .append(NotchCurrency.coinIcon())
                                                            .formatted(Formatting.GREEN),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )
        );

    }
}
