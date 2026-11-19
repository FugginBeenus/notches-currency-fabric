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

public class FactionState extends SavedData implements net.fugginbeenus.notchcurrency.compat.NbtState {

    private static final String DATA_KEY = "notchcurrency_factions";

    public static final int MAX_FACTIONS = 64;

    private final Map<String, Faction> factions = new LinkedHashMap<>();
    private final Map<String, Faction> houses = new LinkedHashMap<>();
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

    @Nullable
    public Faction get(@Nullable String id) {
        if (id == null || id.isBlank()) return null;
        Faction f = factions.get(id);
        return f != null ? f : houses.get(id);
    }

    public boolean exists(@Nullable String id) { return get(id) != null; }

    public List<Faction> all() { return new ArrayList<>(factions.values()); }

    public int count() { return factions.size(); }

    public List<Faction> housesOf(String factionId) {
        List<Faction> out = new ArrayList<>();
        for (Faction h : houses.values()) {
            if (h.parent().equals(factionId)) out.add(h);
        }
        return out;
    }

    public List<Faction> allHouses() { return new ArrayList<>(houses.values()); }

    @Nullable
    public Faction parentOf(Faction faction) {
        return faction.isHouse() ? factions.get(faction.parent()) : faction;
    }

    public boolean addHouse(Faction house) {
        if (house == null || !house.isHouse() || !factions.containsKey(house.parent()) || get(house.id()) != null) {
            return false;
        }
        Faction parent = factions.get(house.parent());
        house.setColor(parent.color());
        houses.put(house.id(), house);
        setDirty();
        return true;
    }

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
        Faction house = houses.remove(id);
        if (house != null) {
            membership.replaceAll((player, group) -> group.equals(id) ? house.parent() : group);
            setDirty();
            return true;
        }
        if (factions.remove(id) == null) return false;
        houses.values().removeIf(h -> h.parent().equals(id));
        membership.entrySet().removeIf(e -> Faction.rootId(e.getValue()).equals(id));
        setDirty();
        return true;
    }

    public void touch() {
        syncHouseColors();
        setDirty();
    }

    private void syncHouseColors() {
        for (Faction h : houses.values()) {
            Faction parent = factions.get(h.parent());
            if (parent != null) h.setColor(parent.color());
        }
    }

    @Nullable
    public String factionIdOf(UUID player) {
        String group = membership.get(player);
        return group == null ? null : Faction.rootId(group);
    }

    @Nullable
    public Faction factionOf(UUID player) { return get(factionIdOf(player)); }

    @Nullable
    public String groupIdOf(UUID player) { return membership.get(player); }

    @Nullable
    public Faction houseOf(UUID player) {
        String group = membership.get(player);
        return Faction.isHouseId(group) ? houses.get(group) : null;
    }

    public boolean isIn(UUID player, @Nullable String groupId) {
        return Faction.within(membership.get(player), groupId);
    }

    public void join(UUID player, String groupId) {
        if (get(groupId) == null) return;
        membership.put(player, groupId);
        setDirty();
    }

    public void leave(UUID player) {
        if (membership.remove(player) != null) setDirty();
    }

    public List<UUID> membersOf(String groupId) {
        List<UUID> out = new ArrayList<>();
        for (var e : membership.entrySet()) {
            if (Faction.within(e.getValue(), groupId)) out.add(e.getKey());
        }
        return out;
    }

    public int memberCount(String groupId) {
        int n = 0;
        for (String id : membership.values()) {
            if (Faction.within(id, groupId)) n++;
        }
        return n;
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
        for (Faction f : factions.values()) list.add(f.toNbt());
        nbt.put("Factions", list);

        ListTag houseList = new ListTag();
        for (Faction h : houses.values()) houseList.add(h.toNbt());
        nbt.put("Houses", houseList);

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
            if (f != null && !f.isHouse()) state.factions.put(f.id(), f);
        }
        ListTag houseList = nbt.getList("Houses", Tag.TAG_COMPOUND);
        for (int i = 0; i < houseList.size(); i++) {
            Faction h = Faction.fromNbt(houseList.getCompound(i));
            if (h != null && h.isHouse() && state.factions.containsKey(h.parent())) state.houses.put(h.id(), h);
        }
        state.syncHouseColors();
        ListTag members = nbt.getList("Members", Tag.TAG_COMPOUND);
        for (int i = 0; i < members.size(); i++) {
            CompoundTag m = members.getCompound(i);
            String groupId = m.getString("Faction");
            if (!net.fugginbeenus.notchcurrency.compat.Nbt.hasUuid(m, "Player")) continue;
            UUID player = net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(m, "Player");
            if (state.get(groupId) != null) {
                state.membership.put(player, groupId);
            } else if (state.factions.containsKey(Faction.rootId(groupId))) {
                state.membership.put(player, Faction.rootId(groupId));
            }
        }
        return state;
    }
}
