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

public class MailInboxMenu extends AbstractContainerMenu {

    public static final int COLS = 9, ROWS = 4;
    public static final int INBOX_SLOTS = COLS * ROWS;

    private final SimpleContainer view = new SimpleContainer(INBOX_SLOTS);
    private final UUID[] backing = new UUID[INBOX_SLOTS];
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
                    return false;
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
        sp.openMenu(new SimpleMenuProvider(
                (containerId, inv, p) -> {
                    MailInboxMenu menu = new MailInboxMenu(containerId, inv);
                    menu.fillFrom(sp);
                    return menu;
                },
                Component.literal("Mailbox")));
    }

    public int waiting() {
        return waiting.get();
    }

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

    private void collected(Player player, int index) {
        if (player.level().isClientSide || !(player instanceof ServerPlayer sp)) return;
        UUID entryId = backing[index];
        if (entryId == null) return;
        backing[index] = null;
        MailState state = MailState.get(sp.level().getServer());
        state.take(sp.getUUID(), entryId);
        waiting.set(state.count(sp.getUUID()));
    }

    public void takeAll(ServerPlayer player) {
        MailState state = MailState.get(player.level().getServer());
        int parcels = 0, items = 0;
        long coins = 0L;
        boolean ranOutOfRoom = false;

        for (int i = 0; i < INBOX_SLOTS; i++) {
            UUID entryId = backing[i];
            if (entryId == null) continue;

            MailItem before = find(state, player.getUUID(), entryId);
            if (before == null) {
                view.setItem(i, ItemStack.EMPTY);
                backing[i] = null;
                continue;
            }

            MailManager.collect(player, entryId, false);
            if (find(state, player.getUUID(), entryId) != null) {
                view.setItem(i, ParcelItem.of(find(state, player.getUUID(), entryId)));
                ranOutOfRoom = true;
                break;
            }

            parcels++;
            coins += before.coins();
            for (ItemStack stack : before.contents()) items += stack.getCount();
            view.setItem(i, ItemStack.EMPTY);
            backing[i] = null;
        }

        MailManager.announceCollected(player, parcels, items, coins, ranOutOfRoom);
        refill(player);
        broadcastChanges();
    }

    private static MailItem find(MailState state, UUID owner, UUID entryId) {
        for (MailItem item : state.inbox(owner)) {
            if (item.id().equals(entryId)) return item;
        }
        return null;
    }

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
            slot.onTake(player, original);
            return ItemStack.EMPTY;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
