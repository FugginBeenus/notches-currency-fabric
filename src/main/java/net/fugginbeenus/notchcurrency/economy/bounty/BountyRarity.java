package net.fugginbeenus.notchcurrency.economy.bounty;

import net.minecraft.ChatFormatting;

public enum BountyRarity {
    COMMON(ChatFormatting.WHITE, 60),
    UNCOMMON(ChatFormatting.GREEN, 25),
    RARE(ChatFormatting.AQUA, 12),
    EPIC(ChatFormatting.LIGHT_PURPLE, 3);

    private final ChatFormatting color;
    private final int weight;

    BountyRarity(ChatFormatting color, int weight) {
        this.color = color;
        this.weight = weight;
    }

    public ChatFormatting color() {
        return color;
    }

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
