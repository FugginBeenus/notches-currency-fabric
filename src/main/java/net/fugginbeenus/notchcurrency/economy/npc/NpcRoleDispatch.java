package net.fugginbeenus.notchcurrency.economy.npc;

import net.fugginbeenus.notchcurrency.auction.AuctionHouseScreenHandler;
import net.fugginbeenus.notchcurrency.auction.AuctionState;
import net.fugginbeenus.notchcurrency.economy.adminshop.AdminShop;
import net.fugginbeenus.notchcurrency.economy.adminshop.AdminShopMenu;
import net.fugginbeenus.notchcurrency.economy.adminshop.AdminShopState;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyManager;
import net.fugginbeenus.notchcurrency.economy.gambling.SlotMachineManager;
import net.fugginbeenus.notchcurrency.economy.raffle.RaffleManager;
import net.fugginbeenus.notchcurrency.shop.NpcShopLogic;
import net.fugginbeenus.notchcurrency.shop.PlayerShop;
import net.fugginbeenus.notchcurrency.shop.ShopState;
import net.fugginbeenus.notchcurrency.ui.ATMTestScreenHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class NpcRoleDispatch {

    private NpcRoleDispatch() {}

    public static void open(ServerPlayer sp, NpcRole role, @Nullable UUID target, @Nullable Entity npc) {
        if (role == null) role = NpcRole.NONE;
        // Opening hours are checked here rather than at the right-click, because this is the one door
        // every route goes through: the direct interaction, a dialogue choice that opens a screen, and
        // roles bound to other mods' entities through the API. Guarding the door beats guarding the
        // paths to it, since the next path added is guarded before it is written.
        if (npc instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity n && !n.isRoleOpenNow()) {
            net.fugginbeenus.notchcurrency.npc.NpcText.say(sp, n, n.closedLineNow());
            return;
        }
        MinecraftServer server = sp.level().getServer();
        switch (role) {
            case ADMIN_SHOP -> {
                AdminShop shop = target != null ? AdminShopState.get(server).get(target) : null;
                if (shop == null) {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("This shop NPC isn't linked to a valid shop.").withStyle(ChatFormatting.RED));
                } else {
                    AdminShopMenu.sendListing(sp, shop);
                }
            }
            case BANKER -> openAtm(sp);
            case AUCTIONEER -> openAuction(sp);
            case MAILBOX -> {
                // Everything owed, from every source, rather than only the auction house.
                net.fugginbeenus.notchcurrency.mail.MailSweep.run(server);
                int taken = net.fugginbeenus.notchcurrency.mail.MailManager.collectAll(sp);
                if (taken == 0) {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(sp,
                            net.minecraft.network.chat.Component.literal("Your mail is empty.")
                                    .withStyle(net.minecraft.ChatFormatting.GRAY));
                }
            }
            case RAFFLE -> RaffleManager.openScreen(sp);
            case BOUNTY -> BountyManager.openScreen(sp);
            case DEALER -> SlotMachineManager.openScreen(sp);
            case SHOP -> openPlayerShop(sp, npc);
            case GREETER -> greet(sp, npc);
            case ENCHANTER -> net.fugginbeenus.notchcurrency.economy.enchanter.EnchanterManager.openScreen(sp);
            case COSMETICS -> net.fugginbeenus.notchcurrency.economy.cosmetic.CosmeticManager.openScreen(sp,
                    npc != null ? npc.getUUID() : null);
            case RECRUITER -> {
                if (npc instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity n) {
                    net.fugginbeenus.notchcurrency.npc.faction.RecruiterManager.open(sp, n);
                }
            }
            case CUSTOM -> customInteract(sp, npc);
            // NONE is a real choice, not a gap: guards, greeters and villagers-with-dialogue all live
            // here. Saying nothing is the point. Anything else nags every time they're talked to.
            case NONE -> { }
        }
    }

    public static String entryLabel(NpcRole role) {
        return switch (role) {
            case SHOP, ADMIN_SHOP -> "Browse the shop";
            case BANKER -> "Open the bank";
            case AUCTIONEER -> "Auction house";
            case MAILBOX -> "Collect my mail";
            case RAFFLE -> "Try the raffle";
            case BOUNTY -> "See the bounties";
            case DEALER -> "Play the slots";
            case ENCHANTER -> "Enchanting services";
            case COSMETICS -> "Browse cosmetics";
            case RECRUITER -> "Ask about the faction";
            case CUSTOM -> "Let's get to it";
            default -> "Continue";
        };
    }

    private static void customInteract(ServerPlayer sp, @Nullable Entity npc) {
        if (npc instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity n) {
            var handler = net.fugginbeenus.notchcurrency.api.NotchNpcApi.customRole(n.getCustomRoleId());
            if (handler != null) {
                handler.interact(sp, n);
                return;
            }
        }
        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("This NPC's job isn't installed on this server.").withStyle(ChatFormatting.GRAY));
    }

    private static void openPlayerShop(ServerPlayer sp, @Nullable Entity npc) {
        if (npc == null) return;
        PlayerShop shop = ShopState.get(sp.serverLevel()).getShopByNpc(npc.getUUID());
        if (shop == null) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("This shop hasn't been set up yet.").withStyle(ChatFormatting.YELLOW));
            return;
        }
        if (shop.getOwnerId().equals(sp.getUUID())) {
            NpcShopLogic.openShopManager(sp, shop.getShopId());
        } else {
            NpcShopLogic.openShopBrowser(sp, shop.getShopId());
        }
    }

    private static void greet(ServerPlayer sp, @Nullable Entity npc) {
        String name = (npc != null && npc.hasCustomName() && npc.getCustomName() != null)
                ? npc.getCustomName().getString() : "NPC";
        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("<" + name + "> Hello there!").withStyle(ChatFormatting.WHITE));
    }

    private static void openAtm(ServerPlayer sp) {
        sp.openMenu(new SimpleMenuProvider(
                (containerId, inv, p) -> new ATMTestScreenHandler(containerId, inv),
                Component.translatable("screen.notchcurrency.atm")));
    }

    private static void openAuction(ServerPlayer sp) {
        sp.openMenu(new SimpleMenuProvider(
                (containerId, inv, p) -> new AuctionHouseScreenHandler(containerId, inv),
                Component.literal("Auction House")));
    }
}
