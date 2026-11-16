package net.fugginbeenus.notchcurrency.client.npcsound;

import com.mojang.blaze3d.platform.InputConstants;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public class SoundBrowserScreen extends Screen {

    private static final int W = 340, H = 236;
    private static final int CHIP_Y = 34, CHIP_W = 78, CHIP_H = 14, CHIPS_PER_LINE = 4;
    private static final int CAT_X = 8, CAT_Y = 74, CAT_W = 90, CAT_H = 15, CAT_STEP = 17;
    private static final int LIST_X = 104, LIST_W = 228, SEARCH_Y = 74, ROWS_Y = 92, ROW_H = 14, ROWS = 8;
    private static final int NAME_W = 130;

    private final String slot;
    private final List<String> suggested;
    private final List<String> chosen;
    private final float pitch;
    private final Consumer<String> onDone;
    private final Runnable onCancel;

    private List<SoundCatalog.Entry> catalog = List.of();
    private List<SoundCatalog.Entry> shown = new ArrayList<>();
    private int category = SoundCatalog.SUGGESTED;
    private int scroll;
    private String lastSearch;
    private int lastCategory = -1;
    private EditBox search;
    private int px, py;

    private static SimpleSoundInstance playing;

    public SoundBrowserScreen(String slot, List<String> suggested, String current, int pitchPercent,
                              Consumer<String> onDone, Runnable onCancel) {
        super(Component.literal("Choose sounds"));
        this.slot = slot;
        this.suggested = suggested;
        this.chosen = new ArrayList<>(NotchNpcEntity.soundIds(current));
        this.pitch = pitchPercent / 100.0f;
        this.onDone = onDone;
        this.onCancel = onCancel;
    }

    public static void play(String id, float pitch, boolean stopLast) {
        ResourceLocation location = ResourceLocation.tryParse(SoundCatalog.full(id));
        if (location == null) return;
        var manager = Minecraft.getInstance().getSoundManager();
        if (stopLast && playing != null) manager.stop(playing);
        playing = SimpleSoundInstance.forUI(SoundEvent.createVariableRangeEvent(location), pitch, 1.0f);
        manager.play(playing);
    }

    private static void stopPlaying() {
        if (playing != null) Minecraft.getInstance().getSoundManager().stop(playing);
        playing = null;
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
        if (catalog.isEmpty()) catalog = SoundCatalog.all();
        String old = search == null ? "" : search.getValue();
        search = new EditBox(this.font, px + LIST_X + 4, py + SEARCH_Y + 3, LIST_W - 8, 10, Component.empty());
        search.setMaxLength(64);
        search.setBordered(false);
        search.setHint(Component.literal("Search names or ids").withStyle(ChatFormatting.DARK_GRAY));
        search.setValue(old);
        addRenderableWidget(search);
        lastSearch = null;
    }

    private boolean over(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int chipX(int i) { return px + 8 + (i % CHIPS_PER_LINE) * (CHIP_W + 4); }
    private int chipY(int i) { return py + CHIP_Y + (i / CHIPS_PER_LINE) * (CHIP_H + 2); }
    private int rowY(int r) { return py + ROWS_Y + r * ROW_H; }
    private int addX() { return px + LIST_X + LIST_W - 14; }

    private void refresh() {
        String q = search.getValue().trim().toLowerCase(Locale.ROOT);
        if (q.equals(lastSearch) && category == lastCategory) return;
        lastSearch = q;
        lastCategory = category;
        scroll = 0;
        shown = new ArrayList<>();
        if (!q.isEmpty()) {
            for (SoundCatalog.Entry e : catalog) {
                if (e.name().toLowerCase(Locale.ROOT).contains(q) || e.id().contains(q)) shown.add(e);
            }
        } else if (category == SoundCatalog.SUGGESTED) {
            for (String id : suggested) shown.add(entryFor(id));
        } else {
            for (SoundCatalog.Entry e : catalog) if (e.category() == category) shown.add(e);
        }
    }

    private SoundCatalog.Entry entryFor(String id) {
        String full = SoundCatalog.full(id);
        for (SoundCatalog.Entry e : catalog) if (e.id().equals(full)) return e;
        return new SoundCatalog.Entry(full, SoundCatalog.nameOf(full), SoundCatalog.OTHER);
    }

    private boolean inBox(String id) {
        String full = SoundCatalog.full(id);
        for (String c : chosen) if (SoundCatalog.full(c).equals(full)) return true;
        return false;
    }

    private static void playIcon(GuiGraphics ctx, int x, int y, int colour) {
        for (int r = 0; r < 7; r++) {
            int w = r <= 3 ? r + 1 : 7 - r;
            ctx.fill(x, y + r, x + w, y + r + 1, colour);
        }
    }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    //?}
        refresh();
        List<Component> tip = null;
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.font, "Choose sounds", px + W / 2, py + 8, NotchWidgets.Plate.STEEL);
        NotchWidgets.centerText(ctx, this.font, slot + ": press + to add a sound, x to take one out.",
                px + W / 2, py + 22, NotchTheme.TEXT_MUTED, false);

        if (chosen.isEmpty()) {
            NotchWidgets.centerText(ctx, this.font, "No sounds in this box yet.", px + W / 2, py + CHIP_Y + 10,
                    NotchTheme.TEXT_MUTED, false);
        }
        for (int i = 0; i < chosen.size(); i++) {
            int cx = chipX(i), cy = chipY(i);
            boolean hover = over(mouseX, mouseY, cx, cy, CHIP_W, CHIP_H);
            boolean xHover = over(mouseX, mouseY, cx + CHIP_W - 11, cy, 11, CHIP_H);
            NotchWidgets.inset(ctx, cx, cy, CHIP_W, CHIP_H, hover && !xHover ? NotchTheme.SLATE : NotchTheme.DEEP);
            String name = SoundCatalog.nameOf(chosen.get(i));
            ctx.drawString(this.font, this.font.plainSubstrByWidth(name, CHIP_W - 15), cx + 3, cy + 3,
                    NotchTheme.TEXT_LIGHT, false);
            ctx.drawString(this.font, "x", cx + CHIP_W - 8, cy + 3,
                    xHover ? NotchTheme.TEXT_RED : NotchTheme.SLATE_TEXT, false);
            if (xHover) {
                tip = List.of(Component.literal("Take it out").withStyle(ChatFormatting.WHITE));
            } else if (hover) {
                tip = List.of(Component.literal(name).withStyle(ChatFormatting.WHITE),
                        Component.literal(SoundCatalog.shortId(SoundCatalog.full(chosen.get(i))))
                                .withStyle(ChatFormatting.GRAY),
                        Component.literal("Click to hear it").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        NotchWidgets.divider(ctx, px + 8, py + 68, W - 16);

        boolean searching = !search.getValue().isBlank();
        for (int c = 0; c < SoundCatalog.CATEGORIES.length; c++) {
            int cy = py + CAT_Y + c * CAT_STEP;
            boolean hover = over(mouseX, mouseY, px + CAT_X, cy, CAT_W, CAT_H);
            if (!searching && c == category) {
                NotchWidgets.goldButton(ctx, this.font, px + CAT_X, cy, CAT_W, CAT_H, SoundCatalog.CATEGORIES[c], hover);
            } else {
                NotchWidgets.neutralButton(ctx, this.font, px + CAT_X, cy, CAT_W, CAT_H, SoundCatalog.CATEGORIES[c], hover);
            }
            if (hover && c == SoundCatalog.AMBIENT) {
                tip = List.of(Component.literal("Ambient").withStyle(ChatFormatting.WHITE),
                        Component.literal("Caves, weather and background noise.").withStyle(ChatFormatting.GRAY));
            }
        }

        NotchWidgets.inset(ctx, px + LIST_X, py + SEARCH_Y, LIST_W, 14, NotchTheme.DEEP);
        boolean full = chosen.size() >= NotchNpcEntity.MAX_SOUNDS;
        for (int r = 0; r < ROWS; r++) {
            int idx = scroll + r;
            if (idx >= shown.size()) break;
            SoundCatalog.Entry e = shown.get(idx);
            int ry = rowY(r);
            boolean rowHover = over(mouseX, mouseY, px + LIST_X, ry, LIST_W, ROW_H);
            if (rowHover) ctx.fill(px + LIST_X, ry, px + LIST_X + LIST_W, ry + ROW_H, 0x22000000);
            boolean playHover = over(mouseX, mouseY, px + LIST_X, ry + 1, 12, 12);
            NotchWidgets.neutralButton(ctx, this.font, px + LIST_X, ry + 1, 12, 12, "", playHover);
            playIcon(ctx, px + LIST_X + 4, ry + 3, NotchTheme.TEXT_DARK);

            int textX = px + LIST_X + 16;
            String name = this.font.plainSubstrByWidth(e.name(), NAME_W);
            ctx.drawString(this.font, name, textX, ry + 3, NotchTheme.TEXT_DARK, false);
            int idX = textX + this.font.width(name) + 6;
            int idRoom = addX() - 4 - idX;
            if (idRoom > 20) {
                ctx.drawString(this.font, this.font.plainSubstrByWidth(SoundCatalog.shortId(e.id()), idRoom),
                        idX, ry + 3, NotchTheme.TEXT_MUTED, false);
            }

            boolean have = inBox(e.id());
            boolean addHover = over(mouseX, mouseY, addX(), ry + 1, 14, 12);
            if (have || full) {
                NotchWidgets.neutralButton(ctx, this.font, addX(), ry + 1, 14, 12, "+", false);
            } else {
                NotchWidgets.primaryButton(ctx, this.font, addX(), ry + 1, 14, 12, "+", addHover);
            }

            if (playHover) {
                tip = List.of(Component.literal("Hear it").withStyle(ChatFormatting.WHITE));
            } else if (addHover) {
                tip = List.of(Component.literal(have ? "Already in the box" : full
                        ? "A box holds up to " + NotchNpcEntity.MAX_SOUNDS + " sounds" : "Add to the box")
                        .withStyle(have || full ? ChatFormatting.GRAY : ChatFormatting.WHITE));
            } else if (rowHover) {
                tip = List.of(Component.literal(e.name()).withStyle(ChatFormatting.WHITE),
                        Component.literal(SoundCatalog.shortId(e.id())).withStyle(ChatFormatting.GRAY),
                        Component.literal("Click to hear it").withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        if (shown.isEmpty()) {
            NotchWidgets.centerText(ctx, this.font, searching ? "No sounds match." : "Nothing here.",
                    px + LIST_X + LIST_W / 2, py + ROWS_Y + 40, NotchTheme.TEXT_MUTED, false);
        }
        if (shown.size() > ROWS) {
            int track = ROWS * ROW_H, knob = Math.max(6, track * ROWS / shown.size());
            int top = py + ROWS_Y + (track - knob) * scroll / Math.max(1, shown.size() - ROWS);
            ctx.fill(px + LIST_X - 4, py + ROWS_Y, px + LIST_X - 2, py + ROWS_Y + track, NotchTheme.PANEL_MID);
            ctx.fill(px + LIST_X - 4, top, px + LIST_X - 2, top + knob, NotchTheme.EDGE);
        }

        int by = py + H - 22;
        NotchWidgets.primaryButton(ctx, this.font, px + W / 2 - 104, by, 100, 16, "Done",
                over(mouseX, mouseY, px + W / 2 - 104, by, 100, 16));
        NotchWidgets.neutralButton(ctx, this.font, px + W / 2 + 4, by, 100, 16, "Cancel",
                over(mouseX, mouseY, px + W / 2 + 4, by, 100, 16));

        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
        if (tip != null) ctx.renderComponentTooltip(this.font, tip, mouseX, mouseY);
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
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            for (int i = 0; i < chosen.size(); i++) {
                int cx = chipX(i), cy = chipY(i);
                if (over(mouseX, mouseY, cx + CHIP_W - 11, cy, 11, CHIP_H)) {
                    NotchWidgets.click();
                    chosen.remove(i);
                    return true;
                }
                if (over(mouseX, mouseY, cx, cy, CHIP_W, CHIP_H)) {
                    play(chosen.get(i), pitch, true);
                    return true;
                }
            }
            for (int c = 0; c < SoundCatalog.CATEGORIES.length; c++) {
                if (over(mouseX, mouseY, px + CAT_X, py + CAT_Y + c * CAT_STEP, CAT_W, CAT_H)) {
                    NotchWidgets.click();
                    category = c;
                    search.setValue("");
                    return true;
                }
            }
            for (int r = 0; r < ROWS; r++) {
                int idx = scroll + r;
                if (idx >= shown.size()) break;
                SoundCatalog.Entry e = shown.get(idx);
                int ry = rowY(r);
                if (over(mouseX, mouseY, addX(), ry + 1, 14, 12)) {
                    if (!inBox(e.id()) && chosen.size() < NotchNpcEntity.MAX_SOUNDS) {
                        NotchWidgets.click();
                        chosen.add(SoundCatalog.shortId(e.id()));
                    }
                    return true;
                }
                if (over(mouseX, mouseY, px + LIST_X, ry, LIST_W, ROW_H)) {
                    play(e.id(), pitch, true);
                    return true;
                }
            }
            int by = py + H - 22;
            if (over(mouseX, mouseY, px + W / 2 - 104, by, 100, 16)) {
                NotchWidgets.click();
                stopPlaying();
                onDone.accept(String.join(", ", chosen));
                return true;
            }
            if (over(mouseX, mouseY, px + W / 2 + 4, by, 100, 16)) {
                NotchWidgets.click();
                onClose();
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
    //? if >=1.21 {
    /*public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
    *///?} else {
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    //?}
        int maxScroll = Math.max(0, shown.size() - ROWS);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(amount) * 3));
        return true;
    }

    @Override
    public void onClose() {
        stopPlaying();
        onCancel.run();
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
