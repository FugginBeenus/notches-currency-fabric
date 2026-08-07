package net.fugginbeenus.notchcurrency.economy.crate;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class CrateLoader implements SimpleSynchronousResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-Crates");

    @Override
    public ResourceLocation getFabricId() {
        return NotchCurrency.id("crates");
    }

    @Override
    public void onResourceManagerReload(ResourceManager manager) {
        CrateRegistry.clear();
        for (Map.Entry<ResourceLocation, Resource> e : manager
                .listResources("crates", id -> id.getPath().endsWith(".json")).entrySet()) {
            String path = e.getKey().getPath();               // "crates/common.json"
            String crateId = path.substring("crates/".length(), path.length() - ".json".length());
            try (InputStream is = e.getValue().open();
                 InputStreamReader reader = new InputStreamReader(is)) {
                JsonObject o = JsonParser.parseReader(reader).getAsJsonObject();
                CrateRegistry.put(parse(crateId, o));
            } catch (Exception ex) {
                LOGGER.warn("Failed to load crate {}: {}", crateId, ex.getMessage());
            }
        }
        LOGGER.info("Loaded {} crate types.", CrateRegistry.count());
    }

    private static CrateDef parse(String id, JsonObject o) {
        String name = o.has("name") ? o.get("name").getAsString() : id;
        int keys = o.has("keys_required") ? Math.max(1, o.get("keys_required").getAsInt()) : 1;

        List<CrateDef.LootEntry> loot = new ArrayList<>();
        JsonArray arr = o.getAsJsonArray("loot");
        for (JsonElement el : arr) {
            JsonObject l = el.getAsJsonObject();
            int weight = l.has("weight") ? Math.max(1, l.get("weight").getAsInt()) : 1;
            if ("coins".equalsIgnoreCase(l.get("type").getAsString())) {
                loot.add(new CrateDef.LootEntry(false, null, 0, 0, l.get("amount").getAsLong(), weight));
            } else {
                int min = l.get("min").getAsInt();
                int max = l.has("max") ? l.get("max").getAsInt() : min;
                ResourceLocation itemId = net.fugginbeenus.notchcurrency.compat.Reg.parse(l.get("item").getAsString());
                if (!net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(itemId)) {
                    // A misconfigured entry would silently pay out "Air": name the culprit instead.
                    LOGGER.warn("Crate '{}': unknown item '{}' - loot entry skipped", id, itemId);
                    continue;
                }
                loot.add(new CrateDef.LootEntry(true, itemId, min, Math.max(min, max), 0, weight));
            }
        }
        return new CrateDef(id, name, keys, loot);
    }
}
