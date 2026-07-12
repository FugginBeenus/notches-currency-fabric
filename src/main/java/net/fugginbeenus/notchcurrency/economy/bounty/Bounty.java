package net.fugginbeenus.notchcurrency.economy.bounty;

import net.fugginbeenus.notchcurrency.compat.Reg;
import net.fugginbeenus.notchcurrency.compat.StackData;
import net.minecraft.entity.EntityType;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.UUID;

/**
 * A single bounty: kill N of a mob, or deliver N of an item, for a coin and/or item reward.
 * Rewards are a money/item FAUCET. Generated bounties carry a rarity and an expiry; admin-posted
 * ones are permanent ({@code expiresGameTime == 0}). Repeatable bounties can be done again;
 * others pay once per player.
 */
public class Bounty {

    private final UUID id;
    private final BountyType type;
    private final Identifier target;    // entity-type id (KILL) or item id (FETCH)
    private final int required;
    private final long rewardCoins;
    private final ItemStack rewardItem; // empty = none
    private final BountyRarity rarity;
    private final boolean repeatable;
    private final long expiresGameTime;  // 0 = never
    private final String description;    // optional override; auto-generated when blank

    public Bounty(UUID id, BountyType type, Identifier target, int required, long rewardCoins,
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
    public Identifier getTarget() { return target; }
    public int getRequired() { return required; }
    public long getRewardCoins() { return rewardCoins; }
    public ItemStack getRewardItem() { return rewardItem; }
    public BountyRarity getRarity() { return rarity; }
    public boolean isRepeatable() { return repeatable; }
    public long getExpiresGameTime() { return expiresGameTime; }

    public boolean isExpired(long now) {
        return expiresGameTime > 0 && now >= expiresGameTime;
    }

    /** Display name of the target mob/item. */
    public Text targetName() {
        if (type == BountyType.KILL) {
            EntityType<?> et = Registries.ENTITY_TYPE.get(target);
            return et.getName();
        }
        return new ItemStack(Registries.ITEM.get(target)).getName();
    }

    /** Human-readable task line, e.g. "Kill 10 Zombie" or "Deliver 32 Iron Ingot". */
    public String describe() {
        if (!description.isEmpty()) return description;
        String verb = type == BountyType.KILL ? "Kill " : "Deliver ";
        return verb + required + " " + targetName().getString();
    }

    /** Short reward summary for the board, e.g. "50 coins" / "1 Diamond" / "50 coins + 1 Diamond". */
    public String rewardSummary() {
        StringBuilder sb = new StringBuilder();
        if (rewardCoins > 0) sb.append(rewardCoins).append(" " + net.fugginbeenus.notchcurrency.core.CurrencyText.word());
        if (!rewardItem.isEmpty()) {
            if (sb.length() > 0) sb.append(" + ");
            sb.append(rewardItem.getCount()).append(" ").append(rewardItem.getName().getString());
        }
        if (sb.length() == 0) sb.append("nothing");
        return sb.toString();
    }

    // ---- NBT ----

    public NbtCompound toNbt() {
        NbtCompound o = new NbtCompound();
        o.putUuid("Id", id);
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

    public static Bounty fromNbt(NbtCompound o) {
        ItemStack reward = o.contains("RewardItem") ? StackData.readStack(o.getCompound("RewardItem")) : ItemStack.EMPTY;
        return new Bounty(
                o.getUuid("Id"),
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
