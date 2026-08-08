package net.fugginbeenus.notchcurrency.config.ui;

import net.fugginbeenus.notchcurrency.auction.AuctionConfig;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.fugginbeenus.notchcurrency.config.ui.ConfigEntry.BoolEntry;
import net.fugginbeenus.notchcurrency.config.ui.ConfigEntry.NumberEntry;
import net.fugginbeenus.notchcurrency.config.ui.ConfigEntry.SelectEntry;
import net.fugginbeenus.notchcurrency.config.ui.ConfigEntry.SliderEntry;
import net.fugginbeenus.notchcurrency.config.ui.ConfigEntry.StringEntry;
import net.fugginbeenus.notchcurrency.crate.DailyCrateManager;
import net.fugginbeenus.notchcurrency.crate.GoldenCacheManager;
import net.fugginbeenus.notchcurrency.economy.ShopRent;
import net.fugginbeenus.notchcurrency.economy.WealthTax;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyManager;
import net.fugginbeenus.notchcurrency.economy.raffle.RaffleManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class NotchConfigScreen extends Screen {

    private static final int ROW_H = 17, HEADER_H = 18;
    private static final Set<String> COLLAPSED = new HashSet<>();

    private final Screen parent;
    private final NotchConfig cfg;
    private final List<ConfigEntry> entries;

    private int px, py, pw, ph;        // panel rect
    private int listTop, listBottom;   // viewport
    private double scroll;
    private boolean draggingBar;
    private SliderEntry draggingSlider;

    private EditBox search;
    private EditBox editField;
    private ConfigEntry editing;
    private int editingY;

    private record Row(String header, ConfigEntry entry, int y) {}
    private final List<Row> rows = new ArrayList<>();
    private int contentH;

    public static Screen create(Screen parent) {
        return new NotchConfigScreen(parent);
    }

    private NotchConfigScreen(Screen parent) {
        super(Component.literal("Notch Currency"));
        this.parent = parent;
        this.cfg = NotchConfigIO.get();
        this.entries = ConfigEntries.build(cfg);
    }

    @Override
    protected void init() {
        pw = Math.min(400, this.width - 8);
        ph = this.height - 16;
        px = (this.width - pw) / 2;
        py = 8;
        listTop = py + 42;
        listBottom = py + ph - 28;

        String keep = search == null ? "" : search.getValue();
        search = new EditBox(this.font, px + 13, py + 21, pw - 26, 14, Component.literal("search"));
        search.setMaxLength(64);
        search.setValue(keep);
        search.setResponder(s -> scroll = 0);
        addWidget(search);
        closeEdit(false);
    }

    /* layout */

    private void buildRows() {
        rows.clear();
        String query = search.getValue().trim().toLowerCase();
        boolean searching = !query.isEmpty();
        int y = 0;
        String open = null;
        for (ConfigEntry e : entries) {
            if (searching && !e.matches(query)) continue;
            if (!e.category.equals(open)) {
                open = e.category;
                rows.add(new Row(open, null, y));
                y += HEADER_H;
            }
            if (!searching && COLLAPSED.contains(e.category)) continue;
            rows.add(new Row(null, e, y));
            y += ROW_H;
        }
        contentH = y;
    }

    private double maxScroll() {
        return Math.max(0, contentH - (listBottom - listTop));
    }

    /* render */

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        //? if >=1.21 {
        /*renderBackground(ctx, mouseX, mouseY, delta);
        *///?} else {
        //? if >=1.21 {
        /*renderTransparentBackground(ctx);
        *///?} else {
        renderBackground(ctx);
        //?}
        //?}
        buildRows();
        scroll = Math.max(0, Math.min(scroll, maxScroll()));

        NotchWidgets.panel(ctx, px, py, pw, ph);
        NotchWidgets.title(ctx, this.font, "Notch Currency", px + pw / 2, py + 7);
        NotchWidgets.inset(ctx, px + 11, py + 19, pw - 22, 18, NotchTheme.PANEL_MID);

        ConfigEntry hoveredEntry = null;
        boolean hoveredReset = false;

        // Scrolling list, scissored to the viewport.
        ctx.enableScissor(px + 1, listTop, px + pw - 1, listBottom);
        for (Row row : rows) {
            int ry = listTop + row.y() - (int) scroll;
            if (ry + HEADER_H < listTop || ry > listBottom) continue;

            if (row.header() != null) {
                boolean folded = COLLAPSED.contains(row.header());
                NotchWidgets.triangle(ctx, px + 19, ry + 7, folded, NotchTheme.TEXT_MUTED);
                ctx.drawString(this.font, row.header(), px + 28, ry + 5, NotchTheme.TEXT_DARK, false);
                int tw = this.font.width(row.header());
                NotchWidgets.divider(ctx, px + 34 + tw, ry + 8, pw - 56 - tw);
            } else {
                ConfigEntry e = row.entry();
                boolean rowHover = mouseY >= ry && mouseY < ry + ROW_H
                        && mouseX >= px + 8 && mouseX < px + pw - 18
                        && mouseY >= listTop && mouseY < listBottom;
                if (rowHover) {
                    ctx.fill(px + 8, ry, px + pw - 18, ry + ROW_H, 0x18000000);
                    hoveredEntry = e;
                }
                ctx.drawString(this.font,
                        this.font.plainSubstrByWidth(e.label, labelW()), px + 16, ry + 4,
                        NotchTheme.TEXT_DARK, false);
                if (!e.isDefault()) {
                    boolean rh = rowHover && mouseX >= resetX() && mouseX < resetX() + 13;
                    NotchWidgets.neutralButton(ctx, this.font, resetX(), ry + 2, 13, 13, "", rh);
                    leftTriangle(ctx, resetX() + 6, ry + 8, rh ? NotchTheme.TEXT_DARK : NotchTheme.TEXT_MUTED);
                    if (rh) hoveredReset = true;
                }
                drawControl(ctx, e, ry, mouseX, mouseY, rowHover);
            }
        }
        ctx.disableScissor();

        // Scrollbar.
        if (maxScroll() > 0) {
            int trackX = px + pw - 14, trackW = 6;
            int trackH = listBottom - listTop;
            NotchWidgets.inset(ctx, trackX, listTop, trackW, trackH, NotchTheme.PANEL_MID);
            int thumbH = Math.max(16, (int) ((long) trackH * trackH / contentH));
            int thumbY = listTop + (int) ((trackH - thumbH) * (scroll / maxScroll()));
            ctx.fill(trackX + 1, thumbY, trackX + trackW - 1, thumbY + thumbH, NotchTheme.EDGE);
        }

        // Footer.
        NotchWidgets.divider(ctx, px + 10, listBottom + 3, pw - 20);
        ctx.drawString(this.font, "config/notchcurrency.json", px + 12, py + ph - 18, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.neutralButton(ctx, this.font, cancelX(), footerY(), 64, 16, "Cancel",
                hit(mouseX, mouseY, cancelX(), footerY(), 64, 16));
        NotchWidgets.primaryButton(ctx, this.font, saveX(), footerY(), 104, 16, "Save & Apply",
                hit(mouseX, mouseY, saveX(), footerY(), 104, 16));

        super.render(ctx, mouseX, mouseY, delta);
        if (search.getValue().isEmpty() && !search.isFocused()) {
            ctx.drawString(this.font, "Search settings…", px + 18, py + 24, NotchTheme.TEXT_MUTED, false);
        }
        if (editField != null) editField.render(ctx, mouseX, mouseY, delta);

        // Tooltip last, above everything.
        if (hoveredEntry != null && editing == null) {
            if (hoveredReset) {
                ctx.renderTooltip(this.font, Component.literal("Reset to default"), mouseX, mouseY);
            } else if (hoveredEntry.tooltip.length > 0 || hoveredEntry instanceof NumberEntry) {
                List<Component> lines = new ArrayList<>();
                for (String s : hoveredEntry.tooltip) lines.add(Component.literal(s));
                if (hoveredEntry instanceof NumberEntry n) {
                    lines.add(Component.literal("Range: " + n.min + " – " + n.max)
                            .withStyle(st -> st.withColor(0xFF808080)));
                }
                ctx.renderComponentTooltip(this.font, lines, mouseX, mouseY);
            }
        }
    }

    private void drawControl(GuiGraphics ctx, ConfigEntry e, int ry, int mouseX, int mouseY, boolean rowHover) {
        int right = px + pw - 20;
        if (e == editing) return; // the edit field renders on top instead

        if (e instanceof BoolEntry b) {
            int bx = right - 40;
            boolean hov = rowHover && mouseX >= bx;
            if (b.value) {
                ctx.fill(bx, ry + 3, bx + 40, ry + 14, hov ? 0xFF6FB25D : NotchTheme.ACCENT_GREEN);
                ctx.drawString(this.font, "ON", bx + 12, ry + 5, NotchTheme.TEXT_LIGHT, true);
            } else {
                ctx.fill(bx, ry + 3, bx + 40, ry + 14, hov ? 0xFF8A8A8A : NotchTheme.PANEL_MID);
                ctx.drawString(this.font, "OFF", bx + 10, ry + 5, NotchTheme.TEXT_DARK, false);
            }
        } else if (e instanceof SliderEntry s) {
            String val = s.value + s.suffix;
            ctx.drawString(this.font, val, right - this.font.width(val), ry + 5,
                    NotchTheme.TEXT_DARK, false);
            int tx = sliderX(), tw = sliderW();
            NotchWidgets.inset(ctx, tx, ry + 6, tw, 6, NotchTheme.PANEL_MID);
            int thumb = tx + 1 + (int) ((tw - 8) * s.fraction());
            ctx.fill(thumb, ry + 4, thumb + 6, ry + 14,
                    draggingSlider == s ? NotchTheme.ACCENT_GREEN : NotchTheme.EDGE);
        } else if (e instanceof SelectEntry sel) {
            int bx = right - 110;
            NotchWidgets.neutralButton(ctx, this.font, bx, ry + 2, 110, 14, sel.value(),
                    rowHover && mouseX >= bx);
        } else {
            String val = e instanceof NumberEntry n ? Long.toString(n.value)
                    : ((StringEntry) e).value.isEmpty() ? "(blank)" : ((StringEntry) e).value;
            val = this.font.plainSubstrByWidth(val, 110);
            int vw = this.font.width(val);
            boolean hov = rowHover && mouseX >= right - 116;
            ctx.drawString(this.font, val, right - vw, ry + 4,
                    e instanceof StringEntry && ((StringEntry) e).value.isEmpty()
                            ? NotchTheme.TEXT_MUTED : NotchTheme.TEXT_DARK, false);
            if (hov) ctx.fill(right - vw, ry + 13, right, ry + 14, NotchTheme.TEXT_MUTED);
        }
    }

    private static void leftTriangle(GuiGraphics ctx, int cx, int cy, int color) {
        for (int c = 0; c < 4; c++) {
            int half = 3 - c;
            ctx.fill(cx - 3 + c, cy - half, cx - 2 + c, cy + half + 1, color);
        }
    }

    private int labelW() { return pw - 200; }
    private int resetX() { return px + pw - 160; }
    private int sliderX() { return px + pw - 142; }
    private int sliderW() { return 88; }
    private int footerY() { return py + ph - 22; }
    private int saveX() { return px + pw - 114; }
    private int cancelX() { return px + pw - 184; }

    private static boolean hit(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /* input */

    //? if >=1.21.11 {
    /*@Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
    *///?} else {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
    //?}
        //? if >=1.21.11 {
        /*if (editField != null && editField.mouseClicked(event, doubleClick)) return true;
        *///?} else {
        if (editField != null && editField.mouseClicked(mouseX, mouseY, button)) return true;
        //?}
        if (editing != null) closeEdit(true);

        if (hit((int) mouseX, (int) mouseY, saveX(), footerY(), 104, 16)) {
            NotchWidgets.click();
            saveAndApply();
            return true;
        }
        if (hit((int) mouseX, (int) mouseY, cancelX(), footerY(), 64, 16)) {
            NotchWidgets.click();
            this.onClose();
            return true;
        }

        // Scrollbar jump/drag.
        if (maxScroll() > 0 && mouseX >= px + pw - 16 && mouseX <= px + pw - 6
                && mouseY >= listTop && mouseY <= listBottom) {
            draggingBar = true;
            scrollBarTo(mouseY);
            return true;
        }

        if (mouseY >= listTop && mouseY < listBottom) {
            for (Row row : rows) {
                int ry = listTop + row.y() - (int) scroll;
                if (row.header() != null) {
                    if (mouseY >= ry && mouseY < ry + HEADER_H && mouseX >= px + 8 && mouseX < px + pw - 18
                            && search.getValue().trim().isEmpty()) {
                        if (!COLLAPSED.remove(row.header())) COLLAPSED.add(row.header());
                        NotchWidgets.tick();
                        return true;
                    }
                    continue;
                }
                if (mouseY < ry || mouseY >= ry + ROW_H) continue;
                if (clickRow(row.entry(), ry, mouseX, mouseY, button)) return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private boolean clickRow(ConfigEntry e, int ry, double mouseX, double mouseY, int button) {
        int right = px + pw - 20;
        if (!e.isDefault() && hit((int) mouseX, (int) mouseY, resetX(), ry + 2, 13, 13)) {
            e.reset();
            NotchWidgets.tick();
            return true;
        }
        if (e instanceof BoolEntry b && mouseX >= right - 40 && mouseX < right) {
            b.value = !b.value;
            NotchWidgets.tick();
            return true;
        }
        if (e instanceof SliderEntry s && mouseX >= sliderX() - 2 && mouseX < sliderX() + sliderW() + 2) {
            draggingSlider = s;
            s.setFromFraction((mouseX - sliderX()) / (double) (sliderW() - 6));
            return true;
        }
        if (e instanceof SelectEntry sel && mouseX >= right - 110 && mouseX < right) {
            sel.cycle(button == 1 || net.fugginbeenus.notchcurrency.compat.Render.shiftDown() ? -1 : 1);
            NotchWidgets.tick();
            return true;
        }
        if ((e instanceof NumberEntry || e instanceof StringEntry) && mouseX >= right - 116 && mouseX < right) {
            openEdit(e, ry);
            return true;
        }
        return false;
    }

    private void openEdit(ConfigEntry e, int ry) {
        editing = e;
        editingY = ry;
        int w = 116;
        editField = new EditBox(this.font, px + pw - 20 - w, ry + 2, w, 13, Component.literal("value"));
        if (e instanceof NumberEntry n) {
            editField.setMaxLength(14);
            editField.setFilter(s -> s.matches("-?[0-9]*"));
            editField.setValue(Long.toString(n.value));
        } else {
            editField.setMaxLength(((StringEntry) e).maxLength);
            editField.setValue(((StringEntry) e).value);
        }
        editField.setFocused(true);
        setFocused(editField);
        NotchWidgets.tick();
    }

    private void closeEdit(boolean commit) {
        if (editing == null) return;
        if (commit) {
            if (editing instanceof NumberEntry n) {
                try {
                    n.set(Long.parseLong(editField.getValue().trim()));
                } catch (NumberFormatException ignored) { /* keep the old value */ }
            } else if (editing instanceof StringEntry s) {
                s.value = editField.getValue().trim();
            }
        }
        editing = null;
        editField = null;
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
    *///?} else {
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
    //?}
        if (draggingBar) {
            scrollBarTo(mouseY);
            return true;
        }
        if (draggingSlider != null) {
            draggingSlider.setFromFraction((mouseX - sliderX()) / (double) (sliderW() - 6));
            return true;
        }
        //? if >=1.21.11 {
        /*return super.mouseDragged(event, dx, dy);
        *///?} else {
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
        //?}
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
    *///?} else {
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
    //?}
        if (draggingSlider != null) {
            draggingSlider = null;
            NotchWidgets.tick();
        }
        draggingBar = false;
        //? if >=1.21.11 {
        /*return super.mouseReleased(event);
        *///?} else {
        return super.mouseReleased(mouseX, mouseY, button);
        //?}
    }

    @Override
    //? if >=1.21 {
    /*public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
    *///?} else {
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    //?}
        if (editing != null) closeEdit(true);
        scroll = Math.max(0, Math.min(scroll - amount * 22, maxScroll()));
        return true;
    }

    private void scrollBarTo(double mouseY) {
        double f = (mouseY - listTop - 8) / (double) (listBottom - listTop - 16);
        scroll = Math.max(0, Math.min(f, 1)) * maxScroll();
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
    *///?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
        if (editing != null) {
            if (keyCode == 257 || keyCode == 335) { // enter
                closeEdit(true);
                NotchWidgets.tick();
                return true;
            }
            if (keyCode == 256) { // esc cancels just the edit
                closeEdit(false);
                return true;
            }
            if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, editField)) return true;
        }
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, search)) return true;
        //? if >=1.21.11 {
        /*return super.keyPressed(event);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        char chr = (char) event.codepoint();
        int modifiers = event.modifiers();
    *///?} else {
    @Override
    public boolean charTyped(char chr, int modifiers) {
    //?}
        //? if >=1.21.11 {
        /*if (editField != null && editField.charTyped(event)) return true;
        *///?} else {
        if (editField != null && editField.charTyped(chr, modifiers)) return true;
        //?}
        //? if >=1.21.11 {
        /*return super.charTyped(event);
        *///?} else {
        return super.charTyped(chr, modifiers);
        //?}
    }

    @Override
    public void tick() {
        //? if <1.21 {
        search.tick();
        if (editField != null) editField.tick();
        //?}
    }

    /* saving */

    private void saveAndApply() {
        closeEdit(true);
        for (ConfigEntry e : entries) e.commit();
        // Keep min/max pairs ordered so a typo can't invert a range.
        if (cfg.gambling.maxBet < cfg.gambling.minBet) cfg.gambling.maxBet = cfg.gambling.minBet;
        if (cfg.cache.currencyStacksMax < cfg.cache.currencyStacksMin) cfg.cache.currencyStacksMax = cfg.cache.currencyStacksMin;
        if (cfg.cache.currencyPerStackMax < cfg.cache.currencyPerStackMin) cfg.cache.currencyPerStackMax = cfg.cache.currencyPerStackMin;
        if (cfg.balloon.maxY < cfg.balloon.minY) cfg.balloon.maxY = cfg.balloon.minY;

        NotchConfigIO.save(cfg);
        AuctionConfig.apply(cfg);
        DailyCrateManager.applyConfig(cfg);
        GoldenCacheManager.applyConfig(cfg);
        WealthTax.applyConfig(cfg);
        ShopRent.applyConfig(cfg);
        RaffleManager.applyConfig(cfg);
        BountyManager.applyConfig(cfg);
        net.fugginbeenus.notchcurrency.economy.crate.CrateManager.applyConfig(cfg);
        net.fugginbeenus.notchcurrency.economy.loan.LoanManager.applyConfig(cfg);
        net.fugginbeenus.notchcurrency.economy.gambling.GamblingManager.applyConfig(cfg);
        net.fugginbeenus.notchcurrency.economy.enchanter.EnchanterManager.applyConfig(cfg);
        net.fugginbeenus.notchcurrency.economy.cosmetic.CosmeticManager.applyConfig(cfg);
        net.fugginbeenus.notchcurrency.integration.WaystoneFeeHandler.applyConfig(cfg);
        net.fugginbeenus.notchcurrency.economy.villager.VillagerCoinTrades.applyConfig(cfg);
        // Rebuild the custom-currency pack so a name change takes effect right away.
        net.fugginbeenus.notchcurrency.client.CurrencyPackGenerator.generate();
        this.onClose();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) this.minecraft.setScreen(parent);
    }

    //? if >=1.21 {
    /*@Override
    protected void renderBlurredBackground(float delta) {
        // No 1.21 menu blur behind the mod's screens. They draw crisp panels over the world.
    }
    *///?}

    //? if >=1.21 {
    /*@Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        // Drawn manually at the top of render(). This screen paints its panel after the darkening,
        // but the 1.21 base render would darken over the finished panel (super.render comes last here).
    }
    *///?}
}
