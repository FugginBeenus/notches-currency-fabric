package net.fugginbeenus.notchcurrency.client;

import net.blay09.mods.waystones.client.gui.screen.WaystoneSelectionScreenBase;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Prices the destination the mouse is over in the Waystones selection menu: a small tooltip with the
 * teleport fee (the dimensional fee for cross-dimension trips), using the same coin glyph chat uses.
 * Fee values are synced from the server on join ({@link WaystoneFees}).
 *
 * <p>Only ever loaded when the Waystones mod is present (gated in ClientInit). The menu lays out its
 * destinations differently per game version — top-level buttons on 1.20.1, a scrolling list on 1.21 —
 * so the hovered waystone is located per-version, then its dimension is read from the one API-typed
 * field on the button (found by type, not name, so a future Waystones build that moves it just shows
 * nothing rather than breaking).
 */
public final class WaystoneFeeOverlay {

    /** The coin glyph mapped into minecraft:default (same one chat uses). */
    private static final String COIN = "";

    //? if >=1.21 {
    /*private static final Class<?> WAYSTONE_API = net.blay09.mods.waystones.api.Waystone.class;
    *///?} else {
    private static final Class<?> WAYSTONE_API = net.blay09.mods.waystones.api.IWaystone.class;
    //?}

    private WaystoneFeeOverlay() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (screen instanceof WaystoneSelectionScreenBase) {
                ScreenEvents.afterRender(screen).register(WaystoneFeeOverlay::drawFeeTooltip);
            }
        });
    }

    private static void drawFeeTooltip(Screen screen, DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!WaystoneFees.enabled()) return;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;

        Object waystone = hoveredWaystone(screen, mouseX, mouseY);
        if (waystone == null) return;

        RegistryKey<World> destination = dimensionOf(waystone);
        if (destination == null) return;

        boolean crossDimension = !mc.world.getRegistryKey().equals(destination);
        int fee = crossDimension ? WaystoneFees.dimensionalFee() : WaystoneFees.fee();
        if (fee <= 0) return;

        List<Text> lines = new ArrayList<>(3);
        lines.add(Text.literal("Teleport Fee").formatted(Formatting.GRAY));
        lines.add(Text.literal(COIN + " " + fee).formatted(Formatting.GOLD));
        if (crossDimension) {
            lines.add(Text.literal("Cross-dimension").formatted(Formatting.DARK_GRAY, Formatting.ITALIC));
        }
        ctx.drawTooltip(mc.textRenderer, lines, mouseX, mouseY);
    }

    /** The waystone the mouse is currently over, or null. The menu's layout differs per game version. */
    @Nullable
    private static Object hoveredWaystone(Screen screen, int mouseX, int mouseY) {
        //? if >=1.21 {
        /*// 1.21: destinations are rows in a scrolling list; each entry's own isMouseOver is scroll-correct.
        for (net.minecraft.client.gui.Element element : screen.children()) {
            if (element instanceof net.blay09.mods.waystones.client.gui.widget.AbstractWaystoneList<?> list
                    && list.isMouseOver(mouseX, mouseY)) {
                for (net.blay09.mods.waystones.client.gui.widget.AbstractWaystoneList.Entry<?> entry : list.children()) {
                    if (entry.isMouseOver(mouseX, mouseY)) {
                        Object button = buttonOf(entry);
                        return button == null ? null : readWaystone(button);
                    }
                }
            }
        }
        return null;
        *///?} else {
        // 1.20.1: destinations are top-level buttons on the screen.
        for (ClickableWidget widget : Screens.getButtons(screen)) {
            if (widget instanceof net.blay09.mods.waystones.client.gui.widget.WaystoneButton button
                    && button.visible && button.isMouseOver(mouseX, mouseY)) {
                return readWaystone(button);
            }
        }
        return null;
        //?}
    }

    //? if >=1.21 {
    /*// The waystone button nested inside a 1.21 list entry, found by type (the entry's field is private).
    @Nullable
    private static Object buttonOf(Object entry) {
        for (Field field : entry.getClass().getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object value = field.get(entry);
                if (value instanceof net.blay09.mods.waystones.client.gui.widget.AbstractWaystoneButton) {
                    return value;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
    *///?}

    /** Read the waystone off a button by its one API-typed field (found by type, not name). */
    @Nullable
    private static Object readWaystone(Object button) {
        for (Class<?> cls = button.getClass(); cls != null && cls != Object.class; cls = cls.getSuperclass()) {
            for (Field field : cls.getDeclaredFields()) {
                if (WAYSTONE_API.isAssignableFrom(field.getType())) {
                    try {
                        field.setAccessible(true);
                        return field.get(button);
                    } catch (Exception ignored) {
                        return null;
                    }
                }
            }
        }
        return null;
    }

    @Nullable
    private static RegistryKey<World> dimensionOf(Object waystone) {
        //? if >=1.21 {
        /*if (waystone instanceof net.blay09.mods.waystones.api.Waystone ws) return ws.getDimension();
        *///?} else {
        if (waystone instanceof net.blay09.mods.waystones.api.IWaystone ws) return ws.getDimension();
        //?}
        return null;
    }
}
