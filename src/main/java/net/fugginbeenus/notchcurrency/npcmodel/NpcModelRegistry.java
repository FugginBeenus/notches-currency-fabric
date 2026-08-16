package net.fugginbeenus.notchcurrency.npcmodel;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class NpcModelRegistry {

    private NpcModelRegistry() {}

    private static final Map<String, NpcModelBundle> LOADED = new ConcurrentHashMap<>();

    public static void replaceAll(Collection<NpcModelBundle> bundles) {
        LOADED.clear();
        for (NpcModelBundle bundle : bundles) LOADED.put(bundle.id(), bundle);
    }

    public static NpcModelBundle forModelId(String modelId) {
        String id = NpcModelBundle.bundleIdOf(modelId);
        return id == null ? null : LOADED.get(id);
    }

    public static boolean isBundle(String modelId) {
        return NpcModelBundle.bundleIdOf(modelId) != null;
    }

    public static List<NpcModelBundle> all() {
        List<NpcModelBundle> out = new java.util.ArrayList<>(LOADED.values());
        out.sort((a, b) -> a.displayName().compareToIgnoreCase(b.displayName()));
        return out;
    }

    public static int count() {
        return LOADED.size();
    }
}
