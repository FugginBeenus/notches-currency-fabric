package net.fugginbeenus.notchcurrency.economy.bounty;

public enum BountyType {
    KILL,
    FETCH,
    TALK_TO,
    VISIT,
    DELIVER;

    public boolean usesNpc() { return this == TALK_TO || this == DELIVER; }

    public boolean usesItem() { return this == FETCH || this == DELIVER; }

    public String label() {
        return switch (this) {
            case KILL -> "Kill";
            case FETCH -> "Collect";
            case TALK_TO -> "Talk to";
            case VISIT -> "Go to";
            case DELIVER -> "Deliver to";
        };
    }
}
