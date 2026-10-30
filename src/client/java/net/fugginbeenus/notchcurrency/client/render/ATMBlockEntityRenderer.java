package net.fugginbeenus.notchcurrency.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.fugginbeenus.notchcurrency.block.entity.ATMBlockEntity;
import net.fugginbeenus.notchcurrency.compat.Render;
import net.fugginbeenus.notchcurrency.registry.ModItems;
//? if <26.1 {
import net.minecraft.client.renderer.MultiBufferSource;
//?}
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
//? if <26.1 {
import net.minecraft.client.renderer.entity.ItemRenderer;
//?}
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

//? if >=1.21.11 {
/*public class ATMBlockEntityRenderer
        implements BlockEntityRenderer<ATMBlockEntity, ATMBlockEntityRenderer.State> {

    public static class State extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState {
        public boolean draw;
        public float time;
        public final net.minecraft.client.renderer.item.ItemStackRenderState[] coins =
                new net.minecraft.client.renderer.item.ItemStackRenderState[CENTERS.length];
        {
            for (int i = 0; i < coins.length; i++) {
                coins[i] = new net.minecraft.client.renderer.item.ItemStackRenderState();
            }
        }
    }
*///?} else {
public class ATMBlockEntityRenderer implements BlockEntityRenderer<ATMBlockEntity> {
//?}

    private static final float BOB = 0.05f, BOB_TICKS = 80f, TURN_TICKS = 320f;
    private static final long LOOP_TICKS = 1920L;
    private static final float[][] CENTERS = {
            {0.5000f, 1.3750f, 0.5000f},
            {0.2618f, 1.1275f, 0.3125f},
            {0.6993f, 1.2525f, 0.3125f},
            {0.7382f, 1.1275f, 0.6875f},
            {0.3007f, 1.2525f, 0.6875f},
    };
    private static final float[][] NUDGE = {{0f, 0f}, {0f, 0f}, {0.0910f, -0.0857f}, {0f, 0f}, {-0.0910f, 0.0857f}};
    private static final float[] SPIN_PHASE = {0f, 0.0625f, 0.6875f, 0.4375f, 0.1875f};
    private static final float[] BOB_PHASE = {0f, 0f, 0.5f, 0f, 0.375f};

    //? if >=1.21.11 {
    /*private final net.minecraft.client.renderer.item.ItemModelResolver itemModelResolver;
    *///?} else {
    private final ItemRenderer itemRenderer;
    //?}
    private ItemStack[] coins;

    public ATMBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        //? if >=1.21.11 {
        /*this.itemModelResolver = ctx.itemModelResolver();
        *///?} else {
        this.itemRenderer = ctx.getItemRenderer();
        //?}
    }

    private ItemStack coin(int i) {
        if (coins == null) {
            coins = new ItemStack[CENTERS.length];
            for (int k = 0; k < coins.length; k++) {
                ItemStack stack = new ItemStack(ModItems.ATM_COINS);
                //? if >=1.21.11 {
                /*stack.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
                        new net.minecraft.world.item.component.CustomModelData(
                                java.util.List.of((float) (k + 1)), java.util.List.of(), java.util.List.of(), java.util.List.of()));
                *///?} elif >=1.21 {
                /*stack.set(net.minecraft.core.component.DataComponents.CUSTOM_MODEL_DATA,
                        new net.minecraft.world.item.component.CustomModelData(k + 1));
                *///?} else {
                stack.getOrCreateTag().putInt("CustomModelData", k + 1);
                //?}
                coins[k] = stack;
            }
        }
        return coins[i];
    }

    private static float time(ATMBlockEntity be, Level world, float tickDelta) {
        long offset = be.getBlockPos().asLong() * 7919L;
        return Math.floorMod(world.getGameTime() + offset, LOOP_TICKS) + tickDelta;
    }

    private static void place(PoseStack matrices, float t, int i) {
        float[] c = CENTERS[i];
        float bob = Mth.sin((t / BOB_TICKS + BOB_PHASE[i]) * (float) (Math.PI * 2)) * BOB;
        matrices.translate(c[0] + NUDGE[i][0], c[1] + bob, c[2] + NUDGE[i][1]);
        matrices.mulPose(Axis.YP.rotationDegrees((t / TURN_TICKS + SPIN_PHASE[i]) * 360f));
        matrices.translate(0.5f - c[0], 0.5f - c[1], 0.5f - c[2]);
    }

    //? if >=1.21.11 {
    /*@Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(ATMBlockEntity be, State out, float tickDelta,
                                   net.minecraft.world.phys.Vec3 cameraPos,
                                   net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, out, tickDelta, cameraPos, crumbling);
        Level world = be.getLevel();
        out.draw = world != null;
        if (!out.draw) return;
        out.time = time(be, world, tickDelta);
        for (int i = 0; i < out.coins.length; i++) {
            itemModelResolver.updateForTopItem(out.coins[i], coin(i),
                    net.minecraft.world.item.ItemDisplayContext.FIXED, world, null, 0);
        }
    }

    @Override
    public void submit(State state, PoseStack matrices,
                       net.minecraft.client.renderer.SubmitNodeCollector collector,
                       net.minecraft.client.renderer.state.CameraRenderState camera) {
        if (!state.draw) return;
        for (int i = 0; i < state.coins.length; i++) {
            matrices.pushPose();
            place(matrices, state.time, i);
            state.coins[i].submit(matrices, collector, state.lightCoords,
                    net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY, 0);
            matrices.popPose();
        }
    }
    *///?} else {
    @Override
    public void render(ATMBlockEntity be, float tickDelta, PoseStack matrices,
                       MultiBufferSource vertexConsumers, int light, int overlay) {
        Level world = be.getLevel();
        if (world == null) return;
        float t = time(be, world, tickDelta);
        for (int i = 0; i < CENTERS.length; i++) {
            matrices.pushPose();
            place(matrices, t, i);
            Render.renderFixedItem(itemRenderer, coin(i), light, overlay, matrices, vertexConsumers, world, 0);
            matrices.popPose();
        }
    }
    //?}
}
