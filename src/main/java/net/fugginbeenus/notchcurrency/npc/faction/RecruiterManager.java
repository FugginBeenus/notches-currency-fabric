package net.fugginbeenus.notchcurrency.npc.faction;

import net.fugginbeenus.notchcurrency.compat.Net;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class RecruiterManager {

    public static final int ACTION_JOIN = 0;
    public static final int ACTION_LEAVE = 1;
    public static final int ACTION_FOUND = 2;
    public static final int ACTION_SETTINGS = 3;

    private RecruiterManager() {}

    public static void open(ServerPlayer sp, NotchNpcEntity npc) {
        FactionState state = FactionState.get(sp.serverLevel());
        String factionId = npc.getFactionId();
        Faction faction = state.get(factionId);

        // An owner standing at an unassigned recruiter is the moment we offer to found one.
        boolean mayAssign = npc.canEdit(sp);
        boolean canFound = mayAssign && faction == null && FactionManager.canFound(sp);

        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npc.getUUID());
        buf.writeUtf(faction == null ? "" : faction.id());
        buf.writeUtf(faction == null ? "" : faction.displayName());
        buf.writeUtf(faction == null ? net.fugginbeenus.notchcurrency.compat.Colors.name(ChatFormatting.WHITE) : net.fugginbeenus.notchcurrency.compat.Colors.name(faction.color()));
        buf.writeVarInt(faction == null ? 0 : state.memberCount(faction.id()));
        buf.writeBoolean(faction != null && faction.id().equals(state.factionIdOf(sp.getUUID())));
        buf.writeBoolean(canFound);
        buf.writeBoolean(mayAssign);
        buf.writeUtf(faction == null ? "" : faction.motto());
        buf.writeVarInt(faction == null ? 0 : faction.joinFee());
        buf.writeBoolean(faction == null || faction.isOpenToJoin());
        // Only whoever runs the faction gets the settings pane.
        buf.writeBoolean(faction != null && FactionManager.canManage(sp, faction));
        Net.sendToClient(sp, NotchPackets.NPC_RECRUITER_OPEN, buf);
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
            // Admins can point an NPC at anything; everyone else only at factions they founded.
            if (!FactionManager.canManage(sp, faction)) {
                net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("That isn't your faction.").withStyle(ChatFormatting.RED));
                return;
            }
            npc.setFactionId(faction.id());
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("")
                    .append(Component.literal(faction.displayName()).withStyle(faction.color()))
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
        java.util.List<Faction> offered = new java.util.ArrayList<>();
        for (Faction f : state.all()) {
            if (FactionManager.canManage(sp, f)) offered.add(f);
        }
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
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
        String factionId = npc.getFactionId();

        switch (action) {
            case ACTION_JOIN -> {
                if (!state.exists(factionId)) {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("This recruiter isn't signed up to a faction yet.")
                            .withStyle(ChatFormatting.YELLOW));
                    return;
                }
                FactionManager.join(sp, factionId);
            }
            case ACTION_LEAVE -> FactionManager.leave(sp);
            case ACTION_FOUND -> {
                // Founding is an owner action on their own recruiter, and it re-checks the one-each rule.
                if (!npc.canEdit(sp)) {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("This isn't your recruiter.").withStyle(ChatFormatting.RED));
                    return;
                }
                if (state.exists(npc.getFactionId())) {
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
                Faction faction = state.get(factionId);
                if (faction == null || !FactionManager.canManage(sp, faction)) {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("That isn't your faction.").withStyle(ChatFormatting.RED));
                    return;
                }
                faction.setMotto(name); // the settings pane sends the motto in the name slot
                faction.setJoinFee(fee);
                faction.setOpenToJoin(open);
                ChatFormatting chosen = net.fugginbeenus.notchcurrency.compat.Colors.byName(color);
                if (chosen != null && net.fugginbeenus.notchcurrency.compat.Colors.isColor(chosen)) faction.setColor(chosen);
                state.touch();
                net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Saved.").withStyle(ChatFormatting.GREEN));
            }
            default -> { return; }
        }
        open(sp, npc); // refresh the screen so it reflects what just happened
    }
}
