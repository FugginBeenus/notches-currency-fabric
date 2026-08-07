package net.fugginbeenus.notchcurrency.npc.faction;

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
import java.util.UUID;

public class FactionState extends SavedData {

    private static final String DATA_KEY = "notchcurrency_factions";

    public static final int MAX_FACTIONS = 64;

    private final Map<String, Faction> factions = new LinkedHashMap<>();
    private final Map<UUID, String> membership = new LinkedHashMap<>();

    public static FactionState get(ServerLevel world) {
        return get(world.getServer());
    }

    public static FactionState get(MinecraftServer server) {
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) throw new IllegalStateException("Overworld not loaded");
        DimensionDataStorage manager = overworld.getDataStorage();
        return StateData.getOrCreate(manager, FactionState::new, FactionState::fromNbt, DATA_KEY);
    }

    // ---- factions ----

    @Nullable
    public Faction get(@Nullable String id) {
        return id == null || id.isBlank() ? null : factions.get(id);
    }

    public boolean exists(@Nullable String id) { return get(id) != null; }

    public List<Faction> all() { return new ArrayList<>(factions.values()); }

    public int count() { return factions.size(); }

    @Nullable
    public Faction foundedBy(UUID player) {
        for (Faction f : factions.values()) {
            if (f.isFoundedBy(player)) return f;
        }
        return null;
    }

    public boolean add(Faction faction) {
        if (faction == null || factions.size() >= MAX_FACTIONS || factions.containsKey(faction.id())) {
            return false;
        }
        factions.put(faction.id(), faction);
        setDirty();
        return true;
    }

    public boolean remove(String id) {
        if (factions.remove(id) == null) return false;
        membership.entrySet().removeIf(e -> e.getValue().equals(id));
        setDirty();
        return true;
    }

    public void touch() { setDirty(); }

    // ---- membership ----

    @Nullable
    public String factionIdOf(UUID player) { return membership.get(player); }

    @Nullable
    public Faction factionOf(UUID player) { return get(membership.get(player)); }

    public void join(UUID player, String factionId) {
        if (!factions.containsKey(factionId)) return;
        membership.put(player, factionId);
        setDirty();
    }

    public void leave(UUID player) {
        if (membership.remove(player) != null) setDirty();
    }

    public List<UUID> membersOf(String factionId) {
        List<UUID> out = new ArrayList<>();
        for (var e : membership.entrySet()) {
            if (e.getValue().equals(factionId)) out.add(e.getKey());
        }
        return out;
    }

    public int memberCount(String factionId) {
        int n = 0;
        for (String id : membership.values()) {
            if (id.equals(factionId)) n++;
        }
        return n;
    }

    // ---- NBT ----

    @Override
    //? if >=1.21 {
    /*public CompoundTag save(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
    *///?} else {
    public CompoundTag save(CompoundTag nbt) {
    //?}
        ListTag list = new ListTag();
        for (Faction f : factions.values()) list.add(f.toNbt());
        nbt.put("Factions", list);

        ListTag members = new ListTag();
        for (var e : membership.entrySet()) {
            CompoundTag m = new CompoundTag();
            net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(m, "Player", e.getKey());
            m.putString("Faction", e.getValue());
            members.add(m);
        }
        nbt.put("Members", members);
        return nbt;
    }

    public static FactionState fromNbt(CompoundTag nbt) {
        FactionState state = new FactionState();
        ListTag list = nbt.getList("Factions", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            Faction f = Faction.fromNbt(list.getCompound(i));
            if (f != null) state.factions.put(f.id(), f);
        }
        ListTag members = nbt.getList("Members", Tag.TAG_COMPOUND);
        for (int i = 0; i < members.size(); i++) {
            CompoundTag m = members.getCompound(i);
            String factionId = m.getString("Faction");
            // Drop members of factions that no longer exist rather than carrying a dead pointer.
            if (net.fugginbeenus.notchcurrency.compat.Nbt.hasUuid(m, "Player") && state.factions.containsKey(factionId)) {
                state.membership.put(net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(m, "Player"), factionId);
            }
        }
        return state;
    }
}
