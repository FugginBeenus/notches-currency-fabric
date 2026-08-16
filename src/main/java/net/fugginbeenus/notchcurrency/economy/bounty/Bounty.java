package net.fugginbeenus.notchcurrency.economy.bounty;

import net.fugginbeenus.notchcurrency.compat.Reg;
import net.fugginbeenus.notchcurrency.compat.StackData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public class Bounty {

    private final UUID id;
    private final BountyType type;
    private final ResourceLocation target;
    private final int required;
    private final long rewardCoins;
    private final ItemStack rewardItem;
    private final BountyRarity rarity;
    private final boolean repeatable;
    private final long expiresGameTime;
    private final String description;

    public Bounty(UUID id, BountyType type, ResourceLocation target, int required, long rewardCoins,
                  ItemStack rewardItem, BountyRarity rarity, boolean repeatable, long expiresGameTime,
                  String description) {
        this.id = id;
        this.type = type;
        this.target = target;
        this.required = Math.max(1, required);
        this.rewardCoins = Math.max(0, rewardCoins);
        this.rewardItem = rewardItem == null ? ItemStack.EMPTY : rewardItem;
        this.rarity = rarity == null ? BountyRarity.COMMON : rarity;
        this.repeatable = repeatable;
        this.expiresGameTime = expiresGameTime;
        this.description = description == null ? "" : description;
    }

    public UUID getId() { return id; }
    public BountyType getType() { return type; }
    public ResourceLocation getTarget() { return target; }
    public int getRequired() { return required; }
    public long getRewardCoins() { return rewardCoins; }
    public ItemStack getRewardItem() { return rewardItem; }
    public BountyRarity getRarity() { return rarity; }
    public boolean isRepeatable() { return repeatable; }
    public long getExpiresGameTime() { return expiresGameTime; }

    public boolean isExpired(long now) {
        return expiresGameTime > 0 && now >= expiresGameTime;
    }

    public Component targetName() {
        if (type == BountyType.KILL) {
            EntityType<?> et = BuiltInRegistries.ENTITY_TYPE.get(target);
            return et.getDescription();
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(target)).getHoverName();
    }

    public String describe() {
        if (!description.isEmpty()) return description;
        String verb = type == BountyType.KILL ? "Kill " : "Deliver ";
        return verb + required + " " + targetName().getString();
    }

    public String rewardSummary() {
        StringBuilder sb = new StringBuilder();
        if (rewardCoins > 0) sb.append(rewardCoins).append(" " + net.fugginbeenus.notchcurrency.core.CurrencyText.word());
        if (!rewardItem.isEmpty()) {
            if (sb.length() > 0) sb.append(" + ");
            sb.append(rewardItem.getCount()).append(" ").append(rewardItem.getHoverName().getString());
        }
        if (sb.length() == 0) sb.append("nothing");
        return sb.toString();
    }

    public CompoundTag toNbt() {
        CompoundTag o = new CompoundTag();
        net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(o, "Id", id);
        o.putString("Type", type.name());
        o.putString("Target", target.toString());
        o.putInt("Required", required);
        o.putLong("RewardCoins", rewardCoins);
        if (!rewardItem.isEmpty()) o.put("RewardItem", StackData.writeStack(rewardItem));
        o.putString("Rarity", rarity.name());
        o.putBoolean("Repeatable", repeatable);
        o.putLong("Expires", expiresGameTime);
        o.putString("Desc", description);
        return o;
    }

    public static Bounty fromNbt(CompoundTag o) {
        ItemStack reward = o.contains("RewardItem") ? StackData.readStack(o.getCompound("RewardItem")) : ItemStack.EMPTY;
        return new Bounty(
                net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(o, "Id"),
                BountyType.valueOf(o.getString("Type")),
                Reg.parse(o.getString("Target")),
                o.getInt("Required"),
                o.getLong("RewardCoins"),
                reward,
                BountyRarity.fromString(o.getString("Rarity")),
                o.getBoolean("Repeatable"),
                o.getLong("Expires"),
                o.getString("Desc"));
    }
}
