package net.fugginbeenus.notchcurrency.client.entity;

import net.fugginbeenus.notchcurrency.entity.ShopkeeperEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.model.EntityModelPartNames;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Player-like model for shopkeeper NPCs.
 * Based on PlayerEntityModel but simplified.
 */
public class ShopkeeperModel extends EntityModel<ShopkeeperEntity> {

    private final ModelPart head;
    private final ModelPart hat;
    private final ModelPart body;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart rightLeg;
    private final ModelPart leftLeg;

    private final boolean slim;

    public ShopkeeperModel(ModelPart root, boolean slim) {
        this.slim = slim;
        this.head = root.getChild(EntityModelPartNames.HEAD);
        this.hat = root.getChild(EntityModelPartNames.HAT);
        this.body = root.getChild(EntityModelPartNames.BODY);
        this.rightArm = root.getChild(EntityModelPartNames.RIGHT_ARM);
        this.leftArm = root.getChild(EntityModelPartNames.LEFT_ARM);
        this.rightLeg = root.getChild(EntityModelPartNames.RIGHT_LEG);
        this.leftLeg = root.getChild(EntityModelPartNames.LEFT_LEG);
    }

    public static TexturedModelData getTexturedModelData(boolean slim) {
        ModelData modelData = new ModelData();
        ModelPartData modelPartData = modelData.getRoot();

        // Head
        modelPartData.addChild(EntityModelPartNames.HEAD,
                ModelPartBuilder.create().uv(0, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        // Hat (overlay)
        modelPartData.addChild(EntityModelPartNames.HAT,
                ModelPartBuilder.create().uv(32, 0).cuboid(-4.0F, -8.0F, -4.0F, 8.0F, 8.0F, 8.0F, new Dilation(0.5F)),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        // Body
        modelPartData.addChild(EntityModelPartNames.BODY,
                ModelPartBuilder.create().uv(16, 16).cuboid(-4.0F, 0.0F, -2.0F, 8.0F, 12.0F, 4.0F),
                ModelTransform.pivot(0.0F, 0.0F, 0.0F));

        // Arms
        if (slim) {
            // Slim arms (Alex model)
            modelPartData.addChild(EntityModelPartNames.RIGHT_ARM,
                    ModelPartBuilder.create().uv(40, 16).cuboid(-2.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F),
                    ModelTransform.pivot(-5.0F, 2.0F, 0.0F));

            modelPartData.addChild(EntityModelPartNames.LEFT_ARM,
                    ModelPartBuilder.create().uv(32, 48).cuboid(-1.0F, -2.0F, -2.0F, 3.0F, 12.0F, 4.0F),
                    ModelTransform.pivot(5.0F, 2.0F, 0.0F));
        } else {
            // Normal arms (Steve model)
            modelPartData.addChild(EntityModelPartNames.RIGHT_ARM,
                    ModelPartBuilder.create().uv(40, 16).cuboid(-3.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                    ModelTransform.pivot(-5.0F, 2.0F, 0.0F));

            modelPartData.addChild(EntityModelPartNames.LEFT_ARM,
                    ModelPartBuilder.create().uv(32, 48).cuboid(-1.0F, -2.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                    ModelTransform.pivot(5.0F, 2.0F, 0.0F));
        }

        // Legs
        modelPartData.addChild(EntityModelPartNames.RIGHT_LEG,
                ModelPartBuilder.create().uv(0, 16).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                ModelTransform.pivot(-1.9F, 12.0F, 0.0F));

        modelPartData.addChild(EntityModelPartNames.LEFT_LEG,
                ModelPartBuilder.create().uv(16, 48).cuboid(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F),
                ModelTransform.pivot(1.9F, 12.0F, 0.0F));

        return TexturedModelData.of(modelData, 64, 64);
    }

    @Override
    public void setAngles(ShopkeeperEntity entity, float limbAngle, float limbDistance, float animationProgress, float headYaw, float headPitch) {
        // Head rotation relative to body (body rotation is handled by LivingEntityRenderer)
        this.head.yaw = headYaw * 0.017453292F;
        this.head.pitch = headPitch * 0.017453292F;
        this.hat.yaw = this.head.yaw;
        this.hat.pitch = this.head.pitch;

        // Reset body rotation - renderer handles world-space rotation
        this.body.yaw = 0;
        this.body.pitch = 0;

        // Breathing animation - subtle chest movement
        float breathe = (float) Math.sin(animationProgress * 0.1F) * 0.01F;
        this.body.pitch = breathe;

        // Idle arm pose with natural hang
        this.rightArm.pitch = 0.05F + breathe * 0.3F;
        this.rightArm.yaw = 0.0F;
        this.rightArm.roll = 0.1F;

        this.leftArm.pitch = 0.05F + breathe * 0.3F;
        this.leftArm.yaw = 0.0F;
        this.leftArm.roll = -0.1F;

        // Subtle arm sway - slightly different frequencies for natural feel
        float swayRight = (float) Math.sin(animationProgress * 0.067F) * 0.02F;
        float swayLeft = (float) Math.sin(animationProgress * 0.073F) * 0.02F;
        this.rightArm.roll += swayRight;
        this.leftArm.roll -= swayLeft;

        // Very subtle arm swing forward/back
        float armSwing = (float) Math.sin(animationProgress * 0.05F) * 0.015F;
        this.rightArm.pitch += armSwing;
        this.leftArm.pitch -= armSwing * 0.8F;

        // Legs at rest
        this.rightLeg.pitch = 0;
        this.rightLeg.yaw = 0;
        this.leftLeg.pitch = 0;
        this.leftLeg.yaw = 0;

        // Occasional subtle head tilt for more life
        float headTilt = (float) Math.sin(animationProgress * 0.023F) * 0.015F;
        this.head.roll = headTilt;
        this.hat.roll = headTilt;
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumer vertices, int light, int overlay, float red, float green, float blue, float alpha) {
        this.head.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.hat.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.body.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.rightArm.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.leftArm.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.rightLeg.render(matrices, vertices, light, overlay, red, green, blue, alpha);
        this.leftLeg.render(matrices, vertices, light, overlay, red, green, blue, alpha);
    }
}