package net.fugginbeenus.notchcurrency.client;

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
