package net.fugginbeenus.notchcurrency.shop;

import net.minecraft.server.level.ServerLevel;

public final class Restock {

    private Restock() {}

    public enum Mode {
        OFF,
        GAME_DAILY,
        GAME_WEEKLY,
        REAL_DAILY,
        REAL_WEEKLY;

        public static Mode byName(String name) {
            if (name == null) return OFF;
            try {
                return valueOf(name);
            } catch (IllegalArgumentException unknown) {
                return OFF;
            }
        }

        public String label() {
            return switch (this) {
                case OFF -> "Never";
                case GAME_DAILY -> "Each game day";
                case GAME_WEEKLY -> "Each game week";
                case REAL_DAILY -> "Each real day";
                case REAL_WEEKLY -> "Each real week";
            };
        }

        public Mode next() {
            Mode[] all = values();
            return all[(ordinal() + 1) % all.length];
        }
    }

    private static final long GAME_DAY = 24000L;
    private static final long REAL_DAY_MS = 86_400_000L;

    public static long periodOf(Mode mode, ServerLevel level) {
        return switch (mode) {
            case OFF -> 0L;
            case GAME_DAILY -> level.getDayTime() / GAME_DAY;
            case GAME_WEEKLY -> level.getDayTime() / (GAME_DAY * 7L);
            case REAL_DAILY -> System.currentTimeMillis() / REAL_DAY_MS;
            case REAL_WEEKLY -> System.currentTimeMillis() / (REAL_DAY_MS * 7L);
        };
    }

    public static boolean isDue(Mode mode, long lastPeriod, ServerLevel level) {
        if (mode == Mode.OFF) return false;
        return periodOf(mode, level) != lastPeriod;
    }
}
