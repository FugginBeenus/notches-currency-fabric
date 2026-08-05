package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fugginbeenus.notchcurrency.economy.raffle.RaffleManager;
import net.fugginbeenus.notchcurrency.economy.raffle.RaffleState;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * {@code /raffle}: opens the raffle screen ({@code info}); players buy tickets ({@code buy}),
 * redeem an old losing ticket for free entries ({@code redeem}) and claim wins ({@code claim}).
 * Ops draw, reset, or open the setup GUI ({@code draw}, {@code reset}, {@code admin}). The
 * screen buttons back these same actions.
 */
public final class RaffleCommands {

    private RaffleCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("raffle")
                .executes(ctx -> info(ctx.getSource()))
                .then(CommandManager.literal("info")
                        .executes(ctx -> info(ctx.getSource())))
                .then(CommandManager.literal("buy")
                        .then(CommandManager.argument("qty", IntegerArgumentType.integer(1, 256))
                                .executes(ctx -> buy(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "qty")))))
                .then(CommandManager.literal("redeem")
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            if (p == null) { ctx.getSource().sendError(Text.literal("Run as a player.")); return 0; }
                            RaffleManager.redeemTicket(p);
                            return 1;
                        }))
                .then(CommandManager.literal("claim")
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            if (p == null) { ctx.getSource().sendError(Text.literal("Run as a player.")); return 0; }
                            RaffleManager.claim(p);
                            return 1;
                        }))
                .then(CommandManager.literal("draw")
                        .requires(s -> s.hasPermissionLevel(2))
                        .executes(ctx -> {
                            boolean drawn = RaffleManager.draw(ctx.getSource().getServer(), true);
                            ctx.getSource().sendFeedback(() -> Text.literal(drawn
                                    ? "Raffle drawn." : "No tickets - nothing to draw.").formatted(Formatting.YELLOW), true);
                            return drawn ? 1 : 0;
                        }))
                .then(CommandManager.literal("admin")
                        .requires(s -> s.hasPermissionLevel(2))
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            if (p == null) { ctx.getSource().sendError(Text.literal("Run as a player.")); return 0; }
                            net.fugginbeenus.notchcurrency.economy.raffle.RaffleAdminScreenHandler.open(p);
                            return 1;
                        }))
                .then(CommandManager.literal("setprize")
                        .requires(s -> s.hasPermissionLevel(2))
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            if (p == null) { ctx.getSource().sendError(Text.literal("Run as a player (uses your held item).")); return 0; }
                            RaffleManager.setPrize(p);
                            return 1;
                        }))
                .then(CommandManager.literal("clearprize")
                        .requires(s -> s.hasPermissionLevel(2))
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            if (p == null) { ctx.getSource().sendError(Text.literal("Run as a player.")); return 0; }
                            RaffleManager.clearPrize(p);
                            return 1;
                        }))
                .then(CommandManager.literal("reset")
                        .requires(s -> s.hasPermissionLevel(2))
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            if (p != null) {
                                RaffleManager.resetAndReturn(p); // wipe + return escrowed prize to the admin
                            } else {
                                RaffleState.get(ctx.getSource().getServer()).resetRound();
                                RaffleManager.refreshAllOnline(ctx.getSource().getServer());
                            }
                            ctx.getSource().sendFeedback(() -> Text.literal("Raffle round wiped (entries & pot cleared, prize returned).")
                                    .formatted(Formatting.YELLOW), true);
                            return 1;
                        }))
        );
    }

    private static int buy(ServerCommandSource src, int qty) {
        ServerPlayerEntity p = src.getPlayer();
        if (p == null) { src.sendError(Text.literal("Run as a player.")); return 0; }
        RaffleManager.buyTicket(p, qty);
        return 1;
    }

    private static int info(ServerCommandSource src) {
        ServerPlayerEntity p = src.getPlayer();
        if (p == null) { src.sendError(Text.literal("Run as a player.")); return 0; }
        RaffleManager.openScreen(p);
        return 1;
    }
}
