package net.fugginbeenus.notchcurrency.trade;

import net.fugginbeenus.notchcurrency.compat.StackData;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import java.util.List;
import java.util.UUID;

public final class TradeOfferManager {

    public static final int MAX_PER_PLAYER = 10;

    private TradeOfferManager() {}

    public static void deliverMail(ServerPlayer sp) {
        TradeOfferState state = TradeOfferState.get(sp.level().getServer());
        if (!state.hasMail(sp.getUUID())) return;
        List<ItemStack> items = state.claimMail(sp.getUUID());
        java.util.List<ItemStack> leftover = new java.util.ArrayList<>();
        for (ItemStack st : items) {
            if (!sp.getInventory().add(st)) leftover.add(st);
        }
        if (!leftover.isEmpty()) {
            state.returnMail(sp.getUUID(), leftover); // no room; keep for next time
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("You have trade items waiting - free up inventory space to receive them.")
                    .withStyle(ChatFormatting.YELLOW));
        }
        if (items.size() != leftover.size()) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Received items from a completed trade offer.").withStyle(ChatFormatting.GREEN));
        }
    }

    public static boolean createOffer(ServerPlayer creator, List<ItemStack> offered, long offeredCoins,
                                      long price, List<ItemStack> requested, String targetName) {
        TradeOfferState state = TradeOfferState.get(creator.level().getServer());
        if (state.countBy(creator.getUUID()) >= MAX_PER_PLAYER) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(creator, Component.literal("You already have " + MAX_PER_PLAYER + " open offers.").withStyle(ChatFormatting.RED));
            return false;
        }
        if (offeredCoins > 0) {
            if (BalanceStore.get(creator) < offeredCoins) {
                net.fugginbeenus.notchcurrency.compat.Msg.chat(creator, Component.literal("You don't have " + offeredCoins + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " to attach.").withStyle(ChatFormatting.RED));
                return false;
            }
            BalanceStore.subtract(creator, offeredCoins, TransactionReason.TRADE, "trade offer escrow");
            NotchPackets.sendBalance(creator, BalanceStore.get(creator));
        }
        TradeOffer offer = new TradeOffer(UUID.randomUUID(), creator.getUUID(),
                creator.getName().getString(), targetName, copyAll(offered), offeredCoins, price,
                copyAll(requested), creator.level().getGameTime());
        state.add(offer);
        net.fugginbeenus.notchcurrency.compat.Msg.chat(creator, Component.literal("Trade offer created" + (offer.isOpen() ? " (open to anyone)."
                : " for " + targetName + ".")).withStyle(ChatFormatting.GREEN));
        return true;
    }

    public static boolean accept(ServerPlayer accepter, UUID offerId) {
        TradeOfferState state = TradeOfferState.get(accepter.level().getServer());
        TradeOffer offer = state.get(offerId);
        if (offer == null) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(accepter, Component.literal("That offer is no longer available.").withStyle(ChatFormatting.RED));
            return false;
        }
        if (offer.creatorUuid().equals(accepter.getUUID())) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(accepter, Component.literal("You can't accept your own offer.").withStyle(ChatFormatting.RED));
            return false;
        }
        if (!offer.acceptableBy(accepter.getName().getString())) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(accepter, Component.literal("This offer isn't directed at you.").withStyle(ChatFormatting.RED));
            return false;
        }
        // Verify the accepter can pay (coins + every requested stack, totals merged by item type).
        if (offer.priceCoins() > 0 && BalanceStore.get(accepter) < offer.priceCoins()) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(accepter, Component.literal("You need " + offer.priceCoins() + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " for this trade.").withStyle(ChatFormatting.RED));
            return false;
        }
        List<ItemStack> wants = aggregate(offer.requestedItems());
        for (ItemStack want : wants) {
            if (countInInventory(accepter, want) < want.getCount()) {
                net.fugginbeenus.notchcurrency.compat.Msg.chat(accepter, Component.literal("You need " + want.getCount() + "x "
                        + want.getHoverName().getString() + " for this trade.").withStyle(ChatFormatting.RED));
                return false;
            }
        }

        MinecraftServer server = accepter.level().getServer();

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
        accepter.playSound(SoundEvents.PLAYER_LEVELUP, 0.8F, 1.2F);
        net.fugginbeenus.notchcurrency.compat.Msg.chat(accepter, Component.literal("Trade complete - received ")
                .append(Component.literal(offer.summary()).withStyle(ChatFormatting.YELLOW))
                .append(Component.literal(".").withStyle(ChatFormatting.GREEN)).withStyle(ChatFormatting.GREEN));

        // Notify the creator if online.
        ServerPlayer creator = server.getPlayerList().getPlayer(offer.creatorUuid());
        if (creator != null) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(creator, Component.literal(accepter.getName().getString() + " accepted your trade offer for ")
                    .append(Component.literal(offer.summary()).withStyle(ChatFormatting.YELLOW))
                    .append(Component.literal(".").withStyle(ChatFormatting.GREEN)).withStyle(ChatFormatting.GREEN));
        }

        state.remove(offerId);
        return true;
    }

    public static boolean cancel(ServerPlayer creator, UUID offerId) {
        TradeOfferState state = TradeOfferState.get(creator.level().getServer());
        TradeOffer offer = state.get(offerId);
        if (offer == null || !offer.creatorUuid().equals(creator.getUUID())) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(creator, Component.literal("That isn't one of your offers.").withStyle(ChatFormatting.RED));
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
        net.fugginbeenus.notchcurrency.compat.Msg.chat(creator, Component.literal("Offer cancelled - your items were returned.").withStyle(ChatFormatting.GREEN));
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
        ServerPlayer creator = server.getPlayerList().getPlayer(offer.creatorUuid());
        if (creator != null) {
            CurrencyApi.deposit(creator, amount, TransactionReason.TRADE, "trade offer sale");
        } else {
            CurrencyApi.deposit(server, offer.creatorUuid(), amount, TransactionReason.TRADE, "trade offer sale (offline)");
        }
    }

    private static void deliverToCreator(MinecraftServer server, TradeOffer offer, ItemStack stack) {
        ServerPlayer creator = server.getPlayerList().getPlayer(offer.creatorUuid());
        if (creator != null) {
            giveOrDrop(creator, stack);
        } else {
            TradeOfferState.get(server).addMail(offer.creatorUuid(), stack);
        }
    }

    private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
        while (!stack.isEmpty()) {
            ItemStack chunk = stack.split(Math.min(stack.getCount(), stack.getMaxStackSize()));
            if (!player.getInventory().add(chunk) && !chunk.isEmpty()) {
                player.drop(chunk, false);
            }
        }
    }

    private static int countInInventory(ServerPlayer player, ItemStack match) {
        int n = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (!st.isEmpty() && StackData.canCombine(st, match)) n += st.getCount();
        }
        return n;
    }

    private static void removeFromInventory(ServerPlayer player, ItemStack match, int count) {
        int remaining = count;
        for (int i = 0; i < player.getInventory().getContainerSize() && remaining > 0; i++) {
            ItemStack st = player.getInventory().getItem(i);
            if (!st.isEmpty() && StackData.canCombine(st, match)) {
                int take = Math.min(remaining, st.getCount());
                st.shrink(take);
                remaining -= take;
            }
        }
    }
}
