package net.fugginbeenus.notchcurrency.economy.raffle;

import net.fugginbeenus.notchcurrency.compat.StateData;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class RaffleState extends PersistentState {

    private static final String DATA_KEY = "notchcurrency_raffle";

    public static final class Result {
        public final long round;
        public final UUID winner;
        public final String winnerName;
        public final long prize;
        public final ItemStack prizeItem;

        public Result(long round, UUID winner, String winnerName, long prize, ItemStack prizeItem) {
            this.round = round;
            this.winner = winner;
            this.winnerName = winnerName;
            this.prize = prize;
            this.prizeItem = prizeItem == null ? ItemStack.EMPTY : prizeItem;
        }
    }

    private long currentRound = 1L;
    private long pot = 0L;
    private long coinsPool = 0L; // admin-set guaranteed coin prize for this round (a faucet)
    private ItemStack prizeItem = ItemStack.EMPTY;
    private final Map<UUID, Integer> tickets = new LinkedHashMap<>();
    private final Map<UUID, String> names = new LinkedHashMap<>();
    private final Map<Long, Result> unclaimed = new LinkedHashMap<>();
    private final Set<UUID> redeemedThisRound = new HashSet<>(); // one old-ticket redemption per player per round

    public static RaffleState get(MinecraftServer server) {
        ServerWorld overworld = server.getOverworld();
        PersistentStateManager mgr = overworld.getPersistentStateManager();
        return StateData.getOrCreate(mgr, RaffleState::new, RaffleState::fromNbt, DATA_KEY);
    }

    // ---- active round ----

    public long getCurrentRound() {
        return currentRound;
    }

    public void recordPurchase(UUID buyer, String name, int count, long potShare) {
        tickets.merge(buyer, count, Integer::sum);
        names.put(buyer, name);
        pot += potShare;
        markDirty();
    }

    public long getPot() {
        return pot;
    }

    public long getCoinsPool() {
        return coinsPool;
    }

    public void setCoinsPool(long c) {
        coinsPool = Math.max(0L, c);
        markDirty();
    }

    public ItemStack getPrizeItem() {
        return prizeItem;
    }

    public void setPrizeItem(ItemStack stack) {
        prizeItem = stack == null ? ItemStack.EMPTY : stack.copy();
        markDirty();
    }

    public int getTickets(UUID player) {
        return tickets.getOrDefault(player, 0);
    }

    public int getTotalTickets() {
        int total = 0;
        for (int c : tickets.values()) total += c;
        return total;
    }

    public String getName(UUID player) {
        return names.getOrDefault(player, "Someone");
    }

    public boolean hasRedeemed(UUID player) {
        return redeemedThisRound.contains(player);
    }

    public void markRedeemed(UUID player) {
        redeemedThisRound.add(player);
        markDirty();
    }

    @Nullable
    public UUID drawWinner(Random random) {
        int total = getTotalTickets();
        if (total <= 0) return null;
        int roll = random.nextInt(total);
        int acc = 0;
        UUID last = null;
        for (Map.Entry<UUID, Integer> e : tickets.entrySet()) {
            last = e.getKey();
            acc += e.getValue();
            if (roll < acc) return e.getKey();
        }
        return last;
    }

    public long recordResult(UUID winner, String winnerName, long prize) {
        long drawn = currentRound;
        unclaimed.put(drawn, new Result(drawn, winner, winnerName, prize, prizeItem));
        currentRound++;
        pot = 0L;
        coinsPool = 0L;             // one-off prize pool is consumed by the draw
        prizeItem = ItemStack.EMPTY; // awarded into the Result; next round starts itemless
        tickets.clear();
        names.clear();
        redeemedThisRound.clear();
        markDirty();
        return drawn;
    }

    public void resetRound() {
        currentRound++;
        pot = 0L;
        coinsPool = 0L;
        prizeItem = ItemStack.EMPTY;
        tickets.clear();
        names.clear();
        redeemedThisRound.clear();
        markDirty();
    }

    public void clearEntries() {
        currentRound++;
        pot = 0L;
        tickets.clear();
        names.clear();
        redeemedThisRound.clear();
        markDirty();
    }

    // ---- unclaimed wins ----

    @Nullable
    public Result getResult(long round) {
        return unclaimed.get(round);
    }

    public List<Result> getUnclaimedWins(UUID player) {
        List<Result> out = new ArrayList<>();
        for (Result r : unclaimed.values()) {
            if (r.winner.equals(player)) out.add(r);
        }
        return out;
    }

    public List<Result> claimWins(UUID player) {
        List<Result> won = getUnclaimedWins(player);
        for (Result r : won) unclaimed.remove(r.round);
        if (!won.isEmpty()) markDirty();
        return won;
    }

    // ---- NBT ----

    @Override
    //? if >=1.21 {
    /*public NbtCompound writeNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
    *///?} else {
    public NbtCompound writeNbt(NbtCompound nbt) {
    //?}
        nbt.putLong("Round", currentRound);
        nbt.putLong("Pot", pot);
        nbt.putLong("CoinsPool", coinsPool);
        if (!prizeItem.isEmpty()) nbt.put("PrizeItem", StackData.writeStack(prizeItem));

        NbtList ticketList = new NbtList();
        for (Map.Entry<UUID, Integer> e : tickets.entrySet()) {
            NbtCompound o = new NbtCompound();
            o.putUuid("Player", e.getKey());
            o.putInt("Count", e.getValue());
            o.putString("Name", names.getOrDefault(e.getKey(), "Someone"));
            ticketList.add(o);
        }
        nbt.put("Tickets", ticketList);

        NbtList winList = new NbtList();
        for (Result r : unclaimed.values()) {
            NbtCompound o = new NbtCompound();
            o.putLong("Round", r.round);
            o.putUuid("Winner", r.winner);
            o.putString("Name", r.winnerName);
            o.putLong("Prize", r.prize);
            if (!r.prizeItem.isEmpty()) o.put("PrizeItem", StackData.writeStack(r.prizeItem));
            winList.add(o);
        }
        nbt.put("Unclaimed", winList);

        NbtList redeemed = new NbtList();
        for (UUID u : redeemedThisRound) {
            NbtCompound o = new NbtCompound();
            o.putUuid("Id", u);
            redeemed.add(o);
        }
        nbt.put("Redeemed", redeemed);
        return nbt;
    }

    public static RaffleState fromNbt(NbtCompound nbt) {
        RaffleState state = new RaffleState();
        state.currentRound = Math.max(1L, nbt.getLong("Round"));
        state.pot = nbt.getLong("Pot");
        state.coinsPool = nbt.getLong("CoinsPool");
        if (nbt.contains("PrizeItem", NbtElement.COMPOUND_TYPE)) {
            state.prizeItem = StackData.readStack(nbt.getCompound("PrizeItem"));
        }

        NbtList ticketList = nbt.getList("Tickets", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < ticketList.size(); i++) {
            NbtCompound o = ticketList.getCompound(i);
            try {
                UUID id = o.getUuid("Player");
                state.tickets.put(id, o.getInt("Count"));
                state.names.put(id, o.getString("Name"));
            } catch (IllegalArgumentException ignored) {
                // skip malformed entry
            }
        }

        NbtList winList = nbt.getList("Unclaimed", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < winList.size(); i++) {
            NbtCompound o = winList.getCompound(i);
            try {
                long round = o.getLong("Round");
                ItemStack prize = o.contains("PrizeItem", NbtElement.COMPOUND_TYPE)
                        ? StackData.readStack(o.getCompound("PrizeItem")) : ItemStack.EMPTY;
                state.unclaimed.put(round, new Result(round, o.getUuid("Winner"),
                        o.getString("Name"), o.getLong("Prize"), prize));
            } catch (IllegalArgumentException ignored) {
                // skip malformed entry
            }
        }

        NbtList redeemed = nbt.getList("Redeemed", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < redeemed.size(); i++) {
            try {
                state.redeemedThisRound.add(redeemed.getCompound(i).getUuid("Id"));
            } catch (IllegalArgumentException ignored) {
                // skip malformed entry
            }
        }
        return state;
    }
}
