package net.fugginbeenus.notchcurrency.client;

import net.fabricmc.loader.api.FabricLoader;
import net.fugginbeenus.notchcurrency.compat.Geo;
import net.fugginbeenus.notchcurrency.compat.Msg;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Answers one question, and is meant to be deleted once it has: does a GeckoLib model written to
 * disk while the game is running get picked up by a resource reload?
 *
 * <p>The whole custom NPC model design rests on that being true. See docs/custom-npc-models.md. If
 * it is false, bundles cannot be delivered to a running client and the design needs rethinking, so
 * it is worth knowing before any of it is built.
 *
 * <p>It copies the mod's own NPC model back out under a different name, which keeps the test to the
 * one thing being measured. No new art, no format, no parsing: just "file appears, does GeckoLib
 * bake it".
 */
public final class NpcModelSpike {

    private NpcModelSpike() {}

    private static final String PACK_DIR_NAME = "NotchCurrencyModels";
    private static final String PACK_PROFILE_NAME = "file/" + PACK_DIR_NAME;
    private static final String SPIKE_ID = "spike_test";

    private static final int PACK_FORMAT =
            //? if >=1.21 {
            /*34;
            *///?} else {
            15;
            //?}

    public static void run(Minecraft client) {
        try {
            say(client, "Writing a model to disk...", ChatFormatting.GRAY);
            if (!writePack(client)) {
                say(client, "Could not read the mod's own model to copy. Spike aborted.",
                        ChatFormatting.RED);
                return;
            }

            say(client, "Reloading resources...", ChatFormatting.GRAY);
            enableAndReload(client);
        } catch (Exception e) {
            say(client, "Spike failed: " + e, ChatFormatting.RED);
        }
    }

    /** Copies the built-in model back out under a new id, in both GeckoLib layouts. */
    private static boolean writePack(Minecraft client) throws Exception {
        byte[] geo = read(client, NotchCurrency.id("geckolib/models/notch_npc.geo.json"));
        if (geo == null) geo = read(client, NotchCurrency.id("geo/notch_npc.geo.json"));
        byte[] anim = read(client, NotchCurrency.id("geckolib/animations/notch_npc.animation.json"));
        if (anim == null) anim = read(client, NotchCurrency.id("animations/notch_npc.animation.json"));
        if (geo == null || anim == null) return false;

        // The model declares its own name inside the file. Renaming the file alone would leave two
        // models claiming the same identifier, and GeckoLib keys by what is inside.
        String geoText = new String(geo, java.nio.charset.StandardCharsets.UTF_8)
                .replace("geometry.notch_npc", "geometry." + SPIKE_ID);

        Path pack = FabricLoader.getInstance().getGameDir().resolve("resourcepacks").resolve(PACK_DIR_NAME);
        Path assets = pack.resolve("assets").resolve("notchcurrency");
        Files.createDirectories(assets.resolve("geckolib").resolve("models"));
        Files.createDirectories(assets.resolve("geckolib").resolve("animations"));
        Files.createDirectories(assets.resolve("geo"));
        Files.createDirectories(assets.resolve("animations"));

        Files.writeString(pack.resolve("pack.mcmeta"), """
                {
                  "pack": {
                    "pack_format": %d,
                    "description": "Notch Currency NPC models (generated)"
                  }
                }
                """.formatted(PACK_FORMAT));

        // Both layouts, because GeckoLib 4 and 5 scan different directories.
        Files.writeString(assets.resolve("geckolib").resolve("models").resolve(SPIKE_ID + ".geo.json"), geoText);
        Files.writeString(assets.resolve("geo").resolve(SPIKE_ID + ".geo.json"), geoText);
        Files.write(assets.resolve("geckolib").resolve("animations").resolve(SPIKE_ID + ".json"), anim);
        Files.write(assets.resolve("animations").resolve(SPIKE_ID + ".animation.json"), anim);
        return true;
    }

    private static byte[] read(Minecraft client, ResourceLocation id) {
        try {
            Optional<Resource> res = client.getResourceManager().getResource(id);
            if (res.isEmpty()) return null;
            try (InputStream in = res.get().open()) {
                return in.readAllBytes();
            }
        } catch (Exception missing) {
            return null;
        }
    }

    private static void enableAndReload(Minecraft client) {
        var mgr = client.getResourcePackRepository();
        mgr.reload();
        boolean enabled = mgr.getSelectedIds().contains(PACK_PROFILE_NAME);
        if (!enabled && !mgr.addPack(PACK_PROFILE_NAME)) {
            say(client, "Could not enable the pack. Turn on \"" + PACK_DIR_NAME
                    + "\" in Options, Resource Packs, then run this again.", ChatFormatting.RED);
            return;
        }
        client.options.updateResourcePacks(mgr);
        // The answer is only meaningful once the reload has actually finished.
        client.reloadResourcePacks().thenRun(() -> client.execute(() -> report(client)));
    }

    private static void report(Minecraft client) {
        //? if >=1.21.11 {
        /*ResourceLocation modelId = NotchCurrency.id(SPIKE_ID);
        ResourceLocation animId = NotchCurrency.id(SPIKE_ID);
        *///?} else {
        ResourceLocation modelId = NotchCurrency.id("geo/" + SPIKE_ID + ".geo.json");
        ResourceLocation animId = NotchCurrency.id("animations/" + SPIKE_ID + ".animation.json");
        //?}

        boolean model = Geo.hasBakedModel(modelId);
        int clips = Geo.clipNames(animId, SPIKE_ID).size();

        say(client, "--- model spike ---", ChatFormatting.AQUA);
        say(client, "model baked: " + (model ? "YES" : "no"),
                model ? ChatFormatting.GREEN : ChatFormatting.RED);
        say(client, "clips found: " + clips, clips > 0 ? ChatFormatting.GREEN : ChatFormatting.RED);

        if (model && clips > 0) {
            say(client, "A model written while the game was running was picked up. The custom "
                    + "model design holds.", ChatFormatting.GREEN);
        } else {
            say(client, "Not picked up. Check the pack is enabled in Options, Resource Packs, then "
                    + "run this again before concluding anything.", ChatFormatting.YELLOW);
        }
    }

    private static void say(Minecraft client, String line, ChatFormatting color) {
        if (client.player == null) return;
        Msg.chat(client.player, Component.literal(line).withStyle(color));
    }
}
