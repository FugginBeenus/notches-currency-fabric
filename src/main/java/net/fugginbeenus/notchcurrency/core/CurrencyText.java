package net.fugginbeenus.notchcurrency.core;

import net.fugginbeenus.notchcurrency.config.NotchConfigIO;

/**
 * The currency's display word for server-built messages ("You won 50 ___"). Returns the admin's
 * custom coin name when one is set (the same name the currency maker bakes into the resource
 * pack), so renaming the coin carries through chat and GUI text everywhere, not just the item.
 */
public final class CurrencyText {

    private CurrencyText() {}

    /** "coins", or the custom coin name from the config ("Rupees" → "You won 50 Rupees"). */
    public static String word() {
        String name = NotchConfigIO.get().currency.itemName.trim();
        return name.isEmpty() ? "coins" : name;
    }
}
