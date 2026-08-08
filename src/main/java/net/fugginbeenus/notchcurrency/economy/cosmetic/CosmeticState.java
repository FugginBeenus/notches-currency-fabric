package net.fugginbeenus.notchcurrency.economy.cosmetic;

import net.fugginbeenus.notchcurrency.compat.StateData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class CosmeticState extends SavedData implements net.fugginbeenus.notchcurrency.compat.NbtState {

    private static final String DATA_KEY = "notchcurrency_cosmetics";

    private final Map<UUID, Set<String>> owned = new HashMap<>();

    public static CosmeticState get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage mgr = overworld.getDataStorage();
        return StateData.getOrCreate(mgr, CosmeticState::new, CosmeticState::fromNbt, DATA_KEY);
    }

    public boolean owns(UUID player, String offerId) {
        Set<String> set = owned.get(player);
        return set != null && set.contains(offerId);
    }

    public void markOwned(UUID player, String offerId) {
        owned.computeIfAbsent(player, k -> new HashSet<>()).add(offerId);
        setDirty();
    }

    // ---- NBT ----

    private static CosmeticState fromNbt(CompoundTag nbt) {
        CosmeticState state = new CosmeticState();
        ListTag players = nbt.getList("Players", Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag entry = players.getCompound(i);
            UUID id = net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(entry, "Player");
            Set<String> set = new HashSet<>();
            ListTag ids = entry.getList("Owned", Tag.TAG_STRING);
            for (int j = 0; j < ids.size(); j++) {
                set.add(ids.getString(j));
            }
            state.owned.put(id, set);
        }
        return state;
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
        ListTag players = new ListTag();
        for (Map.Entry<UUID, Set<String>> e : owned.entrySet()) {
            CompoundTag entry = new CompoundTag();
            net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(entry, "Player", e.getKey());
            ListTag ids = new ListTag();
            for (String s : e.getValue()) {
                ids.add(net.minecraft.nbt.StringTag.valueOf(s));
            }
            entry.put("Owned", ids);
            players.add(entry);
        }
        nbt.put("Players", players);
        return nbt;
    }
}
