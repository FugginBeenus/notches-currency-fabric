package net.fugginbeenus.notchcurrency.mail;

import net.fugginbeenus.notchcurrency.compat.Nbt;
import net.fugginbeenus.notchcurrency.compat.StackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

/**
 * One thing waiting for one player: a parcel from another player, an auction payout, or the item
 * side of a trade offer.
 *
 * <p>An entry can carry an item, an amount of coins, or both, because an auction sale owes the
 * seller money and the buyer goods off the same event.
 *
 * @param id         this entry, so a player can take one thing without taking the lot
 * @param sender     who to show it as being from. A name rather than a uuid: the sender may be
 *                   another player, or the auction house, and the label is all the reader needs
 * @param note       an optional line from the sender, shown under the item
 * @param stack      the goods, or empty
 * @param coins      the money, or zero
 * @param sentAt     the game time it was posted, for sorting and for "how long has this sat here"
 */
public record MailItem(UUID id, String sender, String note, ItemStack stack, long coins, long sentAt) {

    public static MailItem parcel(String sender, String note, ItemStack stack) {
        return new MailItem(UUID.randomUUID(), sender, note, stack.copy(), 0L, 0L);
    }

    public static MailItem payout(String sender, String note, long coins) {
        return new MailItem(UUID.randomUUID(), sender, note, ItemStack.EMPTY, Math.max(0L, coins), 0L);
    }

    public boolean isEmpty() {
        return stack.isEmpty() && coins <= 0L;
    }

    /** A copy stamped with when it was posted, since the clock is not this record's to know. */
    public MailItem at(long gameTime) {
        return new MailItem(id, sender, note, stack, coins, gameTime);
    }

    /** What is left after handing over the goods but not the money, or the other way round. */
    public MailItem without(boolean tookStack, boolean tookCoins) {
        return new MailItem(id, sender, note,
                tookStack ? ItemStack.EMPTY : stack,
                tookCoins ? 0L : coins,
                sentAt);
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        Nbt.putUuid(nbt, "Id", id);
        nbt.putString("Sender", sender);
        if (!note.isEmpty()) nbt.putString("Note", note);
        // Portable rather than native, so a parcel survives a world moving between versions the same
        // way an NPC's equipment does. See StackData.writePortableStack.
        if (!stack.isEmpty()) nbt.put("Stack", StackData.writePortableStack(stack));
        if (coins > 0L) nbt.putLong("Coins", coins);
        nbt.putLong("SentAt", sentAt);
        return nbt;
    }

    public static MailItem fromNbt(CompoundTag nbt) {
        return new MailItem(
                Nbt.hasUuid(nbt, "Id") ? Nbt.getUuid(nbt, "Id") : UUID.randomUUID(),
                nbt.getString("Sender"),
                nbt.getString("Note"),
                nbt.contains("Stack") ? StackData.readPortableStack(nbt.getCompound("Stack")) : ItemStack.EMPTY,
                nbt.getLong("Coins"),
                nbt.getLong("SentAt"));
    }
}
