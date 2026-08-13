package net.fugginbeenus.notchcurrency.crate;

import net.fugginbeenus.notchcurrency.compat.StateData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public final class BalloonConfigState extends SavedData implements net.fugginbeenus.notchcurrency.compat.NbtState {
    public BlockPos center = new BlockPos(0, 80, 0);

    /**
     * Whether anybody has actually chosen where balloons go.
     *
     * <p>The centre above is a placeholder, not a choice, and without this there is no way to tell
     * the two apart. A world nobody has set up would drop balloons on its origin every week and
     * announce it, which reads as a bug because it is one.
     */
    public boolean configured = false;
    public int radius = 25;
    public int minY = 110;
    public int maxY = 150;
    public int perDay = 3;
    public boolean announce = true;

    // ---- One each, alongside the wave ----

    /**
     * Whether each player online also gets one in the sky above them when a wave fires.
     *
     * <p>The wave itself lands over one place, so it belongs to whoever lives near it. This is the
     * part everybody gets. Off until a server turns it on: loot reaching every player is a change to
     * how a server pays out rather than a decoration.
     */
    public boolean perPlayer = false;

    /** How far up, and how far off to the side, a player's own balloon appears. */
    public int playerHeight = 40;
    public int playerSpread = 12;

    /**
     * Whether a player has to be inside the balloon area to get one.
     *
     * <p>Off means everybody online gets theirs wherever they are, which is what gives the whole
     * server a chance rather than only the people who live near spawn. On keeps balloons a
     * spawn-town thing.
     */
    public boolean playerInAreaOnly = false;

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
        s.configured = nbt.getBoolean("configured");
        s.radius = nbt.getInt("radius");
        s.minY = nbt.getInt("minY");
        s.maxY = nbt.getInt("maxY");
        s.perDay = nbt.getInt("perDay");
        s.announce = nbt.getBoolean("announce");
        s.perPlayer = nbt.getBoolean("perPlayer");
        s.playerInAreaOnly = nbt.getBoolean("playerInAreaOnly");
        s.playerHeight = nbt.contains("playerHeight") ? nbt.getInt("playerHeight") : 40;
        s.playerSpread = nbt.contains("playerSpread") ? nbt.getInt("playerSpread") : 12;
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
        nbt.putBoolean("configured", configured);
        nbt.putBoolean("perPlayer", perPlayer);
        nbt.putBoolean("playerInAreaOnly", playerInAreaOnly);
        nbt.putInt("playerHeight", playerHeight);
        nbt.putInt("playerSpread", playerSpread);
        return nbt;
    }
}
