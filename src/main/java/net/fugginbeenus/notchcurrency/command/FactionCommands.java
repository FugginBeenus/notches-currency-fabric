package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fugginbeenus.notchcurrency.npc.faction.Faction;
import net.fugginbeenus.notchcurrency.npc.faction.FactionManager;
import net.fugginbeenus.notchcurrency.npc.faction.FactionState;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * {@code /faction} — the safety net behind the Recruiter NPC.
 *
 * <p>Everything here can also be done in-world at a recruiter, which is how most players will do it.
 * This exists so nobody is ever stranded: if a founder's recruiter is destroyed they can still see,
 * rename and disband what they own, and admins can sort out anything on the server without hunting
 * down an entity.
 *
 * <p>Unlike the rest of the mod's commands this isn't op-only — a founder needs to reach their own
 * faction. Every subcommand still re-checks who may touch what.
 */
public final class FactionCommands {

    private FactionCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("faction")
                .executes(ctx -> list(ctx.getSource()))
                .then(CommandManager.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(CommandManager.literal("leave").executes(ctx -> leave(ctx.getSource())))
                .then(CommandManager.literal("join")
                        .then(CommandManager.argument("faction", StringArgumentType.word())
                                .executes(ctx -> join(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "faction")))))
                .then(CommandManager.literal("disband")
                        .then(CommandManager.argument("faction", StringArgumentType.word())
                                .executes(ctx -> disband(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "faction")))))
                .then(CommandManager.literal("rename")
                        .then(CommandManager.argument("faction", StringArgumentType.word())
                                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> rename(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "faction"),
                                                StringArgumentType.getString(ctx, "name"))))))
                // Creating is offered here too, but the recruiter is the friendlier road in.
                .then(CommandManager.literal("create")
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> create(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"))))));
    }

    private static int list(ServerCommandSource source) {
        FactionState state = FactionState.get(source.getServer());
        var all = state.all();
        if (all.isEmpty()) {
            source.sendFeedback(() -> Text.literal("No factions yet. Set an NPC's role to Recruiter to start one.")
                    .formatted(Formatting.GRAY), false);
            return 1;
        }
        source.sendFeedback(() -> Text.literal("Factions:").formatted(Formatting.GOLD), false);
        for (Faction f : all) {
            int members = state.memberCount(f.id());
            source.sendFeedback(() -> Text.literal(" • ")
                    .append(Text.literal(f.displayName()).formatted(f.color()))
                    .append(Text.literal(" (" + f.id() + ") — "
                            + (members == 1 ? "1 member" : members + " members")).formatted(Formatting.GRAY)), false);
        }
        return 1;
    }

    private static int create(ServerCommandSource source, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity sp = source.getPlayerOrThrow();
        return FactionManager.create(sp, name, Formatting.WHITE) == null ? 0 : 1;
    }

    private static int join(ServerCommandSource source, String factionId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        FactionManager.join(source.getPlayerOrThrow(), factionId);
        return 1;
    }

    private static int leave(ServerCommandSource source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        FactionManager.leave(source.getPlayerOrThrow());
        return 1;
    }

    private static int disband(ServerCommandSource source, String factionId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return FactionManager.disband(source.getPlayerOrThrow(), factionId) ? 1 : 0;
    }

    private static int rename(ServerCommandSource source, String factionId, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayerEntity sp = source.getPlayerOrThrow();
        FactionState state = FactionState.get(sp.getServerWorld());
        Faction faction = state.get(factionId);
        if (faction == null) {
            sp.sendMessage(Text.literal("No faction by that name.").formatted(Formatting.RED), false);
            return 0;
        }
        if (!FactionManager.canManage(sp, faction)) {
            sp.sendMessage(Text.literal("That isn't your faction.").formatted(Formatting.RED), false);
            return 0;
        }
        // The id never changes — NPCs point at it, so renaming is display-only on purpose.
        faction.setDisplayName(name);
        state.touch();
        sp.sendMessage(Text.literal("Renamed to ")
                .formatted(Formatting.GREEN)
                .append(Text.literal(faction.displayName()).formatted(faction.color())), false);
        return 1;
    }
}
