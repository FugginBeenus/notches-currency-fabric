package net.fugginbeenus.notchcurrency.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
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
        ClientPlayNetworking.send(BALANCE_REQUEST, PacketByteBufs.empty());
    }

    public static void registerBalanceReceiver(LongConsumer onBalance) {
        ClientPlayNetworking.registerGlobalReceiver(BALANCE_SYNC, (client, handler, buf, responseSender) -> {
            long bal = buf.readVarLong();
            client.execute(() -> onBalance.accept(bal));
        });
    }

    // ---- Notch NPC editor ----
    public static void registerNpcEditorReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_EDITOR_OPEN, (client, handler, buf, rs) -> {
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
            var state = new net.fugginbeenus.notchcurrency.client.npc.NpcEditorState(
                    npcId, roleOrdinal, name, ownerName, canEdit, model, skinType, skinValue, slim, scale,
                    behaviorOrdinal, wanderRadius, dialogueNodes, dialogueFlat, statsBits, dialogueMode,
                    waypoints, patrolSpeedIdx, patrolWaitIdx, poseId, poseAnim, maxHealth, speedPct,
                    regen, followName, movesBits);
            client.execute(() -> MinecraftClient.getInstance().setScreen(
                    new net.fugginbeenus.notchcurrency.client.NotchNpcEditorScreen(state)));
        });
    }

    public static void sendNpcPatrol(UUID npcId, int action, int value) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(action);
        buf.writeVarInt(value);
        ClientPlayNetworking.send(NotchPackets.NPC_PATROL, buf);
    }

    /** Ask the server to reopen the NPC editor — used by sub-screens' Back buttons. */
    public static void sendNpcEditorReopen(UUID npcId) {
        sendNpcEditorReopen(npcId, 0);
    }

    /** Reopen the NPC editor landing on {@code returnTab} — sub-screens pass their home tab so
     *  "Back" returns you where you came from. */
    public static void sendNpcEditorReopen(UUID npcId, int returnTab) {
        net.fugginbeenus.notchcurrency.client.NotchNpcEditorScreen.reopenAtTab = returnTab;
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        ClientPlayNetworking.send(NotchPackets.NPC_EDITOR_REOPEN, buf);
    }

    public static void sendNpcSetPose(UUID npcId, int pose) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(pose);
        ClientPlayNetworking.send(NotchPackets.NPC_SET_POSE, buf);
    }

    public static void sendNpcPosePart(UUID npcId, int part, int degX, int degY, int degZ) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(part);
        buf.writeVarInt(degX);
        buf.writeVarInt(degY);
        buf.writeVarInt(degZ);
        ClientPlayNetworking.send(NotchPackets.NPC_POSE_PART, buf);
    }

    /** Set the idle animation layered on the pose (statue/breathe/sway/lively). */
    public static void sendNpcSetAnim(UUID npcId, int anim) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(anim);
        ClientPlayNetworking.send(NotchPackets.NPC_SET_ANIM, buf);
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
        ClientPlayNetworking.send(NotchPackets.NPC_TRANSFORM, buf);
    }

    public static void sendNpcDialogueMode(UUID npcId, int modeOrdinal) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(modeOrdinal);
        ClientPlayNetworking.send(NotchPackets.NPC_DIALOGUE_MODE, buf);
    }

    public static void sendNpcSetStats(UUID npcId, int bits) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(bits);
        ClientPlayNetworking.send(NotchPackets.NPC_SET_STATS, buf);
    }

    public static void sendNpcSetAttrs(UUID npcId, int maxHealth, int speedPct, int regen) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(maxHealth);
        buf.writeVarInt(speedPct);
        buf.writeVarInt(regen);
        ClientPlayNetworking.send(NotchPackets.NPC_SET_ATTRS, buf);
    }

    public static void sendShopPurchase(UUID shopId, UUID listingId, int quantity) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(shopId);
        buf.writeUuid(listingId);
        buf.writeVarInt(quantity);
        buf.writeBoolean(false); // legacy useCoins flag: read and ignored by the server
        ClientPlayNetworking.send(NotchPackets.SHOP_PURCHASE, buf);
    }

    public static void sendShopManageAction(int action, String text, @Nullable UUID listingId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(action);
        buf.writeString(text);
        buf.writeBoolean(listingId != null);
        if (listingId != null) buf.writeUuid(listingId);
        ClientPlayNetworking.send(NotchPackets.SHOP_MANAGE_ACTION, buf);
    }

    public static void sendShopEditAction(int action, int price) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(action);
        buf.writeVarInt(price);
        ClientPlayNetworking.send(NotchPackets.SHOP_EDIT_ACTION, buf);
    }

    public static void sendShopWithdraw(UUID shopId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(shopId);
        ClientPlayNetworking.send(NotchPackets.SHOP_WITHDRAW, buf);
    }

    public static void sendTradeOfferCreate(long price, long giveCoins, String target) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarLong(price);
        buf.writeVarLong(giveCoins);
        buf.writeString(target);
        ClientPlayNetworking.send(NotchPackets.TRADE_OFFER_CREATE, buf);
    }

    public static void sendTradeOfferAction(UUID offerId, int action) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(offerId);
        buf.writeVarInt(action);
        ClientPlayNetworking.send(NotchPackets.TRADE_OFFER_ACTION, buf);
    }

    public static void sendCosmeticBuy(String offerId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(offerId);
        ClientPlayNetworking.send(NotchPackets.COSMETIC_BUY, buf);
    }

    public static void sendEnchanterAction(int action, String enchantId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(action);
        buf.writeString(enchantId);
        ClientPlayNetworking.send(NotchPackets.ENCHANTER_ACTION, buf);
    }

    public static void sendNpcPreset(UUID npcId, int action, String name) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(action);
        buf.writeString(name);
        ClientPlayNetworking.send(NotchPackets.NPC_PRESET, buf);
    }

    public static void registerNpcPresetReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_PRESET_LIST, (client, handler, buf, rs) -> {
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
    }

    public static void sendNpcOpenEquip(UUID npcId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        ClientPlayNetworking.send(NotchPackets.NPC_EQUIP, buf);
    }

    public static void registerNpcDialogueReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_DIALOGUE_OPEN, (client, handler, buf, rs) -> {
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
                    // Close signal — only if the dialogue screen is up.
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
        ClientPlayNetworking.send(NotchPackets.NPC_DIALOGUE_CHOICE, buf);
    }

    public static void sendNpcDialogueTemplate(UUID npcId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        ClientPlayNetworking.send(NotchPackets.NPC_DIALOGUE_TEMPLATE, buf);
    }

    public static void sendNpcDialogueClear(UUID npcId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        ClientPlayNetworking.send(NotchPackets.NPC_DIALOGUE_CLEAR, buf);
    }

    public static void registerNpcStudioReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_STUDIO_DATA, (client, handler, buf, rs) -> {
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

    /** The taken-bounty list for the on-screen tracker HUD. */
    public static void registerBountyTrackerReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(NotchPackets.BOUNTY_TRACKER, (client, handler, buf, rs) -> {
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
        ClientPlayNetworking.send(NotchPackets.NPC_STUDIO_OPEN, buf);
    }

    public static void sendNpcStudioSave(UUID npcId, net.minecraft.nbt.NbtCompound treeNbt) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeNbt(treeNbt);
        ClientPlayNetworking.send(NotchPackets.NPC_STUDIO_SAVE, buf);
    }

    public static void sendNpcSetBehavior(UUID npcId, int modeOrdinal, int radius, String followName, int movesBits) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(modeOrdinal);
        buf.writeVarInt(radius);
        buf.writeString(followName);
        buf.writeVarInt(movesBits);
        ClientPlayNetworking.send(NotchPackets.NPC_SET_BEHAVIOR, buf);
    }

    public static void sendNpcSetAppearance(UUID npcId, String model, String skinType, String skinValue, boolean slim, float scale) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeString(model);
        buf.writeString(skinType);
        buf.writeString(skinValue);
        buf.writeBoolean(slim);
        buf.writeFloat(scale);
        ClientPlayNetworking.send(NotchPackets.NPC_SET_APPEARANCE, buf);
    }

    public static void sendNpcSetRole(UUID npcId, int roleOrdinal) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeVarInt(roleOrdinal);
        ClientPlayNetworking.send(NotchPackets.NPC_SET_ROLE, buf);
    }

    public static void sendNpcSetName(UUID npcId, String name) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeString(name);
        ClientPlayNetworking.send(NotchPackets.NPC_SET_NAME, buf);
    }

    public static void sendNpcPickup(UUID npcId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        ClientPlayNetworking.send(NotchPackets.NPC_PICKUP, buf);
    }

    public static void sendNpcDelete(UUID npcId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        ClientPlayNetworking.send(NotchPackets.NPC_DELETE, buf);
    }

}