package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.fugginbeenus.notchcurrency.economy.loan.LoanManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class LoanCommands {

    private LoanCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("loan")
                .executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayer();
                    if (p == null) { ctx.getSource().sendFailure(Component.literal("Run as a player.")); return 0; }
                    LoanManager.openScreen(p);
                    return 1;
                })
                .then(Commands.literal("borrow")
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p == null) { ctx.getSource().sendFailure(Component.literal("Run as a player.")); return 0; }
                                    LoanManager.borrow(p, LongArgumentType.getLong(ctx, "amount"));
                                    return 1;
                                })))
                .then(Commands.literal("repay")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            if (p == null) { ctx.getSource().sendFailure(Component.literal("Run as a player.")); return 0; }
                            LoanManager.repay(p, 0);
                            return 1;
                        })
                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p == null) { ctx.getSource().sendFailure(Component.literal("Run as a player.")); return 0; }
                                    LoanManager.repay(p, LongArgumentType.getLong(ctx, "amount"));
                                    return 1;
                                })))
        );
    }
}
