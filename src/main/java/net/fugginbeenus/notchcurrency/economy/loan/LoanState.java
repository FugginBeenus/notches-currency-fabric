package net.fugginbeenus.notchcurrency.economy.loan;

import net.fugginbeenus.notchcurrency.compat.StateData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import org.jetbrains.annotations.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LoanState extends SavedData implements net.fugginbeenus.notchcurrency.compat.NbtState {

    private static final String DATA_KEY = "notchcurrency_loans";

    public static final class Loan {
        public long debt;
        public long dueTime;         // game time the loan must be repaid by
        public boolean lateFeeApplied;

        Loan(long debt, long dueTime, boolean lateFeeApplied) {
            this.debt = debt;
            this.dueTime = dueTime;
            this.lateFeeApplied = lateFeeApplied;
        }
    }

    private final Map<UUID, Loan> loans = new HashMap<>();

    public static LoanState get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        DimensionDataStorage mgr = overworld.getDataStorage();
        return StateData.getOrCreate(mgr, LoanState::new, LoanState::fromNbt, DATA_KEY);
    }

    @Nullable
    public Loan get(UUID player) {
        return loans.get(player);
    }

    public long getDebt(UUID player) {
        Loan l = loans.get(player);
        return l == null ? 0L : l.debt;
    }

    public void borrow(UUID player, long amount, long dueTime) {
        Loan l = loans.get(player);
        if (l == null) {
            loans.put(player, new Loan(amount, dueTime, false));
        } else {
            l.debt += amount;
        }
        setDirty();
    }

    public void setDebt(UUID player, long debt) {
        Loan l = loans.get(player);
        if (l == null) return;
        if (debt <= 0) loans.remove(player);
        else l.debt = debt;
        setDirty();
    }

    public void markDirtyPublic() {
        setDirty();
    }

    public Map<UUID, Loan> snapshot() {
        return new HashMap<>(loans);
    }

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
        ListTag list = new ListTag();
        for (Map.Entry<UUID, Loan> e : loans.entrySet()) {
            CompoundTag o = new CompoundTag();
            net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(o, "Player", e.getKey());
            o.putLong("Debt", e.getValue().debt);
            o.putLong("Due", e.getValue().dueTime);
            o.putBoolean("Late", e.getValue().lateFeeApplied);
            list.add(o);
        }
        nbt.put("Loans", list);
        return nbt;
    }

    public static LoanState fromNbt(CompoundTag nbt) {
        LoanState state = new LoanState();
        ListTag list = nbt.getList("Loans", Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            CompoundTag o = list.getCompound(i);
            try {
                long debt = o.getLong("Debt");
                if (debt > 0) state.loans.put(net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(o, "Player"),
                        new Loan(debt, o.getLong("Due"), o.getBoolean("Late")));
            } catch (IllegalArgumentException ignored) {
                // skip
            }
        }
        return state;
    }
}
