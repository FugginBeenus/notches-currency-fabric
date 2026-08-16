package net.fugginbeenus.notchcurrency.npcmodel;

import java.util.List;

public record NpcModelBundle(String id, String name, String author, float scale,
                             float width, float height,
                             String idle, String walk, List<String> special) {

    public static final String PREFIX = "npc:";

    public NpcModelBundle {
        special = List.copyOf(special);
    }

    public static String modelIdFor(String bundleId) {
        return PREFIX + bundleId;
    }

    public static String bundleIdOf(String modelId) {
        return modelId != null && modelId.startsWith(PREFIX) ? modelId.substring(PREFIX.length()) : null;
    }

    public String displayName() {
        return name == null || name.isBlank() ? id : name;
    }

    public String assetName() {
        return "npc_" + id;
    }
}
