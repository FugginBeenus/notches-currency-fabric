package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.fugginbeenus.notchcurrency.economy.gambling.CoinFlipManager;
import net.fugginbeenus.notchcurrency.economy.gambling.SlotMachineManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * {@code /slots} opens the slot machine. {@code /coinflip <heads|tails> <bet>} does a quick,
 * block-less coin flip that resolves instantly.
 */
public final class GamblingCommands {

    private GamblingCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("slots")
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    if (p == null) { ctx.getSource().sendError(Text.literal("Run as a player.")); return 0; }
                    SlotMachineManager.openScreen(p);
                    return 1;
                }));

        dispatcher.register(CommandManager.literal("coinflip")
                .then(CommandManager.literal("heads")
                        .then(CommandManager.argument("bet", LongArgumentType.longArg(1))
                                .executes(ctx -> coinflip(ctx.getSource(), true, LongArgumentType.getLong(ctx, "bet")))))
                .then(CommandManager.literal("tails")
                        .then(CommandManager.argument("bet", LongArgumentType.longArg(1))
                                .executes(ctx -> coinflip(ctx.getSource(), false, LongArgumentType.getLong(ctx, "bet"))))));
    }

    private static int coinflip(ServerCommandSource src, boolean heads, long bet) {
        ServerPlayerEntity p = src.getPlayer();
        if (p == null) { src.sendError(Text.literal("Run as a player.")); return 0; }
        CoinFlipManager.flipCommand(p, heads, bet);
        return 1;
    }
}
