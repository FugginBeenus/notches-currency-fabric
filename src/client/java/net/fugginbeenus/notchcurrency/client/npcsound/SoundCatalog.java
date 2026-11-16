package net.fugginbeenus.notchcurrency.client.npcsound;

import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

public final class SoundCatalog {

    public static final int SUGGESTED = 0, MOBS = 1, BLOCKS = 2, ITEMS = 3, AMBIENT = 4, MUSIC = 5,
            OTHER = 6, CUSTOM = 7;
    public static final String[] CATEGORIES =
            {"Suggested", "Mobs", "Blocks", "Items", "Ambient", "Music", "Other", "Custom"};

    public record Entry(String id, String name, int category) {}

    private static List<Entry> cache = List.of();
    private static final Map<String, String> NAMES = new HashMap<>();
    private static int builtFrom = -1;

    private SoundCatalog() {}

    public static List<Entry> all() {
        TreeSet<String> ids = new TreeSet<>();
        for (ResourceLocation id : Minecraft.getInstance().getSoundManager().getAvailableSounds()) {
            ids.add(id.toString());
        }
        for (String own : NpcSoundLoader.names()) ids.add("notchcurrency:" + own);
        if (ids.size() == builtFrom) return cache;

        Map<String, String> base = new HashMap<>();
        Map<String, Integer> uses = new HashMap<>();
        for (String id : ids) {
            String name = baseName(id);
            base.put(id, name);
            uses.merge(name, 1, Integer::sum);
        }
        Map<String, String> named = new HashMap<>();
        Map<String, Integer> usesAgain = new HashMap<>();
        for (String id : ids) {
            String name = base.get(id);
            if (uses.get(name) > 1) name = name + " (" + middle(id) + ")";
            named.put(id, name);
            usesAgain.merge(name, 1, Integer::sum);
        }
        List<Entry> out = new ArrayList<>();
        NAMES.clear();
        for (String id : ids) {
            String name = named.get(id);
            if (usesAgain.get(name) > 1) name = base.get(id) + " (" + tail(id) + ")";
            out.add(new Entry(id, name, categoryOf(id)));
            NAMES.put(id, name);
        }
        out.sort((a, b) -> a.name().compareToIgnoreCase(b.name()));
        cache = out;
        builtFrom = ids.size();
        return cache;
    }

    public static String nameOf(String id) {
        String full = full(id);
        if (builtFrom < 0) all();
        String name = NAMES.get(full);
        return name != null ? name : baseName(full);
    }

    public static String full(String id) {
        String t = id == null ? "" : id.trim();
        return t.contains(":") ? t : "minecraft:" + t;
    }

    public static String shortId(String id) {
        return id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
    }

    private static String baseName(String id) {
        ResourceLocation location = ResourceLocation.tryParse(id);
        if (location != null) {
            WeighedSoundEvents events = Minecraft.getInstance().getSoundManager().getSoundEvent(location);
            Component subtitle = events == null ? null : events.getSubtitle();
            if (subtitle != null && !subtitle.getString().isBlank()) return subtitle.getString();
        }
        String words = tail(id);
        return words.isEmpty() ? id : Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    private static String[] parts(String id) {
        int colon = id.indexOf(':');
        return (colon < 0 ? id : id.substring(colon + 1)).split("\\.");
    }

    private static String middle(String id) {
        String[] parts = parts(id);
        if (parts.length <= 2) return parts[parts.length - 1].replace('_', ' ');
        return String.join(" ", Arrays.copyOfRange(parts, 1, parts.length - 1)).replace('_', ' ');
    }

    private static String tail(String id) {
        String[] parts = parts(id);
        return String.join(" ", Arrays.copyOfRange(parts, parts.length > 1 ? 1 : 0, parts.length))
                .replace('_', ' ');
    }

    private static int categoryOf(String id) {
        if (id.startsWith("notchcurrency:")) return CUSTOM;
        if (!id.startsWith("minecraft:")) return OTHER;
        String path = id.substring("minecraft:".length());
        if (path.startsWith("entity.")) return MOBS;
        if (path.startsWith("block.")) return BLOCKS;
        if (path.startsWith("item.")) return ITEMS;
        if (path.startsWith("ambient.") || path.startsWith("weather.")) return AMBIENT;
        if (path.startsWith("music.") || path.startsWith("music_disc.")) return MUSIC;
        return OTHER;
    }
}
