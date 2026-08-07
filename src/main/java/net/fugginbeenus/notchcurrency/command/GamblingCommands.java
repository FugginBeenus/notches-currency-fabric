package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.fugginbeenus.notchcurrency.economy.gambling.CoinFlipManager;
import net.fugginbeenus.notchcurrency.economy.gambling.SlotMachineManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class GamblingCommands {

    private GamblingCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("slots")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayer();
                    if (p == null) { ctx.getSource().sendFailure(Component.literal("Run as a player.")); return 0; }
                    SlotMachineManager.openScreen(p);
                    return 1;
                }));

        dispatcher.register(Commands.literal("coinflip")
                .then(Commands.literal("heads")
                        .then(Commands.argument("bet", LongArgumentType.longArg(1))
                                .executes(ctx -> coinflip(ctx.getSource(), true, LongArgumentType.getLong(ctx, "bet")))))
                .then(Commands.literal("tails")
                        .then(Commands.argument("bet", LongArgumentType.longArg(1))
                                .executes(ctx -> coinflip(ctx.getSource(), false, LongArgumentType.getLong(ctx, "bet"))))));
    }

    private static int coinflip(CommandSourceStack src, boolean heads, long bet) {
        ServerPlayer p = src.getPlayer();
        if (p == null) { src.sendFailure(Component.literal("Run as a player.")); return 0; }
        CoinFlipManager.flipCommand(p, heads, bet);
        return 1;
    }
}
