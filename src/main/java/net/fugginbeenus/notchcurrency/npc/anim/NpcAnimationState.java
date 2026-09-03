package net.fugginbeenus.notchcurrency.npc.anim;

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

public class NpcAnimationState extends SavedData implements net.fugginbeenus.notchcurrency.compat.NbtState {

    private static final String DATA_KEY = "notchcurrency_npc_animations";
    public static final int MAX = 64;

    private final Map<String, NpcAnimation> animations = new LinkedHashMap<>();

    public static NpcAnimationState get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) throw new IllegalStateException("Overworld not loaded");
        DimensionDataStorage manager = overworld.getDataStorage();
        return StateData.getOrCreate(manager, NpcAnimationState::new, NpcAnimationState::fromNbt, DATA_KEY);
    }

    @Nullable
    public NpcAnimation get(@Nullable String name) {
        return name == null || name.isBlank() ? null : animations.get(name.toLowerCase());
    }

    public List<NpcAnimation> all() { return new ArrayList<>(animations.values()); }

    public void put(NpcAnimation anim) {
        if (anim == null || anim.name().isBlank()) return;
        if (animations.size() >= MAX && !animations.containsKey(anim.name().toLowerCase())) return;
        animations.put(anim.name().toLowerCase(), anim);
        setDirty();
    }

    public boolean remove(String name) {
        if (name == null || animations.remove(name.toLowerCase()) == null) return false;
        setDirty();
        return true;
    }

    public static NpcAnimationState fromNbt(CompoundTag nbt) {
        NpcAnimationState s = new NpcAnimationState();
        ListTag list = nbt.getList("Animations", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            NpcAnimation a = NpcAnimation.fromNbt(list.getCompound(i));
            if (!a.name().isBlank()) s.animations.put(a.name().toLowerCase(), a);
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
        for (NpcAnimation a : animations.values()) list.add(a.toNbt());
        nbt.put("Animations", list);
        return nbt;
    }
}
