package net.fugginbeenus.notchcurrency.client;

import java.util.List;

public final class NpcEffectPicks {

    private NpcEffectPicks() {}

    public record Pick(String name, String id) {}

    public static final List<Pick> ALL = List.of(
            new Pick("Regeneration", "minecraft:regeneration"),
            new Pick("Speed", "minecraft:speed"),
            new Pick("Jump boost", "minecraft:jump_boost"),
            new Pick("Strength", "minecraft:strength"),
            new Pick("Resistance", "minecraft:resistance"),
            new Pick("Fire resistance", "minecraft:fire_resistance"),
            new Pick("Water breathing", "minecraft:water_breathing"),
            new Pick("Night vision", "minecraft:night_vision"),
            new Pick("Invisibility", "minecraft:invisibility"),
            new Pick("Haste", "minecraft:haste"),
            new Pick("Luck", "minecraft:luck"),
            new Pick("Absorption", "minecraft:absorption"),
            new Pick("Health boost", "minecraft:health_boost"),
            new Pick("Saturation", "minecraft:saturation"),
            new Pick("Slowness", "minecraft:slowness"),
            new Pick("Weakness", "minecraft:weakness"),
            new Pick("Poison", "minecraft:poison"),
            new Pick("Blindness", "minecraft:blindness")
    );

    public static String nameFor(String value) {
        String want = base(value);
        for (Pick p : ALL) {
            if (p.id().equals(want)) return p.name();
        }
        return want.isEmpty() ? "None" : "Custom";
    }

    public static String next(String value) {
        String want = base(value);
        String level = levelSuffix(value);
        for (int i = 0; i < ALL.size(); i++) {
            if (ALL.get(i).id().equals(want)) {
                return ALL.get((i + 1) % ALL.size()).id() + level;
            }
        }
        return ALL.get(0).id() + level;
    }

    private static String base(String value) {
        String raw = value == null ? "" : value.trim();
        int space = raw.lastIndexOf(' ');
        if (space <= 0) return raw;
        try {
            Integer.parseInt(raw.substring(space + 1).trim());
            return raw.substring(0, space).trim();
        } catch (NumberFormatException notALevel) {
            return raw;
        }
    }

    private static String levelSuffix(String value) {
        String raw = value == null ? "" : value.trim();
        String b = base(raw);
        return b.length() == raw.length() ? "" : raw.substring(b.length());
    }
}
