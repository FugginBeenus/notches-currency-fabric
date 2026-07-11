package net.fugginbeenus.notchcurrency.client.render;

import net.fugginbeenus.notchcurrency.block.LedgerBoardBlock;
import net.fugginbeenus.notchcurrency.block.entity.LedgerBoardBlockEntity;
import net.fugginbeenus.notchcurrency.economy.EconomyLeaderboard;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the live leaderboard flat on the Ledger Board's tablet, Create display-board style. The
 * transform mirrors Create's FlapDisplayRenderer: centre → rotateY(horizontalAngle) → unCentre puts
 * us in a facing-oriented block-space frame, then we step to the plaque, scale, flip Y, and nudge a
 * texel off the surface. Full-bright, no shadow. PLATE_TOP/FRONT_Z/SCALE tune the placement.
 */
public class LedgerBoardBlockEntityRenderer implements BlockEntityRenderer<LedgerBoardBlockEntity> {

    // --- tunables (block units unless noted) ---
    private static final float PLATE_TOP = 1.55f; // top of the screen (y), rows descend from here
    private static final float FRONT_Z = 0.71f;   // screen front plane in the oriented frame (1 = block front)
    private static final float SCALE = 0.017f;    // text scale
    private static final int LINE_H = 11;         // line spacing (text px)

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

        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal("TOP BALANCES").formatted(Formatting.GOLD));
        List<EconomyLeaderboard.Entry> rows = be.rows();
        if (rows.isEmpty()) {
            lines.add(Text.literal("No balances yet").formatted(Formatting.GRAY));
        }
        for (int i = 0; i < rows.size(); i++) {
            EconomyLeaderboard.Entry e = rows.get(i);
            Formatting rank = i == 0 ? Formatting.GOLD : i == 1 ? Formatting.WHITE : i == 2 ? Formatting.YELLOW : Formatting.GRAY;
            MutableText line = Text.literal((i + 1) + " ").formatted(rank)
                    .append(Text.literal(trim(e.name())).formatted(Formatting.AQUA))
                    .append(Text.literal(" " + compact(e.balance())).formatted(Formatting.YELLOW));
            lines.add(line);
        }

        matrices.push();
        // centre → rotateY(facing) → unCentre  (Create's FlapDisplayRenderer frame)
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(horizontalAngle(facing)));
        matrices.translate(-0.5, -0.5, -0.5);
        // step to the plaque top-centre, on the tablet front plane
        matrices.translate(0.5, PLATE_TOP, FRONT_Z);
        matrices.scale(SCALE, -SCALE, SCALE);
        matrices.translate(0.0, 0.0, 0.5); // a texel off the surface, avoids z-fighting

        int lightBright = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        for (int i = 0; i < lines.size(); i++) {
            Text line = lines.get(i);
            float x = -text.getWidth(line) / 2f;
            text.draw(line, x, i * LINE_H, 0xFFFFFFFF, false, matrices.peek().getPositionMatrix(),
                    vertexConsumers, TextRenderer.TextLayerType.NORMAL, 0, lightBright);
        }
        matrices.pop();
    }

    private static String trim(String name) {
        return name.length() > 8 ? name.substring(0, 7) + "…" : name;
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
