package net.fugginbeenus.notchcurrency.client.ui;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.lwjgl.glfw.GLFW;

public final class NotchWidgets {

    private NotchWidgets() {}

    public static void panel(GuiGraphics ctx, int x, int y, int w, int h) {
        final int r = 1;
        fillRound(ctx, x, y, w, h, r, NotchTheme.OUTLINE);
        fillRound(ctx, x + 1, y + 1, w - 2, h - 2, r - 1, NotchTheme.PANEL_FACE);
        ctx.fill(x + r, y + 1, x + w - r, y + 2, NotchTheme.HIGHLIGHT);
        ctx.fill(x + 1, y + r, x + 2, y + h - r, NotchTheme.HIGHLIGHT);
        ctx.fill(x + r, y + h - 2, x + w - r, y + h - 1, NotchTheme.EDGE);
        ctx.fill(x + w - 2, y + r, x + w - 1, y + h - r, NotchTheme.EDGE);
    }

    public static void slot(GuiGraphics ctx, int x, int y) {
        slot(ctx, x, y, 18, 18);
    }

    public static void slot(GuiGraphics ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, NotchTheme.INSET_SHADOW);
        ctx.fill(x, y, x + w, y + 1, NotchTheme.EDGE);
        ctx.fill(x, y, x + 1, y + h, NotchTheme.EDGE);
        ctx.fill(x + 1, y + h - 1, x + w, y + h, NotchTheme.HIGHLIGHT);
        ctx.fill(x + w - 1, y + 1, x + w, y + h, NotchTheme.HIGHLIGHT);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, NotchTheme.SLOT_FILL);
    }

    public static void button(GuiGraphics ctx, int x, int y, int w, int h, boolean hovered, boolean pressed) {
        buttonSel(ctx, x, y, w, h, hovered || pressed, true, true, true, true);
    }

    public static void colorButton(GuiGraphics ctx, int x, int y, int w, int h,
                                   int base, int light, int dark, boolean hovered) {
        final int r = 1;
        int face = hovered ? lighten(base) : base;
        fillRound(ctx, x, y, w, h, r, NotchTheme.OUTLINE);
        fillRound(ctx, x + 1, y + 1, w - 2, h - 2, r - 1, face);
        ctx.fill(x + r, y + 1, x + w - r, y + 2, light);
        ctx.fill(x + 1, y + r, x + 2, y + h - r, light);
        ctx.fill(x + r, y + h - 2, x + w - r, y + h - 1, dark);
        ctx.fill(x + w - 2, y + r, x + w - 1, y + h - r, dark);
    }

    public static void inset(GuiGraphics ctx, int x, int y, int w, int h, int fill) {
        final int r = 1;
        fillRound(ctx, x, y, w, h, r, NotchTheme.OUTLINE);
        fillRound(ctx, x + 1, y + 1, w - 2, h - 2, r - 1, fill);
        ctx.fill(x + r, y + 1, x + w - r, y + 2, NotchTheme.EDGE);
        ctx.fill(x + 1, y + r, x + 2, y + h - r, NotchTheme.EDGE);
        ctx.fill(x + r, y + h - 2, x + w - r, y + h - 1, NotchTheme.HIGHLIGHT);
        ctx.fill(x + w - 2, y + r, x + w - 1, y + h - r, NotchTheme.HIGHLIGHT);
    }

    public static void insetFlipped(GuiGraphics ctx, int x, int y, int w, int h, int fill) {
        final int r = 1;
        fillRound(ctx, x, y, w, h, r, NotchTheme.OUTLINE);
        fillRound(ctx, x + 1, y + 1, w - 2, h - 2, r - 1, fill);
        ctx.fill(x + r, y + 1, x + w - r, y + 2, NotchTheme.HIGHLIGHT);
        ctx.fill(x + 1, y + r, x + 2, y + h - r, NotchTheme.HIGHLIGHT);
        ctx.fill(x + r, y + h - 2, x + w - r, y + h - 1, NotchTheme.EDGE);
        ctx.fill(x + w - 2, y + r, x + w - 1, y + h - r, NotchTheme.EDGE);
    }

    public static void pill(GuiGraphics ctx, int x, int y, int w, int h) {
        final int r = 1;
        fillRound(ctx, x, y, w, h, r, NotchTheme.OUTLINE);
        fillRound(ctx, x + 1, y + 1, w - 2, h - 2, r - 1, NotchTheme.DEEP);
        ctx.fill(x + r, y + 1, x + w - r, y + 2, NotchTheme.OUTLINE);
    }

    public static void primaryButton(GuiGraphics ctx, Font tr, int x, int y, int w, int h, String label, boolean hovered) {
        colorButton(ctx, x, y, w, h, NotchTheme.ACCENT_GREEN, NotchTheme.GREEN_HI, NotchTheme.GREEN_LO, hovered);
        ctx.drawCenteredString(tr, Component.literal(label), x + w / 2, y + (h - 8) / 2, NotchTheme.TEXT_LIGHT);
    }

    public static void dangerButton(GuiGraphics ctx, Font tr, int x, int y, int w, int h, String label, boolean hovered) {
        colorButton(ctx, x, y, w, h, NotchTheme.ACCENT_RED, NotchTheme.RED_HI, NotchTheme.RED_LO, hovered);
        ctx.drawCenteredString(tr, Component.literal(label), x + w / 2, y + (h - 8) / 2, NotchTheme.TEXT_LIGHT);
    }

    public static void goldButton(GuiGraphics ctx, Font tr, int x, int y, int w, int h, String label, boolean hovered) {
        colorButton(ctx, x, y, w, h, NotchTheme.ACCENT_GOLD, NotchTheme.GOLD_HI, NotchTheme.GOLD_LO, hovered);
        ctx.drawCenteredString(tr, Component.literal(label), x + w / 2, y + (h - 8) / 2, NotchTheme.TEXT_LIGHT);
    }

    public static void neutralButton(GuiGraphics ctx, Font tr, int x, int y, int w, int h, String label, boolean hovered) {
        button(ctx, x, y, w, h, hovered, false);
        centerText(ctx, tr, label, x + w / 2, y + (h - 8) / 2, NotchTheme.TEXT_DARK, false);
    }

    public static void centerText(GuiGraphics ctx, Font tr, String label, int cx, int y, int color, boolean shadow) {
        ctx.drawString(tr, label, cx - tr.width(label) / 2, y, color, shadow);
    }

    public enum Plate {
        PLAIN, STEEL, BRASS, MARQUEE, STAMP, PARCHMENT, WOOD, DARKWOOD, STONE, ROYAL, MYSTIC, SPOOKY, FROST, PAINTED;

        public static Plate forSign(String id) {
            if (id == null) return PLAIN;
            return switch (id) {
                case "wood" -> WOOD;
                case "darkwood" -> DARKWOOD;
                case "stone" -> STONE;
                case "brass" -> BRASS;
                case "parchment" -> PARCHMENT;
                case "royal" -> ROYAL;
                case "mystic" -> MYSTIC;
                case "spooky" -> SPOOKY;
                case "frost" -> FROST;
                case "painted" -> PAINTED;
                default -> PLAIN;
            };
        }
    }

    private static final int GOLD = 0xFFF3B93F, GOLD_HI = 0xFFFFE08A, GOLD_LO = 0xFFC4842A;
    private static final int PAPER = 0xFFF2E6C6, PAPER_MID = 0xFFE2D0A4, PAPER_LO = 0xFFD6C6A0;
    private static final int CREAM = 0xFFF5EFD9, INK = 0xFF523E2A;

    public static void title(GuiGraphics ctx, Font tr, String label, int cx, int y) {
        title(ctx, tr, label, cx, y, Plate.PLAIN);
    }

    public static void title(GuiGraphics ctx, Font tr, String label, int cx, int y, Plate plate) {
        plate(ctx, tr, plate, label, cx, y - 14);
    }

    public static void plate(GuiGraphics ctx, Font tr, Plate kind, String label, int cx, int top) {
        int tw = tr.width(label);
        switch (kind) {
            case STEEL -> {
                int w = tw + 16, x = cx - w / 2;
                shell(ctx, x, top, w, 15, NotchTheme.OUTLINE, 0xFF4A4F58, 0xFF6E7480, 0xFF2E3238);
                ctx.drawString(tr, label, x + 8, top + 4, 0xFFEDEDED, true);
            }
            case BRASS -> {
                int w = tw + 20, x = cx - w / 2;
                shell(ctx, x, top, w, 15, NotchTheme.OUTLINE, GOLD, GOLD_HI, GOLD_LO);
                fillRound(ctx, x + 2, top + 2, w - 4, 11, 1, 0xFF262B34);
                ctx.fill(x + 3, top + 2, x + w - 3, top + 3, 0xFF15181F);
                screw(ctx, x + 4, top + 6);
                screw(ctx, x + w - 6, top + 6);
                ctx.drawString(tr, label, x + 10, top + 4, GOLD_HI, true);
            }
            case MARQUEE -> {
                int w = tw + 26, x = cx - w / 2;
                long phase = System.currentTimeMillis() / 140;
                shell(ctx, x, top, w, 15, NotchTheme.OUTLINE, GOLD, GOLD_HI, GOLD_LO);
                fillRound(ctx, x + 4, top + 3, w - 8, 9, 1, 0xFFD8433A);
                ctx.fill(x + 5, top + 3, x + w - 5, top + 4, 0xFFFF7A6E);
                ctx.fill(x + 5, top + 11, x + w - 5, top + 12, 0xFF952B27);
                int k = 0;
                for (int bx = x + 4; bx <= x + w - 6; bx += 4, k++) {
                    bulb(ctx, bx, top + 1, (k + phase) % 3 == 0);
                    bulb(ctx, bx, top + 12, (k + phase + 1) % 3 == 0);
                }
                ctx.drawString(tr, label, x + (w - tw) / 2 + 1, top + 4, 0xFFFFFFFF, true);
            }
            case STAMP -> {
                int w = tw + 22, h = 15, x = cx - w / 2;
                ctx.fill(x + 2, top + 2, x + w + 1, top + h + 1, 0x40000000);
                ctx.fill(x + 1, top + 1, x + w - 1, top + h - 1, CREAM);
                for (int i = 0; i < w; i += 2) {
                    dot(ctx, x + i, top, CREAM);
                    dot(ctx, x + i, top + h - 1, CREAM);
                }
                for (int i = 0; i < h; i += 2) {
                    dot(ctx, x, top + i, CREAM);
                    dot(ctx, x + w - 1, top + i, CREAM);
                }
                rectLine(ctx, x + 2, top + 2, w - 4, h - 4, 0xFFCDB98C);
                ring(ctx, x + w - 3, top + 7, 5, 0xD8C2362F);
                ring(ctx, x + w - 3, top + 7, 3, 0x80C2362F);
                ctx.drawString(tr, label, x + 8, top + 4, INK, false);
            }
            case PARCHMENT -> {
                int w = tw + 24, h = 15, x = cx - w / 2;
                shell(ctx, x, top, w, h, 0xFF4A3A26, PAPER, 0xFFFFF6DC, PAPER_MID);
                ctx.fill(x + 2, top + h - 3, x + w - 2, top + h - 2, PAPER_MID);
                ctx.fill(x + 1, top + 2, x + 3, top + h - 2, PAPER_LO);
                ctx.fill(x + w - 3, top + 2, x + w - 1, top + h - 2, PAPER_LO);
                ctx.fill(x + 3, top + 2, x + 4, top + h - 2, 0xFFB8A57E);
                ctx.fill(x + w - 4, top + 2, x + w - 3, top + h - 2, 0xFFB8A57E);
                tack(ctx, x + 6, top + 5);
                tack(ctx, x + w - 9, top + 5);
                ctx.drawString(tr, label, x + 12, top + 4, INK, false);
            }
            case WOOD -> {
                int w = tw + 20, x = cx - w / 2;
                hooks(ctx, x, w, top);
                shell(ctx, x, top, w, 15, NotchTheme.OUTLINE, 0xFFA67C48, 0xFFC49A60, 0xFF7C5830);
                ctx.fill(x + 3, top + 5, x + 7, top + 6, 0xFF8F6A3C);
                ctx.fill(x + 3, top + 9, x + 8, top + 10, 0xFF8F6A3C);
                ctx.fill(x + w - 8, top + 6, x + w - 3, top + 7, 0xFF8F6A3C);
                ctx.fill(x + w - 7, top + 10, x + w - 3, top + 11, 0xFF8F6A3C);
                dot(ctx, x + 5, top + 2, 0xFF3E3E3E);
                dot(ctx, x + w - 6, top + 2, 0xFF3E3E3E);
                ctx.drawString(tr, label, x + 10, top + 4, 0xFFFFF3D6, true);
            }
            case DARKWOOD -> {
                int w = tw + 20, x = cx - w / 2;
                hooks(ctx, x, w, top);
                shell(ctx, x, top, w, 15, NotchTheme.OUTLINE, 0xFF5C3C24, 0xFF7C5634, 0xFF3A2614);
                ctx.fill(x + 3, top + 5, x + 7, top + 6, 0xFF4A301C);
                ctx.fill(x + w - 8, top + 9, x + w - 3, top + 10, 0xFF4A301C);
                dot(ctx, x + 5, top + 2, 0xFF2A2A2A);
                dot(ctx, x + w - 6, top + 2, 0xFF2A2A2A);
                ctx.drawString(tr, label, x + 10, top + 4, 0xFFF5E6C8, true);
            }
            case STONE -> {
                int w = tw + 18, h = 15, x = cx - w / 2;
                shell(ctx, x, top, w, h, NotchTheme.OUTLINE, 0xFF8E8E8E, 0xFF8E8E8E, 0xFF8E8E8E);
                for (int j = top + 2; j < top + h - 2; j++) {
                    for (int i = x + 2; i < x + w - 2; i++) {
                        int k = Math.floorMod(i * 7 + j * 13, 11);
                        if (k == 0) dot(ctx, i, j, 0xFF7A7A7A);
                        else if (k == 5) dot(ctx, i, j, 0xFFA2A2A2);
                    }
                }
                ctx.fill(x + 2, top + 1, x + w - 2, top + 2, 0xFFB8B8B8);
                ctx.fill(x + 1, top + 2, x + 2, top + h - 2, 0xFFB8B8B8);
                ctx.fill(x + 2, top + h - 2, x + w - 2, top + h - 1, 0xFF5E5E5E);
                ctx.fill(x + w - 2, top + 2, x + w - 1, top + h - 2, 0xFF5E5E5E);
                ctx.drawString(tr, label, x + 9, top + 5, 0xFFC4C4C4, false);
                ctx.drawString(tr, label, x + 9, top + 4, 0xFF3A3A3A, false);
            }
            case ROYAL -> {
                int w = tw + 24, h = 15, x = cx - w / 2;
                shell(ctx, x, top, w, h, NotchTheme.OUTLINE, 0xFF8C1C24, 0xFFB8323A, 0xFF5A0F15);
                rectLine(ctx, x + 3, top + 3, w - 6, h - 6, NotchTheme.ACCENT_GOLD);
                for (int dx : new int[]{x + 6, x + w - 7}) {
                    dot(ctx, dx, top + 6, NotchTheme.GOLD_HI);
                    dot(ctx, dx - 1, top + 7, NotchTheme.ACCENT_GOLD);
                    dot(ctx, dx + 1, top + 7, NotchTheme.ACCENT_GOLD);
                    dot(ctx, dx, top + 8, NotchTheme.GOLD_LO);
                }
                ctx.drawString(tr, label, x + 12, top + 4, NotchTheme.GOLD_HI, true);
            }
            case MYSTIC -> {
                int w = tw + 22, h = 15, x = cx - w / 2;
                long phase = System.currentTimeMillis() / 350;
                shell(ctx, x, top, w, h, NotchTheme.OUTLINE, 0xFF261C46, 0xFF3E2F6B, 0xFF1A1232);
                int[][] stars = {{4, 4}, {w - 5, 4}, {5, 10}, {w - 5, 10}, {w / 2 - 14, 2}, {w / 2 + 16, 12}, {8, 7}, {w - 9, 7}};
                for (int i = 0; i < stars.length; i++) {
                    boolean on = (i + phase) % 3 != 0;
                    dot(ctx, x + stars[i][0], top + stars[i][1], on ? 0xFFFFFFFF : 0xFF8A7ACA);
                    if (on && i % 2 == 0) {
                        dot(ctx, x + stars[i][0] - 1, top + stars[i][1], 0xFF8A7ACA);
                        dot(ctx, x + stars[i][0] + 1, top + stars[i][1], 0xFF8A7ACA);
                    }
                }
                ctx.drawString(tr, label, x + 11, top + 4, 0xFFE8E0FF, true);
            }
            case SPOOKY -> {
                int w = tw + 20, h = 15, x = cx - w / 2, tx = x + 10, ty = top + 4;
                shell(ctx, x, top, w, h, NotchTheme.OUTLINE, 0xFF1C2420, 0xFF2E4A36, 0xFF121814);
                ctx.fill(x + 3, top + 3, x + 4, top + 7, 0xFF6E7E72);
                ctx.fill(x + 3, top + 3, x + 7, top + 4, 0xFF6E7E72);
                dot(ctx, x + 4, top + 4, 0xFF6E7E72);
                dot(ctx, x + 5, top + 5, 0xFF6E7E72);
                int[][] glow = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
                for (int[] o : glow) ctx.drawString(tr, label, tx + o[0], ty + o[1], 0xFF1E5A2A, false);
                ctx.drawString(tr, label, tx, ty, 0xFF9CFF8A, false);
            }
            case FROST -> {
                int w = tw + 18, h = 15, x = cx - w / 2;
                shell(ctx, x, top, w, h, 0xFF2E5A7A, 0xFFD6F0FF, 0xFFFFFFFF, 0xFF9ACFEA);
                int[][] icicles = {{6, 3}, {w / 2, 2}, {w - 8, 3}, {12, 2}, {w - 14, 2}};
                for (int[] ic : icicles) {
                    ctx.fill(x + ic[0], top + h, x + ic[0] + 1, top + h + ic[1], 0xFFBFE6FF);
                    dot(ctx, x + ic[0], top + h + ic[1] - 1, 0xFFFFFFFF);
                }
                ctx.drawString(tr, label, x + 9, top + 4, 0xFF1E4A6A, false);
            }
            case PAINTED -> {
                int w = tw + 18, x = cx - w / 2;
                shell(ctx, x, top, w, 15, NotchTheme.OUTLINE, 0xFF2E9A8E, 0xFF4EBAAE, 0xFF206B63);
                ctx.drawString(tr, label, x + 9, top + 4, 0xFFFFFFFF, true);
            }
            default -> {
                int w = tw + 16, x = cx - w / 2;
                shell(ctx, x, top, w, 15, NotchTheme.OUTLINE, 0xFFC6C6C6, NotchTheme.HIGHLIGHT, 0xFF8A8A8A);
                ctx.drawString(tr, label, x + 8, top + 4, NotchTheme.TEXT_DARK, false);
            }
        }
    }

    private static void hooks(GuiGraphics ctx, int x, int w, int top) {
        for (int hx : new int[]{x + 5, x + w - 6}) {
            ctx.fill(hx, top - 6, hx + 1, top, 0xFF3E3E3E);
            ctx.fill(hx - 1, top - 5, hx + 2, top - 4, 0xFF7A7A7A);
            ctx.fill(hx - 1, top - 2, hx + 2, top - 1, 0xFF7A7A7A);
        }
    }

    private static void shell(GuiGraphics ctx, int x, int t, int w, int h, int o, int f, int hi, int lo) {
        ctx.fill(x + 2, t, x + w - 2, t + 1, o);
        ctx.fill(x + 1, t + 1, x + w - 1, t + 2, o);
        ctx.fill(x, t + 2, x + w, t + h - 2, o);
        ctx.fill(x + 1, t + h - 2, x + w - 1, t + h - 1, o);
        ctx.fill(x + 2, t + h - 1, x + w - 2, t + h, o);
        fillRound(ctx, x + 1, t + 1, w - 2, h - 2, 1, f);
        ctx.fill(x + 2, t + 1, x + w - 2, t + 2, hi);
        ctx.fill(x + 1, t + 2, x + 2, t + h - 2, hi);
        ctx.fill(x + 2, t + h - 2, x + w - 2, t + h - 1, lo);
        ctx.fill(x + w - 2, t + 2, x + w - 1, t + h - 2, lo);
    }

    private static void dot(GuiGraphics ctx, int x, int y, int color) {
        ctx.fill(x, y, x + 1, y + 1, color);
    }

    private static void rectLine(GuiGraphics ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    private static void ring(GuiGraphics ctx, int cx, int cy, int r, int color) {
        for (int j = -r - 1; j <= r + 1; j++) {
            for (int i = -r - 1; i <= r + 1; i++) {
                if (Math.abs(Math.hypot(i, j) - r) < 0.55) dot(ctx, cx + i, cy + j, color);
            }
        }
    }

    private static void screw(GuiGraphics ctx, int x, int y) {
        dot(ctx, x, y, GOLD_HI);
        dot(ctx, x + 1, y, GOLD);
        dot(ctx, x, y + 1, GOLD);
        dot(ctx, x + 1, y + 1, GOLD_LO);
    }

    private static void tack(GuiGraphics ctx, int x, int y) {
        ctx.fill(x, y, x + 3, y + 3, 0xFF3C424F);
        dot(ctx, x, y, 0xFF5A6272);
        dot(ctx, x + 2, y + 2, 0xFF15181F);
    }

    private static void bulb(GuiGraphics ctx, int x, int y, boolean on) {
        ctx.fill(x, y, x + 2, y + 2, on ? 0xFFFFF3B0 : GOLD_LO);
        if (on) dot(ctx, x, y, 0xFFFFFFFF);
    }

    public static void tab(GuiGraphics ctx, Font tr, int x, int y, int w, int h, String label,
                           boolean active, boolean hovered) {
        if (!active) {
            neutralButton(ctx, tr, x, y, w, h, label, hovered);
            return;
        }
        colorButton(ctx, x, y, w, h, NotchTheme.ACCENT_GREEN, NotchTheme.GREEN_HI, NotchTheme.GREEN_LO, hovered);
        ctx.fill(x + 5, y + h - 3, x + w - 5, y + h - 2, NotchTheme.HIGHLIGHT);
        ctx.drawCenteredString(tr, Component.literal(label), x + w / 2, y + (h - 8) / 2, NotchTheme.TEXT_LIGHT);
    }

    public static int pages(int size, int perPage) {
        return Math.max(1, (size + perPage - 1) / perPage);
    }

    public static int pageStart(int scroll, int size, int perPage) {
        return Math.max(0, Math.min(scroll - scroll % perPage, (pages(size, perPage) - 1) * perPage));
    }

    public static void pager(GuiGraphics ctx, Font tr, int leftX, int rightX, int y, int bw, int h,
                             int page, int pages, boolean hoverLeft, boolean hoverRight) {
        pageArrow(ctx, leftX, y, bw, h, page > 1, hoverLeft, true);
        pageArrow(ctx, rightX, y, bw, h, page < pages, hoverRight, false);
        int mx = leftX + bw + 1, mw = rightX - 1 - mx;
        inset(ctx, mx, y, mw, h, NotchTheme.DEEP);
        String s = page + " / " + pages;
        if (tr.width(s) > mw - 4) s = page + "/" + pages;
        if (tr.width(s) > mw - 4) s = String.valueOf(page);
        centerText(ctx, tr, s, mx + mw / 2, y + (h - 7) / 2, 0xFFE6E6E6, false);
    }

    private static void pageArrow(GuiGraphics ctx, int x, int y, int w, int h, boolean on, boolean hovered,
                                  boolean left) {
        if (on) {
            button(ctx, x, y, w, h, hovered, false);
        } else {
            fillRound(ctx, x, y, w, h, 1, NotchTheme.OUTLINE);
            ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF7C7C7C);
            ctx.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFF5E5E5E);
        }
        int cx = x + (w - 4) / 2 + 2, cy = y + (h - 1) / 2, color = on ? NotchTheme.TEXT_DARK : 0xFF8A8A8A;
        for (int i = 0; i < 4; i++) {
            int xx = left ? cx - 2 + i : cx + 1 - i;
            ctx.fill(xx, cy - i, xx + 1, cy + i + 1, color);
        }
    }

    public static final int COIN_GOLD = 0xFFFFE08A, COIN_GREEN = 0xFF8CEB6E;

    private static net.minecraft.world.item.ItemStack coinStack;

    public static net.minecraft.world.item.ItemStack coinStack() {
        if (coinStack == null) coinStack = new net.minecraft.world.item.ItemStack(
                net.fugginbeenus.notchcurrency.registry.ModItems.NOTCH_COIN);
        return coinStack;
    }

    public static int coinField(GuiGraphics ctx, Font tr, int x, int y, String text, int color) {
        int w = 18 + tr.width(text) - 1 + 4, fx = x + 8, fw = w - 8;
        fillRound(ctx, fx, y + 3, fw, 11, 1, NotchTheme.OUTLINE);
        ctx.fill(fx + 1, y + 4, fx + fw - 1, y + 13, NotchTheme.DEEP);
        ctx.fill(fx + 1, y + 4, fx + fw - 1, y + 5, 0xFF181818);
        ctx.renderItem(coinStack(), x, y);
        ctx.drawString(tr, text, x + 18, y + 5, color, true);
        return w;
    }

    public static void tile(GuiGraphics ctx, net.minecraft.resources.ResourceLocation tex, int x, int y, int w, int h) {
        for (int ty = 0; ty < h; ty += 16) {
            for (int tx = 0; tx < w; tx += 16) {
                int tw = Math.min(16, w - tx), th = Math.min(16, h - ty);
                //? if >=1.21.11 {
                /*ctx.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, tex,
                        x + tx, y + ty, 0f, 0f, tw, th, 16, 16);
                *///?} else {
                ctx.blit(tex, x + tx, y + ty, 0, 0, tw, th, 16, 16);
                //?}
            }
        }
    }

    public static void stampLabel(GuiGraphics ctx, int x, int ly, int w, String label) {
        int pw = smallCapsWidth(label) + 10, h = 9, ty = ly - 4;
        ctx.fill(x + 1, ty + 1, x + pw + 1, ty + h + 1, 0x30000000);
        ctx.fill(x + 1, ty + 1, x + pw - 1, ty + h - 1, NotchTheme.CREAM);
        for (int i = 0; i < pw; i += 2) {
            dot(ctx, x + i, ty, NotchTheme.CREAM);
            dot(ctx, x + i, ty + h - 1, NotchTheme.CREAM);
        }
        for (int i = 0; i < h; i += 2) {
            dot(ctx, x, ty + i, NotchTheme.CREAM);
            dot(ctx, x + pw - 1, ty + i, NotchTheme.CREAM);
        }
        smallCaps(ctx, label, x + 5, ty + 2, NotchTheme.INK);
        divider(ctx, x + pw + 3, ly, w - pw - 3);
    }

    public static void slateInset(GuiGraphics ctx, int x, int y, int w, int h) {
        fillRound(ctx, x, y, w, h, 1, NotchTheme.OUTLINE);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, NotchTheme.SLATE_DK);
        ctx.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFF0B0D11);
        ctx.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, NotchTheme.SLATE_LO);
    }

    public static void rivet(GuiGraphics ctx, int x, int y) {
        ctx.fill(x, y, x + 2, y + 2, NotchTheme.SLATE);
        dot(ctx, x, y, NotchTheme.SLATE_HI);
    }

    public static void roundBox(GuiGraphics ctx, int x, int y, int w, int h, int color) {
        fillRound(ctx, x, y, w, h, 1, color);
    }

    public static void outline(GuiGraphics ctx, int x, int y, int w, int h, int color) {
        rectLine(ctx, x, y, w, h, color);
    }

    public static int coinFieldWidth(Font tr, long amount) {
        return 18 + tr.width(compactCount(amount)) - 1 + 4;
    }

    public static int coinField(GuiGraphics ctx, Font tr, net.minecraft.world.item.ItemStack coin, int x, int y,
                                long amount, int color) {
        String s = compactCount(amount);
        int w = coinFieldWidth(tr, amount), fx = x + 8, fw = w - 8;
        fillRound(ctx, fx, y + 3, fw, 11, 1, NotchTheme.OUTLINE);
        ctx.fill(fx + 1, y + 4, fx + fw - 1, y + 13, NotchTheme.DEEP);
        ctx.fill(fx + 1, y + 4, fx + fw - 1, y + 5, 0xFF181818);
        ctx.renderItem(coin, x, y);
        ctx.drawString(tr, s, x + 18, y + 5, color, true);
        return w;
    }

    public static void badge(GuiGraphics ctx, Font tr, int x, int y, String s) {
        int w = Math.max(9, tr.width(s) + 3);
        fillRound(ctx, x, y, w, 9, 1, NotchTheme.OUTLINE);
        ctx.fill(x + 1, y + 1, x + w - 1, y + 8, 0xFFB23030);
        ctx.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFFD86060);
        centerText(ctx, tr, s, x + (w + 1) / 2, y + 1, NotchTheme.TEXT_LIGHT, false);
    }

    public static void badge(GuiGraphics ctx, Font tr, int x, int y, int count) {
        if (count > 0) badge(ctx, tr, x, y, count > 99 ? "99+" : String.valueOf(count));
    }

    private static final java.util.Map<Character, String[]> SMALL_CAPS = new java.util.HashMap<>();

    static {
        String[] src = {
            "A", ".#./#.#/###/#.#/#.#",
            "B", "##./#.#/##./#.#/##.",
            "C", ".##/#../#../#../.##",
            "D", "##./#.#/#.#/#.#/##.",
            "E", "###/#../##./#../###",
            "F", "###/#../##./#../#..",
            "G", ".##/#../#.#/#.#/.##",
            "H", "#.#/#.#/###/#.#/#.#",
            "I", "###/.#./.#./.#./###",
            "J", "..#/..#/..#/#.#/.#.",
            "K", "#.#/#.#/##./#.#/#.#",
            "L", "#../#../#../#../###",
            "M", "#...#/##.##/#.#.#/#...#/#...#",
            "N", "#..#/##.#/#.##/#..#/#..#",
            "O", ".#./#.#/#.#/#.#/.#.",
            "P", "##./#.#/##./#../#..",
            "Q", ".#./#.#/#.#/#.#/.##",
            "R", "##./#.#/##./#.#/#.#",
            "S", ".##/#../.#./..#/##.",
            "T", "###/.#./.#./.#./.#.",
            "U", "#.#/#.#/#.#/#.#/###",
            "V", "#.#/#.#/#.#/#.#/.#.",
            "W", "#...#/#...#/#.#.#/##.##/#...#",
            "X", "#.#/#.#/.#./#.#/#.#",
            "Y", "#.#/#.#/.#./.#./.#.",
            "Z", "###/..#/.#./#../###",
            "0", "###/#.#/#.#/#.#/###",
            "1", ".#./##./.#./.#./###",
            "2", "##./..#/.#./#../###",
            "3", "##./..#/.#./..#/##.",
            "4", "#.#/#.#/###/..#/..#",
            "5", "###/#../##./..#/##.",
            "6", ".##/#../###/#.#/###",
            "7", "###/..#/.#./.#./.#.",
            "8", "###/#.#/###/#.#/###",
            "9", "###/#.#/###/..#/##.",
            "/", "..#/..#/.#./#../#..",
            "-", ".../.../###/.../...",
            " ", "../../../../.."
        };
        for (int i = 0; i < src.length; i += 2) SMALL_CAPS.put(src[i].charAt(0), src[i + 1].split("/"));
    }

    private static String[] smallGlyph(char c) {
        String[] g = SMALL_CAPS.get(Character.toUpperCase(c));
        return g != null ? g : SMALL_CAPS.get(' ');
    }

    public static int smallCapsWidth(String s) {
        int w = 0;
        for (int i = 0; i < s.length(); i++) w += smallGlyph(s.charAt(i))[0].length() + 1;
        return Math.max(0, w - 1);
    }

    public static void smallCaps(GuiGraphics ctx, String s, int x, int y, int color) {
        int cx = x;
        for (int i = 0; i < s.length(); i++) {
            String[] g = smallGlyph(s.charAt(i));
            for (int r = 0; r < g.length; r++) {
                String row = g[r];
                int k = 0;
                while (k < row.length()) {
                    if (row.charAt(k) != '#') { k++; continue; }
                    int start = k;
                    while (k < row.length() && row.charAt(k) == '#') k++;
                    ctx.fill(cx + start, y + r, cx + k, y + r + 1, color);
                }
            }
            cx += g[0].length() + 1;
        }
    }

    public static void groupBox(GuiGraphics ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, 0xFFCBCBCB);
        ctx.fill(x, y, x + w, y + 1, NotchTheme.EDGE);
        ctx.fill(x, y, x + 1, y + h, NotchTheme.EDGE);
        ctx.fill(x + 1, y + 1, x + w - 1, y + 2, NotchTheme.HIGHLIGHT);
        ctx.fill(x + 1, y + 1, x + 2, y + h - 1, NotchTheme.HIGHLIGHT);
        ctx.fill(x, y + h - 1, x + w, y + h, NotchTheme.HIGHLIGHT);
        ctx.fill(x + w - 1, y, x + w, y + h, NotchTheme.HIGHLIGHT);
        ctx.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0xFF9C9C9C);
        ctx.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, 0xFF9C9C9C);
    }

    public static void groupLabel(GuiGraphics ctx, int x, int boxY, String label) {
        int h = 9, w = smallCapsWidth(label) + 10, top = boxY - 4;
        shell(ctx, x, top, w, h, NotchTheme.OUTLINE, 0xFFD8D8D8, NotchTheme.HIGHLIGHT, 0xFF8E8E8E);
        smallCaps(ctx, label, x + 5, top + 2, 0xFF3C3C3C);
    }

    public static void stamp(GuiGraphics ctx, Font tr, int cx, int y, String text, int color, int tint) {
        int w = tr.width(text) - 1 + 6, h = 11, x = cx - w / 2;
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, tint);
        rectLine(ctx, x, y, w, h, color);
        ctx.drawString(tr, text, x + 3, y + 2, color, false);
    }

    public static void disabledButton(GuiGraphics ctx, int x, int y, int w, int h) {
        fillRound(ctx, x, y, w, h, 1, NotchTheme.OUTLINE);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFFCFCFCF);
        ctx.fill(x + 2, y + 2, x + w - 2, y + h - 2, 0xFFB4B4B4);
    }

    public static void divider(GuiGraphics ctx, int x, int y, int w) {
        ctx.fill(x, y, x + w, y + 1, NotchTheme.EDGE);
        ctx.fill(x, y + 1, x + w, y + 2, NotchTheme.HIGHLIGHT);
    }

    public static String compactCount(long n) {
        if (n < 1_000) return Long.toString(n);
        if (n < 1_000_000) return compact(n, 1_000, "k");
        if (n < 1_000_000_000) return compact(n, 1_000_000, "m");
        return compact(n, 1_000_000_000, "b");
    }

    private static String compact(long n, long unit, String suffix) {
        long whole = n / unit;
        long tenth = (n % unit) * 10 / unit;
        return (whole < 10 && tenth > 0) ? whole + "." + tenth + suffix : whole + suffix;
    }

    public static String coinName() {
        String resolved = net.minecraft.network.chat.Component.translatable("item.notchcurrency.notch_coin").getString();
        return resolved.equals("Notch Coin") || resolved.equals("item.notchcurrency.notch_coin")
                ? "coins" : resolved;
    }

    public static String coinLabel() {
        String resolved = net.minecraft.network.chat.Component.translatable("item.notchcurrency.notch_coin").getString();
        return resolved.equals("item.notchcurrency.notch_coin") ? "Coin" : resolved;
    }

    public static void click() {
        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
    }

    public static void tick() {
        net.minecraft.client.Minecraft.getInstance().getSoundManager().play(
                net.minecraft.client.resources.sounds.SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(), 1.35f, 0.5f));
    }

    public static String colorize(String s) {
        return s == null ? "" : s.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
    }

    public static void arrowRight(GuiGraphics ctx, int x, int y, int color) {
        ctx.fill(x, y + 3, x + 10, y + 5, color);
        for (int i = 0; i < 4; i++) {
            ctx.fill(x + 10 + i, y + i, x + 11 + i, y + 8 - i, color);
        }
    }

    public static void triangle(GuiGraphics ctx, int cx, int cy, boolean up, int color) {
        for (int r = 0; r < 4; r++) {
            int half = up ? r : (3 - r);
            ctx.fill(cx - half, cy + r, cx + half + 1, cy + r + 1, color);
        }
    }

    public static void slider(GuiGraphics ctx, int x, int y, int w, int h, float t, boolean hovered) {
        inset(ctx, x, y, w, h, NotchTheme.DEEP);
        int cx = x + w / 2;
        ctx.fill(cx, y + 2, cx + 1, y + h - 2, NotchTheme.INSET_SHADOW);
        float clamped = Math.max(0f, Math.min(1f, t));
        int hx = x + 2 + Math.round((w - 10) * clamped);
        button(ctx, hx, y + 1, 8, h - 2, hovered, false);
    }

    private static void fillRound(GuiGraphics ctx, int x, int y, int w, int h, int r, int color) {
        fillRoundSel(ctx, x, y, w, h, r, color, true, true, true, true);
    }

    private static void fillRoundSel(GuiGraphics ctx, int x, int y, int w, int h, int r, int color,
                                     boolean tl, boolean tr, boolean bl, boolean br) {
        if (r <= 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        ctx.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int d = r - i;
            int span = (int) Math.round(Math.sqrt((double) r * r - (double) d * d));
            int ins = r - span;
            ctx.fill(x + (tl ? ins : 0), y + i, x + w - (tr ? ins : 0), y + i + 1, color);
            ctx.fill(x + (bl ? ins : 0), y + h - 1 - i, x + w - (br ? ins : 0), y + h - i, color);
        }
    }

    public static void buttonSel(GuiGraphics ctx, int x, int y, int w, int h, boolean hovered,
                                 boolean tl, boolean tr, boolean bl, boolean br) {
        final int r = 1;
        int face = hovered ? lighten(NotchTheme.PANEL_FACE) : NotchTheme.PANEL_FACE;
        fillRoundSel(ctx, x, y, w, h, r, NotchTheme.OUTLINE, tl, tr, bl, br);
        fillRoundSel(ctx, x + 1, y + 1, w - 2, h - 2, r - 1, NotchTheme.HIGHLIGHT, tl, tr, bl, br);
        fillRoundSel(ctx, x + 2, y + 2, w - 4, h - 4, r - 2, face, tl, tr, bl, br);
    }

    private static int lighten(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 16);
        int g = Math.min(255, ((argb >> 8) & 0xFF) + 16);
        int b = Math.min(255, (argb & 0xFF) + 16);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    public static Component priceText(long coinPrice, String barterName, int barterCount) {
        MutableComponent t = Component.empty();
        boolean any = false;
        if (coinPrice > 0) {
            t.append(NotchCurrency.coins(coinPrice));
            any = true;
        }
        if (barterCount > 0 && barterName != null && !barterName.isEmpty()) {
            if (any) t.append(Component.literal(" + "));
            t.append(Component.literal(barterCount + "×" + barterName));
            any = true;
        }
        return any ? t : Component.literal("free");
    }

    public static boolean typingInField(int keyCode, int scanCode, int modifiers, EditBox... fields) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) return false;
        for (EditBox f : fields) {
            if (f != null && f.isFocused() && f.isVisible()) {
                boolean combo = (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0;
                boolean editKey = combo
                        || keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE
                        || keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT
                        || keyCode == GLFW.GLFW_KEY_HOME || keyCode == GLFW.GLFW_KEY_END
                        || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER;
                //? if >=1.21.11 {
                /*if (editKey) f.keyPressed(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, modifiers));
                *///?} else {
                if (editKey) f.keyPressed(keyCode, scanCode, modifiers);
                //?}
                return true;
            }
        }
        return false;
    }

    public static boolean typingInEditBox(int keyCode, int scanCode, int modifiers,
                                          net.minecraft.client.gui.components.MultiLineEditBox box) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) return false;
        if (box == null || !box.isFocused() || !box.visible) return false;
        boolean combo = (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0;
        boolean editKey = combo
                || keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE
                || keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT
                || keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN
                || keyCode == GLFW.GLFW_KEY_PAGE_UP || keyCode == GLFW.GLFW_KEY_PAGE_DOWN
                || keyCode == GLFW.GLFW_KEY_HOME || keyCode == GLFW.GLFW_KEY_END
                || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER;
        //? if >=1.21.11 {
        /*if (editKey) box.keyPressed(new net.minecraft.client.input.KeyEvent(keyCode, scanCode, modifiers));
        *///?} else {
        if (editKey) box.keyPressed(keyCode, scanCode, modifiers);
        //?}
        return true;
    }
}
