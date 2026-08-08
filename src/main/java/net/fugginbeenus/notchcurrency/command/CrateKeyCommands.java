package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fugginbeenus.notchcurrency.economy.crate.CrateManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.Collection;

public final class CrateKeyCommands {

    private CrateKeyCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("crate")
                .then(Commands.literal("buykey")
                        .executes(ctx -> buyKey(ctx.getSource(), 1))
                        .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> buyKey(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount")))))
                .then(Commands.literal("odds")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p == null) { ctx.getSource().sendFailure(Component.literal("Run as a player.")); return 0; }
                                    CrateManager.showOdds(p, StringArgumentType.getString(ctx, "type"));
                                    return 1;
                                })))
                .then(Commands.literal("givekey")
                        .requires(net.fugginbeenus.notchcurrency.compat.Perms::isOperator)
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("amount", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> {
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            for (ServerPlayer t : targets) CrateManager.giveKeys(t, amount);
                                            ctx.getSource().sendSuccess(() -> Component.literal("Gave " + amount + " key(s) to "
                                                    + targets.size() + " player(s).").withStyle(ChatFormatting.GREEN), true);
                                            return 1;
                                        }))))
        );
    }

    private static int buyKey(CommandSourceStack src, int amount) {
        ServerPlayer p = src.getPlayer();
        if (p == null) { src.sendFailure(Component.literal("Run as a player.")); return 0; }
        CrateManager.buyKey(p, amount);
        return 1;
    }
}
