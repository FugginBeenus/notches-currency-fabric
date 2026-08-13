package net.fugginbeenus.notchcurrency.npcmodel;

import java.util.List;

/**
 * One custom NPC model: a Blockbench export plus the handful of things the mod needs to know about
 * it.
 *
 * <p>The clip names are the bundle's own. A pack author calls their animations whatever they like,
 * so rather than imposing names the mod asks which of theirs fills each role. Anything left blank
 * falls back to the role above it, so a bundle with only an idle still works.
 *
 * @param id      folder name, and the tail of the NPC model id: {@code npc:<id>}
 * @param name    what to show in the picker
 * @param author  optional, for credit in the picker
 * @param scale   applied on top of the NPC's own scale
 * @param width   hitbox width, or 0 to leave the NPC's own
 * @param height  hitbox height, or 0 to leave the NPC's own
 * @param idle    clip for standing still. Required in practice, though a missing one is survivable
 * @param walk    clip for moving, or empty to keep playing the idle
 * @param special clips for the occasional flourish when the NPC is set to Lively
 */
public record NpcModelBundle(String id, String name, String author, float scale,
                             float width, float height,
                             String idle, String walk, List<String> special) {

    /** The prefix that tells the renderer this NPC wears a bundle rather than a built-in model. */
    public static final String PREFIX = "npc:";

    public NpcModelBundle {
        special = List.copyOf(special);
    }

    public static String modelIdFor(String bundleId) {
        return PREFIX + bundleId;
    }

    /** The bundle id inside an NPC model id, or null if that model is not a bundle. */
    public static String bundleIdOf(String modelId) {
        return modelId != null && modelId.startsWith(PREFIX) ? modelId.substring(PREFIX.length()) : null;
    }

    public String displayName() {
        return name == null || name.isBlank() ? id : name;
    }

    /** The asset name these files were written under, shared by model, texture and animations. */
    public String assetName() {
        return "npc_" + id;
    }
}
