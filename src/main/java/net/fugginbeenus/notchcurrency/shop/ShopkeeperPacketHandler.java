package net.fugginbeenus.notchcurrency.shop;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/**
 * Handles server-side packets for shopkeeper settings updates.
 */
public class ShopkeeperPacketHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency");

    public static void register() {
        // Skin update
        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.SHOPKEEPER_UPDATE_SKIN, (server, player, handler, buf, responseSender) -> {
            UUID npcId = buf.readUuid();
            String skinType = buf.readString();
            String skinValue = buf.readString();

            server.execute(() -> handleSkinUpdate(player, npcId, skinType, skinValue));
        });

        // NPC name update
        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.SHOPKEEPER_UPDATE_NAME, (server, player, handler, buf, responseSender) -> {
            UUID npcId = buf.readUuid();
            String name = buf.readString();

            server.execute(() -> handleNpcNameUpdate(player, npcId, name));
        });

        // Shop name update
        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.SHOPKEEPER_UPDATE_SHOP_NAME, (server, player, handler, buf, responseSender) -> {
            UUID shopId = buf.readUuid();
            String name = buf.readString();

            server.execute(() -> handleShopNameUpdate(player, shopId, name));
        });

        // Dialog update
        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.SHOPKEEPER_UPDATE_DIALOG, (server, player, handler, buf, responseSender) -> {
            UUID npcId = buf.readUuid();
            String dialog = buf.readString();

            server.execute(() -> handleDialogUpdate(player, npcId, dialog));
        });

        // Open shop request
        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.SHOPKEEPER_OPEN_SHOP, (server, player, handler, buf, responseSender) -> {
            UUID shopId = buf.readUuid();

            server.execute(() -> handleOpenShopRequest(player, shopId));
        });

        // Delete NPC request
        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.SHOPKEEPER_DELETE_NPC, (server, player, handler, buf, responseSender) -> {
            UUID npcId = buf.readUuid();
            UUID shopId = buf.readUuid();

            server.execute(() -> handleDeleteNpc(player, npcId, shopId));
        });
    }

    private static void handleSkinUpdate(ServerPlayerEntity player, UUID npcId, String skinType, String skinValue) {
        // Verify ownership
        ShopState state = ShopState.get(player.getServerWorld());
        PlayerShop shop = state.getShopByNpc(npcId);

        if (shop == null || !shop.getOwnerId().equals(player.getUuid())) {
            player.sendMessage(Text.literal("❌ You don't own this shopkeeper!").formatted(Formatting.RED), false);
            return;
        }

        // Find the NPC entity
        Entity entity = findNpcEntity(player.getServerWorld(), npcId);
        if (!(entity instanceof net.fugginbeenus.notchcurrency.entity.ShopkeeperEntity npc)) {
            player.sendMessage(Text.literal("❌ Shopkeeper not found!").formatted(Formatting.RED), false);
            return;
        }

        // Apply skin to our ShopkeeperEntity
        try {
            if (skinType.equals("preset")) {
                // Preset skin - skinValue is like "1", "2", etc. or full path
                int presetNum = 1;
                try {
                    // Try parsing directly as number
                    presetNum = Integer.parseInt(skinValue);
                } catch (NumberFormatException e) {
                    // Try extracting number from path like "textures/skins/preset_1.png"
                    if (skinValue.contains("preset_")) {
                        String num = skinValue.replaceAll(".*preset_(\\d+).*", "$1");
                        try {
                            presetNum = Integer.parseInt(num);
                        } catch (NumberFormatException e2) {
                            presetNum = 1;
                        }
                    }
                }
                npc.setPresetSkin(presetNum);
                player.sendMessage(Text.literal("✓ Skin updated to preset " + presetNum + "!").formatted(Formatting.GREEN), false);

            } else if (skinType.equals("player")) {
                // Player skin by username
                npc.setPlayerSkin(skinValue);
                player.sendMessage(Text.literal("✓ Skin updated to " + skinValue + "'s skin!").formatted(Formatting.GREEN), false);

            } else if (skinType.equals("url") || skinType.equals("custom_url")) {
                // Custom URL skin
                npc.setUrlSkin(skinValue);
                player.sendMessage(Text.literal("✓ Skin updated from URL!").formatted(Formatting.GREEN), false);

            } else {
                // Default - treat as player name
                npc.setPlayerSkin(skinValue);
                player.sendMessage(Text.literal("✓ Skin updated!").formatted(Formatting.GREEN), false);
            }

            LOGGER.info("Updated shopkeeper skin: type={}, value={}", skinType, skinValue);

        } catch (Exception e) {
            player.sendMessage(Text.literal("❌ Skin update failed!").formatted(Formatting.RED), false);
            LOGGER.error("Skin update error:", e);
        }
    }

    private static void handleNpcNameUpdate(ServerPlayerEntity player, UUID npcId, String name) {
        ShopState state = ShopState.get(player.getServerWorld());
        PlayerShop shop = state.getShopByNpc(npcId);

        if (shop == null || !shop.getOwnerId().equals(player.getUuid())) {
            player.sendMessage(Text.literal("You don't own this shopkeeper!").formatted(Formatting.RED), false);
            return;
        }

        Entity npc = findNpcEntity(player.getServerWorld(), npcId);
        if (npc == null) {
            player.sendMessage(Text.literal("Shopkeeper not found!").formatted(Formatting.RED), false);
            return;
        }

        // Set custom name on entity
        npc.setCustomName(Text.literal(name));
        npc.setCustomNameVisible(true);

        player.sendMessage(Text.literal("Shopkeeper name updated!").formatted(Formatting.GREEN), false);
    }

    private static void handleShopNameUpdate(ServerPlayerEntity player, UUID shopId, String name) {
        ShopState state = ShopState.get(player.getServerWorld());
        PlayerShop shop = state.getShop(shopId);

        if (shop == null || !shop.getOwnerId().equals(player.getUuid())) {
            player.sendMessage(Text.literal("You don't own this shop!").formatted(Formatting.RED), false);
            return;
        }

        shop.setShopName(name);
        state.markDirty();

        player.sendMessage(Text.literal("Shop name updated to: " + name).formatted(Formatting.GREEN), false);
    }

    private static void handleDialogUpdate(ServerPlayerEntity player, UUID npcId, String dialog) {
        ShopState state = ShopState.get(player.getServerWorld());
        PlayerShop shop = state.getShopByNpc(npcId);

        if (shop == null || !shop.getOwnerId().equals(player.getUuid())) {
            player.sendMessage(Text.literal("You don't own this shopkeeper!").formatted(Formatting.RED), false);
            return;
        }

        // Save dialog to the shop (this persists)
        shop.setShopkeeperDialog(dialog);
        state.markDirty();

        // Also try to update EasyNPC's dialog if possible
        Entity npc = findNpcEntity(player.getServerWorld(), npcId);
        if (npc != null) {
            try {
                NbtCompound nbt = new NbtCompound();
                npc.writeNbt(nbt);

                // EasyNPC dialog structure - may need adjustment based on actual EasyNPC version
                NbtCompound dialogData = nbt.getCompound("DialogData");
                dialogData.putString("Greeting", dialog);
                nbt.put("DialogData", dialogData);

                npc.readNbt(nbt);
            } catch (Exception e) {
                // EasyNPC integration not working, but dialog is saved to shop
            }
        }

        player.sendMessage(Text.literal("Shopkeeper greeting saved!").formatted(Formatting.GREEN), false);
    }

    private static void handleOpenShopRequest(ServerPlayerEntity player, UUID shopId) {
        ShopState state = ShopState.get(player.getServerWorld());
        PlayerShop shop = state.getShop(shopId);

        if (shop == null) {
            player.sendMessage(Text.literal("Shop not found!").formatted(Formatting.RED), false);
            return;
        }

        // Open shop in appropriate mode
        if (shop.getOwnerId().equals(player.getUuid())) {
            NpcShopLogic.openShopManager(player, shopId);
        } else {
            NpcShopLogic.openShopBrowser(player, shopId);
        }
    }

    /**
     * Find an NPC entity by UUID in the world.
     */
    private static Entity findNpcEntity(ServerWorld world, UUID npcId) {
        return world.getEntity(npcId);
    }

    /**
     * Handle request to delete an NPC and its associated shop.
     * Returns all items, stock, and currency to the owner.
     */
    private static void handleDeleteNpc(ServerPlayerEntity player, UUID npcId, UUID shopId) {
        ShopState state = ShopState.get(player.getServerWorld());
        PlayerShop shop = state.getShop(shopId);

        // Verify ownership
        if (shop == null || !shop.getOwnerId().equals(player.getUuid())) {
            player.sendMessage(Text.literal("You don't own this shopkeeper!").formatted(Formatting.RED), false);
            return;
        }

        // Collect all items and currency to return to owner
        int totalCurrency = 0;
        List<ItemStack> itemsToReturn = new java.util.ArrayList<>();

        // 1. Collect pending balance (unclaimed earnings)
        totalCurrency += (int) shop.getPendingBalance();

        // 2. Collect pending sale earnings
        totalCurrency += shop.collectPendingEarnings();

        // 3. Collect pending barter items
        itemsToReturn.addAll(shop.collectPendingBarterItems());

        // 4. Collect all stock from listings
        for (ShopListing listing : shop.getListings()) {
            int stock = listing.getStockQuantity();
            if (stock > 0) {
                ItemStack baseItem = listing.getItemForSale();
                // Create stacks of the item (respecting max stack size)
                int maxStackSize = baseItem.getMaxCount();
                while (stock > 0) {
                    int stackSize = Math.min(stock, maxStackSize);
                    ItemStack returnStack = baseItem.copy();
                    returnStack.setCount(stackSize);
                    itemsToReturn.add(returnStack);
                    stock -= stackSize;
                }
            }
        }

        // Give currency to player's balance
        if (totalCurrency > 0) {
            net.fugginbeenus.notchcurrency.core.BalanceStore.add(player, totalCurrency);
            net.fugginbeenus.notchcurrency.net.NotchPackets.sendBalance(player,
                    net.fugginbeenus.notchcurrency.core.BalanceStore.get(player));
        }

        // Give items to player (drop on ground if inventory full)
        int itemsReturned = 0;
        for (ItemStack item : itemsToReturn) {
            if (!item.isEmpty()) {
                if (!player.getInventory().insertStack(item.copy())) {
                    // Inventory full - drop on ground
                    player.dropItem(item.copy(), false);
                }
                itemsReturned += item.getCount();
            }
        }

        // Find and remove the NPC entity
        Entity npc = findNpcEntity(player.getServerWorld(), npcId);
        if (npc != null) {
            npc.discard(); // Remove the entity from the world
            LOGGER.info("Deleted NPC {} for player {}", npcId, player.getName().getString());
        }

        // Remove the shop from state
        state.removeShop(shopId);
        state.markDirtyAndSave();

        // Build detailed message
        StringBuilder msg = new StringBuilder("Shopkeeper deleted!");
        if (totalCurrency > 0 || itemsReturned > 0) {
            msg.append(" Returned: ");
            if (totalCurrency > 0) {
                msg.append(totalCurrency).append(" coins");
            }
            if (totalCurrency > 0 && itemsReturned > 0) {
                msg.append(" and ");
            }
            if (itemsReturned > 0) {
                msg.append(itemsReturned).append(" items");
            }
            msg.append(".");
        }

        player.sendMessage(Text.literal(msg.toString()).formatted(Formatting.GREEN), false);
        LOGGER.info("Player {} deleted shop {}: returned {} coins and {} items",
                player.getName().getString(), shopId, totalCurrency, itemsReturned);
    }
}