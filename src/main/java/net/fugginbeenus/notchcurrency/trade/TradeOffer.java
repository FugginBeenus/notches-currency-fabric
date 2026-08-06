package net.fugginbeenus.notchcurrency.trade;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class TradeOffer {

    private final UUID id;
    private final UUID creatorUuid;
    private final String creatorName;
    private final String targetName;   // lowercase; "" = open to anyone
    private final List<ItemStack> offeredItems; // escrowed stacks handed to the accepter
    private final long offeredCoins;   // escrowed coins handed to the accepter
    private final long priceCoins;     // coins the accepter pays the creator
    private final List<ItemStack> requestedItems; // items the accepter pays (samples; count = required)
    private final long createdTime;

    public TradeOffer(UUID id, UUID creatorUuid, String creatorName, String targetName,
                      List<ItemStack> offeredItems, long offeredCoins, long priceCoins,
                      List<ItemStack> requestedItems, long createdTime) {
        this.id = id;
        this.creatorUuid = creatorUuid;
        this.creatorName = creatorName;
        this.targetName = targetName == null ? "" : targetName.toLowerCase();
        this.offeredItems = offeredItems == null ? new ArrayList<>() : offeredItems;
        this.offeredCoins = Math.max(0, offeredCoins);
        this.priceCoins = Math.max(0, priceCoins);
        this.requestedItems = requestedItems == null ? new ArrayList<>() : requestedItems;
        this.createdTime = createdTime;
    }

    public UUID id() { return id; }
    public UUID creatorUuid() { return creatorUuid; }
    public String creatorName() { return creatorName; }
    public String targetName() { return targetName; }
    public List<ItemStack> offeredItems() { return offeredItems; }
    public long offeredCoins() { return offeredCoins; }
    public long priceCoins() { return priceCoins; }
    public List<ItemStack> requestedItems() { return requestedItems; }
    public boolean requestsItems() { return !requestedItems.isEmpty(); }
    public boolean isOpen() { return targetName.isEmpty(); }

    public ItemStack firstOffered() {
        return offeredItems.isEmpty() ? ItemStack.EMPTY : offeredItems.get(0);
    }

    public String summary() {
        StringBuilder sb = new StringBuilder();
        if (!offeredItems.isEmpty()) {
            sb.append(firstOffered().getName().getString());
            if (offeredItems.size() > 1) sb.append(" + ").append(offeredItems.size() - 1).append(" more");
        }
        if (offeredCoins > 0) {
            if (sb.length() > 0) sb.append(" + ");
            sb.append(offeredCoins).append(" " + net.fugginbeenus.notchcurrency.core.CurrencyText.word());
        }
        return sb.length() > 0 ? sb.toString() : "nothing";
    }

    public boolean acceptableBy(String playerName) {
        return isOpen() || targetName.equalsIgnoreCase(playerName);
    }

    public NbtCompound toNbt() {
        NbtCompound t = new NbtCompound();
        t.putUuid("Id", id);
        t.putUuid("Creator", creatorUuid);
        t.putString("CreatorName", creatorName);
        t.putString("Target", targetName);
        t.put("OfferedList", writeStacks(offeredItems));
        t.putLong("GiveCoins", offeredCoins);
        t.putLong("Price", priceCoins);
        t.put("RequestedList", writeStacks(requestedItems));
        t.putLong("Created", createdTime);
        return t;
    }

    private static NbtList writeStacks(List<ItemStack> stacks) {
        NbtList list = new NbtList();
        for (ItemStack st : stacks) {
            if (!st.isEmpty()) list.add(StackData.writeStack(st));
        }
        return list;
    }

    private static List<ItemStack> readStacks(NbtCompound t, String key) {
        List<ItemStack> out = new ArrayList<>();
        NbtList list = t.getList(key, NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            ItemStack st = StackData.readStack(list.getCompound(i));
            if (!st.isEmpty()) out.add(st);
        }
        return out;
    }

    public static TradeOffer fromNbt(NbtCompound t) {
        List<ItemStack> items = readStacks(t, "OfferedList");
        if (items.isEmpty() && t.contains("Offered")) {
            // Pre-grid offers stored a single stack.
            ItemStack st = StackData.readStack(t.getCompound("Offered"));
            if (!st.isEmpty()) items.add(st);
        }
        List<ItemStack> wants = readStacks(t, "RequestedList");
        if (wants.isEmpty() && t.contains("Requested")) {
            ItemStack st = StackData.readStack(t.getCompound("Requested"));
            if (!st.isEmpty()) wants.add(st);
        }
        return new TradeOffer(
                t.getUuid("Id"),
                t.getUuid("Creator"),
                t.getString("CreatorName"),
                t.getString("Target"),
                items,
                t.getLong("GiveCoins"),
                t.getLong("Price"),
                wants,
                t.getLong("Created"));
    }
}
