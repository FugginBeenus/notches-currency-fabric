package net.fugginbeenus.notchcurrency.economy.gambling;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

/**
 * Backing handler for the slot machine: no slots, just a live {@link PropertyDelegate} of the
 * viewer's balance, bet limits, the last spin's reels/win, and the normalised paytable. The bet
 * amount is sent by packet (the client types it) and drives {@link SlotMachineManager#spin}; the
 * result is read back through the properties, which refresh every tick.
 */
public class SlotMachineScreenHandler extends ScreenHandler {

    public static final int P_ENABLED = 0;
    public static final int P_MIN     = 1;
    public static final int P_MAX     = 2;
    public static final int P_BAL     = 3;
    public static final int P_REEL0   = 4;
    public static final int P_REEL1   = 5;
    public static final int P_REEL2   = 6;
    public static final int P_LASTWIN = 7;
    public static final int P_SPINID  = 8;
    // Normalised 3-of-a-kind payout (×10) per symbol, for the paytable.
    public static final int P_MULT_BASE = 9;
    private static final int PROP_COUNT = P_MULT_BASE + 5;

    private final PlayerInventory playerInv;
    private final World world;
    private final PropertyDelegate props = new ArrayPropertyDelegate(PROP_COUNT);

    private int reel0, reel1, reel2;
    private long lastWin = -1; // -1 = no spin yet
    private int spinId;

    public SlotMachineScreenHandler(int syncId, PlayerInventory inv) {
        super(ModScreenHandlers.SLOT_MACHINE, syncId);
        this.playerInv = inv;
        this.world = inv.player.getWorld();
        this.addProperties(props);
        refresh();
    }

    public int prop(int i) { return props.get(i); }

    /** Take a bet and spin. Called from the SLOTS_SPIN packet receiver. */
    public void spin(long bet) {
        if (!(playerInv.player instanceof ServerPlayerEntity sp)) return;
        SlotMachineManager.SpinResult r = SlotMachineManager.spin(sp, bet);
        if (!r.ok()) return;
        reel0 = r.r0();
        reel1 = r.r1();
        reel2 = r.r2();
        lastWin = r.payout();
        spinId++;
        refresh();
        sendContentUpdates();
    }

    private void refresh() {
        if (!(world instanceof ServerWorld)) return;
        if (!(playerInv.player instanceof ServerPlayerEntity sp)) return;
        props.set(P_ENABLED, GamblingManager.isEnabled() ? 1 : 0);
        props.set(P_MIN, clamp(GamblingManager.getMinBet()));
        props.set(P_MAX, clamp(GamblingManager.getMaxBet()));
        props.set(P_BAL, clamp(CurrencyApi.getBalance(sp)));
        props.set(P_REEL0, reel0);
        props.set(P_REEL1, reel1);
        props.set(P_REEL2, reel2);
        props.set(P_LASTWIN, (int) Math.min(Integer.MAX_VALUE, lastWin));
        props.set(P_SPINID, spinId);
        SlotSymbol[] syms = SlotSymbol.values();
        for (int i = 0; i < syms.length; i++) {
            props.set(P_MULT_BASE + i, SlotMachineManager.displayMult3x10(syms[i]));
        }
    }

    private static int clamp(long v) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(0, v));
    }

    @Override
    public void sendContentUpdates() {
        refresh();
        super.sendContentUpdates();
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
