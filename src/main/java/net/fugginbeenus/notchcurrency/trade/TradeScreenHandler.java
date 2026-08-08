package net.fugginbeenus.notchcurrency.trade;

import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.world.Container;
//? if <26.1 {
import net.minecraft.world.ContainerListener;
//?}
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class TradeScreenHandler extends AbstractContainerMenu {

    private final Player player;
    TradeManager.TradeSession session = null;
    private final boolean leftSideSelf;

    private final SimpleContainer selfInv  = new SimpleContainer(9);
    private final SimpleContainer otherInv = new SimpleContainer(9);

    // [0]=selfMoney, [1]=otherMoney, [2]=selfReady(0/1), [3]=otherReady(0/1), [4]=stage (unused)
    private final ContainerData props = new SimpleContainerData(5);

    private static final int SELF_GRID_X  = 34;
    private static final int SELF_GRID_Y  = 25;
    private static final int OTHER_GRID_X = 142;
    private static final int OTHER_GRID_Y = 25;

    private static final int PLAYER_INV_X = 34;
    private static final int PLAYER_INV_Y = 135;
    private static final int HOTBAR_Y     = PLAYER_INV_Y + 58;

    private static final int SELF_SIZE   = 9;
    private static final int OTHER_SIZE  = 9;
    private static final int INV_SIZE    = 27;
    private static final int HOTBAR_SIZE = 9;

    private int selfStart, selfEnd;
    private int otherStart, otherEnd;
    private int invStart, invEnd;
    private int hotbarStart, hotbarEnd;

    //? if <26.1 {
    private final ContainerListener selfListener = inv -> {
        if (session != null && inv == selfInv) {
            unreadySelfAndSync();   // <-- NEW
            mirrorSelfToPartner();
        }
    };
    //?}

    // ---------- CLIENT constructor ----------
    public TradeScreenHandler(int containerId, Inventory inv) {
        super(ModScreenHandlers.TRADE, containerId);
        this.player = inv.player;
        this.leftSideSelf = true;
        hookInventories();
        buildSlots();
        this.addDataSlots(props);
    }

    // ---------- SERVER constructor ----------
    public TradeScreenHandler(int containerId, Inventory inv, Player player,
                              TradeManager.TradeSession session, boolean selfOnLeft) {
        super(ModScreenHandlers.TRADE, containerId);
        this.player = player;
        this.session = session;
        this.leftSideSelf = selfOnLeft;
        hookInventories();
        buildSlots();
        this.addDataSlots(props);
        sendAllDataToRemote();
    }

    private void hookInventories() {
        // 26.1 dropped container listeners. slotsChanged below already fires for this same container
        // and runs the same two steps, so there it is simply the only path in.
        //? if <26.1 {
        selfInv.addListener(selfListener);
        //?}
    }

    private void buildSlots() {
        // --- Self 3x3 offer grid (locked when ready) ---
        int leftX = SELF_GRID_X, leftY = SELF_GRID_Y;
        selfStart = this.slots.size();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                int index = r * 3 + c;
                this.addSlot(new Slot(selfInv, index, leftX + c * 18, leftY + r * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        // Disable when player has clicked "ready"
                        return TradeScreenHandler.this.getProperties().get(2) == 0;
                    }
                    @Override
                    public boolean mayPickup(Player player) {
                        return TradeScreenHandler.this.getProperties().get(2) == 0;
                    }
                });
            }
        }
        selfEnd = selfStart + SELF_SIZE;

        // --- Other (read-only) 3x3 grid ---
        int rightX = OTHER_GRID_X, rightY = OTHER_GRID_Y;
        otherStart = this.slots.size();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                this.addSlot(new Slot(otherInv, r * 3 + c, rightX + c * 18, rightY + r * 18) {
                    @Override public boolean mayPlace(ItemStack stack) { return false; }
                    @Override public boolean mayPickup(Player playerEntity) { return false; }
                });
            }
        }
        otherEnd = otherStart + OTHER_SIZE;

        // --- Player inventory ---
        invStart = this.slots.size();
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(this.player.getInventory(), col + row * 9 + 9,
                        PLAYER_INV_X + col * 18,
                        PLAYER_INV_Y + row * 18));
            }
        }
        invEnd = invStart + INV_SIZE;

        // --- Hotbar ---
        hotbarStart = this.slots.size();
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(this.player.getInventory(), col,
                    PLAYER_INV_X + col * 18,
                    HOTBAR_Y));
        }
        hotbarEnd = hotbarStart + HOTBAR_SIZE;
    }

    // ======================== MIRRORING LOGIC ========================

    @Override
    public void slotsChanged(Container inv) {
        super.slotsChanged(inv);
        if (session == null) return;
        if (inv == selfInv) {
            unreadySelfAndSync();   // <-- NEW
            mirrorSelfToPartner();
        }
    }

    private void mirrorSelfToPartner() {
        TradeScreenHandler partner =
                (player == session.a) ? session.bHandler : session.aHandler;
        if (partner == null) return;

        for (int i = 0; i < 9; i++) {
            partner.receiveMirrorFromPartner(i, selfInv.getItem(i));
        }
        partner.broadcastChanges();
    }

    void receiveMirrorFromPartner(int index, ItemStack stack) {
        if (index >= 0 && index < otherInv.getContainerSize()) {
            otherInv.setItem(index, stack == null ? ItemStack.EMPTY : stack.copy());
            otherInv.setChanged();
        }
    }

    private void unreadySelfAndSync() {
        if (session == null) return; // client stub
        boolean changed = false;

        if (player == session.a) {
            if (session.aReady) { session.aReady = false; changed = true; }
        } else {
            if (session.bReady) { session.bReady = false; changed = true; }
        }

        if (changed) {
            // Refresh both handlers' property delegates so buttons/labels update
            if (session.aHandler != null) session.aHandler.sendAllDataToRemote();
            if (session.bHandler != null) session.bHandler.sendAllDataToRemote();
        }
    }

    // ======================== MONEY / STATE SYNC ========================

    public void sendAllDataToRemote() {
        if (session == null) return;
        if (player == session.a) {
            props.set(0, session.aMoney);
            props.set(1, session.bMoney);
            props.set(2, session.aReady ? 1 : 0);
            props.set(3, session.bReady ? 1 : 0);
        } else {
            props.set(0, session.bMoney);
            props.set(1, session.aMoney);
            props.set(2, session.bReady ? 1 : 0);
            props.set(3, session.aReady ? 1 : 0);
        }
        props.set(4, 0);
        broadcastChanges();
    }

    public ContainerData getProperties() { return props; }

    // ======================== COMPLETE / CANCEL ========================

    void returnItems() { dump(selfInv, player); }

    List<ItemStack> takeItemsForCompletion() {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < selfInv.getContainerSize(); i++) {
            ItemStack s = selfInv.getItem(i);
            if (!s.isEmpty()) {
                out.add(s.copy());
                selfInv.setItem(i, ItemStack.EMPTY);
            }
        }
        selfInv.setChanged();
        return out;
    }

    private static void dump(SimpleContainer inv, Player to) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty()) {
                to.getInventory().placeItemBackInInventory(s);
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
        inv.setChanged();
    }

    // ======================== VANILLA PLUMBING ========================

    @Override public boolean stillValid(Player player) { return true; }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = (index >= 0 && index < this.slots.size()) ? this.slots.get(index) : null;
        if (slot == null || !slot.hasItem()) return empty;

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();

        if (index >= selfStart && index < selfEnd) {
            if (!this.moveItemStackTo(stack, invStart, hotbarEnd, true)) return ItemStack.EMPTY;
        } else if (index >= otherStart && index < otherEnd) {
            return ItemStack.EMPTY;
        } else {
            if (!this.moveItemStackTo(stack, selfStart, selfEnd, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();

        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);

        if (session != null && index >= selfStart && index < selfEnd) {
            mirrorSelfToPartner();
        }

        return original;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (session != null && !session.isClosed()) {
            session.cancel("Closed");
        }
    }
}
