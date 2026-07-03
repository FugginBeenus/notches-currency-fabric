package net.fugginbeenus.notchcurrency.economy.cosmetic;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * World-persistent record of which one-time cosmetics each player already owns, so they can't
 * re-buy them. Repeatable cosmetics (one_time = false) are never recorded here.
 */
public class CosmeticState extends PersistentState {

    private static final String DATA_KEY = "notchcurrency_cosmetics";

    private final Map<UUID, Set<String>> owned = new HashMap<>();

    public static CosmeticState get(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        PersistentStateManager mgr = overworld.getPersistentStateManager();
        return mgr.getOrCreate(CosmeticState::fromNbt, CosmeticState::new, DATA_KEY);
    }

    public boolean owns(UUID player, String offerId) {
        Set<String> set = owned.get(player);
        return set != null && set.contains(offerId);
    }

    public void markOwned(UUID player, String offerId) {
        owned.computeIfAbsent(player, k -> new HashSet<>()).add(offerId);
        markDirty();
    }

    // ---- NBT ----

    private static CosmeticState fromNbt(NbtCompound nbt) {
        CosmeticState state = new CosmeticState();
        NbtList players = nbt.getList("Players", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < players.size(); i++) {
            NbtCompound entry = players.getCompound(i);
            UUID id = entry.getUuid("Player");
            Set<String> set = new HashSet<>();
            NbtList ids = entry.getList("Owned", NbtElement.STRING_TYPE);
            for (int j = 0; j < ids.size(); j++) {
                set.add(ids.getString(j));
            }
            state.owned.put(id, set);
        }
        return state;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList players = new NbtList();
        for (Map.Entry<UUID, Set<String>> e : owned.entrySet()) {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("Player", e.getKey());
            NbtList ids = new NbtList();
            for (String s : e.getValue()) {
                ids.add(net.minecraft.nbt.NbtString.of(s));
            }
            entry.put("Owned", ids);
            players.add(entry);
        }
        nbt.put("Players", players);
        return nbt;
    }
}
