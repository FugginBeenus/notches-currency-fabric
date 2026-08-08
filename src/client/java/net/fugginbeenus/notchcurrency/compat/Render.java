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

    //? if >=1.21.11 {
    /*// Text in the world is submitted for the drawing pass now rather than batched on the spot.
    public static void submitText(Font text, Component str, float x, float y, int color,
                                  PoseStack matrices,
                                  net.minecraft.client.renderer.SubmitNodeCollector collector, int light) {
        collector.submitText(matrices, x, y, str.getVisualOrderText(), false,
                Font.DisplayMode.NORMAL, light, color, 0, 0);
    }
    *///?}

    public static void pushGui(net.minecraft.client.gui.GuiGraphics ctx) {
        //? if >=1.21.11 {
        /*ctx.pose().pushMatrix();
        *///?} else {
        ctx.pose().pushPose();
        //?}
    }

    public static void popGui(net.minecraft.client.gui.GuiGraphics ctx) {
        //? if >=1.21.11 {
        /*ctx.pose().popMatrix();
        *///?} else {
        ctx.pose().popPose();
        //?}
    }

    public static void translateGui(net.minecraft.client.gui.GuiGraphics ctx, float x, float y) {
        //? if >=1.21.11 {
        /*ctx.pose().translate(x, y);
        *///?} else {
        ctx.pose().translate(x, y, 0f);
        //?}
    }

    public static void scaleGui(net.minecraft.client.gui.GuiGraphics ctx, float sx, float sy) {
        //? if >=1.21.11 {
        /*ctx.pose().scale(sx, sy);
        *///?} else {
        ctx.pose().scale(sx, sy, 1f);
        //?}
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

    /**
     * Whether either shift key is held.
     *
     * <p>Screen.hasShiftDown went away in 1.21.11, where modifier state rides on the input event.
     * Several callers here are plain helpers with no event in scope, so this asks the window instead,
     * which reads the same on every version.
     */
    public static boolean shiftDown() {
        //? if >=1.21.11 {
        /*com.mojang.blaze3d.platform.Window window = net.minecraft.client.Minecraft.getInstance().getWindow();
        *///?} else {
        long window = net.minecraft.client.Minecraft.getInstance().getWindow().getWindow();
        //?}
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

    // 26.1 stopped exposing the open screen on Minecraft: it only takes one, it does not hand one
    // back. Two places here need to know what is open, so the mod keeps its own note of it, kept in
    // step by the screen lifecycle events. Older versions just read the field.
    //? if >=26.1 {
    /*private static net.minecraft.client.gui.screens.Screen openScreen;

    public static void trackScreens() {
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            openScreen = screen;
            net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.remove(screen).register(closed -> {
                if (openScreen == closed) openScreen = null;
            });
        });
    }

    public static net.minecraft.client.gui.screens.Screen currentScreen() {
        return openScreen;
    }
    *///?} else {
    public static void trackScreens() {
        // Nothing to track: Minecraft still holds the open screen itself.
    }

    public static net.minecraft.client.gui.screens.Screen currentScreen() {
        return net.minecraft.client.Minecraft.getInstance().screen;
    }
    //?}
}
