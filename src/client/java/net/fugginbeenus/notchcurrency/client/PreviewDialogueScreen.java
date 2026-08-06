package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueChoice;
import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueNode;
import net.fugginbeenus.notchcurrency.npc.dialogue.DialogueTree;
import net.minecraft.client.MinecraftClient;

import java.util.UUID;

public class PreviewDialogueScreen extends NpcDialogueScreen {

    private final DialogueStudioScreen studio;
    private final DialogueTree tree;

    public PreviewDialogueScreen(DialogueStudioScreen studio, UUID npcId, String npcName,
                                 DialogueTree tree, String nodeId) {
        super(npcId, npcName, nodeId, substituteLocal(textOf(tree, nodeId), npcName),
                indicesOf(tree, nodeId), labelsOf(tree, nodeId, npcName), allEnabled(tree, nodeId));
        this.studio = studio;
        this.tree = tree;
    }

    // ---- snapshot builders (run before super(), so they're static) ----

    private static String textOf(DialogueTree tree, String nodeId) {
        DialogueNode n = tree.get(nodeId);
        return n == null ? "" : n.text();
    }

    private static int[] indicesOf(DialogueTree tree, String nodeId) {
        DialogueNode n = tree.get(nodeId);
        int count = n == null ? 0 : n.choices().size();
        int[] out = new int[count];
        for (int i = 0; i < count; i++) out[i] = i;
        return out;
    }

    private static String[] labelsOf(DialogueTree tree, String nodeId, String npcName) {
        DialogueNode n = tree.get(nodeId);
        int count = n == null ? 0 : n.choices().size();
        String[] out = new String[count];
        for (int i = 0; i < count; i++) {
            String label = n.choices().get(i).label();
            out[i] = substituteLocal(label.isEmpty() ? "(unnamed)" : label, npcName);
        }
        return out;
    }

    private static boolean[] allEnabled(DialogueTree tree, String nodeId) {
        DialogueNode n = tree.get(nodeId);
        boolean[] out = new boolean[n == null ? 0 : n.choices().size()];
        java.util.Arrays.fill(out, true);
        return out;
    }

    private static String substituteLocal(String text, String npcName) {
        MinecraftClient c = MinecraftClient.getInstance();
        String playerName = c.player != null ? c.player.getName().getString() : "player";
        return text.replace("%player%", playerName)
                .replace("%npc%", npcName)
                .replace("%balance%", Long.toString(NotchHud.getBalance()))
                .replace('&', '§');
    }

    // ---- local navigation ----

    @Override
    protected void onChoice(int i) {
        DialogueNode n = tree.get(nodeId);
        if (n == null || i >= n.choices().size()) {
            close();
            return;
        }
        DialogueChoice c = n.choices().get(i);
        String next = c.next();
        if (next.isEmpty() || tree.get(next) == null) {
            close();
        } else {
            MinecraftClient.getInstance().setScreen(
                    new PreviewDialogueScreen(studio, npcId, npcName, tree, next));
        }
    }

    @Override
    protected String bannerText() {
        return "PREVIEW - actions and requirements don't run";
    }

    @Override
    public void close() {
        // Back into the live studio instance (unsaved edits intact).
        MinecraftClient.getInstance().setScreen(studio);
    }
}
