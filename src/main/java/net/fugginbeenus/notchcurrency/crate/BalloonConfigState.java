package net.fugginbeenus.notchcurrency.crate;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

public final class BalloonConfigState extends PersistentState {
    public BlockPos center = new BlockPos(0, 80, 0);
    public int radius = 25;
    public int minY = 110;
    public int maxY = 150;
    public int perDay = 3;
    public boolean announce = true;

    public static BalloonConfigState get(ServerWorld world) {
        PersistentStateManager mgr = world.getPersistentStateManager();
        return mgr.getOrCreate(BalloonConfigState::readNbt, BalloonConfigState::new, "notchcurrency_balloon_cfg");
    }

    public BalloonConfigState() {}

    public static BalloonConfigState readNbt(NbtCompound nbt) {
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

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
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
