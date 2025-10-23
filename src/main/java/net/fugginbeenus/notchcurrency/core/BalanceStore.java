package net.fugginbeenus.notchcurrency.core;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.UUID;

/**
 * Thin facade used by the rest of the mod.
 * Keeps your existing BalanceStore.get/add calls intact while delegating
 * to the world-persistent BalanceState.
 */
public final class BalanceStore {

    private BalanceStore() {}

    private static BalanceState state(MinecraftServer server) {
        return BalanceState.get(server);
    }

    /* --------- API by player --------- */

    public static int get(ServerPlayerEntity sp) {
        return state(sp.getServer()).get(sp.getUuid());
    }

    public static int set(ServerPlayerEntity sp, int value) {
        return state(sp.getServer()).set(sp.getUuid(), value);
    }

    public static int add(ServerPlayerEntity sp, int delta) {
        return state(sp.getServer()).add(sp.getUuid(), delta);
    }

    public static int subtract(ServerPlayerEntity sp, int delta) {
        return state(sp.getServer()).subtract(sp.getUuid(), delta);
    }

    /* --------- API by UUID (optional helpers) --------- */

    public static int get(MinecraftServer server, UUID id) {
        return state(server).get(id);
    }

    public static int set(MinecraftServer server, UUID id, int value) {
        return state(server).set(id, value);
    }

    public static int add(MinecraftServer server, UUID id, int delta) {
        return state(server).add(id, delta);
    }

    public static int subtract(MinecraftServer server, UUID id, int delta) {
        return state(server).subtract(id, delta);
    }
}
