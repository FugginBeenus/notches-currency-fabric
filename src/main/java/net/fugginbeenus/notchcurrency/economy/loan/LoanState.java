package net.fugginbeenus.notchcurrency.economy.loan;

import net.fugginbeenus.notchcurrency.compat.StateData;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * World-persistent loan ledger (overworld save): each player's outstanding debt, its repayment
 * due time (game time), and whether the one-time late fee has been charged. Debt of 0 = no loan.
 */
public class LoanState extends PersistentState {

    private static final String DATA_KEY = "notchcurrency_loans";

    /** One player's loan. */
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
        ServerWorld overworld = server.getOverworld();
        PersistentStateManager mgr = overworld.getPersistentStateManager();
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

    /** Open a fresh loan (sets the due time) or top up an existing one (keeps its due time). */
    public void borrow(UUID player, long amount, long dueTime) {
        Loan l = loans.get(player);
        if (l == null) {
            loans.put(player, new Loan(amount, dueTime, false));
        } else {
            l.debt += amount;
        }
        markDirty();
    }

    /** Adjust an existing loan's debt (removing it at 0). */
    public void setDebt(UUID player, long debt) {
        Loan l = loans.get(player);
        if (l == null) return;
        if (debt <= 0) loans.remove(player);
        else l.debt = debt;
        markDirty();
    }

    public void markDirtyPublic() {
        markDirty();
    }

    public Map<UUID, Loan> snapshot() {
        return new HashMap<>(loans);
    }

    @Override
    //? if >=1.21 {
    /*public NbtCompound writeNbt(NbtCompound nbt, net.minecraft.registry.RegistryWrapper.WrapperLookup registries) {
    *///?} else {
    public NbtCompound writeNbt(NbtCompound nbt) {
    //?}
        NbtList list = new NbtList();
        for (Map.Entry<UUID, Loan> e : loans.entrySet()) {
            NbtCompound o = new NbtCompound();
            o.putUuid("Player", e.getKey());
            o.putLong("Debt", e.getValue().debt);
            o.putLong("Due", e.getValue().dueTime);
            o.putBoolean("Late", e.getValue().lateFeeApplied);
            list.add(o);
        }
        nbt.put("Loans", list);
        return nbt;
    }

    public static LoanState fromNbt(NbtCompound nbt) {
        LoanState state = new LoanState();
        NbtList list = nbt.getList("Loans", NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            NbtCompound o = list.getCompound(i);
            try {
                long debt = o.getLong("Debt");
                if (debt > 0) state.loans.put(o.getUuid("Player"),
                        new Loan(debt, o.getLong("Due"), o.getBoolean("Late")));
            } catch (IllegalArgumentException ignored) {
                // skip
            }
        }
        return state;
    }
}
