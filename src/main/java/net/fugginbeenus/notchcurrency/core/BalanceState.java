package net.fugginbeenus.notchcurrency.core;

import net.fugginbeenus.notchcurrency.compat.StateData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BalanceState extends SavedData implements net.fugginbeenus.notchcurrency.compat.NbtState {

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
        this.setDirty();
        return v;
    }

    public long add(UUID id, long delta) {
        long next = Math.max(0L, get(id) + delta);
        balances.put(id, next);
        this.setDirty();
        return next;
    }

    public long subtract(UUID id, long delta) {
        return add(id, -Math.max(0L, delta));
    }

    public java.util.Map<UUID, Long> snapshot() {
        return java.util.Map.copyOf(balances);
    }

    public long totalSupply() {
        long sum = 0L;
        for (long v : balances.values()) sum += v;
        return sum;
    }

    public int accountCount() {
        int n = 0;
        for (long v : balances.values()) if (v > 0) n++;
        return n;
    }

    /* ---------- Persistence ---------- */

    // Only the older versions call this. 1.21.11 hands writeNbt to a codec instead, so there is
    // nothing on SavedData left to override there.
    //? if >=1.21.11 {
    /*
    *///?} elif >=1.21 {
    /*@Override
    public CompoundTag save(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
        return writeNbt(nbt);
    }
    *///?} else {
    @Override
    public CompoundTag save(CompoundTag nbt) {
        return writeNbt(nbt);
    }
    //?}

    @Override
    public CompoundTag writeNbt(CompoundTag nbt) {
        CompoundTag map = new CompoundTag();
        for (Map.Entry<UUID, Long> e : balances.entrySet()) {
            map.putLong(e.getKey().toString(), e.getValue());
        }
        nbt.put(KEY_ROOT, map);
        return nbt;
    }

    public static BalanceState load(CompoundTag nbt) {
        BalanceState state = new BalanceState();
        if (nbt.contains(KEY_ROOT)) {
            CompoundTag map = nbt.getCompound(KEY_ROOT);
            for (String key : map.getAllKeys()) {
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
        ServerLevel overworld = server.overworld();
        DimensionDataStorage mgr = overworld.getDataStorage();
        // 1.20.1 signature: (reader, factory, name)
        return StateData.getOrCreate(mgr, BalanceState::new, BalanceState::load, "notchcurrency_balances");
    }
}
