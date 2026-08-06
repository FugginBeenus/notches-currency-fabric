package net.fugginbeenus.notchcurrency.block;

import net.minecraft.util.StringIdentifiable;

public enum CoinFace implements StringIdentifiable {
    HEADS("heads"),
    TAILS("tails");

    private final String name;

    CoinFace(String name) { this.name = name; }

    @Override
    public String asString() { return name; }
}
