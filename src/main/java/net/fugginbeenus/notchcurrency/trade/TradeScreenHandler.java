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

/**
 * Two 3x3 panes + money fields + confirm.
 * Mirrors each player's left/self 3x3 into the partner's right/read-only 3x3 in real time.
 */
public class TradeScreenHandler extends ScreenHandler {

    private final PlayerEntity player;
    /** Null on client stub; non-null on the server. */
    TradeManager.TradeSession session = null;  // <-- initialize here (not final)
    private final boolean leftSideSelf;

    // 3x3 inventories for each side (these are THIS client’s view)
    private final SimpleInventory selfInv  = new SimpleInventory(9);
    private final SimpleInventory otherInv = new SimpleInventory(9);

    // [0]=selfMoney, [1]=otherMoney, [2]=selfReady(0/1), [3]=otherReady(0/1), [4]=stage (unused)
    private final PropertyDelegate props = new ArrayPropertyDelegate(5);

    // --- Layout (must match your texture / screen) ---
    private static final int SELF_GRID_X  = 34;
    private static final int SELF_GRID_Y  = 25;
    private static final int OTHER_GRID_X = 142;
    private static final int OTHER_GRID_Y = 25;

    private static final int PLAYER_INV_X = 34;
    private static final int PLAYER_INV_Y = 135;
    private static final int HOTBAR_Y     = PLAYER_INV_Y + 58;

    // slot counts
    private static final int SELF_SIZE   = 9;   // 3x3
    private static final int OTHER_SIZE  = 9;   // 3x3
    private static final int INV_SIZE    = 27;  // player main (3x9)
    private static final int HOTBAR_SIZE = 9;   // hotbar

    // index ranges (set in buildSlots)
    private int selfStart, selfEnd;
    private int otherStart, otherEnd;
    private int invStart, invEnd;
    private int hotbarStart, hotbarEnd;

    /** Fires whenever the 3×3 "self" grid changes; mirrors to partner (server-side). */
    private final InventoryChangedListener selfListener = inv -> {
        if (session != null && inv == selfInv) {
            mirrorSelfToPartner();
        }
    };

    /* ---------- CLIENT constructor (registerSimple uses this) ---------- */
    public TradeScreenHandler(int syncId, PlayerInventory inv) {
        super(ModScreenHandlers.TRADE, syncId);
        this.player = inv.player;
        this.leftSideSelf = true;    // doesn’t matter on client
        // session remains null on client
        hookInventories();           // register listeners
        buildSlots();
        this.addProperties(props);
    }

    /* ---------- SERVER constructor (called from TradeManager.openScreens) ---------- */
    public TradeScreenHandler(int syncId, PlayerInventory inv, PlayerEntity player,
                              TradeManager.TradeSession session, boolean selfOnLeft) {
        super(ModScreenHandlers.TRADE, syncId);
        this.player = player;
        this.session = session;      // now set on server
        this.leftSideSelf = selfOnLeft;
        hookInventories();           // register listeners
        buildSlots();
        this.addProperties(props);
        syncState();
    }

    /** Register inventory listeners (no double-registration). */
    private void hookInventories() {
        selfInv.addListener(selfListener);
    }

    /** Build slots for both 3×3 panes and the embedded player inventory/hotbar. */
    private void buildSlots() {
        // --- Your (self) 3x3 offer grid on the left ---
        int leftX = SELF_GRID_X, leftY = SELF_GRID_Y;
        selfStart = this.slots.size();
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                this.addSlot(new Slot(selfInv, r * 3 + c, leftX + c * 18, leftY + r * 18) {
                    @Override public boolean canInsert(ItemStack stack) { return true; }
                });
            }
        }
        selfEnd = selfStart + SELF_SIZE;

        // --- Other (read-only) 3x3 grid on the right ---
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

        // --- Player inventory (3 rows of 9) ---
        invStart = this.slots.size();
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(this.player.getInventory(), col + row * 9 + 9,
                        PLAYER_INV_X + col * 18,
                        PLAYER_INV_Y + row * 18));
            }
        }
        invEnd = invStart + INV_SIZE;

        // --- Hotbar (1 row of 9) ---
        hotbarStart = this.slots.size();
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(this.player.getInventory(), col,
                    PLAYER_INV_X + col * 18,
                    HOTBAR_Y));
        }
        hotbarEnd = hotbarStart + HOTBAR_SIZE;
    }

    /* ======================== MIRRORING LOGIC ======================== */

    /** Vanilla callback when any bound inventory changes. Extra safety in addition to the listener. */
    @Override
    public void onContentChanged(Inventory inv) {
        super.onContentChanged(inv);
        if (session == null) return; // client stub; server only does mirroring
        if (inv == selfInv) {
            mirrorSelfToPartner();
        }
    }

    /** Copy our self 3x3 into the other handler’s read-only 3x3 and push updates. */
    private void mirrorSelfToPartner() {
        TradeScreenHandler partner =
                (player == session.a) ? session.bHandler : session.aHandler;
        if (partner == null) return;

        for (int i = 0; i < 9; i++) {
            partner.receiveMirrorFromPartner(i, selfInv.getStack(i));
        }
        // Push updates so the partner's client sees changes immediately
        partner.sendContentUpdates();
    }

    /** Set one “other side” slot from our partner (server-side). */
    void receiveMirrorFromPartner(int index, ItemStack stack) {
        if (index >= 0 && index < otherInv.size()) {
            otherInv.setStack(index, stack == null ? ItemStack.EMPTY : stack.copy());
            otherInv.markDirty();
        }
    }

    /* ======================== MONEY / STATE SYNC ======================== */

    /** Server pushes session state into properties so client can render it. */
    public void syncState() {
        if (session == null) return; // client stub
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

    /** Expose properties to the screen (to show the other player’s money). */
    public PropertyDelegate getProperties() { return props; }

    /* ======================== COMPLETE / CANCEL ======================== */

    /** Return any items in the self grid back to the owner (server-side). */
    void returnItems() { dump(selfInv, player); }

    /** Take items from self grid for completion (server-side). */
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

    /* ======================== VANILLA PLUMBING ======================== */

    @Override public boolean canUse(PlayerEntity player) { return true; }

    /** Shift-click routing: player inv <-> self grid (other grid is read-only). */
    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack empty = ItemStack.EMPTY;
        Slot slot = (index >= 0 && index < this.slots.size()) ? this.slots.get(index) : null;
        if (slot == null || !slot.hasStack()) return empty;

        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();

        // From self offer -> back to player inventory/hotbar
        if (index >= selfStart && index < selfEnd) {
            if (!this.insertItem(stack, invStart, hotbarEnd, true)) return ItemStack.EMPTY;
        }
        // From other offer -> nowhere (read-only)
        else if (index >= otherStart && index < otherEnd) {
            return ItemStack.EMPTY;
        }
        // From player inventory/hotbar -> into self offer grid
        else {
            if (!this.insertItem(stack, selfStart, selfEnd, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.setStack(ItemStack.EMPTY);
        else slot.markDirty();

        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTakeItem(player, stack);

        // If we changed our offer, mirror it right away (server-side)
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

