package net.fugginbeenus.notchcurrency.client.npc;

import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

/**
 * Private model layers for the humanoid NPC: an identical copy of the vanilla player model registered
 * under our own name.
 *
 * <p>When the NPC borrowed the vanilla {@code EntityModelLayers.PLAYER} layer directly, CEM animation
 * packs (Fresh Animations / Fresh Moves, via OptiFine or Entity Model Features) replaced that layer's
 * model and ran their own animation over it, which stomped the NPC's configured pose, so "statue"
 * (and the breathe/lively idles) wouldn't hold. Those packs key their replacements to the vanilla layer
 * names, so rendering through our own identically-shaped layer keeps the NPC's poses ours regardless of
 * what animation packs are installed.
 */
public final class NpcModelLayers {

    public static final EntityModelLayer NPC_PLAYER =
            new EntityModelLayer(NotchCurrency.id("npc_player"), "main");
    public static final EntityModelLayer NPC_PLAYER_SLIM =
            new EntityModelLayer(NotchCurrency.id("npc_player_slim"), "main");

    private NpcModelLayers() {}

    public static void register() {
        EntityModelLayerRegistry.registerModelLayer(NPC_PLAYER,
                () -> TexturedModelData.of(PlayerEntityModel.getTexturedModelData(Dilation.NONE, false), 64, 64));
        EntityModelLayerRegistry.registerModelLayer(NPC_PLAYER_SLIM,
                () -> TexturedModelData.of(PlayerEntityModel.getTexturedModelData(Dilation.NONE, true), 64, 64));
    }
}
