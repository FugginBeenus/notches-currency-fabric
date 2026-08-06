package net.fugginbeenus.notchcurrency.economy.crate;

import net.minecraft.util.Identifier;

import java.util.List;

public record CrateDef(String id, String name, int keysRequired, List<LootEntry> loot) {

    public record LootEntry(boolean isItem, Identifier itemId, int min, int max, long coins, int weight) {}

    public int totalWeight() {
        int t = 0;
        for (LootEntry e : loot) t += e.weight();
        return t;
    }
}
