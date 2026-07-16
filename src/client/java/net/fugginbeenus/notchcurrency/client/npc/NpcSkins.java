package net.fugginbeenus.notchcurrency.client.npc;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.PlayerSkinTexture;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Resolves the humanoid skin texture for a Notch NPC: bundled presets (1–12), a live player-name skin
 * (Mojang lookup), or a custom URL skin — both fetched asynchronously and cached. Adapted from the old
 * shopkeeper renderer. Robust best-effort: falls back to the default skin while loading or on failure.
 */
public final class NpcSkins {

    public static final int PRESET_COUNT = 12;
    private static final Identifier[] PRESETS = new Identifier[PRESET_COUNT];
    static {
        for (int i = 0; i < PRESET_COUNT; i++) {
            PRESETS[i] = NotchCurrency.id("textures/skins/preset_" + (i + 1) + ".png");
        }
    }
    private static final Identifier DEFAULT = DefaultSkinHelper.getTexture();

    private static final Map<String, Identifier> cache = new HashMap<>();
    private static final Map<String, Boolean> loading = new HashMap<>();

    private NpcSkins() {}

    public static Identifier resolve(NotchNpcEntity npc) {
        return switch (npc.getSkinType()) {
            case NotchNpcEntity.SKIN_PLAYER -> player(npc.getSkinValue());
            case NotchNpcEntity.SKIN_URL -> url(npc.getSkinValue());
            default -> preset(npc.getSkinValue());
        };
    }

    public static Identifier preset(String value) {
        try {
            int i = Integer.parseInt(value) - 1;
            if (i >= 0 && i < PRESET_COUNT) return PRESETS[i];
        } catch (NumberFormatException ignored) {}
        return PRESETS[0];
    }

    private static Identifier url(String url) {
        if (url == null || url.isEmpty()) return DEFAULT;
        Identifier cached = cache.get("url:" + url);
        if (cached != null) return cached;
        if (Boolean.TRUE.equals(loading.get("url:" + url))) return DEFAULT;
        loading.put("url:" + url, true);
        MinecraftClient.getInstance().execute(() -> {
            try {
                Identifier id = NotchCurrency.id("skins/url/" + Integer.toHexString(url.hashCode()));
                MinecraftClient.getInstance().getTextureManager().registerTexture(id,
                        new PlayerSkinTexture(null, url, DEFAULT, true, () -> {
                            cache.put("url:" + url, id);
                            loading.put("url:" + url, false);
                        }));
            } catch (Exception e) {
                loading.put("url:" + url, false);
            }
        });
        return DEFAULT;
    }

    private static Identifier player(String username) {
        if (username == null || username.isEmpty()) return DEFAULT;
        String key = "player:" + username.toLowerCase();
        Identifier cached = cache.get(key);
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
                MinecraftClient.getInstance().execute(() -> {
                    try {
                        GameProfile profile = new GameProfile(uuid, username);
                        //? if >=1.21 {
                        /*MinecraftClient.getInstance().getSkinProvider().fetchSkinTextures(profile)
                                .thenAccept(textures -> MinecraftClient.getInstance().execute(() -> {
                                    cache.put(key, textures.texture());
                                    loading.put(key, false);
                                }));
                        *///?} else {
                        MinecraftClient.getInstance().getSkinProvider().loadSkin(profile, (type, id, tex) -> {
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
