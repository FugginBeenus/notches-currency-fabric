package net.fugginbeenus.notchcurrency.npc.action;

/**
 * Something that happens to an NPC and can set off a list of actions. Dialogue already reacts to a
 * choice being clicked; these are the moments outside a conversation.
 */
public enum NpcTrigger {

    ON_INTERACT("When talked to", "Runs as soon as a player interacts, before any dialogue or shop opens."),
    ON_PROXIMITY("When a player comes near", "Runs once as a player walks into range, and re-arms when they leave."),
    ON_HURT("When hurt", "Runs when the NPC actually takes damage."),
    ON_DEATH("When killed", "Runs as the NPC dies. May have no player attached — a fall or lava counts."),
    ON_KILL("When it kills", "Runs when the NPC kills something.");

    private final String label;
    private final String hint;

    NpcTrigger(String label, String hint) {
        this.label = label;
        this.hint = hint;
    }

    /** Short editor label. */
    public String label() { return label; }

    /** One line of explanation for the editor, so the trigger doesn't need a wiki page. */
    public String hint() { return hint; }
}
