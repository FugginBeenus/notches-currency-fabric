package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fugginbeenus.notchcurrency.economy.raffle.RaffleManager;
import net.fugginbeenus.notchcurrency.economy.raffle.RaffleState;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class RaffleCommands {

    private RaffleCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("raffle")
                .executes(ctx -> info(ctx.getSource()))
                .then(Commands.literal("info")
                        .executes(ctx -> info(ctx.getSource())))
                .then(Commands.literal("buy")
                        .then(Commands.argument("qty", IntegerArgumentType.integer(1, 256))
                                .executes(ctx -> buy(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "qty")))))
                .then(Commands.literal("redeem")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            if (p == null) { ctx.getSource().sendFailure(Component.literal("Run as a player.")); return 0; }
                            RaffleManager.redeemTicket(p);
                            return 1;
                        }))
                .then(Commands.literal("claim")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            if (p == null) { ctx.getSource().sendFailure(Component.literal("Run as a player.")); return 0; }
                            RaffleManager.claim(p);
                            return 1;
                        }))
                .then(Commands.literal("draw")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> {
                            boolean drawn = RaffleManager.draw(ctx.getSource().getServer(), true);
                            ctx.getSource().sendSuccess(() -> Component.literal(drawn
                                    ? "Raffle drawn." : "No tickets - nothing to draw.").withStyle(ChatFormatting.YELLOW), true);
                            return drawn ? 1 : 0;
                        }))
                .then(Commands.literal("admin")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            if (p == null) { ctx.getSource().sendFailure(Component.literal("Run as a player.")); return 0; }
                            net.fugginbeenus.notchcurrency.economy.raffle.RaffleAdminScreenHandler.open(p);
                            return 1;
                        }))
                .then(Commands.literal("setprize")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            if (p == null) { ctx.getSource().sendFailure(Component.literal("Run as a player (uses your held item).")); return 0; }
                            RaffleManager.setPrize(p);
                            return 1;
                        }))
                .then(Commands.literal("clearprize")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            if (p == null) { ctx.getSource().sendFailure(Component.literal("Run as a player.")); return 0; }
                            RaffleManager.clearPrize(p);
                            return 1;
                        }))
                .then(Commands.literal("reset")
                        .requires(s -> s.hasPermission(2))
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            if (p != null) {
                                RaffleManager.resetAndReturn(p); // wipe + return escrowed prize to the admin
                            } else {
                                RaffleState.get(ctx.getSource().getServer()).resetRound();
                                RaffleManager.refreshAllOnline(ctx.getSource().getServer());
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal("Raffle round wiped (entries & pot cleared, prize returned).")
                                    .withStyle(ChatFormatting.YELLOW), true);
                            return 1;
                        }))
        );
    }

    private static int buy(CommandSourceStack src, int qty) {
        ServerPlayer p = src.getPlayer();
        if (p == null) { src.sendFailure(Component.literal("Run as a player.")); return 0; }
        RaffleManager.buyTicket(p, qty);
        return 1;
    }

    private static int info(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        if (p == null) { src.sendFailure(Component.literal("Run as a player.")); return 0; }
        RaffleManager.openScreen(p);
        return 1;
    }
}
