package net.fugginbeenus.notchcurrency.client;

/**
 * The server's waystone teleport fees, synced on join (see WaystoneFeeHandler). Deliberately free of
 * any Waystones classes: this always loads, while the overlay that uses it only loads when the
 * Waystones mod is present.
 */
public final class WaystoneFees {

    private static boolean enabled = false;
    private static int fee = 0;
    private static int dimensionalFee = 0;

    private WaystoneFees() {}

    public static void set(boolean isEnabled, int sameDimension, int crossDimension) {
        enabled = isEnabled;
        fee = sameDimension;
        dimensionalFee = crossDimension;
    }

    public static boolean enabled() {
        return enabled;
    }

    public static int fee() {
        return fee;
    }

    public static int dimensionalFee() {
        return dimensionalFee;
    }
}
