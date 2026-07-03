package net.fugginbeenus.notchcurrency.block;

import net.minecraft.util.StringIdentifiable;

/**
 * Which face the coin-flip block is currently resting on. A blockstate property so a future model
 * can show the landed side without needing a block entity.
 */
public enum CoinFace implements StringIdentifiable {
    HEADS("heads"),
    TAILS("tails");

    private final String name;

    CoinFace(String name) { this.name = name; }

    @Override
    public String asString() { return name; }
}
