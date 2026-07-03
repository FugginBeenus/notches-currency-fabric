package net.fugginbeenus.notchcurrency.economy.cosmetic;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The loaded cosmetic offers, keyed by id (the datapack filename), insertion-ordered so the shop
 * shows them in a stable order. Populated from {@code data/<ns>/cosmetics/*.json} by
 * {@link CosmeticLoader}, merged across packs.
 */
public final class CosmeticRegistry {

    private static final Map<String, CosmeticOffer> OFFERS = new LinkedHashMap<>();

    private CosmeticRegistry() {}

    public static void clear() {
        OFFERS.clear();
    }

    public static void put(CosmeticOffer offer) {
        OFFERS.put(offer.id(), offer);
    }

    @Nullable
    public static CosmeticOffer get(String id) {
        return OFFERS.get(id);
    }

    public static List<CosmeticOffer> all() {
        return new ArrayList<>(OFFERS.values());
    }

    public static int count() {
        return OFFERS.size();
    }
}
