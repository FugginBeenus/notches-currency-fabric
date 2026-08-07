package net.fugginbeenus.notchcurrency.npc.faction;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
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

        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(npc.getUUID());
        buf.writeUtf(faction == null ? "" : faction.id());
        buf.writeUtf(faction == null ? "" : faction.displayName());
        buf.writeUtf(faction == null ? ChatFormatting.WHITE.getName() : faction.color().getName());
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
                sp.displayClientMessage(Component.literal("No faction by that name.").withStyle(ChatFormatting.RED), false);
                return;
            }
            // Admins can point an NPC at anything; everyone else only at factions they founded.
            if (!FactionManager.canManage(sp, faction)) {
                sp.displayClientMessage(Component.literal("That isn't your faction.").withStyle(ChatFormatting.RED), false);
                return;
            }
            npc.setFactionId(faction.id());
            sp.displayClientMessage(Component.literal("")
                    .append(Component.literal(faction.displayName()).withStyle(faction.color()))
                    .append(Component.literal(" now flies here.").withStyle(ChatFormatting.GREEN)), false);
        } else if (action == PICK_CLEAR) {
            npc.setFactionId("");
            sp.displayClientMessage(Component.literal("Faction cleared.").withStyle(ChatFormatting.YELLOW), false);
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
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(npc.getUUID());
        buf.writeUtf(npc.getFactionId());
        buf.writeVarInt(offered.size());
        for (Faction f : offered) {
            buf.writeUtf(f.id());
            buf.writeUtf(f.displayName());
            buf.writeUtf(f.color().getName());
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
                    sp.displayClientMessage(Component.literal("This recruiter isn't signed up to a faction yet.")
                            .withStyle(ChatFormatting.YELLOW), false);
                    return;
                }
                FactionManager.join(sp, factionId);
            }
            case ACTION_LEAVE -> FactionManager.leave(sp);
            case ACTION_FOUND -> {
                // Founding is an owner action on their own recruiter, and it re-checks the one-each rule.
                if (!npc.canEdit(sp)) {
                    sp.displayClientMessage(Component.literal("This isn't your recruiter.").withStyle(ChatFormatting.RED), false);
                    return;
                }
                if (state.exists(npc.getFactionId())) {
                    sp.displayClientMessage(Component.literal("This recruiter already represents a faction.")
                            .withStyle(ChatFormatting.YELLOW), false);
                    return;
                }
                ChatFormatting chosen = ChatFormatting.getByName(color);
                Faction created = FactionManager.create(sp, name,
                        chosen == null || !chosen.isColor() ? ChatFormatting.WHITE : chosen);
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
                    sp.displayClientMessage(Component.literal("That isn't your faction.").withStyle(ChatFormatting.RED), false);
                    return;
                }
                faction.setMotto(name); // the settings pane sends the motto in the name slot
                faction.setJoinFee(fee);
                faction.setOpenToJoin(open);
                ChatFormatting chosen = ChatFormatting.getByName(color);
                if (chosen != null && chosen.isColor()) faction.setColor(chosen);
                state.touch();
                sp.displayClientMessage(Component.literal("Saved.").withStyle(ChatFormatting.GREEN), false);
            }
            default -> { return; }
        }
        open(sp, npc); // refresh the screen so it reflects what just happened
    }
}
