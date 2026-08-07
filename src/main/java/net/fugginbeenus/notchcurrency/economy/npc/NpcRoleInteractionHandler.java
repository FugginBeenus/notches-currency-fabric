package net.fugginbeenus.notchcurrency.economy.npc;

import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.Nullable;

public final class NpcRoleInteractionHandler {

    private NpcRoleInteractionHandler() {}

    public static void register() {
        UseEntityCallback.EVENT.register(NpcRoleInteractionHandler::onUse);
    }

    private static InteractionResult onUse(Player player, Level world, InteractionHand hand,
                                      Entity entity, @Nullable EntityHitResult hit) {
        if (world.isClientSide() || hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        MinecraftServer server = sp.getServer();
        NpcRoleState.Assignment a = NpcRoleState.get(server).get(entity.getUUID());
        if (a == null) return InteractionResult.PASS;   // not a role NPC: let others handle it

        NpcRoleDispatch.open(sp, a.role(), a.shopId(), entity);
        return InteractionResult.SUCCESS;
    }
}
