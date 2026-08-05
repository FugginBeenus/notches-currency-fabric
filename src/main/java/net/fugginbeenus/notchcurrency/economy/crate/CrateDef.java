package net.fugginbeenus.notchcurrency.economy.crate;

import net.minecraft.util.Identifier;

import java.util.List;

/**
 * A datapack-defined crate type: a name, how many keys it costs to open, and a weighted loot
 * table (items and/or coins). Loot odds are transparent: the crate shows them on request.
 */
public record CrateDef(String id, String name, int keysRequired, List<LootEntry> loot) {

    /** One weighted loot outcome: an item stack (count in [min,max]) or a coin payout. */
    public record LootEntry(boolean isItem, Identifier itemId, int min, int max, long coins, int weight) {}

    public int totalWeight() {
        int t = 0;
        for (LootEntry e : loot) t += e.weight();
        return t;
    }
}
