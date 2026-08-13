package net.fugginbeenus.notchcurrency.net;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.fugginbeenus.notchcurrency.compat.NetClient;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.LongConsumer;

public final class NotchPacketsClient {
    // Reuse the same identifiers defined in the common class
    public static final ResourceLocation BALANCE_SYNC    = NotchPackets.BALANCE_SYNC;
    public static final ResourceLocation BALANCE_REQUEST = NotchPackets.BALANCE_REQUEST;

    public static final ResourceLocation TRADE_OPEN     = NotchPackets.TRADE_OPEN;
    public static final ResourceLocation TRADE_UPDATE   = NotchPackets.TRADE_UPDATE;
    public static final ResourceLocation TRADE_CANCEL   = NotchPackets.TRADE_CANCEL;
    public static final ResourceLocation TRADE_COMPLETE = NotchPackets.TRADE_COMPLETE;

    private NotchPacketsClient() {}

    public static void requestBalance() {
        NetClient.sendToServer(BALANCE_REQUEST, net.fugginbeenus.notchcurrency.compat.Net.emptyBuf());
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
            UUID npcId = buf.readUUID();
            int roleOrdinal = buf.readVarInt();
            String name = buf.readUtf();
            String ownerName = buf.readUtf();
            boolean canEdit = buf.readBoolean();
            String model = buf.readUtf();
            String skinType = buf.readUtf();
            String skinValue = buf.readUtf();
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
            String followName = buf.readUtf(16);
            int movesBits = buf.readVarInt();
            String farewell = buf.readUtf(160);
            String billboard = buf.readUtf(400);
            String subtitle = buf.readUtf(64);
            String voice = buf.readUtf(128);
            int voicePitch = buf.readVarInt();
            var state = new net.fugginbeenus.notchcurrency.client.npc.NpcEditorState(
                    npcId, roleOrdinal, name, ownerName, canEdit, model, skinType, skinValue, slim,
                    scale, scaleY, scaleZ, nameOffset,
                    behaviorOrdinal, wanderRadius, dialogueNodes, dialogueFlat, statsBits, dialogueMode,
                    waypoints, patrolSpeedIdx, patrolWaitIdx, poseId, poseAnim, maxHealth, speedPct,
                    regen, followName, movesBits, farewell, billboard, subtitle, voice, voicePitch);
            client.execute(() -> Minecraft.getInstance().setScreen(
                    new net.fugginbeenus.notchcurrency.client.NotchNpcEditorScreen(state)));
        });
    }

    public static void sendNpcPatrol(UUID npcId, int action, int value) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeVarInt(action);
        buf.writeVarInt(value);
        NetClient.sendToServer(NotchPackets.NPC_PATROL, buf);
    }

    public static void sendNpcEditorReopen(UUID npcId) {
        sendNpcEditorReopen(npcId, 0);
    }

    public static void sendNpcEditorReopen(UUID npcId, int returnTab) {
        net.fugginbeenus.notchcurrency.client.NotchNpcEditorScreen.reopenAtTab = returnTab;
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        NetClient.sendToServer(NotchPackets.NPC_EDITOR_REOPEN, buf);
    }

    public static void sendNpcSetPose(UUID npcId, int pose) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeVarInt(pose);
        NetClient.sendToServer(NotchPackets.NPC_SET_POSE, buf);
    }

    public static void sendNpcPosePart(UUID npcId, int part, int degX, int degY, int degZ) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeVarInt(part);
        buf.writeVarInt(degX);
        buf.writeVarInt(degY);
        buf.writeVarInt(degZ);
        NetClient.sendToServer(NotchPackets.NPC_POSE_PART, buf);
    }

    public static void sendNpcSetClip(UUID npcId, String clip) {
        var buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeUtf(clip == null ? "" : clip, 128);
        NetClient.sendToServer(NotchPackets.NPC_SET_CLIP, buf);
    }

    public static void sendNpcSetAnim(UUID npcId, int anim) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeVarInt(anim);
        NetClient.sendToServer(NotchPackets.NPC_SET_ANIM, buf);
    }

    public static void sendNpcTransform(UUID npcId, double dx, double dy, double dz, float yawDeg, boolean applyYaw) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeDouble(dx);
        buf.writeDouble(dy);
        buf.writeDouble(dz);
        buf.writeFloat(yawDeg);
        buf.writeBoolean(applyYaw);
        NetClient.sendToServer(NotchPackets.NPC_TRANSFORM, buf);
    }

    public static void sendNpcDialogueMode(UUID npcId, int modeOrdinal) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeVarInt(modeOrdinal);
        NetClient.sendToServer(NotchPackets.NPC_DIALOGUE_MODE, buf);
    }

    public static void sendNpcSetStats(UUID npcId, int bits) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeVarInt(bits);
        NetClient.sendToServer(NotchPackets.NPC_SET_STATS, buf);
    }

    public static void sendNpcSetAttrs(UUID npcId, int maxHealth, int speedPct, int regen) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeVarInt(maxHealth);
        buf.writeVarInt(speedPct);
        buf.writeVarInt(regen);
        NetClient.sendToServer(NotchPackets.NPC_SET_ATTRS, buf);
    }

    public static void sendShopPurchase(UUID shopId, UUID listingId, int quantity) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(shopId);
        buf.writeUUID(listingId);
        buf.writeVarInt(quantity);
        buf.writeBoolean(false); // legacy useCoins flag: read and ignored by the server
        NetClient.sendToServer(NotchPackets.SHOP_PURCHASE, buf);
    }

    public static void sendShopManageAction(int action, String text, @Nullable UUID listingId) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeVarInt(action);
        buf.writeUtf(text);
        buf.writeBoolean(listingId != null);
        if (listingId != null) buf.writeUUID(listingId);
        NetClient.sendToServer(NotchPackets.SHOP_MANAGE_ACTION, buf);
    }

    public static void sendShopEditAction(int action, int price) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeVarInt(action);
        buf.writeVarInt(price);
        NetClient.sendToServer(NotchPackets.SHOP_EDIT_ACTION, buf);
    }

    public static void sendShopWithdraw(UUID shopId) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(shopId);
        NetClient.sendToServer(NotchPackets.SHOP_WITHDRAW, buf);
    }

    public static void sendTradeOfferCreate(long price, long giveCoins, String target) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeVarLong(price);
        buf.writeVarLong(giveCoins);
        buf.writeUtf(target);
        NetClient.sendToServer(NotchPackets.TRADE_OFFER_CREATE, buf);
    }

    public static void sendTradeOfferAction(UUID offerId, int action) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(offerId);
        buf.writeVarInt(action);
        NetClient.sendToServer(NotchPackets.TRADE_OFFER_ACTION, buf);
    }

    public static void sendCosmeticBuy(String offerId) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUtf(offerId);
        NetClient.sendToServer(NotchPackets.COSMETIC_BUY, buf);
    }

    public static void sendEnchanterAction(int action, String enchantId) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeVarInt(action);
        buf.writeUtf(enchantId);
        NetClient.sendToServer(NotchPackets.ENCHANTER_ACTION, buf);
    }

    public static void sendNpcPreset(UUID npcId, int action, String name) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeVarInt(action);
        buf.writeUtf(name);
        NetClient.sendToServer(NotchPackets.NPC_PRESET, buf);
    }

    public static void sendNpcShare(UUID npcId, int action, String payload) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeVarInt(action);
        buf.writeUtf(payload, net.fugginbeenus.notchcurrency.npc.NpcShareCodec.MAX_WIRE_CHARS);
        NetClient.sendToServer(NotchPackets.NPC_SHARE, buf);
    }

    public static void sendNpcFlavor(UUID npcId, String subtitle, String voice, int voicePitch) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeUtf(subtitle);
        buf.writeUtf(voice);
        buf.writeVarInt(voicePitch);
        NetClient.sendToServer(NotchPackets.NPC_SET_FLAVOR, buf);
    }

    public static void sendNpcScheduleOpen(UUID npcId) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        NetClient.sendToServer(NotchPackets.NPC_SCHEDULE_OPEN, buf);
    }

    public static void sendNpcScheduleSave(UUID npcId, net.minecraft.nbt.CompoundTag nbt) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeNbt(nbt);
        NetClient.sendToServer(NotchPackets.NPC_SCHEDULE_SAVE, buf);
    }

    /** Ask for the anchor tool bound to one schedule entry. */
    public static void sendNpcScheduleTool(UUID npcId, int entryIndex) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeVarInt(entryIndex);
        NetClient.sendToServer(NotchPackets.NPC_SCHEDULE_TOOL, buf);
    }

    public static void registerNpcScheduleReceiver() {
        NetClient.registerClientReceiver(NotchPackets.NPC_SCHEDULE_DATA, (client, buf) -> {
            UUID npcId = buf.readUUID();
            boolean dimensionOk = buf.readBoolean();
            net.minecraft.nbt.CompoundTag nbt = buf.readNbt();
            client.execute(() -> {
                var schedule = nbt == null
                        ? new net.fugginbeenus.notchcurrency.npc.schedule.NpcSchedule()
                        : net.fugginbeenus.notchcurrency.npc.schedule.NpcSchedule.fromNbt(nbt);
                Minecraft.getInstance().setScreen(
                        new net.fugginbeenus.notchcurrency.client.NpcScheduleScreen(npcId, dimensionOk, schedule));
            });
        });
    }

    public static void registerNpcPresetReceiver() {
        NetClient.registerClientReceiver(NotchPackets.NPC_PRESET_LIST, (client, buf) -> {
            UUID npcId = buf.readUUID();
            int count = buf.readVarInt();
            java.util.List<String> names = new java.util.ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                names.add(buf.readUtf(64));
            }
            client.execute(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (net.fugginbeenus.notchcurrency.compat.Render.currentScreen() instanceof net.fugginbeenus.notchcurrency.client.NpcPresetScreen s
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
            String code = buf.readUtf(net.fugginbeenus.notchcurrency.npc.NpcShareCodec.MAX_WIRE_CHARS);
            client.execute(() -> {
                Minecraft mc = Minecraft.getInstance();
                mc.keyboardHandler.setClipboard(code);
                if (mc.player != null) {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(mc.player, net.minecraft.network.chat.Component.literal(
                                    "Share code copied. Paste it anywhere, or into a .npc file.")
                            .withStyle(net.minecraft.ChatFormatting.GREEN));
                }
            });
        });
    }

    public static void sendNpcOpenEquip(UUID npcId) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        NetClient.sendToServer(NotchPackets.NPC_EQUIP, buf);
    }

    public static void registerNpcDialogueReceiver() {
        NetClient.registerClientReceiver(NotchPackets.NPC_DIALOGUE_OPEN, (client, buf) -> {
            UUID npcId = buf.readUUID();
            String npcName = buf.readUtf();
            String nodeId = buf.readUtf();
            String text = buf.readUtf();
            int count = buf.readVarInt();
            int[] indices = new int[count];
            String[] labels = new String[count];
            boolean[] enabled = new boolean[count];
            for (int i = 0; i < count; i++) {
                indices[i] = buf.readVarInt();
                labels[i] = buf.readUtf();
                enabled[i] = buf.readBoolean();
            }
            client.execute(() -> {
                Minecraft mc = Minecraft.getInstance();
                if (nodeId.isEmpty()) {
                    // Close signal: only if the dialogue screen is up.
                    if (net.fugginbeenus.notchcurrency.compat.Render.currentScreen() instanceof net.fugginbeenus.notchcurrency.client.NpcDialogueScreen) {
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
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeUtf(nodeId);
        buf.writeVarInt(choiceIndex);
        NetClient.sendToServer(NotchPackets.NPC_DIALOGUE_CHOICE, buf);
    }

    public static void sendNpcDialogueTemplate(UUID npcId) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        NetClient.sendToServer(NotchPackets.NPC_DIALOGUE_TEMPLATE, buf);
    }

    public static void sendNpcDialogueClear(UUID npcId) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        NetClient.sendToServer(NotchPackets.NPC_DIALOGUE_CLEAR, buf);
    }

    public static void registerNpcStudioReceiver() {
        NetClient.registerClientReceiver(NotchPackets.NPC_STUDIO_DATA, (client, buf) -> {
            UUID npcId = buf.readUUID();
            net.minecraft.nbt.CompoundTag tree = buf.readNbt();
            client.execute(() -> {
                var parsed = net.fugginbeenus.notchcurrency.npc.dialogue.DialogueTree.fromNbt(
                        tree == null ? new net.minecraft.nbt.CompoundTag() : tree);
                if (nextStudioOpensQuickLines) {
                    nextStudioOpensQuickLines = false;
                    Minecraft.getInstance().setScreen(
                            new net.fugginbeenus.notchcurrency.client.QuickLinesScreen(npcId, parsed));
                } else {
                    Minecraft.getInstance().setScreen(
                            new net.fugginbeenus.notchcurrency.client.DialogueStudioScreen(npcId, parsed));
                }
            });
        });
    }

    public static void registerNpcActionsReceiver() {
        NetClient.registerClientReceiver(NotchPackets.NPC_ACTIONS_DATA, (client, buf) -> {
            UUID npcId = buf.readUUID();
            net.minecraft.nbt.CompoundTag nbt = buf.readNbt();
            client.execute(() -> {
                var parsed = net.fugginbeenus.notchcurrency.npc.action.NpcActions.fromNbt(nbt);
                Minecraft.getInstance().setScreen(
                        new net.fugginbeenus.notchcurrency.client.NpcActionsScreen(npcId, parsed));
            });
        });
    }

    public static void registerCurrencySyncReceiver() {
        NetClient.registerClientReceiver(NotchPackets.WAYSTONE_FEE_SYNC, (client, buf) -> {
            boolean enabled = buf.readBoolean();
            int fee = buf.readVarInt();
            int dimensionalFee = buf.readVarInt();
            client.execute(() -> net.fugginbeenus.notchcurrency.client.WaystoneFees.set(enabled, fee, dimensionalFee));
        });

        NetClient.registerClientReceiver(NotchPackets.CURRENCY_SYNC, (client, buf) -> {
            String itemName = buf.readUtf(64);
            byte[] coin = buf.readBoolean() ? buf.readByteArray() : null;
            byte[] tails = buf.readBoolean() ? buf.readByteArray() : null;
            client.execute(() -> net.fugginbeenus.notchcurrency.client.CurrencyPackGenerator
                    .applyServerData(client, itemName, coin, tails));
        });
    }

    public static void registerBountyTrackerReceiver() {
        NetClient.registerClientReceiver(NotchPackets.BOUNTY_TRACKER, (client, buf) -> {
            int count = buf.readVarInt();
            var list = new java.util.ArrayList<net.fugginbeenus.notchcurrency.client.BountyTrackerHud.Entry>();
            for (int i = 0; i < count; i++) {
                String desc = buf.readUtf();
                boolean kill = buf.readBoolean();
                String target = buf.readUtf();
                int prog = buf.readVarInt();
                int req = buf.readVarInt();
                long expiry = buf.readLong();
                String rarity = buf.readUtf();
                list.add(new net.fugginbeenus.notchcurrency.client.BountyTrackerHud.Entry(
                        desc, kill, target, prog, req, expiry, rarity));
            }
            client.execute(() -> net.fugginbeenus.notchcurrency.client.BountyTrackerHud.setEntries(list));
        });
    }

    private static boolean nextStudioOpensQuickLines = false;

    public static void sendNpcStudioOpen(UUID npcId) {
        sendNpcStudioOpen(npcId, false);
    }

    public static void sendNpcStudioOpen(UUID npcId, boolean quickLines) {
        nextStudioOpensQuickLines = quickLines;
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        NetClient.sendToServer(NotchPackets.NPC_STUDIO_OPEN, buf);
    }

    public static void sendNpcBillboard(UUID npcId, String text) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeUtf(text);
        NetClient.sendToServer(NotchPackets.NPC_BILLBOARD, buf);
    }

    public static void sendFactionPick(UUID npcId, int action, String factionId) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeVarInt(action);
        buf.writeUtf(factionId);
        NetClient.sendToServer(NotchPackets.NPC_FACTION_PICK, buf);
    }

    public static void sendMailTakeAll() {
        var buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(new java.util.UUID(0L, 0L));
        NetClient.sendToServer(NotchPackets.MAIL_TAKE, buf);
    }

    public static void sendMailTab(int tab, UUID aim) {
        var buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeVarInt(tab);
        buf.writeBoolean(aim != null);
        if (aim != null) buf.writeUUID(aim);
        NetClient.sendToServer(NotchPackets.MAIL_TAB, buf);
    }

    public static void sendMailPostOpen() {
        NetClient.sendToServer(NotchPackets.MAIL_POST_OPEN,
                net.fugginbeenus.notchcurrency.compat.Net.buf());
    }

    public static void sendMailTrade(UUID recipient) {
        var buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(recipient);
        NetClient.sendToServer(NotchPackets.MAIL_TRADE, buf);
    }

    public static void sendMailPost(UUID recipient, String note, long coins) {
        var buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(recipient);
        buf.writeUtf(note, 128);
        buf.writeVarLong(Math.max(0L, coins));
        NetClient.sendToServer(NotchPackets.MAIL_SEND, buf);
    }

    public static void sendNpcModelWant(String id) {
        var buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUtf(id, 64);
        NetClient.sendToServer(NotchPackets.NPC_MODEL_WANT, buf);
    }

    /** One piece of a model on its way up to the server. */
    public static void sendNpcModelPush(int phase, String id, byte[] part, int announcedBytes) {
        var buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeByte(phase);
        buf.writeUtf(id, 64);
        if (phase == net.fugginbeenus.notchcurrency.npcmodel.NpcModelStream.PHASE_CHUNK) {
            buf.writeByteArray(part);
        }
        if (phase == net.fugginbeenus.notchcurrency.npcmodel.NpcModelStream.PHASE_BEGIN) {
            buf.writeVarInt(announcedBytes);
        }
        NetClient.sendToServer(NotchPackets.NPC_MODEL_PUSH, buf);
    }

    public static void registerNpcModelReceivers() {
        NetClient.registerClientReceiver(NotchPackets.NPC_MODEL_LIST, (client, buf) -> {
            boolean mayShare = buf.readBoolean();
            int count = buf.readVarInt();
            java.util.Map<String, String> offered = new java.util.LinkedHashMap<>();
            for (int i = 0; i < count; i++) {
                String id = buf.readUtf(64);
                offered.put(id, buf.readUtf(32));
            }
            client.execute(() ->
                    net.fugginbeenus.notchcurrency.client.npcmodel.NpcModelDownloads
                            .onList(offered, mayShare));
        });

        NetClient.registerClientReceiver(NotchPackets.NPC_MODEL_SEND, (client, buf) -> {
            int phase = buf.readByte();
            String id = buf.readUtf(64);
            byte[] part = phase == net.fugginbeenus.notchcurrency.npcmodel.NpcModelStream.PHASE_CHUNK
                    ? buf.readByteArray(net.fugginbeenus.notchcurrency.npcmodel.NpcModelStream.CHUNK_BYTES)
                    : new byte[0];
            int announced = phase == net.fugginbeenus.notchcurrency.npcmodel.NpcModelStream.PHASE_BEGIN
                    ? buf.readVarInt() : 0;
            client.execute(() -> net.fugginbeenus.notchcurrency.client.npcmodel.NpcModelDownloads
                    .onPiece(phase, id, part, announced));
        });
    }

    public static void registerModelReloadReceiver() {
        NetClient.registerClientReceiver(NotchPackets.NPC_MODELS_RELOAD, (client, buf) ->
                client.execute(() ->
                        net.fugginbeenus.notchcurrency.client.npcmodel.NpcModelPacks.reload(client, true)));
    }

    public static void registerMailAimReceiver() {
        NetClient.registerClientReceiver(NotchPackets.MAIL_AIM, (client, buf) -> {
            UUID recipient = buf.readUUID();
            client.execute(() -> {
                net.fugginbeenus.notchcurrency.client.MailTabs.aimAt(recipient);
                net.fugginbeenus.notchcurrency.client.MailPostScreen.preselect(recipient);
            });
        });
    }

    public static void registerMailRecipientsReceiver() {
        NetClient.registerClientReceiver(NotchPackets.MAIL_RECIPIENTS, (client, buf) -> {
            int count = buf.readVarInt();
            java.util.List<net.fugginbeenus.notchcurrency.client.MailPostScreen.Recipient> list =
                    new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                UUID id = buf.readUUID();
                String name = buf.readUtf(32);
                boolean online = buf.readBoolean();
                list.add(new net.fugginbeenus.notchcurrency.client.MailPostScreen.Recipient(id, name, online));
            }
            client.execute(() -> net.fugginbeenus.notchcurrency.client.MailPostScreen.setRecipients(list));
        });
    }

    public static void registerFactionListReceiver() {
        NetClient.registerClientReceiver(NotchPackets.NPC_FACTION_LIST, (client, buf) -> {
            UUID npcId = buf.readUUID();
            String currentId = buf.readUtf();
            int count = buf.readVarInt();
            java.util.List<net.fugginbeenus.notchcurrency.client.NpcFactionPickerScreen.Entry> entries =
                    new java.util.ArrayList<>();
            for (int i = 0; i < count; i++) {
                String id = buf.readUtf();
                String name = buf.readUtf();
                net.minecraft.ChatFormatting color = net.fugginbeenus.notchcurrency.compat.Colors.byName(buf.readUtf());
                int members = buf.readVarInt();
                entries.add(new net.fugginbeenus.notchcurrency.client.NpcFactionPickerScreen.Entry(
                        id, name, color == null ? net.minecraft.ChatFormatting.WHITE : color, members));
            }
            client.execute(() -> Minecraft.getInstance().setScreen(
                    new net.fugginbeenus.notchcurrency.client.NpcFactionPickerScreen(npcId, currentId, entries)));
        });
    }

    public static void sendRecruiterAction(UUID npcId, int action, String name, String color,
                                          int fee, boolean open) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeVarInt(action);
        buf.writeUtf(name);
        buf.writeUtf(color);
        buf.writeVarInt(fee);
        buf.writeBoolean(open);
        NetClient.sendToServer(NotchPackets.NPC_RECRUITER_ACTION, buf);
    }

    public static void registerRecruiterReceiver() {
        NetClient.registerClientReceiver(NotchPackets.NPC_RECRUITER_OPEN, (client, buf) -> {
            UUID npcId = buf.readUUID();
            String factionId = buf.readUtf();
            String factionName = buf.readUtf();
            String colorName = buf.readUtf();
            int members = buf.readVarInt();
            boolean alreadyIn = buf.readBoolean();
            boolean canFound = buf.readBoolean();
            buf.readBoolean(); // may-assign: reserved for the Role tab picker
            String motto = buf.readUtf();
            int fee = buf.readVarInt();
            boolean open = buf.readBoolean();
            boolean canManage = buf.readBoolean();
            client.execute(() -> {
                net.minecraft.ChatFormatting color = net.fugginbeenus.notchcurrency.compat.Colors.byName(colorName);
                Minecraft.getInstance().setScreen(
                        new net.fugginbeenus.notchcurrency.client.NpcRecruiterScreen(npcId, factionId,
                                factionName, color == null ? net.minecraft.ChatFormatting.WHITE : color,
                                members, alreadyIn, canFound, motto, fee, open, canManage));
            });
        });
    }

    public static void sendNpcActionsOpen(UUID npcId) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        NetClient.sendToServer(NotchPackets.NPC_ACTIONS_OPEN, buf);
    }

    public static void sendNpcActionsSave(UUID npcId, net.minecraft.nbt.CompoundTag actionsNbt) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeNbt(actionsNbt);
        NetClient.sendToServer(NotchPackets.NPC_ACTIONS_SAVE, buf);
    }

    public static void sendNpcStudioSave(UUID npcId, net.minecraft.nbt.CompoundTag treeNbt) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeNbt(treeNbt);
        NetClient.sendToServer(NotchPackets.NPC_STUDIO_SAVE, buf);
    }

    public static void sendNpcSetBehavior(UUID npcId, int modeOrdinal, int radius, String followName, int movesBits) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeVarInt(modeOrdinal);
        buf.writeVarInt(radius);
        buf.writeUtf(followName);
        buf.writeVarInt(movesBits);
        NetClient.sendToServer(NotchPackets.NPC_SET_BEHAVIOR, buf);
    }

    public static void sendNpcSetAppearance(UUID npcId, String model, String skinType, String skinValue,
                                            boolean slim, float scaleX, float scaleY, float scaleZ,
                                            float nameOffset) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeUtf(model);
        buf.writeUtf(skinType);
        buf.writeUtf(skinValue);
        buf.writeBoolean(slim);
        buf.writeFloat(scaleX);
        buf.writeFloat(scaleY);
        buf.writeFloat(scaleZ);
        buf.writeFloat(nameOffset);
        NetClient.sendToServer(NotchPackets.NPC_SET_APPEARANCE, buf);
    }

    public static void sendNpcSetRole(UUID npcId, int roleOrdinal) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeVarInt(roleOrdinal);
        NetClient.sendToServer(NotchPackets.NPC_SET_ROLE, buf);
    }

    public static void sendNpcSetName(UUID npcId, String name) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeUtf(name);
        NetClient.sendToServer(NotchPackets.NPC_SET_NAME, buf);
    }

    public static void sendNpcSetFarewell(UUID npcId, String text) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        buf.writeUtf(text);
        NetClient.sendToServer(NotchPackets.NPC_SET_FAREWELL, buf);
    }

    public static void sendNpcPickup(UUID npcId) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        NetClient.sendToServer(NotchPackets.NPC_PICKUP, buf);
    }

    public static void sendNpcDelete(UUID npcId) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(npcId);
        NetClient.sendToServer(NotchPackets.NPC_DELETE, buf);
    }

}