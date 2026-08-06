package net.fugginbeenus.notchcurrency.client.render;

import net.fugginbeenus.notchcurrency.block.LedgerBoardBlock;
import net.fugginbeenus.notchcurrency.compat.Render;
import net.fugginbeenus.notchcurrency.block.entity.LedgerBoardBlockEntity;
import net.fugginbeenus.notchcurrency.economy.EconomyLeaderboard;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

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

    private final TextRenderer text;

    public LedgerBoardBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.text = ctx.getTextRenderer();
    }

    /** Create's AngleHelper.horizontalAngle: facing yaw, negated on the X axis. */
    private static float horizontalAngle(Direction f) {
        float a = f.asRotation();
        return f.getAxis() == Direction.Axis.X ? -a : a;
    }

    @Override
    public void render(LedgerBoardBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        var state = be.getCachedState();
        if (!(state.getBlock() instanceof LedgerBoardBlock)
                || state.get(LedgerBoardBlock.HALF) != DoubleBlockHalf.LOWER) {
            return;
        }
        Direction facing = state.get(LedgerBoardBlock.FACING);

        List<EconomyLeaderboard.Entry> rows = be.rows();

        matrices.push();
        // centre → rotateY(facing) → unCentre  (Create's FlapDisplayRenderer frame)
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(horizontalAngle(facing)));
        matrices.translate(-0.5, -0.5, -0.5);
        // step to the screen top-centre, on the front plane
        matrices.translate(0.5, PLATE_TOP, FRONT_Z);
        matrices.scale(SCALE, -SCALE, SCALE);
        matrices.translate(0.0, 0.0, 0.5); // a texel off the surface, avoids z-fighting

        int lb = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        var matrix = matrices.peek().getPositionMatrix();

        // Header, centred.
        Text header = Text.literal("TOP BALANCES").formatted(Formatting.GOLD);
        Render.drawText(text, header, -text.getWidth(header) / 2f, 0, 0xFFFFFFFF, matrix, vertexConsumers, lb);

        if (rows.isEmpty()) {
            Text none = Text.literal("No balances yet").formatted(Formatting.GRAY);
            Render.drawText(text, none, -text.getWidth(none) / 2f, LINE_H, 0xFFFFFFFF, matrix, vertexConsumers, lb);
        }
        // Rows: rank + name left-aligned, balance right-aligned (Create-style columns).
        for (int i = 0; i < rows.size(); i++) {
            EconomyLeaderboard.Entry e = rows.get(i);
            int y = (i + 1) * LINE_H;
            Formatting rank = i == 0 ? Formatting.GOLD : i == 1 ? Formatting.WHITE : i == 2 ? Formatting.YELLOW : Formatting.GRAY;
            Text name = Text.literal((i + 1) + " ").formatted(rank)
                    .copy().append(Text.literal(e.name()).formatted(Formatting.AQUA));
            Text bal = Text.literal(compact(e.balance())).formatted(Formatting.YELLOW);
            Render.drawText(text, name, LEFT_X, y, 0xFFFFFFFF, matrix, vertexConsumers, lb);
            Render.drawText(text, bal, RIGHT_X - text.getWidth(bal), y, 0xFFFFFFFF, matrix, vertexConsumers, lb);
        }
        matrices.pop();
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
    public boolean rendersOutsideBoundingBox(LedgerBoardBlockEntity be) {
        return true;
    }
}
