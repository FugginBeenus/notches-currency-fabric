package net.fugginbeenus.notchcurrency.client.bounty;

import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.compat.Reg;
import net.fugginbeenus.notchcurrency.compat.Render;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

public final class BoardArt {

    public static final int FRAME = 0xFF1E140C, LABEL = 0xFFEADFC8, LABEL_SHADOW = 0xFF1A120B, WANTED = 0xFF7A2E22;
    private static final ResourceLocation LOG = Reg.id("minecraft", "textures/block/stripped_oak_log.png");
    private static final ResourceLocation CORK = Reg.id("minecraft", "textures/block/brown_concrete_powder.png");

    private BoardArt() {}

    public static void frame(GuiGraphics ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, FRAME);
        logAcross(ctx, x + 1, y + 1, w - 2, 0);
        logAcross(ctx, x + 1, y + h - 5, w - 2, 12);
        logDown(ctx, x + 1, y + 5, h - 10, 0);
        logDown(ctx, x + w - 5, y + 5, h - 10, 12);
        ctx.fill(x + 1, y + 1, x + w - 1, y + 2, 0x40FFFFFF);
        ctx.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x50000000);
    }

    public static void logAcross(GuiGraphics ctx, int x, int y, int len, int u) {
        for (int dx = 0; dx < len; dx += 16) {
            Render.pushGui(ctx);
            Render.translateGui(ctx, x + dx, y + 4);
            Render.rotateGui(ctx, -90f);
            NotchWidgets.blit(ctx, LOG, 0, 0, u, 0, 4, Math.min(16, len - dx), 16, 16);
            Render.popGui(ctx);
        }
    }

    public static void logDown(GuiGraphics ctx, int x, int y, int len, int u) {
        for (int dy = 0; dy < len; dy += 16) {
            NotchWidgets.blit(ctx, LOG, x, y + dy, u, 0, 4, Math.min(16, len - dy), 16, 16);
        }
    }

    public static void cork(GuiGraphics ctx, int x, int y, int w, int h) {
        NotchWidgets.tile(ctx, CORK, x, y, w, h);
        ctx.fill(x, y, x + w, y + h, 0xB2523E2A);
        ctx.fillGradient(x, y, x + w, y + h, 0x10FFFFFF, 0x00FFFFFF);
        ctx.fillGradient(x, y, x + w, y + h, 0x00000000, 0x18000000);
        int[] near = {0x88, 0x64, 0x4A, 0x36, 0x26, 0x1A, 0x10, 0x08};
        for (int i = 0; i < near.length; i++) {
            ctx.fill(x, y + i, x + w, y + i + 1, near[i] << 24);
            ctx.fill(x + i, y, x + i + 1, y + h, near[i] << 24);
        }
        int[] far = {0x4C, 0x34, 0x22, 0x14, 0x0A};
        for (int i = 0; i < far.length; i++) {
            ctx.fill(x, y + h - 1 - i, x + w, y + h - i, far[i] << 24);
            ctx.fill(x + w - 1 - i, y, x + w - i, y + h, far[i] << 24);
        }
    }

    public static void label(GuiGraphics ctx, String s, int x, int y) {
        NotchWidgets.smallCaps(ctx, s, x + 1, y + 1, LABEL_SHADOW);
        NotchWidgets.smallCaps(ctx, s, x, y, LABEL);
    }

    public static void pin(GuiGraphics ctx, int cx, int y) {
        ctx.fill(cx, y + 2, cx + 2, y + 3, 0x55000000);
        ctx.fill(cx - 1, y - 1, cx + 2, y + 2, 0xFF7A7F8A);
        ctx.fill(cx - 1, y - 1, cx + 1, y + 1, 0xFFD8DCE4);
        ctx.fill(cx + 1, y + 1, cx + 2, y + 2, 0xFF3A3E46);
    }
}
