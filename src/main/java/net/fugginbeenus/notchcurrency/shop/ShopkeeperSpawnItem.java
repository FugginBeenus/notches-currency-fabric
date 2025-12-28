package net.fugginbeenus.notchcurrency.shop;

import net.fugginbeenus.notchcurrency.entity.ShopkeeperEntity;
import net.fugginbeenus.notchcurrency.registry.ModEntities;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Random;

/**
 * Shopkeeper Spawn Egg - Spawns our custom ShopkeeperEntity.
 */
public class ShopkeeperSpawnItem extends Item {

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency");
    private static final Random RANDOM = new Random();

    public ShopkeeperSpawnItem(Settings settings) {
        super(settings.maxCount(16));
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();

        if (world.isClient()) {
            return ActionResult.SUCCESS;
        }

        ServerWorld serverWorld = (ServerWorld) world;
        BlockPos clickedPos = context.getBlockPos();
        Direction side = context.getSide();
        BlockPos spawnPos = clickedPos.offset(side);

        try {
            // Spawn our custom ShopkeeperEntity
            ShopkeeperEntity npc = new ShopkeeperEntity(ModEntities.SHOPKEEPER, serverWorld);
            npc.setPosition(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);

            // Set a default name
            npc.setCustomName(Text.literal("Shopkeeper"));
            npc.setCustomNameVisible(true);

            // Set a random preset skin (1-12)
            int randomPreset = RANDOM.nextInt(12) + 1;
            npc.setPresetSkin(randomPreset);

            // Set owner if player is known
            if (context.getPlayer() != null) {
                npc.setOwnerUuid(context.getPlayer().getUuid());
            }

            // Spawn the entity
            serverWorld.spawnEntity(npc);

            // Consume the item
            if (context.getPlayer() != null && !context.getPlayer().getAbilities().creativeMode) {
                context.getStack().decrement(1);
            }

            if (context.getPlayer() != null) {
                context.getPlayer().sendMessage(
                        Text.literal("Shopkeeper spawned! Use a ").formatted(Formatting.GREEN)
                                .append(Text.literal("Merchant License").formatted(Formatting.GOLD))
                                .append(Text.literal(" to claim them.").formatted(Formatting.GREEN)),
                        false
                );
            }

            LOGGER.info("Spawned shopkeeper NPC at {} with preset skin {}", spawnPos, randomPreset);
            return ActionResult.CONSUME;

        } catch (Exception e) {
            LOGGER.error("Error spawning shopkeeper NPC", e);
            if (context.getPlayer() != null) {
                context.getPlayer().sendMessage(
                        Text.literal("Error spawning shopkeeper!").formatted(Formatting.RED),
                        false
                );
            }
            return ActionResult.FAIL;
        }
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("Spawns a humanoid NPC that can").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("become your shopkeeper.").formatted(Formatting.GRAY));
        tooltip.add(Text.literal(""));
        tooltip.add(Text.literal("1. Place this to spawn an NPC").formatted(Formatting.YELLOW));
        tooltip.add(Text.literal("2. Use a Merchant License on them").formatted(Formatting.YELLOW));
        tooltip.add(Text.literal("3. Configure your shop!").formatted(Formatting.YELLOW));
        super.appendTooltip(stack, world, tooltip, context);
    }
}