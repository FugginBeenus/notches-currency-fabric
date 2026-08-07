package net.fugginbeenus.notchcurrency.economy.raffle;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.economy.raffle.RaffleState.Result;
import net.fugginbeenus.notchcurrency.item.RaffleTicketItem;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

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

    public static void buyTicket(ServerPlayer player, int qty) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        if (!enabled) {
            player.displayClientMessage(Component.literal("The raffle isn't running right now.").withStyle(ChatFormatting.RED), false);
            return;
        }
        if (qty <= 0) return;

        refreshTickets(player);
        RaffleState state = RaffleState.get(server);

        if (maxTicketsPerPlayer > 0) {
            int have = state.getTickets(player.getUUID());
            if (have + qty > maxTicketsPerPlayer) {
                player.displayClientMessage(Component.literal("You can hold at most " + maxTicketsPerPlayer
                        + " entries this round (you have " + have + ").").withStyle(ChatFormatting.RED), false);
                return;
            }
        }

        long cost = ticketPrice * qty;
        if (CurrencyApi.getBalance(player) < cost) {
            player.displayClientMessage(Component.literal("You can't afford " + qty + " entr" + (qty == 1 ? "y" : "ies") + " (")
                    .withStyle(ChatFormatting.RED)
                    .append(NotchCurrency.coins(cost))
                    .append(Component.literal(").").withStyle(ChatFormatting.RED)), false);
            return;
        }

        long cut = cost * houseCutPercent / 100;
        long potShare = cost - cut;
        if (cut > 0) CurrencyApi.withdraw(player, cut, TransactionReason.SINK, "raffle house cut");
        if (potShare > 0) CurrencyApi.withdraw(player, potShare, TransactionReason.RAFFLE, "raffle ticket x" + qty);

        state.recordPurchase(player.getUUID(), player.getName().getString(), qty, potShare);
        issueOrUpdateTicket(player, state);

        int have = state.getTickets(player.getUUID());
        player.displayClientMessage(Component.literal("Bought ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(qty + " entr" + (qty == 1 ? "y" : "ies")).withStyle(ChatFormatting.WHITE))
                .append(Component.literal(". You hold " + have + " (" + oddsString(state, have) + " to win). Pot: ").withStyle(ChatFormatting.GREEN))
                .append(NotchCurrency.coins(state.getPot()))
                .append(Component.literal(".").withStyle(ChatFormatting.GREEN)), false);

        if (announce) {
            server.getPlayerList().broadcastSystemMessage(Component.literal(player.getName().getString()).withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal(" entered the raffle - pot is now ").withStyle(ChatFormatting.GRAY))
                    .append(NotchCurrency.coins(state.getPot()))
                    .append(Component.literal("!").withStyle(ChatFormatting.GRAY)), false);
        }
    }

    public static void redeemTicket(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        if (!enabled || !redeemEnabled) {
            player.displayClientMessage(Component.literal("There's no raffle to redeem into right now.").withStyle(ChatFormatting.RED), false);
            return;
        }
        refreshTickets(player);
        RaffleState state = RaffleState.get(server);

        if (state.hasRedeemed(player.getUUID())) {
            player.displayClientMessage(Component.literal("You've already redeemed an old ticket this raffle.").withStyle(ChatFormatting.RED), false);
            return;
        }

        // Find one expired losing ticket.
        Inventory inv = player.getInventory();
        int loserSlot = -1;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (RaffleTicketItem.isTicket(st) && RaffleTicketItem.STATUS_LOSER.equals(RaffleTicketItem.status(st))) {
                loserSlot = i;
                break;
            }
        }
        if (loserSlot < 0) {
            player.displayClientMessage(Component.literal("You have no old losing tickets to redeem.").withStyle(ChatFormatting.GRAY), false);
            return;
        }

        int oldEntries = RaffleTicketItem.entries(inv.getItem(loserSlot));
        int free = redeemEntriesFor(oldEntries);

        inv.setItem(loserSlot, ItemStack.EMPTY);     // consume the old ticket
        state.markRedeemed(player.getUUID());          // one redemption per round
        state.recordPurchase(player.getUUID(), player.getName().getString(), free, 0L); // free entries, no pot
        issueOrUpdateTicket(player, state);

        int have = state.getTickets(player.getUUID());
        player.displayClientMessage(Component.literal("Redeemed an old ticket (" + oldEntries + " entr" + (oldEntries == 1 ? "y" : "ies")
                + ") for ").withStyle(ChatFormatting.GREEN)
                .append(Component.literal(free + " free entr" + (free == 1 ? "y" : "ies")).withStyle(ChatFormatting.WHITE))
                .append(Component.literal("! You now hold " + have + " (" + oddsString(state, have) + " to win).").withStyle(ChatFormatting.GREEN)), false);
    }

    private static int redeemEntriesFor(int oldEntries) {
        if (oldEntries < 5) return 1;
        if (oldEntries < 10) return 5;
        return 10;
    }

    private static void issueOrUpdateTicket(ServerPlayer player, RaffleState state) {
        int have = state.getTickets(player.getUUID());
        long roundNow = state.getCurrentRound();
        ItemStack existing = findActiveTicket(player, roundNow);
        if (existing != null) {
            RaffleTicketItem.setEntries(existing, have);
        } else {
            player.getInventory().placeItemBackInInventory(RaffleTicketItem.create(roundNow, have,
                    player.getUUID(), player.getName().getString()));
        }
    }

    // ---- drawing ----

    public static boolean draw(MinecraftServer server, boolean broadcast) {
        RaffleState state = RaffleState.get(server);
        UUID winnerId = state.drawWinner(RANDOM);
        if (winnerId == null) {
            if (broadcast && announce) {
                server.getPlayerList().broadcastSystemMessage(Component.literal("The raffle round ended with no entries.")
                        .withStyle(ChatFormatting.GRAY), false);
            }
            return false;
        }

        long prize = state.getPot() + state.getCoinsPool();
        ItemStack prizeItem = state.getPrizeItem().copy();
        String winnerName = state.getName(winnerId);
        long round = state.recordResult(winnerId, winnerName, prize);
        refreshAllOnline(server); // everyone's current-round tickets become losers / the winner's a winner

        ServerPlayer online = server.getPlayerList().getPlayer(winnerId);
        if (online != null) {
            online.displayClientMessage(Component.literal("🎉 You won Raffle #" + round + "! ").withStyle(ChatFormatting.GOLD)
                    .append(prizeDescription(prize, prizeItem))
                    .append(Component.literal(" is waiting - claim it at the raffle or with /raffle claim.").withStyle(ChatFormatting.GOLD)), false);
        }

        if (broadcast && announce) {
            server.getPlayerList().broadcastSystemMessage(Component.literal("🎉 Raffle #" + round + " drawn! ").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)
                    .append(Component.literal(winnerName).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(" won ").withStyle(ChatFormatting.WHITE))
                    .append(prizeDescription(prize, prizeItem))
                    .append(Component.literal(" - they must claim it at the raffle. A new round starts now.").withStyle(ChatFormatting.WHITE)), false);
        }
        return true;
    }

    // ---- claiming ----

    public static void claim(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        refreshTickets(player);

        RaffleState state = RaffleState.get(server);
        List<Result> wins = state.claimWins(player.getUUID());
        if (wins.isEmpty()) {
            player.displayClientMessage(Component.literal("You have no raffle prizes to claim.").withStyle(ChatFormatting.GRAY), false);
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
                player.getInventory().placeItemBackInInventory(give);
            }
        }
        consumeTicketsForRounds(player, rounds);
        if (total > 0) CurrencyApi.deposit(player, total, TransactionReason.RAFFLE, "raffle winnings (claim)");

        MutableComponent msg = Component.literal("🎉 Claimed ").withStyle(ChatFormatting.GOLD);
        if (total > 0) msg.append(NotchCurrency.coins(total));
        boolean anyItem = wins.stream().anyMatch(r -> !r.prizeItem.isEmpty());
        if (anyItem) msg.append(Component.literal(total > 0 ? " plus your prize item" : "your prize item").withStyle(ChatFormatting.GOLD));
        msg.append(Component.literal(wins.size() == 1 ? " from your winning ticket!" : " from " + wins.size() + " winning tickets!").withStyle(ChatFormatting.GOLD));
        player.displayClientMessage(msg, false);
    }

    // ---- admin: prize item + opening the screen ----

    public static void setPrize(ServerPlayer admin) {
        MinecraftServer server = admin.getServer();
        if (server == null) return;
        RaffleState state = RaffleState.get(server);
        ItemStack previous = state.getPrizeItem().copy();
        ItemStack held = admin.getMainHandItem();

        if (held.isEmpty()) {
            state.setPrizeItem(ItemStack.EMPTY);
            if (!previous.isEmpty()) admin.getInventory().placeItemBackInInventory(previous);
            admin.displayClientMessage(Component.literal(previous.isEmpty() ? "No prize was set."
                    : "Prize cleared and returned to you.").withStyle(ChatFormatting.YELLOW), false);
            return;
        }

        ItemStack prize = held.copy();
        held.shrink(held.getCount()); // escrow the held stack
        state.setPrizeItem(prize);
        if (!previous.isEmpty()) admin.getInventory().placeItemBackInInventory(previous);
        admin.displayClientMessage(Component.literal("Raffle prize set to ").withStyle(ChatFormatting.GREEN)
                .append(prize.getHoverName().copy().withStyle(ChatFormatting.WHITE))
                .append(Component.literal(prize.getCount() > 1 ? " x" + prize.getCount() : "").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(previous.isEmpty() ? "." : " (previous prize returned).").withStyle(ChatFormatting.GREEN)), false);
    }

    public static void clearPrize(ServerPlayer admin) {
        MinecraftServer server = admin.getServer();
        if (server == null) return;
        RaffleState state = RaffleState.get(server);
        ItemStack previous = state.getPrizeItem().copy();
        state.setPrizeItem(ItemStack.EMPTY);
        if (!previous.isEmpty()) {
            admin.getInventory().placeItemBackInInventory(previous);
            admin.displayClientMessage(Component.literal("Prize cleared and returned to you.").withStyle(ChatFormatting.YELLOW), false);
        } else {
            admin.displayClientMessage(Component.literal("No prize set.").withStyle(ChatFormatting.YELLOW), false);
        }
    }

    public static void resetAndReturn(ServerPlayer admin) {
        MinecraftServer server = admin.getServer();
        if (server == null) return;
        RaffleState state = RaffleState.get(server);
        ItemStack prize = state.getPrizeItem().copy();
        state.resetRound();
        if (!prize.isEmpty()) admin.getInventory().placeItemBackInInventory(prize);
        refreshAllOnline(server);
    }

    public static void openScreen(ServerPlayer sp) {
        refreshTickets(sp);
        sp.openMenu(new SimpleMenuProvider(
                (containerId, inv, p) -> new net.fugginbeenus.notchcurrency.economy.raffle.RaffleScreenHandler(containerId, inv),
                Component.literal("Raffle")));
    }

    // ---- ticket restamping ----

    public static void refreshTickets(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        RaffleState state = RaffleState.get(server);
        long current = state.getCurrentRound();
        UUID me = player.getUUID();

        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
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

    public static void refreshAllOnline(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            refreshTickets(p);
        }
    }

    public static void remindOnJoin(ServerPlayer player) {
        refreshTickets(player);
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        List<Result> wins = RaffleState.get(server).getUnclaimedWins(player.getUUID());
        if (wins.isEmpty()) return;
        long total = 0L;
        for (Result r : wins) total += r.prize;
        player.displayClientMessage(Component.literal("🎟 You have an unclaimed raffle prize of ").withStyle(ChatFormatting.GOLD)
                .append(NotchCurrency.coins(total))
                .append(Component.literal("! Use /raffle claim.").withStyle(ChatFormatting.GOLD)), false);
    }

    // ---- inventory helpers ----

    private static ItemStack findActiveTicket(ServerPlayer player, long round) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (RaffleTicketItem.isTicket(st) && RaffleTicketItem.round(st) == round) {
                UUID o = RaffleTicketItem.owner(st);
                if (o != null && o.equals(player.getUUID())) return st;
            }
        }
        return null;
    }

    private static void consumeTicketsForRounds(ServerPlayer player, Set<Long> rounds) {
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (RaffleTicketItem.isTicket(st) && rounds.contains(RaffleTicketItem.round(st))) {
                inv.setItem(i, ItemStack.EMPTY);
            }
        }
    }

    public static int countLoserEntries(ServerPlayer player) {
        Inventory inv = player.getInventory();
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack st = inv.getItem(i);
            if (RaffleTicketItem.isTicket(st)
                    && RaffleTicketItem.STATUS_LOSER.equals(RaffleTicketItem.status(st))) {
                n += RaffleTicketItem.entries(st);
            }
        }
        return n;
    }

    public static long unclaimedPrizeTotal(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return 0L;
        long total = 0L;
        for (Result r : RaffleState.get(server).getUnclaimedWins(player.getUUID())) total += r.prize;
        return total;
    }

    public static boolean hasUnclaimedWin(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        return server != null && !RaffleState.get(server).getUnclaimedWins(player.getUUID()).isEmpty();
    }

    private static Component prizeDescription(long coins, ItemStack item) {
        boolean hasItem = !item.isEmpty();
        if (hasItem && coins > 0) {
            return Component.empty().append(item.getHoverName().copy().withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(item.getCount() > 1 ? " x" + item.getCount() : "").withStyle(ChatFormatting.GRAY))
                    .append(Component.literal(" + ").withStyle(ChatFormatting.WHITE))
                    .append(NotchCurrency.coins(coins));
        }
        if (hasItem) {
            return Component.empty().append(item.getHoverName().copy().withStyle(ChatFormatting.AQUA))
                    .append(Component.literal(item.getCount() > 1 ? " x" + item.getCount() : "").withStyle(ChatFormatting.GRAY));
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

    public static boolean canRedeem(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (!enabled || !redeemEnabled || server == null) return false;
        return !RaffleState.get(server).hasRedeemed(player.getUUID());
    }
}
