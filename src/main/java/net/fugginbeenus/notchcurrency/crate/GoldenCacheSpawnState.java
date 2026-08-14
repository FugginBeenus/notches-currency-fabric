package net.fugginbeenus.notchcurrency.crate;

import net.fugginbeenus.notchcurrency.compat.StateData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

/**
 * Which chunks have already had their golden cache placed.
 *
 * <p>Small on purpose. Whether a chunk is a cache chunk at all is worked out from the world seed and
 * the chunk's own coordinates, so that answer never has to be stored: it is the same every time it
 * is asked. Only the rare chunk that actually wins needs remembering, so that opening a cache and
 * coming back later does not hand out another.
 *
 * <p>A world that has run for years holds a few dozen numbers here, not one per chunk ever loaded.
 */
public final class GoldenCacheSpawnState extends SavedData
        implements net.fugginbeenus.notchcurrency.compat.NbtState {

    private final Set<Long> placed = new HashSet<>();

    /**
     * Caches that are out there and have not been opened yet.
     *
     * <p>Kept so the world can be held to a fixed number waiting at once. Without it, covering new
     * ground quickly is a way to make more of them exist, which is the opposite of rare.
     */
    private final Set<Long> outstanding = new HashSet<>();

    public static GoldenCacheSpawnState get(ServerLevel world) {
        return StateData.getOrCreate(world.getDataStorage(), GoldenCacheSpawnState::new,
                GoldenCacheSpawnState::load, "notchcurrency_cache_spawns");
    }

    public GoldenCacheSpawnState() {}

    /** @return true if this chunk had not been done before, and is now marked as done */
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

    /** @return true if that position was one of ours */
    public boolean clearOutstanding(long posKey) {
        if (!outstanding.remove(posKey)) return false;
        setDirty();
        return true;
    }

    public static GoldenCacheSpawnState load(CompoundTag nbt) {
        GoldenCacheSpawnState s = new GoldenCacheSpawnState();
        // Numbered keys rather than a long array: getLongArray started returning an Optional at
        // 26, and a counted run of keys reads the same on every version this mod covers.
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
        nbt.putInt("count", placed.size());
        int i = 0;
        for (long key : placed) nbt.putLong("c" + i++, key);

        nbt.putInt("waitingCount", outstanding.size());
        int w = 0;
        for (long key : outstanding) nbt.putLong("w" + w++, key);
        return nbt;
    }
}
