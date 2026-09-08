package net.fugginbeenus.notchcurrency.client.npc;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.client.Minecraft;
//? if <1.21.11 {
import net.minecraft.client.renderer.texture.HttpTexture;
//?}
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class NpcSkins {

    public static final int PRESET_COUNT = 12;
    private static final ResourceLocation[] PRESETS = new ResourceLocation[PRESET_COUNT];
    static {
        for (int i = 0; i < PRESET_COUNT; i++) {
            PRESETS[i] = NotchCurrency.id("textures/skins/preset_" + (i + 1) + ".png");
        }
    }
    //? if >=1.21 {
    /*private static final ResourceLocation DEFAULT = DefaultPlayerSkin.getDefaultTexture();
    *///?} else {
    private static final ResourceLocation DEFAULT = DefaultPlayerSkin.getDefaultSkin();
    //?}

    private static final Map<String, ResourceLocation> cache = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, Boolean> loading = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<String, Long> retryAt = new java.util.concurrent.ConcurrentHashMap<>();
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("NotchCurrency-Skins");
    private static final long RETRY_DELAY_MS = 60_000L;

    private static boolean shouldStart(String key) {
        Long wait = retryAt.get(key);
        if (wait != null && System.currentTimeMillis() < wait) return false;
        return !Boolean.TRUE.equals(loading.put(key, true));
    }

    private static void fail(String key) {
        retryAt.put(key, System.currentTimeMillis() + RETRY_DELAY_MS);
        loading.put(key, false);
    }

    private static void done(String key, ResourceLocation texture) {
        cache.put(key, texture);
        retryAt.remove(key);
        loading.put(key, false);
    }

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
        String key = "url:" + url;
        ResourceLocation cached = cache.get(key);
        if (cached != null) return cached;
        if (!shouldStart(key)) return DEFAULT;
        download(url, key);
        return DEFAULT;
    }

    private static ResourceLocation player(String username) {
        if (username == null || username.isEmpty()) return DEFAULT;
        String key = "player:" + username.toLowerCase();
        ResourceLocation cached = cache.get(key);
        if (cached != null) return cached;
        if (!shouldStart(key)) return DEFAULT;
        loadPlayerAsync(username, key);
        return DEFAULT;
    }

    private static void download(String url, String key) {
        LOGGER.info("downloading skin for {} from {}", key, url);
        Minecraft.getInstance().execute(() -> {
            try {
                ResourceLocation id = NotchCurrency.id("skins/web/"
                        + Integer.toHexString(url.hashCode()) + "_" + url.length());
                //? if >=1.21.11 {
                /*Minecraft mc = Minecraft.getInstance();
                new net.minecraft.client.renderer.texture.SkinTextureDownloader(
                        mc.getProxy(), mc.getTextureManager(), mc)
                        .downloadAndRegisterSkin(id, mc.gameDirectory.toPath().resolve("assets/skins"), url, true)
                        .whenComplete((asset, error) -> mc.execute(() -> {
                            if (error != null || asset == null) {
                                LOGGER.warn("skin download failed for {}", key, error);
                                fail(key);
                                return;
                            }
                            LOGGER.info("skin ready for {} as {}", key, asset.texturePath());
                            done(key, asset.texturePath());
                        }));
                *///?} else {
                java.io.File cacheFile = new java.io.File(Minecraft.getInstance().gameDirectory,
                        "assets/skins/" + Integer.toHexString(url.hashCode()) + "_" + url.length() + ".png");
                Minecraft.getInstance().getTextureManager().register(id,
                        new HttpTexture(cacheFile, url, DEFAULT, true, () -> {
                            LOGGER.info("skin ready for {} as {}", key, id);
                            done(key, id);
                        }));
                //?}
                CompletableFuture.delayedExecutor(30, java.util.concurrent.TimeUnit.SECONDS).execute(() -> {
                    if (cache.get(key) == null) {
                        LOGGER.warn("skin timed out for {}", key);
                        fail(key);
                    }
                });
            } catch (Exception e) {
                LOGGER.warn("skin download errored for {}", key, e);
                fail(key);
            }
        });
    }

    private static void loadPlayerAsync(String username, String key) {
        CompletableFuture.runAsync(() -> {
            try {
                String raw = jsonString(fetch("https://api.mojang.com/users/profiles/minecraft/" + username), "id");
                if (raw == null || raw.length() < 32) {
                    LOGGER.warn("no mojang account for {}", username);
                    fail(key);
                    return;
                }
                String textures = jsonString(fetch(
                        "https://sessionserver.mojang.com/session/minecraft/profile/" + raw), "value");
                if (textures == null) {
                    LOGGER.warn("no texture property for {}", username);
                    fail(key);
                    return;
                }
                String decoded = new String(java.util.Base64.getDecoder().decode(textures),
                        java.nio.charset.StandardCharsets.UTF_8);
                int skinAt = decoded.indexOf("\"SKIN\"");
                String skinUrl = skinAt < 0 ? null : jsonString(decoded.substring(skinAt), "url");
                if (skinUrl == null) {
                    LOGGER.warn("no skin url for {}", username);
                    fail(key);
                    return;
                }
                download(skinUrl.replace("\\/", "/"), key);
            } catch (Exception ex) {
                LOGGER.warn("skin lookup failed for {}", username, ex);
                fail(key);
            }
        });
    }

    private static String fetch(String address) throws Exception {
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) new java.net.URL(address).openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.setRequestProperty("User-Agent", "NotchCurrency-Mod");
        int code = conn.getResponseCode();
        if (code != 200) {
            LOGGER.warn("mojang returned {} for {}", code, address);
            conn.disconnect();
            return null;
        }
        StringBuilder sb = new StringBuilder();
        try (java.io.BufferedReader r = new java.io.BufferedReader(
                new java.io.InputStreamReader(conn.getInputStream(), java.nio.charset.StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
        }
        conn.disconnect();
        return sb.toString();
    }

    private static String jsonString(String json, String field) {
        if (json == null) return null;
        int at = json.indexOf("\"" + field + "\"");
        if (at < 0) return null;
        int colon = json.indexOf(":", at);
        if (colon < 0) return null;
        int s = json.indexOf("\"", colon + 1);
        if (s < 0) return null;
        int e = json.indexOf("\"", s + 1);
        if (e < 0) return null;
        return json.substring(s + 1, e);
    }
}
