package net.fugginbeenus.notchcurrency.client.ui;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.shop.SignStyles;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class Signs implements SimpleSynchronousResourceReloadListener {

    public static final Signs LOADER = new Signs();
    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-Signs");
    private static final String FOLDER = "textures/gui/signs";
    private static final Map<String, Sign> BY_ID = new HashMap<>();
    private static List<Sign> sorted = List.of();

    public record Sign(String id, String name, int order, ResourceLocation texture, int texW, int texH,
                       int frames, int frameTime, int left, int right, int padding, int top, int textY,
                       int color, boolean shadow, int emboss, int outline) {

        public int frameHeight() {
            return texH / frames;
        }

        public int frameV() {
            return frames <= 1 ? 0 : (int) (System.currentTimeMillis() / (50L * frameTime) % frames) * frameHeight();
        }
    }

    private Signs() {}

    @Override
    public ResourceLocation getFabricId() {
        return NotchCurrency.id("signs");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        Map<String, Sign> found = new HashMap<>();
        for (Map.Entry<ResourceLocation, Resource> e : manager
                .listResources(FOLDER, path -> path.getPath().endsWith(".png")).entrySet()) {
            ResourceLocation tex = e.getKey();
            String path = tex.getPath();
            String id = path.substring(FOLDER.length() + 1, path.length() - 4);
            if (!NotchCurrency.MOD_ID.equals(tex.getNamespace()) || !SignStyles.valid(id)) continue;
            try {
                int[] size = pngSize(e.getValue());
                JsonObject meta = readMeta(manager, path.substring(0, path.length() - 4) + ".json");
                found.put(id, parse(id, tex, size[0], size[1], meta));
            } catch (Exception ex) {
                LOGGER.warn("Failed to load sign {}: {}", id, ex.getMessage());
            }
        }
        BY_ID.clear();
        BY_ID.putAll(found);
        List<Sign> list = new ArrayList<>(found.values());
        list.sort(Comparator.comparingInt(Sign::order).thenComparing(Sign::id));
        sorted = List.copyOf(list);
    }

    public static List<Sign> all() {
        return sorted;
    }

    public static Sign find(String id, String fallback) {
        Sign s = BY_ID.get(id);
        return s != null ? s : BY_ID.get(fallback);
    }

    public static String name(String id, String fallback) {
        Sign s = find(id, fallback);
        return s != null ? s.name() : id;
    }

    public static void draw(GuiGraphics ctx, Font font, String id, String fallback, String label, int cx, int top) {
        Sign s = find(id, fallback);
        if (s == null) {
            NotchWidgets.plate(ctx, font, NotchWidgets.Plate.PLAIN, label, cx, top);
            return;
        }
        int w = Math.max(s.left() + s.right() + 1, font.width(label) + s.padding() * 2);
        int x = cx - w / 2, y = top - s.top(), mid = s.texW() - s.left() - s.right();
        int v = s.frameV(), h = s.frameHeight();
        NotchWidgets.blit(ctx, s.texture(), x, y, 0, v, s.left(), h, s.texW(), s.texH());
        for (int dx = s.left(); dx < w - s.right(); dx += mid) {
            NotchWidgets.blit(ctx, s.texture(), x + dx, y, s.left(), v,
                    Math.min(mid, w - s.right() - dx), h, s.texW(), s.texH());
        }
        NotchWidgets.blit(ctx, s.texture(), x + w - s.right(), y, s.texW() - s.right(), v,
                s.right(), h, s.texW(), s.texH());
        int tx = x + s.padding(), ty = top + s.textY();
        if (s.outline() != 0) {
            ctx.drawString(font, label, tx - 1, ty, s.outline(), false);
            ctx.drawString(font, label, tx + 1, ty, s.outline(), false);
            ctx.drawString(font, label, tx, ty - 1, s.outline(), false);
            ctx.drawString(font, label, tx, ty + 1, s.outline(), false);
        }
        if (s.emboss() != 0) ctx.drawString(font, label, tx, ty + 1, s.emboss(), false);
        ctx.drawString(font, label, tx, ty, s.color(), s.shadow());
    }

    private static int[] pngSize(Resource res) throws IOException {
        try (InputStream in = res.open()) {
            byte[] b = in.readNBytes(24);
            if (b.length < 24) throw new IOException("not a PNG");
            return new int[]{readInt(b, 16), readInt(b, 20)};
        }
    }

    private static int readInt(byte[] b, int at) {
        return ((b[at] & 255) << 24) | ((b[at + 1] & 255) << 16) | ((b[at + 2] & 255) << 8) | (b[at + 3] & 255);
    }

    private static JsonObject readMeta(ResourceManager manager, String path) throws IOException {
        Optional<Resource> res = manager.getResource(NotchCurrency.id(path));
        if (res.isEmpty()) return new JsonObject();
        try (InputStream in = res.get().open(); InputStreamReader reader = new InputStreamReader(in)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }

    private static Sign parse(String id, ResourceLocation tex, int w, int h, JsonObject o) {
        int left = num(o, "left", 4), right = num(o, "right", 4);
        if (left < 0 || right < 0 || left + right >= w) {
            left = w / 3;
            right = w / 3;
        }
        int frames = Math.max(1, num(o, "frames", 1));
        if (h % frames != 0) frames = 1;
        return new Sign(id, text(o, "name", pretty(id)), num(o, "order", 100), tex, w, h,
                frames, Math.max(1, num(o, "frametime", 1)), left, right,
                num(o, "padding", 8), num(o, "top", 0), num(o, "text_y", 4),
                color(o, "color", 0xFFFFFFFF), bool(o, "shadow", true), color(o, "emboss", 0), color(o, "outline", 0));
    }

    private static int num(JsonObject o, String key, int fallback) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isNumber() ? e.getAsInt() : fallback;
    }

    private static boolean bool(JsonObject o, String key, boolean fallback) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() && e.getAsJsonPrimitive().isBoolean() ? e.getAsBoolean() : fallback;
    }

    private static String text(JsonObject o, String key, String fallback) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : fallback;
    }

    private static int color(JsonObject o, String key, int fallback) {
        String s = text(o, key, null);
        if (s == null) return fallback;
        try {
            return 0xFF000000 | Integer.parseInt(s.startsWith("#") ? s.substring(1) : s, 16);
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private static String pretty(String id) {
        String spaced = id.replace('_', ' ');
        return Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
