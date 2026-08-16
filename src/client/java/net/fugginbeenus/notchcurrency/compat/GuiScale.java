package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public final class GuiScale {

    private GuiScale() {}
    private static final int DESIGN_WIDTH = 400;
    private static final int DESIGN_HEIGHT = 290;
    private static double borrowedFrom = 0;

    public static boolean isOurs(Screen screen) {
        return screen != null && screen.getClass().getName().startsWith("net.fugginbeenus.notchcurrency.");
    }

    public static void fit(Minecraft client, Screen screen) {
        if (client == null) return;
        var window = client.getWindow();
        double current = window.getGuiScale();
        if (current <= 1 || fits(client)) return;

        double original = borrowedFrom == 0 ? current : borrowedFrom;
        int chosen = 1;
        for (int scale = (int) current - 1; scale >= 1; scale--) {
            applyScale(window, scale);
            if (fits(client)) {
                chosen = scale;
                break;
            }
        }
        borrowedFrom = original;
        applyScale(window, chosen);
        relayout(client, screen);
    }

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
