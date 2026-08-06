package net.fugginbeenus.notchcurrency.economy.crate;

import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;

public final class CrateRegistry {

    private static final Map<String, CrateDef> CRATES = new LinkedHashMap<>();

    private CrateRegistry() {}

    public static void clear() {
        CRATES.clear();
    }

    public static void put(CrateDef def) {
        CRATES.put(def.id(), def);
    }

    @Nullable
    public static CrateDef get(String id) {
        return CRATES.get(id);
    }

    public static int count() {
        return CRATES.size();
    }

    @Nullable
    public static CrateDef.LootEntry roll(CrateDef def, Random rng) {
        int total = def.totalWeight();
        if (total <= 0) return null;
        int r = rng.nextInt(total);
        for (CrateDef.LootEntry e : def.loot()) {
            r -= e.weight();
            if (r < 0) return e;
        }
        return null;
    }
}
