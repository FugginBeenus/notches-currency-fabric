package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

/**
 * Keeps this mod's screens on the screen.
 *
 * <p>Every panel here is laid out at a fixed size, and the biggest is 400 by 290. At a GUI scale of
 * three that wants 1200 by 870 real pixels, so on a laptop it simply runs off the edges. Rather than
 * rebuild thirty-seven layouts to be fluid, this drops the GUI scale by as little as it takes while
 * one of our screens is open, and puts the player's own setting back the moment they leave.
 *
 * <p>Letting Minecraft do the scaling is the point. Drawing, mouse coordinates and vanilla widgets
 * all stay in agreement, which they would not if we scaled the drawing ourselves and left everything
 * else in the old coordinate space.
 */
public final class GuiScale {

    private GuiScale() {}

    // The bounding box of the largest panel in the mod. One rule for all of them beats asking each
    // screen its size, and leaves room for a screen to grow a little without anyone remembering this.
    private static final int DESIGN_WIDTH = 400;
    private static final int DESIGN_HEIGHT = 290;

    // The player's own setting, kept while we are borrowing it. Zero means we have not touched it.
    // A double because the scale was one until 1.21.11 turned it into a plain int.
    private static double borrowedFrom = 0;

    public static boolean isOurs(Screen screen) {
        return screen != null && screen.getClass().getName().startsWith("net.fugginbeenus.notchcurrency.");
    }

    /** Shrinks the GUI scale, by as little as possible, until a full size panel fits. */
    public static void fit(Minecraft client, Screen screen) {
        if (client == null) return;
        var window = client.getWindow();
        double current = window.getGuiScale();
        if (current <= 1 || fits(client)) return;

        double original = borrowedFrom == 0 ? current : borrowedFrom;
        int chosen = 1;
        // Down one step at a time, stopping at the first that fits. Setting the scale is cheap and
        // recalculates the scaled size, so asking the window is more reliable than repeating its maths.
        for (int scale = (int) current - 1; scale >= 1; scale--) {
            applyScale(window, scale);
            if (fits(client)) {
                chosen = scale;
                break;
            }
        }
        // Nothing fit, so the smallest is the best on offer. The window is simply tiny.
        borrowedFrom = original;
        applyScale(window, chosen);
        relayout(client, screen);
    }

    /** Puts the player's setting back, if we borrowed it. */
    public static void release(Minecraft client, Screen screen) {
        if (client == null || borrowedFrom == 0) return;
        double original = borrowedFrom;
        borrowedFrom = 0;
        applyScale(client.getWindow(), original);
        relayout(client, screen);
    }

    private static void applyScale(com.mojang.blaze3d.platform.Window window, double scale) {
        //? if >=1.21.11 {
        /*window.setGuiScale((int) scale);
        *///?} else {
        window.setGuiScale(scale);
        //?}
    }

    private static boolean fits(Minecraft client) {
        var window = client.getWindow();
        return window.getGuiScaledWidth() >= DESIGN_WIDTH && window.getGuiScaledHeight() >= DESIGN_HEIGHT;
    }

    private static void relayout(Minecraft client, Screen screen) {
        if (screen == null) return;
        var window = client.getWindow();
        //? if >=1.21.11 {
        /*screen.resize(window.getGuiScaledWidth(), window.getGuiScaledHeight());
        *///?} else {
        screen.resize(client, window.getGuiScaledWidth(), window.getGuiScaledHeight());
        //?}
    }
}
