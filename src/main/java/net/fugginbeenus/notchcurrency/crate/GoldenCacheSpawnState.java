package net.fugginbeenus.notchcurrency.crate;

import net.fugginbeenus.notchcurrency.compat.StateData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

public final class GoldenCacheSpawnState extends SavedData
        implements net.fugginbeenus.notchcurrency.compat.NbtState {

    private final Set<Long> placed = new HashSet<>();
    private final Set<Long> outstanding = new HashSet<>();

    public static GoldenCacheSpawnState get(ServerLevel world) {
        return StateData.getOrCreate(world.getDataStorage(), GoldenCacheSpawnState::new,
                GoldenCacheSpawnState::load, "notchcurrency_cache_spawns");
    }

    public GoldenCacheSpawnState() {}

    public boolean claim(long chunkKey) {
        if (!placed.add(chunkKey)) return false;
        setDirty();
        return true;
    }

    public int outstandingCount() {
        return outstanding.size();
    }

    public java.util.List<Long> outstandingPositions() {
        return java.util.List.copyOf(outstanding);
    }

    public void addOutstanding(long posKey) {
        if (outstanding.add(posKey)) setDirty();
    }

    public boolean clearOutstanding(long posKey) {
        if (!outstanding.remove(posKey)) return false;
        setDirty();
        return true;
    }

    public static GoldenCacheSpawnState load(CompoundTag nbt) {
        GoldenCacheSpawnState s = new GoldenCacheSpawnState();
        int count = nbt.getInt("count");
        for (int i = 0; i < count; i++) {
            if (nbt.contains("c" + i)) s.placed.add(nbt.getLong("c" + i));
        }
        int waiting = nbt.getInt("waitingCount");
        for (int i = 0; i < waiting; i++) {
            if (nbt.contains("w" + i)) s.outstanding.add(nbt.getLong("w" + i));
        }
        return s;
    }

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
        nbt.putInt("count", placed.size());
        int i = 0;
        for (long key : placed) nbt.putLong("c" + i++, key);

        nbt.putInt("waitingCount", outstanding.size());
        int w = 0;
        for (long key : outstanding) nbt.putLong("w" + w++, key);
        return nbt;
    }
}
