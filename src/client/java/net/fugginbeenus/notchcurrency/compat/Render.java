package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.world.World;
import org.joml.Matrix4f;

public final class Render {

    private Render() {}

    public static void renderFixedItem(ItemRenderer itemRenderer, ItemStack stack, int light, int overlay,
                                       MatrixStack matrices, VertexConsumerProvider vcp, World world, int seed) {
        itemRenderer.renderItem(stack, ModelTransformationMode.FIXED, light, overlay, matrices, vcp, world, seed);
    }

    public static void drawText(TextRenderer text, Text str, float x, float y, int color,
                                Matrix4f matrix, VertexConsumerProvider vcp, int light) {
        text.draw(str, x, y, color, false, matrix, vcp, TextRenderer.TextLayerType.NORMAL, 0, light);
    }

    public static void drawEntityAt(net.minecraft.client.gui.DrawContext ctx, int x, int y, int size,
                                    float mouseX, float mouseY, net.minecraft.entity.LivingEntity entity) {
        //? if >=1.21 {
        /*float yawAngle = (float) Math.atan(mouseX / 40.0F);
        float pitchAngle = (float) Math.atan(mouseY / 40.0F);
        org.joml.Quaternionf flip = new org.joml.Quaternionf().rotateZ((float) Math.PI);
        org.joml.Quaternionf pitchRot = new org.joml.Quaternionf().rotateX(pitchAngle * 20.0F * ((float) Math.PI / 180.0F));
        flip.mul(pitchRot);
        float bodyYaw = entity.bodyYaw;
        float yaw = entity.getYaw();
        float pitch = entity.getPitch();
        float prevHeadYaw = entity.prevHeadYaw;
        float headYaw = entity.headYaw;
        entity.bodyYaw = 180.0F + yawAngle * 20.0F;
        entity.setYaw(180.0F + yawAngle * 40.0F);
        entity.setPitch(-pitchAngle * 20.0F);
        entity.headYaw = entity.getYaw();
        entity.prevHeadYaw = entity.getYaw();
        net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(
                ctx, x, y, size, new org.joml.Vector3f(), flip, pitchRot, entity);
        entity.bodyYaw = bodyYaw;
        entity.setYaw(yaw);
        entity.setPitch(pitch);
        entity.prevHeadYaw = prevHeadYaw;
        entity.headYaw = headYaw;
        *///?} else {
        net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(ctx, x, y, size, mouseX, mouseY, entity);
        //?}
    }
}
