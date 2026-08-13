package net.fugginbeenus.notchcurrency.mail;

import net.fugginbeenus.notchcurrency.compat.Msg;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
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

/**
 * Posting things to players and handing them over again.
 *
 * <p>Everything that owes a player something goes through here rather than dropping it in their lap,
 * because the player is usually not standing there when the debt is created. Auction sales, offline
 * trade offers and parcels from other players all post the same way.
 */
public final class MailManager {

    private MailManager() {}

    /**
     * Puts something in a player's box.
     *
     * @return false if the box is full, in which case nothing was posted and the caller still owns
     *         whatever it was trying to send
     */
    public static boolean post(MinecraftServer server, UUID recipient, MailItem item) {
        MailState state = MailState.get(server);
        long now = server.overworld().getGameTime();
        if (!state.post(recipient, item, now)) return false;

        // A nudge only if they are here to read it. Anyone offline gets told when they log in.
        ServerPlayer online = server.getPlayerList().getPlayer(recipient);
        if (online != null) {
            Msg.chat(online, Component.literal("Mail from " + item.sender() + ". ")
                    .withStyle(ChatFormatting.AQUA)
                    .append(Component.literal("Open any mailbox to collect it.")
                            .withStyle(ChatFormatting.GRAY)));
            online.playSound(SoundEvents.ITEM_PICKUP, 0.6F, 1.4F);
        }
        return true;
    }

    /** Told once on login, so nothing sits forgotten in a box for a month. */
    public static void greet(ServerPlayer player) {
        int waiting = MailState.get(player.level().getServer()).count(player.getUUID());
        if (waiting <= 0) return;
        Msg.chat(player, Component.literal("You have " + waiting + (waiting == 1 ? " item" : " items"))
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal(" waiting in the mail.").withStyle(ChatFormatting.GRAY)));
    }

    /**
     * Hands over one entry.
     *
     * <p>The coins and the goods are taken separately: money always fits, an item might not, and a
     * player with one free slot should get the money and keep the parcel rather than neither.
     */
    public static boolean collect(ServerPlayer player, UUID itemId) {
        MailState state = MailState.get(player.level().getServer());
        MailItem item = state.take(player.getUUID(), itemId);
        if (item == null) return false;

        boolean tookCoins = false;
        if (item.coins() > 0L) {
            BalanceStore.add(player, item.coins(), TransactionReason.AUCTION, "collected from mail");
            NotchPackets.sendBalance(player, BalanceStore.get(player));
            Msg.chat(player, Component.literal("Collected ")
                    .append(Component.literal(item.coins() + " ").withStyle(ChatFormatting.GOLD))
                    .append(NotchCurrency.coinIcon())
                    .append(Component.literal(" from " + item.sender() + ".").withStyle(ChatFormatting.GREEN)));
            tookCoins = true;
        }

        boolean tookStack = false;
        if (!item.stack().isEmpty()) {
            ItemStack giving = item.stack().copy();
            player.getInventory().add(giving);
            if (giving.isEmpty()) {
                Msg.chat(player, Component.literal("Collected ")
                        .append(item.stack().getHoverName().copy())
                        .append(Component.literal(" from " + item.sender() + ".").withStyle(ChatFormatting.GREEN)));
                tookStack = true;
            } else {
                // Part of a stack may have gone in. Only what is left goes back in the box.
                MailItem remainder = new MailItem(item.id(), item.sender(), item.note(),
                        giving, 0L, item.sentAt());
                state.putBack(player.getUUID(), remainder);
                Msg.chat(player, Component.literal("Your inventory is full. The rest is still in the mail.")
                        .withStyle(ChatFormatting.RED));
                player.playSound(SoundEvents.VILLAGER_NO, 0.7F, 1.0F);
                return tookCoins;
            }
        }

        if (tookStack || tookCoins) player.playSound(SoundEvents.ITEM_PICKUP, 0.8F, 1.0F);
        return true;
    }

    /** Empties the box as far as the player's inventory allows. */
    public static int collectAll(ServerPlayer player) {
        List<MailItem> waiting = MailState.get(player.level().getServer()).inbox(player.getUUID());
        int taken = 0;
        for (MailItem item : waiting) {
            if (collect(player, item.id())) taken++;
        }
        return taken;
    }
}
