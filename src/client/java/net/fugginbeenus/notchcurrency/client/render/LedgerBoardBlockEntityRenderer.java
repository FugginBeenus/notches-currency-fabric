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

import java.util.List;

/**
 * Draws the live leaderboard onto the Ledger Board's tablet face. The transform anchors the origin
 * at the CENTRE of the tablet's front plaque; rows are drawn in text-pixel space around it. The six
 * constants below are the only things to nudge if the plate placement needs an in-game tweak.
 */
public class LedgerBoardBlockEntityRenderer implements BlockEntityRenderer<LedgerBoardBlockEntity> {

    // --- tunables ---
    private static final float FACE_Z = 0.372f; // world z of the tablet front (model tablet min z = 6/16)
    private static final float PLATE_Y = 0.46f; // how far above block-centre the plaque centre sits (blocks)
    private static final float SCALE = 0.009f;  // text scale (smaller = more fits on the narrow tablet)
    private static final int HEADER_Y = -34;    // header baseline (text px; negative = up)
    private static final int ROW0 = -22;        // first rank baseline (text px)
    private static final int ROW_STEP = 9;      // gap between ranks (text px)

    private final TextRenderer text;

    public LedgerBoardBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.text = ctx.getTextRenderer();
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
        int deg = switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0; // NORTH
        };

        matrices.push();
        matrices.translate(0.5, 0.5, 0.5);                                   // block centre
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180 - deg)); // orient to the front
        matrices.translate(0.0, PLATE_Y, 0.5 - FACE_Z + 0.002);              // up to the plaque, out to its face
        matrices.scale(-SCALE, -SCALE, SCALE);                               // upright + un-mirrored

        int lightBright = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        drawCentered(Text.literal("TOP BALANCES").formatted(Formatting.GOLD), matrices, vertexConsumers, lightBright, HEADER_Y);

        List<EconomyLeaderboard.Entry> rows = be.rows();
        if (rows.isEmpty()) {
            drawCentered(Text.literal("No balances yet").formatted(Formatting.GRAY), matrices, vertexConsumers, lightBright, ROW0 + ROW_STEP);
        }
        for (int i = 0; i < rows.size(); i++) {
            EconomyLeaderboard.Entry e = rows.get(i);
            Formatting rank = i == 0 ? Formatting.GOLD : i == 1 ? Formatting.WHITE : i == 2 ? Formatting.YELLOW : Formatting.GRAY;
            MutableText line = Text.literal((i + 1) + " ").formatted(rank)
                    .append(Text.literal(trim(e.name())).formatted(Formatting.WHITE))
                    .append(Text.literal("  " + compact(e.balance())).formatted(Formatting.YELLOW));
            drawCentered(line, matrices, vertexConsumers, lightBright, ROW0 + i * ROW_STEP);
        }
        matrices.pop();
    }

    private void drawCentered(Text t, MatrixStack m, VertexConsumerProvider vc, int light, int y) {
        float x = -text.getWidth(t) / 2f;
        text.draw(t, x, (float) y, 0xFFFFFF, false, m.peek().getPositionMatrix(), vc,
                TextRenderer.TextLayerType.NORMAL, 0, light);
    }

    private static String trim(String name) {
        return name.length() > 7 ? name.substring(0, 6) + "…" : name;
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
        return true; // text spans up into the upper half
    }
}
