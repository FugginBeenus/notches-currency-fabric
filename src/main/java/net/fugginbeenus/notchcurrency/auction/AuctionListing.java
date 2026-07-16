package net.fugginbeenus.notchcurrency.auction;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.util.UUID;

public final class AuctionListing {
    public final UUID id;
    public final UUID sellerUuid;
    public final String sellerName;
    public final ItemStack stack;
    public final long price;          // starting / buyout price in Notch currency
    public final long createdGameTime;
    public final long expiresGameTime;
    public final String category;     // e.g. "blocks", "gear", etc.

    // --- bidding state ---
    public long   highestBid;          // 0 if no bids
    public UUID   highestBidderUuid;   // null if no bids
    public String highestBidderName;   // null or empty if no bids

    public AuctionListing(UUID id,
                          UUID sellerUuid,
                          String sellerName,
                          ItemStack stack,
                          long price,
                          long createdGameTime,
                          long expiresGameTime,
                          String category) {
        this.id = id;
        this.sellerUuid = sellerUuid;
        this.sellerName = sellerName;
        this.stack = stack.copy();
        this.price = price;
        this.createdGameTime = createdGameTime;
        this.expiresGameTime = expiresGameTime;
        this.category = category;

        // default: no bids yet
        this.highestBid = 0L;
        this.highestBidderUuid = null;
        this.highestBidderName = null;
    }

    public enum Status {
        ACTIVE,
        SOLD,
        EXPIRED
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putUuid("Id", id);
        tag.putUuid("Seller", sellerUuid);
        tag.putString("SellerName", sellerName);
        tag.putLong("Price", price);
        tag.putLong("Created", createdGameTime);
        tag.putLong("Expires", expiresGameTime);
        tag.putString("Category", category);
        tag.put("Stack", StackData.writeStack(stack));

        // bidding fields
        tag.putLong("HighestBid", highestBid);
        if (highestBidderUuid != null) {
            tag.putUuid("HighestBidderUuid", highestBidderUuid);
        }
        if (highestBidderName != null) {
            tag.putString("HighestBidder", highestBidderName);
        }
        return tag;
    }

    public static AuctionListing fromNbt(NbtCompound tag) {
        UUID id = tag.getUuid("Id");
        UUID seller = tag.getUuid("Seller");
        String sellerName = tag.getString("SellerName");
        long price = tag.getLong("Price");
        long created = tag.getLong("Created");
        long expires = tag.getLong("Expires");
        String category = tag.getString("Category");
        ItemStack stack = StackData.readStack(tag.getCompound("Stack"));

        AuctionListing listing = new AuctionListing(
                id, seller, sellerName, stack, price, created, expires, category
        );

        // bidding fields (backwards-compatible if absent)
        if (tag.contains("HighestBid", NbtElement.LONG_TYPE)) {
            listing.highestBid = tag.getLong("HighestBid");
        }
        if (tag.contains("HighestBidderUuid", NbtElement.INT_ARRAY_TYPE)) {
            listing.highestBidderUuid = tag.getUuid("HighestBidderUuid");
        }
        if (tag.contains("HighestBidder", NbtElement.STRING_TYPE)) {
            String name = tag.getString("HighestBidder");
            listing.highestBidderName = name.isEmpty() ? null : name;
        }

        return listing;
    }
}
