package net.fugginbeenus.notchcurrency.client.npcmodel;

import net.fugginbeenus.notchcurrency.compat.Msg;
import net.fugginbeenus.notchcurrency.npcmodel.NpcModelRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NpcModelPacks {

    private NpcModelPacks() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-NpcModels");

    public static void reload(Minecraft client, boolean announce) {
        boolean changed = NpcModelLoader.loadAll();
        boolean packWanted = NpcModelRegistry.count() > 0;

        try {
            var mgr = client.getResourcePackRepository();
            mgr.reload();
            boolean on = mgr.getSelectedIds().contains(NpcModelLoader.PACK_PROFILE_NAME);
            boolean needsSwitchingOn = packWanted && !on;
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

        for (String problem : problems) {
            say(client, "Skipped " + problem, ChatFormatting.RED);
        }
    }

    public static String share(String id) {
        java.nio.file.Path folder = NpcModelLoader.modelsDir().resolve(id);
        byte[] blob;
        try {
            blob = net.fugginbeenus.notchcurrency.npcmodel.NpcModelBlob.pack(folder);
        } catch (Exception e) {
            return e.getMessage();
        }

        int chunkSize = net.fugginbeenus.notchcurrency.npcmodel.NpcModelStream.CHUNK_BYTES;
        net.fugginbeenus.notchcurrency.net.NotchPacketsClient.sendNpcModelPush(
                net.fugginbeenus.notchcurrency.npcmodel.NpcModelStream.PHASE_BEGIN, id, null, blob.length);
        for (int at = 0; at < blob.length; at += chunkSize) {
            int size = Math.min(chunkSize, blob.length - at);
            byte[] part = new byte[size];
            System.arraycopy(blob, at, part, 0, size);
            net.fugginbeenus.notchcurrency.net.NotchPacketsClient.sendNpcModelPush(
                    net.fugginbeenus.notchcurrency.npcmodel.NpcModelStream.PHASE_CHUNK, id, part, 0);
        }
        net.fugginbeenus.notchcurrency.net.NotchPacketsClient.sendNpcModelPush(
                net.fugginbeenus.notchcurrency.npcmodel.NpcModelStream.PHASE_END, id, null, 0);
        return null;
    }

    private static void say(Minecraft client, String line, ChatFormatting color) {
        if (client.player == null) {
            LOGGER.info(line);
            return;
        }
        Msg.chat(client.player, Component.literal(line).withStyle(color));
    }
}
