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
    /*
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


    //? if >=26.2 {
    /*public static org.joml.Quaternionf cameraFacing(
            net.minecraft.client.renderer.state.level.CameraRenderState camera) {
        return new org.joml.Quaternionf().rotationYXZ(
                -camera.yRot * ((float) Math.PI / 180f), camera.xRot * ((float) Math.PI / 180f), 0f);
    }
    *///?} elif >=1.21.11 {
    /*public static org.joml.Quaternionf cameraFacing(
            net.minecraft.client.renderer.state.CameraRenderState camera) {
        return camera.orientation;
    }
    *///?}

    @org.jetbrains.annotations.Nullable
    public static net.minecraft.world.entity.Entity createDetached(
            net.minecraft.world.level.Level level, net.minecraft.world.entity.EntityType<?> type) {
        //? if >=26.2 {
        /*net.minecraft.world.entity.Entity entity = type.create(level,
                new net.minecraft.world.entity.EntitySpawnRequest(
                        net.minecraft.world.entity.EntitySpawnReason.LOAD, true));
        *///?} elif >=1.21.11 {
        /*net.minecraft.world.entity.Entity entity = type.create(level,
                net.minecraft.world.entity.EntitySpawnReason.LOAD);
        *///?} else {
        net.minecraft.world.entity.Entity entity = type.create(level);
        //?}
        if (entity != null) entity.setId(-1);
        return entity;
    }

    public static void drawEntityAt(net.minecraft.client.gui.GuiGraphics ctx, int x, int y, int size,
                                    float mouseX, float mouseY, net.minecraft.world.entity.LivingEntity entity) {
        //? if >=1.21.11 {
        /*
        float bbHeight = Math.max(0.1f, entity.getBbHeight());
        float bbWidth = Math.max(0.1f, entity.getBbWidth());
        int drawnHeight = Math.max(2, Math.round(size * bbHeight));
        int drawnWidth = Math.max(2, Math.round(size * bbWidth));
        int extent = Math.max(drawnWidth, drawnHeight);
        int half = extent / 2 + Math.max(3, extent / 4);
        int centreY = y - drawnHeight / 2;
        net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(
                ctx, x - half, centreY - half, x + half, centreY + half,
                size, 0.0625f, x - mouseX, centreY - mouseY, entity);
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

    public static boolean shiftDown() {
        //? if >=1.21.11 {
        /*com.mojang.blaze3d.platform.Window window = net.minecraft.client.Minecraft.getInstance().getWindow();
        *///?} else {
        long window = net.minecraft.client.Minecraft.getInstance().getWindow().getWindow();
        //?}
        return com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT)
                || com.mojang.blaze3d.platform.InputConstants.isKeyDown(window, org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT);
    }

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
