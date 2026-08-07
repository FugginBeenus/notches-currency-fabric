package net.fugginbeenus.notchcurrency.economy;

import net.fugginbeenus.notchcurrency.compat.StateData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReceiptState extends SavedData {

    private static final String DATA_KEY = "notchcurrency_receipts";
    public static final int MAX_PER_PLAYER = 50;

    public record Receipt(long time, long delta, long balanceAfter, String reason, String detail) {}

    private final Map<UUID, Deque<Receipt>> history = new HashMap<>();

    public static ReceiptState get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage mgr = overworld.getDataStorage();
        return StateData.getOrCreate(mgr, ReceiptState::new, ReceiptState::fromNbt, DATA_KEY);
    }

    public static void record(MinecraftServer server, UUID id, long delta, long balanceAfter,
                              TransactionReason reason, String detail) {
        if (delta == 0 || server == null || id == null) return;
        ReceiptState state = get(server);
        Deque<Receipt> q = state.history.computeIfAbsent(id, k -> new ArrayDeque<>());
        q.addFirst(new Receipt(System.currentTimeMillis(), delta, balanceAfter,
                (reason == null ? TransactionReason.UNSPECIFIED : reason).name(),
                detail == null ? "" : detail));
        while (q.size() > MAX_PER_PLAYER) q.removeLast();
        state.setDirty();
    }

    public List<Receipt> recent(UUID id) {
        Deque<Receipt> q = history.get(id);
        return q == null ? List.of() : new ArrayList<>(q);
    }

    // ---- NBT ----

    private static ReceiptState fromNbt(CompoundTag nbt) {
        ReceiptState state = new ReceiptState();
        ListTag players = nbt.getList("Players", Tag.TAG_COMPOUND);
        for (int i = 0; i < players.size(); i++) {
            CompoundTag entry = players.getCompound(i);
            UUID id = net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(entry, "Player");
            Deque<Receipt> q = new ArrayDeque<>();
            ListTag recs = entry.getList("Receipts", Tag.TAG_COMPOUND);
            for (int j = 0; j < recs.size(); j++) {
                CompoundTag r = recs.getCompound(j);
                q.addLast(new Receipt(r.getLong("t"), r.getLong("d"), r.getLong("b"),
                        r.getString("r"), r.getString("x")));
            }
            state.history.put(id, q);
        }
        return state;
    }

    @Override
    //? if >=1.21 {
    /*public CompoundTag save(CompoundTag nbt, net.minecraft.core.HolderLookup.Provider registries) {
    *///?} else {
    public CompoundTag save(CompoundTag nbt) {
    //?}
        ListTag players = new ListTag();
        for (Map.Entry<UUID, Deque<Receipt>> e : history.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(entry, "Player", e.getKey());
            ListTag recs = new ListTag();
            for (Receipt rec : e.getValue()) {
                CompoundTag r = new CompoundTag();
                r.putLong("t", rec.time());
                r.putLong("d", rec.delta());
                r.putLong("b", rec.balanceAfter());
                r.putString("r", rec.reason());
                r.putString("x", rec.detail());
                recs.add(r);
            }
            entry.put("Receipts", recs);
            players.add(entry);
        }
        nbt.put("Players", players);
        return nbt;
    }
}
