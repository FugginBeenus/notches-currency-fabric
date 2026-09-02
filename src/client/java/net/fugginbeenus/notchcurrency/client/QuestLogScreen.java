package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class QuestLogScreen extends Screen {

    private static final int W = 300, H = 220;
    private int px, py, scroll;

    public QuestLogScreen() {
        super(Component.literal("Quest Log"));
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
    }

    private List<BountyTrackerHud.Entry> quests() {
        List<BountyTrackerHud.Entry> out = new ArrayList<>();
        for (BountyTrackerHud.Entry e : BountyTrackerHud.entries()) {
            if (e.quest()) out.add(e);
        }
        return out;
    }

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
        NotchWidgets.title(ctx, this.font, "Quest Log", px + W / 2, py + 8);

        List<BountyTrackerHud.Entry> list = quests();
        if (list.isEmpty()) {
            NotchWidgets.centerText(ctx, this.font, "You are not on any quests.",
                    px + W / 2, py + H / 2 - 8, NotchTheme.TEXT_MUTED, false);
            NotchWidgets.centerText(ctx, this.font, "Talk to people to find some.",
                    px + W / 2, py + H / 2 + 2, NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.divider(ctx, px + 8, py + 22, W - 16);
        int rows = Math.min(6, list.size());
        for (int i = 0; i < rows; i++) {
            BountyTrackerHud.Entry e = list.get(i + scroll);
            int ry = py + 30 + i * 28;
            NotchWidgets.inset(ctx, px + 10, ry, W - 20, 24, NotchTheme.PANEL_MID);
            ctx.drawString(this.font, fit(e.desc(), W - 32), px + 16, ry + 4,
                    NotchTheme.TEXT_DARK, false);
            boolean done = e.prog() >= e.req();
            String state = done ? "Ready to hand in" : e.prog() + " of " + e.req();
            ctx.drawString(this.font, state, px + 16, ry + 14,
                    done ? 0xFF3FA34B : NotchTheme.TEXT_MUTED, false);
        }

        if (list.size() > 6) {
            NotchWidgets.neutralButton(ctx, this.font, px + W - 34, py + 30, 24, 14, "^",
                    over(mouseX, mouseY, px + W - 34, py + 30, 24, 14));
            NotchWidgets.neutralButton(ctx, this.font, px + W - 34, py + 48, 24, 14, "v",
                    over(mouseX, mouseY, px + W - 34, py + 48, 24, 14));
        }

        boolean admin = net.fugginbeenus.notchcurrency.compat.PermsClient.isOperator();
        if (admin) {
            NotchWidgets.neutralButton(ctx, this.font, px + 12, py + H - 26, 100, 16, "Design quests",
                    over(mouseX, mouseY, px + 12, py + H - 26, 100, 16));
        }
        NotchWidgets.primaryButton(ctx, this.font, px + W - 112, py + H - 26, 100, 16, "Close",
                over(mouseX, mouseY, px + W - 112, py + H - 26, 100, 16));

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
            int size = quests().size();
            if (over(mx, my, px + W - 34, py + 30, 24, 14)) {
                NotchWidgets.click();
                scroll = Math.max(0, scroll - 1);
                return true;
            }
            if (over(mx, my, px + W - 34, py + 48, 24, 14)) {
                NotchWidgets.click();
                scroll = Math.min(Math.max(0, size - 6), scroll + 1);
                return true;
            }
            if (net.fugginbeenus.notchcurrency.compat.PermsClient.isOperator()
                    && over(mx, my, px + 12, py + H - 26, 100, 16)) {
                NotchWidgets.click();
                net.fugginbeenus.notchcurrency.net.NotchPacketsClient.sendQuestDesign();
                return true;
            }
            if (over(mx, my, px + W - 112, py + H - 26, 100, 16)) {
                NotchWidgets.click();
                this.onClose();
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
}
