package net.fugginbeenus.notchcurrency.npc.faction;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fugginbeenus.notchcurrency.compat.Net;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The Recruiter NPC: how factions are joined, left and founded without anyone learning a command.
 *
 * <p>The NPC is a doorway, not a container. It holds a faction id and nothing else, so whatever
 * happens to it, the faction on the other side is untouched.
 */
public final class RecruiterManager {

    public static final int ACTION_JOIN = 0;
    public static final int ACTION_LEAVE = 1;
    public static final int ACTION_FOUND = 2;
    /** Save the faction's settings: {@code name} is the motto, {@code extra} packs fee + open flag. */
    public static final int ACTION_SETTINGS = 3;

    private RecruiterManager() {}

    /** Show the recruiter screen for whoever just talked to it. */
    public static void open(ServerPlayerEntity sp, NotchNpcEntity npc) {
        FactionState state = FactionState.get(sp.getServerWorld());
        String factionId = npc.getFactionId();
        Faction faction = state.get(factionId);

        // An owner standing at an unassigned recruiter is the moment we offer to found one.
        boolean mayAssign = npc.canEdit(sp);
        boolean canFound = mayAssign && faction == null && FactionManager.canFound(sp);

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npc.getUuid());
        buf.writeString(faction == null ? "" : faction.id());
        buf.writeString(faction == null ? "" : faction.displayName());
        buf.writeString(faction == null ? Formatting.WHITE.getName() : faction.color().getName());
        buf.writeVarInt(faction == null ? 0 : state.memberCount(faction.id()));
        buf.writeBoolean(faction != null && faction.id().equals(state.factionIdOf(sp.getUuid())));
        buf.writeBoolean(canFound);
        buf.writeBoolean(mayAssign);
        buf.writeString(faction == null ? "" : faction.motto());
        buf.writeVarInt(faction == null ? 0 : faction.joinFee());
        buf.writeBoolean(faction == null || faction.isOpenToJoin());
        // Only whoever runs the faction gets the settings pane.
        buf.writeBoolean(faction != null && FactionManager.canManage(sp, faction));
        Net.sendToClient(sp, NotchPackets.NPC_RECRUITER_OPEN, buf);
    }

    public static final int PICK_LIST = 0;
    public static final int PICK_SET = 1;
    public static final int PICK_CLEAR = 2;

    /**
     * The faction picker behind the Role tab. This is how a founder who lost their recruiter gets
     * back on their feet: place a new one, pick the faction they already own, carry on. Without it
     * they'd be stuck, since founding is only offered on an unassigned recruiter and they've already
     * used their one faction.
     */
    public static void pick(ServerPlayerEntity sp, NotchNpcEntity npc, int action, String factionId) {
        if (!npc.canEdit(sp)) return;
        FactionState state = FactionState.get(sp.getServerWorld());

        if (action == PICK_SET) {
            Faction faction = state.get(factionId);
            if (faction == null) {
                sp.sendMessage(Text.literal("No faction by that name.").formatted(Formatting.RED), false);
                return;
            }
            // Admins can point an NPC at anything; everyone else only at factions they founded.
            if (!FactionManager.canManage(sp, faction)) {
                sp.sendMessage(Text.literal("That isn't your faction.").formatted(Formatting.RED), false);
                return;
            }
            npc.setFactionId(faction.id());
            sp.sendMessage(Text.literal("")
                    .append(Text.literal(faction.displayName()).formatted(faction.color()))
                    .append(Text.literal(" now flies here.").formatted(Formatting.GREEN)), false);
        } else if (action == PICK_CLEAR) {
            npc.setFactionId("");
            sp.sendMessage(Text.literal("Faction cleared.").formatted(Formatting.YELLOW), false);
        }
        sendList(sp, npc);
    }

    /** Send the factions this player may point the NPC at. */
    public static void sendList(ServerPlayerEntity sp, NotchNpcEntity npc) {
        if (!npc.canEdit(sp)) return;
        FactionState state = FactionState.get(sp.getServerWorld());
        java.util.List<Faction> offered = new java.util.ArrayList<>();
        for (Faction f : state.all()) {
            if (FactionManager.canManage(sp, f)) offered.add(f);
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npc.getUuid());
        buf.writeString(npc.getFactionId());
        buf.writeVarInt(offered.size());
        for (Faction f : offered) {
            buf.writeString(f.id());
            buf.writeString(f.displayName());
            buf.writeString(f.color().getName());
            buf.writeVarInt(state.memberCount(f.id()));
        }
        Net.sendToClient(sp, NotchPackets.NPC_FACTION_LIST, buf);
    }

    /** Handle a button on that screen. Everything is re-checked here; the client is never trusted. */
    public static void act(ServerPlayerEntity sp, NotchNpcEntity npc, int action, String name, String color,
                           int fee, boolean open) {
        FactionState state = FactionState.get(sp.getServerWorld());
        String factionId = npc.getFactionId();

        switch (action) {
            case ACTION_JOIN -> {
                if (!state.exists(factionId)) {
                    sp.sendMessage(Text.literal("This recruiter isn't signed up to a faction yet.")
                            .formatted(Formatting.YELLOW), false);
                    return;
                }
                FactionManager.join(sp, factionId);
            }
            case ACTION_LEAVE -> FactionManager.leave(sp);
            case ACTION_FOUND -> {
                // Founding is an owner action on their own recruiter, and it re-checks the one-each rule.
                if (!npc.canEdit(sp)) {
                    sp.sendMessage(Text.literal("This isn't your recruiter.").formatted(Formatting.RED), false);
                    return;
                }
                if (state.exists(npc.getFactionId())) {
                    sp.sendMessage(Text.literal("This recruiter already represents a faction.")
                            .formatted(Formatting.YELLOW), false);
                    return;
                }
                Formatting chosen = Formatting.byName(color);
                Faction created = FactionManager.create(sp, name,
                        chosen == null || !chosen.isColor() ? Formatting.WHITE : chosen);
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
                    sp.sendMessage(Text.literal("That isn't your faction.").formatted(Formatting.RED), false);
                    return;
                }
                faction.setMotto(name); // the settings pane sends the motto in the name slot
                faction.setJoinFee(fee);
                faction.setOpenToJoin(open);
                Formatting chosen = Formatting.byName(color);
                if (chosen != null && chosen.isColor()) faction.setColor(chosen);
                state.touch();
                sp.sendMessage(Text.literal("Saved.").formatted(Formatting.GREEN), false);
            }
            default -> { return; }
        }
        open(sp, npc); // refresh the screen so it reflects what just happened
    }
}
