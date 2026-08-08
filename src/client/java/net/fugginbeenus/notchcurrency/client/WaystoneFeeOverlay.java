package net.fugginbeenus.notchcurrency.client;

import net.blay09.mods.waystones.client.gui.screen.WaystoneSelectionScreenBase;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;

public final class WaystoneFeeOverlay {

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

    private static void drawFeeTooltip(Screen screen, GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        if (!WaystoneFees.enabled()) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Object waystone = hoveredWaystone(screen, mouseX, mouseY);
        if (waystone == null) return;

        ResourceKey<Level> destination = dimensionOf(waystone);
        if (destination == null) return;

        boolean crossDimension = !mc.level.dimension().equals(destination);
        int fee = crossDimension ? WaystoneFees.dimensionalFee() : WaystoneFees.fee();
        if (fee <= 0) return;

        drawFeeBadge(screen, ctx, mc.font, mouseX, mouseY, fee, crossDimension);
    }

    private static void drawFeeBadge(Screen screen, GuiGraphics ctx, Font tr,
                                     int mouseX, int mouseY, int fee, boolean crossDimension) {
        String label = crossDimension ? "Teleport Fee (other dimension)" : "Teleport Fee";
        String amount = String.valueOf(fee);

        int coin = 16;
        int gap = 3;
        int pad = 5;
        int line = 10;
        int content = Math.max(tr.width(label), coin + gap + tr.width(amount));
        int boxW = content + pad * 2;
        int boxH = line + gap + coin + pad * 2;

        int x = Math.max(2, Math.min(mouseX + 12, screen.width - boxW - 2));
        int y = Math.max(2, Math.min(mouseY - 12, screen.height - boxH - 2));

        net.fugginbeenus.notchcurrency.compat.Render.pushGui(ctx); // drawn last, so it sits above the menu like a vanilla tooltip
        ctx.fill(x - 1, y - 1, x + boxW + 1, y + boxH + 1, 0xFF000000);
        ctx.fill(x, y, x + boxW, y + boxH, 0xF01B1B22);
        ctx.drawString(tr, label, x + pad, y + pad, 0xFFB8B8B8, true);
        int rowY = y + pad + line + gap;
        ctx.renderItem(COIN, x + pad, rowY);
        ctx.drawString(tr, amount, x + pad + coin + gap, rowY + (coin - 8) / 2, NotchTheme.TEXT_GOLD, true);
        net.fugginbeenus.notchcurrency.compat.Render.popGui(ctx);
    }

    @Nullable
    private static Object hoveredWaystone(Screen screen, int mouseX, int mouseY) {
        //? if >=1.21.11 {
        /*// Back to top-level buttons, as on 1.20.1: the list widgets are gone again.
        for (AbstractWidget widget : Screens.getButtons(screen)) {
            if (widget instanceof net.blay09.mods.waystones.client.gui.widget.WaystoneButton button
                    && button.visible && button.isMouseOver(mouseX, mouseY)) {
                return readWaystone(button);
            }
        }
        return null;
        *///?} elif >=1.21 {
        /*// 1.21: destinations are rows in a scrolling list; each entry's own isMouseOver is scroll-correct.
        for (net.minecraft.client.gui.components.events.GuiEventListener element : screen.children()) {
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
        for (AbstractWidget widget : Screens.getButtons(screen)) {
            if (widget instanceof net.blay09.mods.waystones.client.gui.widget.WaystoneButton button
                    && button.visible && button.isMouseOver(mouseX, mouseY)) {
                return readWaystone(button);
            }
        }
        return null;
        //?}
    }

    //? if >=1.21 && <1.21.11 {
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
    private static ResourceKey<Level> dimensionOf(Object waystone) {
        //? if >=1.21 {
        /*if (waystone instanceof net.blay09.mods.waystones.api.Waystone ws) return ws.getDimension();
        *///?} else {
        if (waystone instanceof net.blay09.mods.waystones.api.IWaystone ws) return ws.getDimension();
        //?}
        return null;
    }
}
