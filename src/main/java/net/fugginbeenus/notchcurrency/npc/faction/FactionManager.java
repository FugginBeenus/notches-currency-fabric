package net.fugginbeenus.notchcurrency.npc.faction;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class FactionManager {

    private FactionManager() {}

    public static boolean canFound(ServerPlayer sp) {
        if (sp.hasPermissions(2)) return true;
        return FactionState.get(sp.serverLevel()).foundedBy(sp.getUUID()) == null;
    }

    public static boolean canManage(ServerPlayer sp, Faction faction) {
        return sp.hasPermissions(2) || faction.isFoundedBy(sp.getUUID());
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
        // The founder is in it from the start; standing outside your own faction reads as a bug.
        state.join(sp.getUUID(), id);
        msg(sp, "Founded " + faction.displayName() + ".", ChatFormatting.GREEN);
        return faction;
    }

    public static boolean disband(ServerPlayer sp, String factionId) {
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
        clearFromNpcs(sp.level().getServer(), factionId);
        state.remove(factionId);
        msg(sp, "Disbanded " + faction.displayName() + ".", ChatFormatting.YELLOW);
        return true;
    }

    public static void join(ServerPlayer sp, String factionId) {
        FactionState state = FactionState.get(sp.serverLevel());
        Faction faction = state.get(factionId);
        if (faction == null) {
            msg(sp, "That faction doesn't exist any more.", ChatFormatting.RED);
            return;
        }
        if (factionId.equals(state.factionIdOf(sp.getUUID()))) {
            msg(sp, "You're already with " + faction.displayName() + ".", ChatFormatting.YELLOW);
            return;
        }
        // The founder and admins get in regardless: a closed faction shouldn't lock out its own.
        if (!faction.isOpenToJoin() && !canManage(sp, faction)) {
            msg(sp, faction.displayName() + " isn't taking new members.", ChatFormatting.RED);
            return;
        }
        int fee = faction.joinFee();
        if (fee > 0 && !canManage(sp, faction)) {
            if (!net.fugginbeenus.notchcurrency.api.CurrencyApi.withdraw(sp, fee,
                    net.fugginbeenus.notchcurrency.economy.TransactionReason.SINK, "faction dues")) {
                msg(sp, "Joining costs " + fee + " "
                        + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".", ChatFormatting.RED);
                return;
            }
        }
        state.join(sp.getUUID(), factionId);
        sp.displayClientMessage(Component.literal("You joined ")
                .withStyle(ChatFormatting.GREEN)
                .append(Component.literal(faction.displayName()).withStyle(faction.color()))
                .append(Component.literal(".").withStyle(ChatFormatting.GREEN)), false);
    }

    public static void leave(ServerPlayer sp) {
        FactionState state = FactionState.get(sp.serverLevel());
        Faction faction = state.factionOf(sp.getUUID());
        if (faction == null) {
            msg(sp, "You aren't with a faction.", ChatFormatting.YELLOW);
            return;
        }
        // A founder leaving would orphan the faction, so send them to disband instead.
        if (faction.isFoundedBy(sp.getUUID()) && !sp.hasPermissions(2)) {
            msg(sp, "You founded " + faction.displayName() + " - disband it rather than walking out.",
                    ChatFormatting.RED);
            return;
        }
        state.leave(sp.getUUID());
        msg(sp, "You left " + faction.displayName() + ".", ChatFormatting.YELLOW);
    }

    public static boolean sameFaction(@Nullable String a, @Nullable String b) {
        return a != null && !a.isBlank() && a.equals(b);
    }

    private static void clearFromNpcs(@Nullable MinecraftServer server, String factionId) {
        if (server == null) return;
        for (ServerLevel world : server.getAllLevels()) {
            for (Entity e : world.getAllEntities()) {
                if (e instanceof NotchNpcEntity npc && factionId.equals(npc.getFactionId())) {
                    npc.setFactionId("");
                }
            }
        }
    }

    private static void msg(ServerPlayer sp, String text, ChatFormatting color) {
        sp.displayClientMessage(Component.literal(text).withStyle(color), false);
    }
}
