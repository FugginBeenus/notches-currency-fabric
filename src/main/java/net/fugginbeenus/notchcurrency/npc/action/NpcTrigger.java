package net.fugginbeenus.notchcurrency.npc.action;

public enum NpcTrigger {

    ON_INTERACT("When talked to", "Runs as soon as a player interacts, before any dialogue or shop opens.", 20),
    ON_PROXIMITY("When a player comes near", "Runs once as a player walks into range, and re-arms when they leave.", 0),
    ON_HURT("When hurt", "Runs when the NPC is hit, even if it's protected from the damage.", 40),
    ON_DEATH("When killed", "Runs as the NPC dies. May have no player attached - a fall or lava counts.", 0),
    ON_KILL("When it kills", "Runs when the NPC kills something.", 0),
    ON_NPC_NEAR("When an NPC is near", "Runs when another NPC comes close. Use it for chatter between NPCs.", 0),
    ON_QUEST_DONE("When a quest is finished", "Runs on this NPC when a player hands in a quest to it.", 0);

    private final String label;
    private final String hint;
    private final int cooldownTicks;

    NpcTrigger(String label, String hint, int cooldownTicks) {
        this.label = label;
        this.hint = hint;
        this.cooldownTicks = cooldownTicks;
    }

    public int cooldownTicks() { return cooldownTicks; }
    public String label() { return label; }
    public String hint() { return hint; }
}
