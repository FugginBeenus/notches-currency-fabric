package net.fugginbeenus.notchcurrency.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;

public final class Render {

    private Render() {}

    public static void renderFixedItem(ItemRenderer itemRenderer, ItemStack stack, int light, int overlay,
                                       PoseStack matrices, MultiBufferSource vcp, Level world, int seed) {
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, matrices, vcp, world, seed);
    }

    public static void drawText(Font text, Component str, float x, float y, int color,
                                Matrix4f matrix, MultiBufferSource vcp, int light) {
        text.drawInBatch(str, x, y, color, false, matrix, vcp, Font.DisplayMode.NORMAL, 0, light);
    }

    public static void drawEntityAt(net.minecraft.client.gui.GuiGraphics ctx, int x, int y, int size,
                                    float mouseX, float mouseY, net.minecraft.world.entity.LivingEntity entity) {
        //? if >=1.21 {
        /*float yawAngle = (float) Math.atan(mouseX / 40.0F);
        float pitchAngle = (float) Math.atan(mouseY / 40.0F);
        org.joml.Quaternionf flip = new org.joml.Quaternionf().rotateZ((float) Math.PI);
        org.joml.Quaternionf pitchRot = new org.joml.Quaternionf().rotateX(pitchAngle * 20.0F * ((float) Math.PI / 180.0F));
        flip.mul(pitchRot);
        float yBodyRot = entity.yBodyRot;
        float yaw = entity.getYRot();
        float pitch = entity.getXRot();
        float prevHeadYaw = entity.yHeadRotO;
        float yHeadRot = entity.yHeadRot;
        entity.yBodyRot = 180.0F + yawAngle * 20.0F;
        entity.setYRot(180.0F + yawAngle * 40.0F);
        entity.setXRot(-pitchAngle * 20.0F);
        entity.yHeadRot = entity.getYRot();
        entity.yHeadRotO = entity.getYRot();
        net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventory(
                ctx, x, y, size, new org.joml.Vector3f(), flip, pitchRot, entity);
        entity.yBodyRot = yBodyRot;
        entity.setYRot(yaw);
        entity.setXRot(pitch);
        entity.yHeadRotO = prevHeadYaw;
        entity.yHeadRot = yHeadRot;
        *///?} else {
        net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(ctx, x, y, size, mouseX, mouseY, entity);
        //?}
    }
}
