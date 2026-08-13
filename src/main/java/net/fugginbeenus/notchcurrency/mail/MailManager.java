package net.fugginbeenus.notchcurrency.mail;

import net.fugginbeenus.notchcurrency.compat.Msg;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.core.CurrencyText;
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
        Msg.chat(player, Component.literal("You have " + waiting + (waiting == 1 ? " parcel" : " parcels"))
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal(" waiting in the mail.").withStyle(ChatFormatting.GRAY)));
    }

    /**
     * Hands over one entry, unwrapped.
     *
     * <p>This is the delivered-to-your-hands path, used by the mailman and by the commands. The
     * mailbox does not come through here: it gives out sealed parcels the player opens themselves.
     *
     * <p>The coins and the goods are taken separately: money always fits, an item might not, and a
     * player with one free slot should get the money and keep the goods rather than neither.
     */
    public static boolean collect(ServerPlayer player, UUID itemId) {
        return collect(player, itemId, true);
    }

    /**
     * @param announce false when the caller is emptying the whole box and will sum it up itself.
     *                 Thirty parcels announcing themselves one at a time is a wall of chat.
     */
    public static boolean collect(ServerPlayer player, UUID itemId, boolean announce) {
        MailState state = MailState.get(player.level().getServer());
        MailItem item = state.take(player.getUUID(), itemId);
        if (item == null) return false;

        boolean tookCoins = false;
        if (item.coins() > 0L) {
            BalanceStore.add(player, item.coins(), TransactionReason.AUCTION, "collected from mail");
            NotchPackets.sendBalance(player, BalanceStore.get(player));
            if (announce) {
                Msg.chat(player, Component.literal("Collected ")
                        .append(Component.literal(item.coins() + " ").withStyle(ChatFormatting.GOLD))
                        .append(NotchCurrency.coinIcon())
                        .append(Component.literal(" from " + item.sender() + ".")
                                .withStyle(ChatFormatting.GREEN)));
            }
            tookCoins = true;
        }

        List<ItemStack> left = new java.util.ArrayList<>();
        int given = 0;
        for (ItemStack stack : item.contents()) {
            ItemStack giving = stack.copy();
            player.getInventory().add(giving);
            if (giving.isEmpty()) given++;
            else left.add(giving);
        }

        if (given > 0 && announce) {
            Msg.chat(player, Component.literal("Collected ")
                    .append(Component.literal(given + (given == 1 ? " item" : " items"))
                            .withStyle(ChatFormatting.WHITE))
                    .append(Component.literal(" from " + item.sender() + ".").withStyle(ChatFormatting.GREEN)));
        }

        if (!left.isEmpty()) {
            // Only what would not fit goes back in the box.
            state.putBack(player.getUUID(), item.withContents(left).without(false, true));
            Msg.chat(player, Component.literal("Your inventory is full. The rest is still in the mail.")
                    .withStyle(ChatFormatting.RED));
            player.playSound(SoundEvents.VILLAGER_NO, 0.7F, 1.0F);
            return tookCoins;
        }

        if (announce && (given > 0 || tookCoins)) player.playSound(SoundEvents.ITEM_PICKUP, 0.8F, 1.0F);
        return true;
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

    /** Opens the Inbox tab. */
    public static void openInbox(ServerPlayer player) {
        MailInboxMenu.open(player);
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
     * <p>Everything in the slots goes into one parcel, so the recipient opens one thing rather than
     * finding four entries with the same note on each. Anything the recipient's box will not hold
     * stays with the sender rather than disappearing, which is the only honest thing to do with
     * someone else's goods.
     */
    public static void send(ServerPlayer sender, UUID recipient, String note, long coins,
                            MailPostScreenHandler parcel) {
        if (recipient == null) return;
        if (recipient.equals(sender.getUUID())) {
            Msg.chat(sender, Component.literal("You cannot post a parcel to yourself.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        // Never trust the amount off the wire: it decides how much money moves.
        long money = Math.max(0L, coins);
        if (money > 0L && BalanceStore.get(sender) < money) {
            Msg.chat(sender, Component.literal("You do not have that much to send.")
                    .withStyle(ChatFormatting.RED));
            return;
        }
        if (parcel.isEmpty() && money <= 0L) {
            Msg.chat(sender, Component.literal("Put something in the parcel first.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        var server = sender.level().getServer();
        String from = sender.getName().getString();
        String trimmed = note == null ? "" : note.strip();

        List<ItemStack> goods = parcel.takeContents();
        if (money > 0L) {
            BalanceStore.subtract(sender, money, TransactionReason.PAY, "posted a parcel");
            NotchPackets.sendBalance(sender, BalanceStore.get(sender));
        }

        if (!post(server, recipient, MailItem.parcel(from, trimmed, goods, money))) {
            Msg.chat(sender, Component.literal("Their mailbox is full. Nothing was sent.")
                    .withStyle(ChatFormatting.RED));
            // Everything goes back, money included: it left the sender only a moment ago.
            for (ItemStack stack : goods) {
                if (!sender.getInventory().add(stack)) sender.drop(stack, false);
            }
            if (money > 0L) {
                BalanceStore.add(sender, money, TransactionReason.PAY, "parcel came back");
                NotchPackets.sendBalance(sender, BalanceStore.get(sender));
            }
            return;
        }

        String to = MailState.get(server).knownMailboxes().getOrDefault(recipient, "them");
        Component what = money > 0L
                ? Component.literal("a parcel with " + money + " " + CurrencyText.word() + " in it")
                : Component.literal("a parcel");
        Msg.chat(sender, Component.literal("Posted ").append(what)
                .append(Component.literal(" to " + to + ".")).withStyle(ChatFormatting.GREEN));
        sender.playSound(net.minecraft.sounds.SoundEvents.BOOK_PAGE_TURN, 0.8F, 1.0F);
    }

    /** One line for a whole box, rather than two per parcel. */
    public static void announceCollected(ServerPlayer player, int parcels, int items, long coins,
                                         boolean ranOutOfRoom) {
        if (parcels <= 0) {
            if (ranOutOfRoom) {
                Msg.chat(player, Component.literal("Your inventory is full.")
                        .withStyle(ChatFormatting.RED));
                player.playSound(SoundEvents.VILLAGER_NO, 0.7F, 1.0F);
            }
            return;
        }

        Component line = Component.literal("Opened " + parcels + (parcels == 1 ? " parcel" : " parcels"))
                .withStyle(ChatFormatting.GREEN);
        if (items > 0) {
            line = line.copy().append(Component.literal(": " + items + (items == 1 ? " item" : " items"))
                    .withStyle(ChatFormatting.WHITE));
        }
        if (coins > 0L) {
            line = line.copy()
                    .append(Component.literal(items > 0 ? " and " : ": ").withStyle(ChatFormatting.GREEN))
                    .append(Component.literal(coins + " " + CurrencyText.word())
                            .withStyle(ChatFormatting.GOLD));
        }
        Msg.chat(player, line.copy().append(Component.literal(".").withStyle(ChatFormatting.GREEN)));
        player.playSound(SoundEvents.ITEM_PICKUP, 0.8F, 1.0F);

        if (ranOutOfRoom) {
            Msg.chat(player, Component.literal("Your inventory filled up. The rest is still waiting.")
                    .withStyle(ChatFormatting.RED));
        }
    }

    /** Empties the box as far as the player's inventory allows, unwrapped. */
    public static int collectAll(ServerPlayer player) {
        List<MailItem> waiting = MailState.get(player.level().getServer()).inbox(player.getUUID());
        int taken = 0;
        for (MailItem item : waiting) {
            if (collect(player, item.id())) taken++;
        }
        return taken;
    }
}
