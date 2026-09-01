package net.fugginbeenus.notchcurrency.shop;

import net.fugginbeenus.notchcurrency.economy.adminshop.AdminShop;
import net.fugginbeenus.notchcurrency.economy.adminshop.AdminShopEntry;
import net.fugginbeenus.notchcurrency.economy.adminshop.AdminShopState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public final class AdminShopMigration {

    private AdminShopMigration() {}

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency-ShopMigration");

    public static final UUID SERVER_OWNER = new UUID(0L, 0L);
    public static final String SERVER_OWNER_NAME = "Server";

    private static void repointNpcs(MinecraftServer server, UUID oldShopId, UUID newShopId) {
        var roles = net.fugginbeenus.notchcurrency.economy.npc.NpcRoleState.get(server);
        java.util.List<UUID> toMove = new java.util.ArrayList<>();
        for (var e : roles.all().entrySet()) {
            var a = e.getValue();
            if (a.role() == net.fugginbeenus.notchcurrency.economy.npc.NpcRole.ADMIN_SHOP
                    && oldShopId.equals(a.shopId())) {
                toMove.add(e.getKey());
            }
        }
        for (UUID npcId : toMove) {
            roles.assign(npcId, net.fugginbeenus.notchcurrency.economy.npc.NpcRole.SHOP, newShopId);
            LOGGER.info("Repointed NPC {} to the migrated shop", npcId);
        }
    }

    public static int run(MinecraftServer server, ServerLevel overworld) {
        AdminShopState old = AdminShopState.get(server);
        ShopState shops = ShopState.get(overworld);

        int moved = 0;
        for (AdminShop source : old.allShops()) {
            if (old.isMigrated(source.getId())) continue;

            PlayerShop target = new PlayerShop(SERVER_OWNER, SERVER_OWNER_NAME, source.getName());
            target.setAdminMode(true);

            for (AdminShopEntry entry : source.getEntries()) {
                ItemStack item = entry.getItem();
                if (item.isEmpty()) continue;
                ShopListing listing = new ShopListing(item.copy(), 0,
                        (int) Math.min(Integer.MAX_VALUE, entry.getBaseBuyPrice()));
                listing.setShopPaysPrice((int) Math.min(Integer.MAX_VALUE, entry.getBaseSellPrice()));
                listing.setDynamicPricing(entry.isDynamic());
                listing.setBuyLimitFrom(entry.getBuyLimit(), entry.getSellLimit(), entry.getResetMode());
                target.addListing(listing);
            }

            shops.adopt(target);
            repointNpcs(server, source.getId(), target.getShopId());
            old.markMigrated(source.getId());
            moved++;
            LOGGER.info("Moved admin shop '{}' ({} items) into the shop system", source.getName(),
                    source.getEntries().size());
        }

        if (moved > 0) {
            shops.setDirty();
            old.setDirty();
        }
        return moved;
    }
}
