package net.fugginbeenus.notchcurrency.mail;

import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * The inbox as slots you drag out of, rather than a list you press buttons on.
 *
 * <p>The slots are a view of the mail, not storage: each one is a copy of a waiting entry, and
 * taking one is what actually collects it. Nothing can be put in, and whatever is still sitting
 * there when the screen closes is untouched, because it never left the mail in the first place.
 *
 * <p>Coins do not appear here at all. An auction payout has no item to put in a slot, so the screen
 * shows those as a total with its own collect button.
 */
public class MailInboxMenu extends AbstractContainerMenu {

    public static final int COLS = 9, ROWS = 3;
    public static final int INBOX_SLOTS = COLS * ROWS;
    public static final int INBOX_X = MailLayout.MAIN_X, INBOX_Y = MailLayout.SLOTS_Y;
    private static final int INV_X = MailLayout.INV_X, INV_Y = MailLayout.INV_Y,
            HOTBAR_Y = MailLayout.HOTBAR_Y;

    private final SimpleContainer view = new SimpleContainer(INBOX_SLOTS);
    /** Which mail entry each slot is showing, so taking one knows what to remove. */
    private final UUID[] backing = new UUID[INBOX_SLOTS];

    public MailInboxMenu(int containerId, Inventory playerInv) {
        super(ModScreenHandlers.MAIL_INBOX, containerId);

        for (int i = 0; i < INBOX_SLOTS; i++) {
            final int index = i;
            addSlot(new Slot(view, i, INBOX_X + (i % COLS) * 18, INBOX_Y + (i / COLS) * 18) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false; // a view of the mail, not somewhere to put things
                }

                @Override
                public void onTake(Player player, ItemStack taken) {
                    collected(player, index);
                    super.onTake(player, taken);
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInv, col + row * 9 + 9, INV_X + col * 18, INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, INV_X + col * 18, HOTBAR_Y));
        }
    }

    public static void open(ServerPlayer sp) {
        // The same name on both tabs: whose box you are standing at does not change whose mail this
        // is, and a title that changed on a tab click would read as a different window.
        sp.openMenu(new SimpleMenuProvider(
                (containerId, inv, p) -> {
                    MailInboxMenu menu = new MailInboxMenu(containerId, inv);
                    menu.fillFrom(sp);
                    return menu;
                },
                Component.literal("Mailbox")));
    }

    /** Shows the waiting entries that actually have an item in them. */
    public void fillFrom(ServerPlayer player) {
        MailSweep.run(player.level().getServer());
        var waiting = MailState.get(player.level().getServer()).inbox(player.getUUID());
        int slot = 0;
        for (MailItem item : waiting) {
            if (slot >= INBOX_SLOTS) break;
            if (item.stack().isEmpty()) continue;
            view.setItem(slot, item.stack().copy());
            backing[slot] = item.id();
            slot++;
        }
    }

    /** What each slot is showing, so the labels can be lined up with the slots exactly. */
    public MailItem[] shownEntries(MailState state, UUID owner) {
        MailItem[] out = new MailItem[INBOX_SLOTS];
        for (int i = 0; i < INBOX_SLOTS; i++) {
            if (backing[i] == null) continue;
            for (MailItem item : state.inbox(owner)) {
                if (item.id().equals(backing[i])) {
                    out[i] = item;
                    break;
                }
            }
        }
        return out;
    }

    /** Taking a slot is the collection: the entry leaves the mail here, not before. */
    private void collected(Player player, int index) {
        if (player.level().isClientSide || !(player instanceof ServerPlayer sp)) return;
        UUID entryId = backing[index];
        if (entryId == null) return;

        MailState state = MailState.get(sp.level().getServer());
        MailItem taken = state.take(sp.getUUID(), entryId);
        if (taken == null) {
            backing[index] = null;
            return;
        }

        // A right click takes half. Whatever is still sitting in the slot has not been collected, so
        // it goes back under the same id rather than vanishing when the screen closes. The same
        // applies to coins riding along with the goods, which are not in any slot to be taken.
        ItemStack left = view.getItem(index).copy();
        if (!left.isEmpty() || taken.coins() > 0L) {
            state.putBack(sp.getUUID(), new MailItem(taken.id(), taken.sender(), taken.note(),
                    left, taken.coins(), taken.sentAt()));
        }
        backing[index] = left.isEmpty() ? null : entryId;
        MailManager.sendSummary(sp);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // The slots were only ever a view. Anything left is still in the mail, so it is dropped from
        // the view rather than handed over.
        for (int i = 0; i < view.getContainerSize(); i++) {
            view.setItem(i, ItemStack.EMPTY);
            backing[i] = null;
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack inSlot = slot.getItem();
        ItemStack original = inSlot.copy();
        if (index < INBOX_SLOTS) {
            if (!moveItemStackTo(inSlot, INBOX_SLOTS, this.slots.size(), true)) return ItemStack.EMPTY;
            if (inSlot.isEmpty()) slot.set(ItemStack.EMPTY);
            else slot.setChanged();
            // Shift-clicking is still taking it, so the mail has to hear about it.
            slot.onTake(player, original);
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY; // nothing goes the other way
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
