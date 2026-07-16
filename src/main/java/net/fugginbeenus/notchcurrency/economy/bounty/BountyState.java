package net.fugginbeenus.notchcurrency.economy.bounty;

import net.fugginbeenus.notchcurrency.compat.StateData;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
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

/**
 * World-persistent bounty board (overworld save): the board <b>offers</b> (auto-generated +
 * admin-posted) and each player's <b>taken</b> bounties (personal copies with their own deadline
 * and progress). Take-first: kills only count toward taken bounties, collected back at the board.
 */
public class BountyState extends PersistentState {

    private static final String DATA_KEY = "notchcurrency_bounties";

    private final Map<UUID, Bounty> offers = new LinkedHashMap<>();
    private final Map<UUID, Map<UUID, TakenBounty>> taken = new HashMap<>(); // player -> (offerId -> taken)
    private final Map<UUID, Set<UUID>> completed = new HashMap<>();          // player -> finished offer ids (hidden until rotated)
    private final List<ItemStack> decrees = new ArrayList<>(); // placed decree items (gate categories)

    public static BountyState get(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        PersistentStateManager mgr = overworld.getPersistentStateManager();
        return StateData.getOrCreate(mgr, BountyState::new, BountyState::fromNbt, DATA_KEY);
    }

    // ---- offers ----

    public void addOffer(Bounty b) {
        offers.put(b.getId(), b);
        markDirty();
    }

    public boolean removeOffer(UUID id) {
        boolean removed = offers.remove(id) != null;
        if (removed) {
            for (Set<UUID> s : completed.values()) s.remove(id); // clear completion once the offer rotates out
            markDirty();
        }
        return removed;
    }

    public boolean hasCompletedOffer(UUID player, UUID offerId) {
        Set<UUID> s = completed.get(player);
        return s != null && s.contains(offerId);
    }

    /** Mark an offer finished for a player (so it stays hidden from their board until it rotates). */
    public void markOfferCompleted(UUID player, UUID offerId) {
        if (offers.containsKey(offerId)) { // only worth tracking while the offer still exists
            completed.computeIfAbsent(player, k -> new HashSet<>()).add(offerId);
            markDirty();
        }
    }

    @Nullable
    public Bounty getOffer(UUID id) {
        return offers.get(id);
    }

    public Collection<Bounty> allOffers() {
        return offers.values();
    }

    // ---- taken ----

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
        markDirty();
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
        if (m != null && m.remove(offerId) != null) markDirty();
    }

    /** Drop any taken bounties whose deadline has passed. Returns how many were dropped. */
    public int cleanupExpired(UUID player, long now) {
        Map<UUID, TakenBounty> m = taken.get(player);
        if (m == null) return 0;
        int before = m.size();
        m.values().removeIf(tb -> tb.isExpired(now));
        int dropped = before - m.size();
        if (dropped > 0) markDirty();
        return dropped;
    }

    // ---- decrees ----

    public List<ItemStack> getDecrees() {
        return new ArrayList<>(decrees);
    }

    public void setDecrees(List<ItemStack> items) {
        decrees.clear();
        for (ItemStack s : items) if (!s.isEmpty()) decrees.add(s.copy());
        markDirty();
    }

    /** Categories the board may generate: the placed decrees' categories, or null (= all) if none. */
    @Nullable
    public Set<String> activeCategories() {
        if (decrees.isEmpty()) return null;
        Set<String> cats = new HashSet<>();
        for (ItemStack d : decrees) {
            String c = BountyPools.decreeCategory(Registries.ITEM.getId(d.getItem()));
            if (c != null) cats.add(c);
        }
        return cats.isEmpty() ? null : cats;
    }

    // ---- NBT ----

    @Override
    //? if >=1.21 {
    /*public NbtCompound writeNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
    *///?} else {
    public NbtCompound writeNbt(NbtCompound nbt) {
    //?}
        NbtList offerList = new NbtList();
        for (Bounty b : offers.values()) offerList.add(b.toNbt());
        nbt.put("Offers", offerList);

        NbtList takenList = new NbtList();
        for (Map.Entry<UUID, Map<UUID, TakenBounty>> e : taken.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            NbtCompound o = new NbtCompound();
            o.putUuid("Player", e.getKey());
            NbtList entries = new NbtList();
            for (TakenBounty tb : e.getValue().values()) entries.add(tb.toNbt());
            o.put("Taken", entries);
            takenList.add(o);
        }
        nbt.put("TakenByPlayer", takenList);

        NbtList doneList = new NbtList();
        for (Map.Entry<UUID, Set<UUID>> e : completed.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            NbtCompound o = new NbtCompound();
            o.putUuid("Player", e.getKey());
            NbtList ids = new NbtList();
            for (UUID id : e.getValue()) {
                NbtCompound io = new NbtCompound();
                io.putUuid("Id", id);
                ids.add(io);
            }
            o.put("Ids", ids);
            doneList.add(o);
        }
        nbt.put("Completed", doneList);

        NbtList decreeList = new NbtList();
        for (ItemStack s : decrees) decreeList.add(s.writeNbt(new NbtCompound()));
        nbt.put("Decrees", decreeList);
        return nbt;
    }

    public static BountyState fromNbt(NbtCompound nbt) {
        BountyState state = new BountyState();
        NbtList offerList = nbt.getList("Offers", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < offerList.size(); i++) {
            try {
                Bounty b = Bounty.fromNbt(offerList.getCompound(i));
                state.offers.put(b.getId(), b);
            } catch (IllegalArgumentException ignored) {
                // skip malformed / unknown target
            }
        }

        NbtList takenList = nbt.getList("TakenByPlayer", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < takenList.size(); i++) {
            NbtCompound o = takenList.getCompound(i);
            try {
                UUID player = o.getUuid("Player");
                Map<UUID, TakenBounty> m = new LinkedHashMap<>();
                NbtList entries = o.getList("Taken", NbtElement.COMPOUND_TYPE);
                for (int j = 0; j < entries.size(); j++) {
                    TakenBounty tb = TakenBounty.fromNbt(entries.getCompound(j));
                    m.put(tb.bounty().getId(), tb);
                }
                state.taken.put(player, m);
            } catch (IllegalArgumentException ignored) {
                // skip
            }
        }

        NbtList doneList = nbt.getList("Completed", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < doneList.size(); i++) {
            NbtCompound o = doneList.getCompound(i);
            try {
                UUID player = o.getUuid("Player");
                Set<UUID> s = new HashSet<>();
                NbtList ids = o.getList("Ids", NbtElement.COMPOUND_TYPE);
                for (int j = 0; j < ids.size(); j++) s.add(ids.getCompound(j).getUuid("Id"));
                state.completed.put(player, s);
            } catch (IllegalArgumentException ignored) {
                // skip
            }
        }

        NbtList decreeList = nbt.getList("Decrees", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < decreeList.size(); i++) {
            ItemStack s = StackData.readStack(decreeList.getCompound(i));
            if (!s.isEmpty()) state.decrees.add(s);
        }
        return state;
    }
}

