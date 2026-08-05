package net.fugginbeenus.notchcurrency.npc.faction;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The guarded operations on factions. Everything that changes a faction goes through here so the
 * rules live in one place: admins manage anything, a founder manages the one they founded, and
 * nobody else can touch either.
 */
public final class FactionManager {

    private FactionManager() {}

    /** Admins are unlimited; everyone else gets one faction to their name. */
    public static boolean canFound(ServerPlayerEntity sp) {
        if (sp.hasPermissionLevel(2)) return true;
        return FactionState.get(sp.getServerWorld()).foundedBy(sp.getUuid()) == null;
    }

    public static boolean canManage(ServerPlayerEntity sp, Faction faction) {
        return sp.hasPermissionLevel(2) || faction.isFoundedBy(sp.getUuid());
    }

    /**
     * Create a faction owned by the player. Returns null (with a message) if the name is unusable,
     * already taken, the server is full of factions, or they already founded one.
     */
    @Nullable
    public static Faction create(ServerPlayerEntity sp, String name, Formatting color) {
        FactionState state = FactionState.get(sp.getServerWorld());
        String id = Faction.toId(name);
        if (id == null) {
            msg(sp, "That name doesn't have any letters or numbers in it.", Formatting.RED);
            return null;
        }
        if (state.exists(id)) {
            msg(sp, "There's already a faction called that.", Formatting.RED);
            return null;
        }
        if (state.count() >= FactionState.MAX_FACTIONS) {
            msg(sp, "This server has as many factions as it can hold.", Formatting.RED);
            return null;
        }
        if (!canFound(sp)) {
            Faction existing = state.foundedBy(sp.getUuid());
            msg(sp, "You already founded " + (existing == null ? "a faction" : existing.displayName())
                    + ". Disband it first if you want a new one.", Formatting.RED);
            return null;
        }
        Faction faction = new Faction(id, name.trim(), color, sp.getUuid());
        if (!state.add(faction)) {
            msg(sp, "Couldn't create that faction.", Formatting.RED);
            return null;
        }
        // The founder is in it from the start; standing outside your own faction reads as a bug.
        state.join(sp.getUuid(), id);
        msg(sp, "Founded " + faction.displayName() + ".", Formatting.GREEN);
        return faction;
    }

    /**
     * Disband a faction: members are released and every NPC pointing at it is cleared, so no id is
     * left dangling anywhere.
     */
    public static boolean disband(ServerPlayerEntity sp, String factionId) {
        FactionState state = FactionState.get(sp.getServerWorld());
        Faction faction = state.get(factionId);
        if (faction == null) {
            msg(sp, "No faction by that name.", Formatting.RED);
            return false;
        }
        if (!canManage(sp, faction)) {
            msg(sp, "That isn't your faction to disband.", Formatting.RED);
            return false;
        }
        clearFromNpcs(sp.getServer(), factionId);
        state.remove(factionId);
        msg(sp, "Disbanded " + faction.displayName() + ".", Formatting.YELLOW);
        return true;
    }

    public static void join(ServerPlayerEntity sp, String factionId) {
        FactionState state = FactionState.get(sp.getServerWorld());
        Faction faction = state.get(factionId);
        if (faction == null) {
            msg(sp, "That faction doesn't exist any more.", Formatting.RED);
            return;
        }
        if (factionId.equals(state.factionIdOf(sp.getUuid()))) {
            msg(sp, "You're already with " + faction.displayName() + ".", Formatting.YELLOW);
            return;
        }
        state.join(sp.getUuid(), factionId);
        sp.sendMessage(Text.literal("You joined ")
                .formatted(Formatting.GREEN)
                .append(Text.literal(faction.displayName()).formatted(faction.color()))
                .append(Text.literal(".").formatted(Formatting.GREEN)), false);
    }

    public static void leave(ServerPlayerEntity sp) {
        FactionState state = FactionState.get(sp.getServerWorld());
        Faction faction = state.factionOf(sp.getUuid());
        if (faction == null) {
            msg(sp, "You aren't with a faction.", Formatting.YELLOW);
            return;
        }
        // A founder leaving would orphan the faction, so send them to disband instead.
        if (faction.isFoundedBy(sp.getUuid()) && !sp.hasPermissionLevel(2)) {
            msg(sp, "You founded " + faction.displayName() + " — disband it rather than walking out.",
                    Formatting.RED);
            return;
        }
        state.leave(sp.getUuid());
        msg(sp, "You left " + faction.displayName() + ".", Formatting.YELLOW);
    }

    /** Whether two sides count as the same faction. Nobody without a faction is anyone's ally. */
    public static boolean sameFaction(@Nullable String a, @Nullable String b) {
        return a != null && !a.isBlank() && a.equals(b);
    }

    /** Clear a faction id off every loaded NPC — used when a faction is disbanded. */
    private static void clearFromNpcs(@Nullable MinecraftServer server, String factionId) {
        if (server == null) return;
        for (ServerWorld world : server.getWorlds()) {
            for (Entity e : world.iterateEntities()) {
                if (e instanceof NotchNpcEntity npc && factionId.equals(npc.getFactionId())) {
                    npc.setFactionId("");
                }
            }
        }
    }

    private static void msg(ServerPlayerEntity sp, String text, Formatting color) {
        sp.sendMessage(Text.literal(text).formatted(color), false);
    }
}
