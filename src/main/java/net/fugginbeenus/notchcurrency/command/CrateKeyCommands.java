package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fugginbeenus.notchcurrency.economy.crate.CrateManager;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Collection;

/**
 * {@code /crate}: players buy keys ({@code buykey}) and peek loot odds ({@code odds}); ops hand
 * out keys ({@code givekey}). Crates themselves are opened by right-clicking the crate block.
 */
public final class CrateKeyCommands {

    private CrateKeyCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("crate")
                .then(CommandManager.literal("buykey")
                        .executes(ctx -> buyKey(ctx.getSource(), 1))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> buyKey(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "amount")))))
                .then(CommandManager.literal("odds")
                        .then(CommandManager.argument("type", StringArgumentType.word())
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    if (p == null) { ctx.getSource().sendError(Text.literal("Run as a player.")); return 0; }
                                    CrateManager.showOdds(p, StringArgumentType.getString(ctx, "type"));
                                    return 1;
                                })))
                .then(CommandManager.literal("givekey")
                        .requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.argument("targets", EntityArgumentType.players())
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> {
                                            Collection<ServerPlayerEntity> targets = EntityArgumentType.getPlayers(ctx, "targets");
                                            int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                            for (ServerPlayerEntity t : targets) CrateManager.giveKeys(t, amount);
                                            ctx.getSource().sendFeedback(() -> Text.literal("Gave " + amount + " key(s) to "
                                                    + targets.size() + " player(s).").formatted(Formatting.GREEN), true);
                                            return 1;
                                        }))))
        );
    }

    private static int buyKey(ServerCommandSource src, int amount) {
        ServerPlayerEntity p = src.getPlayer();
        if (p == null) { src.sendError(Text.literal("Run as a player.")); return 0; }
        CrateManager.buyKey(p, amount);
        return 1;
    }
}
