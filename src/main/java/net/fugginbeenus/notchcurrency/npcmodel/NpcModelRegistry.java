package net.fugginbeenus.notchcurrency.npcmodel;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * What bundles this game knows about, by id.
 *
 * <p>Filled by the client loader, which is where the files and the resource manager are. It lives on
 * the common side because the entity's animation choice needs to read it, and that code is shared.
 * On a dedicated server it simply stays empty, which callers treat the same as "no bundles".
 *
 * <p>Read from the render thread every frame, so a concurrent map rather than a plain one.
 */
public final class NpcModelRegistry {

    private NpcModelRegistry() {}

    private static final Map<String, NpcModelBundle> LOADED = new ConcurrentHashMap<>();

    public static void replaceAll(Collection<NpcModelBundle> bundles) {
        LOADED.clear();
        for (NpcModelBundle bundle : bundles) LOADED.put(bundle.id(), bundle);
    }

    /** The bundle an NPC model id points at, or null for a built-in model or a missing bundle. */
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
