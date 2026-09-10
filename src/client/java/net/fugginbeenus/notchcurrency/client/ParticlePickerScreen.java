package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.particle.ParticleCatalog;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ParticlePickerScreen extends Screen {

    private static final int W = 340, H = 232;
    private static final int LIST_Y = 60, ROW_H = 17, ROWS = 8;

    private final String current;
    private final java.util.function.Consumer<String> onPick;
    private final Runnable onBack;
    private final List<ParticleCatalog.Entry> all = ParticleCatalog.all();

    private int px, py, scroll, group;
    private EditBox search;
    private String lastQuery = "";
    private List<ParticleCatalog.Entry> shown = new ArrayList<>();
    private String hoveredId;

    public ParticlePickerScreen(String current, java.util.function.Consumer<String> onPick, Runnable onBack) {
        super(Component.literal("Pick a particle"));
        this.current = current == null ? "" : current;
        this.onPick = onPick;
        this.onBack = onBack;
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
        String old = search == null ? "" : search.getValue();
        search = new EditBox(this.font, px + 14, py + 43, 200, 10, Component.empty());
        search.setMaxLength(40);
        search.setBordered(false);
        search.setHint(Component.literal("search by name").withStyle(ChatFormatting.DARK_GRAY));
        search.setValue(old);
        addRenderableWidget(search);
        refilter();
    }

    private void refilter() {
        String q = search == null ? "" : search.getValue().trim().toLowerCase();
        lastQuery = q;
        shown = new ArrayList<>();
        String want = ParticleCatalog.GROUPS[group];
        for (ParticleCatalog.Entry e : all) {
            if (!want.equals("All") && !e.group().equals(want)) continue;
            if (!q.isEmpty() && !e.name().toLowerCase().contains(q) && !e.id().contains(q)) continue;
            shown.add(e);
        }
        scroll = Math.max(0, Math.min(scroll, Math.max(0, shown.size() - ROWS)));
    }

    private boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int groupX(int i) { return px + 10 + i * 40; }

    private String fit(String text, int room) {
        if (this.font.width(text) <= room) return text;
        return this.font.plainSubstrByWidth(text, room - this.font.width("...")) + "...";
    }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    //?}
        if (!search.getValue().trim().toLowerCase().equals(lastQuery)) refilter();
        hoveredId = null;

        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.font, "Pick a particle", px + W / 2, py + 7);

        for (int i = 0; i < ParticleCatalog.GROUPS.length; i++) {
            boolean hover = over(mouseX, mouseY, groupX(i), py + 20, 38, 13);
            if (i == group) NotchWidgets.primaryButton(ctx, this.font, groupX(i), py + 20, 38, 13, ParticleCatalog.GROUPS[i], hover);
            else NotchWidgets.neutralButton(ctx, this.font, groupX(i), py + 20, 38, 13, ParticleCatalog.GROUPS[i], hover);
        }

        NotchWidgets.inset(ctx, px + 10, py + 39, 208, 14, NotchTheme.DEEP);
        NotchWidgets.centerText(ctx, this.font, shown.size() + " found", px + 280, py + 43,
                NotchTheme.TEXT_MUTED, false);

        int end = Math.min(shown.size(), scroll + ROWS);
        for (int i = scroll; i < end; i++) {
            ParticleCatalog.Entry e = shown.get(i);
            int ry = py + LIST_Y + (i - scroll) * ROW_H;
            boolean isCurrent = e.id().equals(current);
            boolean hover = over(mouseX, mouseY, px + 10, ry, W - 20, ROW_H - 2);
            if (isCurrent) NotchWidgets.primaryButton(ctx, this.font, px + 10, ry, W - 20, ROW_H - 2, "", hover);
            else NotchWidgets.neutralButton(ctx, this.font, px + 10, ry, W - 20, ROW_H - 2, "", hover);
            ctx.drawString(this.font, fit(e.name(), 150), px + 16, ry + 4, NotchTheme.TEXT_DARK, false);
            String id = e.id().startsWith("minecraft:") ? e.id().substring(10) : e.id();
            ctx.drawString(this.font, fit(id, 120), px + 172, ry + 4, NotchTheme.TEXT_MUTED, false);
            if (e.wanders()) {
                ctx.drawString(this.font, "wanders", px + W - 54, ry + 4, NotchTheme.TEXT_RED, false);
            }
            if (hover) hoveredId = e.id();
        }
        if (shown.isEmpty()) {
            NotchWidgets.centerText(ctx, this.font, "Nothing matches.", px + W / 2, py + 110,
                    NotchTheme.TEXT_MUTED, false);
        }

        if (shown.size() > ROWS) {
            int barTop = py + LIST_Y, barH = ROWS * ROW_H - 2;
            int thumb = Math.max(10, barH * ROWS / shown.size());
            int at = barTop + (barH - thumb) * scroll / Math.max(1, shown.size() - ROWS);
            ctx.fill(px + W - 8, barTop, px + W - 5, barTop + barH, 0x40000000);
            ctx.fill(px + W - 8, at, px + W - 5, at + thumb, 0xFF6FC274);
        }

        NotchWidgets.centerText(ctx, this.font, "Scroll to browse. Click one to use it.",
                px + W / 2, py + H - 32, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.neutralButton(ctx, this.font, px + W / 2 - 50, py + H - 22, 100, 16, "Back",
                over(mouseX, mouseY, px + W / 2 - 50, py + H - 22, 100, 16));

        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
        if (hoveredId != null && net.fugginbeenus.notchcurrency.client.particle.ParticleThumbs.has(hoveredId)) {
            int size = 40, pad = 6;
            int bx = mouseX + 12, by = mouseY - size / 2 - pad;
            if (bx + size + pad * 2 > this.width) bx = mouseX - size - pad * 2 - 12;
            if (by < 2) by = 2;
            net.fugginbeenus.notchcurrency.compat.Render.pushGuiOverlay(ctx);
            ctx.fill(bx, by, bx + size + pad * 2, by + size + pad * 2, 0xF0100C18);
            ctx.fill(bx + 1, by + 1, bx + size + pad * 2 - 1, by + size + pad * 2 - 1, 0xFF2A2438);
            ctx.fill(bx + pad, by + pad, bx + pad + size, by + pad + size, 0xFF0B0B10);
            net.fugginbeenus.notchcurrency.client.particle.ParticleThumbs.draw(ctx, hoveredId,
                    bx + pad, by + pad, size);
            net.fugginbeenus.notchcurrency.compat.Render.popGuiOverlay(ctx);
        }
    }

    @Override
    //? if >=1.21 {
    /*public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
    *///?} else {
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    //?}
        int max = Math.max(0, shown.size() - ROWS);
        scroll = Math.max(0, Math.min(max, scroll - (int) Math.signum(amount)));
        return true;
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
    *///?} else {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
    //?}
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            for (int i = 0; i < ParticleCatalog.GROUPS.length; i++) {
                if (over(mx, my, groupX(i), py + 20, 38, 13)) {
                    NotchWidgets.click();
                    group = i;
                    scroll = 0;
                    refilter();
                    return true;
                }
            }
            int end = Math.min(shown.size(), scroll + ROWS);
            for (int i = scroll; i < end; i++) {
                int ry = py + LIST_Y + (i - scroll) * ROW_H;
                if (over(mx, my, px + 10, ry, W - 20, ROW_H - 2)) {
                    NotchWidgets.click();
                    if (onPick != null) onPick.accept(shown.get(i).id());
                    return true;
                }
            }
            if (over(mx, my, px + W / 2 - 50, py + H - 22, 100, 16)) {
                NotchWidgets.click();
                if (onBack != null) onBack.run(); else this.onClose();
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    @Override
    public boolean isPauseScreen() { return false; }

    //? if >=1.21.11 {
    /*@Override
    protected void renderBlurredBackground(net.minecraft.client.gui.GuiGraphics ctx) {
    }
    *///?} elif >=1.21 {
    /*@Override
    protected void renderBlurredBackground(float delta) {
    }
    *///?}

    //? if >=1.21 {
    /*@Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    }
    *///?}
}
