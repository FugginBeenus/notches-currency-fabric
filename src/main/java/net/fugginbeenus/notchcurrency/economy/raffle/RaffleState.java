package net.fugginbeenus.notchcurrency.economy.raffle;

import net.fugginbeenus.notchcurrency.compat.Nbt;
import net.fugginbeenus.notchcurrency.compat.StateData;

import net.fugginbeenus.notchcurrency.compat.StackData;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

public class RaffleState extends SavedData implements net.fugginbeenus.notchcurrency.compat.NbtState {

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
        ServerLevel overworld = server.overworld();
        DimensionDataStorage mgr = overworld.getDataStorage();
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
        setDirty();
    }

    public long getPot() {
        return pot;
    }

    public long getCoinsPool() {
        return coinsPool;
    }

    public void setCoinsPool(long c) {
        coinsPool = Math.max(0L, c);
        setDirty();
    }

    public ItemStack getPrizeItem() {
        return prizeItem;
    }

    public void setPrizeItem(ItemStack stack) {
        prizeItem = stack == null ? ItemStack.EMPTY : stack.copy();
        setDirty();
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
        setDirty();
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
        setDirty();
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
        setDirty();
    }

    public void clearEntries() {
        currentRound++;
        pot = 0L;
        tickets.clear();
        names.clear();
        redeemedThisRound.clear();
        setDirty();
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
        if (!won.isEmpty()) setDirty();
        return won;
    }

    // ---- NBT ----

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
        nbt.putLong("Round", currentRound);
        nbt.putLong("Pot", pot);
        nbt.putLong("CoinsPool", coinsPool);
        if (!prizeItem.isEmpty()) nbt.put("PrizeItem", StackData.writeStack(prizeItem));

        ListTag ticketList = new ListTag();
        for (Map.Entry<UUID, Integer> e : tickets.entrySet()) {
            CompoundTag o = new CompoundTag();
            net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(o, "Player", e.getKey());
            o.putInt("Count", e.getValue());
            o.putString("Name", names.getOrDefault(e.getKey(), "Someone"));
            ticketList.add(o);
        }
        nbt.put("Tickets", ticketList);

        ListTag winList = new ListTag();
        for (Result r : unclaimed.values()) {
            CompoundTag o = new CompoundTag();
            o.putLong("Round", r.round);
            net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(o, "Winner", r.winner);
            o.putString("Name", r.winnerName);
            o.putLong("Prize", r.prize);
            if (!r.prizeItem.isEmpty()) o.put("PrizeItem", StackData.writeStack(r.prizeItem));
            winList.add(o);
        }
        nbt.put("Unclaimed", winList);

        ListTag redeemed = new ListTag();
        for (UUID u : redeemedThisRound) {
            CompoundTag o = new CompoundTag();
            net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(o, "Id", u);
            redeemed.add(o);
        }
        nbt.put("Redeemed", redeemed);
        return nbt;
    }

    public static RaffleState fromNbt(CompoundTag nbt) {
        RaffleState state = new RaffleState();
        state.currentRound = Math.max(1L, nbt.getLong("Round"));
        state.pot = nbt.getLong("Pot");
        state.coinsPool = nbt.getLong("CoinsPool");
        if (nbt.contains("PrizeItem", Tag.TAG_COMPOUND)) {
            state.prizeItem = StackData.readStack(nbt.getCompound("PrizeItem"));
        }

        ListTag ticketList = nbt.getList("Tickets", Tag.TAG_COMPOUND);
        for (int i = 0; i < ticketList.size(); i++) {
            CompoundTag o = ticketList.getCompound(i);
            try {
                UUID id = net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(o, "Player");
                state.tickets.put(id, o.getInt("Count"));
                state.names.put(id, o.getString("Name"));
            } catch (IllegalArgumentException ignored) {
                // skip malformed entry
            }
        }

        ListTag winList = nbt.getList("Unclaimed", Tag.TAG_COMPOUND);
        for (int i = 0; i < winList.size(); i++) {
            CompoundTag o = winList.getCompound(i);
            try {
                long round = o.getLong("Round");
                ItemStack prize = o.contains("PrizeItem", Tag.TAG_COMPOUND)
                        ? StackData.readStack(o.getCompound("PrizeItem")) : ItemStack.EMPTY;
                state.unclaimed.put(round, new Result(round, net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(o, "Winner"),
                        o.getString("Name"), o.getLong("Prize"), prize));
            } catch (IllegalArgumentException ignored) {
                // skip malformed entry
            }
        }

        ListTag redeemed = nbt.getList("Redeemed", Tag.TAG_COMPOUND);
        for (int i = 0; i < redeemed.size(); i++) {
            try {
                state.redeemedThisRound.add(Nbt.getUuid(redeemed.getCompound(i), "Id"));
            } catch (IllegalArgumentException ignored) {
                // skip malformed entry
            }
        }
        return state;
    }
}
