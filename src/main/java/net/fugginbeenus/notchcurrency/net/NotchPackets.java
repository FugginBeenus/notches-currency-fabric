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

    // Auction GUI cancel-listing packet (click a listing in the My Listings popup)
    public static final Identifier AUCTION_CANCEL  = new Identifier(NotchCurrency.MOD_ID, "auction_cancel");

    // Auction GUI create-listing packet (from the "List an Item" screen): price + days
    public static final Identifier AUCTION_LIST    = new Identifier(NotchCurrency.MOD_ID, "auction_list");

    // Raffle admin GUI save packet: price, cut%, maxTickets, intervalMin, enabled
    public static final Identifier RAFFLE_ADMIN_SAVE = new Identifier(NotchCurrency.MOD_ID, "raffle_admin_save");

    // Bounty board GUI action packet: bounty UUID + action (0=take, 1=claim, 2=turnin)
    public static final Identifier BOUNTY_ACTION = new Identifier(NotchCurrency.MOD_ID, "bounty_action");

    // Bounty admin GUI save packet: enabled, activeCount, takeLimit, durationMinutes
    public static final Identifier BOUNTY_ADMIN_SAVE = new Identifier(NotchCurrency.MOD_ID, "bounty_admin_save");

    // Loan GUI action packet: action (0=borrow, 1=repay) + amount
    public static final Identifier LOAN_ACTION = new Identifier(NotchCurrency.MOD_ID, "loan_action");

    // Slot machine GUI: spin with a bet (varLong)
    public static final Identifier SLOTS_SPIN = new Identifier(NotchCurrency.MOD_ID, "slots_spin");

    // Coin flip GUI: flip with a guessed side (boolean heads) + bet (varLong)
    public static final Identifier COINFLIP_FLIP = new Identifier(NotchCurrency.MOD_ID, "coinflip_flip");

    // ---- Notch NPC editor ----
    // Server -> client: open the NPC editor (npc uuid, roleOrdinal, name, ownerName, canEdit)
    public static final Identifier NPC_EDITOR_OPEN = new Identifier(NotchCurrency.MOD_ID, "npc_editor_open");
    // Client -> server: reopen the NPC editor (npc uuid) — used by sub-screens' Back buttons
    public static final Identifier NPC_EDITOR_REOPEN = new Identifier(NotchCurrency.MOD_ID, "npc_editor_reopen");
    // Client -> server: set the NPC's role (npc uuid, roleOrdinal)
    public static final Identifier NPC_SET_ROLE = new Identifier(NotchCurrency.MOD_ID, "npc_set_role");
    // Client -> server: set the NPC's display name (npc uuid, name)
    public static final Identifier NPC_SET_NAME = new Identifier(NotchCurrency.MOD_ID, "npc_set_name");
    // Client -> server: set the NPC's goodbye line (uuid, text; "" clears)
    public static final Identifier NPC_SET_FAREWELL = new Identifier(NotchCurrency.MOD_ID, "npc_set_farewell");
    // Client -> server: pick the NPC up into an item (npc uuid)
    public static final Identifier NPC_PICKUP = new Identifier(NotchCurrency.MOD_ID, "npc_pickup");
    // Client -> server: delete the NPC (npc uuid)
    public static final Identifier NPC_DELETE = new Identifier(NotchCurrency.MOD_ID, "npc_delete");
    // Client -> server: set the NPC's appearance (npc uuid, texture id, scale)
    public static final Identifier NPC_SET_APPEARANCE = new Identifier(NotchCurrency.MOD_ID, "npc_set_appearance");
    // Client -> server: set the NPC's behavior (npc uuid, mode ordinal, wander radius,
    // follow-player name ["" = owner], moves bits: 1=avoid monsters 2=watch players)
    public static final Identifier NPC_SET_BEHAVIOR = new Identifier(NotchCurrency.MOD_ID, "npc_set_behavior");
    // Client -> server: set NPC stat toggles (npc uuid, bitmask: 1=protected 2=silent 4=glowing 8=nameplate
    // 16=no gravity 32=opens doors 64=leashable 128=invisible)
    public static final Identifier NPC_SET_STATS = new Identifier(NotchCurrency.MOD_ID, "npc_set_stats");
    // Client -> server: set NPC slider attributes (npc uuid, max health, speed %, regen)
    public static final Identifier NPC_SET_ATTRS = new Identifier(NotchCurrency.MOD_ID, "npc_set_attrs");
    // Client -> server: enchanter action (action: 0=repair 1=upgrade 2=extract, enchantment id) —
    // routed to the player's open EnchanterScreenHandler
    public static final Identifier ENCHANTER_ACTION = new Identifier(NotchCurrency.MOD_ID, "enchanter_action");
    // Client -> server: buy a cosmetic (offer id) while the cosmetics shop is open
    public static final Identifier COSMETIC_BUY = new Identifier(NotchCurrency.MOD_ID, "cosmetic_buy");
    // Client -> server: submit a trade offer (price, target name) from the open create screen
    public static final Identifier TRADE_OFFER_CREATE = new Identifier(NotchCurrency.MOD_ID, "trade_offer_create");
    // Client -> server: trade offer action (offer id, action: 0=accept 1=cancel)
    public static final Identifier TRADE_OFFER_ACTION = new Identifier(NotchCurrency.MOD_ID, "trade_offer_action");
    // Client -> server: shop manage action (action: 0=rename 1=greeting 2=toggle 3=edit 4=new,
    // text, hasUuid + listing uuid) — routed to the open ShopManageScreenHandler
    public static final Identifier SHOP_MANAGE_ACTION = new Identifier(NotchCurrency.MOD_ID, "shop_manage_action");
    // Client -> server: listing editor action (action: 0=save 1=deposit 2=return 3=delete 4=back
    // 5=clear barter, coin price) — routed to the open ShopListingEditScreenHandler
    public static final Identifier SHOP_EDIT_ACTION = new Identifier(NotchCurrency.MOD_ID, "shop_edit_action");
    // Client -> server: preset action (npc uuid, action: 0=open 1=save 2=load 3=delete, preset name)
    public static final Identifier NPC_PRESET = new Identifier(NotchCurrency.MOD_ID, "npc_preset");
    // Server -> client: the saved preset list (npc uuid, count, names) — opens/updates the preset screen
    public static final Identifier NPC_PRESET_LIST = new Identifier(NotchCurrency.MOD_ID, "npc_preset_list");
    // Client -> server: open the NPC equipment screen (npc uuid)
    public static final Identifier NPC_EQUIP = new Identifier(NotchCurrency.MOD_ID, "npc_equip");
    // Client -> server: patrol edit (npc uuid, action, value): 0=give route tool, 1=clear route,
    // 2=finalize (take tools back), 3=set speed (value=preset index)
    public static final Identifier NPC_PATROL = new Identifier(NotchCurrency.MOD_ID, "npc_patrol");
    // Client -> server: set pose preset (npc uuid, pose id)
    public static final Identifier NPC_SET_POSE = new Identifier(NotchCurrency.MOD_ID, "npc_set_pose");
    // Client -> server: set one custom-pose part (npc uuid, part [-1=reset all], degX, degY, degZ)
    public static final Identifier NPC_POSE_PART = new Identifier(NotchCurrency.MOD_ID, "npc_pose_part");
    // Client -> server: move/rotate the whole NPC (npc uuid, dx, dy, dz, yawDeg, applyYaw)
    public static final Identifier NPC_TRANSFORM = new Identifier(NotchCurrency.MOD_ID, "npc_transform");
    // Client -> server: set the idle animation layered on the pose (npc uuid, anim id)
    public static final Identifier NPC_SET_ANIM = new Identifier(NotchCurrency.MOD_ID, "npc_set_anim");
    // Server -> client: the player's taken bounties, for the on-screen tracker HUD
    // (count, then per bounty: desc, isKill, targetItemId, prog, req, expiry, rarity)
    public static final Identifier BOUNTY_TRACKER = new Identifier(NotchCurrency.MOD_ID, "bounty_tracker");
    // Server -> client: the server's custom-currency skin, pushed on join so every player sees the
    // admin's coin (itemName, hasCoin?, coinPngBytes, hasTails?, tailsPngBytes; all-empty = clear)
    public static final Identifier CURRENCY_SYNC = new Identifier(NotchCurrency.MOD_ID, "currency_sync");

    // ---- NPC dialogue ----
    // Server -> client: show a dialogue node (uuid, npcName, nodeId ["" = close], text, choices[idx,label,enabled])
    public static final Identifier NPC_DIALOGUE_OPEN = new Identifier(NotchCurrency.MOD_ID, "npc_dialogue_open");
    // Client -> server: a dialogue choice was clicked (npc uuid, nodeId, choice index)
    public static final Identifier NPC_DIALOGUE_CHOICE = new Identifier(NotchCurrency.MOD_ID, "npc_dialogue_choice");
    // Client -> server: create the starter dialogue template on this NPC (npc uuid)
    public static final Identifier NPC_DIALOGUE_TEMPLATE = new Identifier(NotchCurrency.MOD_ID, "npc_dialogue_template");
    // Client -> server: clear this NPC's dialogue (npc uuid)
    public static final Identifier NPC_DIALOGUE_CLEAR = new Identifier(NotchCurrency.MOD_ID, "npc_dialogue_clear");
    // Client -> server: request the dialogue studio for an NPC (npc uuid)
    public static final Identifier NPC_STUDIO_OPEN = new Identifier(NotchCurrency.MOD_ID, "npc_studio_open");
    // Server -> client: the NPC's full dialogue tree for editing (npc uuid, tree NBT)
    public static final Identifier NPC_STUDIO_DATA = new Identifier(NotchCurrency.MOD_ID, "npc_studio_data");
    // Client -> server: save an edited dialogue tree (npc uuid, tree NBT)
    public static final Identifier NPC_STUDIO_SAVE = new Identifier(NotchCurrency.MOD_ID, "npc_studio_save");
    // Client -> server: set the dialogue style (npc uuid, mode ordinal: 0=window 1=chat)
    public static final Identifier NPC_DIALOGUE_MODE = new Identifier(NotchCurrency.MOD_ID, "npc_dialogue_mode");

    // ATM withdraw (client -> server)
    public static final Identifier ATM_WITHDRAW    = new Identifier(NotchCurrency.MOD_ID, "atm_withdraw");

    // Shop packets (client -> server); listing edits go through SHOP_MANAGE_ACTION / SHOP_EDIT_ACTION
    public static final Identifier SHOP_PURCHASE   = new Identifier(NotchCurrency.MOD_ID, "shop_purchase");
    public static final Identifier SHOP_WITHDRAW = new Identifier(NotchCurrency.MOD_ID, "shop_withdraw");

    private NotchPackets() {}

    // ---- Server -> Client helpers ----
    public static void sendBalance(ServerPlayerEntity sp, long value) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarLong(value);
        ServerPlayNetworking.send(sp, BALANCE_SYNC, buf);
    }
}