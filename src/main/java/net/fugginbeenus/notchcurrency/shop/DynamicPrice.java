package net.fugginbeenus.notchcurrency.shop;

public final class DynamicPrice {

    private DynamicPrice() {}

    public static double ELASTICITY = 0.004;
    public static double MIN_MULT = 0.25;
    public static double MAX_MULT = 4.0;
    public static double DECAY = 0.02;

    public static double multiplier(double stockIndex) {
        double m = 1.0 - ELASTICITY * stockIndex;
        return Math.max(MIN_MULT, Math.min(MAX_MULT, m));
    }

    public static double decayed(double stockIndex) {
        if (stockIndex == 0.0) return 0.0;
        double next = stockIndex * (1.0 - DECAY);
        return Math.abs(next) < 0.01 ? 0.0 : next;
    }
}
