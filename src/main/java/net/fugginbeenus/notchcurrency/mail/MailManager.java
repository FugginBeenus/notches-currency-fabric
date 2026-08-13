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
     * Sends a player their inbox so the client can show it.
     *
     * <p>Items travel as an id and a count rather than a serialised stack: the wire shape of a stack
     * has changed twice across the versions this mod builds for, and the screen only needs enough to
     * draw an icon and a name.
     */
    public static void openInbox(ServerPlayer player, String boxOwner) {
        MailSweep.run(player.level().getServer());
        List<MailItem> waiting = MailState.get(player.level().getServer()).inbox(player.getUUID());

        var buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUtf(boxOwner == null ? "" : boxOwner, 32);
        buf.writeVarInt(waiting.size());
        for (MailItem item : waiting) {
            buf.writeUUID(item.id());
            buf.writeUtf(item.sender(), 64);
            buf.writeUtf(item.note(), 128);
            ItemStack stack = item.stack();
            if (stack.isEmpty()) {
                buf.writeUtf("", 128);
                buf.writeVarInt(0);
                buf.writeUtf("", 128);
            } else {
                buf.writeUtf(String.valueOf(
                        net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem())), 128);
                buf.writeVarInt(stack.getCount());
                buf.writeUtf(stack.getHoverName().getString(), 128);
            }
            buf.writeLong(item.coins());
        }
        net.fugginbeenus.notchcurrency.compat.Net.sendToClient(player, NotchPackets.MAIL_OPEN, buf);
    }

    /**
     * Opens the parcel screen.
     *
     * <p>The list and the pre-selection go first: the menu opens a screen built by Minecraft, which
     * cannot be handed constructor arguments, so both arrive as packets just before it.
     */
    public static void openPost(ServerPlayer sender, java.util.UUID aimedAt) {
        sendRecipients(sender);
        if (aimedAt != null) {
            var buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
            buf.writeUUID(aimedAt);
            net.fugginbeenus.notchcurrency.compat.Net.sendToClient(
                    sender, NotchPackets.MAIL_AIM, buf);
        }
        MailPostScreenHandler.open(sender);
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
