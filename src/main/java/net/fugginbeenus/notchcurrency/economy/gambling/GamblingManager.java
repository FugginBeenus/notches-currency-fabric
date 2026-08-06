package net.fugginbeenus.notchcurrency.economy.gambling;

import net.fugginbeenus.notchcurrency.config.NotchConfig;

public final class GamblingManager {

    private static boolean enabled = true;
    private static long minBet = 10L;
    private static long maxBet = 1_000L;

    private GamblingManager() {}

    public static void init() {
        CoinFlipManager.init();
    }

    public static void applyConfig(NotchConfig cfg) {
        NotchConfig.Gambling g = cfg.gambling;
        enabled = g.enabled;
        minBet = Math.max(1L, g.minBet);
        maxBet = Math.max(minBet, g.maxBet);
        SlotMachineManager.applyConfig(cfg);
        CoinFlipManager.applyConfig(cfg);
    }

    public static boolean isEnabled() { return enabled; }
    public static long getMinBet()    { return minBet; }
    public static long getMaxBet()    { return maxBet; }

    public static long clampBet(long bet) {
        return Math.max(minBet, Math.min(maxBet, bet));
    }

    public static boolean betInRange(long bet) {
        return bet >= minBet && bet <= maxBet;
    }
}
