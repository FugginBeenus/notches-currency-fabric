package net.fugginbeenus.notchcurrency.shop;

import java.util.List;

public final class SignStyles {

    public static final List<String> IDS = List.of(
            "plain", "wood", "darkwood", "stone", "brass", "parchment",
            "royal", "mystic", "spooky", "frost", "painted");
    public static final List<String> NAMES = List.of(
            "Plain", "Wood", "Dark wood", "Stone", "Brass", "Parchment",
            "Royal", "Mystic", "Spooky", "Frost", "Painted");
    public static final String SHOP_DEFAULT = "wood", NPC_DEFAULT = "plain";

    private SignStyles() {}

    public static int indexOf(String id) {
        int i = IDS.indexOf(id);
        return Math.max(0, i);
    }

    public static String clean(String id, String fallback) {
        return id != null && IDS.contains(id) ? id : fallback;
    }

    public static String byIndex(int i, String fallback) {
        return i >= 0 && i < IDS.size() ? IDS.get(i) : fallback;
    }
}
