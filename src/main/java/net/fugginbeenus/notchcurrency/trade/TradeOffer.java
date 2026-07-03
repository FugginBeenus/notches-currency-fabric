package net.fugginbeenus.notchcurrency.trade;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

import java.util.UUID;

/**
 * A standing, offline trade offer: a player escrows one stack of items and names a price (coins
 * and/or one requested item stack). Another player — a specific named target, or anyone if no name
 * was set — can accept it later, even if the creator is offline. Payment goes to the creator (coins
 * to their balance by UUID, items to their mailbox), and the escrowed items go to the accepter.
 */
public class TradeOffer {

    private final UUID id;
    private final UUID creatorUuid;
    private final String creatorName;
    private final String targetName;   // lowercase; "" = open to anyone
    private final ItemStack offered;   // escrowed items handed to the accepter
    private final long priceCoins;     // coins the accepter pays the creator
    private final ItemStack requestedItem; // item the accepter pays (may be empty); count = required
    private final long createdTime;

    public TradeOffer(UUID id, UUID creatorUuid, String creatorName, String targetName,
                      ItemStack offered, long priceCoins, ItemStack requestedItem, long createdTime) {
        this.id = id;
        this.creatorUuid = creatorUuid;
        this.creatorName = creatorName;
        this.targetName = targetName == null ? "" : targetName.toLowerCase();
        this.offered = offered;
        this.priceCoins = Math.max(0, priceCoins);
        this.requestedItem = requestedItem == null ? ItemStack.EMPTY : requestedItem;
        this.createdTime = createdTime;
    }

    public UUID id() { return id; }
    public UUID creatorUuid() { return creatorUuid; }
    public String creatorName() { return creatorName; }
    public String targetName() { return targetName; }
    public ItemStack offered() { return offered; }
    public long priceCoins() { return priceCoins; }
    public ItemStack requestedItem() { return requestedItem; }
    public boolean requestsItem() { return !requestedItem.isEmpty(); }
    public boolean isOpen() { return targetName.isEmpty(); }

    /** Whether the named player is allowed to accept this offer (target match, or open). */
    public boolean acceptableBy(String playerName) {
        return isOpen() || targetName.equalsIgnoreCase(playerName);
    }

    public NbtCompound toNbt() {
        NbtCompound t = new NbtCompound();
        t.putUuid("Id", id);
        t.putUuid("Creator", creatorUuid);
        t.putString("CreatorName", creatorName);
        t.putString("Target", targetName);
        t.put("Offered", offered.writeNbt(new NbtCompound()));
        t.putLong("Price", priceCoins);
        if (!requestedItem.isEmpty()) t.put("Requested", requestedItem.writeNbt(new NbtCompound()));
        t.putLong("Created", createdTime);
        return t;
    }

    public static TradeOffer fromNbt(NbtCompound t) {
        return new TradeOffer(
                t.getUuid("Id"),
                t.getUuid("Creator"),
                t.getString("CreatorName"),
                t.getString("Target"),
                ItemStack.fromNbt(t.getCompound("Offered")),
                t.getLong("Price"),
                t.contains("Requested") ? ItemStack.fromNbt(t.getCompound("Requested")) : ItemStack.EMPTY,
                t.getLong("Created"));
    }
}
