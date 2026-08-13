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

    // ---- A balloon of your own, on joining ----

    /**
     * Whether each player gets one in the sky when they log in.
     *
     * <p>Off by default. A weekly wave over spawn is a thing a server puts on; loot arriving because
     * somebody logged in is a change to how the server pays out, and that should be chosen.
     */
    public boolean onJoin = false;

    /**
     * How long before a player can be given another, in minutes.
     *
     * <p>The whole point of the timer. Without it, logging out and back in is a loot button.
     */
    public int joinCooldownMinutes = 720;

    /** How far up, and how far off to the side, a joining player's balloon appears. */
    public int joinHeight = 40;
    public int joinSpread = 12;

    /**
     * Whether a player has to be inside the balloon area to get one.
     *
     * <p>Off means anybody anywhere gets theirs, which is the reading that gives everyone on the
     * server a chance rather than only the people who live near spawn. On ties it to the area, for
     * a server that wants balloons to be a spawn-town thing.
     */
    public boolean joinInAreaOnly = false;

    /** When each player was last given one, so the cooldown survives a restart. */
    public final java.util.Map<java.util.UUID, Long> lastJoinBalloon = new java.util.HashMap<>();

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
        s.onJoin = nbt.getBoolean("onJoin");
        s.joinInAreaOnly = nbt.getBoolean("joinInAreaOnly");
        s.joinCooldownMinutes = nbt.contains("joinCooldown") ? nbt.getInt("joinCooldown") : 720;
        s.joinHeight = nbt.contains("joinHeight") ? nbt.getInt("joinHeight") : 40;
        s.joinSpread = nbt.contains("joinSpread") ? nbt.getInt("joinSpread") : 12;

        // Numbered keys rather than a list tag, the same way the mail stores its parcels: reading a
        // list changed shape more than once across the versions this mod covers.
        int seen = nbt.getInt("joinSeenCount");
        for (int i = 0; i < seen; i++) {
            if (!nbt.contains("joinSeen" + i)) continue;
            CompoundTag entry = nbt.getCompound("joinSeen" + i);
            if (!net.fugginbeenus.notchcurrency.compat.Nbt.hasUuid(entry, "id")) continue;
            s.lastJoinBalloon.put(net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(entry, "id"),
                    entry.getLong("at"));
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
        nbt.putInt("cx", center.getX());
        nbt.putInt("cy", center.getY());
        nbt.putInt("cz", center.getZ());
        nbt.putInt("radius", radius);
        nbt.putInt("minY", minY);
        nbt.putInt("maxY", maxY);
        nbt.putInt("perDay", perDay);
        nbt.putBoolean("announce", announce);
        nbt.putBoolean("configured", configured);
        nbt.putBoolean("onJoin", onJoin);
        nbt.putBoolean("joinInAreaOnly", joinInAreaOnly);
        nbt.putInt("joinCooldown", joinCooldownMinutes);
        nbt.putInt("joinHeight", joinHeight);
        nbt.putInt("joinSpread", joinSpread);

        nbt.putInt("joinSeenCount", lastJoinBalloon.size());
        int i = 0;
        for (var seen : lastJoinBalloon.entrySet()) {
            CompoundTag entry = new CompoundTag();
            net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(entry, "id", seen.getKey());
            entry.putLong("at", seen.getValue());
            nbt.put("joinSeen" + i++, entry);
        }
        return nbt;
    }
}
