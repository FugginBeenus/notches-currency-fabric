package net.fugginbeenus.notchcurrency.client;

public final class ClientBalanceState {
    private static int balance = 0;
    public static int get() { return balance; }
    public static void set(int v) { balance = Math.max(0, v); }
}
