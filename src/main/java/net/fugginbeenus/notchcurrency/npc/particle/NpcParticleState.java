package net.fugginbeenus.notchcurrency.npc.particle;

import net.fugginbeenus.notchcurrency.compat.StateData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class NpcParticleState extends SavedData implements net.fugginbeenus.notchcurrency.compat.NbtState {

    private static final String DATA_KEY = "notchcurrency_npc_particles";
    public static final int MAX = 64;

    private final Map<String, ParticleEffect> effects = new LinkedHashMap<>();

    public static NpcParticleState get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) throw new IllegalStateException("Overworld not loaded");
        DimensionDataStorage manager = overworld.getDataStorage();
        return StateData.getOrCreate(manager, NpcParticleState::fresh, NpcParticleState::fromNbt, DATA_KEY);
    }

    private static NpcParticleState fresh() {
        NpcParticleState s = new NpcParticleState();
        s.addStarters();
        return s;
    }

    public int addStarters() {
        int added = 0;
        for (ParticleEffect e : StarterEffects.all()) {
            if (effects.containsKey(e.name().toLowerCase())) continue;
            effects.put(e.name().toLowerCase(), e);
            added++;
        }
        if (added > 0) setDirty();
        return added;
    }

    @Nullable
    public ParticleEffect get(@Nullable String name) {
        return name == null || name.isBlank() ? null : effects.get(name.toLowerCase());
    }

    public List<ParticleEffect> all() { return new ArrayList<>(effects.values()); }

    public boolean isFull() { return effects.size() >= MAX; }

    public void put(ParticleEffect effect) {
        if (effect == null || effect.name().isBlank()) return;
        if (effects.size() >= MAX && !effects.containsKey(effect.name().toLowerCase())) return;
        effects.put(effect.name().toLowerCase(), effect);
        setDirty();
    }

    public boolean remove(String name) {
        if (name == null || effects.remove(name.toLowerCase()) == null) return false;
        setDirty();
        return true;
    }

    public static NpcParticleState fromNbt(CompoundTag nbt) {
        NpcParticleState s = new NpcParticleState();
        ListTag list = nbt.getList("Effects", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ParticleEffect e = ParticleEffect.fromNbt(list.getCompound(i));
            if (!e.name().isBlank()) s.effects.put(e.name().toLowerCase(), e);
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
        ListTag list = new ListTag();
        for (ParticleEffect e : effects.values()) list.add(e.toNbt());
        nbt.put("Effects", list);
        return nbt;
    }
}
