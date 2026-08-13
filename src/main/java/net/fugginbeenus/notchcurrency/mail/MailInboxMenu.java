package net.fugginbeenus.notchcurrency.mail;

import net.fugginbeenus.notchcurrency.item.ParcelItem;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * The inbox: a grid of wrapped parcels to drag out.
 *
 * <p>The slots are a view of the mail rather than storage. Each holds a parcel standing in for a
 * waiting entry, and taking it is what actually collects it. Nothing can be put in, and anything
 * still sitting there when the screen closes is untouched, because it never left the mail.
 *
 * <p>Everything the screen needs to say is written on the parcels themselves, so no packet carries
 * senders or notes alongside. The one thing a slot cannot show is how many parcels did not fit on
 * screen, and that rides on a data slot, which the game keeps in step on its own.
 */
public class MailInboxMenu extends AbstractContainerMenu {

    public static final int COLS = 9, ROWS = 4;
    public static final int INBOX_SLOTS = COLS * ROWS;

    private final SimpleContainer view = new SimpleContainer(INBOX_SLOTS);
    /** Which mail entry each slot is showing, so taking one knows what to remove. */
    private final UUID[] backing = new UUID[INBOX_SLOTS];
    /** Everything waiting, including whatever did not fit on screen. */
    private final DataSlot waiting = DataSlot.standalone();

    public MailInboxMenu(int containerId, Inventory playerInv) {
        super(ModScreenHandlers.MAIL_INBOX, containerId);

        for (int i = 0; i < INBOX_SLOTS; i++) {
            final int index = i;
            addSlot(new Slot(view, i,
                    MailLayout.SLOTS_X + (i % COLS) * 18,
                    MailLayout.SLOTS_Y + (i / COLS) * 18) {
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
                addSlot(new Slot(playerInv, col + row * 9 + 9,
                        MailLayout.INV_X + col * 18, MailLayout.INV_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInv, col, MailLayout.INV_X + col * 18, MailLayout.HOTBAR_Y));
        }
        addDataSlot(waiting);
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

    /** How many parcels are waiting in total, which may be more than there are slots to show. */
    public int waiting() {
        return waiting.get();
    }

    /** Wraps up as many waiting entries as there are slots for. */
    public void fillFrom(ServerPlayer player) {
        MailSweep.run(player.level().getServer());
        List<MailItem> mail = MailState.get(player.level().getServer()).inbox(player.getUUID());
        int slot = 0;
        for (MailItem item : mail) {
            if (slot >= INBOX_SLOTS) break;
            if (item.isEmpty()) continue;
            view.setItem(slot, ParcelItem.of(item));
            backing[slot] = item.id();
            slot++;
        }
        waiting.set(mail.size());
    }

    /** Taking a slot is the collection: the entry leaves the mail here, not before. */
    private void collected(Player player, int index) {
        if (player.level().isClientSide || !(player instanceof ServerPlayer sp)) return;
        UUID entryId = backing[index];
        if (entryId == null) return;
        backing[index] = null;
        MailState state = MailState.get(sp.level().getServer());
        state.take(sp.getUUID(), entryId);
        waiting.set(state.count(sp.getUUID()));
    }

    /**
     * Hands over every parcel on screen that the player has room for.
     *
     * <p>Only what is on screen. Taking those frees the slots, which are then refilled from the rest
     * of the box, so a very full mailbox empties in a few goes rather than dumping two hundred
     * parcels on a player who cannot hold them.
     */
    public void takeAll(ServerPlayer player) {
        for (int i = 0; i < INBOX_SLOTS; i++) {
            ItemStack parcel = view.getItem(i);
            if (parcel.isEmpty()) continue;
            ItemStack giving = parcel.copy();
            player.getInventory().add(giving);
            if (!giving.isEmpty()) break; // out of room, so the rest stay where they are
            view.setItem(i, ItemStack.EMPTY);
            collected(player, i);
        }
        refill(player);
        broadcastChanges();
    }

    /** Puts the next waiting entries into whatever slots have come free. */
    private void refill(ServerPlayer player) {
        MailState state = MailState.get(player.level().getServer());
        List<MailItem> mail = state.inbox(player.getUUID());
        Set<UUID> onScreen = new HashSet<>();
        for (UUID id : backing) {
            if (id != null) onScreen.add(id);
        }
        int slot = 0;
        for (MailItem item : mail) {
            if (item.isEmpty() || onScreen.contains(item.id())) continue;
            while (slot < INBOX_SLOTS && backing[slot] != null) slot++;
            if (slot >= INBOX_SLOTS) break;
            view.setItem(slot, ParcelItem.of(item));
            backing[slot] = item.id();
        }
        waiting.set(mail.size());
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
