package net.fugginbeenus.notchcurrency.net;

import net.fugginbeenus.notchcurrency.compat.NetClient;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.LongConsumer;

public final class NotchPacketsClient {
    // Reuse the same identifiers defined in the common class
    public static final Identifier BALANCE_SYNC    = NotchPackets.BALANCE_SYNC;
    public static final Identifier BALANCE_REQUEST = NotchPackets.BALANCE_REQUEST;

    public static final Identifier TRADE_OPEN     = NotchPackets.TRADE_OPEN;
    public static final Identifier TRADE_UPDATE   = NotchPackets.TRADE_UPDATE;
    public static final Identifier TRADE_CANCEL   = NotchPackets.TRADE_CANCEL;
    public static final Identifier TRADE_COMPLETE = NotchPackets.TRADE_COMPLETE;

    private NotchPacketsClient() {}

    public static void requestBalance() {
        NetClient.sendToServer(BALANCE_REQUEST, PacketByteBufs.empty());
    }

    public static void registerBalanceReceiver(LongConsumer onBalance) {
        NetClient.registerClientReceiver(BALANCE_SYNC, (client, buf) -> {
            long bal = buf.readVarLong();
            client.execute(() -> onBalance.accept(bal));
        });
    }

    // ---- Notch NPC editor ----
    public static void registerNpcEditorReceiver() {
        NetClient.registerClientReceiver(NotchPackets.NPC_EDITOR_OPEN, (client, buf) -> {
            UUID npcId = buf.readUuid();
            int roleOrdinal = buf.readVarInt();
            String name = buf.readString();
            String ownerName = buf.readString();
            boolean canEdit = buf.readBoolean();
            String model = buf.readString();
            String skinType = buf.readString();
            String skinValue = buf.readString();
            boolean slim = buf.readBoolean();
            float scale = buf.readFloat();
            float scaleY = buf.readFloat();
            float scaleZ = buf.readFloat();
            float nameOffset = buf.readFloat();
            int behaviorOrdinal = buf.readVarInt();
            int wanderRadius = buf.readVarInt();
            int dialogueNodes = buf.readVarInt();
            boolean dialogueFlat = buf.readBoolean();
            int statsBits = buf.readVarInt();
            int dialogueMode = buf.readVarInt();
            int waypoints = buf.readVarInt();
            int patrolSpeedIdx = buf.readVarInt();
            int patrolWaitIdx = buf.readVarInt();
            int poseId = buf.readVarInt();
            int poseAnim = buf.readVarInt();
            int maxHealth = buf.readVarInt();
            int speedPct = buf.readVarInt();
            int regen = buf.readVarInt();
            String followName = buf.readString(16);
            int movesBits = buf.readVarInt();
            String farewell = buf.readString(160);
            String billboard = buf.readString(400);
            var state = new net.fugginbeenus.notchcurrency.client.npc.NpcEditorState(
                    npcId, roleOrdinal, name, ownerName, canEdit, model, skinType, skinValue, slim,
                    scale, scaleY, scaleZ, nameOffset,
                    behaviorOrdinal, wanderRadius, dialogueNodes, dialogueFlat, statsBits, dialogueMode,
                    waypoints, patrolSpeedIdx, patrolWaitIdx, poseId, poseAnim, maxHealth, speedPct,
                    regen, followName, movesBits, farewell, billboard);
            client.execute(() -> MinecraftClient.getInstance().setScreen(
                    new net.fugginbeenus.notchcurrency.client.NotchNpcEditorScreen(state)));
        });
    }

    public static void sendNpcPatrol(UUID npcId, int action, int value) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(action);
        buf.writeVarInt(value);
        NetClient.sendToServer(NotchPackets.NPC_PATROL, buf);
    }

    /** Ask the server to reopen the NPC editor, used by sub-screens' Back buttons. */
    public static void sendNpcEditorReopen(UUID npcId) {
        sendNpcEditorReopen(npcId, 0);
    }

    /** Reopen the NPC editor landing on {@code returnTab}: sub-screens pass their home tab so
     *  "Back" returns you where you came from. */
    public static void sendNpcEditorReopen(UUID npcId, int returnTab) {
        net.fugginbeenus.notchcurrency.client.NotchNpcEditorScreen.reopenAtTab = returnTab;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        NetClient.sendToServer(NotchPackets.NPC_EDITOR_REOPEN, buf);
    }

    public static void sendNpcSetPose(UUID npcId, int pose) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(pose);
        NetClient.sendToServer(NotchPackets.NPC_SET_POSE, buf);
    }

    public static void sendNpcPosePart(UUID npcId, int part, int degX, int degY, int degZ) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(part);
        buf.writeVarInt(degX);
        buf.writeVarInt(degY);
        buf.writeVarInt(degZ);
        NetClient.sendToServer(NotchPackets.NPC_POSE_PART, buf);
    }

    /** Set the idle animation layered on the pose (statue/breathe/sway/lively). */
    public static void sendNpcSetAnim(UUID npcId, int anim) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(anim);
        NetClient.sendToServer(NotchPackets.NPC_SET_ANIM, buf);
    }

    /** Move (delta blocks) and/or rotate (absolute yaw, when applyYaw) the whole NPC. */
    public static void sendNpcTransform(UUID npcId, double dx, double dy, double dz, float yawDeg, boolean applyYaw) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeDouble(dx);
        buf.writeDouble(dy);
        buf.writeDouble(dz);
        buf.writeFloat(yawDeg);
        buf.writeBoolean(applyYaw);
        NetClient.sendToServer(NotchPackets.NPC_TRANSFORM, buf);
    }

    public static void sendNpcDialogueMode(UUID npcId, int modeOrdinal) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(modeOrdinal);
        NetClient.sendToServer(NotchPackets.NPC_DIALOGUE_MODE, buf);
    }

    public static void sendNpcSetStats(UUID npcId, int bits) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(bits);
        NetClient.sendToServer(NotchPackets.NPC_SET_STATS, buf);
    }

    public static void sendNpcSetAttrs(UUID npcId, int maxHealth, int speedPct, int regen) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(maxHealth);
        buf.writeVarInt(speedPct);
        buf.writeVarInt(regen);
        NetClient.sendToServer(NotchPackets.NPC_SET_ATTRS, buf);
    }

    public static void sendShopPurchase(UUID shopId, UUID listingId, int quantity) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(shopId);
        buf.writeUuid(listingId);
        buf.writeVarInt(quantity);
        buf.writeBoolean(false); // legacy useCoins flag: read and ignored by the server
        NetClient.sendToServer(NotchPackets.SHOP_PURCHASE, buf);
    }

    public static void sendShopManageAction(int action, String text, @Nullable UUID listingId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(action);
        buf.writeString(text);
        buf.writeBoolean(listingId != null);
        if (listingId != null) buf.writeUuid(listingId);
        NetClient.sendToServer(NotchPackets.SHOP_MANAGE_ACTION, buf);
    }

    public static void sendShopEditAction(int action, int price) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(action);
        buf.writeVarInt(price);
        NetClient.sendToServer(NotchPackets.SHOP_EDIT_ACTION, buf);
    }

    public static void sendShopWithdraw(UUID shopId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(shopId);
        NetClient.sendToServer(NotchPackets.SHOP_WITHDRAW, buf);
    }

    public static void sendTradeOfferCreate(long price, long giveCoins, String target) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarLong(price);
        buf.writeVarLong(giveCoins);
        buf.writeString(target);
        NetClient.sendToServer(NotchPackets.TRADE_OFFER_CREATE, buf);
    }

    public static void sendTradeOfferAction(UUID offerId, int action) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(offerId);
        buf.writeVarInt(action);
        NetClient.sendToServer(NotchPackets.TRADE_OFFER_ACTION, buf);
    }

    public static void sendCosmeticBuy(String offerId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(offerId);
        NetClient.sendToServer(NotchPackets.COSMETIC_BUY, buf);
    }

    public static void sendEnchanterAction(int action, String enchantId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(action);
        buf.writeString(enchantId);
        NetClient.sendToServer(NotchPackets.ENCHANTER_ACTION, buf);
    }

    public static void sendNpcPreset(UUID npcId, int action, String name) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(action);
        buf.writeString(name);
        NetClient.sendToServer(NotchPackets.NPC_PRESET, buf);
    }

    public static void sendNpcShare(UUID npcId, int action, String payload) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(action);
        buf.writeString(payload, net.fugginbeenus.notchcurrency.npc.NpcShareCodec.MAX_WIRE_CHARS);
        NetClient.sendToServer(NotchPackets.NPC_SHARE, buf);
    }

    public static void registerNpcPresetReceiver() {
        NetClient.registerClientReceiver(NotchPackets.NPC_PRESET_LIST, (client, buf) -> {
            UUID npcId = buf.readUuid();
            int count = buf.readVarInt();
            java.util.List<String> names = new java.util.ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                names.add(buf.readString(64));
            }
            client.execute(() -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (mc.currentScreen instanceof net.fugginbeenus.notchcurrency.client.NpcPresetScreen s
                        && s.isFor(npcId)) {
                    s.setPresets(names);
                } else {
                    mc.setScreen(new net.fugginbeenus.notchcurrency.client.NpcPresetScreen(npcId, names));
                }
            });
        });

        // The clipboard only exists on the client, so the code is built server-side and handed back
        // here to be put on it.
        NetClient.registerClientReceiver(NotchPackets.NPC_SHARE_CODE, (client, buf) -> {
            String code = buf.readString(net.fugginbeenus.notchcurrency.npc.NpcShareCodec.MAX_WIRE_CHARS);
            client.execute(() -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                mc.keyboard.setClipboard(code);
                if (mc.player != null) {
                    mc.player.sendMessage(net.minecraft.text.Text.literal(
                                    "Share code copied. Paste it anywhere, or into a .npc file.")
                            .formatted(net.minecraft.util.Formatting.GREEN), false);
                }
            });
        });
    }

    public static void sendNpcOpenEquip(UUID npcId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        NetClient.sendToServer(NotchPackets.NPC_EQUIP, buf);
    }

    public static void registerNpcDialogueReceiver() {
        NetClient.registerClientReceiver(NotchPackets.NPC_DIALOGUE_OPEN, (client, buf) -> {
            UUID npcId = buf.readUuid();
            String npcName = buf.readString();
            String nodeId = buf.readString();
            String text = buf.readString();
            int count = buf.readVarInt();
            int[] indices = new int[count];
            String[] labels = new String[count];
            boolean[] enabled = new boolean[count];
            for (int i = 0; i < count; i++) {
                indices[i] = buf.readVarInt();
                labels[i] = buf.readString();
                enabled[i] = buf.readBoolean();
            }
            client.execute(() -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                if (nodeId.isEmpty()) {
                    // Close signal: only if the dialogue screen is up.
                    if (mc.currentScreen instanceof net.fugginbeenus.notchcurrency.client.NpcDialogueScreen) {
                        mc.setScreen(null);
                    }
                } else {
                    mc.setScreen(new net.fugginbeenus.notchcurrency.client.NpcDialogueScreen(
                            npcId, npcName, nodeId, text, indices, labels, enabled));
                }
            });
        });
    }

    public static void sendNpcDialogueChoice(UUID npcId, String nodeId, int choiceIndex) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeString(nodeId);
        buf.writeVarInt(choiceIndex);
        NetClient.sendToServer(NotchPackets.NPC_DIALOGUE_CHOICE, buf);
    }

    public static void sendNpcDialogueTemplate(UUID npcId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        NetClient.sendToServer(NotchPackets.NPC_DIALOGUE_TEMPLATE, buf);
    }

    public static void sendNpcDialogueClear(UUID npcId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        NetClient.sendToServer(NotchPackets.NPC_DIALOGUE_CLEAR, buf);
    }

    public static void registerNpcStudioReceiver() {
        NetClient.registerClientReceiver(NotchPackets.NPC_STUDIO_DATA, (client, buf) -> {
            UUID npcId = buf.readUuid();
            net.minecraft.nbt.NbtCompound tree = buf.readNbt();
            client.execute(() -> {
                var parsed = net.fugginbeenus.notchcurrency.npc.dialogue.DialogueTree.fromNbt(
                        tree == null ? new net.minecraft.nbt.NbtCompound() : tree);
                if (nextStudioOpensQuickLines) {
                    nextStudioOpensQuickLines = false;
                    MinecraftClient.getInstance().setScreen(
                            new net.fugginbeenus.notchcurrency.client.QuickLinesScreen(npcId, parsed));
                } else {
                    MinecraftClient.getInstance().setScreen(
                            new net.fugginbeenus.notchcurrency.client.DialogueStudioScreen(npcId, parsed));
                }
            });
        });
    }

    public static void registerNpcActionsReceiver() {
        NetClient.registerClientReceiver(NotchPackets.NPC_ACTIONS_DATA, (client, buf) -> {
            UUID npcId = buf.readUuid();
            net.minecraft.nbt.NbtCompound nbt = buf.readNbt();
            client.execute(() -> {
                var parsed = net.fugginbeenus.notchcurrency.npc.action.NpcActions.fromNbt(nbt);
                MinecraftClient.getInstance().setScreen(
                        new net.fugginbeenus.notchcurrency.client.NpcActionsScreen(npcId, parsed));
            });
        });
    }

    /** The server's custom coin skin, pushed on join: written into a local auto-enabled pack. */
    public static void registerCurrencySyncReceiver() {
        NetClient.registerClientReceiver(NotchPackets.WAYSTONE_FEE_SYNC, (client, buf) -> {
            boolean enabled = buf.readBoolean();
            int fee = buf.readVarInt();
            int dimensionalFee = buf.readVarInt();
            client.execute(() -> net.fugginbeenus.notchcurrency.client.WaystoneFees.set(enabled, fee, dimensionalFee));
        });

        NetClient.registerClientReceiver(NotchPackets.CURRENCY_SYNC, (client, buf) -> {
            String itemName = buf.readString(64);
            byte[] coin = buf.readBoolean() ? buf.readByteArray() : null;
            byte[] tails = buf.readBoolean() ? buf.readByteArray() : null;
            client.execute(() -> net.fugginbeenus.notchcurrency.client.CurrencyPackGenerator
                    .applyServerData(client, itemName, coin, tails));
        });
    }

    /** The taken-bounty list for the on-screen tracker HUD. */
    public static void registerBountyTrackerReceiver() {
        NetClient.registerClientReceiver(NotchPackets.BOUNTY_TRACKER, (client, buf) -> {
            int count = buf.readVarInt();
            var list = new java.util.ArrayList<net.fugginbeenus.notchcurrency.client.BountyTrackerHud.Entry>();
            for (int i = 0; i < count; i++) {
                String desc = buf.readString();
                boolean kill = buf.readBoolean();
                String target = buf.readString();
                int prog = buf.readVarInt();
                int req = buf.readVarInt();
                long expiry = buf.readLong();
                String rarity = buf.readString();
                list.add(new net.fugginbeenus.notchcurrency.client.BountyTrackerHud.Entry(
                        desc, kill, target, prog, req, expiry, rarity));
            }
            client.execute(() -> net.fugginbeenus.notchcurrency.client.BountyTrackerHud.setEntries(list));
        });
    }

    /** When set, the next NPC_STUDIO_DATA reply opens Quick Lines instead of the full studio. */
    private static boolean nextStudioOpensQuickLines = false;

    public static void sendNpcStudioOpen(UUID npcId) {
        sendNpcStudioOpen(npcId, false);
    }

    /** {@code quickLines} routes the server's tree reply into the Quick Lines editor. */
    public static void sendNpcStudioOpen(UUID npcId, boolean quickLines) {
        nextStudioOpensQuickLines = quickLines;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        NetClient.sendToServer(NotchPackets.NPC_STUDIO_OPEN, buf);
    }

    public static void sendNpcBillboard(UUID npcId, String text) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeString(text);
        NetClient.sendToServer(NotchPackets.NPC_BILLBOARD, buf);
    }

    public static void sendFactionPick(UUID npcId, int action, String factionId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(action);
        buf.writeString(factionId);
        NetClient.sendToServer(NotchPackets.NPC_FACTION_PICK, buf);
    }

    public static void registerFactionListReceiver() {
        NetClient.registerClientReceiver(NotchPackets.NPC_FACTION_LIST, (client, buf) -> {
            UUID npcId = buf.readUuid();
            String currentId = buf.readString();
            int count = buf.readVarInt();
            java.util.List<net.fugginbeenus.notchcurrency.client.NpcFactionPickerScreen.Entry> entries =
                    new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                String id = buf.readString();
                String name = buf.readString();
                net.minecraft.util.Formatting color = net.minecraft.util.Formatting.byName(buf.readString());
                int members = buf.readVarInt();
                entries.add(new net.fugginbeenus.notchcurrency.client.NpcFactionPickerScreen.Entry(
                        id, name, color == null ? net.minecraft.util.Formatting.WHITE : color, members));
            }
            client.execute(() -> MinecraftClient.getInstance().setScreen(
                    new net.fugginbeenus.notchcurrency.client.NpcFactionPickerScreen(npcId, currentId, entries)));
        });
    }

    public static void sendRecruiterAction(UUID npcId, int action, String name, String color,
                                          int fee, boolean open) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(action);
        buf.writeString(name);
        buf.writeString(color);
        buf.writeVarInt(fee);
        buf.writeBoolean(open);
        NetClient.sendToServer(NotchPackets.NPC_RECRUITER_ACTION, buf);
    }

    public static void registerRecruiterReceiver() {
        NetClient.registerClientReceiver(NotchPackets.NPC_RECRUITER_OPEN, (client, buf) -> {
            UUID npcId = buf.readUuid();
            String factionId = buf.readString();
            String factionName = buf.readString();
            String colorName = buf.readString();
            int members = buf.readVarInt();
            boolean alreadyIn = buf.readBoolean();
            boolean canFound = buf.readBoolean();
            buf.readBoolean(); // may-assign: reserved for the Role tab picker
            String motto = buf.readString();
            int fee = buf.readVarInt();
            boolean open = buf.readBoolean();
            boolean canManage = buf.readBoolean();
            client.execute(() -> {
                net.minecraft.util.Formatting color = net.minecraft.util.Formatting.byName(colorName);
                MinecraftClient.getInstance().setScreen(
                        new net.fugginbeenus.notchcurrency.client.NpcRecruiterScreen(npcId, factionId,
                                factionName, color == null ? net.minecraft.util.Formatting.WHITE : color,
                                members, alreadyIn, canFound, motto, fee, open, canManage));
            });
        });
    }

    public static void sendNpcActionsOpen(UUID npcId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        NetClient.sendToServer(NotchPackets.NPC_ACTIONS_OPEN, buf);
    }

    public static void sendNpcActionsSave(UUID npcId, net.minecraft.nbt.NbtCompound actionsNbt) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeNbt(actionsNbt);
        NetClient.sendToServer(NotchPackets.NPC_ACTIONS_SAVE, buf);
    }

    public static void sendNpcStudioSave(UUID npcId, net.minecraft.nbt.NbtCompound treeNbt) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeNbt(treeNbt);
        NetClient.sendToServer(NotchPackets.NPC_STUDIO_SAVE, buf);
    }

    public static void sendNpcSetBehavior(UUID npcId, int modeOrdinal, int radius, String followName, int movesBits) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(modeOrdinal);
        buf.writeVarInt(radius);
        buf.writeString(followName);
        buf.writeVarInt(movesBits);
        NetClient.sendToServer(NotchPackets.NPC_SET_BEHAVIOR, buf);
    }

    public static void sendNpcSetAppearance(UUID npcId, String model, String skinType, String skinValue,
                                            boolean slim, float scaleX, float scaleY, float scaleZ,
                                            float nameOffset) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeString(model);
        buf.writeString(skinType);
        buf.writeString(skinValue);
        buf.writeBoolean(slim);
        buf.writeFloat(scaleX);
        buf.writeFloat(scaleY);
        buf.writeFloat(scaleZ);
        buf.writeFloat(nameOffset);
        NetClient.sendToServer(NotchPackets.NPC_SET_APPEARANCE, buf);
    }

    public static void sendNpcSetRole(UUID npcId, int roleOrdinal) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(roleOrdinal);
        NetClient.sendToServer(NotchPackets.NPC_SET_ROLE, buf);
    }

    public static void sendNpcSetName(UUID npcId, String name) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeString(name);
        NetClient.sendToServer(NotchPackets.NPC_SET_NAME, buf);
    }

    public static void sendNpcSetFarewell(UUID npcId, String text) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeString(text);
        NetClient.sendToServer(NotchPackets.NPC_SET_FAREWELL, buf);
    }

    public static void sendNpcPickup(UUID npcId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        NetClient.sendToServer(NotchPackets.NPC_PICKUP, buf);
    }

    public static void sendNpcDelete(UUID npcId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        NetClient.sendToServer(NotchPackets.NPC_DELETE, buf);
    }

}