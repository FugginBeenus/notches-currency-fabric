package net.fugginbeenus.notchcurrency.mail;

import net.fugginbeenus.notchcurrency.compat.Nbt;
import net.fugginbeenus.notchcurrency.compat.StackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record MailItem(UUID id, String sender, String note, List<ItemStack> contents, long coins,
                       long sentAt, String wrap) {

    public static final int MAX_CONTENTS = 6;

    public static final String WRAP_PLAIN = "";
    public static final String WRAP_CHRISTMAS = "christmas";
    public static final String WRAP_HALLOWEEN = "halloween";

    public MailItem {
        contents = List.copyOf(contents);
        wrap = wrap == null ? WRAP_PLAIN : wrap;
    }

    public static MailItem parcel(String sender, String note, ItemStack stack) {
        return parcel(sender, note, List.of(stack));
    }

    public static MailItem parcel(String sender, String note, List<ItemStack> stacks) {
        return parcel(sender, note, stacks, 0L);
    }

    public static MailItem parcel(String sender, String note, List<ItemStack> stacks, long coins) {
        return parcel(sender, note, stacks, coins, WRAP_PLAIN);
    }

    public static MailItem parcel(String sender, String note, List<ItemStack> stacks, long coins, String wrap) {
        List<ItemStack> copies = new ArrayList<>();
        for (ItemStack stack : stacks) {
            if (!stack.isEmpty() && copies.size() < MAX_CONTENTS) copies.add(stack.copy());
        }
        return new MailItem(UUID.randomUUID(), sender, note, copies, Math.max(0L, coins), 0L, wrap);
    }

    public static MailItem payout(String sender, String note, long coins) {
        return new MailItem(UUID.randomUUID(), sender, note, List.of(), Math.max(0L, coins), 0L, WRAP_PLAIN);
    }

    public boolean isEmpty() {
        return contents.isEmpty() && coins <= 0L;
    }

    public MailItem at(long gameTime) {
        return new MailItem(id, sender, note, contents, coins, gameTime, wrap);
    }

    public MailItem without(boolean tookGoods, boolean tookCoins) {
        return new MailItem(id, sender, note,
                tookGoods ? List.of() : contents,
                tookCoins ? 0L : coins,
                sentAt, wrap);
    }

    public MailItem withContents(List<ItemStack> left) {
        return new MailItem(id, sender, note, left, coins, sentAt, wrap);
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        Nbt.putUuid(nbt, "Id", id);
        nbt.putString("Sender", sender);
        if (!note.isEmpty()) nbt.putString("Note", note);
        nbt.putInt("N", contents.size());
        for (int i = 0; i < contents.size(); i++) {
            nbt.put("I" + i, StackData.writePortableStack(contents.get(i)));
        }
        if (coins > 0L) nbt.putLong("Coins", coins);
        nbt.putLong("SentAt", sentAt);
        if (!wrap.isEmpty()) nbt.putString("Wrap", wrap);
        return nbt;
    }

    public static MailItem fromNbt(CompoundTag nbt) {
        List<ItemStack> contents = new ArrayList<>();
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
                nbt.getLong("SentAt"),
                nbt.getString("Wrap"));
    }
}
