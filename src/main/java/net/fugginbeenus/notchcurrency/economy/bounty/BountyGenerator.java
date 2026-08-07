package net.fugginbeenus.notchcurrency.economy.bounty;

import org.jetbrains.annotations.Nullable;

import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

public final class BountyGenerator {

    private BountyGenerator() {}

    @Nullable
    public static Bounty generate(long now, long durationTicks, Random rng, @Nullable Set<String> categories,
                                  int rewardMultPercent, long maxCoinReward) {
        if (BountyPools.isEmpty()) return null;

        BountyRarity rarity = pickRarity(rng);
        BountyPools.ObjectiveEntry obj = BountyPools.pickObjective(rarity, categories, rng);
        if (obj == null) return null;
        // Match the reward to the objective's actual rarity (it may have fallen back to another tier).
        BountyPools.RewardEntry rew = BountyPools.pickReward(obj.rarity(), rng);
        if (rew == null) return null;

        int count = randRange(obj.min(), obj.max(), rng);
        int amount = randRange(rew.min(), rew.max(), rng);

        long coins = 0L;
        ItemStack item = ItemStack.EMPTY;
        if (rew.item()) {
            item = new ItemStack(BuiltInRegistries.ITEM.get(rew.itemId()), Math.max(1, amount));
        } else {
            coins = (long) amount * rewardMultPercent / 100;   // global scale
            if (maxCoinReward > 0) coins = Math.min(coins, maxCoinReward); // hard cap
        }

        long expires = durationTicks <= 0 ? 0 : now + durationTicks;
        return new Bounty(UUID.randomUUID(), obj.type(), obj.target(), count, coins, item,
                obj.rarity(), false, expires, "");
    }

    private static BountyRarity pickRarity(Random rng) {
        int total = 0;
        for (BountyRarity r : BountyRarity.values()) total += r.weight();
        int roll = rng.nextInt(total);
        for (BountyRarity r : BountyRarity.values()) {
            roll -= r.weight();
            if (roll < 0) return r;
        }
        return BountyRarity.COMMON;
    }

    private static int randRange(int min, int max, Random rng) {
        return max <= min ? min : rng.nextInt(max - min + 1) + min;
    }
}
