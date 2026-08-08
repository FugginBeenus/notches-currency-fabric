package net.fugginbeenus.notchcurrency.crate;

import net.fugginbeenus.notchcurrency.compat.StateData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public final class BalloonConfigState extends SavedData implements net.fugginbeenus.notchcurrency.compat.NbtState {
    public BlockPos center = new BlockPos(0, 80, 0);
    public int radius = 25;
    public int minY = 110;
    public int maxY = 150;
    public int perDay = 3;
    public boolean announce = true;

    public static BalloonConfigState get(ServerLevel world) {
        DimensionDataStorage mgr = world.getDataStorage();
        return StateData.getOrCreate(mgr, BalloonConfigState::new, BalloonConfigState::load, "notchcurrency_balloon_cfg");
    }

    public BalloonConfigState() {}

    public static BalloonConfigState load(CompoundTag nbt) {
        BalloonConfigState s = new BalloonConfigState();
        int cx = nbt.getInt("cx");
        int cy = nbt.getInt("cy");
        int cz = nbt.getInt("cz");
        s.center = new BlockPos(cx, cy, cz);
        s.radius = nbt.getInt("radius");
        s.minY = nbt.getInt("minY");
        s.maxY = nbt.getInt("maxY");
        s.perDay = nbt.getInt("perDay");
        s.announce = nbt.getBoolean("announce");
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
        nbt.putInt("cx", center.getX());
        nbt.putInt("cy", center.getY());
        nbt.putInt("cz", center.getZ());
        nbt.putInt("radius", radius);
        nbt.putInt("minY", minY);
        nbt.putInt("maxY", maxY);
        nbt.putInt("perDay", perDay);
        nbt.putBoolean("announce", announce);
        return nbt;
    }
}
