package net.fugginbeenus.notchcurrency.core;

import net.fugginbeenus.notchcurrency.compat.StateData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * How many heart crystals each player has absorbed.
 *
 * <p>Kept here rather than on the player, because a player is not a durable place to put this. The
 * entity is thrown away and rebuilt on death, and what carries across is decided by vanilla and by
 * whatever the server has set keepInventory to. A crystal that is exceptionally rare must not be
 * something a death can quietly take back, so the count lives in the world save and the player's
 * health is rebuilt from it rather than the other way round.
 *
 * <p>The count is the truth. Max health is a consequence, reapplied on join and after every respawn.
 */
public class HeartState extends SavedData implements net.fugginbeenus.notchcurrency.compat.NbtState {

    private static final String KEY_ROOT = "hearts";

    /** Vanilla's own max health, and the floor this can never take a player below. */
    public static final double BASE_MAX_HEALTH = 20.0;

    /** One crystal is one heart, and a heart is two health. */
    public static final int HEALTH_PER_CRYSTAL = 2;

    /**
     * Two extra rows on the health bar, which is ten hearts a row. Twenty crystals, taking a player
     * from ten hearts to thirty.
     */
    public static final int MAX_CRYSTALS = 20;

    // UUID -> crystals absorbed
    private final Map<UUID, Integer> absorbed = new HashMap<>();

    /**
     * Whether dying costs a crystal.
     *
     * <p>On by default, because the risk is what the item is for: hearts you cannot lose are just a
     * bigger number. A server that would rather hand out permanent ones says so once, with
     * {@code /hearts loseondeath false}, and it is remembered in the world.
     */
    private boolean loseOnDeath = true;

    public HeartState() {}

    /* ---------- API ---------- */

    public int count(UUID id) {
        return absorbed.getOrDefault(id, 0);
    }

    /** @return false if that player is already carrying all the hearts they may */
    public boolean absorb(UUID id) {
        int now = count(id);
        if (now >= MAX_CRYSTALS) return false;
        absorbed.put(id, now + 1);
        this.setDirty();
        return true;
    }

    /** Admin path: hand back to zero, or set an exact count. Clamped rather than trusted. */
    public void set(UUID id, int crystals) {
        absorbed.put(id, Math.max(0, Math.min(MAX_CRYSTALS, crystals)));
        this.setDirty();
    }

    public boolean losesOnDeath() { return loseOnDeath; }

    public void setLosesOnDeath(boolean lose) {
        this.loseOnDeath = lose;
        this.setDirty();
    }

    /**
     * Takes one crystal back after a death, down to none.
     *
     * <p>Only the extra hearts are at stake. The ten a player is born with are not this system's to
     * take, so this stops at zero rather than going on to make death permanent.
     *
     * @return how many are left, or -1 if nothing was taken
     */
    public static int onDeath(ServerPlayer player) {
        HeartState state = get(serverOf(player));
        if (!state.loseOnDeath) return -1;

        int had = state.count(player.getUUID());
        if (had <= 0) return -1;
        int left = had - 1;
        state.absorbed.put(player.getUUID(), left);
        state.setDirty();
        return left;
    }

    /* ---------- Applying it ---------- */

    public static double maxHealthFor(int crystals) {
        return BASE_MAX_HEALTH + (double) Math.max(0, Math.min(MAX_CRYSTALS, crystals)) * HEALTH_PER_CRYSTAL;
    }

    /**
     * Rebuilds a player's max health from their crystal count.
     *
     * <p>The base value is set rather than a modifier added. A modifier needs an identity to be
     * removed by later, and that identity is a UUID on the older versions and a resource location
     * on the newer ones, which is a version branch for no gain: nothing else here touches max
     * health, so there is nothing to stack with.
     */
    public static void applyTo(ServerPlayer player) {
        MinecraftServer server = serverOf(player);
        AttributeInstance max = player.getAttribute(Attributes.MAX_HEALTH);
        if (max == null) return;

        double want = maxHealthFor(get(server).count(player.getUUID()));
        if (max.getBaseValue() != want) max.setBaseValue(want);

        // A player holding more health than the rules now allow, because the world was opened
        // without the mod or an admin took crystals back, is clamped. Left alone they would keep
        // hearts that nothing will ever refill.
        if (player.getHealth() > want) player.setHealth((float) want);
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
        for (Map.Entry<UUID, Integer> e : absorbed.entrySet()) {
            if (e.getValue() > 0) map.putInt(e.getKey().toString(), e.getValue());
        }
        nbt.put(KEY_ROOT, map);
        nbt.putBoolean("loseOnDeath", loseOnDeath);
        return nbt;
    }

    public static HeartState load(CompoundTag nbt) {
        HeartState state = new HeartState();
        // Absent in a world saved before the toggle existed, which should read as the default
        // rather than as off.
        if (nbt.contains("loseOnDeath")) state.loseOnDeath = nbt.getBoolean("loseOnDeath");
        if (nbt.contains(KEY_ROOT)) {
            CompoundTag map = nbt.getCompound(KEY_ROOT);
            for (String key : map.getAllKeys()) {
                try {
                    state.absorbed.put(UUID.fromString(key),
                            Math.max(0, Math.min(MAX_CRYSTALS, map.getInt(key))));
                } catch (IllegalArgumentException ignored) {
                    // skip malformed keys
                }
            }
        }
        return state;
    }

    /* ---------- Loader ---------- */

    // ServerPlayer.getServer was removed at 1.21.11. The level has always had it.
    public static MinecraftServer serverOf(ServerPlayer player) {
        return player.serverLevel().getServer();
    }

    public static HeartState get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage mgr = overworld.getDataStorage();
        return StateData.getOrCreate(mgr, HeartState::new, HeartState::load, "notchcurrency_hearts");
    }
}
