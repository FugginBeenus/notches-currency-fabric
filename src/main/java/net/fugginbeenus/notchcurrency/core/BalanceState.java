package net.fugginbeenus.notchcurrency.core;

import net.fugginbeenus.notchcurrency.compat.StateData;

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
 *
 * Balances are stored as {@code long} so a server economy can grow past the
 * ~2.1 billion limit of {@code int} without silently overflowing.
 */
public class BalanceState extends PersistentState {

    private static final String KEY_ROOT = "balances";

    // UUID -> balance
    private final Map<UUID, Long> balances = new HashMap<>();

    public BalanceState() {}

    /* ---------- API ---------- */

    public long get(UUID id) {
        return balances.getOrDefault(id, 0L);
    }

    public long set(UUID id, long value) {
        long v = Math.max(0L, value);
        balances.put(id, v);
        this.markDirty();
        return v;
    }

    public long add(UUID id, long delta) {
        long next = Math.max(0L, get(id) + delta);
        balances.put(id, next);
        this.markDirty();
        return next;
    }

    public long subtract(UUID id, long delta) {
        return add(id, -Math.max(0L, delta));
    }

    /** Immutable snapshot of every known balance (for /baltop and /eco stats). */
    public java.util.Map<UUID, Long> snapshot() {
        return java.util.Map.copyOf(balances);
    }

    /** Sum of all balances = total money supply in circulation. */
    public long totalSupply() {
        long sum = 0L;
        for (long v : balances.values()) sum += v;
        return sum;
    }

    /** Number of accounts with a non-zero balance. */
    public int accountCount() {
        int n = 0;
        for (long v : balances.values()) if (v > 0) n++;
        return n;
    }

    /* ---------- Persistence ---------- */

    @Override
    //? if >=1.21 {
    /*public NbtCompound writeNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
    *///?} else {
    public NbtCompound writeNbt(NbtCompound nbt) {
    //?}
        NbtCompound map = new NbtCompound();
        for (Map.Entry<UUID, Long> e : balances.entrySet()) {
            map.putLong(e.getKey().toString(), e.getValue());
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
                    // getLong tolerates older int-typed entries, so this still reads legacy data.
                    state.balances.put(id, map.getLong(key));
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
        return StateData.getOrCreate(mgr, BalanceState::new, BalanceState::readNbt, "notchcurrency_balances");
    }
}
