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

public class HeartState extends SavedData implements net.fugginbeenus.notchcurrency.compat.NbtState {

    private static final String KEY_ROOT = "hearts";
    public static final double BASE_MAX_HEALTH = 20.0;
    public static final int HEALTH_PER_CRYSTAL = 2;
    public static final int MAX_CRYSTALS = 20;
    private final Map<UUID, Integer> absorbed = new HashMap<>();
    private boolean loseOnDeath = true;

    public HeartState() {}
    public int count(UUID id) {
        return absorbed.getOrDefault(id, 0);
    }

    public boolean absorb(UUID id) {
        int now = count(id);
        if (now >= MAX_CRYSTALS) return false;
        absorbed.put(id, now + 1);
        this.setDirty();
        return true;
    }

    public void set(UUID id, int crystals) {
        absorbed.put(id, Math.max(0, Math.min(MAX_CRYSTALS, crystals)));
        this.setDirty();
    }

    public boolean losesOnDeath() { return loseOnDeath; }

    public void setLosesOnDeath(boolean lose) {
        this.loseOnDeath = lose;
        this.setDirty();
    }

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

    public static double maxHealthFor(int crystals) {
        return BASE_MAX_HEALTH + (double) Math.max(0, Math.min(MAX_CRYSTALS, crystals)) * HEALTH_PER_CRYSTAL;
    }

    public static void applyTo(ServerPlayer player) {
        MinecraftServer server = serverOf(player);
        AttributeInstance max = player.getAttribute(Attributes.MAX_HEALTH);
        if (max == null) return;

        double want = maxHealthFor(get(server).count(player.getUUID()));
        if (max.getBaseValue() != want) max.setBaseValue(want);
        if (player.getHealth() > want) player.setHealth((float) want);
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

    public static MinecraftServer serverOf(ServerPlayer player) {
        return player.serverLevel().getServer();
    }

    public static HeartState get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage mgr = overworld.getDataStorage();
        return StateData.getOrCreate(mgr, HeartState::new, HeartState::load, "notchcurrency_hearts");
    }
}
