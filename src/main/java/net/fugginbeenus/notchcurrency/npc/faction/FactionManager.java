package net.fugginbeenus.notchcurrency.npc.faction;

import net.fugginbeenus.notchcurrency.compat.Perms;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public final class FactionManager {

    private static final String PERMANENT = "Faction membership is permanent on this server.";

    private FactionManager() {}

    public static boolean membershipLocked(ServerPlayer sp) {
        return NotchConfigIO.get().factions.permanentMembership
                && FactionState.get(sp.serverLevel()).factionOf(sp.getUUID()) != null;
    }

    private static String permanent(ServerPlayer sp) {
        return Perms.isAdmin(sp, Perms.FACTION) ? PERMANENT + " You can move players with /faction admin." : PERMANENT;
    }

    public static boolean housesEnabled() {
        return NotchConfigIO.get().factions.houses;
    }

    public static int maxHouses() {
        return Math.max(1, NotchConfigIO.get().factions.maxHouses);
    }

    public static boolean canFound(ServerPlayer sp) {
        if (membershipLocked(sp)) return false;
        if (Perms.isAdmin(sp, Perms.FACTION)) return true;
        return FactionState.get(sp.serverLevel()).foundedBy(sp.getUUID()) == null;
    }

    public static boolean canManage(ServerPlayer sp, Faction faction) {
        if (Perms.isAdmin(sp, Perms.FACTION) || faction.isFoundedBy(sp.getUUID())) return true;
        if (!faction.isHouse()) return false;
        Faction parent = FactionState.get(sp.serverLevel()).get(faction.parent());
        return parent != null && parent.isFoundedBy(sp.getUUID());
    }

    public static boolean canRunHouses(ServerPlayer sp, Faction faction) {
        return !faction.isHouse() && canManage(sp, faction);
    }

    @Nullable
    public static Faction create(ServerPlayer sp, String name, ChatFormatting color) {
        FactionState state = FactionState.get(sp.serverLevel());
        String id = Faction.toId(name);
        if (id == null) {
            msg(sp, "That name doesn't have any letters or numbers in it.", ChatFormatting.RED);
            return null;
        }
        if (state.exists(id)) {
            msg(sp, "There's already a faction called that.", ChatFormatting.RED);
            return null;
        }
        if (state.count() >= FactionState.MAX_FACTIONS) {
            msg(sp, "This server has as many factions as it can hold.", ChatFormatting.RED);
            return null;
        }
        if (membershipLocked(sp)) {
            msg(sp, permanent(sp), ChatFormatting.RED);
            return null;
        }
        if (!canFound(sp)) {
            Faction existing = state.foundedBy(sp.getUUID());
            msg(sp, "You already founded " + (existing == null ? "a faction" : existing.displayName())
                    + ". Disband it first if you want a new one.", ChatFormatting.RED);
            return null;
        }
        Faction faction = new Faction(id, name.trim(), color, sp.getUUID());
        if (!state.add(faction)) {
            msg(sp, "Couldn't create that faction.", ChatFormatting.RED);
            return null;
        }

        changeMembership(sp.level().getServer(), sp.getUUID(), id);
        msg(sp, "Founded " + faction.displayName() + ".", ChatFormatting.GREEN);
        return faction;
    }

    public static boolean disband(ServerPlayer sp, String factionId) {
        if (Faction.isHouseId(factionId)) return disbandHouse(sp, factionId);
        FactionState state = FactionState.get(sp.serverLevel());
        Faction faction = state.get(factionId);
        if (faction == null) {
            msg(sp, "No faction by that name.", ChatFormatting.RED);
            return false;
        }
        if (!canManage(sp, faction)) {
            msg(sp, "That isn't your faction to disband.", ChatFormatting.RED);
            return false;
        }
        for (Faction house : state.housesOf(faction.id())) endHouse(sp.level().getServer(), state, house, faction);
        FactionBoards.dropAllJobs(sp.level().getServer(), factionId);
        net.fugginbeenus.notchcurrency.economy.bounty.BountyManager.takeDownFactionPosts(sp.level().getServer(), factionId);
        net.fugginbeenus.notchcurrency.economy.FactionBank.payOutOnDisband(sp.level().getServer(), faction, sp);
        clearFromNpcs(sp.level().getServer(), factionId);
        state.remove(factionId);
        msg(sp, "Disbanded " + faction.displayName() + ".", ChatFormatting.YELLOW);
        return true;
    }

    public static void join(ServerPlayer sp, String groupId) {
        FactionState state = FactionState.get(sp.serverLevel());
        MinecraftServer server = sp.level().getServer();
        Faction group = state.get(groupId);
        Faction faction = group == null ? null : state.parentOf(group);
        if (group == null || faction == null) {
            msg(sp, "That faction doesn't exist any more.", ChatFormatting.RED);
            return;
        }
        String mine = state.factionIdOf(sp.getUUID());
        boolean newHere = !faction.id().equals(mine);
        if (group.id().equals(state.groupIdOf(sp.getUUID())) || (!group.isHouse() && !newHere)) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("You're already with ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(FactionText.name(server, group))
                    .append(Component.literal(".").withStyle(ChatFormatting.YELLOW)));
            return;
        }
        if (newHere && membershipLocked(sp)) {
            msg(sp, "You're with " + state.factionOf(sp.getUUID()).displayName() + ". " + permanent(sp),
                    ChatFormatting.RED);
            return;
        }
        if (newHere && !faction.isOpenToJoin() && !canManage(sp, faction)) {
            msg(sp, faction.displayName() + " isn't taking new members.", ChatFormatting.RED);
            return;
        }
        if (group.isHouse() && !group.isOpenToJoin() && !canManage(sp, group)) {
            msg(sp, group.displayName() + " isn't taking new members.", ChatFormatting.RED);
            return;
        }
        long factionFee = newHere && !canManage(sp, faction) ? faction.joinFee() : 0;
        long houseFee = group.isHouse() && !canManage(sp, group) ? group.joinFee() : 0;
        long fee = factionFee + houseFee;
        if (fee > 0) {
            boolean banked = net.fugginbeenus.notchcurrency.economy.FactionBank.enabled();
            if (!net.fugginbeenus.notchcurrency.api.CurrencyApi.withdraw(sp, fee, banked
                    ? net.fugginbeenus.notchcurrency.economy.TransactionReason.FACTION
                    : net.fugginbeenus.notchcurrency.economy.TransactionReason.SINK, "faction dues")) {
                msg(sp, "Joining costs " + fee + " "
                        + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".", ChatFormatting.RED);
                return;
            }
            if (banked) {
                String from = sp.getName().getString();
                if (factionFee > 0) net.fugginbeenus.notchcurrency.economy.FactionBank.addDues(server, faction, factionFee, from);
                if (houseFee > 0) net.fugginbeenus.notchcurrency.economy.FactionBank.addDues(server, group, houseFee, from);
            }
        }
        changeMembership(server, sp.getUUID(), group.id());
        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("You joined ")
                .withStyle(ChatFormatting.GREEN)
                .append(FactionText.name(server, group))
                .append(Component.literal(".").withStyle(ChatFormatting.GREEN)));
    }

    public static void leave(ServerPlayer sp) {
        FactionState state = FactionState.get(sp.serverLevel());
        Faction faction = state.factionOf(sp.getUUID());
        if (faction == null) {
            msg(sp, "You aren't with a faction.", ChatFormatting.YELLOW);
            return;
        }

        if (faction.isFoundedBy(sp.getUUID()) && !Perms.isAdmin(sp, Perms.FACTION)) {
            msg(sp, "You founded " + faction.displayName() + " - disband it rather than walking out.",
                    ChatFormatting.RED);
            return;
        }
        if (membershipLocked(sp)) {
            msg(sp, permanent(sp), ChatFormatting.RED);
            return;
        }
        changeMembership(sp.level().getServer(), sp.getUUID(), null);
        msg(sp, "You left " + faction.displayName() + ".", ChatFormatting.YELLOW);
    }

    public static void leaveHouse(ServerPlayer sp) {
        FactionState state = FactionState.get(sp.serverLevel());
        Faction house = state.houseOf(sp.getUUID());
        Faction faction = house == null ? null : state.parentOf(house);
        if (house == null || faction == null) {
            msg(sp, "You aren't in a House.", ChatFormatting.YELLOW);
            return;
        }
        changeMembership(sp.level().getServer(), sp.getUUID(), faction.id());
        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("You left " + house.displayName()
                        + ". You are still in ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(faction.displayName()).withStyle(faction.color()))
                .append(Component.literal(".").withStyle(ChatFormatting.YELLOW)));
    }

    @Nullable
    public static Faction createHouse(ServerPlayer sp, String factionId, String name) {
        FactionState state = FactionState.get(sp.serverLevel());
        Faction faction = state.get(factionId);
        if (faction == null || faction.isHouse()) {
            msg(sp, "No faction by that name.", ChatFormatting.RED);
            return null;
        }
        if (!canManage(sp, faction)) {
            msg(sp, "Only the founder of " + faction.displayName() + " can make Houses.", ChatFormatting.RED);
            return null;
        }
        if (!housesEnabled()) {
            msg(sp, "Houses are off on this server.", ChatFormatting.RED);
            return null;
        }
        String clean = name == null ? "" : name.trim();
        if (clean.length() > Faction.MAX_NAME_LENGTH) clean = clean.substring(0, Faction.MAX_NAME_LENGTH);
        String id = Faction.houseId(faction.id(), clean);
        if (id == null) {
            msg(sp, "That name doesn't have any letters or numbers in it.", ChatFormatting.RED);
            return null;
        }
        if (state.exists(id)) {
            msg(sp, faction.displayName() + " already has a House called that.", ChatFormatting.RED);
            return null;
        }
        if (state.housesOf(faction.id()).size() >= maxHouses()) {
            msg(sp, faction.displayName() + " already has " + maxHouses() + " Houses.", ChatFormatting.RED);
            return null;
        }
        Faction house = new Faction(id, clean, faction.color(), null).asHouseOf(faction.id());
        if (!state.addHouse(house)) {
            msg(sp, "Couldn't make that House.", ChatFormatting.RED);
            return null;
        }
        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Made the House ")
                .withStyle(ChatFormatting.GREEN)
                .append(FactionText.name(sp.level().getServer(), house))
                .append(Component.literal(".").withStyle(ChatFormatting.GREEN)));
        return house;
    }

    public static boolean disbandHouse(ServerPlayer sp, String houseId) {
        FactionState state = FactionState.get(sp.serverLevel());
        MinecraftServer server = sp.level().getServer();
        Faction house = state.get(houseId);
        Faction faction = house == null ? null : state.parentOf(house);
        if (house == null || !house.isHouse() || faction == null) {
            msg(sp, "No House by that name.", ChatFormatting.RED);
            return false;
        }
        if (!canManage(sp, faction)) {
            msg(sp, "Only the founder of " + faction.displayName() + " can end its Houses.", ChatFormatting.RED);
            return false;
        }
        List<UUID> members = state.membersOf(house.id());
        endHouse(server, state, house, faction);
        for (UUID id : members) {
            ServerPlayer online = server.getPlayerList().getPlayer(id);
            if (online == null || online == sp) continue;
            net.fugginbeenus.notchcurrency.compat.Msg.chat(online, Component.literal("The House " + house.displayName()
                            + " was ended. You are still in ").withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(faction.displayName()).withStyle(faction.color()))
                    .append(Component.literal(".").withStyle(ChatFormatting.YELLOW)));
        }
        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Ended the House " + house.displayName()
                        + ". Its members are still in ").withStyle(ChatFormatting.YELLOW)
                .append(Component.literal(faction.displayName()).withStyle(faction.color()))
                .append(Component.literal(".").withStyle(ChatFormatting.YELLOW)));
        return true;
    }

    private static void endHouse(MinecraftServer server, FactionState state, Faction house, Faction faction) {
        FactionBoards.dropAllJobs(server, house.id());
        net.fugginbeenus.notchcurrency.economy.bounty.BountyManager.takeDownFactionPosts(server, house.id());
        net.fugginbeenus.notchcurrency.economy.FactionBank.moveAll(server, house, faction,
                "House " + house.displayName() + " ended");
        moveNpcs(server, house.id(), faction.id());
        state.remove(house.id());
    }

    public static boolean setLeader(ServerPlayer sp, String houseId, @Nullable UUID leader) {
        FactionState state = FactionState.get(sp.serverLevel());
        MinecraftServer server = sp.level().getServer();
        Faction house = state.get(houseId);
        Faction faction = house == null ? null : state.parentOf(house);
        if (house == null || !house.isHouse() || faction == null) {
            msg(sp, "No House by that name.", ChatFormatting.RED);
            return false;
        }
        if (!canManage(sp, faction)) {
            msg(sp, "Only the founder of " + faction.displayName() + " can pick a leader.", ChatFormatting.RED);
            return false;
        }
        if (leader != null && !house.id().equals(state.groupIdOf(leader))) {
            msg(sp, "The leader has to be a member of " + house.displayName() + ".", ChatFormatting.RED);
            return false;
        }
        if (Objects.equals(house.founder(), leader)) return true;
        house.setFounder(leader);
        state.touch();
        if (leader == null) {
            msg(sp, house.displayName() + " has no leader now.", ChatFormatting.YELLOW);
            return true;
        }
        String name = net.fugginbeenus.notchcurrency.compat.Profiles.nameOf(server, leader).orElse("They");
        msg(sp, name + " now leads " + house.displayName() + ".", ChatFormatting.GREEN);
        ServerPlayer online = server.getPlayerList().getPlayer(leader);
        if (online != null && online != sp) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(online, Component.literal("You now lead ")
                    .withStyle(ChatFormatting.GREEN)
                    .append(FactionText.name(server, house))
                    .append(Component.literal(".").withStyle(ChatFormatting.GREEN)));
        }
        return true;
    }

    public static boolean saveHouse(ServerPlayer sp, String houseId, int fee, boolean open) {
        FactionState state = FactionState.get(sp.serverLevel());
        Faction house = state.get(houseId);
        if (house == null || !house.isHouse()) {
            msg(sp, "No House by that name.", ChatFormatting.RED);
            return false;
        }
        if (!canManage(sp, house)) {
            msg(sp, "That isn't your House.", ChatFormatting.RED);
            return false;
        }
        house.setJoinFee(fee);
        house.setOpenToJoin(open);
        state.touch();
        return true;
    }

    public static void changeMembership(MinecraftServer server, UUID player, @Nullable String newGroupId) {
        FactionState state = FactionState.get(server);
        String old = state.groupIdOf(player);
        if (old != null && !old.equals(newGroupId)) {
            String oldFaction = Faction.rootId(old);
            boolean sameFaction = newGroupId != null && oldFaction.equals(Faction.rootId(newGroupId));
            if (!sameFaction) {
                FactionBoards.dropJobs(server, player, oldFaction);
            } else if (Faction.isHouseId(old)) {
                FactionBoards.dropJobs(server, player, old);
            }
            Faction oldHouse = Faction.isHouseId(old) ? state.get(old) : null;
            if (oldHouse != null && oldHouse.isFoundedBy(player)) {
                oldHouse.setFounder(null);
                state.touch();
            }
        }
        if (newGroupId == null) {
            state.leave(player);
        } else {
            state.join(player, newGroupId);
        }
    }

    private static void clearFromNpcs(@Nullable MinecraftServer server, String factionId) {
        if (server == null) return;
        for (ServerLevel world : server.getAllLevels()) {
            for (Entity e : world.getAllEntities()) {
                if (e instanceof NotchNpcEntity npc && Faction.within(npc.getFactionId(), factionId)) {
                    npc.setFactionId("");
                }
            }
        }
    }

    private static void moveNpcs(@Nullable MinecraftServer server, String fromId, String toId) {
        if (server == null) return;
        for (ServerLevel world : server.getAllLevels()) {
            for (Entity e : world.getAllEntities()) {
                if (e instanceof NotchNpcEntity npc && fromId.equals(npc.getFactionId())) npc.setFactionId(toId);
            }
        }
    }

    private static void msg(ServerPlayer sp, String text, ChatFormatting color) {
        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal(text).withStyle(color));
    }
}
