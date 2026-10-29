package net.fugginbeenus.notchcurrency.client.ui;

import net.fugginbeenus.notchcurrency.shop.SignStyles;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class SignPicker {

    private static final int W = 248, H = 186, COLS = 3, CELL_W = 76, CELL_H = 24, GAP = 4;
    private boolean open;
    private String current = SignStyles.SHOP_DEFAULT;
    private String preview = "";
    private int left, top;

    public void open(String current, String preview) {
        this.open = true;
        this.current = current;
        this.preview = preview == null || preview.isBlank() ? "Your name here" : preview;
    }

    public void close() { open = false; }

    public boolean isOpen() { return open; }

    private int cellX(int i) { return left + (W - (COLS * CELL_W + (COLS - 1) * GAP)) / 2 + (i % COLS) * (CELL_W + GAP); }
    private int cellY(int i) { return top + 50 + (i / COLS) * (CELL_H + 2); }
    private int cancelX() { return left + W / 2 - 40; }
    private int cancelY() { return top + H - 22; }

    public void draw(GuiGraphics ctx, Font font, int screenW, int screenH, int mx, int my) {
        if (!open) return;
        left = screenW / 2 - W / 2;
        top = screenH / 2 - H / 2;
        net.fugginbeenus.notchcurrency.compat.Render.pushGuiOverlay(ctx);
        ctx.fill(0, 0, screenW, screenH, 0x88000000);
        NotchWidgets.panel(ctx, left, top, W, H);
        NotchWidgets.centerText(ctx, font, "Pick a sign", left + W / 2, top + 7, NotchTheme.TEXT_DARK, false);
        String shown = current;
        for (int i = 0; i < SignStyles.IDS.size(); i++) {
            if (over(mx, my, cellX(i), cellY(i), CELL_W, CELL_H)) shown = SignStyles.IDS.get(i);
        }
        NotchWidgets.plate(ctx, font, NotchWidgets.Plate.forSign(shown), preview, left + W / 2, top + 24);
        for (int i = 0; i < SignStyles.IDS.size(); i++) {
            int cx = cellX(i), cy = cellY(i);
            boolean chosen = SignStyles.IDS.get(i).equals(current);
            NotchWidgets.inset(ctx, cx, cy, CELL_W, CELL_H, chosen ? 0xFF7FB069 : NotchTheme.PANEL_MID);
            NotchWidgets.plate(ctx, font, NotchWidgets.Plate.forSign(SignStyles.IDS.get(i)), SignStyles.NAMES.get(i),
                    cx + CELL_W / 2, cy + 5);
        }
        NotchWidgets.neutralButton(ctx, font, cancelX(), cancelY(), 80, 16, "Cancel",
                over(mx, my, cancelX(), cancelY(), 80, 16));
        net.fugginbeenus.notchcurrency.compat.Render.popGuiOverlay(ctx);
    }

    public String click(int mx, int my) {
        if (!open) return null;
        for (int i = 0; i < SignStyles.IDS.size(); i++) {
            if (over(mx, my, cellX(i), cellY(i), CELL_W, CELL_H)) {
                open = false;
                NotchWidgets.click();
                return SignStyles.IDS.get(i);
            }
        }
        if (over(mx, my, cancelX(), cancelY(), 80, 16) || !over(mx, my, left, top, W, H)) {
            NotchWidgets.click();
            open = false;
        }
        return "";
    }

    private static boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
