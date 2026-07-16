package net.fugginbeenus.notchcurrency.client;

import net.blay09.mods.waystones.client.gui.screen.WaystoneSelectionScreenBase;
import net.blay09.mods.waystones.client.gui.widget.WaystoneButton;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * Prices each destination in the Waystones selection menu: a small "N ⛁" tag on the right edge of
 * every waystone button, using the dimensional fee for cross-dimension trips. Fee values are synced
 * from the server on join ({@link WaystoneFees}).
 *
 * <p>Only ever loaded when the Waystones mod is present (gated in ClientInit). The button doesn't
 * expose its waystone, so it's read from the button's one field of the waystone API type — found by
 * type, not name, and if a future Waystones build moves it the tags just don't draw.
 */
public final class WaystoneFeeOverlay {

    /** The coin glyph mapped into minecraft:default (same one chat uses). */
    private static final String COIN = "";

    @Nullable private static Field waystoneField;
    private static boolean fieldSearched = false;

    private WaystoneFeeOverlay() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof WaystoneSelectionScreenBase) {
                ScreenEvents.afterRender(screen).register(WaystoneFeeOverlay::drawFees);
            }
        });
    }

    private static void drawFees(Screen screen, DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!WaystoneFees.enabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        RegistryKey<World> here = mc.world.getRegistryKey();

        for (ClickableWidget widget : Screens.getButtons(screen)) {
            if (!(widget instanceof WaystoneButton button) || !button.visible) continue;
            RegistryKey<World> destination = dimensionOf(button);
            if (destination == null) continue;

            int fee = here.equals(destination) ? WaystoneFees.fee() : WaystoneFees.dimensionalFee();
            if (fee <= 0) continue;

            String tag = fee + " " + COIN;
            int w = mc.textRenderer.getWidth(tag);
            int tx = button.getX() + button.getWidth() - w - 6;
            int ty = button.getY() + (button.getHeight() - 8) / 2;
            ctx.drawText(mc.textRenderer, tag, tx, ty, NotchTheme.ACCENT_GOLD, true);
        }
    }

    @Nullable
    private static RegistryKey<World> dimensionOf(WaystoneButton button) {
        Field field = findWaystoneField(button);
        if (field == null) return null;
        try {
            Object waystone = field.get(button);
            //? if >=1.21 {
            /*if (waystone instanceof net.blay09.mods.waystones.api.Waystone ws) return ws.getDimension();
            *///?} else {
            if (waystone instanceof net.blay09.mods.waystones.api.IWaystone ws) return ws.getDimension();
            //?}
        } catch (Exception ignored) {
        }
        return null;
    }

    @Nullable
    private static Field findWaystoneField(WaystoneButton button) {
        if (fieldSearched) return waystoneField;
        fieldSearched = true;
        //? if >=1.21 {
        /*Class<?> api = net.blay09.mods.waystones.api.Waystone.class;
        *///?} else {
        Class<?> api = net.blay09.mods.waystones.api.IWaystone.class;
        //?}
        for (Class<?> cls = button.getClass(); cls != null && cls != Object.class; cls = cls.getSuperclass()) {
            for (Field candidate : cls.getDeclaredFields()) {
                if (api.isAssignableFrom(candidate.getType())) {
                    candidate.setAccessible(true);
                    waystoneField = candidate;
                    return waystoneField;
                }
            }
        }
        return null;
    }
}
