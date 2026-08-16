package net.fugginbeenus.notchcurrency.economy.bounty;

import net.fugginbeenus.notchcurrency.compat.Nbt;
import net.fugginbeenus.notchcurrency.compat.StateData;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class BountyState extends SavedData implements net.fugginbeenus.notchcurrency.compat.NbtState {

    private static final String DATA_KEY = "notchcurrency_bounties";

    private final Map<UUID, Bounty> offers = new LinkedHashMap<>();
    private final Map<UUID, Map<UUID, TakenBounty>> taken = new HashMap<>();
    private final Map<UUID, Set<UUID>> completed = new HashMap<>();
    private final List<ItemStack> decrees = new ArrayList<>();

    public static BountyState get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage mgr = overworld.getDataStorage();
        return StateData.getOrCreate(mgr, BountyState::new, BountyState::fromNbt, DATA_KEY);
    }

    public void addOffer(Bounty b) {
        offers.put(b.getId(), b);
        setDirty();
    }

    public boolean removeOffer(UUID id) {
        boolean removed = offers.remove(id) != null;
        if (removed) {
            for (Set<UUID> s : completed.values()) s.remove(id);
            setDirty();
        }
        return removed;
    }

    public boolean hasCompletedOffer(UUID player, UUID offerId) {
        Set<UUID> s = completed.get(player);
        return s != null && s.contains(offerId);
    }

    public void markOfferCompleted(UUID player, UUID offerId) {
        if (offers.containsKey(offerId)) {
            completed.computeIfAbsent(player, k -> new HashSet<>()).add(offerId);
            setDirty();
        }
    }

    @Nullable
    public Bounty getOffer(UUID id) {
        return offers.get(id);
    }

    public Collection<Bounty> allOffers() {
        return offers.values();
    }

    public boolean hasTaken(UUID player, UUID offerId) {
        Map<UUID, TakenBounty> m = taken.get(player);
        return m != null && m.containsKey(offerId);
    }

    public int takeCount(UUID player) {
        Map<UUID, TakenBounty> m = taken.get(player);
        return m == null ? 0 : m.size();
    }

    public void take(UUID player, TakenBounty tb) {
        taken.computeIfAbsent(player, k -> new LinkedHashMap<>()).put(tb.bounty().getId(), tb);
        setDirty();
    }

    @Nullable
    public TakenBounty getTaken(UUID player, UUID offerId) {
        Map<UUID, TakenBounty> m = taken.get(player);
        return m == null ? null : m.get(offerId);
    }

    public List<TakenBounty> getTakenAll(UUID player) {
        Map<UUID, TakenBounty> m = taken.get(player);
        return m == null ? new ArrayList<>() : new ArrayList<>(m.values());
    }

    public void removeTaken(UUID player, UUID offerId) {
        Map<UUID, TakenBounty> m = taken.get(player);
        if (m != null && m.remove(offerId) != null) setDirty();
    }

    public int cleanupExpired(UUID player, long now) {
        Map<UUID, TakenBounty> m = taken.get(player);
        if (m == null) return 0;
        int before = m.size();
        m.values().removeIf(tb -> tb.isExpired(now));
        int dropped = before - m.size();
        if (dropped > 0) setDirty();
        return dropped;
    }

    public List<ItemStack> getDecrees() {
        return new ArrayList<>(decrees);
    }

    public void setDecrees(List<ItemStack> items) {
        decrees.clear();
        for (ItemStack s : items) if (!s.isEmpty()) decrees.add(s.copy());
        setDirty();
    }

    @Nullable
    public Set<String> activeCategories() {
        if (decrees.isEmpty()) return null;
        Set<String> cats = new HashSet<>();
        for (ItemStack d : decrees) {
            String c = BountyPools.decreeCategory(BuiltInRegistries.ITEM.getKey(d.getItem()));
            if (c != null) cats.add(c);
        }
        return cats.isEmpty() ? null : cats;
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
        ListTag offerList = new ListTag();
        for (Bounty b : offers.values()) offerList.add(b.toNbt());
        nbt.put("Offers", offerList);

        ListTag takenList = new ListTag();
        for (Map.Entry<UUID, Map<UUID, TakenBounty>> e : taken.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            CompoundTag o = new CompoundTag();
            net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(o, "Player", e.getKey());
            ListTag entries = new ListTag();
            for (TakenBounty tb : e.getValue().values()) entries.add(tb.toNbt());
            o.put("Taken", entries);
            takenList.add(o);
        }
        nbt.put("TakenByPlayer", takenList);

        ListTag doneList = new ListTag();
        for (Map.Entry<UUID, Set<UUID>> e : completed.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            CompoundTag o = new CompoundTag();
            net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(o, "Player", e.getKey());
            ListTag ids = new ListTag();
            for (UUID id : e.getValue()) {
                CompoundTag io = new CompoundTag();
                net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(io, "Id", id);
                ids.add(io);
            }
            o.put("Ids", ids);
            doneList.add(o);
        }
        nbt.put("Completed", doneList);

        ListTag decreeList = new ListTag();
        for (ItemStack s : decrees) decreeList.add(StackData.writeStack(s));
        nbt.put("Decrees", decreeList);
        return nbt;
    }

    public static BountyState fromNbt(CompoundTag nbt) {
        BountyState state = new BountyState();
        ListTag offerList = nbt.getList("Offers", Tag.TAG_COMPOUND);
        for (int i = 0; i < offerList.size(); i++) {
            try {
                Bounty b = Bounty.fromNbt(offerList.getCompound(i));
                state.offers.put(b.getId(), b);
            } catch (IllegalArgumentException ignored) {
            }
        }

        ListTag takenList = nbt.getList("TakenByPlayer", Tag.TAG_COMPOUND);
        for (int i = 0; i < takenList.size(); i++) {
            CompoundTag o = takenList.getCompound(i);
            try {
                UUID player = net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(o, "Player");
                Map<UUID, TakenBounty> m = new LinkedHashMap<>();
                ListTag entries = o.getList("Taken", Tag.TAG_COMPOUND);
                for (int j = 0; j < entries.size(); j++) {
                    TakenBounty tb = TakenBounty.fromNbt(entries.getCompound(j));
                    m.put(tb.bounty().getId(), tb);
                }
                state.taken.put(player, m);
            } catch (IllegalArgumentException ignored) {
            }
        }

        ListTag doneList = nbt.getList("Completed", Tag.TAG_COMPOUND);
        for (int i = 0; i < doneList.size(); i++) {
            CompoundTag o = doneList.getCompound(i);
            try {
                UUID player = net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(o, "Player");
                Set<UUID> s = new HashSet<>();
                ListTag ids = o.getList("Ids", Tag.TAG_COMPOUND);
                for (int j = 0; j < ids.size(); j++) s.add(Nbt.getUuid(ids.getCompound(j), "Id"));
                state.completed.put(player, s);
            } catch (IllegalArgumentException ignored) {
            }
        }

        ListTag decreeList = nbt.getList("Decrees", Tag.TAG_COMPOUND);
        for (int i = 0; i < decreeList.size(); i++) {
            ItemStack s = StackData.readStack(decreeList.getCompound(i));
            if (!s.isEmpty()) state.decrees.add(s);
        }
        return state;
    }
}

