package net.fugginbeenus.notchcurrency.economy.npc;

/**
 * A behaviour an NPC can be bound to. Any entity (APP.ly, EasyNPC, even a villager) can
 * be assigned a role by an admin; interacting with it then opens the matching economy
 * feature. All roles are admin-assigned only.
 *
 * {@code ADMIN_SHOP} additionally references a specific admin shop (the assignment's shopId).
 */
public enum NpcRole {
    /** No economy function — interaction plays the NPC's dialogue (if any), nothing after. */
    NONE,
    /** A player-owned shop NPC (links a {@code PlayerShop} keyed by the NPC's UUID). */
    SHOP,
    /** Legacy — hidden from the editor's role picker. NONE + dialogue covers this now (any NPC can
     *  talk); existing Greeters keep working: dialogue plays, no screen ever opens. */
    GREETER,
    /** Opens the Enchanter: buy enchant levels, repair, extract to book, uncraft. */
    ENCHANTER,
    /** Opens an admin server-shop (needs a shopId on the assignment). */
    ADMIN_SHOP,
    /** Opens the Bank/ATM screen. */
    BANKER,
    /** Opens the Auction House. */
    AUCTIONEER,
    /** Claims the player's pending auction winnings (mailbox). */
    MAILBOX,
    /** Opens the raffle: shows the pot and lets the player buy tickets. */
    RAFFLE,
    /** Opens the bounty board: kill/fetch tasks players complete for coin rewards. */
    BOUNTY,
    /** Opens the slot machine (casino dealer). */
    DEALER,
    /** Opens the cosmetics shop (buy cosmetics from any mod for coins). */
    COSMETICS,
    /** Recruits for a faction: players join or leave through it, and its owner can found one here.
     *  The NPC only points at the faction — the faction itself lives in the world save. */
    RECRUITER,
    /** An API-registered role from another mod ({@code NotchNpcApi.registerCustomRole});
     *  the NPC stores the handler id. Not selectable in the editor. */
    CUSTOM
}
