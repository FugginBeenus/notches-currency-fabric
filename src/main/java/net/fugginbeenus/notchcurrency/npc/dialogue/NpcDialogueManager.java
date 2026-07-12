package net.fugginbeenus.notchcurrency.npc.dialogue;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fugginbeenus.notchcurrency.compat.Net;
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
            if (hasRoleScreen(npc)) {
                // Give the greeting a beat to be read before the GUI covers it.
                pendingOpens.add(new PendingOpen(sp.getUuid(), npc.getUuid(), GREETING_DELAY_TICKS));
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
                    openRole(sp, npc);
                    openedRole = true;
                }
                case OPEN_SCREEN -> {
                    try {
                        var role = net.fugginbeenus.notchcurrency.economy.npc.NpcRole.valueOf(a.value());
                        NpcRoleDispatch.open(sp, role, null, npc);
                        watchForFarewell(sp, npc);
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
                        sp.sendMessage(Text.literal("You can't afford that (" + a.amount() + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ").")
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

        // Reaching the role screen is a REAL choice with an OPEN_ROLE action (seeded by default for
        // role NPCs, but the author can edit or remove it) — not a synthetic appended here.
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
        Net.sendToClient(sp, NotchPackets.NPC_DIALOGUE_OPEN, buf);
    }

    // ---- role hand-off: delayed opens (chat greetings) + farewell lines on screen close ----

    private static final int GREETING_DELAY_TICKS = 25;   // ~1.25s to read the greeting
    private static final int FAREWELL_TTL_TICKS = 20 * 60 * 5; // give up after 5 minutes

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

    /** True when interacting should be able to reach a screen/feature beyond the dialogue. */
    private static boolean hasRoleScreen(NotchNpcEntity npc) {
        var role = npc.getRole();
        return role != net.fugginbeenus.notchcurrency.economy.npc.NpcRole.NONE
                && role != net.fugginbeenus.notchcurrency.economy.npc.NpcRole.GREETER;
    }

    /** Open the NPC's role feature and, if it has a goodbye line, watch for the screen closing. */
    private static void openRole(ServerPlayerEntity sp, NotchNpcEntity npc) {
        NpcRoleDispatch.open(sp, npc.getRole(), npc.getRoleTarget(), npc);
        watchForFarewell(sp, npc);
    }

    private static void watchForFarewell(ServerPlayerEntity sp, NotchNpcEntity npc) {
        String farewell = npc.getFarewellText();
        if (farewell == null || farewell.isBlank()) return;
        String npcName = (npc.hasCustomName() && npc.getCustomName() != null)
                ? npc.getCustomName().getString() : "NPC";
        farewells.put(sp.getUuid(), new FarewellWatch(npcName, farewell));
    }

    /** Server tick: fire delayed role opens, and say goodbyes when the opened screen closes. */
    public static void tick(net.minecraft.server.MinecraftServer server) {
        if (!pendingOpens.isEmpty()) {
            var it = pendingOpens.iterator();
            while (it.hasNext()) {
                PendingOpen p = it.next();
                if (--p.ticks > 0) continue;
                it.remove();
                ServerPlayerEntity sp = server.getPlayerManager().getPlayer(p.player);
                if (sp != null && sp.getServerWorld().getEntity(p.npc) instanceof NotchNpcEntity npc) {
                    openRole(sp, npc);
                }
            }
        }
        if (!farewells.isEmpty()) {
            var it = farewells.entrySet().iterator();
            while (it.hasNext()) {
                var entry = it.next();
                ServerPlayerEntity sp = server.getPlayerManager().getPlayer(entry.getKey());
                FarewellWatch w = entry.getValue();
                if (sp == null || --w.ttl <= 0) {
                    it.remove();
                    continue;
                }
                boolean inScreen = sp.currentScreenHandler != sp.playerScreenHandler;
                if (inScreen) {
                    w.sawScreen = true;
                } else if (w.sawScreen) {
                    sp.sendMessage(Text.literal("<" + w.npcName + "> " + substitute(w.farewell, sp, w.npcName))
                            .formatted(Formatting.WHITE), false);
                    it.remove();
                }
            }
        }
    }

    private static void sendClose(ServerPlayerEntity sp) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(UUID.randomUUID());
        buf.writeString("");
        buf.writeString(""); // empty node id = close
        buf.writeString("");
        buf.writeVarInt(0);
        Net.sendToClient(sp, NotchPackets.NPC_DIALOGUE_OPEN, buf);
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

    /** Commands only run for NPCs whose owner is an operator (or server-owned NPCs) — a stored
     *  command must never outlive its author's authority. */
    private static boolean ownerMayRunCommands(NotchNpcEntity npc, net.minecraft.server.MinecraftServer server) {
        if (npc.getOwnerType() == NotchNpcEntity.OwnerType.SERVER) return true;
        UUID owner = npc.getOwner();
        if (owner == null) return false;
        ServerPlayerEntity online = server.getPlayerManager().getPlayer(owner);
        if (online != null) return online.hasPermissionLevel(2);
        var profile = server.getUserCache() == null ? java.util.Optional.<com.mojang.authlib.GameProfile>empty()
                : server.getUserCache().getByUuid(owner);
        return profile.isPresent() && server.getPlayerManager().isOperator(profile.get());
    }

    private static void runCommand(ServerPlayerEntity sp, NotchNpcEntity npc, String command, boolean asPlayer) {
        if (command == null || command.isBlank() || sp.getServer() == null) return;
        if (!ownerMayRunCommands(npc, sp.getServer())) return;
        String npcName = (npc.hasCustomName() && npc.getCustomName() != null)
                ? npc.getCustomName().getString() : "NPC";
        String cmd = substitute(command, sp, npcName);
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        var source = asPlayer ? sp.getCommandSource() : sp.getServer().getCommandSource();
        sp.getServer().getCommandManager().executeWithPrefix(source, cmd);
    }
}
