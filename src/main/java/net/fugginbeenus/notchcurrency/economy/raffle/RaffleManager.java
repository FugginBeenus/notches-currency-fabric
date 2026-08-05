package net.fugginbeenus.notchcurrency.economy.raffle;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.economy.raffle.RaffleState.Result;
import net.fugginbeenus.notchcurrency.item.RaffleTicketItem;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

/**
 * Drives the server raffle: selling physical tickets, taking the house cut as a {@link
 * TransactionReason#SINK}, accumulating the pot, drawing a weighted winner, and holding the
 * prize until the winner claims it at the raffle. Tickets are personal receipts: the buyer
 * wins by identity, so a lost ticket never forfeits a prize, but turning the ticket in (or
 * running {@code /raffle claim}) is how you collect.
 *
 * Because items already in inventories can't be mutated at draw time, ticket statuses
 * (active / winning / expired-loser) are restamped lazily via {@link #refreshTickets} on any
 * contact with the raffle. Expired losing tickets can be redeemed for a discount on new entries.
 */
public final class RaffleManager {

    private static final Random RANDOM = new Random();

    private static boolean enabled = false;
    private static long ticketPrice = 100L;
    private static int houseCutPercent = 20;
    private static int maxTicketsPerPlayer = 0;
    private static long drawIntervalTicks = 1440L * 60L * 20L; // 0 = manual only
    private static boolean announce = true;
    private static boolean redeemEnabled = true;

    private static long tickAccum = 0;

    private RaffleManager() {}

    public static void init() {
        ServerTickEvents.END_SERVER_TICK.register(RaffleManager::tick);
    }

    public static void applyConfig(NotchConfig cfg) {
        NotchConfig.Raffle r = cfg.raffle;
        enabled = r.enabled;
        ticketPrice = Math.max(1L, r.ticketPrice);
        houseCutPercent = Math.max(0, Math.min(100, r.houseCutPercent));
        maxTicketsPerPlayer = Math.max(0, r.maxTicketsPerPlayer);
        drawIntervalTicks = Math.max(0, r.drawIntervalMinutes) * 60L * 20L;
        announce = r.announce;
        redeemEnabled = r.redeemEnabled;
    }

    private static void tick(MinecraftServer server) {
        if (!enabled || drawIntervalTicks <= 0) return;
        if (++tickAccum < drawIntervalTicks) return;
        tickAccum = 0;
        draw(server, true);
    }

    // ---- ticket sales ----

    /** Buy {@code qty} entries for {@code player}. */
    public static void buyTicket(ServerPlayerEntity player, int qty) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        if (!enabled) {
            player.sendMessage(Text.literal("The raffle isn't running right now.").formatted(Formatting.RED), false);
            return;
        }
        if (qty <= 0) return;

        refreshTickets(player);
        RaffleState state = RaffleState.get(server);

        if (maxTicketsPerPlayer > 0) {
            int have = state.getTickets(player.getUuid());
            if (have + qty > maxTicketsPerPlayer) {
                player.sendMessage(Text.literal("You can hold at most " + maxTicketsPerPlayer
                        + " entries this round (you have " + have + ").").formatted(Formatting.RED), false);
                return;
            }
        }

        long cost = ticketPrice * qty;
        if (CurrencyApi.getBalance(player) < cost) {
            player.sendMessage(Text.literal("You can't afford " + qty + " entr" + (qty == 1 ? "y" : "ies") + " (")
                    .formatted(Formatting.RED)
                    .append(NotchCurrency.coins(cost))
                    .append(Text.literal(").").formatted(Formatting.RED)), false);
            return;
        }

        long cut = cost * houseCutPercent / 100;
        long potShare = cost - cut;
        if (cut > 0) CurrencyApi.withdraw(player, cut, TransactionReason.SINK, "raffle house cut");
        if (potShare > 0) CurrencyApi.withdraw(player, potShare, TransactionReason.RAFFLE, "raffle ticket x" + qty);

        state.recordPurchase(player.getUuid(), player.getName().getString(), qty, potShare);
        issueOrUpdateTicket(player, state);

        int have = state.getTickets(player.getUuid());
        player.sendMessage(Text.literal("Bought ").formatted(Formatting.GREEN)
                .append(Text.literal(qty + " entr" + (qty == 1 ? "y" : "ies")).formatted(Formatting.WHITE))
                .append(Text.literal(". You hold " + have + " (" + oddsString(state, have) + " to win). Pot: ").formatted(Formatting.GREEN))
                .append(NotchCurrency.coins(state.getPot()))
                .append(Text.literal(".").formatted(Formatting.GREEN)), false);

        if (announce) {
            server.getPlayerManager().broadcast(Text.literal(player.getName().getString()).formatted(Formatting.YELLOW)
                    .append(Text.literal(" entered the raffle - pot is now ").formatted(Formatting.GRAY))
                    .append(NotchCurrency.coins(state.getPot()))
                    .append(Text.literal("!").formatted(Formatting.GRAY)), false);
        }
    }

    /** Turn in one old losing ticket for a few free entries into the current round. */
    public static void redeemTicket(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        if (!enabled || !redeemEnabled) {
            player.sendMessage(Text.literal("There's no raffle to redeem into right now.").formatted(Formatting.RED), false);
            return;
        }
        refreshTickets(player);
        RaffleState state = RaffleState.get(server);

        if (state.hasRedeemed(player.getUuid())) {
            player.sendMessage(Text.literal("You've already redeemed an old ticket this raffle.").formatted(Formatting.RED), false);
            return;
        }

        // Find one expired losing ticket.
        PlayerInventory inv = player.getInventory();
        int loserSlot = -1;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack st = inv.getStack(i);
            if (RaffleTicketItem.isTicket(st) && RaffleTicketItem.STATUS_LOSER.equals(RaffleTicketItem.status(st))) {
                loserSlot = i;
                break;
            }
        }
        if (loserSlot < 0) {
            player.sendMessage(Text.literal("You have no old losing tickets to redeem.").formatted(Formatting.GRAY), false);
            return;
        }

        int oldEntries = RaffleTicketItem.entries(inv.getStack(loserSlot));
        int free = redeemEntriesFor(oldEntries);

        inv.setStack(loserSlot, ItemStack.EMPTY);     // consume the old ticket
        state.markRedeemed(player.getUuid());          // one redemption per round
        state.recordPurchase(player.getUuid(), player.getName().getString(), free, 0L); // free entries, no pot
        issueOrUpdateTicket(player, state);

        int have = state.getTickets(player.getUuid());
        player.sendMessage(Text.literal("Redeemed an old ticket (" + oldEntries + " entr" + (oldEntries == 1 ? "y" : "ies")
                + ") for ").formatted(Formatting.GREEN)
                .append(Text.literal(free + " free entr" + (free == 1 ? "y" : "ies")).formatted(Formatting.WHITE))
                .append(Text.literal("! You now hold " + have + " (" + oddsString(state, have) + " to win).").formatted(Formatting.GREEN)), false);
    }

    /** &lt;5 entries → 1, &lt;10 → 5, else → 10 (a consolation that's always a net loss). */
    private static int redeemEntriesFor(int oldEntries) {
        if (oldEntries < 5) return 1;
        if (oldEntries < 10) return 5;
        return 10;
    }

    /** One ticket per round: update the player's existing ticket to their new total, or issue one. */
    private static void issueOrUpdateTicket(ServerPlayerEntity player, RaffleState state) {
        int have = state.getTickets(player.getUuid());
        long roundNow = state.getCurrentRound();
        ItemStack existing = findActiveTicket(player, roundNow);
        if (existing != null) {
            RaffleTicketItem.setEntries(existing, have);
        } else {
            player.getInventory().offerOrDrop(RaffleTicketItem.create(roundNow, have,
                    player.getUuid(), player.getName().getString()));
        }
    }

    // ---- drawing ----

    /** Draw a winner and file the pot as an unclaimed prize. Returns false if nobody entered. */
    public static boolean draw(MinecraftServer server, boolean broadcast) {
        RaffleState state = RaffleState.get(server);
        UUID winnerId = state.drawWinner(RANDOM);
        if (winnerId == null) {
            if (broadcast && announce) {
                server.getPlayerManager().broadcast(Text.literal("The raffle round ended with no entries.")
                        .formatted(Formatting.GRAY), false);
            }
            return false;
        }

        long prize = state.getPot() + state.getCoinsPool();
        ItemStack prizeItem = state.getPrizeItem().copy();
        String winnerName = state.getName(winnerId);
        long round = state.recordResult(winnerId, winnerName, prize);
        refreshAllOnline(server); // everyone's current-round tickets become losers / the winner's a winner

        ServerPlayerEntity online = server.getPlayerManager().getPlayer(winnerId);
        if (online != null) {
            online.sendMessage(Text.literal("🎉 You won Raffle #" + round + "! ").formatted(Formatting.GOLD)
                    .append(prizeDescription(prize, prizeItem))
                    .append(Text.literal(" is waiting - claim it at the raffle or with /raffle claim.").formatted(Formatting.GOLD)), false);
        }

        if (broadcast && announce) {
            server.getPlayerManager().broadcast(Text.literal("🎉 Raffle #" + round + " drawn! ").formatted(Formatting.GOLD, Formatting.BOLD)
                    .append(Text.literal(winnerName).formatted(Formatting.YELLOW))
                    .append(Text.literal(" won ").formatted(Formatting.WHITE))
                    .append(prizeDescription(prize, prizeItem))
                    .append(Text.literal(" - they must claim it at the raffle. A new round starts now.").formatted(Formatting.WHITE)), false);
        }
        return true;
    }

    // ---- claiming ----

    /** Pay out every unclaimed prize this player has won and consume the matching tickets. */
    public static void claim(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        refreshTickets(player);

        RaffleState state = RaffleState.get(server);
        List<Result> wins = state.claimWins(player.getUuid());
        if (wins.isEmpty()) {
            player.sendMessage(Text.literal("You have no raffle prizes to claim.").formatted(Formatting.GRAY), false);
            return;
        }

        long total = 0L;
        Set<Long> rounds = new HashSet<>();
        for (Result r : wins) {
            total += r.prize;
            rounds.add(r.round);
            // Hand over any item prize attached to this win.
            if (!r.prizeItem.isEmpty()) {
                ItemStack give = r.prizeItem.copy();
                player.getInventory().offerOrDrop(give);
            }
        }
        consumeTicketsForRounds(player, rounds);
        if (total > 0) CurrencyApi.deposit(player, total, TransactionReason.RAFFLE, "raffle winnings (claim)");

        MutableText msg = Text.literal("🎉 Claimed ").formatted(Formatting.GOLD);
        if (total > 0) msg.append(NotchCurrency.coins(total));
        boolean anyItem = wins.stream().anyMatch(r -> !r.prizeItem.isEmpty());
        if (anyItem) msg.append(Text.literal(total > 0 ? " plus your prize item" : "your prize item").formatted(Formatting.GOLD));
        msg.append(Text.literal(wins.size() == 1 ? " from your winning ticket!" : " from " + wins.size() + " winning tickets!").formatted(Formatting.GOLD));
        player.sendMessage(msg, false);
    }

    // ---- admin: prize item + opening the screen ----

    /**
     * Set the prize item from the admin's held item (empty hand clears it). The item is
     * <em>escrowed</em> (taken from the admin and awarded to the winner), so any previously
     * set prize is returned, and clearing/cancelling hands it back.
     */
    public static void setPrize(ServerPlayerEntity admin) {
        MinecraftServer server = admin.getServer();
        if (server == null) return;
        RaffleState state = RaffleState.get(server);
        ItemStack previous = state.getPrizeItem().copy();
        ItemStack held = admin.getMainHandStack();

        if (held.isEmpty()) {
            state.setPrizeItem(ItemStack.EMPTY);
            if (!previous.isEmpty()) admin.getInventory().offerOrDrop(previous);
            admin.sendMessage(Text.literal(previous.isEmpty() ? "No prize was set."
                    : "Prize cleared and returned to you.").formatted(Formatting.YELLOW), false);
            return;
        }

        ItemStack prize = held.copy();
        held.decrement(held.getCount()); // escrow the held stack
        state.setPrizeItem(prize);
        if (!previous.isEmpty()) admin.getInventory().offerOrDrop(previous);
        admin.sendMessage(Text.literal("Raffle prize set to ").formatted(Formatting.GREEN)
                .append(prize.getName().copy().formatted(Formatting.WHITE))
                .append(Text.literal(prize.getCount() > 1 ? " x" + prize.getCount() : "").formatted(Formatting.GRAY))
                .append(Text.literal(previous.isEmpty() ? "." : " (previous prize returned).").formatted(Formatting.GREEN)), false);
    }

    public static void clearPrize(ServerPlayerEntity admin) {
        MinecraftServer server = admin.getServer();
        if (server == null) return;
        RaffleState state = RaffleState.get(server);
        ItemStack previous = state.getPrizeItem().copy();
        state.setPrizeItem(ItemStack.EMPTY);
        if (!previous.isEmpty()) {
            admin.getInventory().offerOrDrop(previous);
            admin.sendMessage(Text.literal("Prize cleared and returned to you.").formatted(Formatting.YELLOW), false);
        } else {
            admin.sendMessage(Text.literal("No prize set.").formatted(Formatting.YELLOW), false);
        }
    }

    /** Wipe the round and hand any escrowed prize item back to the admin (cancel/reset). */
    public static void resetAndReturn(ServerPlayerEntity admin) {
        MinecraftServer server = admin.getServer();
        if (server == null) return;
        RaffleState state = RaffleState.get(server);
        ItemStack prize = state.getPrizeItem().copy();
        state.resetRound();
        if (!prize.isEmpty()) admin.getInventory().offerOrDrop(prize);
        refreshAllOnline(server);
    }

    /** Open the code-drawn raffle screen for the player. */
    public static void openScreen(ServerPlayerEntity sp) {
        refreshTickets(sp);
        sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new net.fugginbeenus.notchcurrency.economy.raffle.RaffleScreenHandler(syncId, inv),
                Text.literal("Raffle")));
    }

    // ---- ticket restamping ----

    /** Resolve the status of every raffle ticket in the player's inventory against current state. */
    public static void refreshTickets(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return;
        RaffleState state = RaffleState.get(server);
        long current = state.getCurrentRound();
        UUID me = player.getUuid();

        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack st = inv.getStack(i);
            if (!RaffleTicketItem.isTicket(st)) continue;

            long r = RaffleTicketItem.round(st);
            UUID o = RaffleTicketItem.owner(st);

            // A win from a finished round is always claimable, even after the raffle closes.
            Result res = state.getResult(r);
            if (res != null && o != null && res.winner.equals(o)) {
                RaffleTicketItem.setStatus(st, RaffleTicketItem.STATUS_WINNER, res.prize);
                continue;
            }

            // A ticket is only ACTIVE if it's for the current round AND the raffle is running.
            // Anything else (a past round, or any ticket while the raffle is off) is a dead loser.
            if (enabled && r == current) {
                RaffleTicketItem.setStatus(st, RaffleTicketItem.STATUS_ACTIVE, 0L);
                if (o != null && o.equals(me)) {
                    RaffleTicketItem.setEntries(st, state.getTickets(me)); // keep the count truthful
                }
            } else {
                RaffleTicketItem.setStatus(st, RaffleTicketItem.STATUS_LOSER, 0L);
            }
        }
    }

    /** Restamp every online player's tickets (after a draw/reset/disable so they update at once). */
    public static void refreshAllOnline(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            refreshTickets(p);
        }
    }

    /** Refresh tickets on login and nudge the player if they have a prize waiting. */
    public static void remindOnJoin(ServerPlayerEntity player) {
        refreshTickets(player);
        MinecraftServer server = player.getServer();
        if (server == null) return;
        List<Result> wins = RaffleState.get(server).getUnclaimedWins(player.getUuid());
        if (wins.isEmpty()) return;
        long total = 0L;
        for (Result r : wins) total += r.prize;
        player.sendMessage(Text.literal("🎟 You have an unclaimed raffle prize of ").formatted(Formatting.GOLD)
                .append(NotchCurrency.coins(total))
                .append(Text.literal("! Use /raffle claim.").formatted(Formatting.GOLD)), false);
    }

    // ---- inventory helpers ----

    /** The player's own ticket for the given round, or null. */
    private static ItemStack findActiveTicket(ServerPlayerEntity player, long round) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack st = inv.getStack(i);
            if (RaffleTicketItem.isTicket(st) && RaffleTicketItem.round(st) == round) {
                UUID o = RaffleTicketItem.owner(st);
                if (o != null && o.equals(player.getUuid())) return st;
            }
        }
        return null;
    }

    private static void consumeTicketsForRounds(ServerPlayerEntity player, Set<Long> rounds) {
        PlayerInventory inv = player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack st = inv.getStack(i);
            if (RaffleTicketItem.isTicket(st) && rounds.contains(RaffleTicketItem.round(st))) {
                inv.setStack(i, ItemStack.EMPTY);
            }
        }
    }

    /** Public for the raffle screen handler: count expired losing-ticket entries held. */
    public static int countLoserEntries(ServerPlayerEntity player) {
        PlayerInventory inv = player.getInventory();
        int n = 0;
        for (int i = 0; i < inv.size(); i++) {
            ItemStack st = inv.getStack(i);
            if (RaffleTicketItem.isTicket(st)
                    && RaffleTicketItem.STATUS_LOSER.equals(RaffleTicketItem.status(st))) {
                n += RaffleTicketItem.entries(st);
            }
        }
        return n;
    }

    /** Total unclaimed coin prize this player is owed (for the screen's claim banner). */
    public static long unclaimedPrizeTotal(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (server == null) return 0L;
        long total = 0L;
        for (Result r : RaffleState.get(server).getUnclaimedWins(player.getUuid())) total += r.prize;
        return total;
    }

    /** Whether this player has any unclaimed win (coins or item). */
    public static boolean hasUnclaimedWin(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        return server != null && !RaffleState.get(server).getUnclaimedWins(player.getUuid()).isEmpty();
    }

    /** "<n> coins", "<item> xN", or "<item> + <n> coins" depending on what the round awards. */
    private static Text prizeDescription(long coins, ItemStack item) {
        boolean hasItem = !item.isEmpty();
        if (hasItem && coins > 0) {
            return Text.empty().append(item.getName().copy().formatted(Formatting.AQUA))
                    .append(Text.literal(item.getCount() > 1 ? " x" + item.getCount() : "").formatted(Formatting.GRAY))
                    .append(Text.literal(" + ").formatted(Formatting.WHITE))
                    .append(NotchCurrency.coins(coins));
        }
        if (hasItem) {
            return Text.empty().append(item.getName().copy().formatted(Formatting.AQUA))
                    .append(Text.literal(item.getCount() > 1 ? " x" + item.getCount() : "").formatted(Formatting.GRAY));
        }
        return NotchCurrency.coins(coins);
    }

    private static String oddsString(RaffleState state, int have) {
        int total = state.getTotalTickets();
        if (total <= 0 || have <= 0) return "0%";
        return String.format("%.1f%%", 100.0 * have / total);
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static long getTicketPrice() {
        return ticketPrice;
    }

    public static int getHouseCutPercent() {
        return houseCutPercent;
    }

    /** Whether this player can still redeem an old ticket this round (for the screen's button). */
    public static boolean canRedeem(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        if (!enabled || !redeemEnabled || server == null) return false;
        return !RaffleState.get(server).hasRedeemed(player.getUuid());
    }
}
