package net.fugginbeenus.notchcurrency.client;

public final class ClientState {
    private static int balance;

    private ClientState() {}

    public static int getBalance() {
        return balance;
    }
    public static void setBalance(int value) {
        balance = Math.max(0, value);
    }
}
