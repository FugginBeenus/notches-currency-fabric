package net.fugginbeenus.notchcurrency.economy.gambling;

import net.minecraft.item.Item;
import net.minecraft.item.Items;

/**
 * A symbol on the slot-machine reels. Each carries a reel {@code weight} (how often it lands), a
 * {@code mult3} payout for three-of-a-kind and a {@code mult2} payout for two-of-a-kind, plus the
 * vanilla item used to draw it in the GUI (placeholder art — no custom textures needed).
 *
 * <p>These are the <b>raw</b> multipliers. {@link SlotMachineManager} normalises them by a single
 * factor so the machine's expected return matches the configured house edge, whatever the weights.
 */
public enum SlotSymbol {
    CHERRY (35, 3.0,   1.5,  Items.SWEET_BERRIES),
    BELL   (25, 6.0,   2.0,  Items.GOLD_INGOT),
    GRAPE  (18, 12.0,  3.0,  Items.EMERALD),
    GEM    (10, 30.0,  5.0,  Items.DIAMOND),
    STAR   (4,  120.0, 8.0,  Items.NETHER_STAR); // jackpot

    private final int weight;
    private final double mult3;
    private final double mult2;
    private final Item displayItem;

    SlotSymbol(int weight, double mult3, double mult2, Item displayItem) {
        this.weight = weight;
        this.mult3 = mult3;
        this.mult2 = mult2;
        this.displayItem = displayItem;
    }

    public int weight()       { return weight; }
    public double mult3()      { return mult3; }
    public double mult2()      { return mult2; }
    public Item displayItem()  { return displayItem; }

    public static int totalWeight() {
        int t = 0;
        for (SlotSymbol s : values()) t += s.weight;
        return t;
    }
}
