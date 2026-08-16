package net.fugginbeenus.notchcurrency.client.render;

import net.fugginbeenus.notchcurrency.block.LedgerBoardBlock;
import net.fugginbeenus.notchcurrency.compat.Render;
import net.fugginbeenus.notchcurrency.block.entity.LedgerBoardBlockEntity;
import net.fugginbeenus.notchcurrency.economy.EconomyLeaderboard;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
//? if <1.21.11 {
import net.minecraft.client.renderer.MultiBufferSource;
//?}
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;

//? if >=1.21.11 {
/*public class LedgerBoardBlockEntityRenderer
        implements BlockEntityRenderer<LedgerBoardBlockEntity, LedgerBoardBlockEntityRenderer.State> {

    public static class State extends net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState {
        public boolean draw;
        public float angle;
        public List<EconomyLeaderboard.Entry> rows = List.of();
    }
*///?} else {
public class LedgerBoardBlockEntityRenderer implements BlockEntityRenderer<LedgerBoardBlockEntity> {
//?}

    private static final float PLATE_TOP = 1.5f;
    private static final float FRONT_Z = 0.71f;
    private static final float SCALE = 0.0125f;
    private static final int LINE_H = 13;
    private static final int LEFT_X = -55;
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final int RIGHT_X = 55;

    private final Font text;

    public LedgerBoardBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        //? if >=1.21.11 {
        /*this.text = ctx.font();
        *///?} else {
        this.text = ctx.getFont();
        //?}
    }

    private static float horizontalAngle(Direction f) {
        float a = f.toYRot();
        return f.getAxis() == Direction.Axis.X ? -a : a;
    }

    //? if >=1.21.11 {
    /*@Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(LedgerBoardBlockEntity be, State out, float tickDelta,
                                   net.minecraft.world.phys.Vec3 cameraPos,
                                   net.minecraft.client.renderer.feature.ModelFeatureRenderer.CrumblingOverlay crumbling) {
        BlockEntityRenderer.super.extractRenderState(be, out, tickDelta, cameraPos, crumbling);
        var blockState = be.getBlockState();
        out.draw = blockState.getBlock() instanceof LedgerBoardBlock
                && blockState.getValue(LedgerBoardBlock.HALF) == DoubleBlockHalf.LOWER;
        if (!out.draw) return;
        out.angle = horizontalAngle(blockState.getValue(LedgerBoardBlock.FACING));
        out.rows = be.rows();
    }

    @Override
    public void submit(State state, PoseStack matrices,
                       net.minecraft.client.renderer.SubmitNodeCollector collector,
                       net.minecraft.client.renderer.state.CameraRenderState camera) {
        if (!state.draw) return;
        List<EconomyLeaderboard.Entry> rows = state.rows;

        matrices.pushPose();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.mulPose(Axis.YP.rotationDegrees(state.angle));
        matrices.translate(-0.5, -0.5, -0.5);
    *///?} else {
    @Override
    public void render(LedgerBoardBlockEntity be, float tickDelta, PoseStack matrices,
                       MultiBufferSource vertexConsumers, int light, int overlay) {
        var state = be.getBlockState();
        if (!(state.getBlock() instanceof LedgerBoardBlock)
                || state.getValue(LedgerBoardBlock.HALF) != DoubleBlockHalf.LOWER) {
            return;
        }
        Direction facing = state.getValue(LedgerBoardBlock.FACING);

        List<EconomyLeaderboard.Entry> rows = be.rows();

        matrices.pushPose();
        matrices.translate(0.5, 0.5, 0.5);
        matrices.mulPose(Axis.YP.rotationDegrees(horizontalAngle(facing)));
        matrices.translate(-0.5, -0.5, -0.5);
    //?}
        matrices.translate(0.5, PLATE_TOP, FRONT_Z);
        matrices.scale(SCALE, -SCALE, SCALE);
        matrices.translate(0.0, 0.0, 0.5); // a texel off the surface, avoids z-fighting

        int lb = FULL_BRIGHT;
        //? if <1.21.11 {
        var matrix = matrices.last().pose();
        //?}

        Component header = Component.literal("TOP BALANCES").withStyle(ChatFormatting.GOLD);
        //? if >=1.21.11 {
            /*Render.submitText(text, header, -text.width(header) / 2f, 0, 0xFFFFFFFF, matrices, collector, lb);
            *///?} else {
            Render.drawText(text, header, -text.width(header) / 2f, 0, 0xFFFFFFFF, matrix, vertexConsumers, lb);
            //?}

        if (rows.isEmpty()) {
            Component none = Component.literal("No balances yet").withStyle(ChatFormatting.GRAY);
            //? if >=1.21.11 {
            /*Render.submitText(text, none, -text.width(none) / 2f, LINE_H, 0xFFFFFFFF, matrices, collector, lb);
            *///?} else {
            Render.drawText(text, none, -text.width(none) / 2f, LINE_H, 0xFFFFFFFF, matrix, vertexConsumers, lb);
            //?}
        }

        for (int i = 0; i < rows.size(); i++) {
            EconomyLeaderboard.Entry e = rows.get(i);
            int y = (i + 1) * LINE_H;
            ChatFormatting rank = i == 0 ? ChatFormatting.GOLD : i == 1 ? ChatFormatting.WHITE : i == 2 ? ChatFormatting.YELLOW : ChatFormatting.GRAY;
            Component name = Component.literal((i + 1) + " ").withStyle(rank)
                    .copy().append(Component.literal(e.name()).withStyle(ChatFormatting.AQUA));
            Component bal = Component.literal(compact(e.balance())).withStyle(ChatFormatting.YELLOW);
            //? if >=1.21.11 {
            /*Render.submitText(text, name, LEFT_X, y, 0xFFFFFFFF, matrices, collector, lb);
            *///?} else {
            Render.drawText(text, name, LEFT_X, y, 0xFFFFFFFF, matrix, vertexConsumers, lb);
            //?}
            //? if >=1.21.11 {
            /*Render.submitText(text, bal, RIGHT_X - text.width(bal), y, 0xFFFFFFFF, matrices, collector, lb);
            *///?} else {
            Render.drawText(text, bal, RIGHT_X - text.width(bal), y, 0xFFFFFFFF, matrix, vertexConsumers, lb);
            //?}
        }
        matrices.popPose();
    }

    private static String compact(long n) {
        if (n < 1_000) return Long.toString(n);
        if (n < 1_000_000) return fmt(n, 1_000, "k");
        if (n < 1_000_000_000) return fmt(n, 1_000_000, "m");
        return fmt(n, 1_000_000_000, "b");
    }

    private static String fmt(long n, long unit, String suffix) {
        long whole = n / unit, tenth = (n % unit) * 10 / unit;
        return (whole < 10 && tenth > 0) ? whole + "." + tenth + suffix : whole + suffix;
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean shouldRenderOffScreen() {
        return true;
    }
    *///?} else {
    @Override
    public boolean shouldRenderOffScreen(LedgerBoardBlockEntity be) {
        return true;
    }
    //?}
}
