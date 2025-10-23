package net.fugginbeenus.notchcurrency.client;

public final class ClientBalance {
    private static volatile int cached = 0;
    private ClientBalance() {}

    public static int get() { return cached; }
    public static void set(int value) { cached = Math.max(0, value); }
}
