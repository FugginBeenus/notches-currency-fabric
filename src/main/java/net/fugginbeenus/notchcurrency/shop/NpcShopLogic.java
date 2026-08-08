package net.fugginbeenus.notchcurrency.shop;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fugginbeenus.notchcurrency.core.CoinEconomy;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public final class NpcShopLogic {

    private NpcShopLogic() {}

    public static boolean performPurchase(ServerPlayer buyer,
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

        if (!buyer.getInventory().add(toGive)) {
            // Inventory full → drop at feet
            buyer.drop(toGive, false);
        }

        // 3) Feedback: ding + actionbar
        buyer.playSound(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F, 1.2F);
        net.fugginbeenus.notchcurrency.compat.Msg.actionBar(buyer, Component.literal("§aYou bought §e" + quantity + "x §f" + item.getHoverName().getString()
                        + " §afor §e" + totalCost + "§a " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + "."));

        return true;
    }

    public static void openShopBrowser(ServerPlayer player, UUID shopId) {
        ShopState state = ShopState.get(player.serverLevel());
        PlayerShop shop = state.getShop(shopId);

        if (shop == null) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("§cShop not found!"));
            return;
        }

        // Listings sync live through the handler's data-carrier slots; the buf only carries identity.
        net.fugginbeenus.notchcurrency.compat.Screens.openExtended(player, Component.literal(shop.getShopName()),
                (containerId, playerInventory, p) -> new ShopBrowseScreenHandler(containerId, playerInventory, shopId,
                        shop.getShopName(), shop.getShopkeeperDialog(), shop.getLinkedNpcId(), shop),
                buf -> {
                    buf.writeUUID(shopId);
                    buf.writeUtf(shop.getShopName());
                    buf.writeUtf(shop.getShopkeeperDialog());
                    writeNpcId(buf, shop.getLinkedNpcId());
                });
    }

    private static void writeNpcId(FriendlyByteBuf buf, @Nullable UUID npcId) {
        buf.writeBoolean(npcId != null);
        if (npcId != null) buf.writeUUID(npcId);
    }

    public static void openShopManager(ServerPlayer owner, UUID shopId) {
        ShopState state = ShopState.get(owner.serverLevel());
        PlayerShop shop = state.getShop(shopId);

        if (shop == null) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(owner, Component.literal("§cShop not found!"));
            return;
        }

        if (!shop.getOwnerId().equals(owner.getUUID())) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(owner, Component.literal("§cYou don't own this shop!"));
            return;
        }

        // Listings/earnings sync live through the handler; the buf only carries identity.
        net.fugginbeenus.notchcurrency.compat.Screens.openExtended(owner, Component.literal("Manage: " + shop.getShopName()),
                (containerId, playerInventory, p) -> new ShopManageScreenHandler(containerId, playerInventory, shopId,
                        shop.getShopName(), shop.getShopkeeperDialog(), shop.getLinkedNpcId(), shop),
                buf -> {
                    buf.writeUUID(shopId);
                    buf.writeUtf(shop.getShopName());
                    buf.writeUtf(shop.getShopkeeperDialog());
                    writeNpcId(buf, shop.getLinkedNpcId());
                });
    }

    public static void openShop(ServerPlayer player, UUID shopId) {
        ShopState state = ShopState.get(player.serverLevel());
        PlayerShop shop = state.getShop(shopId);

        if (shop == null) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("§cShop not found!"));
            return;
        }

        if (shop.getOwnerId().equals(player.getUUID())) {
            openShopManager(player, shopId);
        } else {
            openShopBrowser(player, shopId);
        }
    }

    public static boolean openShopFromNpc(ServerPlayer player, UUID npcId) {
        ShopState state = ShopState.get(player.serverLevel());
        PlayerShop shop = state.getShopByNpc(npcId);

        if (shop == null) {
            return false;
        }

        openShop(player, shop.getShopId());
        return true;
    }
}