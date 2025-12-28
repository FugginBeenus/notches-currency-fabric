package net.fugginbeenus.notchcurrency.net;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class NotchPackets {
    public static final Identifier BALANCE_SYNC    = new Identifier(NotchCurrency.MOD_ID, "balance_sync");
    public static final Identifier BALANCE_REQUEST = new Identifier(NotchCurrency.MOD_ID, "balance_request");

    // Trade channels
    public static final Identifier TRADE_OPEN      = new Identifier(NotchCurrency.MOD_ID, "trade_open");
    public static final Identifier TRADE_UPDATE    = new Identifier(NotchCurrency.MOD_ID, "trade_update");
    public static final Identifier TRADE_CANCEL    = new Identifier(NotchCurrency.MOD_ID, "trade_cancel");
    public static final Identifier TRADE_COMPLETE  = new Identifier(NotchCurrency.MOD_ID, "trade_complete");

    // Auction GUI bid packet (for future use / right-click GUI etc)
    public static final Identifier BID_REQUEST     = new Identifier(NotchCurrency.MOD_ID, "bid_request");

    // ATM withdraw (client -> server)
    public static final Identifier ATM_WITHDRAW    = new Identifier(NotchCurrency.MOD_ID, "atm_withdraw");

    // Shop packets (client -> server)
    public static final Identifier SHOP_PURCHASE   = new Identifier(NotchCurrency.MOD_ID, "shop_purchase");
    public static final Identifier SHOP_ADD_LISTING = new Identifier(NotchCurrency.MOD_ID, "shop_add_listing");
    public static final Identifier SHOP_UPDATE_PRICE = new Identifier(NotchCurrency.MOD_ID, "shop_update_price");
    public static final Identifier SHOP_REMOVE_LISTING = new Identifier(NotchCurrency.MOD_ID, "shop_remove_listing");
    public static final Identifier SHOP_ADD_STOCK = new Identifier(NotchCurrency.MOD_ID, "shop_add_stock");
    public static final Identifier SHOP_SET_BARTER = new Identifier(NotchCurrency.MOD_ID, "shop_set_barter");
    public static final Identifier SHOP_REFRESH   = new Identifier(NotchCurrency.MOD_ID, "shop_refresh");
    public static final Identifier SHOP_SAVE_LISTINGS = new Identifier(NotchCurrency.MOD_ID, "shop_save_listings");
    public static final Identifier SHOP_WITHDRAW = new Identifier(NotchCurrency.MOD_ID, "shop_withdraw");

    // Shopkeeper settings packets (server -> client)
    public static final Identifier SHOPKEEPER_SETTINGS_OPEN = new Identifier(NotchCurrency.MOD_ID, "shopkeeper_settings_open");

    // Shopkeeper settings packets (client -> server)
    public static final Identifier SHOPKEEPER_UPDATE_SKIN = new Identifier(NotchCurrency.MOD_ID, "shopkeeper_update_skin");
    public static final Identifier SHOPKEEPER_UPDATE_NAME = new Identifier(NotchCurrency.MOD_ID, "shopkeeper_update_name");
    public static final Identifier SHOPKEEPER_UPDATE_SHOP_NAME = new Identifier(NotchCurrency.MOD_ID, "shopkeeper_update_shop_name");
    public static final Identifier SHOPKEEPER_UPDATE_DIALOG = new Identifier(NotchCurrency.MOD_ID, "shopkeeper_update_dialog");
    public static final Identifier SHOPKEEPER_OPEN_SHOP = new Identifier(NotchCurrency.MOD_ID, "shopkeeper_open_shop");
    public static final Identifier SHOPKEEPER_DELETE_NPC = new Identifier(NotchCurrency.MOD_ID, "shopkeeper_delete_npc");

    private NotchPackets() {}

    // ---- Server -> Client helpers ----
    public static void sendBalance(ServerPlayerEntity sp, int value) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(value);
        ServerPlayNetworking.send(sp, BALANCE_SYNC, buf);
    }
}