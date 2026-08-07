package net.fugginbeenus.notchcurrency.client.npc;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.HttpTexture;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class NpcSkins {

    public static final int PRESET_COUNT = 12;
    private static final ResourceLocation[] PRESETS = new ResourceLocation[PRESET_COUNT];
    static {
        for (int i = 0; i < PRESET_COUNT; i++) {
            PRESETS[i] = NotchCurrency.id("textures/skins/preset_" + (i + 1) + ".png");
        }
    }
    private static final ResourceLocation DEFAULT = DefaultPlayerSkin.getDefaultSkin();

    private static final Map<String, ResourceLocation> cache = new HashMap<>();
    private static final Map<String, Boolean> loading = new HashMap<>();

    private NpcSkins() {}

    public static ResourceLocation resolve(NotchNpcEntity npc) {
        return switch (npc.getSkinType()) {
            case NotchNpcEntity.SKIN_PLAYER -> player(npc.getSkinValue());
            case NotchNpcEntity.SKIN_URL -> url(npc.getSkinValue());
            default -> preset(npc.getSkinValue());
        };
    }

    public static ResourceLocation preset(String value) {
        try {
            int i = Integer.parseInt(value) - 1;
            if (i >= 0 && i < PRESET_COUNT) return PRESETS[i];
        } catch (NumberFormatException ignored) {}
        return PRESETS[0];
    }

    private static ResourceLocation url(String url) {
        if (url == null || url.isEmpty()) return DEFAULT;
        ResourceLocation cached = cache.get("url:" + url);
        if (cached != null) return cached;
        if (Boolean.TRUE.equals(loading.get("url:" + url))) return DEFAULT;
        loading.put("url:" + url, true);
        Minecraft.getInstance().execute(() -> {
            try {
                ResourceLocation id = NotchCurrency.id("skins/url/" + Integer.toHexString(url.hashCode()));
                Minecraft.getInstance().getTextureManager().register(id,
                        new HttpTexture(null, url, DEFAULT, true, () -> {
                            cache.put("url:" + url, id);
                            loading.put("url:" + url, false);
                        }));
            } catch (Exception e) {
                loading.put("url:" + url, false);
            }
        });
        return DEFAULT;
    }

    private static ResourceLocation player(String username) {
        if (username == null || username.isEmpty()) return DEFAULT;
        String key = "player:" + username.toLowerCase();
        ResourceLocation cached = cache.get(key);
        if (cached != null) return cached;
        if (Boolean.TRUE.equals(loading.get(key))) return DEFAULT;
        loading.put(key, true);
        loadPlayerAsync(username, key);
        return DEFAULT;
    }

    private static void loadPlayerAsync(String username, String key) {
        CompletableFuture.runAsync(() -> {
            try {
                java.net.URL api = new java.net.URL("https://api.mojang.com/users/profiles/minecraft/" + username);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) api.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "NotchCurrency-Mod");
                if (conn.getResponseCode() != 200) { loading.put(key, false); return; }
                StringBuilder sb = new StringBuilder();
                try (java.io.BufferedReader r = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()))) {
                    String line;
                    while ((line = r.readLine()) != null) sb.append(line);
                }
                conn.disconnect();
                String json = sb.toString();
                int idIdx = json.indexOf("\"id\"");
                if (idIdx < 0) { loading.put(key, false); return; }
                int colon = json.indexOf(":", idIdx);
                int s = json.indexOf("\"", colon + 1);
                int e = json.indexOf("\"", s + 1);
                String raw = json.substring(s + 1, e);
                if (raw.length() == 32) {
                    raw = raw.substring(0, 8) + "-" + raw.substring(8, 12) + "-" + raw.substring(12, 16)
                            + "-" + raw.substring(16, 20) + "-" + raw.substring(20);
                }
                UUID uuid = UUID.fromString(raw);
                Minecraft.getInstance().execute(() -> {
                    try {
                        GameProfile profile = new GameProfile(uuid, username);
                        //? if >=1.21 {
                        /*Minecraft.getInstance().getSkinProvider().fetchSkinTextures(profile)
                                .thenAccept(textures -> Minecraft.getInstance().execute(() -> {
                                    cache.put(key, textures.texture());
                                    loading.put(key, false);
                                }));
                        *///?} else {
                        Minecraft.getInstance().getSkinManager().registerSkins(profile, (type, id, tex) -> {
                            if (type == MinecraftProfileTexture.Type.SKIN) {
                                cache.put(key, id);
                                loading.put(key, false);
                            }
                        }, true);
                        //?}
                        CompletableFuture.delayedExecutor(30, java.util.concurrent.TimeUnit.SECONDS).execute(() -> {
                            if (cache.get(key) == null) loading.put(key, false);
                        });
                    } catch (Exception ex) {
                        loading.put(key, false);
                    }
                });
            } catch (Exception ex) {
                loading.put(key, false);
            }
        });
    }
}
