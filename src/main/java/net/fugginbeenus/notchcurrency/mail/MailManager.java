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

    /**
     * Sends what a slot cannot carry: the money owed, and who each parcel is from.
     *
     * <p>The labels are in the same order the slots are filled in, so slot n and label n are the
     * same entry. Both walk the inbox skipping entries with no item, so they cannot drift apart.
     */
    public static void sendSummary(ServerPlayer player) {
        MailState state = MailState.get(player.level().getServer());
        List<MailItem> waiting = state.inbox(player.getUUID());
        long coins = 0L;
        int parcels = 0;
        for (MailItem item : waiting) {
            coins += item.coins();
            if (!item.stack().isEmpty()) parcels++;
        }

        // Straight from the open menu rather than from inbox order: the slots keep their positions
        // as things are taken, so anything rebuilt from the list would soon name the wrong parcel.
        MailItem[] shown = player.containerMenu instanceof MailInboxMenu inbox
                ? inbox.shownEntries(state, player.getUUID())
                : new MailItem[0];

        var buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeLong(coins);
        buf.writeVarInt(parcels);
        buf.writeVarInt(shown.length);
        for (MailItem item : shown) {
            buf.writeUtf(item == null ? "" : item.sender(), 64);
            buf.writeUtf(item == null ? "" : item.note(), 128);
        }
        net.fugginbeenus.notchcurrency.compat.Net.sendToClient(player, NotchPackets.MAIL_OPEN, buf);
    }

    /**
     * Hands over the money only.
     *
     * <p>The parcels are sitting in slots the player can drag out themselves, so a button that also
     * emptied those would be taking a decision that is not its to take.
     */
    public static long collectCoins(ServerPlayer player) {
        MailState state = MailState.get(player.level().getServer());
        long total = 0L;
        for (MailItem item : state.inbox(player.getUUID())) {
            if (item.coins() <= 0L) continue;
            MailItem taken = state.take(player.getUUID(), item.id());
            if (taken == null) continue;
            total += taken.coins();
            if (!taken.stack().isEmpty()) state.putBack(player.getUUID(), taken.without(false, true));
        }
        if (total <= 0L) return 0L;

        BalanceStore.add(player, total, TransactionReason.AUCTION, "collected from mail");
        NotchPackets.sendBalance(player, BalanceStore.get(player));
        Msg.chat(player, Component.literal("Collected ")
                .append(Component.literal(total + " ").withStyle(ChatFormatting.GOLD))
                .append(NotchCurrency.coinIcon())
                .append(Component.literal(" from the mail.").withStyle(ChatFormatting.GREEN)));
        player.playSound(SoundEvents.ITEM_PICKUP, 0.8F, 1.0F);
        return total;
    }

    /**
     * Opens the Outbox tab.
     *
     * <p>The recipient list and any pre-selection go first: a menu opens a screen built by
     * Minecraft, which cannot be handed arguments of ours, so both arrive as packets just before it.
     */
    public static void openPost(ServerPlayer sender, UUID aimedAt) {
        sendRecipients(sender);
        if (aimedAt != null) {
            var buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
            buf.writeUUID(aimedAt);
            net.fugginbeenus.notchcurrency.compat.Net.sendToClient(sender, NotchPackets.MAIL_AIM, buf);
        }
        MailPostScreenHandler.open(sender);
    }

    /** Opens the Inbox tab, with the summary the slots cannot carry. */
    public static void openInbox(ServerPlayer player) {
        MailInboxMenu.open(player);
        sendSummary(player);
    }

    /** Everyone with a mailbox, so the sender has a list to choose from rather than typing a name. */
    public static void sendRecipients(ServerPlayer player) {
        var server = player.level().getServer();
        var known = MailState.get(server).knownMailboxes();

        var buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeVarInt(known.size());
        for (var entry : known.entrySet()) {
            buf.writeUUID(entry.getKey());
            buf.writeUtf(entry.getValue(), 32);
            buf.writeBoolean(server.getPlayerList().getPlayer(entry.getKey()) != null);
        }
        net.fugginbeenus.notchcurrency.compat.Net.sendToClient(
                player, NotchPackets.MAIL_RECIPIENTS, buf);
    }

    /**
     * Posts what is in the parcel slots to another player.
     *
     * <p>Each stack becomes its own entry, so the recipient can take a full inventory's worth one at
     * a time. Anything the recipient's box will not hold stays with the sender rather than
     * disappearing, which is the only honest thing to do with someone else's goods.
     */
    public static void send(ServerPlayer sender, UUID recipient, String note,
                            MailPostScreenHandler parcel) {
        if (recipient == null) return;
        if (recipient.equals(sender.getUUID())) {
            Msg.chat(sender, Component.literal("You cannot post a parcel to yourself.")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        if (parcel.isEmpty()) {
            Msg.chat(sender, Component.literal("Put something in the parcel first.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        var server = sender.level().getServer();
        String from = sender.getName().getString();
        String trimmed = note == null ? "" : note.strip();

        int posted = 0;
        for (ItemStack stack : parcel.takeContents()) {
            MailItem item = MailItem.parcel(from, trimmed, stack);
            if (post(server, recipient, item)) {
                posted++;
            } else {
                // Their box is full. Hand it back rather than eating it.
                if (!sender.getInventory().add(stack)) sender.drop(stack, false);
            }
        }

        if (posted == 0) {
            Msg.chat(sender, Component.literal("Their mailbox is full. Nothing was sent.")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        String to = MailState.get(server).knownMailboxes().getOrDefault(recipient, "them");
        Msg.chat(sender, Component.literal("Posted " + posted + (posted == 1 ? " parcel" : " parcels")
                + " to " + to + ".").withStyle(ChatFormatting.GREEN));
        sender.playSound(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 0.8F, 1.0F);
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
