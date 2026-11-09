package net.fugginbeenus.notchcurrency.npc;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.compat.Chat;
import net.fugginbeenus.notchcurrency.compat.Msg;
import net.fugginbeenus.notchcurrency.economy.TransactionReason;
import net.fugginbeenus.notchcurrency.economy.npc.NpcRole;
import net.fugginbeenus.notchcurrency.economy.npc.NpcRoleState;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.npc.action.NpcActionSweep;
import net.fugginbeenus.notchcurrency.shop.PlayerShop;
import net.fugginbeenus.notchcurrency.shop.PlayerShopManager;
import net.fugginbeenus.notchcurrency.shop.ShopState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class NpcTransfer {

    private static final long OFFER_MILLIS = 60_000L;
    private static final int LISTED = 16;

    private record Offer(UUID npc, UUID from, String fromName, @Nullable UUID ownerAtOffer, long expires) {}

    // Keyed by the player being asked.
    private static final Map<UUID, Offer> OFFERS = new HashMap<>();

    private NpcTransfer() {}

    public static void choose(ServerPlayer giver, NotchNpcEntity npc) {
        if (!mayGive(giver, npc)) return;
        List<ServerPlayer> others = new ArrayList<>(giver.level().getServer().getPlayerList().getPlayers());
        others.remove(giver);
        if (others.isEmpty()) {
            say(giver, "No one else is online to give it to.", ChatFormatting.YELLOW);
            return;
        }
        others.sort(Comparator.comparing(p -> p.getName().getString(), String.CASE_INSENSITIVE_ORDER));

        String command = "/npctransfer offer " + npc.getUUID() + " ";
        MutableComponent line = Component.literal("Give " + name(npc) + " to:").withStyle(ChatFormatting.GOLD);
        for (ServerPlayer p : others.subList(0, Math.min(LISTED, others.size()))) {
            String who = p.getName().getString();
            line.append(Component.literal(" "))
                    .append(button("[" + who + "]", ChatFormatting.AQUA, Chat.runCommand(command + who),
                            "Offer it to " + who));
        }
        if (others.size() > LISTED) {
            line.append(Component.literal(" "))
                    .append(button("[Someone else]", ChatFormatting.GRAY, Chat.suggestCommand(command),
                            "Type their name"));
        }
        Msg.chat(giver, line);
    }

    public static void offer(ServerPlayer giver, UUID npcId, ServerPlayer target) {
        if (!(giver.serverLevel().getEntity(npcId) instanceof NotchNpcEntity npc)) {
            say(giver, "Stand near the NPC and try again.", ChatFormatting.RED);
            return;
        }
        if (!mayGive(giver, npc)) return;
        if (target == giver) {
            say(giver, "You already have it.", ChatFormatting.YELLOW);
            return;
        }
        if (atShopLimit(npc, target)) {
            say(giver, target.getName().getString() + " already has the most shops allowed.", ChatFormatting.RED);
            return;
        }

        OFFERS.put(target.getUUID(), new Offer(npc.getUUID(), giver.getUUID(), giver.getName().getString(),
                npc.getOwner(), System.currentTimeMillis() + OFFER_MILLIS));

        PlayerShop shop = shopOf(npc);
        String what = name(npc) + (shop == null ? "" : " and its shop, " + shop.getShopName());
        Msg.chat(target, Component.literal(giver.getName().getString() + " wants to give you " + what + ". ")
                .withStyle(ChatFormatting.GOLD)
                .append(button("[Accept]", ChatFormatting.GREEN, Chat.runCommand("/npctransfer accept"), "Take it"))
                .append(Component.literal(" "))
                .append(button("[Decline]", ChatFormatting.RED, Chat.runCommand("/npctransfer decline"), "Say no")));
        say(giver, "Asked " + target.getName().getString() + ". The offer lasts 60 seconds.", ChatFormatting.GREEN);
    }

    public static void accept(ServerPlayer target) {
        Offer offer = OFFERS.remove(target.getUUID());
        if (offer == null || offer.expires() < System.currentTimeMillis()) {
            say(target, "There's no offer waiting for you.", ChatFormatting.YELLOW);
            return;
        }
        MinecraftServer server = target.level().getServer();
        ServerPlayer giver = server.getPlayerList().getPlayer(offer.from());
        NotchNpcEntity npc = find(server, offer.npc());
        if (npc == null) {
            say(target, "That NPC isn't loaded right now. Ask " + offer.fromName()
                    + " to stand near it and offer it again.", ChatFormatting.RED);
            if (giver != null) {
                say(giver, target.getName().getString() + " accepted, but the NPC wasn't loaded. Stand near it and try again.",
                        ChatFormatting.RED);
            }
            return;
        }
        if (!Objects.equals(npc.getOwner(), offer.ownerAtOffer())) {
            say(target, "That NPC changed hands before you accepted.", ChatFormatting.RED);
            return;
        }
        if (atShopLimit(npc, target)) {
            say(target, "You already have the most shops allowed.", ChatFormatting.RED);
            return;
        }

        int removed = handOver(server, npc, shopOf(npc), target.getUUID(), target.getName().getString());
        say(target, "You now own " + name(npc) + ".", ChatFormatting.GREEN);
        if (giver != null) {
            say(giver, target.getName().getString() + " accepted. " + name(npc) + " is theirs now.", ChatFormatting.GREEN);
            if (removed > 0) {
                say(giver, "Its admin-only actions were removed, because " + target.getName().getString()
                        + " isn't an op.", ChatFormatting.GRAY);
            }
        }
    }

    public static void decline(ServerPlayer target) {
        Offer offer = OFFERS.remove(target.getUUID());
        if (offer == null) {
            say(target, "There's no offer waiting for you.", ChatFormatting.YELLOW);
            return;
        }
        say(target, "You said no.", ChatFormatting.GRAY);
        ServerPlayer giver = target.level().getServer().getPlayerList().getPlayer(offer.from());
        if (giver != null) say(giver, target.getName().getString() + " said no.", ChatFormatting.YELLOW);
    }

    // Moves the NPC and its shop. Returns how many admin-only actions came off.
    public static int handOver(MinecraftServer server, @Nullable NotchNpcEntity npc, @Nullable PlayerShop shop,
                               UUID newOwner, String newOwnerName) {
        if (shop != null) {
            long till = shop.withdrawBalance();
            if (till > 0) {
                CurrencyApi.deposit(server, shop.getOwnerId(), till, TransactionReason.SHOP_PAYOUT,
                        "shop handed over: " + shop.getShopName());
            }
            PlayerShopManager.transferOwnership(server, shop.getShopId(), newOwner, newOwnerName);
        }
        if (npc == null) return 0;
        npc.setOwner(newOwner, newOwnerName);
        npc.setActionSweepVersion(0);
        return NpcActionSweep.sweep(npc);
    }

    @Nullable
    public static NotchNpcEntity find(MinecraftServer server, UUID npcId) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(npcId) instanceof NotchNpcEntity npc) return npc;
        }
        return null;
    }

    private static boolean mayGive(ServerPlayer giver, NotchNpcEntity npc) {
        if (!npc.canEdit(giver)) {
            say(giver, "This isn't your NPC.", ChatFormatting.RED);
            return false;
        }
        NpcRoleState.Assignment role = NpcRoleState.get(giver.level().getServer()).get(npc.getUUID());
        PlayerShop shop = shopOf(npc);
        if ((role != null && role.role() == NpcRole.ADMIN_SHOP) || (shop != null && shop.isAdminMode())) {
            say(giver, "Admin shops can't be given away.", ChatFormatting.RED);
            return false;
        }
        return true;
    }

    private static boolean atShopLimit(NotchNpcEntity npc, ServerPlayer target) {
        return shopOf(npc) != null
                && ShopState.get(target.level().getServer()).getShopsByOwner(target.getUUID()).size()
                >= PlayerShopManager.MAX_SHOPS_PER_PLAYER;
    }

    @Nullable
    private static PlayerShop shopOf(NotchNpcEntity npc) {
        return ShopState.get(npc.level().getServer()).getShopByNpc(npc.getUUID());
    }

    private static String name(NotchNpcEntity npc) {
        return npc.getName().getString();
    }

    private static MutableComponent button(String label, ChatFormatting color, ClickEvent click, String hover) {
        return Component.literal(label)
                .withStyle(color, ChatFormatting.BOLD)
                .withStyle(style -> style.withClickEvent(click).withHoverEvent(Chat.showText(Component.literal(hover))));
    }

    private static void say(ServerPlayer player, String text, ChatFormatting color) {
        Msg.chat(player, Component.literal(text).withStyle(color));
    }
}
