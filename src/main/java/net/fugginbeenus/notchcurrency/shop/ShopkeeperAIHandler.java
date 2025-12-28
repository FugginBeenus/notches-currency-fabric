package net.fugginbeenus.notchcurrency.shop;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fugginbeenus.notchcurrency.entity.ShopkeeperEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

/**
 * Handles AI behavior for shopkeeper NPCs.
 * Makes shopkeepers look at nearby players.
 */
public class ShopkeeperAIHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger("NotchCurrency");

    // Update every 2 ticks for smooth rotation (was 20 - way too slow!)
    private static final int UPDATE_INTERVAL = 2;
    private static final double LOOK_RANGE = 8.0;
    private static final double LOOK_RANGE_SQ = LOOK_RANGE * LOOK_RANGE;
    private static final float SMOOTH_FACTOR = 0.15f; // Smoother interpolation

    private static int tickCounter = 0;

    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(ShopkeeperAIHandler::onWorldTick);
        LOGGER.info("Shopkeeper AI handler registered");
    }

    private static void onWorldTick(ServerWorld world) {
        tickCounter++;
        if (tickCounter % UPDATE_INTERVAL != 0) return;
        if (world.getPlayers().isEmpty()) return;

        ShopState state = ShopState.get(world);
        if (state == null) return;

        var allShops = state.getAllShops();
        if (allShops.isEmpty()) return;

        for (PlayerShop shop : allShops) {
            UUID npcId = shop.getLinkedNpcId();
            if (npcId == null) continue;

            Entity entity = world.getEntity(npcId);
            if (!(entity instanceof ShopkeeperEntity npc)) continue;

            PlayerEntity nearest = findNearestPlayer(world, npc);
            if (nearest != null) {
                makeNpcLookAt(npc, nearest);
            }
        }
    }

    private static PlayerEntity findNearestPlayer(ServerWorld world, Entity entity) {
        PlayerEntity nearest = null;
        double nearestDistSq = LOOK_RANGE_SQ;

        for (PlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) continue;
            double distSq = entity.squaredDistanceTo(player);
            if (distSq < nearestDistSq) {
                nearestDistSq = distSq;
                nearest = player;
            }
        }
        return nearest;
    }

    private static void makeNpcLookAt(ShopkeeperEntity npc, PlayerEntity target) {
        Vec3d npcPos = npc.getPos().add(0, npc.getStandingEyeHeight(), 0);
        Vec3d targetPos = target.getPos().add(0, target.getStandingEyeHeight(), 0);
        Vec3d direction = targetPos.subtract(npcPos);

        double horizontalDist = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
        float targetYaw = (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
        float targetPitch = (float) Math.toDegrees(-Math.atan2(direction.y, horizontalDist));
        targetPitch = Math.max(-90, Math.min(90, targetPitch));

        float currentHeadYaw = npc.getHeadYaw();
        float yawDiff = targetYaw - currentHeadYaw;
        while (yawDiff > 180) yawDiff -= 360;
        while (yawDiff < -180) yawDiff += 360;
        float newHeadYaw = currentHeadYaw + yawDiff * SMOOTH_FACTOR;

        npc.setHeadYaw(newHeadYaw);
        npc.setBodyYaw(newHeadYaw);
        npc.setPitch(targetPitch);
    }
}