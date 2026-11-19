package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.fugginbeenus.notchcurrency.economy.FactionBank;
import net.fugginbeenus.notchcurrency.npc.faction.Faction;
import net.fugginbeenus.notchcurrency.npc.faction.FactionManager;
import net.fugginbeenus.notchcurrency.npc.faction.FactionState;
import net.fugginbeenus.notchcurrency.npc.faction.FactionText;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class FactionCommands {

    private FactionCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("faction").requires(net.fugginbeenus.notchcurrency.compat.Perms.command("faction"))
                .executes(ctx -> list(ctx.getSource()))
                .then(Commands.literal("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.literal("leave").executes(ctx -> leave(ctx.getSource())))
                .then(Commands.literal("join")
                        .then(Commands.argument("faction", StringArgumentType.word())
                                .suggests(FactionCommands::suggestGroups)
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
                                        StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("bank")
                        .executes(ctx -> bankShow(ctx.getSource()))
                        .then(Commands.literal("deposit")
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .executes(ctx -> bankMove(ctx.getSource(), true,
                                                LongArgumentType.getLong(ctx, "amount")))))
                        .then(Commands.literal("withdraw")
                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                        .executes(ctx -> bankMove(ctx.getSource(), false,
                                                LongArgumentType.getLong(ctx, "amount")))))
                        .then(Commands.literal("pay")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                                .executes(ctx -> bankPay(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        LongArgumentType.getLong(ctx, "amount")))))))
                .then(Commands.literal("house")
                        .executes(ctx -> houseList(ctx.getSource()))
                        .then(Commands.literal("list").executes(ctx -> houseList(ctx.getSource())))
                        .then(Commands.literal("join")
                                .then(Commands.argument("house", StringArgumentType.word())
                                        .suggests(FactionCommands::suggestHouses)
                                        .executes(ctx -> houseJoin(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "house")))))
                        .then(Commands.literal("leave")
                                .executes(ctx -> houseLeave(ctx.getSource())))
                        .then(Commands.literal("bank")
                                .executes(ctx -> houseBankShow(ctx.getSource()))
                                .then(Commands.literal("deposit")
                                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                                .executes(ctx -> houseBankMove(ctx.getSource(), true,
                                                        LongArgumentType.getLong(ctx, "amount")))))
                                .then(Commands.literal("withdraw")
                                        .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                                .executes(ctx -> houseBankMove(ctx.getSource(), false,
                                                        LongArgumentType.getLong(ctx, "amount"))))))
                        .then(Commands.literal("create")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> houseCreate(ctx.getSource(), null,
                                                StringArgumentType.getString(ctx, "name")))))
                        .then(Commands.literal("leader")
                                .then(Commands.argument("house", StringArgumentType.word())
                                        .suggests(FactionCommands::suggestHouses)
                                        .then(Commands.literal("none")
                                                .executes(ctx -> houseLeader(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "house"), null)))
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(ctx -> houseLeader(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "house"),
                                                        EntityArgument.getPlayer(ctx, "player"))))))
                        .then(Commands.literal("disband")
                                .then(Commands.argument("house", StringArgumentType.word())
                                        .suggests(FactionCommands::suggestHouses)
                                        .executes(ctx -> houseDisband(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "house"))))))
                .then(Commands.literal("admin")
                        .requires(net.fugginbeenus.notchcurrency.compat.Perms.admin(net.fugginbeenus.notchcurrency.compat.Perms.FACTION))
                        .then(Commands.literal("house")
                                .then(Commands.literal("create")
                                        .then(Commands.argument("faction", StringArgumentType.word())
                                                .suggests(FactionCommands::suggestFactions)
                                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                                        .executes(ctx -> houseCreate(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "faction"),
                                                                StringArgumentType.getString(ctx, "name")))))))
                        .then(Commands.literal("bank")
                                .then(Commands.argument("faction", StringArgumentType.word())
                                        .executes(ctx -> adminBank(ctx.getSource(),
                                                StringArgumentType.getString(ctx, "faction"), 0L))
                                        .then(Commands.literal("add")
                                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                                        .executes(ctx -> adminBank(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "faction"),
                                                                LongArgumentType.getLong(ctx, "amount")))))
                                        .then(Commands.literal("take")
                                                .then(Commands.argument("amount", LongArgumentType.longArg(1))
                                                        .executes(ctx -> adminBank(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "faction"),
                                                                -LongArgumentType.getLong(ctx, "amount")))))))
                        .then(Commands.literal("move")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .then(Commands.argument("faction", StringArgumentType.word())
                                                .suggests(FactionCommands::suggestGroups)
                                                .executes(ctx -> adminMove(ctx.getSource(),
                                                        EntityArgument.getPlayer(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "faction"))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("player", EntityArgument.player())
                                        .executes(ctx -> adminRemove(ctx.getSource(),
                                                EntityArgument.getPlayer(ctx, "player")))))));
    }

    private static CompletableFuture<Suggestions> suggestFactions(CommandContext<CommandSourceStack> ctx,
                                                                  SuggestionsBuilder builder) {
        List<String> ids = new ArrayList<>();
        for (Faction f : FactionState.get(ctx.getSource().getServer()).all()) ids.add(f.id());
        return SharedSuggestionProvider.suggest(ids, builder);
    }

    private static CompletableFuture<Suggestions> suggestHouses(CommandContext<CommandSourceStack> ctx,
                                                                SuggestionsBuilder builder) {
        List<String> ids = new ArrayList<>();
        for (Faction h : FactionState.get(ctx.getSource().getServer()).allHouses()) ids.add(h.id());
        return SharedSuggestionProvider.suggest(ids, builder);
    }

    private static CompletableFuture<Suggestions> suggestGroups(CommandContext<CommandSourceStack> ctx,
                                                                SuggestionsBuilder builder) {
        FactionState state = FactionState.get(ctx.getSource().getServer());
        List<String> ids = new ArrayList<>();
        for (Faction f : net.fugginbeenus.notchcurrency.npc.faction.FactionBoards.pickerList(state, state.all())) {
            ids.add(f.id());
        }
        return SharedSuggestionProvider.suggest(ids, builder);
    }

    private static int houseList(CommandSourceStack source) {
        FactionState state = FactionState.get(source.getServer());
        ServerPlayer sp = source.getPlayer();
        Faction mine = sp == null ? null : state.factionOf(sp.getUUID());
        List<Faction> factions = mine != null ? List.of(mine) : state.all();
        boolean any = false;
        for (Faction f : factions) {
            List<Faction> houses = state.housesOf(f.id());
            if (houses.isEmpty()) continue;
            any = true;
            source.sendSuccess(() -> Component.literal("Houses of ").withStyle(ChatFormatting.GOLD)
                    .append(Component.literal(f.displayName()).withStyle(f.color()))
                    .append(Component.literal(":").withStyle(ChatFormatting.GOLD)), false);
            for (Faction h : houses) houseLine(source, state, h, " • ");
        }
        if (!any) {
            String text = mine != null ? mine.displayName() + " has no Houses yet." : "No Houses yet.";
            source.sendSuccess(() -> Component.literal(text).withStyle(ChatFormatting.GRAY), false);
        }
        return 1;
    }

    private static void houseLine(CommandSourceStack source, FactionState state, Faction h, String bullet) {
        int members = state.memberCount(h.id());
        String leader = h.founder() == null ? "no leader"
                : "led by " + net.fugginbeenus.notchcurrency.compat.Profiles.nameOf(source.getServer(), h.founder()).orElse("?");
        source.sendSuccess(() -> Component.literal(bullet)
                .append(Component.literal(h.displayName()).withStyle(h.color()))
                .append(Component.literal(" (" + h.id() + ") - " + (members == 1 ? "1 member" : members + " members")
                        + ", " + leader).withStyle(ChatFormatting.GRAY)), false);
    }

    private static int houseJoin(CommandSourceStack source, String houseId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = source.getPlayerOrException();
        Faction house = FactionState.get(source.getServer()).get(houseId);
        if (house == null || !house.isHouse()) {
            source.sendFailure(Component.literal("No House by that name."));
            return 0;
        }
        FactionManager.join(sp, house.id());
        return 1;
    }

    private static int houseLeave(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        FactionManager.leaveHouse(source.getPlayerOrException());
        return 1;
    }

    private static int houseCreate(CommandSourceStack source, String factionId, String name) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = source.getPlayerOrException();
        String id = factionId;
        if (id == null) {
            id = FactionState.get(source.getServer()).factionIdOf(sp.getUUID());
            if (id == null) {
                source.sendFailure(Component.literal("You're not in a faction."));
                return 0;
            }
        }
        return FactionManager.createHouse(sp, id, name) == null ? 0 : 1;
    }

    private static int houseLeader(CommandSourceStack source, String houseId, ServerPlayer target) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        return FactionManager.setLeader(source.getPlayerOrException(), houseId,
                target == null ? null : target.getUUID()) ? 1 : 0;
    }

    private static int houseDisband(CommandSourceStack source, String houseId) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer sp = source.getPlayerOrException();
        if (!Faction.isHouseId(houseId)) {
            source.sendFailure(Component.literal("No House by that name."));
            return 0;
        }
        return FactionManager.disbandHouse(sp, houseId) ? 1 : 0;
    }

    private static Faction ownHouse(CommandSourceStack source) {
        ServerPlayer sp = source.getPlayer();
        if (sp == null) return null;
        if (!FactionBank.enabled()) {
            source.sendFailure(Component.literal("Faction banks are off on this server."));
            return null;
        }
        Faction house = FactionState.get(source.getServer()).houseOf(sp.getUUID());
        if (house == null) source.sendFailure(Component.literal("You aren't in a House."));
        return house;
    }

    private static int houseBankShow(CommandSourceStack source) {
        Faction house = ownHouse(source);
        if (house == null) return 0;
        long balance = FactionBank.balance(source.getServer(), house);
        source.sendSuccess(() -> Component.literal("Bank of ").withStyle(ChatFormatting.GRAY)
                .append(FactionText.name(source.getServer(), house))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(net.fugginbeenus.notchcurrency.core.NotchCurrency.coins(balance)), false);
        return 1;
    }

    private static int houseBankMove(CommandSourceStack source, boolean deposit, long amount) {
        Faction house = ownHouse(source);
        if (house == null) return 0;
        ServerPlayer sp = source.getPlayer();
        boolean done = deposit ? FactionBank.deposit(sp, house, amount) : FactionBank.withdraw(sp, house, amount);
        return done ? 1 : 0;
    }

    private static Faction ownBank(CommandSourceStack source) {
        ServerPlayer sp = source.getPlayer();
        if (sp == null) return null;
        if (!FactionBank.enabled()) {
            source.sendFailure(Component.literal("Faction banks are off on this server."));
            return null;
        }
        Faction faction = FactionState.get(source.getServer()).factionOf(sp.getUUID());
        if (faction == null) source.sendFailure(Component.literal("You're not in a faction."));
        return faction;
    }

    private static int bankShow(CommandSourceStack source) {
        Faction faction = ownBank(source);
        if (faction == null) return 0;
        long balance = FactionBank.balance(source.getServer(), faction);
        source.sendSuccess(() -> Component.literal("Bank of ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(faction.displayName()).withStyle(faction.color()))
                .append(Component.literal(": ").withStyle(ChatFormatting.GRAY))
                .append(net.fugginbeenus.notchcurrency.core.NotchCurrency.coins(balance)), false);
        return 1;
    }

    private static int bankMove(CommandSourceStack source, boolean deposit, long amount) {
        Faction faction = ownBank(source);
        if (faction == null) return 0;
        ServerPlayer sp = source.getPlayer();
        boolean done = deposit ? FactionBank.deposit(sp, faction, amount) : FactionBank.withdraw(sp, faction, amount);
        return done ? 1 : 0;
    }

    private static int bankPay(CommandSourceStack source, ServerPlayer target, long amount) {
        Faction faction = ownBank(source);
        if (faction == null) return 0;
        return FactionBank.pay(source.getPlayer(), faction, target, amount) ? 1 : 0;
    }

    private static int adminBank(CommandSourceStack source, String factionId, long delta) {
        Faction faction = FactionState.get(source.getServer()).get(factionId);
        if (faction == null) {
            source.sendFailure(Component.literal("No faction by that name."));
            return 0;
        }
        if (delta != 0 && !FactionBank.adjust(source.getServer(), faction, delta)) {
            source.sendFailure(Component.literal("The bank doesn't have that much."));
            return 0;
        }
        long balance = FactionBank.balance(source.getServer(), faction);
        source.sendSuccess(() -> Component.literal("Bank of ").withStyle(ChatFormatting.GRAY)
                .append(Component.literal(faction.displayName()).withStyle(faction.color()))
                .append(Component.literal(" now holds ").withStyle(ChatFormatting.GRAY))
                .append(net.fugginbeenus.notchcurrency.core.NotchCurrency.coins(balance)), delta != 0);
        return 1;
    }

    private static int adminMove(CommandSourceStack source, ServerPlayer target, String factionId) {
        FactionState state = FactionState.get(source.getServer());
        Faction faction = state.get(factionId);
        if (faction == null) {
            source.sendFailure(Component.literal("No faction by that name."));
            return 0;
        }
        net.fugginbeenus.notchcurrency.npc.faction.FactionManager.changeMembership(source.getServer(), target.getUUID(), faction.id());
        String name = target.getName().getString();
        source.sendSuccess(() -> Component.literal("Moved " + name + " to ").withStyle(ChatFormatting.GREEN)
                .append(FactionText.name(source.getServer(), faction)), true);
        if (source.getEntity() != target) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(target, Component.literal("An admin moved you to ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(FactionText.name(source.getServer(), faction)));
        }
        return 1;
    }

    private static int adminRemove(CommandSourceStack source, ServerPlayer target) {
        FactionState state = FactionState.get(source.getServer());
        Faction faction = state.factionOf(target.getUUID());
        String name = target.getName().getString();
        if (faction == null) {
            source.sendFailure(Component.literal(name + " isn't with a faction."));
            return 0;
        }
        net.fugginbeenus.notchcurrency.npc.faction.FactionManager.changeMembership(source.getServer(), target.getUUID(), null);
        source.sendSuccess(() -> Component.literal("Removed " + name + " from ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(faction.displayName()).withStyle(faction.color())), true);
        if (source.getEntity() != target) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(target, Component.literal("An admin removed you from ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(faction.displayName()).withStyle(faction.color())));
        }
        return 1;
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
            for (Faction h : state.housesOf(f.id())) houseLine(source, state, h, "    > ");
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
