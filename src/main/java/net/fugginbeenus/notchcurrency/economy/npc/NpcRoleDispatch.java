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
import net.minecraft.entity.Entity;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The single place that turns a {@link NpcRole} into an opened economy feature. Shared by the built-in
 * {@code NotchNpcEntity} (its role lives on the entity) and by {@link NpcRoleInteractionHandler} (roles
 * bound to external entities via the API), so both paths behave identically.
 */
public final class NpcRoleDispatch {

    private NpcRoleDispatch() {}

    /** Open the feature for {@code role}. {@code target} is the ADMIN_SHOP shop id; {@code npc} is the
     *  NPC entity (needed for the SHOP role's per-NPC shop lookup). */
    public static void open(ServerPlayerEntity sp, NpcRole role, @Nullable UUID target, @Nullable Entity npc) {
        if (role == null) role = NpcRole.NONE;
        MinecraftServer server = sp.getServer();
        switch (role) {
            case ADMIN_SHOP -> {
                AdminShop shop = target != null ? AdminShopState.get(server).get(target) : null;
                if (shop == null) {
                    sp.sendMessage(Text.literal("This shop NPC isn't linked to a valid shop.").formatted(Formatting.RED), false);
                } else {
                    AdminShopMenu.sendListing(sp, shop);
                }
            }
            case BANKER -> openAtm(sp);
            case AUCTIONEER -> openAuction(sp);
            case MAILBOX -> {
                ServerWorld w = sp.getServerWorld();
                AuctionState.get(w).claimAll(w, sp);
            }
            case RAFFLE -> RaffleManager.openScreen(sp);
            case BOUNTY -> BountyManager.openScreen(sp);
            case DEALER -> SlotMachineManager.openScreen(sp);
            case SHOP -> openPlayerShop(sp, npc);
            case GREETER -> greet(sp, npc);
            case ENCHANTER -> net.fugginbeenus.notchcurrency.economy.enchanter.EnchanterManager.openScreen(sp);
            case COSMETICS -> net.fugginbeenus.notchcurrency.economy.cosmetic.CosmeticManager.openScreen(sp,
                    npc != null ? npc.getUuid() : null);
            case RECRUITER -> {
                if (npc instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity n) {
                    net.fugginbeenus.notchcurrency.npc.faction.RecruiterManager.open(sp, n);
                }
            }
            case CUSTOM -> customInteract(sp, npc);
            // NONE is a real choice, not a gap: guards, greeters and villagers-with-dialogue all live
            // here. Saying nothing is the point — anything else nags every time they're talked to.
            case NONE -> { }
        }
    }

    /** The label for the synthetic "enter my shop" choice appended to a role NPC's dialogue window. */
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

    /** CUSTOM role: hand the interaction to the handler another mod registered via the API. */
    private static void customInteract(ServerPlayerEntity sp, @Nullable Entity npc) {
        if (npc instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity n) {
            var handler = net.fugginbeenus.notchcurrency.api.NotchNpcApi.customRole(n.getCustomRoleId());
            if (handler != null) {
                handler.interact(sp, n);
                return;
            }
        }
        sp.sendMessage(Text.literal("This NPC's job isn't installed on this server.").formatted(Formatting.GRAY), false);
    }

    private static void openPlayerShop(ServerPlayerEntity sp, @Nullable Entity npc) {
        if (npc == null) return;
        PlayerShop shop = ShopState.get(sp.getServerWorld()).getShopByNpc(npc.getUuid());
        if (shop == null) {
            sp.sendMessage(Text.literal("This shop hasn't been set up yet.").formatted(Formatting.YELLOW), false);
            return;
        }
        if (shop.getOwnerId().equals(sp.getUuid())) {
            NpcShopLogic.openShopManager(sp, shop.getShopId());
        } else {
            NpcShopLogic.openShopBrowser(sp, shop.getShopId());
        }
    }

    private static void greet(ServerPlayerEntity sp, @Nullable Entity npc) {
        String name = (npc != null && npc.hasCustomName() && npc.getCustomName() != null)
                ? npc.getCustomName().getString() : "NPC";
        sp.sendMessage(Text.literal("<" + name + "> Hello there!").formatted(Formatting.WHITE), false);
    }

    private static void openAtm(ServerPlayerEntity sp) {
        sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new ATMTestScreenHandler(syncId, inv),
                Text.translatable("screen.notchcurrency.atm")));
    }

    private static void openAuction(ServerPlayerEntity sp) {
        sp.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, p) -> new AuctionHouseScreenHandler(syncId, inv),
                Text.literal("Auction House")));
    }
}
