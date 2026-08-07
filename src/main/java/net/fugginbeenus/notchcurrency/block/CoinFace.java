package net.fugginbeenus.notchcurrency.block;

import net.minecraft.util.StringRepresentable;

public enum CoinFace implements StringRepresentable {
    HEADS("heads"),
    TAILS("tails");

    private final String name;

    CoinFace(String name) { this.name = name; }

    @Override
    public String getSerializedName() { return name; }
}
