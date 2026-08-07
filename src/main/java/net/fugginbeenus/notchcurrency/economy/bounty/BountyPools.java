package net.fugginbeenus.notchcurrency.economy.bounty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import net.minecraft.resources.ResourceLocation;

public final class BountyPools {

    public record ObjectiveEntry(BountyType type, ResourceLocation target, int min, int max,
                                 BountyRarity rarity, int weight, String category) {}

    public record RewardEntry(boolean item, ResourceLocation itemId, int min, int max,
                              BountyRarity rarity, int weight) {}

    private static final List<ObjectiveEntry> OBJECTIVES = new ArrayList<>();
    private static final List<RewardEntry> REWARDS = new ArrayList<>();
    private static final Map<ResourceLocation, String> DECREES = new HashMap<>(); // decree item -> category

    private BountyPools() {}

    public static void clear() {
        OBJECTIVES.clear();
        REWARDS.clear();
        DECREES.clear();
    }

    public static void addDecree(ResourceLocation item, String category) {
        DECREES.put(item, category);
    }

    public static String decreeCategory(ResourceLocation item) {
        return DECREES.get(item);
    }

    public static void addObjective(ObjectiveEntry e) {
        OBJECTIVES.add(e);
    }

    public static void addReward(RewardEntry e) {
        REWARDS.add(e);
    }

    public static boolean isEmpty() {
        return OBJECTIVES.isEmpty() || REWARDS.isEmpty();
    }

    public static int objectiveCount() {
        return OBJECTIVES.size();
    }

    public static int rewardCount() {
        return REWARDS.size();
    }

    public static ObjectiveEntry pickObjective(BountyRarity rarity, @org.jetbrains.annotations.Nullable Set<String> categories, Random rng) {
        ObjectiveEntry e = weightedObjective(rarity, categories, rng);
        return e != null ? e : weightedObjective(null, categories, rng);
    }

    public static RewardEntry pickReward(BountyRarity rarity, Random rng) {
        RewardEntry e = weightedReward(rarity, rng);
        return e != null ? e : weightedReward(null, rng);
    }

    private static ObjectiveEntry weightedObjective(BountyRarity rarity, Set<String> categories, Random rng) {
        int total = 0;
        for (ObjectiveEntry e : OBJECTIVES) if (matches(e, rarity, categories)) total += e.weight();
        if (total <= 0) return null;
        int roll = rng.nextInt(total);
        for (ObjectiveEntry e : OBJECTIVES) {
            if (!matches(e, rarity, categories)) continue;
            roll -= e.weight();
            if (roll < 0) return e;
        }
        return null;
    }

    private static boolean matches(ObjectiveEntry e, BountyRarity rarity, Set<String> categories) {
        if (rarity != null && e.rarity() != rarity) return false;
        return categories == null || categories.contains(e.category());
    }

    private static RewardEntry weightedReward(BountyRarity rarity, Random rng) {
        int total = 0;
        for (RewardEntry e : REWARDS) if (rarity == null || e.rarity() == rarity) total += e.weight();
        if (total <= 0) return null;
        int roll = rng.nextInt(total);
        for (RewardEntry e : REWARDS) {
            if (rarity != null && e.rarity() != rarity) continue;
            roll -= e.weight();
            if (roll < 0) return e;
        }
        return null;
    }
}
