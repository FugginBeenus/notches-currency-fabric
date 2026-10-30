package net.fugginbeenus.notchcurrency.client.ui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

public final class SignPicker {

    private static final int W = 248, H = 186, COLS = 3, ROWS = 4, PER_PAGE = COLS * ROWS,
            CELL_W = 76, CELL_H = 24, GAP = 4, PAGER_BW = 16;
    private boolean open;
    private String current = "";
    private String fallback = "";
    private String preview = "";
    private int left, top, page;

    public void open(String current, String fallback, String preview) {
        this.open = true;
        this.current = current;
        this.fallback = fallback;
        this.preview = preview == null || preview.isBlank() ? "Your name here" : preview;
        int i = indexOf(current);
        this.page = i < 0 ? 0 : i / PER_PAGE;
    }

    public void close() { open = false; }

    public boolean isOpen() { return open; }

    private static int indexOf(String id) {
        List<Signs.Sign> all = Signs.all();
        for (int i = 0; i < all.size(); i++) {
            if (all.get(i).id().equals(id)) return i;
        }
        return -1;
    }

    private int pages() { return NotchWidgets.pages(Signs.all().size(), PER_PAGE); }
    private int cellX(int k) { return left + (W - (COLS * CELL_W + (COLS - 1) * GAP)) / 2 + (k % COLS) * (CELL_W + GAP); }
    private int cellY(int k) { return top + 50 + (k / COLS) * (CELL_H + 2); }
    private int cancelX() { return left + W / 2 - 40; }
    private int cancelY() { return top + H - 22; }
    private int pagerLeftX() { return left + 10; }
    private int pagerRightX() { return left + 66; }

    public void draw(GuiGraphics ctx, Font font, int screenW, int screenH, int mx, int my) {
        if (!open) return;
        left = screenW / 2 - W / 2;
        top = screenH / 2 - H / 2;
        List<Signs.Sign> all = Signs.all();
        int start = page * PER_PAGE;
        net.fugginbeenus.notchcurrency.compat.Render.pushGuiOverlay(ctx);
        ctx.fill(0, 0, screenW, screenH, 0x88000000);
        NotchWidgets.panel(ctx, left, top, W, H);
        NotchWidgets.centerText(ctx, font, "Pick a sign", left + W / 2, top + 7, NotchTheme.TEXT_DARK, false);
        String shown = current;
        for (int k = 0; k < PER_PAGE && start + k < all.size(); k++) {
            if (over(mx, my, cellX(k), cellY(k), CELL_W, CELL_H)) shown = all.get(start + k).id();
        }
        Signs.draw(ctx, font, shown, fallback, preview, left + W / 2, top + 24);
        for (int k = 0; k < PER_PAGE && start + k < all.size(); k++) {
            Signs.Sign s = all.get(start + k);
            int cx = cellX(k), cy = cellY(k);
            NotchWidgets.inset(ctx, cx, cy, CELL_W, CELL_H, s.id().equals(current) ? 0xFF7FB069 : NotchTheme.PANEL_MID);
            Signs.draw(ctx, font, s.id(), fallback, s.name(), cx + CELL_W / 2, cy + 5);
        }
        if (pages() > 1) {
            NotchWidgets.pager(ctx, font, pagerLeftX(), pagerRightX(), cancelY(), PAGER_BW, 16, page + 1, pages(),
                    over(mx, my, pagerLeftX(), cancelY(), PAGER_BW, 16),
                    over(mx, my, pagerRightX(), cancelY(), PAGER_BW, 16));
        }
        NotchWidgets.neutralButton(ctx, font, cancelX(), cancelY(), 80, 16, "Cancel",
                over(mx, my, cancelX(), cancelY(), 80, 16));
        net.fugginbeenus.notchcurrency.compat.Render.popGuiOverlay(ctx);
    }

    public String click(int mx, int my) {
        if (!open) return null;
        List<Signs.Sign> all = Signs.all();
        int start = page * PER_PAGE;
        for (int k = 0; k < PER_PAGE && start + k < all.size(); k++) {
            if (over(mx, my, cellX(k), cellY(k), CELL_W, CELL_H)) {
                open = false;
                NotchWidgets.click();
                return all.get(start + k).id();
            }
        }
        if (pages() > 1 && over(mx, my, pagerLeftX(), cancelY(), PAGER_BW, 16)) {
            if (page > 0) {
                page--;
                NotchWidgets.tick();
            }
            return "";
        }
        if (pages() > 1 && over(mx, my, pagerRightX(), cancelY(), PAGER_BW, 16)) {
            if (page + 1 < pages()) {
                page++;
                NotchWidgets.tick();
            }
            return "";
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
