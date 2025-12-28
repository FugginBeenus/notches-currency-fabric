package net.fugginbeenus.notchcurrency.net;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fugginbeenus.notchcurrency.client.ShopkeeperSettingsScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.Identifier;

import java.util.UUID;
import java.util.function.IntConsumer;

public final class NotchPacketsClient {
    // Reuse the same identifiers defined in the common class
    public static final Identifier BALANCE_SYNC    = NotchPackets.BALANCE_SYNC;
    public static final Identifier BALANCE_REQUEST = NotchPackets.BALANCE_REQUEST;

    public static final Identifier TRADE_OPEN     = NotchPackets.TRADE_OPEN;
    public static final Identifier TRADE_UPDATE   = NotchPackets.TRADE_UPDATE;
    public static final Identifier TRADE_CANCEL   = NotchPackets.TRADE_CANCEL;
    public static final Identifier TRADE_COMPLETE = NotchPackets.TRADE_COMPLETE;

    // Shopkeeper settings
    public static final Identifier SHOPKEEPER_SETTINGS_OPEN = NotchPackets.SHOPKEEPER_SETTINGS_OPEN;

    private NotchPacketsClient() {}

    public static void requestBalance() {
        ClientPlayNetworking.send(BALANCE_REQUEST, PacketByteBufs.empty());
    }

    public static void registerBalanceReceiver(IntConsumer onBalance) {
        ClientPlayNetworking.registerGlobalReceiver(BALANCE_SYNC, (client, handler, buf, responseSender) -> {
            int bal = buf.readVarInt();
            client.execute(() -> onBalance.accept(bal));
        });
    }

    public static void registerShopkeeperSettingsReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(SHOPKEEPER_SETTINGS_OPEN, (client, handler, buf, responseSender) -> {
            UUID shopId = buf.readUuid();
            UUID npcId = buf.readUuid();
            String shopName = buf.readString();
            String ownerName = buf.readString();
            String dialog = buf.readString();

            client.execute(() -> {
                // Open the settings screen with the IDs and current data
                MinecraftClient.getInstance().setScreen(new ShopkeeperSettingsScreen(shopId, npcId, shopName, ownerName, dialog));
            });
        });
    }

    // Shopkeeper settings update packets (client -> server)
    public static void sendSkinUpdate(UUID npcId, String skinType, String skinValue) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeString(skinType);
        buf.writeString(skinValue);
        ClientPlayNetworking.send(NotchPackets.SHOPKEEPER_UPDATE_SKIN, buf);
    }

    public static void sendNpcNameUpdate(UUID npcId, String name) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeString(name);
        ClientPlayNetworking.send(NotchPackets.SHOPKEEPER_UPDATE_NAME, buf);
    }

    public static void sendShopNameUpdate(UUID shopId, String name) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(shopId);
        buf.writeString(name);
        ClientPlayNetworking.send(NotchPackets.SHOPKEEPER_UPDATE_SHOP_NAME, buf);
    }

    public static void sendDialogUpdate(UUID npcId, String dialog) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeString(dialog);
        ClientPlayNetworking.send(NotchPackets.SHOPKEEPER_UPDATE_DIALOG, buf);
    }

    public static void sendOpenShopRequest(UUID shopId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(shopId);
        ClientPlayNetworking.send(NotchPackets.SHOPKEEPER_OPEN_SHOP, buf);
    }

    public static void sendDeleteNpc(UUID npcId, UUID shopId) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(npcId);
        buf.writeUuid(shopId);
        ClientPlayNetworking.send(NotchPackets.SHOPKEEPER_DELETE_NPC, buf);
    }
}