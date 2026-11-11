package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.fugginbeenus.notchcurrency.core.BalanceState;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.economy.EconomyLeaderboard;
import net.fugginbeenus.notchcurrency.economy.EconomyLedger;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.economy.Treasury;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static net.fugginbeenus.notchcurrency.core.NotchCurrency.coinIcon;

public final class EcoCommands {

    private static final int TOP_LIMIT = 10;

    private EcoCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("baltop").requires(net.fugginbeenus.notchcurrency.compat.Perms.command("baltop"))
                        .executes(ctx -> {
                            CommandSourceStack src = ctx.getSource();
                            for (Component line : EconomyLeaderboard.topLines(src.getServer(), TOP_LIMIT)) {
                                src.sendSuccess(() -> line, false);
                            }
                            return 1;
                        })
        );

        dispatcher.register(
                Commands.literal("receipts").requires(net.fugginbeenus.notchcurrency.compat.Perms.command("receipts"))
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            if (p != null) net.fugginbeenus.notchcurrency.economy.ReceiptsScreenHandler.open(p);
                            return 1;
                        })
        );

        dispatcher.register(
                Commands.literal("eco")
                        .requires(net.fugginbeenus.notchcurrency.compat.Perms.admin(net.fugginbeenus.notchcurrency.compat.Perms.ECO))
                        .then(Commands.literal("give")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                                .executes(ctx -> adjust(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "target"),
                                                        LongArgumentType.getLong(ctx, "amount"), Mode.GIVE)))))
                        .then(Commands.literal("take")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                                .executes(ctx -> adjust(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "target"),
                                                        LongArgumentType.getLong(ctx, "amount"), Mode.TAKE)))))
                        .then(Commands.literal("set")
                                .then(Commands.argument("target", EntityArgument.player())
                                        .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                                .executes(ctx -> adjust(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "target"),
                                                        LongArgumentType.getLong(ctx, "amount"), Mode.SET)))))
                        .then(Commands.literal("stats")
                                .executes(ctx -> stats(ctx.getSource())))
                        .then(Commands.literal("treasury")
                                .executes(ctx -> treasury(ctx.getSource()))
                                .then(Commands.literal("pay")
                                        .then(Commands.argument("target", EntityArgument.player())
                                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                                        .executes(ctx -> treasuryPay(ctx.getSource(),
                                                                EntityArgument.getPlayer(ctx, "target"),
                                                                LongArgumentType.getLong(ctx, "amount"))))))
                                .then(Commands.literal("set")
                                        .then(Commands.argument("amount", LongArgumentType.longArg(0))
                                                .executes(ctx -> treasurySet(ctx.getSource(),
                                                        LongArgumentType.getLong(ctx, "amount"))))))
        );
    }

    private static int treasury(CommandSourceStack src) {
        long held = Treasury.balance(src.getServer());
        src.sendSuccess(() -> Component.literal("Treasury: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(held + " ").withStyle(ChatFormatting.GOLD))
                .append(coinIcon()), false);
        if (!NotchConfigIO.get().treasury.enabled) {
            src.sendSuccess(() -> Component.literal("Collecting is off. Turn on Treasury in the config.")
                    .withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static int treasuryPay(CommandSourceStack src, ServerPlayer target, long amount) {
        if (!Treasury.pay(target, amount, "treasury payment by " + src.getTextName())) {
            src.sendFailure(Component.literal("The treasury only holds " + Treasury.balance(src.getServer()) + "."));
            return 0;
        }
        long left = Treasury.balance(src.getServer());
        src.sendSuccess(() -> Component.literal("Paid ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(amount + " ").withStyle(ChatFormatting.GOLD))
                .append(coinIcon())
                .append(Component.literal(" to ").withStyle(ChatFormatting.GREEN))
                .append(Component.literal(target.getName().getString()).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" from the treasury (" + left + " left)").withStyle(ChatFormatting.GRAY)), true);
        return 1;
    }

    private static int treasurySet(CommandSourceStack src, long amount) {
        long now = Treasury.set(src.getServer(), amount, "set by " + src.getTextName());
        src.sendSuccess(() -> Component.literal("Set the treasury to ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(now + " ").withStyle(ChatFormatting.GOLD))
                .append(coinIcon()), true);
        return 1;
    }

    private enum Mode { GIVE, TAKE, SET }

    private static int adjust(CommandSourceStack src, ServerPlayer target, long amount, Mode mode) {
        String admin = src.getTextName();
        long newBal;
        String verb;
        switch (mode) {
            case GIVE -> { newBal = BalanceStore.add(target, amount, TransactionReason.ADMIN, "give by " + admin); verb = "Gave"; }
            case TAKE -> { newBal = BalanceStore.subtract(target, amount, TransactionReason.ADMIN, "take by " + admin); verb = "Took"; }
            default   -> { newBal = BalanceStore.set(target, amount, TransactionReason.ADMIN, "set by " + admin); verb = "Set"; }
        }
        NotchPackets.sendBalance(target, newBal);

        final long fNew = newBal;
        src.sendSuccess(() -> Component.literal(verb + " ")
                .append(Component.literal(amount + " ").withStyle(ChatFormatting.GOLD))
                .append(coinIcon())
                .append(Component.literal(" " + (mode == Mode.SET ? "for " : (mode == Mode.GIVE ? "to " : "from ")))
                        .withStyle(ChatFormatting.GREEN))
                .append(Component.literal(target.getName().getString()).withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" (now " + fNew + ")").withStyle(ChatFormatting.GRAY)), true);
        return 1;
    }

    private static int stats(CommandSourceStack src) {
        MinecraftServer server = src.getServer();
        BalanceState state = BalanceState.get(server);
        long supply = state.totalSupply();
        int accounts = state.accountCount();
        long created = EconomyLedger.getSessionCreated();
        long destroyed = EconomyLedger.getSessionDestroyed();
        long net = created - destroyed;

        src.sendSuccess(() -> Component.literal("─── Economy Stats ───").withStyle(ChatFormatting.GOLD), false);
        src.sendSuccess(() -> Component.literal("Money supply: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(supply + " ").withStyle(ChatFormatting.GOLD)).append(coinIcon()), false);
        long held = Treasury.balance(server);
        if (held > 0 || NotchConfigIO.get().treasury.enabled) {
            src.sendSuccess(() -> Component.literal("Treasury: ").withStyle(ChatFormatting.GRAY)
                    .append(Component.literal(held + " ").withStyle(ChatFormatting.GOLD)).append(coinIcon()), false);
        }
        src.sendSuccess(() -> Component.literal("Accounts: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(accounts)).withStyle(ChatFormatting.WHITE)), false);
        src.sendSuccess(() -> Component.literal("Since restart - created: ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(String.valueOf(created)).withStyle(ChatFormatting.GREEN))
                .append(Component.literal(", destroyed: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(destroyed)).withStyle(ChatFormatting.RED))
                .append(Component.literal(", net: ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(String.valueOf(net)).withStyle(net >= 0 ? ChatFormatting.GREEN : ChatFormatting.RED)), false);
        if (net > 0) {
            src.sendSuccess(() -> Component.literal("⚠ Faucets are outpacing sinks - inflation risk.")
                    .withStyle(ChatFormatting.YELLOW), false);
        }
        return 1;
    }
}
