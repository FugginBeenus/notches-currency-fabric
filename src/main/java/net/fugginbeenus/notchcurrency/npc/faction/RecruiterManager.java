package net.fugginbeenus.notchcurrency.npc.faction;

import net.fugginbeenus.notchcurrency.compat.Net;
import net.fugginbeenus.notchcurrency.compat.Profiles;
import net.fugginbeenus.notchcurrency.economy.FactionBank;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RecruiterManager {

    public static final int ACTION_JOIN = 0;
    public static final int ACTION_LEAVE = 1;
    public static final int ACTION_FOUND = 2;
    public static final int ACTION_SETTINGS = 3;
    public static final int ACTION_DEPOSIT = 4;
    public static final int ACTION_WITHDRAW = 5;
    public static final int ACTION_HOUSE_NEW = 6;
    public static final int ACTION_HOUSE_SAVE = 7;
    public static final int ACTION_HOUSE_END = 8;
    public static final int ACTION_HOUSE_JOIN = 9;
    public static final int ACTION_HOUSE_LEAVE = 10;

    public static final int PAGE_VIEW = 0;
    public static final int PAGE_BANK = 1;
    public static final int PAGE_HOUSES = 2;

    public static final int HOUSE_CAN_JOIN = 0;
    public static final int HOUSE_CLOSED = 1;
    public static final int HOUSE_LOCKED = 2;
    public static final int HOUSE_MINE = 3;

    private RecruiterManager() {}

    public static void open(ServerPlayer sp, NotchNpcEntity npc) {
        open(sp, npc, PAGE_VIEW);
    }

    public static void open(ServerPlayer sp, NotchNpcEntity npc, int page) {
        FactionState state = FactionState.get(sp.serverLevel());
        MinecraftServer server = sp.level().getServer();
        String groupId = npc.getFactionId();
        if (Faction.isHouseId(groupId) && !state.exists(groupId) && state.exists(Faction.rootId(groupId))) {
            npc.setFactionId(Faction.rootId(groupId));
            groupId = npc.getFactionId();
        }
        Faction group = state.get(groupId);
        Faction faction = group == null ? null : state.parentOf(group);
        boolean house = group != null && group.isHouse() && faction != null;
        String mine = state.groupIdOf(sp.getUUID());
        boolean sameFaction = faction != null && faction.id().equals(Faction.rootId(mine));

        boolean mayAssign = npc.canEdit(sp);
        boolean canFound = mayAssign && group == null && FactionManager.canFound(sp);
        boolean locked = FactionManager.membershipLocked(sp) && !(house && sameFaction);

        FriendlyByteBuf buf = Net.buf();
        buf.writeUUID(npc.getUUID());
        buf.writeUtf(group == null ? "" : group.id());
        buf.writeUtf(group == null ? "" : group.displayName());
        buf.writeUtf(net.fugginbeenus.notchcurrency.compat.Colors.name(group == null ? ChatFormatting.WHITE : group.color()));
        buf.writeVarInt(group == null ? 0 : state.memberCount(group.id()));
        buf.writeBoolean(group != null && state.isIn(sp.getUUID(), group.id()));
        buf.writeBoolean(canFound);
        buf.writeBoolean(mayAssign);
        buf.writeUtf(group == null ? "" : house ? "of " + faction.displayName() : group.motto());
        buf.writeVarInt(group == null ? 0 : group.joinFee());
        buf.writeBoolean(group == null || group.isOpenToJoin());
        buf.writeBoolean(group != null && FactionManager.canManage(sp, group));
        buf.writeBoolean(locked);
        boolean bank = group != null && FactionBank.enabled()
                && (FactionBank.isMember(sp, group) || FactionBank.canWithdraw(sp, group));
        buf.writeBoolean(bank);
        buf.writeVarLong(bank ? FactionBank.balance(server, group) : 0L);
        buf.writeBoolean(bank && FactionBank.isMember(sp, group));
        buf.writeBoolean(bank && FactionBank.canWithdraw(sp, group));
        buf.writeVarInt(bank || page != PAGE_BANK ? page : PAGE_VIEW);

        boolean runs = faction != null && FactionManager.canRunHouses(sp, faction);
        List<Faction> houses = faction == null ? List.of() : house ? List.of(group) : state.housesOf(faction.id());
        buf.writeBoolean(house);
        buf.writeBoolean(!house && runs && FactionManager.housesEnabled()
                && houses.size() < FactionManager.maxHouses());
        buf.writeBoolean(runs);
        buf.writeVarInt(houses.size());
        for (Faction h : houses) {
            buf.writeUtf(h.id());
            buf.writeUtf(h.displayName());
            buf.writeVarInt(state.memberCount(h.id()));
            buf.writeVarInt(h.joinFee());
            buf.writeBoolean(h.isOpenToJoin());
            buf.writeVarLong(joinCost(sp, state, faction, h));
            buf.writeVarInt(houseStatus(sp, state, faction, h));
            buf.writeBoolean(FactionManager.canManage(sp, h));
            buf.writeUtf(h.founder() == null ? "" : Profiles.nameOf(server, h.founder()).orElse("?"));
            List<String> names = runs ? memberNames(server, state, h) : List.of();
            buf.writeVarInt(names.size());
            for (String n : names) buf.writeUtf(n);
        }
        Net.sendToClient(sp, NotchPackets.NPC_RECRUITER_OPEN, buf);
    }

    private static long joinCost(ServerPlayer sp, FactionState state, Faction faction, Faction house) {
        long cost = 0;
        if (!faction.id().equals(state.factionIdOf(sp.getUUID())) && !FactionManager.canManage(sp, faction)) {
            cost += faction.joinFee();
        }
        if (!FactionManager.canManage(sp, house)) cost += house.joinFee();
        return cost;
    }

    private static int houseStatus(ServerPlayer sp, FactionState state, Faction faction, Faction house) {
        if (house.id().equals(state.groupIdOf(sp.getUUID()))) return HOUSE_MINE;
        boolean newHere = !faction.id().equals(state.factionIdOf(sp.getUUID()));
        if (newHere && FactionManager.membershipLocked(sp)) return HOUSE_LOCKED;
        if (newHere && !faction.isOpenToJoin() && !FactionManager.canManage(sp, faction)) return HOUSE_CLOSED;
        if (!house.isOpenToJoin() && !FactionManager.canManage(sp, house)) return HOUSE_CLOSED;
        return HOUSE_CAN_JOIN;
    }

    private static List<String> memberNames(MinecraftServer server, FactionState state, Faction house) {
        List<String> out = new ArrayList<>();
        for (UUID id : state.membersOf(house.id())) {
            Profiles.nameOf(server, id).ifPresent(out::add);
        }
        out.sort(String.CASE_INSENSITIVE_ORDER);
        return out;
    }

    public static final int PICK_LIST = 0;
    public static final int PICK_SET = 1;
    public static final int PICK_CLEAR = 2;

    public static void pick(ServerPlayer sp, NotchNpcEntity npc, int action, String factionId) {
        if (!npc.canEdit(sp)) return;
        FactionState state = FactionState.get(sp.serverLevel());

        if (action == PICK_SET) {
            Faction faction = state.get(factionId);
            if (faction == null) {
                net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("No faction by that name.").withStyle(ChatFormatting.RED));
                return;
            }

            if (!FactionManager.canManage(sp, faction)) {
                net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("That isn't your faction.").withStyle(ChatFormatting.RED));
                return;
            }
            npc.setFactionId(faction.id());
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("")
                    .append(FactionText.name(sp.level().getServer(), faction))
                    .append(Component.literal(" now flies here.").withStyle(ChatFormatting.GREEN)));
        } else if (action == PICK_CLEAR) {
            npc.setFactionId("");
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Faction cleared.").withStyle(ChatFormatting.YELLOW));
        }
        sendList(sp, npc);
    }

    public static void sendList(ServerPlayer sp, NotchNpcEntity npc) {
        if (!npc.canEdit(sp)) return;
        FactionState state = FactionState.get(sp.serverLevel());
        List<Faction> offered = new ArrayList<>();
        for (Faction f : FactionBoards.pickerList(state, state.all())) {
            if (FactionManager.canManage(sp, f)) offered.add(f);
        }
        FriendlyByteBuf buf = Net.buf();
        buf.writeUUID(npc.getUUID());
        buf.writeUtf(npc.getFactionId());
        buf.writeVarInt(offered.size());
        for (Faction f : offered) {
            buf.writeUtf(f.id());
            buf.writeUtf(f.displayName());
            buf.writeUtf(net.fugginbeenus.notchcurrency.compat.Colors.name(f.color()));
            buf.writeVarInt(state.memberCount(f.id()));
        }
        Net.sendToClient(sp, NotchPackets.NPC_FACTION_LIST, buf);
    }

    public static void act(ServerPlayer sp, NotchNpcEntity npc, int action, String name, String color,
                           int fee, boolean open) {
        FactionState state = FactionState.get(sp.serverLevel());
        String groupId = npc.getFactionId();
        Faction group = state.get(groupId);
        Faction faction = group == null ? null : state.parentOf(group);

        switch (action) {
            case ACTION_JOIN -> {
                if (group == null) {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("This recruiter isn't signed up to a faction yet.")
                            .withStyle(ChatFormatting.YELLOW));
                    return;
                }
                FactionManager.join(sp, group.id());
            }
            case ACTION_LEAVE -> {
                if (group != null && group.isHouse()) {
                    FactionManager.leaveHouse(sp);
                } else {
                    FactionManager.leave(sp);
                }
            }
            case ACTION_FOUND -> {
                if (!npc.canEdit(sp)) {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("This isn't your recruiter.").withStyle(ChatFormatting.RED));
                    return;
                }
                if (group != null) {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("This recruiter already represents a faction.")
                            .withStyle(ChatFormatting.YELLOW));
                    return;
                }
                ChatFormatting chosen = net.fugginbeenus.notchcurrency.compat.Colors.byName(color);
                Faction created = FactionManager.create(sp, name,
                        chosen == null || !net.fugginbeenus.notchcurrency.compat.Colors.isColor(chosen) ? ChatFormatting.WHITE : chosen);
                if (created != null) {
                    created.setJoinFee(fee);
                    created.setOpenToJoin(open);
                    state.touch();
                    npc.setFactionId(created.id());
                }
            }
            case ACTION_SETTINGS -> {
                if (group == null || !FactionManager.canManage(sp, group)) {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("That isn't your faction.").withStyle(ChatFormatting.RED));
                    return;
                }
                if (group.isHouse()) {
                    if (FactionManager.saveHouse(sp, group.id(), fee, open)) {
                        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Saved.").withStyle(ChatFormatting.GREEN));
                    }
                    break;
                }
                group.setMotto(name);
                group.setJoinFee(fee);
                group.setOpenToJoin(open);
                ChatFormatting chosen = net.fugginbeenus.notchcurrency.compat.Colors.byName(color);
                if (chosen != null && net.fugginbeenus.notchcurrency.compat.Colors.isColor(chosen)) group.setColor(chosen);
                state.touch();
                net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Saved.").withStyle(ChatFormatting.GREEN));
            }
            case ACTION_DEPOSIT, ACTION_WITHDRAW -> {
                if (group == null) return;
                long amount;
                try {
                    amount = Long.parseLong(name.trim());
                } catch (NumberFormatException e) {
                    amount = 0L;
                }
                if (action == ACTION_DEPOSIT) {
                    FactionBank.deposit(sp, group, amount);
                } else {
                    FactionBank.withdraw(sp, group, amount);
                }
                open(sp, npc, PAGE_BANK);
                return;
            }
            case ACTION_HOUSE_NEW -> {
                if (faction == null || group.isHouse()) return;
                Faction made = FactionManager.createHouse(sp, faction.id(), name);
                if (made != null) {
                    made.setJoinFee(fee);
                    made.setOpenToJoin(open);
                    state.touch();
                }
                open(sp, npc, PAGE_HOUSES);
                return;
            }
            case ACTION_HOUSE_SAVE -> {
                Faction house = ownHouse(state, faction, name);
                if (house == null) return;
                boolean saved = FactionManager.saveHouse(sp, house.id(), fee, open);
                if (saved && FactionManager.canRunHouses(sp, faction)) {
                    UUID leader = color.isBlank() ? null : memberByName(sp.level().getServer(), state, house, color);
                    if (color.isBlank() || leader != null) FactionManager.setLeader(sp, house.id(), leader);
                }
                if (saved) {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Saved.").withStyle(ChatFormatting.GREEN));
                }
                open(sp, npc, group.isHouse() ? PAGE_VIEW : PAGE_HOUSES);
                return;
            }
            case ACTION_HOUSE_END -> {
                Faction house = ownHouse(state, faction, name);
                if (house == null) return;
                boolean fromHouse = group.isHouse();
                FactionManager.disbandHouse(sp, house.id());
                open(sp, npc, fromHouse ? PAGE_VIEW : PAGE_HOUSES);
                return;
            }
            case ACTION_HOUSE_JOIN -> {
                Faction house = ownHouse(state, faction, name);
                if (house == null) return;
                FactionManager.join(sp, house.id());
                open(sp, npc, group.isHouse() ? PAGE_VIEW : PAGE_HOUSES);
                return;
            }
            case ACTION_HOUSE_LEAVE -> {
                FactionManager.leaveHouse(sp);
                open(sp, npc, group != null && group.isHouse() ? PAGE_VIEW : PAGE_HOUSES);
                return;
            }
            default -> { return; }
        }
        open(sp, npc);
    }

    private static Faction ownHouse(FactionState state, Faction faction, String houseId) {
        if (faction == null || houseId == null || !Faction.isHouseId(houseId)) return null;
        Faction house = state.get(houseId);
        return house != null && house.isHouse() && house.parent().equals(faction.id()) ? house : null;
    }

    private static UUID memberByName(MinecraftServer server, FactionState state, Faction house, String name) {
        for (UUID id : state.membersOf(house.id())) {
            if (Profiles.nameOf(server, id).map(n -> n.equalsIgnoreCase(name)).orElse(false)) return id;
        }
        return null;
    }
}
