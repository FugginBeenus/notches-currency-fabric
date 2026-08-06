package net.fugginbeenus.notchcurrency.client.npc;

import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.client.model.Dilation;
import net.minecraft.client.model.TexturedModelData;
import net.minecraft.client.render.entity.model.EntityModelLayer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;

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
