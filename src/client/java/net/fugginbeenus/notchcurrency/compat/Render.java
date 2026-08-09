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
        /*// Every caller here means "stand the entity on (x, y) at this scale", which is what the
        // old anchored call did. This one takes a rectangle instead and centres the entity inside
        // it, so the rectangle has to be built around where the entity will actually end up.
        // Deriving it from the scale, as this once did, put the centre a whole tile too high.
        //
        // Rendered pixels are the scale times the entity's size in blocks. Sit the box so the feet
        // land on y, and pad it, because plenty of mobs draw outside their collision box (wings,
        // tails, ears) and the rectangle crops whatever leaves it.
        // A given scale also draws bigger here than it used to, by about half again, so every
        // caller's long-tuned number came out oversized. Bring it back down rather than retune five
        // call sites, and keep the box maths in the old units the callers still think in.
        int scale = Math.max(1, Math.round(size / 1.5f));
        float bbHeight = Math.max(0.1f, entity.getBbHeight());
        float bbWidth = Math.max(0.1f, entity.getBbWidth());
        int drawnHeight = Math.max(2, Math.round(size * bbHeight));
        int drawnWidth = Math.max(2, Math.round(size * bbWidth));
        // A square box on the larger dimension, because models routinely reach past their collision
        // box in whichever direction is the short one, and anything outside the box is cropped.
        int extent = Math.max(drawnWidth, drawnHeight);
        int half = extent / 2 + Math.max(3, extent / 4);
        int centreY = y - drawnHeight / 2;
        net.minecraft.client.gui.screens.inventory.InventoryScreen.renderEntityInInventoryFollowsMouse(
                ctx, x - half, centreY - half, x + half, centreY + half,
                scale, 0.0625f, mouseX, mouseY, entity);
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
