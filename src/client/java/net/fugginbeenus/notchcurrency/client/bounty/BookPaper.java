package net.fugginbeenus.notchcurrency.client.bounty;

import com.mojang.blaze3d.platform.NativeImage;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fugginbeenus.notchcurrency.compat.Reg;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public final class BookPaper implements SimpleSynchronousResourceReloadListener {

    public static final BookPaper LOADER = new BookPaper();
    private static final ResourceLocation BOOK = Reg.id("minecraft", "textures/gui/book.png");
    private static Sheet current = new Sheet(null, 0, 0);

    private BookPaper() {}

    public static Sheet sheet() {
        return current;
    }

    @Override
    public ResourceLocation getFabricId() {
        return NotchCurrency.id("book_paper");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        int[] pixels = null;
        int size = 0;
        try {
            Optional<Resource> res = manager.getResource(BOOK);
            if (res.isPresent()) {
                try (InputStream in = res.get().open(); NativeImage img = NativeImage.read(in)) {
                    if (img.getWidth() == img.getHeight() && img.getWidth() % 256 == 0) {
                        size = img.getWidth();
                        pixels = new int[size * size];
                        for (int y = 0; y < size; y++) {
                            for (int x = 0; x < size; x++) pixels[y * size + x] = PaperNote.read(img, x, y);
                        }
                    }
                }
            }
        } catch (Exception e) {
            pixels = null;
        }
        current = new Sheet(pixels, size, current.version + 1);
    }

    static float lum(int c) {
        return 0.2126f * ((c >> 16) & 255) + 0.7152f * ((c >> 8) & 255) + 0.0722f * (c & 255);
    }

    static int mix(int a, int b, float t) {
        int r = Math.round(((a >> 16) & 255) + (((b >> 16) & 255) - ((a >> 16) & 255)) * t);
        int g = Math.round(((a >> 8) & 255) + (((b >> 8) & 255) - ((a >> 8) & 255)) * t);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    public static final class Sheet {
        private final int[] pixels;
        private final int size;
        public final int scale, version;
        public final boolean curl;
        public final int body, edge, edgeLit, edgeMid, tear, fringe, band, half, ink, faint;

        Sheet(int[] pixels, int size, int version) {
            this.pixels = pixels;
            this.size = size;
            this.scale = pixels == null ? 1 : size / 256;
            this.version = version;
            int b = pixels == null ? 0xFFF2E6C6 : commonest();
            int l = pick(60, 172, b), bd = pick(60, 170, b), hf = pick(60, 168, b);
            if (!(lum(l) < lum(b) - 50 && lum(l) < lum(bd) && lum(bd) <= lum(hf) && lum(hf) <= lum(b))) {
                l = 0xFF000000 | (Math.round(((b >> 16) & 255) * 0.58f) << 16)
                        | (Math.round(((b >> 8) & 255) * 0.46f) << 8) | Math.round((b & 255) * 0.38f);
                bd = 0xFF000000 | (Math.round(((b >> 16) & 255) + (((l >> 16) & 255) - ((b >> 16) & 255)) * 0.06f) << 16)
                        | (Math.round(((b >> 8) & 255) + (((l >> 8) & 255) - ((b >> 8) & 255)) * 0.09f) << 8)
                        | Math.round((b & 255) + ((l & 255) - (b & 255)) * 0.2f);
                hf = mix(b, bd, 0.5f);
            }
            this.body = b;
            this.edge = l;
            this.band = bd;
            this.half = hf;
            this.edgeLit = mix(l, b, 0.34f);
            this.edgeMid = mix(l, b, 0.17f);
            this.tear = mix(l, b, 0.08f);
            this.fringe = mix(l, b, 0.5f);
            if (lum(b) >= 110) {
                this.ink = mix(l, 0xFF000000, 0.58f);
                this.faint = mix(l, 0xFF000000, 0.07f);
            } else {
                this.ink = mix(b, 0xFFFFFFFF, 0.85f);
                this.faint = mix(b, 0xFFFFFFFF, 0.5f);
            }
            float dark = lum(b) - 60;
            this.curl = pixels != null && lum(pick(147, 171, b)) < dark && lum(pick(148, 171, b)) < dark
                    && lum(pick(147, 163, b)) > lum(b) - 15;
        }

        public int at(int u, int v) {
            return texel(u * scale, v * scale);
        }

        public int texel(int x, int y) {
            if (pixels == null || x < 0 || y < 0 || x >= size || y >= size) return body;
            return pixels[y * size + x] | 0xFF000000;
        }

        private int pick(int u, int v, int fallback) {
            return pixels == null ? fallback : pixels[v * scale * size + u * scale] | 0xFF000000;
        }

        private int commonest() {
            Map<Integer, Integer> counts = new HashMap<>();
            int best = 0xFFF2E6C6, bestN = 0;
            for (int v = 20; v < 150; v += 2) {
                for (int u = 40; u < 140; u += 2) {
                    int c = pixels[v * scale * size + u * scale] | 0xFF000000;
                    int n = counts.merge(c, 1, Integer::sum);
                    if (n > bestN) {
                        bestN = n;
                        best = c;
                    }
                }
            }
            return best;
        }
    }
}
