package net.fugginbeenus.notchcurrency.client.npc;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client-side catalog of built-in NPC appearance variants (id → display name + texture). Phase 2
 * ships the placeholder humanoid skins (adapted from APP.ly). Later this becomes the client half of
 * the model-provider registry that other mods can extend.
 */
public final class NpcAppearances {

    public record Variant(String id, String displayName, Identifier texture) {}

    private static final Map<String, Variant> BY_ID = new LinkedHashMap<>();

    private static void add(String id, String name, String texturePath) {
        BY_ID.put(id, new Variant(id, name, NotchCurrency.id("textures/entity/" + texturePath)));
    }

    static {
        add("default",    "Default",    "notch_npc.png");
        add("banker",     "Banker",     "notch_npc_banker.png");
        add("auctioneer", "Auctioneer", "notch_npc_auctioneer.png");
        add("postman",    "Postman",    "notch_npc_postman.png");
        add("builder",    "Builder",    "notch_npc_builder.png");
    }

    private NpcAppearances() {}

    public static Identifier texture(String id) {
        Variant v = BY_ID.get(id);
        return v != null ? v.texture() : BY_ID.get("default").texture();
    }

    public static List<Variant> all() {
        return new ArrayList<>(BY_ID.values());
    }
}
