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
        Net.sendToClient(sp, NotchPackets.NPC_RECRUITER_OPEN, buf);
    }

    /** Handle a button on that screen. Everything is re-checked here; the client is never trusted. */
    public static void act(ServerPlayerEntity sp, NotchNpcEntity npc, int action, String name, String color) {
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
                if (created != null) npc.setFactionId(created.id());
            }
            default -> { return; }
        }
        open(sp, npc); // refresh the screen so it reflects what just happened
    }
}
