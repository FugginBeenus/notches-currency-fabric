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

public final class MailManager {

    private MailManager() {}

    public static boolean post(MinecraftServer server, UUID recipient, MailItem item) {
        MailState state = MailState.get(server);
        long now = server.overworld().getGameTime();
        if (!state.post(recipient, item, now)) return false;

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

    public static void greet(ServerPlayer player) {
        int waiting = MailState.get(player.level().getServer()).count(player.getUUID());
        if (waiting <= 0) return;
        Msg.chat(player, Component.literal("You have " + waiting + (waiting == 1 ? " parcel" : " parcels"))
                .withStyle(ChatFormatting.AQUA)
                .append(Component.literal(" waiting in the mail.").withStyle(ChatFormatting.GRAY)));
    }

    public static boolean collect(ServerPlayer player, UUID itemId) {
        return collect(player, itemId, true);
    }

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
            state.putBack(player.getUUID(), item.withContents(left).without(false, true));
            Msg.chat(player, Component.literal("Your inventory is full. The rest is still in the mail.")
                    .withStyle(ChatFormatting.RED));
            player.playSound(SoundEvents.VILLAGER_NO, 0.7F, 1.0F);
            return tookCoins;
        }

        if (announce && (given > 0 || tookCoins)) player.playSound(SoundEvents.ITEM_PICKUP, 0.8F, 1.0F);
        return true;
    }

    public static void openPost(ServerPlayer sender, UUID aimedAt) {
        sendRecipients(sender);
        if (aimedAt != null) {
            var buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
            buf.writeUUID(aimedAt);
            net.fugginbeenus.notchcurrency.compat.Net.sendToClient(sender, NotchPackets.MAIL_AIM, buf);
        }
        MailPostScreenHandler.open(sender);
    }

    public static void openInbox(ServerPlayer player) {
        MailInboxMenu.open(player);
    }

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

    public static void send(ServerPlayer sender, UUID recipient, String note, long coins,
                            MailPostScreenHandler parcel) {
        if (recipient == null) return;
        if (recipient.equals(sender.getUUID())) {
            Msg.chat(sender, Component.literal("You cannot post a parcel to yourself.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

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

    public static int collectAll(ServerPlayer player) {
        List<MailItem> waiting = MailState.get(player.level().getServer()).inbox(player.getUUID());
        int taken = 0;
        for (MailItem item : waiting) {
            if (collect(player, item.id())) taken++;
        }
        return taken;
    }
}
