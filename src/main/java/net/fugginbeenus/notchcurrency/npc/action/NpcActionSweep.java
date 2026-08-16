package net.fugginbeenus.notchcurrency.npc.action;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueAction;
import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueChoice;
import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueNode;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NpcActionSweep {

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-NpcSweep");

    public static final int CURRENT_VERSION = 1;

    private NpcActionSweep() {}

    public static void sweep(NotchNpcEntity npc) {
        if (npc.getActionSweepVersion() >= CURRENT_VERSION) return;
        MinecraftServer server = npc.level().getServer();
        if (server == null) return;
        npc.setActionSweepVersion(CURRENT_VERSION);

        if (NpcActionRunner.ownerMayRunCommands(npc, server)) return;

        int removed = 0;
        for (DialogueNode node : npc.getDialogue().nodes().values()) {
            for (DialogueChoice choice : node.choices()) {
                removed += strip(choice.actions());
            }
        }
        for (NpcTrigger trigger : NpcTrigger.values()) {
            var kept = new java.util.ArrayList<>(npc.getActions().get(trigger));
            int before = kept.size();
            strip(kept);
            if (kept.size() != before) {
                removed += before - kept.size();
                npc.getActions().set(trigger, kept);
            }
        }

        if (removed > 0) {
            LOGGER.info("Removed {} admin-only action(s) from NPC {} - its owner ({}) isn't an operator",
                    removed, npc.getUUID(), npc.getOwnerName().isEmpty() ? "unknown" : npc.getOwnerName());
        }
    }

    private static int strip(java.util.List<DialogueAction> actions) {
        int before = actions.size();
        actions.removeIf(a -> DialogueAction.isAdminOnly(a.type()));
        return before - actions.size();
    }
}
