package net.fugginbeenus.notchcurrency.net;

import net.fugginbeenus.notchcurrency.compat.Net;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

public final class NotchPackets {
    public static final ResourceLocation BALANCE_SYNC    = NotchCurrency.id("balance_sync");
    public static final ResourceLocation BALANCE_REQUEST = NotchCurrency.id("balance_request");

    // Trade channels
    public static final ResourceLocation TRADE_OPEN      = NotchCurrency.id("trade_open");
    public static final ResourceLocation TRADE_UPDATE    = NotchCurrency.id("trade_update");
    public static final ResourceLocation TRADE_CANCEL    = NotchCurrency.id("trade_cancel");
    public static final ResourceLocation TRADE_COMPLETE  = NotchCurrency.id("trade_complete");

    // Auction GUI bid packet (for future use / right-click GUI etc)
    public static final ResourceLocation BID_REQUEST     = NotchCurrency.id("bid_request");

    // Auction GUI cancel-listing packet (click a listing in the My Listings popup)
    public static final ResourceLocation AUCTION_CANCEL  = NotchCurrency.id("auction_cancel");

    // Auction GUI create-listing packet (from the "List an Item" screen): price + days
    public static final ResourceLocation AUCTION_LIST    = NotchCurrency.id("auction_list");

    // Raffle admin GUI save packet: price, cut%, maxTickets, intervalMin, enabled
    public static final ResourceLocation RAFFLE_ADMIN_SAVE = NotchCurrency.id("raffle_admin_save");

    // Bounty board GUI action packet: bounty UUID + action (0=take, 1=claim, 2=turnin)
    public static final ResourceLocation BOUNTY_ACTION = NotchCurrency.id("bounty_action");

    // Bounty admin GUI save packet: enabled, activeCount, takeLimit, durationMinutes
    public static final ResourceLocation BOUNTY_ADMIN_SAVE = NotchCurrency.id("bounty_admin_save");

    // Loan GUI action packet: action (0=borrow, 1=repay) + amount
    public static final ResourceLocation LOAN_ACTION = NotchCurrency.id("loan_action");

    // Slot machine GUI: spin with a bet (varLong)
    public static final ResourceLocation SLOTS_SPIN = NotchCurrency.id("slots_spin");

    // Coin flip GUI: flip with a guessed side (boolean heads) + bet (varLong)
    public static final ResourceLocation COINFLIP_FLIP = NotchCurrency.id("coinflip_flip");

    // ---- Notch NPC editor ----
    // Server -> client: open the NPC editor (npc uuid, roleOrdinal, name, ownerName, canEdit)
    public static final ResourceLocation NPC_EDITOR_OPEN = NotchCurrency.id("npc_editor_open");
    // Client -> server: reopen the NPC editor (npc uuid), used by sub-screens' Back buttons
    public static final ResourceLocation NPC_EDITOR_REOPEN = NotchCurrency.id("npc_editor_reopen");
    // Client -> server: set the NPC's role (npc uuid, roleOrdinal)
    public static final ResourceLocation NPC_SET_ROLE = NotchCurrency.id("npc_set_role");
    // Client -> server: set the NPC's display name (npc uuid, name)
    public static final ResourceLocation NPC_SET_NAME = NotchCurrency.id("npc_set_name");
    // Client -> server: set the NPC's goodbye line (uuid, text; "" clears)
    public static final ResourceLocation NPC_SET_FAREWELL = NotchCurrency.id("npc_set_farewell");
    // Client -> server: pick the NPC up into an item (npc uuid)
    public static final ResourceLocation NPC_PICKUP = NotchCurrency.id("npc_pickup");
    // Client -> server: delete the NPC (npc uuid)
    public static final ResourceLocation NPC_DELETE = NotchCurrency.id("npc_delete");
    // Client -> server: set the NPC's appearance (npc uuid, texture id, scale)
    public static final ResourceLocation NPC_SET_APPEARANCE = NotchCurrency.id("npc_set_appearance");
    // Client -> server: set the NPC's behavior (npc uuid, mode ordinal, wander radius,
    // follow-player name ["" = owner], moves bits: 1=avoid monsters 2=watch players)
    public static final ResourceLocation NPC_SET_BEHAVIOR = NotchCurrency.id("npc_set_behavior");
    // Client -> server: set NPC stat toggles (npc uuid, bitmask: 1=protected 2=silent 4=glowing 8=nameplate
    // 16=no gravity 32=opens doors 64=leashable 128=invisible)
    public static final ResourceLocation NPC_SET_STATS = NotchCurrency.id("npc_set_stats");
    // Client -> server: set NPC slider attributes (npc uuid, max health, speed %, regen)
    public static final ResourceLocation NPC_SET_ATTRS = NotchCurrency.id("npc_set_attrs");
    // Client -> server: enchanter action (action: 0=repair 1=upgrade 2=extract, enchantment id),
    // routed to the player's open EnchanterScreenHandler
    public static final ResourceLocation ENCHANTER_ACTION = NotchCurrency.id("enchanter_action");
    // Client -> server: buy a cosmetic (offer id) while the cosmetics shop is open
    public static final ResourceLocation COSMETIC_BUY = NotchCurrency.id("cosmetic_buy");
    // Client -> server: submit a trade offer (price, target name) from the open create screen
    public static final ResourceLocation TRADE_OFFER_CREATE = NotchCurrency.id("trade_offer_create");
    // Client -> server: trade offer action (offer id, action: 0=accept 1=cancel)
    public static final ResourceLocation TRADE_OFFER_ACTION = NotchCurrency.id("trade_offer_action");
    // Client -> server: shop manage action (action: 0=rename 1=greeting 2=toggle 3=edit 4=new,
    // text, hasUuid + listing uuid), routed to the open ShopManageScreenHandler
    public static final ResourceLocation SHOP_MANAGE_ACTION = NotchCurrency.id("shop_manage_action");
    // Client -> server: listing editor action (action: 0=save 1=deposit 2=return 3=delete 4=back
    // 5=clear barter, coin price), routed to the open ShopListingEditScreenHandler
    public static final ResourceLocation SHOP_EDIT_ACTION = NotchCurrency.id("shop_edit_action");
    // Client -> server: preset action (npc uuid, action: 0=open 1=save 2=load 3=delete, preset name)
    public static final ResourceLocation NPC_PRESET = NotchCurrency.id("npc_preset");
    // Server -> client: the saved preset list (npc uuid, count, names), which opens/updates the preset screen
    public static final ResourceLocation NPC_PRESET_LIST = NotchCurrency.id("npc_preset_list");
    // Client -> server: share action (npc uuid, action: 0=copy 1=paste 2=save file 3=load file,
    // payload is the pasted code for 1 and a file name for 2/3)
    public static final ResourceLocation NPC_SHARE = NotchCurrency.id("npc_share");
    // Server -> client: a freshly built share code for the client to put on the clipboard
    public static final ResourceLocation NPC_SHARE_CODE = NotchCurrency.id("npc_share_code");
    // Client -> server: open the NPC equipment screen (npc uuid)
    public static final ResourceLocation NPC_EQUIP = NotchCurrency.id("npc_equip");
    // Client -> server: patrol edit (npc uuid, action, value): 0=give route tool, 1=clear route,
    // 2=finalize (take tools back), 3=set speed (value=preset index)
    public static final ResourceLocation NPC_PATROL = NotchCurrency.id("npc_patrol");
    // Client -> server: set pose preset (npc uuid, pose id)
    public static final ResourceLocation NPC_SET_POSE = NotchCurrency.id("npc_set_pose");
    // Client -> server: set one custom-pose part (npc uuid, part [-1=reset all], degX, degY, degZ)
    public static final ResourceLocation NPC_POSE_PART = NotchCurrency.id("npc_pose_part");
    // Client -> server: move/rotate the whole NPC (npc uuid, dx, dy, dz, yawDeg, applyYaw)
    public static final ResourceLocation NPC_TRANSFORM = NotchCurrency.id("npc_transform");
    // Client -> server: set the idle animation layered on the pose (npc uuid, anim id)
    public static final ResourceLocation NPC_MODELS_RELOAD = NotchCurrency.id("npc_models_reload");
    public static final ResourceLocation NPC_MODEL_SPIKE = NotchCurrency.id("npc_model_spike");
    public static final ResourceLocation NPC_SET_CLIP = NotchCurrency.id("npc_set_clip");
    public static final ResourceLocation NPC_SET_ANIM = NotchCurrency.id("npc_set_anim");
    // Server -> client: the player's taken bounties, for the on-screen tracker HUD
    // (count, then per bounty: desc, isKill, targetItemId, prog, req, expiry, rarity)
    public static final ResourceLocation BOUNTY_TRACKER = NotchCurrency.id("bounty_tracker");
    // Server -> client: the server's custom-currency skin, pushed on join so every player sees the
    // admin's coin (itemName, hasCoin?, coinPngBytes, hasTails?, tailsPngBytes; all-empty = clear)
    public static final ResourceLocation CURRENCY_SYNC = NotchCurrency.id("currency_sync");

    // ---- NPC dialogue ----
    // Server -> client: show a dialogue node (uuid, npcName, nodeId ["" = close], text, choices[idx,label,enabled])
    public static final ResourceLocation NPC_DIALOGUE_OPEN = NotchCurrency.id("npc_dialogue_open");
    // Client -> server: a dialogue choice was clicked (npc uuid, nodeId, choice index)
    public static final ResourceLocation NPC_DIALOGUE_CHOICE = NotchCurrency.id("npc_dialogue_choice");
    // Client -> server: create the starter dialogue template on this NPC (npc uuid)
    public static final ResourceLocation NPC_DIALOGUE_TEMPLATE = NotchCurrency.id("npc_dialogue_template");
    // Client -> server: clear this NPC's dialogue (npc uuid)
    public static final ResourceLocation NPC_DIALOGUE_CLEAR = NotchCurrency.id("npc_dialogue_clear");
    // Client -> server: request the dialogue studio for an NPC (npc uuid)
    public static final ResourceLocation NPC_STUDIO_OPEN = NotchCurrency.id("npc_studio_open");
    // Server -> client: the NPC's full dialogue tree for editing (npc uuid, tree NBT)
    public static final ResourceLocation NPC_STUDIO_DATA = NotchCurrency.id("npc_studio_data");
    // Client -> server: save an edited dialogue tree (npc uuid, tree NBT)
    public static final ResourceLocation NPC_STUDIO_SAVE = NotchCurrency.id("npc_studio_save");
    // Client -> server: set the dialogue style (npc uuid, mode ordinal: 0=window 1=chat)
    public static final ResourceLocation NPC_DIALOGUE_MODE = NotchCurrency.id("npc_dialogue_mode");
    // Client -> server: set the NPC's floating sign (npc uuid, newline-separated lines)
    public static final ResourceLocation NPC_BILLBOARD = NotchCurrency.id("npc_billboard");
    // Client -> server: faction picker (npc uuid, action: 0 list / 1 set / 2 clear, faction id)
    public static final ResourceLocation NPC_FACTION_PICK = NotchCurrency.id("npc_faction_pick");
    // Server -> client: the factions this NPC could belong to (npc uuid, current id, then each entry)
    public static final ResourceLocation NPC_FACTION_LIST = NotchCurrency.id("npc_faction_list");

    // Server -> client: open the mailbox screen (owner name, then each waiting entry)
    // Server -> client: who has a mailbox, for the send screen's list (each: uuid, name, online)
    public static final ResourceLocation MAIL_RECIPIENTS = NotchCurrency.id("mail_recipients");
    // Client -> server: switch tab, 0 inbox / 1 outbox. Two menus behind one tabbed look, because
    // slots cannot be moved between tabs without fighting every version's screen code.
    public static final ResourceLocation MAIL_TAB = NotchCurrency.id("mail_tab");
    // Client -> server: swap from reading the mail to posting a parcel
    public static final ResourceLocation MAIL_POST_OPEN = NotchCurrency.id("mail_post_open");
    // Server -> client: which recipient the post screen should open with already chosen
    public static final ResourceLocation MAIL_AIM = NotchCurrency.id("mail_aim");
    // Client -> server: post the parcel slots to this player, with an optional note
    public static final ResourceLocation MAIL_TRADE = NotchCurrency.id("mail_trade");
    public static final ResourceLocation MAIL_SEND = NotchCurrency.id("mail_send");
    // Client -> server: take one entry by its id, or every entry when the id is all zeroes
    public static final ResourceLocation MAIL_TAKE = NotchCurrency.id("mail_take");

    // Server -> client: open the recruiter screen (npc uuid, faction id/name/colour, members,
    // whether the viewer is already in it, and whether they may set this NPC's faction)
    public static final ResourceLocation NPC_RECRUITER_OPEN = NotchCurrency.id("npc_recruiter_open");
    // Client -> server: recruiter action (npc uuid, action: 0 join / 1 leave / 2 found, name, colour)
    public static final ResourceLocation NPC_RECRUITER_ACTION = NotchCurrency.id("npc_recruiter_action");

    // Client -> server: request this NPC's trigger reactions for editing (npc uuid)
    public static final ResourceLocation NPC_ACTIONS_OPEN = NotchCurrency.id("npc_actions_open");
    // Server -> client: the NPC's reactions (npc uuid, actions NBT)
    public static final ResourceLocation NPC_ACTIONS_DATA = NotchCurrency.id("npc_actions_data");
    // Client -> server: save edited reactions (npc uuid, actions NBT)
    public static final ResourceLocation NPC_ACTIONS_SAVE = NotchCurrency.id("npc_actions_save");
    // Client -> server: ask for the NPC's schedule (npc uuid)
    public static final ResourceLocation NPC_SCHEDULE_OPEN = NotchCurrency.id("npc_schedule_open");
    // Server -> client: the schedule (npc uuid, whether this dimension has a day, NBT)
    public static final ResourceLocation NPC_SCHEDULE_DATA = NotchCurrency.id("npc_schedule_data");
    // Client -> server: an edited schedule (npc uuid, NBT)
    public static final ResourceLocation NPC_SCHEDULE_SAVE = NotchCurrency.id("npc_schedule_save");
    // Client -> server: hand over the spot-marking tool bound to one schedule entry (npc uuid, entry index)
    public static final ResourceLocation NPC_SCHEDULE_TOOL = NotchCurrency.id("npc_schedule_tool");
    // Client -> server: personality (npc uuid, subtitle, voice sound id, pitch percent)
    public static final ResourceLocation NPC_SET_FLAVOR = NotchCurrency.id("npc_set_flavor");

    // Server -> client: waystone teleport fees, so the selection menu can price each destination
    // (enabled, fee, dimensionalFee; sent on join by WaystoneFeeHandler when Waystones is present)
    public static final ResourceLocation WAYSTONE_FEE_SYNC = NotchCurrency.id("waystone_fee_sync");

    // ATM withdraw (client -> server)
    public static final ResourceLocation ATM_WITHDRAW    = NotchCurrency.id("atm_withdraw");

    // Shop packets (client -> server); listing edits go through SHOP_MANAGE_ACTION / SHOP_EDIT_ACTION
    public static final ResourceLocation SHOP_PURCHASE   = NotchCurrency.id("shop_purchase");
    public static final ResourceLocation SHOP_WITHDRAW = NotchCurrency.id("shop_withdraw");

    private NotchPackets() {}

    // ---- Server -> Client helpers ----
    public static void sendBalance(ServerPlayer sp, long value) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeVarLong(value);
        Net.sendToClient(sp, BALANCE_SYNC, buf);
    }
}