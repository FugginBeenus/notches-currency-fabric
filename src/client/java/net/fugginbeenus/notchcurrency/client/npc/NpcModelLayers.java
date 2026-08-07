package net.fugginbeenus.notchcurrency.client.npc;

import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.PlayerModel;
import net.fabricmc.fabric.api.client.rendering.v1.EntityModelLayerRegistry;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.client.model.geom.ModelLayerLocation;

public final class NpcModelLayers {

    public static final ModelLayerLocation NPC_PLAYER =
            new ModelLayerLocation(NotchCurrency.id("npc_player"), "main");
    public static final ModelLayerLocation NPC_PLAYER_SLIM =
            new ModelLayerLocation(NotchCurrency.id("npc_player_slim"), "main");

    private NpcModelLayers() {}

    public static void register() {
        EntityModelLayerRegistry.registerModelLayer(NPC_PLAYER,
                () -> LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64));
        EntityModelLayerRegistry.registerModelLayer(NPC_PLAYER_SLIM,
                () -> LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, true), 64, 64));
    }
}
