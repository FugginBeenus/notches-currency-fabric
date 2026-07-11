package net.fugginbeenus.notchcurrency.shop;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fugginbeenus.notchcurrency.core.CoinEconomy;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Server-side logic for NPC shops and player shops.
 *
 * Provides helper methods for:
 * - Legacy NPC shop purchases (single item transactions)
 * - Opening player shop GUIs
 * - NPC-to-shop linking
 */
public final class NpcShopLogic {

    private NpcShopLogic() {}

    /**
     * Perform a simple purchase (legacy API for basic NPC shops).
     *
     * @param buyer    the player buying
     * @param price    price per item in coins
     * @param item     item being sold (template stack)
     * @param quantity how many items to buy
     * @return true if success, false otherwise
     */
    public static boolean performPurchase(ServerPlayerEntity buyer,
                                          int price,
                                          ItemStack item,
                                          int quantity) {
        if (buyer == null || item == null || item.isEmpty()) {
            return false;
        }
        if (price < 0 || quantity <= 0) {
            return false;
        }

        int totalCost = price * quantity;

        // 1) Charge the buyer
        if (!CoinEconomy.tryCharge(buyer, totalCost, false)) {
            // tryCharge already sent a "not enough coins" message
            return false;
        }

        // 2) Give items
        ItemStack toGive = item.copy();
        toGive.setCount(quantity);

        if (!buyer.getInventory().insertStack(toGive)) {
            // Inventory full → drop at feet
            buyer.dropItem(toGive, false);
        }

        // 3) Feedback: ding + actionbar
        buyer.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0F, 1.2F);
        buyer.sendMessage(
                Text.literal("§aYou bought §e" + quantity + "x §f" + item.getName().getString()
                        + " §afor §e" + totalCost + "§a " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + "."),
                true
        );

        return true;
    }

    /**
     * Open a player shop GUI for browsing/purchasing.
     */
    public static void openShopBrowser(ServerPlayerEntity player, UUID shopId) {
        ShopState state = ShopState.get(player.getServerWorld());
        PlayerShop shop = state.getShop(shopId);

        if (shop == null) {
            player.sendMessage(Text.literal("§cShop not found!"), false);
            return;
        }

        // Listings sync live through the handler's data-carrier slots; the buf only carries identity.
        player.openHandledScreen(new ExtendedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return Text.literal(shop.getShopName());
            }

            @Nullable
            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity p) {
                return new ShopBrowseScreenHandler(syncId, playerInventory, shopId,
                        shop.getShopName(), shop.getShopkeeperDialog(), shop.getLinkedNpcId(), shop);
            }

            @Override
            public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
                buf.writeUuid(shopId);
                buf.writeString(shop.getShopName());
                buf.writeString(shop.getShopkeeperDialog());
                writeNpcId(buf, shop.getLinkedNpcId());
            }
        });
    }

    /** Append the linked NPC uuid (for the shop screen's live preview), or a "no npc" marker. */
    private static void writeNpcId(PacketByteBuf buf, @Nullable UUID npcId) {
        buf.writeBoolean(npcId != null);
        if (npcId != null) buf.writeUuid(npcId);
    }

    /**
     * Open a player shop GUI for owner management.
     */
    public static void openShopManager(ServerPlayerEntity owner, UUID shopId) {
        ShopState state = ShopState.get(owner.getServerWorld());
        PlayerShop shop = state.getShop(shopId);

        if (shop == null) {
            owner.sendMessage(Text.literal("§cShop not found!"), false);
            return;
        }

        if (!shop.getOwnerId().equals(owner.getUuid())) {
            owner.sendMessage(Text.literal("§cYou don't own this shop!"), false);
            return;
        }

        // Listings/earnings sync live through the handler; the buf only carries identity.
        owner.openHandledScreen(new ExtendedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return Text.literal("Manage: " + shop.getShopName());
            }

            @Nullable
            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity p) {
                return new ShopManageScreenHandler(syncId, playerInventory, shopId,
                        shop.getShopName(), shop.getShopkeeperDialog(), shop.getLinkedNpcId(), shop);
            }

            @Override
            public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
                buf.writeUuid(shopId);
                buf.writeString(shop.getShopName());
                buf.writeString(shop.getShopkeeperDialog());
                writeNpcId(buf, shop.getLinkedNpcId());
            }
        });
    }

    /**
     * Open the appropriate shop GUI based on whether the player is the owner.
     */
    public static void openShop(ServerPlayerEntity player, UUID shopId) {
        ShopState state = ShopState.get(player.getServerWorld());
        PlayerShop shop = state.getShop(shopId);

        if (shop == null) {
            player.sendMessage(Text.literal("§cShop not found!"), false);
            return;
        }

        if (shop.getOwnerId().equals(player.getUuid())) {
            openShopManager(player, shopId);
        } else {
            openShopBrowser(player, shopId);
        }
    }

    /**
     * Try to open a shop linked to an NPC.
     * @return true if a shop was found and opened
     */
    public static boolean openShopFromNpc(ServerPlayerEntity player, UUID npcId) {
        ShopState state = ShopState.get(player.getServerWorld());
        PlayerShop shop = state.getShopByNpc(npcId);

        if (shop == null) {
            return false;
        }

        openShop(player, shop.getShopId());
        return true;
    }
}