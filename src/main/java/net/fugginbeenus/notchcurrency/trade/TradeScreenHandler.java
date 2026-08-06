package net.fugginbeenus.notchcurrency.trade;

import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.InventoryChangedListener;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;

import java.util.ArrayList;
import java.util.List;

public class TradeScreenHandler extends ScreenHandler {

    private final PlayerEntity player;
    TradeManager.TradeSession session = null;
    private final boolean leftSideSelf;

    private final SimpleInventory selfInv  = new SimpleInventory(9);
    private final SimpleInventory otherInv = new SimpleInventory(9);

    // [0]=selfMoney, [1]=otherMoney, [2]=selfReady(0/1), [3]=otherReady(0/1), [4]=stage (unused)
    private final PropertyDelegate props = new ArrayPropertyDelegate(5);

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

    private final InventoryChangedListener selfListener = inv -> {
        if (session != null && inv == selfInv) {
            unreadySelfAndSync();   // <-- NEW
            mirrorSelfToPartner();
        }
    };

    // ---------- CLIENT constructor ----------
    public TradeScreenHandler(int syncId, PlayerInventory inv) {
        super(ModScreenHandlers.TRADE, syncId);
        this.player = inv.player;
        this.leftSideSelf = true;
        hookInventories();
        buildSlots();
        this.addProperties(props);
    }

    // ---------- SERVER constructor ----------
    public TradeScreenHandler(int syncId, PlayerInventory inv, PlayerEntity player,
                              TradeManager.TradeSession session, boolean selfOnLeft) {
        super(ModScreenHandlers.TRADE, syncId);
        this.player = player;
        this.session = session;
        this.leftSideSelf = selfOnLeft;
        hookInventories();
        buildSlots();
        this.addProperties(props);
        syncState();
    }

    private void hookInventories() {
        selfInv.addListener(selfListener);
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
                    public boolean canInsert(ItemStack stack) {
                        // Disable when player has clicked "ready"
                        return TradeScreenHandler.this.getProperties().get(2) == 0;
                    }
                    @Override
                    public boolean canTakeItems(PlayerEntity player) {
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
                    @Override public boolean canInsert(ItemStack stack) { return false; }
                    @Override public boolean canTakeItems(PlayerEntity playerEntity) { return false; }
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
    public void onContentChanged(Inventory inv) {
        super.onContentChanged(inv);
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
            partner.receiveMirrorFromPartner(i, selfInv.getStack(i));
        }
        partner.sendContentUpdates();
    }

    void receiveMirrorFromPartner(int index, ItemStack stack) {
        if (index >= 0 && index < otherInv.size()) {
            otherInv.setStack(index, stack == null ? ItemStack.EMPTY : stack.copy());
            otherInv.markDirty();
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
            if (session.aHandler != null) session.aHandler.syncState();
            if (session.bHandler != null) session.bHandler.syncState();
        }
    }

    // ======================== MONEY / STATE SYNC ========================

    public void syncState() {
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
        sendContentUpdates();
    }

    public PropertyDelegate getProperties() { return props; }

    // ======================== COMPLETE / CANCEL ========================

    void returnItems() { dump(selfInv, player); }

    List<ItemStack> takeItemsForCompletion() {
        List<ItemStack> out = new ArrayList<>();
        for (int i = 0; i < selfInv.size(); i++) {
            ItemStack s = selfInv.getStack(i);
            if (!s.isEmpty()) {
                out.add(s.copy());
                selfInv.setStack(i, ItemStack.EMPTY);
            }
        }
        selfInv.markDirty();
        return out;
    }

    private static void dump(SimpleInventory inv, PlayerEntity to) {
        for (int i = 0; i < inv.size(); i++) {
            ItemStack s = inv.getStack(i);
            if (!s.isEmpty()) {
                to.getInventory().offerOrDrop(s);
                inv.setStack(i, ItemStack.EMPTY);
            }
        }
        inv.markDirty();
    }

    // ======================== VANILLA PLUMBING ========================

    @Override public boolean canUse(PlayerEntity player) { return true; }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = (index >= 0 && index < this.slots.size()) ? this.slots.get(index) : null;
        if (slot == null || !slot.hasStack()) return empty;

        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();

        if (index >= selfStart && index < selfEnd) {
            if (!this.insertItem(stack, invStart, hotbarEnd, true)) return ItemStack.EMPTY;
        } else if (index >= otherStart && index < otherEnd) {
            return ItemStack.EMPTY;
        } else {
            if (!this.insertItem(stack, selfStart, selfEnd, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();

        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTakeItem(player, stack);

        if (session != null && index >= selfStart && index < selfEnd) {
            mirrorSelfToPartner();
        }

        return original;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (session != null && !session.isClosed()) {
            session.cancel("Closed");
        }
    }
}
