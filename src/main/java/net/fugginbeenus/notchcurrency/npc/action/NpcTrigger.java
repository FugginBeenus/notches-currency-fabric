package net.fugginbeenus.notchcurrency.npc.action;

/**
 * Something that happens to an NPC and can set off a list of actions. Dialogue already reacts to a
 * choice being clicked; these are the moments outside a conversation.
 */
public enum NpcTrigger {

    ON_INTERACT("When talked to", "Runs as soon as a player interacts, before any dialogue or shop opens.", 20),
    // Paced per player as they arrive rather than per NPC, so two people walking up together are
    // both greeted: see the proximity scan on the entity.
    ON_PROXIMITY("When a player comes near", "Runs once as a player walks into range, and re-arms when they leave.", 0),
    ON_HURT("When hurt", "Runs when the NPC is hit, even if it's protected from the damage.", 40),
    ON_DEATH("When killed", "Runs as the NPC dies. May have no player attached - a fall or lava counts.", 0),
    ON_KILL("When it kills", "Runs when the NPC kills something.", 0);

    private final String label;
    private final String hint;
    private final int cooldownTicks;

    NpcTrigger(String label, String hint, int cooldownTicks) {
        this.label = label;
        this.hint = hint;
        this.cooldownTicks = cooldownTicks;
    }

    /**
     * Shortest gap between firings, so a trigger that can happen in bursts can't spam chat or
     * commands. Zero means the moment paces itself. It either happens once, or is paced elsewhere.
     */
    public int cooldownTicks() { return cooldownTicks; }

    /** Short editor label. */
    public String label() { return label; }

    /** One line of explanation for the editor, so the trigger doesn't need a wiki page. */
    public String hint() { return hint; }
}
