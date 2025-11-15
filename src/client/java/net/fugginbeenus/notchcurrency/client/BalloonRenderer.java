package net.fugginbeenus.notchcurrency.client.render;

import net.minecraft.client.render.*;
import net.minecraft.client.render.block.BlockRenderManager;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.client.MinecraftClient;
import net.fugginbeenus.notchcurrency.crate.BalloonEntity;
import net.minecraft.block.Blocks;
import net.minecraft.client.render.OverlayTexture;

public class BalloonRenderer extends EntityRenderer<BalloonEntity> {

    public BalloonRenderer(EntityRendererFactory.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.1f;
    }

    @Override
    public void render(BalloonEntity entity, float yaw, float tickDelta,
                       MatrixStack matrices, VertexConsumerProvider consumers, int light) {
        matrices.push();

        // Slight scale so it looks like a small cube balloon
        matrices.scale(0.6f, 0.6f, 0.6f);
        matrices.translate(-0.5, 0.0, -0.5);

        var brm   = MinecraftClient.getInstance().getBlockRenderManager();
        var state = Blocks.RED_WOOL.getDefaultState();
        brm.renderBlockAsEntity(state, matrices, consumers, light, OverlayTexture.DEFAULT_UV);

        matrices.pop();
        super.render(entity, yaw, tickDelta, matrices, consumers, light);
    }

    private static final Identifier DUMMY = new Identifier("minecraft", "textures/block/red_wool.png");
    @Override public Identifier getTexture(BalloonEntity e) { return null; }
}
