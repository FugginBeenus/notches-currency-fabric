package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public class QuestDesignerScreen extends Screen {

    private static final int W = 320, H = 260;
    private static final int ROW_H = 31, ROWS = 5;

    public static java.util.UUID cameFromNpc = null;

    private int px, py, scroll;
    private EditBox newNameField;

    public QuestDesignerScreen() {
        super(Component.literal("Quest Designer"));
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
        newNameField = new EditBox(this.font, px + 13, py + H - 45, 170, 10, Component.empty());
        newNameField.setMaxLength(48);
        newNameField.setBordered(false);
        newNameField.setHint(Component.literal("new quest name")
                .withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        addRenderableWidget(newNameField);
    }

    private List<QuestNames.Entry> quests() { return QuestNames.all(); }

    private boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

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
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.font, "Quest Designer", px + W / 2, py + 8);
        NotchWidgets.centerText(ctx, this.font, "Write quests here, then hand them out anywhere.",
                px + W / 2, py + 22, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.divider(ctx, px + 8, py + 34, W - 16);

        List<QuestNames.Entry> list = quests();
        if (list.isEmpty()) {
            NotchWidgets.centerText(ctx, this.font, "No quests yet. Name one below.",
                    px + W / 2, py + 80, NotchTheme.TEXT_MUTED, false);
        }
        int shown = Math.min(ROWS, Math.max(0, list.size() - scroll));
        for (int i = 0; i < shown; i++) {
            QuestNames.Entry e = list.get(i + scroll);
            int ry = py + 42 + i * ROW_H;
            boolean hover = over(mouseX, mouseY, px + 12, ry, W - 46, 27);
            NotchWidgets.neutralButton(ctx, this.font, px + 12, ry, W - 46, 27, "", hover);
            ctx.drawString(this.font, fit(e.key(), W - 62), px + 18, ry + 6, NotchTheme.TEXT_DARK, false);
            ctx.drawString(this.font, fit(e.summary(), W - 62), px + 18, ry + 16,
                    NotchTheme.TEXT_MUTED, false);
            NotchWidgets.dangerButton(ctx, this.font, px + W - 32, ry + 5, 20, 18, "x",
                    over(mouseX, mouseY, px + W - 32, ry + 5, 20, 18));
        }

        if (list.size() > ROWS) {
            NotchWidgets.neutralButton(ctx, this.font, px + W - 32, py + 42 + ROWS * ROW_H, 20, 12, "^",
                    over(mouseX, mouseY, px + W - 32, py + 42 + ROWS * ROW_H, 20, 12));
            NotchWidgets.neutralButton(ctx, this.font, px + 12, py + 42 + ROWS * ROW_H, 20, 12, "v",
                    over(mouseX, mouseY, px + 12, py + 42 + ROWS * ROW_H, 20, 12));
        }

        NotchWidgets.divider(ctx, px + 8, py + H - 54, W - 16);
        NotchWidgets.inset(ctx, px + 10, py + H - 49, 176, 14, NotchTheme.DEEP);
        NotchWidgets.primaryButton(ctx, this.font, px + 192, py + H - 49, 116, 14, "+ New quest",
                over(mouseX, mouseY, px + 192, py + H - 49, 116, 14));
        NotchWidgets.neutralButton(ctx, this.font, px + W / 2 - 50, py + H - 26, 100, 16, "Back",
                over(mouseX, mouseY, px + W / 2 - 50, py + H - 26, 100, 16));

        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
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
            List<QuestNames.Entry> list = quests();
            int shown = Math.min(ROWS, Math.max(0, list.size() - scroll));
            for (int i = 0; i < shown; i++) {
                int ry = py + 42 + i * ROW_H;
                QuestNames.Entry e = list.get(i + scroll);
                if (over(mx, my, px + W - 32, ry + 5, 20, 18)) {
                    NotchWidgets.click();
                    NotchPacketsClient.sendQuestDelete(e.key());
                    return true;
                }
                if (over(mx, my, px + 12, ry, W - 46, 27)) {
                    NotchWidgets.click();
                    QuestEditorScreen.cameFromDesigner = true;
                    NotchPacketsClient.sendQuestOpen(e.key());
                    return true;
                }
            }
            if (list.size() > ROWS) {
                if (over(mx, my, px + W - 32, py + 42 + ROWS * ROW_H, 20, 12)) {
                    NotchWidgets.click();
                    scroll = Math.max(0, scroll - 1);
                    return true;
                }
                if (over(mx, my, px + 12, py + 42 + ROWS * ROW_H, 20, 12)) {
                    NotchWidgets.click();
                    scroll = Math.min(Math.max(0, list.size() - ROWS), scroll + 1);
                    return true;
                }
            }
            if (over(mx, my, px + 192, py + H - 49, 116, 14)) {
                String name = newNameField.getValue().trim();
                if (!name.isBlank()) {
                    NotchWidgets.click();
                    QuestEditorScreen.cameFromDesigner = true;
                    NotchPacketsClient.sendQuestOpen(name);
                }
                return true;
            }
            if (over(mx, my, px + W / 2 - 50, py + H - 26, 100, 16)) {
                NotchWidgets.click();
                if (cameFromNpc != null) {
                    java.util.UUID back = cameFromNpc;
                    cameFromNpc = null;
                    NotchPacketsClient.sendNpcEditorReopen(back, 5);
                } else {
                    this.onClose();
                }
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
