package net.fugginbeenus.notchcurrency.core;

import net.fugginbeenus.notchcurrency.config.NotchConfigIO;

public final class CurrencyText {

    private CurrencyText() {}

    public static String word() {
        String name = NotchConfigIO.get().currency.itemName.trim();
        return name.isEmpty() ? "coins" : name;
    }
}
