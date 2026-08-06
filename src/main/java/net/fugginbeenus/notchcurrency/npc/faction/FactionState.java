package net.fugginbeenus.notchcurrency.npc.faction;

import net.fugginbeenus.notchcurrency.compat.StateData;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FactionState extends PersistentState {

    private static final String DATA_KEY = "notchcurrency_factions";

    public static final int MAX_FACTIONS = 64;

    private final Map<String, Faction> factions = new LinkedHashMap<>();
    private final Map<UUID, String> membership = new LinkedHashMap<>();

    public static FactionState get(ServerWorld world) {
        return get(world.getServer());
    }

    public static FactionState get(MinecraftServer server) {
        ServerWorld overworld = server.getWorld(World.OVERWORLD);
        if (overworld == null) throw new IllegalStateException("Overworld not loaded");
        PersistentStateManager manager = overworld.getPersistentStateManager();
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
        markDirty();
        return true;
    }

    public boolean remove(String id) {
        if (factions.remove(id) == null) return false;
        membership.entrySet().removeIf(e -> e.getValue().equals(id));
        markDirty();
        return true;
    }

    public void touch() { markDirty(); }

    // ---- membership ----

    @Nullable
    public String factionIdOf(UUID player) { return membership.get(player); }

    @Nullable
    public Faction factionOf(UUID player) { return get(membership.get(player)); }

    public void join(UUID player, String factionId) {
        if (!factions.containsKey(factionId)) return;
        membership.put(player, factionId);
        markDirty();
    }

    public void leave(UUID player) {
        if (membership.remove(player) != null) markDirty();
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
    /*public NbtCompound writeNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
    *///?} else {
    public NbtCompound writeNbt(NbtCompound nbt) {
    //?}
        NbtList list = new NbtList();
        for (Faction f : factions.values()) list.add(f.toNbt());
        nbt.put("Factions", list);

        NbtList members = new NbtList();
        for (var e : membership.entrySet()) {
            NbtCompound m = new NbtCompound();
            m.putUuid("Player", e.getKey());
            m.putString("Faction", e.getValue());
            members.add(m);
        }
        nbt.put("Members", members);
        return nbt;
    }

    public static FactionState fromNbt(NbtCompound nbt) {
        FactionState state = new FactionState();
        NbtList list = nbt.getList("Factions", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            Faction f = Faction.fromNbt(list.getCompound(i));
            if (f != null) state.factions.put(f.id(), f);
        }
        NbtList members = nbt.getList("Members", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < members.size(); i++) {
            NbtCompound m = members.getCompound(i);
            String factionId = m.getString("Faction");
            // Drop members of factions that no longer exist rather than carrying a dead pointer.
            if (m.containsUuid("Player") && state.factions.containsKey(factionId)) {
                state.membership.put(m.getUuid("Player"), factionId);
            }
        }
        return state;
    }
}
