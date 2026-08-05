package net.fugginbeenus.notchcurrency.trade;

import net.fugginbeenus.notchcurrency.compat.StackData;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.UUID;

/**
 * Runs the offline trade-offer flow: create (escrow items), accept (swap coins/items, offline-safe
 * via the mailbox), cancel (return the escrow), and mailbox delivery on login. Coin transfers are
 * tagged TRADE (a transfer, not a sink/faucet) so the economy totals stay honest.
 */
public final class TradeOfferManager {

    public static final int MAX_PER_PLAYER = 10;

    private TradeOfferManager() {}

    /** Deliver any items owed to the player from earlier offline resolutions. Call on JOIN. */
    public static void deliverMail(ServerPlayerEntity sp) {
        TradeOfferState state = TradeOfferState.get(sp.getServer());
        if (!state.hasMail(sp.getUuid())) return;
        List<ItemStack> items = state.claimMail(sp.getUuid());
        java.util.List<ItemStack> leftover = new java.util.ArrayList<>();
        for (ItemStack st : items) {
            if (!sp.getInventory().insertStack(st)) leftover.add(st);
        }
        if (!leftover.isEmpty()) {
            state.returnMail(sp.getUuid(), leftover); // no room; keep for next time
            sp.sendMessage(Text.literal("You have trade items waiting. Free up inventory space to receive them.")
                    .formatted(Formatting.YELLOW), false);
        }
        if (items.size() != leftover.size()) {
            sp.sendMessage(Text.literal("Received items from a completed trade offer.").formatted(Formatting.GREEN), false);
        }
    }

    /** Create an offer. {@code offered} stacks must already be removed from the creator (escrowed by
     *  the GUI); {@code offeredCoins} are charged here (escrowed into the offer). */
    public static boolean createOffer(ServerPlayerEntity creator, List<ItemStack> offered, long offeredCoins,
                                      long price, List<ItemStack> requested, String targetName) {
        TradeOfferState state = TradeOfferState.get(creator.getServer());
        if (state.countBy(creator.getUuid()) >= MAX_PER_PLAYER) {
            creator.sendMessage(Text.literal("You already have " + MAX_PER_PLAYER + " open offers.").formatted(Formatting.RED), false);
            return false;
        }
        if (offeredCoins > 0) {
            if (BalanceStore.get(creator) < offeredCoins) {
                creator.sendMessage(Text.literal("You don't have " + offeredCoins + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " to attach.").formatted(Formatting.RED), false);
                return false;
            }
            BalanceStore.subtract(creator, offeredCoins, TransactionReason.TRADE, "trade offer escrow");
            NotchPackets.sendBalance(creator, BalanceStore.get(creator));
        }
        TradeOffer offer = new TradeOffer(UUID.randomUUID(), creator.getUuid(),
                creator.getName().getString(), targetName, copyAll(offered), offeredCoins, price,
                copyAll(requested), creator.getWorld().getTime());
        state.add(offer);
        creator.sendMessage(Text.literal("Trade offer created" + (offer.isOpen() ? " (open to anyone)."
                : " for " + targetName + ".")).formatted(Formatting.GREEN), false);
        return true;
    }

    /** Accept an offer. Returns false (with a message) if it can't be completed. */
    public static boolean accept(ServerPlayerEntity accepter, UUID offerId) {
        TradeOfferState state = TradeOfferState.get(accepter.getServer());
        TradeOffer offer = state.get(offerId);
        if (offer == null) {
            accepter.sendMessage(Text.literal("That offer is no longer available.").formatted(Formatting.RED), false);
            return false;
        }
        if (offer.creatorUuid().equals(accepter.getUuid())) {
            accepter.sendMessage(Text.literal("You can't accept your own offer.").formatted(Formatting.RED), false);
            return false;
        }
        if (!offer.acceptableBy(accepter.getName().getString())) {
            accepter.sendMessage(Text.literal("This offer isn't directed at you.").formatted(Formatting.RED), false);
            return false;
        }
        // Verify the accepter can pay (coins + every requested stack, totals merged by item type).
        if (offer.priceCoins() > 0 && BalanceStore.get(accepter) < offer.priceCoins()) {
            accepter.sendMessage(Text.literal("You need " + offer.priceCoins() + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " for this trade.").formatted(Formatting.RED), false);
            return false;
        }
        List<ItemStack> wants = aggregate(offer.requestedItems());
        for (ItemStack want : wants) {
            if (countInInventory(accepter, want) < want.getCount()) {
                accepter.sendMessage(Text.literal("You need " + want.getCount() + "x "
                        + want.getName().getString() + " for this trade.").formatted(Formatting.RED), false);
                return false;
            }
        }

        MinecraftServer server = accepter.getServer();

        // Take payment from the accepter → creator (coins by UUID, items to the mailbox if offline).
        if (offer.priceCoins() > 0) {
            BalanceStore.subtract(accepter, offer.priceCoins(), TransactionReason.TRADE, "trade offer payment");
            NotchPackets.sendBalance(accepter, BalanceStore.get(accepter));
            payCreatorCoins(server, offer, offer.priceCoins());
        }
        for (ItemStack want : wants) {
            removeFromInventory(accepter, want, want.getCount());
            deliverToCreator(server, offer, want.copy());
        }

        // Hand the escrowed items and coins to the accepter.
        for (ItemStack st : offer.offeredItems()) {
            giveOrDrop(accepter, st.copy());
        }
        if (offer.offeredCoins() > 0) {
            CurrencyApi.deposit(accepter, offer.offeredCoins(), TransactionReason.TRADE, "trade offer payout");
            NotchPackets.sendBalance(accepter, BalanceStore.get(accepter));
        }
        accepter.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.8F, 1.2F);
        accepter.sendMessage(Text.literal("Trade complete. Received ")
                .append(Text.literal(offer.summary()).formatted(Formatting.YELLOW))
                .append(Text.literal(".").formatted(Formatting.GREEN)).formatted(Formatting.GREEN), false);

        // Notify the creator if online.
        ServerPlayerEntity creator = server.getPlayerManager().getPlayer(offer.creatorUuid());
        if (creator != null) {
            creator.sendMessage(Text.literal(accepter.getName().getString() + " accepted your trade offer for ")
                    .append(Text.literal(offer.summary()).formatted(Formatting.YELLOW))
                    .append(Text.literal(".").formatted(Formatting.GREEN)).formatted(Formatting.GREEN), false);
        }

        state.remove(offerId);
        return true;
    }

    /** Cancel an offer and return the escrowed items and coins to the creator. */
    public static boolean cancel(ServerPlayerEntity creator, UUID offerId) {
        TradeOfferState state = TradeOfferState.get(creator.getServer());
        TradeOffer offer = state.get(offerId);
        if (offer == null || !offer.creatorUuid().equals(creator.getUuid())) {
            creator.sendMessage(Text.literal("That isn't one of your offers.").formatted(Formatting.RED), false);
            return false;
        }
        for (ItemStack st : offer.offeredItems()) {
            giveOrDrop(creator, st.copy());
        }
        if (offer.offeredCoins() > 0) {
            CurrencyApi.deposit(creator, offer.offeredCoins(), TransactionReason.TRADE, "trade offer escrow refund");
            NotchPackets.sendBalance(creator, BalanceStore.get(creator));
        }
        state.remove(offerId);
        creator.sendMessage(Text.literal("Offer cancelled. Your items were returned.").formatted(Formatting.GREEN), false);
        return true;
    }

    // ---- helpers ----

    private static List<ItemStack> copyAll(List<ItemStack> stacks) {
        java.util.List<ItemStack> copies = new java.util.ArrayList<>();
        if (stacks != null) {
            for (ItemStack st : stacks) {
                if (!st.isEmpty()) copies.add(st.copy());
            }
        }
        return copies;
    }

    /** Merge stacks of the same item type into one total each, so "have enough?" checks and
     *  removals are correct even when the want grid holds duplicates of one item. */
    private static List<ItemStack> aggregate(List<ItemStack> stacks) {
        java.util.List<ItemStack> totals = new java.util.ArrayList<>();
        for (ItemStack st : stacks) {
            if (st.isEmpty()) continue;
            boolean merged = false;
            for (ItemStack have : totals) {
                if (StackData.canCombine(have, st)) {
                    have.setCount(have.getCount() + st.getCount());
                    merged = true;
                    break;
                }
            }
            if (!merged) totals.add(st.copy());
        }
        return totals;
    }

    private static void payCreatorCoins(MinecraftServer server, TradeOffer offer, long amount) {
        ServerPlayerEntity creator = server.getPlayerManager().getPlayer(offer.creatorUuid());
        if (creator != null) {
            CurrencyApi.deposit(creator, amount, TransactionReason.TRADE, "trade offer sale");
        } else {
            CurrencyApi.deposit(server, offer.creatorUuid(), amount, TransactionReason.TRADE, "trade offer sale (offline)");
        }
    }

    private static void deliverToCreator(MinecraftServer server, TradeOffer offer, ItemStack stack) {
        ServerPlayerEntity creator = server.getPlayerManager().getPlayer(offer.creatorUuid());
        if (creator != null) {
            giveOrDrop(creator, stack);
        } else {
            TradeOfferState.get(server).addMail(offer.creatorUuid(), stack);
        }
    }

    private static void giveOrDrop(ServerPlayerEntity player, ItemStack stack) {
        while (!stack.isEmpty()) {
            ItemStack chunk = stack.split(Math.min(stack.getCount(), stack.getMaxCount()));
            if (!player.getInventory().insertStack(chunk) && !chunk.isEmpty()) {
                player.dropItem(chunk, false);
            }
        }
    }

    private static int countInInventory(ServerPlayerEntity player, ItemStack match) {
        int n = 0;
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack st = player.getInventory().getStack(i);
            if (!st.isEmpty() && StackData.canCombine(st, match)) n += st.getCount();
        }
        return n;
    }

    private static void removeFromInventory(ServerPlayerEntity player, ItemStack match, int count) {
        int remaining = count;
        for (int i = 0; i < player.getInventory().size() && remaining > 0; i++) {
            ItemStack st = player.getInventory().getStack(i);
            if (!st.isEmpty() && StackData.canCombine(st, match)) {
                int take = Math.min(remaining, st.getCount());
                st.decrement(take);
                remaining -= take;
            }
        }
    }
}
