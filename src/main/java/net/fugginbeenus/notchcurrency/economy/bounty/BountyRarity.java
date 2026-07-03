package net.fugginbeenus.notchcurrency.economy.bounty;

import net.minecraft.util.Formatting;

/**
 * Bounty rarity tiers (à la Bountiful). Rarity drives how often a bounty of that tier is rolled
 * and pairs a harder objective with a bigger reward. {@code weight} is the default roll weight.
 */
public enum BountyRarity {
    COMMON(Formatting.WHITE, 60),
    UNCOMMON(Formatting.GREEN, 25),
    RARE(Formatting.AQUA, 12),
    EPIC(Formatting.LIGHT_PURPLE, 3);

    private final Formatting color;
    private final int weight;

    BountyRarity(Formatting color, int weight) {
        this.color = color;
        this.weight = weight;
    }

    public Formatting color() {
        return color;
    }

    /** A darker, saturated ARGB accent that stays readable on the light GUI panel. */
    public int accentArgb() {
        return switch (this) {
            case COMMON -> 0xFF8A8A8A;
            case UNCOMMON -> 0xFF4CA64C;
            case RARE -> 0xFF3A7BD5;
            case EPIC -> 0xFF9B4CD5;
        };
    }

    public int weight() {
        return weight;
    }

    public static BountyRarity fromString(String s) {
        try {
            return valueOf(s.toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return COMMON;
        }
    }
}
