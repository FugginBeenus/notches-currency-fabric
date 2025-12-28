package net.fugginbeenus.notchcurrency.shop;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fugginbeenus.notchcurrency.entity.ShopkeeperEntity;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Handles player interactions with ShopkeeperEntity NPCs.
 *
 * Interaction priority:
 * 1. Merchant License + Shopkeeper entity - Link NPC to new shop
 * 2. Linked shopkeeper + Owner sneaking - Open Shopkeeper Settings
 * 3. Linked shopkeeper + Owner - Open Shop Manage GUI
 * 4. Linked shopkeeper + Non-owner - Open Shop Browse GUI
 */
public class ShopkeeperInteractionHandler {

    public static void register() {
        UseEntityCallback.EVENT.register(ShopkeeperInteractionHandler::onUseEntity);
    }

    private static ActionResult onUseEntity(PlayerEntity player, World world, Hand hand, Entity entity, @Nullable EntityHitResult hitResult) {
        // Only handle main hand interactions on server
        if (world.isClient() || hand != Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }

        // Check if this is our ShopkeeperEntity
        if (!(entity instanceof ShopkeeperEntity)) {
            return ActionResult.PASS;
        }

        ItemStack heldItem = player.getStackInHand(hand);
        ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
        ServerWorld serverWorld = (ServerWorld) world;

        // 1. Merchant License - link NPC to new shop
        if (heldItem.isOf(ModItems.MERCHANT_LICENSE)) {
            return handleMerchantLicense(serverPlayer, serverWorld, entity, heldItem);
        }

        // 2-4. Check if this NPC is linked to a shop
        UUID npcId = entity.getUuid();
        ShopState state = ShopState.get(serverWorld);
        PlayerShop linkedShop = state.getShopByNpc(npcId);

        if (linkedShop == null) {
            // Not a shopkeeper yet - tell them to use a license
            serverPlayer.sendMessage(Text.literal("Use a Merchant License to claim this NPC as your shopkeeper!")
                    .formatted(Formatting.YELLOW), false);
            return ActionResult.SUCCESS;
        }

        // This is a linked shopkeeper - handle our interactions
        boolean isOwner = linkedShop.getOwnerId().equals(player.getUuid());

        if (isOwner && player.isSneaking()) {
            // Owner sneaking - open Shopkeeper Settings
            openShopkeeperSettings(serverPlayer, linkedShop, entity);
            return ActionResult.SUCCESS;
        } else if (isOwner) {
            // Owner not sneaking - open Shop Manage GUI
            NpcShopLogic.openShopManager(serverPlayer, linkedShop.getShopId());
            return ActionResult.SUCCESS;
        } else {
            // Non-owner - show greeting if set, then open Shop Browse GUI
            String dialog = linkedShop.getShopkeeperDialog();
            if (dialog != null && !dialog.isEmpty()) {
                // Display one random line from the shopkeeper's dialog
                String npcName = entity.hasCustomName() ? entity.getCustomName().getString() : "Shopkeeper";

                // Split by newlines and pick one randomly
                String[] lines = dialog.split("\n");
                String selectedLine;
                if (lines.length > 1) {
                    selectedLine = lines[serverPlayer.getRandom().nextInt(lines.length)].trim();
                } else {
                    selectedLine = dialog.trim();
                }

                if (!selectedLine.isEmpty()) {
                    serverPlayer.sendMessage(Text.literal("<" + npcName + "> " + selectedLine).formatted(Formatting.WHITE), false);
                }
            }
            NpcShopLogic.openShopBrowser(serverPlayer, linkedShop.getShopId());
            return ActionResult.SUCCESS;
        }
    }

    /**
     * Handle using Merchant License on a ShopkeeperEntity
     */
    private static ActionResult handleMerchantLicense(ServerPlayerEntity player, ServerWorld world, Entity npc, ItemStack license) {
        UUID npcId = npc.getUuid();
        ShopState state = ShopState.get(world);

        // Check if NPC is already linked to a shop
        PlayerShop existingShop = state.getShopByNpc(npcId);
        if (existingShop != null) {
            if (existingShop.getOwnerId().equals(player.getUuid())) {
                player.sendMessage(Text.literal("This NPC is already your shopkeeper!").formatted(Formatting.YELLOW), false);
            } else {
                player.sendMessage(Text.literal("This NPC is already someone else's shopkeeper!").formatted(Formatting.RED), false);
            }
            return ActionResult.FAIL;
        }

        // Create new shop
        String shopName = player.getName().getString() + "'s Shop";
        PlayerShop newShop = new PlayerShop(player.getUuid(), player.getName().getString(), shopName);
        newShop.setLinkedNpcId(npcId);

        // Register the shop
        state.addShop(newShop);
        state.markDirtyAndSave();

        // Consume the license
        if (!player.isCreative()) {
            license.decrement(1);
        }

        // === EFFECTS: Particles and Sound ===
        double x = npc.getX();
        double y = npc.getY() + 1.0;
        double z = npc.getZ();

        // Happy villager particles (green sparkles) - spiral upward
        for (int i = 0; i < 30; i++) {
            double angle = (i / 30.0) * Math.PI * 4;
            double radius = 0.5 + (i / 30.0) * 0.3;
            double px = x + Math.cos(angle) * radius;
            double py = y + (i / 30.0) * 1.5;
            double pz = z + Math.sin(angle) * radius;
            world.spawnParticles(net.minecraft.particle.ParticleTypes.HAPPY_VILLAGER, px, py, pz, 1, 0.1, 0.1, 0.1, 0);
        }

        // Gold/coin particles
        world.spawnParticles(net.minecraft.particle.ParticleTypes.TOTEM_OF_UNDYING, x, y + 0.5, z, 20, 0.3, 0.5, 0.3, 0.1);

        // Enchant particles
        world.spawnParticles(net.minecraft.particle.ParticleTypes.ENCHANT, x, y + 1.5, z, 15, 0.5, 0.3, 0.5, 0.5);

        // Success sounds
        player.playSound(net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP, net.minecraft.sound.SoundCategory.PLAYERS, 0.7F, 1.2F);
        world.playSound(null, npc.getBlockPos(), net.minecraft.sound.SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, net.minecraft.sound.SoundCategory.BLOCKS, 1.0F, 1.0F);

        // Success message
        player.sendMessage(Text.literal("Shopkeeper created! ").formatted(Formatting.GREEN)
                .append(Text.literal("Sneak + right-click").formatted(Formatting.YELLOW))
                .append(Text.literal(" to customize, or ").formatted(Formatting.GREEN))
                .append(Text.literal("right-click").formatted(Formatting.YELLOW))
                .append(Text.literal(" to manage your shop.").formatted(Formatting.GREEN)), false);

        return ActionResult.SUCCESS;
    }

    /**
     * Open the Shopkeeper Settings GUI
     */
    private static void openShopkeeperSettings(ServerPlayerEntity player, PlayerShop shop, Entity npc) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(shop.getShopId());
        buf.writeUuid(npc.getUuid());
        buf.writeString(shop.getShopName());
        buf.writeString(shop.getOwnerName());
        buf.writeString(shop.getShopkeeperDialog());

        ServerPlayNetworking.send(player, NotchPackets.SHOPKEEPER_SETTINGS_OPEN, buf);
    }
}