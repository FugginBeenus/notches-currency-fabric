package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fugginbeenus.notchcurrency.core.HeartState;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * Reading and setting extra hearts, and the rule about losing them.
 *
 * <p>The toggle is a world setting rather than a config file entry, because the config here is the
 * player's own and this is the server's rule. Somebody joining with a different file must not get
 * different hearts.
 */
public final class HeartCommands {

    private HeartCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hearts")
                .executes(ctx -> show(ctx.getSource()))
                .then(Commands.literal("loseondeath")
                        .requires(net.fugginbeenus.notchcurrency.compat.Perms::isOperator)
                        .executes(ctx -> {
                            HeartState state = HeartState.get(ctx.getSource().getServer());
                            ctx.getSource().sendSuccess(() -> Component.literal("Dying costs a heart: "
                                    + (state.losesOnDeath() ? "on" : "off")).withStyle(ChatFormatting.GRAY), false);
                            return 1;
                        })
                        .then(Commands.argument("enabled", BoolArgumentType.bool())
                                .executes(ctx -> {
                                    boolean on = BoolArgumentType.getBool(ctx, "enabled");
                                    HeartState.get(ctx.getSource().getServer()).setLosesOnDeath(on);
                                    ctx.getSource().sendSuccess(() -> Component.literal(on
                                            ? "Dying now costs a heart, down to none."
                                            : "Extra hearts now survive death.")
                                            .withStyle(ChatFormatting.GREEN), true);
                                    return 1;
                                })))
                .then(Commands.literal("set")
                        .requires(net.fugginbeenus.notchcurrency.compat.Perms::isOperator)
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("hearts", IntegerArgumentType.integer(0, HeartState.MAX_CRYSTALS))
                                        .executes(ctx -> {
                                            Collection<ServerPlayer> targets = EntityArgument.getPlayers(ctx, "targets");
                                            int hearts = IntegerArgumentType.getInteger(ctx, "hearts");
                                            HeartState state = HeartState.get(ctx.getSource().getServer());
                                            for (ServerPlayer t : targets) {
                                                state.set(t.getUUID(), hearts);
                                                HeartState.applyTo(t);
                                            }
                                            ctx.getSource().sendSuccess(() -> Component.literal("Set " + targets.size()
                                                    + " player(s) to " + hearts + " extra heart(s).")
                                                    .withStyle(ChatFormatting.GREEN), true);
                                            return 1;
                                        })))))
        ;
    }

    private static int show(CommandSourceStack src) {
        ServerPlayer p = src.getPlayer();
        if (p == null) { src.sendFailure(Component.literal("Run as a player.")); return 0; }
        HeartState state = HeartState.get(src.getServer());
        int have = state.count(p.getUUID());
        src.sendSuccess(() -> Component.literal(have + " extra heart" + (have == 1 ? "" : "s")
                        + " of " + HeartState.MAX_CRYSTALS
                        + (state.losesOnDeath() ? ". Dying costs one." : ". They survive death."))
                .withStyle(ChatFormatting.LIGHT_PURPLE), false);
        return 1;
    }
}
