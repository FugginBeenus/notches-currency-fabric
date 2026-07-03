package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.fugginbeenus.notchcurrency.core.BalanceState;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.economy.EconomyLeaderboard;
import net.fugginbeenus.notchcurrency.economy.EconomyLedger;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.fugginbeenus.notchcurrency.core.NotchCurrency.coinIcon;

/**
 * Admin economy commands ({@code /eco give|take|set|stats}) and the public
 * {@code /baltop} leaderboard. Visibility tooling for tuning the economy.
 */
public final class EcoCommands {

    private static final int TOP_LIMIT = 10;

    private EcoCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // ===== /baltop (everyone) =====
        dispatcher.register(
                CommandManager.literal("baltop")
                        .executes(ctx -> {
                            ServerCommandSource src = ctx.getSource();
                            for (Text line : EconomyLeaderboard.topLines(src.getServer(), TOP_LIMIT)) {
                                src.sendFeedback(() -> line, false);
                            }
                            return 1;
                        })
        );

        // ===== /receipts (everyone) — your recent transaction history =====
        dispatcher.register(
                CommandManager.literal("receipts")
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            if (p != null) net.fugginbeenus.notchcurrency.economy.ReceiptsScreenHandler.open(p);
                            return 1;
                        })
        );

        // ===== /eco (admin) =====
        dispatcher.register(
                CommandManager.literal("eco")
                        .requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.literal("give")
                                .then(CommandManager.argument("target", EntityArgumentType.player())
                                        .then(CommandManager.argument("amount", LongArgumentType.longArg(1))
                                                .executes(ctx -> adjust(ctx.getSource(),
                                                        EntityArgumentType.getPlayer(ctx, "target"),
                                                        LongArgumentType.getLong(ctx, "amount"), Mode.GIVE)))))
                        .then(CommandManager.literal("take")
                                .then(CommandManager.argument("target", EntityArgumentType.player())
                                        .then(CommandManager.argument("amount", LongArgumentType.longArg(1))
                                                .executes(ctx -> adjust(ctx.getSource(),
                                                        EntityArgumentType.getPlayer(ctx, "target"),
                                                        LongArgumentType.getLong(ctx, "amount"), Mode.TAKE)))))
                        .then(CommandManager.literal("set")
                                .then(CommandManager.argument("target", EntityArgumentType.player())
                                        .then(CommandManager.argument("amount", LongArgumentType.longArg(0))
                                                .executes(ctx -> adjust(ctx.getSource(),
                                                        EntityArgumentType.getPlayer(ctx, "target"),
                                                        LongArgumentType.getLong(ctx, "amount"), Mode.SET)))))
                        .then(CommandManager.literal("stats")
                                .executes(ctx -> stats(ctx.getSource())))
        );
    }

    private enum Mode { GIVE, TAKE, SET }

    private static int adjust(ServerCommandSource src, ServerPlayerEntity target, long amount, Mode mode) {
        String admin = src.getName();
        long newBal;
        String verb;
        switch (mode) {
            case GIVE -> { newBal = BalanceStore.add(target, amount, TransactionReason.ADMIN, "give by " + admin); verb = "Gave"; }
            case TAKE -> { newBal = BalanceStore.subtract(target, amount, TransactionReason.ADMIN, "take by " + admin); verb = "Took"; }
            default   -> { newBal = BalanceStore.set(target, amount, TransactionReason.ADMIN, "set by " + admin); verb = "Set"; }
        }
        NotchPackets.sendBalance(target, newBal);

        final long fNew = newBal;
        src.sendFeedback(() -> Text.literal(verb + " ")
                .append(Text.literal(amount + " ").formatted(Formatting.GOLD))
                .append(coinIcon())
                .append(Text.literal(" " + (mode == Mode.SET ? "for " : (mode == Mode.GIVE ? "to " : "from ")))
                        .formatted(Formatting.GREEN))
                .append(Text.literal(target.getName().getString()).formatted(Formatting.AQUA))
                .append(Text.literal(" (now " + fNew + ")").formatted(Formatting.GRAY)), true);
        return 1;
    }

    private static int stats(ServerCommandSource src) {
        MinecraftServer server = src.getServer();
        BalanceState state = BalanceState.get(server);
        long supply = state.totalSupply();
        int accounts = state.accountCount();
        long created = EconomyLedger.getSessionCreated();
        long destroyed = EconomyLedger.getSessionDestroyed();
        long net = created - destroyed;

        src.sendFeedback(() -> Text.literal("─── Economy Stats ───").formatted(Formatting.GOLD), false);
        src.sendFeedback(() -> Text.literal("Money supply: ").formatted(Formatting.GRAY)
                .append(Text.literal(supply + " ").formatted(Formatting.GOLD)).append(coinIcon()), false);
        src.sendFeedback(() -> Text.literal("Accounts: ").formatted(Formatting.GRAY)
                .append(Text.literal(String.valueOf(accounts)).formatted(Formatting.WHITE)), false);
        src.sendFeedback(() -> Text.literal("Since restart — created: ").formatted(Formatting.GRAY)
                .append(Text.literal(String.valueOf(created)).formatted(Formatting.GREEN))
                .append(Text.literal(", destroyed: ").formatted(Formatting.GRAY))
                .append(Text.literal(String.valueOf(destroyed)).formatted(Formatting.RED))
                .append(Text.literal(", net: ").formatted(Formatting.GRAY))
                .append(Text.literal(String.valueOf(net)).formatted(net >= 0 ? Formatting.GREEN : Formatting.RED)), false);
        if (net > 0) {
            src.sendFeedback(() -> Text.literal("⚠ Faucets are outpacing sinks — inflation risk.")
                    .formatted(Formatting.YELLOW), false);
        }
        return 1;
    }
}
