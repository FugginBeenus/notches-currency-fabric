package net.fugginbeenus.notchcurrency.economy.npc;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

/**
 * Opens the right economy feature when a player interacts with an NPC that has been
 * bound to a {@link NpcRole} (via {@code /npc} or the NPC API). Any entity can carry a
 * role, so this works with APP.ly, EasyNPC, villagers, etc.
 */
public final class NpcRoleInteractionHandler {

    private NpcRoleInteractionHandler() {}

    public static void register() {
        UseEntityCallback.EVENT.register(NpcRoleInteractionHandler::onUse);
    }

    private static ActionResult onUse(PlayerEntity player, World world, Hand hand,
                                      Entity entity, @Nullable EntityHitResult hit) {
        if (world.isClient() || hand != Hand.MAIN_HAND) return ActionResult.PASS;
        if (!(player instanceof ServerPlayerEntity sp)) return ActionResult.PASS;

        MinecraftServer server = sp.getServer();
        NpcRoleState.Assignment a = NpcRoleState.get(server).get(entity.getUuid());
        if (a == null) return ActionResult.PASS;   // not a role NPC — let others handle it

        NpcRoleDispatch.open(sp, a.role(), a.shopId(), entity);
        return ActionResult.SUCCESS;
    }
}
