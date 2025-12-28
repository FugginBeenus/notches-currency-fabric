package net.fugginbeenus.notchcurrency.client.entity;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftProfileTexture;
import net.fugginbeenus.notchcurrency.entity.ShopkeeperEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Renderer for ShopkeeperEntity using LivingEntityRenderer for proper rotation handling.
 * Supports preset skins, URL skins, and player username skins.
 */
public class ShopkeeperRenderer extends LivingEntityRenderer<ShopkeeperEntity, ShopkeeperModel> {

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency");

    // Preset skin textures (bundled with mod)
    private static final Identifier[] PRESET_SKINS = new Identifier[12];
    static {
        for (int i = 0; i < 12; i++) {
            PRESET_SKINS[i] = new Identifier("notchcurrency", "textures/skins/preset_" + (i + 1) + ".png");
        }
    }

    // Default fallback skin
    private static final Identifier DEFAULT_SKIN = DefaultSkinHelper.getTexture();

    // Cache for loaded skins (URL/player skins)
    private static final Map<String, Identifier> skinCache = new HashMap<>();
    private static final Map<String, Boolean> skinLoadingStatus = new HashMap<>();

    private final ShopkeeperModel modelNormal;
    private final ShopkeeperModel modelSlim;

    public ShopkeeperRenderer(EntityRendererFactory.Context ctx) {
        // Pass model and shadow radius (0.5f like player)
        super(ctx, new ShopkeeperModel(ShopkeeperModel.getTexturedModelData(false).createModel(), false), 0.5f);

        this.modelNormal = this.model;
        this.modelSlim = new ShopkeeperModel(ShopkeeperModel.getTexturedModelData(true).createModel(), true);
    }

    @Override
    public void render(ShopkeeperEntity entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light) {
        // Swap model if entity has slim arms
        this.model = entity.hasSlimArms() ? this.modelSlim : this.modelNormal;

        super.render(entity, yaw, tickDelta, matrices, vertexConsumers, light);
    }

    @Override
    public Identifier getTexture(ShopkeeperEntity entity) {
        String skinType = entity.getSkinType();
        String skinValue = entity.getSkinValue();

        switch (skinType) {
            case ShopkeeperEntity.SKIN_PRESET:
                return getPresetSkin(skinValue);

            case ShopkeeperEntity.SKIN_URL:
                return getUrlSkin(skinValue);

            case ShopkeeperEntity.SKIN_PLAYER:
                return getPlayerSkin(skinValue);

            default:
                return DEFAULT_SKIN;
        }
    }

    private Identifier getPresetSkin(String value) {
        try {
            int index = Integer.parseInt(value) - 1;
            if (index >= 0 && index < PRESET_SKINS.length) {
                return PRESET_SKINS[index];
            }
        } catch (NumberFormatException e) {
            // Fall through to default
        }
        return PRESET_SKINS[0];
    }

    private Identifier getUrlSkin(String url) {
        if (url == null || url.isEmpty()) {
            return DEFAULT_SKIN;
        }

        // Check cache
        if (skinCache.containsKey(url)) {
            return skinCache.get(url);
        }

        // Check if already loading
        if (skinLoadingStatus.getOrDefault(url, false)) {
            return DEFAULT_SKIN;
        }

        // Start async download
        skinLoadingStatus.put(url, true);
        loadUrlSkinAsync(url);

        return DEFAULT_SKIN;
    }

    private void loadUrlSkinAsync(String url) {
        CompletableFuture.runAsync(() -> {
            try {
                // Use Minecraft's texture manager to download
                Identifier textureId = new Identifier("notchcurrency", "skins/url/" + url.hashCode());

                MinecraftClient.getInstance().execute(() -> {
                    try {
                        // Download and register texture
                        MinecraftClient.getInstance().getTextureManager().registerTexture(
                                textureId,
                                new net.minecraft.client.texture.PlayerSkinTexture(
                                        null, // File cache (null = no cache)
                                        url,
                                        DEFAULT_SKIN,
                                        true, // Transparent
                                        () -> {
                                            skinCache.put(url, textureId);
                                            skinLoadingStatus.put(url, false);
                                            LOGGER.debug("Loaded URL skin: {}", url);
                                        }
                                )
                        );
                    } catch (Exception e) {
                        LOGGER.warn("Failed to load URL skin: {}", url, e);
                        skinLoadingStatus.put(url, false);
                    }
                });
            } catch (Exception e) {
                LOGGER.warn("Error loading URL skin: {}", url, e);
                skinLoadingStatus.put(url, false);
            }
        });
    }

    private Identifier getPlayerSkin(String username) {
        if (username == null || username.isEmpty()) {
            return DEFAULT_SKIN;
        }

        String cacheKey = "player:" + username.toLowerCase();

        // Check cache first
        Identifier cached = skinCache.get(cacheKey);
        if (cached != null) {
            return cached;
        }

        // Check if already loading (with timeout protection)
        Boolean loading = skinLoadingStatus.get(cacheKey);
        if (loading != null && loading) {
            return DEFAULT_SKIN;
        }

        // Start async fetch
        skinLoadingStatus.put(cacheKey, true);
        loadPlayerSkinAsync(username, cacheKey);

        return DEFAULT_SKIN;
    }

    private void loadPlayerSkinAsync(String username, String cacheKey) {
        CompletableFuture.runAsync(() -> {
            try {
                // First, look up the player's UUID from Mojang API
                String apiUrl = "https://api.mojang.com/users/profiles/minecraft/" + username;
                java.net.URL url = new java.net.URL(apiUrl);
                java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setConnectTimeout(10000); // Increased timeout
                conn.setReadTimeout(10000);
                conn.setRequestProperty("User-Agent", "NotchCurrency-Mod");

                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    LOGGER.warn("Player not found or API error for {}: HTTP {}", username, responseCode);
                    skinLoadingStatus.put(cacheKey, false);
                    return;
                }

                // Read response
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                conn.disconnect();

                // Parse UUID from JSON (simple parsing)
                String json = response.toString();
                int idIndex = json.indexOf("\"id\"");
                if (idIndex < 0) {
                    LOGGER.warn("Invalid API response for {}: no id field", username);
                    skinLoadingStatus.put(cacheKey, false);
                    return;
                }
                int colonIndex = json.indexOf(":", idIndex);
                int valueStart = json.indexOf("\"", colonIndex + 1);
                int valueEnd = json.indexOf("\"", valueStart + 1);
                String uuidStr = json.substring(valueStart + 1, valueEnd);

                // Format UUID with dashes
                if (uuidStr.length() == 32) {
                    uuidStr = uuidStr.substring(0, 8) + "-" + uuidStr.substring(8, 12) + "-" +
                            uuidStr.substring(12, 16) + "-" + uuidStr.substring(16, 20) + "-" +
                            uuidStr.substring(20);
                }

                final UUID playerUUID = UUID.fromString(uuidStr);
                LOGGER.debug("Found UUID for {}: {}", username, playerUUID);

                // Use Minecraft's built-in skin fetching on main thread
                final String finalCacheKey = cacheKey;
                MinecraftClient.getInstance().execute(() -> {
                    try {
                        GameProfile profile = new GameProfile(playerUUID, username);
                        MinecraftClient client = MinecraftClient.getInstance();

                        // Use the skin provider to fetch and cache the skin
                        client.getSkinProvider().loadSkin(profile, (type, id, tex) -> {
                            if (type == MinecraftProfileTexture.Type.SKIN) {
                                skinCache.put(finalCacheKey, id);
                                skinLoadingStatus.put(finalCacheKey, false);
                                LOGGER.info("Successfully loaded skin for {}: {}", username, id);
                            }
                        }, true);

                        // Set a timeout - if skin doesn't load in 30 seconds, allow retry
                        CompletableFuture.delayedExecutor(30, java.util.concurrent.TimeUnit.SECONDS)
                                .execute(() -> {
                                    if (skinCache.get(finalCacheKey) == null) {
                                        skinLoadingStatus.put(finalCacheKey, false);
                                        LOGGER.debug("Skin load timeout for {}, allowing retry", username);
                                    }
                                });

                    } catch (Exception e) {
                        LOGGER.warn("Failed to load skin for {}: {}", username, e.getMessage());
                        skinLoadingStatus.put(finalCacheKey, false);
                    }
                });

            } catch (Exception e) {
                LOGGER.warn("Error fetching player UUID for {}: {}", username, e.getMessage());
                skinLoadingStatus.put(cacheKey, false);
            }
        });
    }

    @Override
    protected boolean hasLabel(ShopkeeperEntity entity) {
        return entity.hasCustomName();
    }

    /**
     * Clear the skin cache (useful for skin refresh)
     */
    public static void clearSkinCache() {
        skinCache.clear();
        skinLoadingStatus.clear();
    }

    /**
     * Clear a specific skin from cache (for updates)
     */
    public static void clearSkin(String key) {
        skinCache.remove(key);
        skinLoadingStatus.remove(key);
    }
}