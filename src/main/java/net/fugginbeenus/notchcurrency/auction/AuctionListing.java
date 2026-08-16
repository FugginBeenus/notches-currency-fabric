package net.fugginbeenus.notchcurrency.auction;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public final class AuctionListing {
    public final UUID id;
    public final UUID sellerUuid;
    public final String sellerName;
    public final ItemStack stack;
    public final long price;
    public final long createdGameTime;
    public final long expiresGameTime;
    public final String category;
    public long   highestBid;
    public UUID   highestBidderUuid;
    public String highestBidderName;

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
        this.highestBid = 0L;
        this.highestBidderUuid = null;
        this.highestBidderName = null;
    }

    public enum Status {
        ACTIVE,
        SOLD,
        EXPIRED
    }

    public CompoundTag toNbt() {
        CompoundTag tag = new CompoundTag();
        net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(tag, "Id", id);
        net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(tag, "Seller", sellerUuid);
        tag.putString("SellerName", sellerName);
        tag.putLong("Price", price);
        tag.putLong("Created", createdGameTime);
        tag.putLong("Expires", expiresGameTime);
        tag.putString("Category", category);
        tag.put("Stack", StackData.writeStack(stack));
        tag.putLong("HighestBid", highestBid);
        if (highestBidderUuid != null) {
            net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(tag, "HighestBidderUuid", highestBidderUuid);
        }
        if (highestBidderName != null) {
            tag.putString("HighestBidder", highestBidderName);
        }
        return tag;
    }

    public static AuctionListing fromNbt(CompoundTag tag) {
        UUID id = net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(tag, "Id");
        UUID seller = net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(tag, "Seller");
        String sellerName = tag.getString("SellerName");
        long price = tag.getLong("Price");
        long created = tag.getLong("Created");
        long expires = tag.getLong("Expires");
        String category = tag.getString("Category");
        ItemStack stack = StackData.readStack(tag.getCompound("Stack"));

        AuctionListing listing = new AuctionListing(
                id, seller, sellerName, stack, price, created, expires, category
        );

        if (tag.contains("HighestBid", Tag.TAG_LONG)) {
            listing.highestBid = tag.getLong("HighestBid");
        }
        if (tag.contains("HighestBidderUuid", Tag.TAG_INT_ARRAY)) {
            listing.highestBidderUuid = net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(tag, "HighestBidderUuid");
        }
        if (tag.contains("HighestBidder", Tag.TAG_STRING)) {
            String name = tag.getString("HighestBidder");
            listing.highestBidderName = name.isEmpty() ? null : name;
        }

        return listing;
    }
}
