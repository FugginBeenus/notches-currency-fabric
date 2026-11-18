package net.fugginbeenus.notchcurrency.economy.bounty;

import net.fugginbeenus.notchcurrency.compat.StateData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

public final class VillageBoardState extends SavedData
        implements net.fugginbeenus.notchcurrency.compat.NbtState {

    private final Set<Long> villages = new HashSet<>();

    public static VillageBoardState get(ServerLevel level) {
        return StateData.getOrCreate(level.getDataStorage(), VillageBoardState::new,
                VillageBoardState::load, "notchcurrency_village_boards");
    }

    public VillageBoardState() {}

    public boolean claim(long village) {
        if (!villages.add(village)) return false;
        setDirty();
        return true;
    }

    public static VillageBoardState load(CompoundTag nbt) {
        VillageBoardState s = new VillageBoardState();
        int count = nbt.getInt("count");
        for (int i = 0; i < count; i++) {
            if (nbt.contains("v" + i)) s.villages.add(nbt.getLong("v" + i));
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
        nbt.putInt("count", villages.size());
        int i = 0;
        for (long key : villages) nbt.putLong("v" + i++, key);
        return nbt;
    }
}
