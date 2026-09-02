package net.fugginbeenus.notchcurrency.client;

import java.util.ArrayList;
import java.util.List;

public final class QuestNames {

    private QuestNames() {}

    public record Entry(String key, String summary) {}

    private static List<Entry> known = new ArrayList<>();

    public static void set(List<Entry> list) {
        known = list == null ? new ArrayList<>() : new ArrayList<>(list);
    }

    public static List<Entry> all() { return known; }

    public static String next(String current) {
        List<String> options = new ArrayList<>();
        options.add("");
        for (Entry e : known) {
            if (!e.key().isBlank() && !options.contains(e.key())) options.add(e.key());
        }
        if (options.size() == 1) return current == null ? "" : current;
        int at = options.indexOf(current == null ? "" : current.trim());
        return options.get((at < 0 ? 0 : at + 1) % options.size());
    }
}
