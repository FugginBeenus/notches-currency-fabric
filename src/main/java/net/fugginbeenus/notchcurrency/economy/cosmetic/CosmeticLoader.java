package net.fugginbeenus.notchcurrency.economy.cosmetic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

public class CosmeticLoader implements SimpleSynchronousResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-Cosmetics");

    @Override
    public ResourceLocation getFabricId() {
        return NotchCurrency.id("cosmetics");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        CosmeticRegistry.clear();
        for (Map.Entry<ResourceLocation, Resource> e : manager
                .listResources("cosmetics", id -> id.getPath().endsWith(".json")).entrySet()) {
            String path = e.getKey().getPath();               // "cosmetics/halo.json"
            String offerId = path.substring("cosmetics/".length(), path.length() - ".json".length());
            try (InputStream is = e.getValue().open();
                 InputStreamReader reader = new InputStreamReader(is)) {
                JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();
                CosmeticRegistry.put(CosmeticOffer.fromJson(offerId, o));
            } catch (Exception ex) {
                LOGGER.warn("Failed to load cosmetic {}: {}", offerId, ex.getMessage());
            }
        }
        LOGGER.info("Loaded {} cosmetic offers.", CosmeticRegistry.count());
    }
}
