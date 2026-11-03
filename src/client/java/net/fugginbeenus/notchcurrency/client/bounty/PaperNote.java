package net.fugginbeenus.notchcurrency.client.bounty;

import com.mojang.blaze3d.platform.NativeImage;
import net.fugginbeenus.notchcurrency.compat.Reg;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class PaperNote {

    private static final int PAGE_R = 152, PAGE_B = 171, CURL_U = 143, CURL_V = 160;
    private static final int[][] BEYOND = {{9, 6}, {9, 7}, {8, 8}, {9, 8}, {8, 9}, {9, 9}, {7, 10}, {8, 10},
            {9, 10}, {6, 11}, {7, 11}, {8, 11}, {9, 11}};
    private static final int SHADOW = 0x78000000;
    private static int serial;

    private final ResourceLocation id;
    private final int w, h, texW, texH;
    private final int[] shadow;

    private PaperNote(ResourceLocation id, int w, int h, int scale, int[] shadow) {
        this.id = id;
        this.w = w;
        this.h = h;
        this.texW = w * scale;
        this.texH = h * scale;
        this.shadow = shadow;
    }

    public void draw(GuiGraphics ctx, int x, int y) {
        for (int k = 0; k < shadow.length; k += 3) {
            ctx.fill(x + shadow[k], y + shadow[k + 2], x + shadow[k + 1], y + shadow[k + 2] + 1, SHADOW);
        }
        //? if >=1.21.11 {
        /*ctx.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, id, x, y, 0f, 0f, w, h,
                texW, texH, texW, texH);
        *///?} else {
        ctx.blit(id, x, y, w, h, 0f, 0f, texW, texH, texW, texH);
        //?}
    }

    public void release() {
        Minecraft.getInstance().getTextureManager().release(id);
    }

    public static PaperNote bake(long seed, int w, int h, BookPaper.Sheet sheet) {
        Random rng = new Random(seed);
        boolean curl = sheet.curl && w >= 40 && h >= 18 && rng.nextInt(4) != 0;
        int[] top = walk(rng, w, 6, 14, 1);
        int[] bot = walk(rng, w, 6, 14, 1);
        int[] rig = walk(rng, h, 5, 12, 1);
        int[] lef = walk(rng, h, 3, 8, 2);
        for (int k = Math.max(0, w / 2 - 4); k < Math.min(w, w / 2 + 5); k++) top[k] = 0;
        if (curl) {
            for (int k = Math.max(0, w - 24); k < w; k++) bot[k] = 1;
            for (int k = Math.max(0, h - 24); k < h; k++) rig[k] = 1;
        }
        int nick = -1;
        if (w > 40 && rng.nextBoolean()) {
            List<Integer> spots = new ArrayList<>();
            for (int i = 6; i < w - 16; i++) if (Math.abs(i + 2 - w / 2) > 7) spots.add(i);
            nick = spots.get(rng.nextInt(spots.size()));
            flat(top, nick - 1, nick + 5);
        }

        boolean[] on = new boolean[w * h];
        Arrays.fill(on, true);
        for (int i = 0; i < w; i++) {
            for (int r = 0; r < Math.min(h, top[i]); r++) on[r * w + i] = false;
            for (int r = 0; r < Math.min(h, bot[i]); r++) on[(h - 1 - r) * w + i] = false;
        }
        for (int j = 0; j < h; j++) {
            for (int c = 0; c < Math.min(w, lef[j]); c++) on[j * w + c] = false;
            for (int c = 0; c < Math.min(w, rig[j]); c++) on[j * w + w - 1 - c] = false;
        }
        bite(rng, on, w, h, false, false, Math.max(top[0], lef[0]));
        bite(rng, on, w, h, true, false, Math.max(top[w - 1], rig[0]));
        bite(rng, on, w, h, false, true, Math.max(bot[0], lef[h - 1]));
        if (!curl) bite(rng, on, w, h, true, true, Math.max(bot[w - 1], rig[h - 1]));

        int rx0 = w - 11, ry0 = h - 13;
        boolean[] beyond = new boolean[w * h];
        if (curl) {
            for (int[] p : BEYOND) {
                int k = (ry0 + p[1]) * w + rx0 + p[0];
                on[k] = false;
                beyond[k] = true;
            }
        }

        int[] sprite = new int[w * h];
        if (nick >= 0) {
            int e = top[nick];
            on[e * w + nick + 2] = false;
            put(sprite, w, h, nick, e, sheet.fringe);
            put(sprite, w, h, nick + 1, e, sheet.edge);
            put(sprite, w, h, nick + 3, e, sheet.body);
            put(sprite, w, h, nick + 1, e + 1, sheet.fringe);
            put(sprite, w, h, nick + 2, e + 1, sheet.edge);
            put(sprite, w, h, nick + 3, e + 1, sheet.body);
            put(sprite, w, h, nick + 2, e + 2, sheet.fringe);
        }

        List<int[]> teeth = new ArrayList<>();
        int[] skipNick = nick >= 0 ? new int[]{nick - 2, nick + 6} : new int[]{-9, -9};
        for (int i : stops(rng, w, 8, 15, new int[]{w / 2 - 5, w / 2 + 6}, skipNick, new int[]{0, 4})) {
            int e = -1;
            for (int j = 0; j < h && e < 0; j++) if (on[j * w + i]) e = j;
            if (e >= 0 && flatAt(on, w, h, i - 1, e, 0, -1) && flatAt(on, w, h, i + 1, e, 0, -1)) {
                teeth.add(new int[]{i, e, 0, 1});
            }
        }
        for (int i : stops(rng, w, 8, 15, curl ? new int[]{w - 27, w} : new int[]{w - 4, w}, new int[]{0, 4})) {
            int e = -1;
            for (int j = h - 1; j >= 0 && e < 0; j--) if (on[j * w + i]) e = j;
            if (e >= 0 && flatAt(on, w, h, i - 1, e, 0, 1) && flatAt(on, w, h, i + 1, e, 0, 1)) {
                teeth.add(new int[]{i, e, 0, -1});
            }
        }
        for (int j : stops(rng, h, 4, 8, new int[]{0, 4}, new int[]{h - 4, h})) {
            int e = -1;
            for (int i = 0; i < w && e < 0; i++) if (on[j * w + i]) e = i;
            if (e >= 0 && flatAt(on, w, h, e, j - 1, -1, 0) && flatAt(on, w, h, e, j + 1, -1, 0)) {
                teeth.add(new int[]{e, j, 1, 0});
            }
        }
        for (int j : stops(rng, h, 7, 13, curl ? new int[]{h - 27, h} : new int[]{h - 4, h}, new int[]{0, 4})) {
            int e = -1;
            for (int i = w - 1; i >= 0 && e < 0; i--) if (on[j * w + i]) e = i;
            if (e >= 0 && flatAt(on, w, h, e, j - 1, 1, 0) && flatAt(on, w, h, e, j + 1, 1, 0)) {
                teeth.add(new int[]{e, j, -1, 0});
            }
        }
        int lit = teeth.isEmpty() ? -1 : rng.nextInt(teeth.size());
        for (int k = 0; k < teeth.size(); k++) {
            int[] t = teeth.get(k);
            int n = rng.nextInt(3) == 2 ? 2 : 1;
            put(sprite, w, h, t[0], t[1], sheet.edge);
            put(sprite, w, h, t[0] + t[2], t[1] + t[3], sheet.edge);
            if (n == 2) put(sprite, w, h, t[0] + 2 * t[2], t[1] + 2 * t[3], sheet.tear);
            if (k == lit) put(sprite, w, h, t[0] + (n + 1) * t[2], t[1] + (n + 1) * t[3], sheet.body);
        }

        int sw = w + 2, sh = h + 2;
        boolean[] dark = new boolean[sw * sh];
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                if (!on[j * w + i]) continue;
                dark[(j + 1) * sw + i + 1] = true;
                if (curl && i >= rx0 && j >= ry0) dark[(j + 2) * sw + i + 2] = true;
            }
        }
        List<Integer> runs = new ArrayList<>();
        for (int y = 0; y < sh; y++) {
            int x = 0;
            while (x < sw) {
                while (x < sw && (!dark[y * sw + x] || has(on, w, h, x, y))) x++;
                int a = x;
                while (x < sw && dark[y * sw + x] && !has(on, w, h, x, y)) x++;
                if (x > a) {
                    runs.add(a);
                    runs.add(x);
                    runs.add(y);
                }
            }
        }

        int s = sheet.scale;
        NativeImage img = new NativeImage(w * s, h * s, true);
        for (int j = 0; j < h; j++) {
            for (int i = 0; i < w; i++) {
                int k = j * w + i;
                if (!on[k]) continue;
                if (curl && i >= rx0 && j >= ry0) {
                    int cu = CURL_U + i - rx0, cv = CURL_V + j - ry0;
                    boolean outR = !has(on, w, h, i + 1, j) && !(i + 1 < w && beyond[k + 1]);
                    boolean outD = !has(on, w, h, i, j + 1) && !(j + 1 < h && beyond[k + w]);
                    if ((outR || outD) && BookPaper.lum(sheet.at(cu, cv)) > BookPaper.lum(sheet.tear)) {
                        solid(img, i, j, s, sheet.edge);
                    } else {
                        copy(img, i, j, s, sheet, cu, cv, 0);
                    }
                    continue;
                }
                if (sprite[k] != 0) {
                    solid(img, i, j, s, sprite[k]);
                    continue;
                }
                boolean up = !has(on, w, h, i, j - 1), left = !has(on, w, h, i - 1, j);
                boolean down = !has(on, w, h, i, j + 1), right = !has(on, w, h, i + 1, j);
                if (up || left || down || right) {
                    boolean litSide = up || left, darkSide = down || right;
                    solid(img, i, j, s, litSide && darkSide ? sheet.edgeMid : darkSide ? sheet.edge : sheet.edgeLit);
                    continue;
                }
                int tint = exposed(on, w, h, i, j, 2) || exposed(on, w, h, i, j, 3) ? sheet.band
                        : exposed(on, w, h, i, j, 4) ? sheet.half : 0;
                copy(img, i, j, s, sheet, pageU(i, w, curl), PAGE_B - h + 1 + j, tint);
            }
        }

        ResourceLocation id = Reg.id("bounty_note/" + serial++);
        //? if >=1.21.11 {
        /*DynamicTexture tex = new DynamicTexture(() -> "notchcurrency bounty note", img);
        *///?} else {
        DynamicTexture tex = new DynamicTexture(img);
        //?}
        Minecraft.getInstance().getTextureManager().register(id, tex);
        int[] packed = new int[runs.size()];
        for (int k = 0; k < packed.length; k++) packed[k] = runs.get(k);
        return new PaperNote(id, w, h, s, packed);
    }

    static int read(NativeImage img, int x, int y) {
        //? if >=1.21.11 {
        /*return img.getPixel(x, y);
        *///?} else {
        return swap(img.getPixelRGBA(x, y));
        //?}
    }

    private static void write(NativeImage img, int x, int y, int argb) {
        //? if >=1.21.11 {
        /*img.setPixel(x, y, argb);
        *///?} else {
        img.setPixelRGBA(x, y, swap(argb));
        //?}
    }

    private static int swap(int c) {
        return (c & 0xFF00FF00) | ((c >> 16) & 0xFF) | ((c & 0xFF) << 16);
    }

    private static void solid(NativeImage img, int i, int j, int s, int color) {
        for (int b = 0; b < s; b++) {
            for (int a = 0; a < s; a++) write(img, i * s + a, j * s + b, color);
        }
    }

    private static void copy(NativeImage img, int i, int j, int s, BookPaper.Sheet sheet, int u, int v, int tint) {
        for (int b = 0; b < s; b++) {
            for (int a = 0; a < s; a++) {
                int c = sheet.texel(u * s + a, v * s + b);
                if (tint != 0 && BookPaper.lum(tint) < BookPaper.lum(c)) c = tint;
                write(img, i * s + a, j * s + b, c);
            }
        }
    }

    private static int pageU(int i, int w, boolean curl) {
        int right = curl ? PAGE_R : 140;
        if (w <= 106) return right - w + 1 + i;
        return i >= w - 40 ? right - (w - 1 - i) : 34 + i % 80;
    }

    private static int[] walk(Random rng, int n, int lo, int hi, int deep) {
        int[] d = new int[n];
        int cur = rng.nextInt(deep + 1), i = 0;
        while (i < n) {
            int r = lo + rng.nextInt(hi - lo + 1);
            for (int k = i; k < Math.min(n, i + r); k++) d[k] = cur;
            i += r;
            int next = rng.nextInt(deep);
            cur = next >= cur ? next + 1 : next;
        }
        return d;
    }

    private static void flat(int[] d, int a, int b) {
        int v = d[Math.max(0, a)];
        for (int k = Math.max(0, a); k < Math.min(d.length, b); k++) d[k] = v;
    }

    private static void bite(Random rng, boolean[] on, int w, int h, boolean fromRight, boolean fromBottom, int base) {
        int roll = rng.nextInt(10), reach = (roll < 6 ? 1 : roll < 9 ? 2 : 3) + base;
        for (int dy = 0; dy < Math.min(h, reach); dy++) {
            for (int dx = 0; dx + dy < reach && dx < w; dx++) {
                int i = fromRight ? w - 1 - dx : dx, j = fromBottom ? h - 1 - dy : dy;
                on[j * w + i] = false;
            }
        }
    }

    private static List<Integer> stops(Random rng, int n, int lo, int hi, int[]... avoid) {
        List<Integer> out = new ArrayList<>();
        int p = 3 + rng.nextInt(hi - 2);
        while (p < n - 4) {
            boolean skip = false;
            for (int[] a : avoid) if (p >= a[0] && p < a[1]) skip = true;
            if (!skip) out.add(p);
            p += lo + rng.nextInt(hi - lo + 1);
        }
        return out;
    }

    private static boolean flatAt(boolean[] on, int w, int h, int i, int j, int di, int dj) {
        return has(on, w, h, i, j) && !has(on, w, h, i + di, j + dj);
    }

    private static boolean has(boolean[] on, int w, int h, int i, int j) {
        return i >= 0 && j >= 0 && i < w && j < h && on[j * w + i];
    }

    private static boolean exposed(boolean[] on, int w, int h, int i, int j, int r) {
        return !has(on, w, h, i, j - r) || !has(on, w, h, i - r, j) || !has(on, w, h, i, j + r) || !has(on, w, h, i + r, j);
    }

    private static void put(int[] sprite, int w, int h, int i, int j, int color) {
        if (i >= 0 && j >= 0 && i < w && j < h) sprite[j * w + i] = color;
    }
}
