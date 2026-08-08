package net.fugginbeenus.notchcurrency.compat;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
//? if <1.21.11 {
import net.minecraft.client.renderer.MultiBufferSource;
//?}
//? if <1.21.11 {
import net.minecraft.client.renderer.entity.ItemRenderer;
//?}
import net.minecraft.network.chat.Component;
//? if <1.21.11 {
import net.minecraft.world.item.ItemDisplayContext;
//?}
import net.minecraft.world.item.ItemStack;
//? if <1.21.11 {
import net.minecraft.world.level.Level;
//?}
//? if <1.21.11 {
import org.joml.Matrix4f;
//?}

public final class Render {

    private Render() {}

    // Both of these speak the pre-1.21.11 world-drawing API. Their callers submit instead there,
    // so they simply do not exist from that version on.
    //? if <1.21.11 {
    public static void renderFixedItem(ItemRenderer itemRenderer, ItemStack stack, int light, int overlay,
                                       PoseStack matrices, MultiBufferSource vcp, Level world, int seed) {
        itemRenderer.renderStatic(stack, ItemDisplayContext.FIXED, light, overlay, matrices, vcp, world, seed);
    }
    //?}

    //? if <1.21.11 {
    public static void drawText(Font text, Component str, float x, float y, int color,
                                Matrix4f matrix, MultiBufferSource vcp, int light) {
        text.drawInBatch(str, x, y, color, false, matrix, vcp, Font.DisplayMode.NORMAL, 0, light);
    }
    //?}

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

    /**
     * Restricts what a text field will accept, and optionally watches it change.
     *
     * <p>EditBox.setFilter went away in 26.1. The stand-in is a responder that puts back the last
     * good value, which means it occupies the one responder slot a field has, so anything that also
     * wanted to watch the field has to come through here rather than set its own afterwards.
     */
    public static void setFilter(net.minecraft.client.gui.components.EditBox box,
                                 java.util.function.Predicate<String> allowed) {
        setFilter(box, allowed, null);
    }

    public static void setFilter(net.minecraft.client.gui.components.EditBox box,
                                 java.util.function.Predicate<String> allowed,
                                 java.util.function.Consumer<String> onChange) {
        //? if >=26.1 {
        /*String[] lastGood = { box.getValue() };
        box.setResponder(value -> {
            if (!allowed.test(value)) {
                box.setValue(lastGood[0]);
                return;
            }
            lastGood[0] = value;
            if (onChange != null) onChange.accept(value);
        });
        *///?} else {
        box.setFilter(allowed);
        if (onChange != null) box.setResponder(onChange);
        //?}
    }

    // One line of floating text above an entity. Flat rather than nested because its caller already
    // sits inside a version block, and 26.2 dropped the distance argument.
    //? if >=26.2 {
    /*public static void submitNameLine(net.minecraft.client.renderer.entity.state.EntityRenderState anchor,
                                      net.minecraft.network.chat.Component text,
                                      com.mojang.blaze3d.vertex.PoseStack matrices,
                                      net.minecraft.client.renderer.SubmitNodeCollector collector,
                                      net.minecraft.client.renderer.state.CameraRenderState camera) {
        collector.submitNameTag(matrices, anchor.nameTagAttachment, 0, text, !anchor.isDiscrete,
                anchor.lightCoords, camera);
    }
    *///?} elif >=1.21.11 {
    /*public static void submitNameLine(net.minecraft.client.renderer.entity.state.EntityRenderState anchor,
                                      net.minecraft.network.chat.Component text,
                                      com.mojang.blaze3d.vertex.PoseStack matrices,
                                      net.minecraft.client.renderer.SubmitNodeCollector collector,
                                      net.minecraft.client.renderer.state.CameraRenderState camera) {
        collector.submitNameTag(matrices, anchor.nameTagAttachment, 0, text, !anchor.isDiscrete,
                anchor.lightCoords, anchor.distanceToCameraSq, camera);
    }
    *///?}

    public static void drawEntityAt(net.minecraft.client.gui.GuiGraphics ctx, int x, int y, int size,
                                    float mouseX, float mouseY, net.minecraft.world.entity.LivingEntity entity) {
        //? if >=1.21.11 {
        /*net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(
                ctx, x - size, y - size * 2, x + size, y, size, 0.0625f, mouseX, mouseY, entity);
        *///?} elif >=1.21 {
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
