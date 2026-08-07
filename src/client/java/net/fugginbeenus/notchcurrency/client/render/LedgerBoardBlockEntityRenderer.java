package net.fugginbeenus.notchcurrency.client.render;

import net.fugginbeenus.notchcurrency.block.LedgerBoardBlock;
import net.fugginbeenus.notchcurrency.compat.Render;
import net.fugginbeenus.notchcurrency.block.entity.LedgerBoardBlockEntity;
import net.fugginbeenus.notchcurrency.economy.EconomyLeaderboard;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.ArrayList;
import java.util.List;

public class LedgerBoardBlockEntityRenderer implements BlockEntityRenderer<LedgerBoardBlockEntity> {

    // --- tunables (block units unless noted) ---
    private static final float PLATE_TOP = 1.5f;  // top of the screen (y), rows descend from here
    private static final float FRONT_Z = 0.71f;   // screen front plane in the oriented frame (1 = block front)
    private static final float SCALE = 0.0125f;   // text scale (smaller so full names fit)
    private static final int LINE_H = 13;         // line spacing (text px)
    private static final int LEFT_X = -55;        // left margin: rank + name start here (text px)
    private static final int RIGHT_X = 55;        // right margin: balance right-aligns here (text px)

    private final Font text;

    public LedgerBoardBlockEntityRenderer(BlockEntityRendererProvider.Context ctx) {
        this.text = ctx.getFont();
    }

    /** Create's AngleHelper.horizontalAngle: facing yaw, negated on the X axis. */
    private static float horizontalAngle(Direction f) {
        float a = f.toYRot();
        return f.getAxis() == Direction.Axis.X ? -a : a;
    }

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
        // centre → rotateY(facing) → unCentre  (Create's FlapDisplayRenderer frame)
        matrices.translate(0.5, 0.5, 0.5);
        matrices.mulPose(Axis.YP.rotationDegrees(horizontalAngle(facing)));
        matrices.translate(-0.5, -0.5, -0.5);
        // step to the screen top-centre, on the front plane
        matrices.translate(0.5, PLATE_TOP, FRONT_Z);
        matrices.scale(SCALE, -SCALE, SCALE);
        matrices.translate(0.0, 0.0, 0.5); // a texel off the surface, avoids z-fighting

        int lb = LightTexture.FULL_BRIGHT;
        var matrix = matrices.last().pose();

        // Header, centred.
        Component header = Component.literal("TOP BALANCES").withStyle(ChatFormatting.GOLD);
        Render.drawText(text, header, -text.width(header) / 2f, 0, 0xFFFFFFFF, matrix, vertexConsumers, lb);

        if (rows.isEmpty()) {
            Component none = Component.literal("No balances yet").withStyle(ChatFormatting.GRAY);
            Render.drawText(text, none, -text.width(none) / 2f, LINE_H, 0xFFFFFFFF, matrix, vertexConsumers, lb);
        }
        // Rows: rank + name left-aligned, balance right-aligned (Create-style columns).
        for (int i = 0; i < rows.size(); i++) {
            EconomyLeaderboard.Entry e = rows.get(i);
            int y = (i + 1) * LINE_H;
            ChatFormatting rank = i == 0 ? ChatFormatting.GOLD : i == 1 ? ChatFormatting.WHITE : i == 2 ? ChatFormatting.YELLOW : ChatFormatting.GRAY;
            Component name = Component.literal((i + 1) + " ").withStyle(rank)
                    .copy().append(Component.literal(e.name()).withStyle(ChatFormatting.AQUA));
            Component bal = Component.literal(compact(e.balance())).withStyle(ChatFormatting.YELLOW);
            Render.drawText(text, name, LEFT_X, y, 0xFFFFFFFF, matrix, vertexConsumers, lb);
            Render.drawText(text, bal, RIGHT_X - text.width(bal), y, 0xFFFFFFFF, matrix, vertexConsumers, lb);
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

    @Override
    public boolean shouldRenderOffScreen(LedgerBoardBlockEntity be) {
        return true;
    }
}
