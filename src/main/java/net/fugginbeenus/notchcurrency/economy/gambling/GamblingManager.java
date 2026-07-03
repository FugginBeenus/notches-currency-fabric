package net.fugginbeenus.notchcurrency.economy.gambling;

import net.fugginbeenus.notchcurrency.config.NotchConfig;

/**
 * Shared gambling state: the master toggle and bet limits used by both the slot machine and the
 * coin flip. {@link #init()} and {@link #applyConfig} fan out to the two sub-managers so the rest
 * of the mod only has to wire up one entry point.
 */
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

    /** Clamp a requested bet into the allowed range. */
    public static long clampBet(long bet) {
        return Math.max(minBet, Math.min(maxBet, bet));
    }

    /** True if {@code bet} is a legal bet the player can actually afford (checked by callers). */
    public static boolean betInRange(long bet) {
        return bet >= minBet && bet <= maxBet;
    }
}
