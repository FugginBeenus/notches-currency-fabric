package net.fugginbeenus.notchcurrency.npc.dialogue;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.economy.npc.NpcRoleDispatch;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Runs NPC dialogue: sends nodes to the client (with per-choice availability), and handles clicked
 * choices — re-validating conditions server-side, running the actions in order (coins move through
 * the CurrencyApi with SINK/FAUCET tagging), then jumping to the next node or closing. Stateless:
 * the client echoes back the node id + choice index and the server re-derives everything.
 */
public final class NpcDialogueManager {

    private static final double MAX_TALK_DIST_SQ = 8.0 * 8.0;

    private NpcDialogueManager() {}

    /** Play the NPC's dialogue. Returns false if it has none (caller falls back to the role).
     *  WINDOW mode opens the conversation screen; CHAT mode says a quick line (a random page from
     *  the tree) in chat and then opens the role directly — the lightweight style. */
    public static boolean open(ServerPlayerEntity sp, NotchNpcEntity npc) {
        DialogueTree tree = npc.getDialogue();
        if (tree.isEmpty()) return false;

        if (npc.getDialogueMode() == NotchNpcEntity.DialogueMode.CHAT) {
            List<DialogueNode> pages = new ArrayList<>(tree.nodes().values());
            DialogueNode pick = pages.get(sp.getRandom().nextInt(pages.size()));
            String npcName = (npc.hasCustomName() && npc.getCustomName() != null)
                    ? npc.getCustomName().getString() : "NPC";
            sp.sendMessage(Text.literal("<" + npcName + "> " + substitute(pick.text(), sp, npcName))
                    .formatted(Formatting.WHITE), false);
            var role = npc.getRole();
            if (role != net.fugginbeenus.notchcurrency.economy.npc.NpcRole.NONE
                    && role != net.fugginbeenus.notchcurrency.economy.npc.NpcRole.GREETER) {
                NpcRoleDispatch.open(sp, role, npc.getRoleTarget(), npc);
            }
            return true;
        }

        DialogueNode start = tree.start();
        if (start == null) return false;
        sendNode(sp, npc, start);
        return true;
    }

    /** A choice was clicked on the client. Validate, run actions, advance or close. */
    public static void choose(ServerPlayerEntity sp, UUID npcId, String nodeId, int choiceIndex) {
        if (!(sp.getServerWorld().getEntity(npcId) instanceof NotchNpcEntity npc)) return;
        if (sp.squaredDistanceTo(npc) > MAX_TALK_DIST_SQ) return;
        DialogueNode node = npc.getDialogue().get(nodeId);
        if (node == null || choiceIndex < 0 || choiceIndex >= node.choices().size()) return;
        DialogueChoice choice = node.choices().get(choiceIndex);
        if (!choice.isAvailable(sp, npc)) return; // locked/hidden — client shouldn't send, but re-check

        boolean openedRole = false;
        for (DialogueAction a : choice.actions()) {
            switch (a.type()) {
                case NONE -> { }
                case OPEN_ROLE -> {
                    NpcRoleDispatch.open(sp, npc.getRole(), npc.getRoleTarget(), npc);
                    openedRole = true;
                }
                case OPEN_SCREEN -> {
                    try {
                        var role = net.fugginbeenus.notchcurrency.economy.npc.NpcRole.valueOf(a.value());
                        NpcRoleDispatch.open(sp, role, null, npc);
                        openedRole = true;
                    } catch (IllegalArgumentException ignored) {
                        // Unknown screen id — skip.
                    }
                }
                case PAY_COINS -> {
                    if (a.amount() > 0) {
                        CurrencyApi.deposit(sp, a.amount(), TransactionReason.FAUCET, "NPC dialogue reward");
                    }
                }
                case CHARGE_COINS -> {
                    if (a.amount() > 0 && !CurrencyApi.withdraw(sp, a.amount(), TransactionReason.SINK, "NPC dialogue fee")) {
                        sp.sendMessage(Text.literal("You can't afford that (" + a.amount() + " coins).")
                                .formatted(Formatting.RED), false);
                        sendNode(sp, npc, node); // stay on this page
                        return;
                    }
                }
                case GIVE_ITEM -> giveItem(sp, a);
                case RUN_COMMAND -> runCommand(sp, npc, a.value(), false);
                case RUN_COMMAND_AS_PLAYER -> runCommand(sp, npc, a.value(), true);
            }
        }

        if (openedRole) return; // the role's screen replaced the dialogue

        DialogueNode next = npc.getDialogue().get(choice.next());
        if (next != null) {
            sendNode(sp, npc, next);
        } else {
            sendClose(sp);
        }
    }

    // ---- helpers ----

    /** Send a node to the client (empty node id in this packet means "close the dialogue screen"). */
    public static void sendNode(ServerPlayerEntity sp, NotchNpcEntity npc, DialogueNode node) {
        String npcName = (npc.hasCustomName() && npc.getCustomName() != null)
                ? npc.getCustomName().getString() : "NPC";

        // Only visible choices go over the wire, each with its ORIGINAL index so clicks map back.
        List<int[]> visible = new ArrayList<>(); // {originalIndex, enabled}
        for (int i = 0; i < node.choices().size(); i++) {
            DialogueChoice c = node.choices().get(i);
            boolean ok = c.isAvailable(sp, npc);
            if (!ok && c.hideWhenLocked()) continue;
            visible.add(new int[]{i, ok ? 1 : 0});
        }

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npc.getUuid());
        buf.writeString(npcName);
        buf.writeString(node.id());
        buf.writeString(substitute(node.text(), sp, npcName));
        buf.writeVarInt(visible.size());
        for (int[] v : visible) {
            buf.writeVarInt(v[0]);
            buf.writeString(substitute(node.choices().get(v[0]).label(), sp, npcName));
            buf.writeBoolean(v[1] == 1);
        }
        ServerPlayNetworking.send(sp, NotchPackets.NPC_DIALOGUE_OPEN, buf);
    }

    private static void sendClose(ServerPlayerEntity sp) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(UUID.randomUUID());
        buf.writeString("");
        buf.writeString(""); // empty node id = close
        buf.writeString("");
        buf.writeVarInt(0);
        ServerPlayNetworking.send(sp, NotchPackets.NPC_DIALOGUE_OPEN, buf);
    }

    private static String substitute(String text, ServerPlayerEntity sp, String npcName) {
        if (text == null) return "";
        String out = text.replace("%player%", sp.getName().getString()).replace("%npc%", npcName);
        if (out.contains("%balance%")) {
            out = out.replace("%balance%", Long.toString(CurrencyApi.getBalance(sp)));
        }
        // Classic '&' color/format codes (&6 gold, &l bold, &r reset, ...) render as § formatting.
        out = out.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
        return out;
    }

    private static void giveItem(ServerPlayerEntity sp, DialogueAction a) {
        Identifier id = Identifier.tryParse(a.value());
        if (id == null) return;
        Item item = Registries.ITEM.get(id);
        if (item == Items.AIR || a.amount() <= 0) return;
        int remaining = (int) Math.min(a.amount(), 64L * 9L);
        while (remaining > 0) {
            int give = Math.min(remaining, item.getMaxCount());
            sp.getInventory().offerOrDrop(new ItemStack(item, give));
            remaining -= give;
        }
    }

    private static void runCommand(ServerPlayerEntity sp, NotchNpcEntity npc, String command, boolean asPlayer) {
        if (command == null || command.isBlank() || sp.getServer() == null) return;
        String npcName = (npc.hasCustomName() && npc.getCustomName() != null)
                ? npc.getCustomName().getString() : "NPC";
        String cmd = substitute(command, sp, npcName);
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        var source = asPlayer ? sp.getCommandSource() : sp.getServer().getCommandSource();
        sp.getServer().getCommandManager().executeWithPrefix(source, cmd);
    }
}
