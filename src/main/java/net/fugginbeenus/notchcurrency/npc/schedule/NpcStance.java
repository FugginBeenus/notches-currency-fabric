package net.fugginbeenus.notchcurrency.npc.schedule;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity.Behavior;

public enum NpcStance {

    SLEEP("Sleep", "Walks to the bed and lies in it until the next entry.", Behavior.STATIONARY, true),
    WANDER("Wander", "Strolls around the spot, within the radius.", Behavior.WANDER, true),
    STAND("Stand", "Walks to the spot and holds still. The one for standing at a counter.",
            Behavior.STATIONARY, true),
    PATROL("Patrol", "Walks the NPC's waypoint route for this block of time.", Behavior.PATROL, false);

    private final String label;
    private final String hint;
    private final Behavior behavior;
    private final boolean needsSpot;

    NpcStance(String label, String hint, Behavior behavior, boolean needsSpot) {
        this.label = label;
        this.hint = hint;
        this.behavior = behavior;
        this.needsSpot = needsSpot;
    }

    public String label() {
        return label;
    }

    public String hint() {
        return hint;
    }

    public Behavior behavior() {
        return behavior;
    }

    public boolean needsSpot() {
        return needsSpot;
    }

    public static NpcStance byName(String name, NpcStance fallback) {
        for (NpcStance s : values()) {
            if (s.name().equals(name)) return s;
        }
        return fallback;
    }
}
