package net.fugginbeenus.notchcurrency.core;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World-saved storage for player balances.
 * File name: leveldata/data/notchcurrency_balances.dat
 */
public class BalanceState extends PersistentState {

    private static final String KEY_ROOT = "balances";

    // UUID -> balance
    private final Map<UUID, Integer> balances = new HashMap<>();

    public BalanceState() {}

    /* ---------- API ---------- */

    public int get(UUID id) {
        return balances.getOrDefault(id, 0);
    }

    public int set(UUID id, int value) {
        int v = Math.max(0, value);
        balances.put(id, v);
        this.markDirty();
        return v;
    }

    public int add(UUID id, int delta) {
        int next = Math.max(0, get(id) + delta);
        balances.put(id, next);
        this.markDirty();
        return next;
    }

    public int subtract(UUID id, int delta) {
        return add(id, -Math.max(0, delta));
    }

    /* ---------- Persistence ---------- */

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound map = new NbtCompound();
        for (Map.Entry<UUID, Integer> e : balances.entrySet()) {
            map.putInt(e.getKey().toString(), e.getValue());
        }
        nbt.put(KEY_ROOT, map);
        return nbt;
    }

    public static BalanceState readNbt(NbtCompound nbt) {
        BalanceState state = new BalanceState();
        if (nbt.contains(KEY_ROOT)) {
            NbtCompound map = nbt.getCompound(KEY_ROOT);
            for (String key : map.getKeys()) {
                try {
                    UUID id = UUID.fromString(key);
                    state.balances.put(id, map.getInt(key));
                } catch (IllegalArgumentException ignored) {
                    // skip malformed keys
                }
            }
        }
        return state;
    }

    /* ---------- Loader ---------- */

    public static BalanceState get(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        PersistentStateManager mgr = overworld.getPersistentStateManager();
        // 1.20.1 signature: (reader, factory, name)
        return mgr.getOrCreate(BalanceState::readNbt, BalanceState::new, "notchcurrency_balances");
    }
}
