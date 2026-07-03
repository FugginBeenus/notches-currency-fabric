package net.fugginbeenus.notchcurrency.client.npc;

import java.util.UUID;

/**
 * Everything the NPC editor needs about the NPC being edited, as sent by NPC_EDITOR_OPEN. Bundled so
 * the payload can grow without the editor constructor sprouting another parameter each time.
 */
public record NpcEditorState(UUID npcId, int roleOrdinal, String name, String ownerName, boolean canEdit,
                             String model, String skinType, String skinValue, boolean slim, float scale,
                             int behaviorOrdinal, int wanderRadius, int dialogueNodes, int statsBits,
                             int dialogueMode, int waypointCount, int patrolSpeedIdx, int poseId,
                             int maxHealth, int speedPct, int regen, String followName, int movesBits) {
}
