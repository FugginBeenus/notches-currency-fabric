package net.fugginbeenus.notchcurrency.economy;

import net.fugginbeenus.notchcurrency.compat.StateData;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReceiptState extends PersistentState {

    private static final String DATA_KEY = "notchcurrency_receipts";
    public static final int MAX_PER_PLAYER = 50;

    public record Receipt(long time, long delta, long balanceAfter, String reason, String detail) {}

    private final Map<UUID, Deque<Receipt>> history = new HashMap<>();

    public static ReceiptState get(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        PersistentStateManager mgr = overworld.getPersistentStateManager();
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
        state.markDirty();
    }

    public List<Receipt> recent(UUID id) {
        Deque<Receipt> q = history.get(id);
        return q == null ? List.of() : new ArrayList<>(q);
    }

    // ---- NBT ----

    private static ReceiptState fromNbt(NbtCompound nbt) {
        ReceiptState state = new ReceiptState();
        NbtList players = nbt.getList("Players", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < players.size(); i++) {
            NbtCompound entry = players.getCompound(i);
            UUID id = entry.getUuid("Player");
            Deque<Receipt> q = new ArrayDeque<>();
            NbtList recs = entry.getList("Receipts", NbtElement.COMPOUND_TYPE);
            for (int j = 0; j < recs.size(); j++) {
                NbtCompound r = recs.getCompound(j);
                q.addLast(new Receipt(r.getLong("t"), r.getLong("d"), r.getLong("b"),
                        r.getString("r"), r.getString("x")));
            }
            state.history.put(id, q);
        }
        return state;
    }

    @Override
    //? if >=1.21 {
    /*public NbtCompound writeNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
    *///?} else {
    public NbtCompound writeNbt(NbtCompound nbt) {
    //?}
        NbtList players = new NbtList();
        for (Map.Entry<UUID, Deque<Receipt>> e : history.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            NbtCompound entry = new NbtCompound();
            entry.putUuid("Player", e.getKey());
            NbtList recs = new NbtList();
            for (Receipt rec : e.getValue()) {
                NbtCompound r = new NbtCompound();
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
