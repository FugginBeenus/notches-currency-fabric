package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fugginbeenus.notchcurrency.npc.faction.Faction;
import net.fugginbeenus.notchcurrency.npc.faction.FactionManager;
import net.fugginbeenus.notchcurrency.npc.faction.FactionState;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class FactionCommands {

    private FactionCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("faction")
                .executes(ctx -> list(ctx.getSource()))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("leave").executes(ctx -> leave(ctx.getSource())))
                .then(Commands.literal("join")
                        .then(Commands.argument("faction", StringArgumentType.word())
                                .executes(ctx -> join(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "faction")))))
                .then(Commands.literal("disband")
                        .then(Commands.argument("faction", StringArgumentType.word())
                                .executes(ctx -> disband(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "faction")))))
                .then(Commands.literal("rename")
                        .then(Commands.argument("faction", StringArgumentType.word())
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> rename(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "faction"),
                                                StringArgumentType.getString(ctx, "name"))))))
                .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> create(ctx.getSource(),
                                        StringArgumentType.getString(ctx, "name"))))));
    }

    private static int list(CommandSourceStack source) {
        FactionState state = FactionState.get(source.getServer());
        var all = state.all();
        if (all.isEmpty()) {
            source.sendSuccess(() -> Component.literal("No factions yet. Set an NPC's role to Recruiter to start one.")
                    .withStyle(ChatFormatting.GRAY), false);
            return 1;
        }
        source.sendSuccess(() -> Component.literal("Factions:").withStyle(ChatFormatting.GOLD), false);
        for (Faction f : all) {
            int members = state.memberCount(f.id());
            source.sendSuccess(() -> Component.literal(" • ")
                    .append(Component.literal(f.displayName()).withStyle(f.color()))
                    .append(Component.literal(" (" + f.id() + ") - "
                            + (members == 1 ? "1 member" : members + " members")).withStyle(ChatFormatting.GRAY)), false);
        }
        return 1;
    }

    private static int create(CommandSourceStack source, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = source.getPlayerOrException();
        return FactionManager.create(sp, name, ChatFormatting.WHITE) == null ? 0 : 1;
    }

    private static int join(CommandSourceStack source, String factionId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        FactionManager.join(source.getPlayerOrException(), factionId);
        return 1;
    }

    private static int leave(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        FactionManager.leave(source.getPlayerOrException());
        return 1;
    }

    private static int disband(CommandSourceStack source, String factionId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return FactionManager.disband(source.getPlayerOrException(), factionId) ? 1 : 0;
    }

    private static int rename(CommandSourceStack source, String factionId, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = source.getPlayerOrException();
        FactionState state = FactionState.get(sp.serverLevel());
        Faction faction = state.get(factionId);
        if (faction == null) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("No faction by that name.").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (!FactionManager.canManage(sp, faction)) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("That isn't your faction.").withStyle(ChatFormatting.RED));
            return 0;
        }
        faction.setDisplayName(name);
        state.touch();
        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Renamed to ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(faction.displayName()).withStyle(faction.color())));
        return 1;
    }
}
