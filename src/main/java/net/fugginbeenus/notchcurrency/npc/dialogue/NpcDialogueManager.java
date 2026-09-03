package net.fugginbeenus.notchcurrency.npc.dialogue;

import net.fugginbeenus.notchcurrency.compat.Net;
import net.fugginbeenus.notchcurrency.economy.npc.NpcRoleDispatch;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.npc.NpcText;
import net.fugginbeenus.notchcurrency.npc.action.NpcActionRunner;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class NpcDialogueManager {

    private static final double MAX_TALK_DIST_SQ = 8.0 * 8.0;

    private NpcDialogueManager() {}

    public static boolean open(ServerPlayer sp, NotchNpcEntity npc) {
        DialogueTree tree = npc.getDialogue();
        if (tree.isEmpty()) return false;

        if (npc.getDialogueMode() == NotchNpcEntity.DialogueMode.CHAT) {
            List<DialogueNode> pages = new ArrayList<>(tree.nodes().values());
            DialogueNode pick = pages.get(sp.getRandom().nextInt(pages.size()));
            String npcName = (npc.hasCustomName() && npc.getCustomName() != null)
                    ? npc.getCustomName().getString() : "NPC";
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("<" + npcName + "> " + substitute(pick.text(), sp, npcName))
                    .withStyle(ChatFormatting.WHITE));
            if (hasRoleScreen(npc)) {
                pendingOpens.add(new PendingOpen(sp.getUUID(), npc.getUUID(), GREETING_DELAY_TICKS));
            }
            return true;
        }

        DialogueNode start = pickOpening(sp, npc, tree);
        if (start == null) return false;
        if (isLineList(tree)) {
            List<DialogueNode> pages = new ArrayList<>(tree.nodes().values());
            start = pages.get(sp.getRandom().nextInt(pages.size()));
        }
        sendNode(sp, npc, start);
        return true;
    }

    private static boolean opens(ServerPlayer sp, NotchNpcEntity npc, DialogueNode page) {
        for (DialogueCondition c : page.openIf()) {
            if (c != null && !c.test(sp, npc)) return false;
        }
        return true;
    }

    private static DialogueNode pickOpening(ServerPlayer sp, NotchNpcEntity npc, DialogueTree tree) {
        DialogueNode best = null;
        int bestCount = 0;
        for (DialogueNode page : tree.nodes().values()) {
            if (!page.hasOpenIf() || !opens(sp, npc, page)) continue;
            int count = 0;
            for (DialogueCondition c : page.openIf()) {
                if (c != null) count++;
            }
            if (count > bestCount) {
                best = page;
                bestCount = count;
            }
        }
        if (best != null) return best;
        DialogueNode start = tree.start();
        if (start != null && opens(sp, npc, start)) return start;
        for (DialogueNode page : tree.nodes().values()) {
            if (!page.hasOpenIf()) return page;
        }
        return start;
    }

    private static boolean isLineList(DialogueTree tree) {
        if (tree.nodes().size() < 2) return false;
        for (DialogueNode node : tree.nodes().values()) {
            if (!node.choices().isEmpty()) return false;
        }
        return true;
    }

    public static void choose(ServerPlayer sp, UUID npcId, String nodeId, int choiceIndex) {
        if (!(sp.serverLevel().getEntity(npcId) instanceof NotchNpcEntity npc)) return;
        if (sp.distanceToSqr(npc) > MAX_TALK_DIST_SQ) return;
        DialogueNode node = npc.getDialogue().get(nodeId);
        if (node == null || choiceIndex < 0 || choiceIndex >= node.choices().size()) return;
        DialogueChoice choice = node.choices().get(choiceIndex);
        if (!choice.isAvailable(sp, npc)) return;

        var outcome = NpcActionRunner.run(sp, npc, choice.actions());
        if (outcome == NpcActionRunner.Outcome.ABORTED) {
            sendNode(sp, npc, node);
            return;
        }
        if (outcome == NpcActionRunner.Outcome.OPENED_SCREEN) return;

        DialogueNode next = npc.getDialogue().get(choice.next());
        if (next != null) {
            sendNode(sp, npc, next);
        } else {
            sendClose(sp);
        }
    }

    public static void sendNode(ServerPlayer sp, NotchNpcEntity npc, DialogueNode node) {
        String npcName = (npc.hasCustomName() && npc.getCustomName() != null)
                ? npc.getCustomName().getString() : "NPC";

        List<int[]> visible = new ArrayList<>();
        for (int i = 0; i < node.choices().size(); i++) {
            DialogueChoice c = node.choices().get(i);
            boolean ok = c.isAvailable(sp, npc);
            if (!ok && c.hideWhenLocked()) continue;
            visible.add(new int[]{i, ok ? 1 : 0});
        }
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npc.getUUID());
        buf.writeUtf(npcName);
        buf.writeUtf(node.id());
        buf.writeUtf(substitute(node.text(), sp, npcName));
        buf.writeVarInt(visible.size());
        for (int[] v : visible) {
            buf.writeVarInt(v[0]);
            buf.writeUtf(substitute(node.choices().get(v[0]).label(), sp, npcName));
            buf.writeBoolean(v[1] == 1);
        }
        Net.sendToClient(sp, NotchPackets.NPC_DIALOGUE_OPEN, buf);
    }

    private static final int GREETING_DELAY_TICKS = 25;
    private static final int FAREWELL_TTL_TICKS = 20 * 60 * 5;

    private static final class PendingOpen {
        final UUID player, npc;
        int ticks;
        PendingOpen(UUID player, UUID npc, int ticks) { this.player = player; this.npc = npc; this.ticks = ticks; }
    }

    private static final class FarewellWatch {
        final String npcName, farewell;
        boolean sawScreen;
        int ttl = FAREWELL_TTL_TICKS;
        FarewellWatch(String npcName, String farewell) { this.npcName = npcName; this.farewell = farewell; }
    }

    private static final List<PendingOpen> pendingOpens = new ArrayList<>();
    private static final java.util.Map<UUID, FarewellWatch> farewells = new java.util.HashMap<>();

    private static boolean hasRoleScreen(NotchNpcEntity npc) {
        var role = npc.getRole();
        return role != net.fugginbeenus.notchcurrency.economy.npc.NpcRole.NONE
                && role != net.fugginbeenus.notchcurrency.economy.npc.NpcRole.GREETER;
    }

    public static void openRole(ServerPlayer sp, NotchNpcEntity npc) {
        NpcRoleDispatch.open(sp, npc.getRole(), npc.getRoleTarget(), npc);
        watchForFarewell(sp, npc);
    }

    public static void watchForFarewell(ServerPlayer sp, NotchNpcEntity npc) {
        String farewell = npc.getFarewellText();
        if (farewell == null || farewell.isBlank()) return;
        String npcName = (npc.hasCustomName() && npc.getCustomName() != null)
                ? npc.getCustomName().getString() : "NPC";
        farewells.put(sp.getUUID(), new FarewellWatch(npcName, farewell));
    }

    public static void tick(net.minecraft.server.MinecraftServer server) {
        if (!pendingOpens.isEmpty()) {
            var it = pendingOpens.iterator();
            while (it.hasNext()) {
                PendingOpen p = it.next();
                if (--p.ticks > 0) continue;
                it.remove();
                ServerPlayer sp = server.getPlayerList().getPlayer(p.player);
                if (sp != null && sp.serverLevel().getEntity(p.npc) instanceof NotchNpcEntity npc) {
                    openRole(sp, npc);
                }
            }
        }
        if (!farewells.isEmpty()) {
            var it = farewells.entrySet().iterator();
            while (it.hasNext()) {
                var entry = it.next();
                ServerPlayer sp = server.getPlayerList().getPlayer(entry.getKey());
                FarewellWatch w = entry.getValue();
                if (sp == null || --w.ttl <= 0) {
                    it.remove();
                    continue;
                }
                boolean inScreen = sp.containerMenu != sp.inventoryMenu;
                if (inScreen) {
                    w.sawScreen = true;
                } else if (w.sawScreen) {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("<" + w.npcName + "> " + substitute(w.farewell, sp, w.npcName))
                            .withStyle(ChatFormatting.WHITE));
                    it.remove();
                }
            }
        }
    }

    private static void sendClose(ServerPlayer sp) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(UUID.randomUUID());
        buf.writeUtf("");
        buf.writeUtf("");
        buf.writeUtf("");
        buf.writeVarInt(0);
        Net.sendToClient(sp, NotchPackets.NPC_DIALOGUE_OPEN, buf);
    }

    private static String substitute(String text, ServerPlayer sp, String npcName) {
        return NpcText.substitute(text, sp, npcName);
    }
}
