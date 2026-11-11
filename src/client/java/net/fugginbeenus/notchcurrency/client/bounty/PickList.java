package net.fugginbeenus.notchcurrency.client.bounty;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class PickList {

    private static final int ROWS = 6, ROW_H = 10;
    private final Font font;
    private final EditBox box;
    private List<String> options = List.of();
    private String lastTyped = "";
    private int scroll, highlight;

    public PickList(Font font, EditBox box) {
        this.font = font;
        this.box = box;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public String chosen() {
        String typed = box.getValue().trim();
        for (String o : options) if (o.equalsIgnoreCase(typed)) return o;
        return null;
    }

    public boolean isOpen() {
        return box.isVisible() && box.isFocused() && !matches().isEmpty();
    }

    private List<String> matches() {
        String typed = box.getValue().trim().toLowerCase(Locale.ROOT);
        List<String> first = new ArrayList<>(), rest = new ArrayList<>();
        for (String o : options) {
            String low = o.toLowerCase(Locale.ROOT);
            if (low.startsWith(typed)) first.add(o);
            else if (low.contains(typed)) rest.add(o);
        }
        first.addAll(rest);
        return first;
    }

    private void sync() {
        if (!box.getValue().equals(lastTyped)) {
            lastTyped = box.getValue();
            scroll = 0;
            highlight = 0;
        }
    }

    private int panelX() { return box.getX() - 3; }
    private int panelY() { return box.getY() + 12; }
    private int panelW() { return box.getWidth() + 6; }

    public void render(GuiGraphics ctx, int mx, int my) {
        if (!box.isVisible()) return;
        if (!box.getValue().isBlank()) {
            int line = chosen() != null ? 0xFF6FC274 : 0xFFD06B5A;
            ctx.fill(box.getX(), box.getY() + 10, box.getX() + box.getWidth(), box.getY() + 11, line);
        }
        if (!isOpen()) return;
        sync();
        List<String> m = matches();
        int shown = Math.min(ROWS, m.size()), x = panelX(), y = panelY(), w = panelW(), h = shown * ROW_H + 2;
        ctx.fill(x, y, x + w, y + h, 0xFF000000);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF262A33);
        for (int i = 0; i < shown; i++) {
            int idx = scroll + i, ry = y + 1 + i * ROW_H;
            boolean hot = idx == highlight || (mx >= x && mx < x + w && my >= ry && my < ry + ROW_H);
            if (hot) ctx.fill(x + 1, ry, x + w - 1, ry + ROW_H, 0xFF454B58);
            ctx.drawString(font, font.plainSubstrByWidth(m.get(idx), w - 10), x + 4, ry + 1,
                    hot ? 0xFFFFFFFF : 0xFFC9CCD3, false);
        }
        if (m.size() > ROWS) {
            int track = h - 2, knob = Math.max(4, track * ROWS / m.size());
            int top = y + 1 + (track - knob) * scroll / Math.max(1, m.size() - ROWS);
            ctx.fill(x + w - 3, top, x + w - 2, top + knob, 0xFF8C93A3);
        }
    }

    public boolean click(double mx, double my) {
        if (!isOpen()) return false;
        List<String> m = matches();
        int shown = Math.min(ROWS, m.size()), x = panelX(), y = panelY(), w = panelW(), h = shown * ROW_H + 2;
        if (mx < x || mx >= x + w || my < y || my >= y + h) return false;
        int i = (int) ((my - y - 1) / ROW_H);
        if (i >= 0 && i < shown) pick(m.get(scroll + i));
        return true;
    }

    public boolean scroll(double mx, double my, double amount) {
        if (!isOpen()) return false;
        List<String> m = matches();
        int x = panelX(), y = panelY(), h = Math.min(ROWS, m.size()) * ROW_H + 2;
        if (mx < x || mx >= x + panelW() || my < y || my >= y + h) return false;
        scroll = Math.max(0, Math.min(m.size() - ROWS, scroll - (int) Math.signum(amount)));
        return true;
    }

    public boolean key(int keyCode) {
        if (!isOpen()) return false;
        List<String> m = matches();
        if (keyCode == GLFW.GLFW_KEY_DOWN || keyCode == GLFW.GLFW_KEY_UP) {
            highlight = Math.max(0, Math.min(m.size() - 1, highlight + (keyCode == GLFW.GLFW_KEY_DOWN ? 1 : -1)));
            if (highlight < scroll) scroll = highlight;
            if (highlight >= scroll + ROWS) scroll = highlight - ROWS + 1;
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_TAB) {
            pick(m.get(Math.min(highlight, m.size() - 1)));
            return true;
        }
        return false;
    }

    private void pick(String option) {
        box.setValue(option);
        lastTyped = option;
        box.setFocused(false);
    }
}
