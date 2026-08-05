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

/**
 * Version-compat facade for the few client render calls whose signatures churn across Minecraft
 * versions: the item render inside a block-entity renderer and the flat world-space text draw used
 * by the coin-flip and ledger-board renderers.
 *
 * <p>One of the mod's four compat facades for the Stonecutter port. On 1.21 the
 * {@link ModelTransformationMode} enum becomes {@code ItemDisplayContext} and the
 * {@code renderItem}/{@code TextRenderer.draw} signatures shift; only this file changes. On 1.20.1
 * (this build) both methods are passthroughs, so there is no behavior change.
 *
 * <p>This facade lives in the client source set (unlike {@code StackData}/{@code Reg}, which are
 * common) because it touches client-only render types.
 */
public final class Render {

    private Render() {}

    /**
     * Render a stack in the "fixed" display pose (as in an item frame) inside a block-entity
     * renderer. On 1.20.1 this is {@code itemRenderer.renderItem(stack, ModelTransformationMode.FIXED,
     * …)}; on 1.21 the mode becomes {@code ItemDisplayContext.FIXED}.
     */
    public static void renderFixedItem(ItemRenderer itemRenderer, ItemStack stack, int light, int overlay,
                                       MatrixStack matrices, VertexConsumerProvider vcp, World world, int seed) {
        itemRenderer.renderItem(stack, ModelTransformationMode.FIXED, light, overlay, matrices, vcp, world, seed);
    }

    /**
     * Draw one line of world-space text: no drop shadow, normal text layer, no background box.
     * Wraps the {@code Matrix4f} overload of {@link TextRenderer#draw} so the ledger board renderer
     * stays version-stable.
     */
    public static void drawText(TextRenderer text, Text str, float x, float y, int color,
                                Matrix4f matrix, VertexConsumerProvider vcp, int light) {
        text.draw(str, x, y, color, false, matrix, vcp, TextRenderer.TextLayerType.NORMAL, 0, light);
    }

    /**
     * Draw a living entity in a GUI with its feet anchored at (x, y), turned toward the mouse:
     * the NPC preview used across the editor screens. 1.21 removed this exact vanilla overload in
     * favour of a bounding-box variant, so the 1.21 branch replays the removed method's body (mouse
     * angles, the flip/pitch quaternions, and the yaw save-restore) against the low-level overload.
     */
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
