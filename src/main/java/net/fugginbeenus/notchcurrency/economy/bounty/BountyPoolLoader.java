package net.fugginbeenus.notchcurrency.economy.bounty;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.fugginbeenus.notchcurrency.compat.Reg;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;

/**
 * Loads bounty pools from datapacks: JSON arrays under
 * {@code data/&lt;namespace&gt;/notch_bounties/objectives/*.json} and {@code .../rewards/*.json},
 * merged across all packs (the mod ships defaults; server packs add or override). Reloads on
 * {@code /reload}.
 */
public class BountyPoolLoader implements SimpleSynchronousResourceReloadListener {

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-BountyPools");

    @Override
    public Identifier getFabricId() {
        return NotchCurrency.id("bounty_pools");
    }

    @Override
    public void reload(ResourceManager manager) {
        BountyPools.clear();
        loadObjectives(manager);
        loadRewards(manager);
        loadDecrees(manager);
        LOGGER.info("Loaded {} bounty objectives and {} rewards.",
                BountyPools.objectiveCount(), BountyPools.rewardCount());
    }

    private void loadObjectives(ResourceManager manager) {
        for (Map.Entry<Identifier, Resource> e : manager
                .findResources("notch_bounties/objectives", id -> id.getPath().endsWith(".json")).entrySet()) {
            forEachEntry(e, o -> {
                BountyType type = "fetch".equalsIgnoreCase(o.get("type").getAsString())
                        ? BountyType.FETCH : BountyType.KILL;
                int min = o.get("min").getAsInt();
                int max = o.has("max") ? o.get("max").getAsInt() : min;
                BountyPools.addObjective(new BountyPools.ObjectiveEntry(
                        type,
                        Reg.parse(o.get("target").getAsString()),
                        min, Math.max(min, max),
                        rarityOf(o), weightOf(o),
                        o.has("category") ? o.get("category").getAsString() : "general"));
            });
        }
    }

    private void loadDecrees(ResourceManager manager) {
        for (Map.Entry<Identifier, Resource> e : manager
                .findResources("notch_bounties/decrees", id -> id.getPath().endsWith(".json")).entrySet()) {
            forEachEntry(e, o -> BountyPools.addDecree(
                    Reg.parse(o.get("item").getAsString()), o.get("category").getAsString()));
        }
    }

    private void loadRewards(ResourceManager manager) {
        for (Map.Entry<Identifier, Resource> e : manager
                .findResources("notch_bounties/rewards", id -> id.getPath().endsWith(".json")).entrySet()) {
            forEachEntry(e, o -> {
                boolean item = "item".equalsIgnoreCase(o.get("type").getAsString());
                int min = o.get("min").getAsInt();
                int max = o.has("max") ? o.get("max").getAsInt() : min;
                BountyPools.addReward(new BountyPools.RewardEntry(
                        item,
                        item ? Reg.parse(o.get("item").getAsString()) : null,
                        min, Math.max(min, max),
                        rarityOf(o), weightOf(o)));
            });
        }
    }

    private interface EntryConsumer { void accept(JsonObject o); }

    private void forEachEntry(Map.Entry<Identifier, Resource> e, EntryConsumer consumer) {
        try (InputStream is = e.getValue().getInputStream();
             InputStreamReader reader = new InputStreamReader(is)) {
            JsonElement root = JsonParser.parseReader(reader);
            JsonArray arr = root.isJsonArray() ? root.getAsJsonArray()
                    : root.getAsJsonObject().getAsJsonArray("entries");
            for (JsonElement el : arr) {
                try {
                    consumer.accept(el.getAsJsonObject());
                } catch (Exception ex) {
                    LOGGER.warn("Skipping malformed bounty entry in {}: {}", e.getKey(), ex.getMessage());
                }
            }
        } catch (Exception ex) {
            LOGGER.warn("Failed to read bounty pool {}: {}", e.getKey(), ex.getMessage());
        }
    }

    private static BountyRarity rarityOf(JsonObject o) {
        return BountyRarity.fromString(o.has("rarity") ? o.get("rarity").getAsString() : "common");
    }

    private static int weightOf(JsonObject o) {
        return o.has("weight") ? Math.max(1, o.get("weight").getAsInt()) : 1;
    }
}
