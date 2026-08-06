package net.fugginbeenus.notchcurrency.npc.schedule;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity.Behavior;

/**
 * What an NPC is doing during one block of its day.
 *
 * <p>Each stance is a thin cover over a behaviour the NPC already has, which is the whole point: a
 * schedule decides <em>when</em>, and the movement code that has been working all along decides
 * <em>how</em>. Nothing here drives the NPC directly.
 *
 * <p>{@link #STAND} and {@link #SLEEP} are the two that need the NPC to travel to a particular block
 * first, which is what {@code NpcScheduleGoal} exists for. {@link #WANDER} and {@link #PATROL} hand
 * over to goals that already know what to do and leave that goal idle.
 */
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

    /** The existing behaviour this stance switches the NPC into. */
    public Behavior behavior() {
        return behavior;
    }

    /** True when the entry is unusable without a spot, which is what the repair flow looks for. */
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
