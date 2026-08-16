package net.fugginbeenus.notchcurrency.economy.gambling;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class SlotMachineScreenHandler extends AbstractContainerMenu {

    public static final int P_ENABLED = 0;
    public static final int P_MIN     = 1;
    public static final int P_MAX     = 2;
    public static final int P_BAL     = 3;
    public static final int P_REEL0   = 4;
    public static final int P_REEL1   = 5;
    public static final int P_REEL2   = 6;
    public static final int P_LASTWIN = 7;
    public static final int P_SPINID  = 8;
    public static final int P_MULT_BASE = 9;
    private static final int PROP_COUNT = P_MULT_BASE + 5;

    private final Inventory playerInv;
    private final Level world;
    private final ContainerData props = new SimpleContainerData(PROP_COUNT);

    private int reel0, reel1, reel2;
    private long lastWin = -1;
    private int spinId;

    public SlotMachineScreenHandler(int containerId, Inventory inv) {
        super(ModScreenHandlers.SLOT_MACHINE, containerId);
        this.playerInv = inv;
        this.world = inv.player.level();
        this.addDataSlots(props);
        refresh();
    }

    public int prop(int i) { return props.get(i); }

    public void spin(long bet) {
        if (!(playerInv.player instanceof ServerPlayer sp)) return;
        SlotMachineManager.SpinResult r = SlotMachineManager.spin(sp, bet);
        if (!r.ok()) return;
        reel0 = r.r0();
        reel1 = r.r1();
        reel2 = r.r2();
        lastWin = r.payout();
        spinId++;
        refresh();
        broadcastChanges();
    }

    private void refresh() {
        if (!(world instanceof ServerLevel)) return;
        if (!(playerInv.player instanceof ServerPlayer sp)) return;
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
    public void broadcastChanges() {
        refresh();
        super.broadcastChanges();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
