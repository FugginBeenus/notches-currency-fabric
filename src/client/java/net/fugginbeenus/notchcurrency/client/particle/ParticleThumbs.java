package net.fugginbeenus.notchcurrency.client.particle;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class ParticleThumbs {

    private ParticleThumbs() {}

    private record Thumb(ResourceLocation texture, int width, int height) {}

    private static final Map<String, Thumb> CACHE = new HashMap<>();
    private static final Thumb NONE = new Thumb(null, 0, 0);

    private static Thumb lookup(String particleId) {
        Thumb cached = CACHE.get(particleId);
        if (cached != null) return cached;
        Thumb found = NONE;
        try {
            ResourceLocation id = ResourceLocation.tryParse(particleId);
            if (id != null) {
                ResourceLocation json = ResourceLocation.tryParse(id.getNamespace() + ":particles/" + id.getPath() + ".json");
                var res = Minecraft.getInstance().getResourceManager().getResource(json);
                if (res.isPresent()) {
                    JsonObject root;
                    try (InputStream in = res.get().open()) {
                        root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
                    }
                    var textures = root.getAsJsonArray("textures");
                    if (textures != null && !textures.isEmpty()) {
                        ResourceLocation sprite = ResourceLocation.tryParse(textures.get(0).getAsString());
                        if (sprite != null) {
                            ResourceLocation png = ResourceLocation.tryParse(
                                    sprite.getNamespace() + ":textures/particle/" + sprite.getPath() + ".png");
                            var pngRes = Minecraft.getInstance().getResourceManager().getResource(png);
                            if (pngRes.isPresent()) {
                                try (InputStream in = pngRes.get().open();
                                     com.mojang.blaze3d.platform.NativeImage img =
                                             com.mojang.blaze3d.platform.NativeImage.read(in)) {
                                    found = new Thumb(png, img.getWidth(), img.getHeight());
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
        CACHE.put(particleId, found);
        return found;
    }

    public static void forget() {
        CACHE.clear();
    }

    public static boolean has(String particleId) {
        return lookup(particleId).texture() != null;
    }

    public static void draw(GuiGraphics ctx, String particleId, int x, int y, int size) {
        Thumb t = lookup(particleId);
        if (t.texture() == null) return;
        int frame = Math.min(t.width(), t.height());
        //? if >=1.21.11 {
        /*ctx.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, t.texture(),
                x, y, 0f, 0f, size, size, frame, frame, t.width(), t.height());
        *///?} else {
        ctx.blit(t.texture(), x, y, size, size, 0f, 0f, frame, frame, t.width(), t.height());
        //?}
    }
}
