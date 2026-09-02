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
    private final boolean quest;
    private final String questKey;
    private final String targetText;
    private final String needsQuest;
    private final boolean handInRequired;
    private final boolean factionOnly;
    private final String needsFaction;
    private final String nextQuest;

    public Bounty(UUID id, BountyType type, ResourceLocation target, int required, long rewardCoins,
                  ItemStack rewardItem, BountyRarity rarity, boolean repeatable, long expiresGameTime,
                  String description) {
        this(id, type, target, required, rewardCoins, rewardItem, rarity, repeatable, expiresGameTime,
                description, false, "", "", "", false, false, "", "");
    }

    public Bounty(UUID id, BountyType type, ResourceLocation target, int required, long rewardCoins,
                  ItemStack rewardItem, BountyRarity rarity, boolean repeatable, long expiresGameTime,
                  String description, boolean quest, String questKey, String targetText,
                  String needsQuest, boolean handInRequired,
                  boolean factionOnly, String needsFaction, String nextQuest) {
        this.nextQuest = nextQuest == null ? "" : nextQuest;
        this.factionOnly = factionOnly;
        this.needsFaction = needsFaction == null ? "" : needsFaction;
        this.handInRequired = handInRequired;
        this.needsQuest = needsQuest == null ? "" : needsQuest;
        this.quest = quest;
        this.questKey = questKey == null ? "" : questKey;
        this.targetText = targetText == null ? "" : targetText;
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
    public boolean isQuest() { return quest; }
    public String getQuestKey() { return questKey; }
    public String getTargetText() { return targetText; }
    public String getNeedsQuest() { return needsQuest; }
    public boolean needsHandIn() { return handInRequired || type == BountyType.FETCH; }
    public boolean isFactionOnly() { return factionOnly || !needsFaction.isEmpty(); }
    public String getNeedsFaction() { return needsFaction; }
    public String getNextQuest() { return nextQuest; }

    public static UUID idForKey(String key) {
        return UUID.nameUUIDFromBytes(("notchquest:" + (key == null ? "" : key.trim().toLowerCase()))
                .getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public boolean isExpired(long now) {
        return expiresGameTime > 0 && now >= expiresGameTime;
    }

    public Component targetName() {
        if (type == BountyType.TALK_TO || type == BountyType.VISIT) {
            return Component.literal(targetText);
        }
        if (type == BountyType.KILL) {
            EntityType<?> et = BuiltInRegistries.ENTITY_TYPE.get(target);
            return et.getDescription();
        }
        return new ItemStack(BuiltInRegistries.ITEM.get(target)).getHoverName();
    }

    public String describe() {
        if (!description.isEmpty()) return description;
        return switch (type) {
            case KILL -> "Kill " + required + " " + targetName().getString();
            case FETCH -> "Collect " + required + " " + targetName().getString();
            case TALK_TO -> "Talk to " + targetText;
            case VISIT -> "Go to " + targetText;
            case DELIVER -> "Take " + required + " " + targetName().getString() + " to " + targetText;
        };
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
        if (quest) o.putBoolean("Quest", true);
        if (!questKey.isEmpty()) o.putString("QuestKey", questKey);
        if (!targetText.isEmpty()) o.putString("TargetText", targetText);
        if (!needsQuest.isEmpty()) o.putString("NeedsQuest", needsQuest);
        if (handInRequired) o.putBoolean("HandIn", true);
        if (factionOnly) o.putBoolean("FactionOnly", true);
        if (!needsFaction.isEmpty()) o.putString("NeedsFaction", needsFaction);
        if (!nextQuest.isEmpty()) o.putString("NextQuest", nextQuest);
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
                o.getString("Desc"),
                o.getBoolean("Quest"),
                o.getString("QuestKey"),
                o.getString("TargetText"),
                o.getString("NeedsQuest"),
                o.getBoolean("HandIn"),
                o.getBoolean("FactionOnly"),
                o.getString("NeedsFaction"),
                o.getString("NextQuest"));
    }
}
