package net.fugginbeenus.notchcurrency.client.npc;

import java.util.UUID;

public record NpcEditorState(UUID npcId, int roleOrdinal, String name, String ownerName, boolean canEdit,
                             String model, String skinType, String skinValue, boolean slim,
                             float scale, float scaleY, float scaleZ, float nameOffset,
                             int behaviorOrdinal, int wanderRadius, int dialogueNodes, boolean dialogueFlat,
                             int statsBits, int dialogueMode, int waypointCount, int patrolSpeedIdx,
                             int patrolWaitIdx, int poseId, int poseAnim, int maxHealth, int speedPct,
                             int regen, String followName, int movesBits, String farewell,
                             String billboard, String subtitle, String voice, int voicePitch) {
}
