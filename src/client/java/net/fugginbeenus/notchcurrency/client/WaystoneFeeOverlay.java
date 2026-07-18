package net.fugginbeenus.notchcurrency.client;

import net.blay09.mods.waystones.client.gui.screen.WaystoneSelectionScreenBase;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

/**
 * Prices the destination the mouse is over in the Waystones selection menu: a small badge with the
 * teleport fee (the dimensional fee for cross-dimension trips), fronted by the coin item sprite so it
 * follows the player's custom currency art. Fee values are synced from the server on join
 * ({@link WaystoneFees}).
 *
 * <p>Only ever loaded when the Waystones mod is present (gated in ClientInit). The menu lays out its
 * destinations differently per game version — top-level buttons on 1.20.1, a scrolling list on 1.21 —
 * so the hovered waystone is located per-version, then its dimension is read from the one API-typed
 * field on the button (found by type, not name, so a future Waystones build that moves it just shows
 * nothing rather than breaking).
 */
public final class WaystoneFeeOverlay {

    /** The coin item, drawn as a sprite (like the shop screens) so it tracks custom currency art. */
    private static final ItemStack COIN =
            new ItemStack(net.fugginbeenus.notchcurrency.registry.ModItems.NOTCH_COIN);

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

        drawFeeBadge(screen, ctx, mc.textRenderer, mouseX, mouseY, fee, crossDimension);
    }

    /** A little floating panel by the cursor: a "Teleport Fee" label over the coin sprite and price. */
    private static void drawFeeBadge(Screen screen, DrawContext ctx, TextRenderer tr,
                                     int mouseX, int mouseY, int fee, boolean crossDimension) {
        String label = crossDimension ? "Teleport Fee (other dimension)" : "Teleport Fee";
        String amount = String.valueOf(fee);

        int coin = 16;
        int gap = 3;
        int pad = 5;
        int line = 10;
        int content = Math.max(tr.getWidth(label), coin + gap + tr.getWidth(amount));
        int boxW = content + pad * 2;
        int boxH = line + gap + coin + pad * 2;

        int x = Math.max(2, Math.min(mouseX + 12, screen.width - boxW - 2));
        int y = Math.max(2, Math.min(mouseY - 12, screen.height - boxH - 2));

        ctx.getMatrices().push();
        ctx.getMatrices().translate(0f, 0f, 400f); // above the menu, like a vanilla tooltip
        ctx.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, 0xFF000000);
        ctx.fill(x, y, x + boxW, y + boxH, 0xF01B1B22);
        ctx.drawText(tr, label, x + pad, y + pad, 0xFFB8B8B8, true);
        int rowY = y + pad + line + gap;
        ctx.drawItem(COIN, x + pad, rowY);
        ctx.drawText(tr, amount, x + pad + coin + gap, rowY + (coin - 8) / 2, NotchTheme.TEXT_GOLD, true);
        ctx.getMatrices().pop();
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
