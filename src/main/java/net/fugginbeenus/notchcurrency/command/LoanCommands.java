package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.fugginbeenus.notchcurrency.economy.loan.LoanManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class LoanCommands {

    private LoanCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("loan")
                .executes(ctx -> {
                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                    if (p == null) { ctx.getSource().sendError(Text.literal("Run as a player.")); return 0; }
                    LoanManager.openScreen(p);
                    return 1;
                })
                .then(CommandManager.literal("borrow")
                        .then(CommandManager.argument("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    if (p == null) { ctx.getSource().sendError(Text.literal("Run as a player.")); return 0; }
                                    LoanManager.borrow(p, LongArgumentType.getLong(ctx, "amount"));
                                    return 1;
                                })))
                .then(CommandManager.literal("repay")
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            if (p == null) { ctx.getSource().sendError(Text.literal("Run as a player.")); return 0; }
                            LoanManager.repay(p, 0);
                            return 1;
                        })
                        .then(CommandManager.argument("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    if (p == null) { ctx.getSource().sendError(Text.literal("Run as a player.")); return 0; }
                                    LoanManager.repay(p, LongArgumentType.getLong(ctx, "amount"));
                                    return 1;
                                })))
        );
    }
}
