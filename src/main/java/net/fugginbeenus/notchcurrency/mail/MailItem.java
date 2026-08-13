package net.fugginbeenus.notchcurrency.mail;

import net.fugginbeenus.notchcurrency.compat.Nbt;
import net.fugginbeenus.notchcurrency.compat.StackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One thing waiting for one player: a parcel from another player, an auction payout, or the item
 * side of a trade offer.
 *
 * <p>An entry can carry several stacks, an amount of coins, or both. Several because one send is
 * one parcel however much went into it, and both because an auction sale owes the seller money and
 * the buyer goods off the same event.
 *
 * @param id       this entry, so a player can take one thing without taking the lot
 * @param sender   who to show it as being from. A name rather than a uuid: the sender may be
 *                 another player, or the auction house, and the label is all the reader needs
 * @param note     an optional line from the sender, shown with the parcel
 * @param contents the goods, or empty
 * @param coins    the money, or zero
 * @param sentAt   the game time it was posted, for sorting and for "how long has this sat here"
 */
public record MailItem(UUID id, String sender, String note, List<ItemStack> contents, long coins,
                       long sentAt) {

    /** As many stacks as fit in one parcel, which is what the Outbox offers to fill. */
    public static final int MAX_CONTENTS = 6;

    public MailItem {
        contents = List.copyOf(contents);
    }

    public static MailItem parcel(String sender, String note, ItemStack stack) {
        return parcel(sender, note, List.of(stack));
    }

    public static MailItem parcel(String sender, String note, List<ItemStack> stacks) {
        List<ItemStack> copies = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && copies.size() < MAX_CONTENTS) copies.add(stack.copy());
        }
        return new MailItem(UUID.randomUUID(), sender, note, copies, 0L, 0L);
    }

    public static MailItem payout(String sender, String note, long coins) {
        return new MailItem(UUID.randomUUID(), sender, note, List.of(), Math.max(0L, coins), 0L);
    }

    public boolean isEmpty() {
        return contents.isEmpty() && coins <= 0L;
    }

    /** A copy stamped with when it was posted, since the clock is not this record's to know. */
    public MailItem at(long gameTime) {
        return new MailItem(id, sender, note, contents, coins, gameTime);
    }

    /** What is left after handing over the goods but not the money, or the other way round. */
    public MailItem without(boolean tookGoods, boolean tookCoins) {
        return new MailItem(id, sender, note,
                tookGoods ? List.of() : contents,
                tookCoins ? 0L : coins,
                sentAt);
    }

    /** The same entry with only part of its goods left, for a delivery an inventory could not hold. */
    public MailItem withContents(List<ItemStack> left) {
        return new MailItem(id, sender, note, left, coins, sentAt);
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        Nbt.putUuid(nbt, "Id", id);
        nbt.putString("Sender", sender);
        if (!note.isEmpty()) nbt.putString("Note", note);
        // Portable rather than native, so a parcel survives a world moving between versions the same
        // way an NPC's equipment does. See StackData.writePortableStack.
        //
        // Numbered keys rather than a list tag: reading a list changed shape more than once across
        // the versions this mod covers, and a counted run of keys reads the same on all of them.
        nbt.putInt("N", contents.size());
        for (int i = 0; i < contents.size(); i++) {
            nbt.put("I" + i, StackData.writePortableStack(contents.get(i)));
        }
        if (coins > 0L) nbt.putLong("Coins", coins);
        nbt.putLong("SentAt", sentAt);
        return nbt;
    }

    public static MailItem fromNbt(CompoundTag nbt) {
        List<ItemStack> contents = new ArrayList<>();
        // Mail written before a parcel could hold more than one thing.
        if (nbt.contains("Stack")) {
            ItemStack old = StackData.readPortableStack(nbt.getCompound("Stack"));
            if (!old.isEmpty()) contents.add(old);
        }
        int count = nbt.getInt("N");
        for (int i = 0; i < count; i++) {
            if (!nbt.contains("I" + i)) continue;
            ItemStack stack = StackData.readPortableStack(nbt.getCompound("I" + i));
            if (!stack.isEmpty()) contents.add(stack);
        }
        return new MailItem(
                Nbt.hasUuid(nbt, "Id") ? Nbt.getUuid(nbt, "Id") : UUID.randomUUID(),
                nbt.getString("Sender"),
                nbt.getString("Note"),
                contents,
                nbt.getLong("Coins"),
                nbt.getLong("SentAt"));
    }
}
