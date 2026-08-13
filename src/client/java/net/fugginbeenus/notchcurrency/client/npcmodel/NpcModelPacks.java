package net.fugginbeenus.notchcurrency.client.npcmodel;

import net.fugginbeenus.notchcurrency.compat.Msg;
import net.fugginbeenus.notchcurrency.npcmodel.NpcModelRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gets loaded bundles in front of GeckoLib, which means the generated pack has to be switched on
 * and the resources reloaded.
 *
 * <p>Split from the loader because the loader only touches files, and this touches the running
 * game. It is also the only place that reloads, so the "one reload, after everything is written"
 * rule has somewhere to live.
 */
public final class NpcModelPacks {

    private NpcModelPacks() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-NpcModels");

    /**
     * Reads the folder, rewrites the pack, and reloads if anything is there to reload.
     *
     * @param announce whether to report the outcome in chat, which a hand-run reload wants and a
     *                 quiet one at startup does not
     */
    public static void reload(Minecraft client, boolean announce) {
        boolean changed = NpcModelLoader.loadAll();
        boolean packWanted = NpcModelRegistry.count() > 0;

        try {
            var mgr = client.getResourcePackRepository();
            mgr.reload();
            boolean on = mgr.getSelectedIds().contains(NpcModelLoader.PACK_PROFILE_NAME);
            boolean needsSwitchingOn = packWanted && !on;

            // Nothing moved and the pack is already in the right state, so there is nothing worth
            // charging a resource reload for.
            if (!changed && !needsSwitchingOn) {
                if (announce) report(client);
                return;
            }

            if (needsSwitchingOn) {
                if (!mgr.addPack(NpcModelLoader.PACK_PROFILE_NAME)) {
                    say(client, "Could not switch on the models pack. Enable \""
                            + NpcModelLoader.PACK_DIR_NAME + "\" in Options, Resource Packs.",
                            ChatFormatting.RED);
                    return;
                }
                client.options.updateResourcePacks(mgr);
            }
            client.reloadResourcePacks().thenRun(() -> client.execute(() -> {
                if (announce) report(client);
            }));
        } catch (Exception e) {
            LOGGER.error("Could not apply the NPC models pack", e);
            say(client, "Could not apply the models pack: " + e.getMessage(), ChatFormatting.RED);
        }
    }

    private static void report(Minecraft client) {
        int loaded = NpcModelRegistry.count();
        var problems = NpcModelLoader.problems();

        if (loaded == 0 && problems.isEmpty()) {
            say(client, "No custom NPC models found. Put one in "
                    + "config/notchcurrency/npc_models and run this again.", ChatFormatting.YELLOW);
        } else if (loaded > 0) {
            say(client, "Loaded " + loaded + (loaded == 1 ? " custom NPC model." : " custom NPC models."),
                    ChatFormatting.GREEN);
        }

        // Each skipped model says what is wrong with it, since a silent skip is the worst outcome:
        // the model is simply missing and nothing says why.
        for (String problem : problems) {
            say(client, "Skipped " + problem, ChatFormatting.RED);
        }
    }

    private static void say(Minecraft client, String line, ChatFormatting color) {
        if (client.player == null) {
            LOGGER.info(line);
            return;
        }
        Msg.chat(client.player, Component.literal(line).withStyle(color));
    }
}
