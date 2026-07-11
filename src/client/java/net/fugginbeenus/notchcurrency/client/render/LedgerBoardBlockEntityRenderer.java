package net.fugginbeenus.notchcurrency.client.render;

import net.fugginbeenus.notchcurrency.block.LedgerBoardBlock;
import net.fugginbeenus.notchcurrency.block.entity.LedgerBoardBlockEntity;
import net.fugginbeenus.notchcurrency.economy.EconomyLeaderboard;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.client.MinecraftClient;
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

import java.util.List;

/**
 * Draws the live leaderboard onto the Ledger Board's tablet face. Server-synced rows (see the block
 * entity) are rendered as ranked name/balance lines. The transform constants up top are deliberately
 * easy to nudge — the exact plate placement wants an in-game eyeball.
 */
public class LedgerBoardBlockEntityRenderer implements BlockEntityRenderer<LedgerBoardBlockEntity> {

    // Tunables (block units, 1.0 = a full block). Front face of the tablet, plaque area.
    private static final float FACE_Z = 0.372f;   // tablet front plane (model tablet min z = 6/16)
    private static final float TOP_Y = 1.34f;     // first row baseline height (spans into upper block)
    private static final float ROW_H = 0.155f;    // vertical gap between rows
    private static final float SCALE = 0.0125f;   // text scale
    private static final float MARGIN = 0.14f;    // left inset from the tablet edge
    private static final float WIDTH = 0.72f;      // usable plate width (balance right-aligns to this)

    private final TextRenderer text;

    public LedgerBoardBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        this.text = ctx.getTextRenderer();
    }

    @Override
    public void render(LedgerBoardBlockEntity be, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        if (be.getWorld() == null) return;
        var state = be.getCachedState();
        if (!(state.getBlock() instanceof LedgerBoardBlock) || state.get(LedgerBoardBlock.HALF) != DoubleBlockHalf.LOWER) {
            return;
        }
        List<EconomyLeaderboard.Entry> rows = be.rows();

        Direction facing = state.get(LedgerBoardBlock.FACING);
        int deg = switch (facing) {
            case EAST -> 90;
            case SOUTH -> 180;
            case WEST -> 270;
            default -> 0; // NORTH
        };

        matrices.push();
        matrices.translate(0.5, 0.0, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180 - deg));
        matrices.translate(0.0, 0.0, -(0.5f - (1f - FACE_Z)) - 0.5f + FACE_Z + 0.001f);
        // Flip so text is upright and faces outward; scale to block space.
        matrices.scale(-SCALE, -SCALE, SCALE);

        // Header.
        int fullLight = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        drawCentered(Text.literal("TOP BALANCES").formatted(Formatting.GOLD), matrices, vertexConsumers, fullLight,
                px(TOP_Y + ROW_H));

        if (rows.isEmpty()) {
            drawCentered(Text.literal("No balances yet").formatted(Formatting.GRAY), matrices, vertexConsumers,
                    fullLight, px(TOP_Y - ROW_H));
        }
        for (int i = 0; i < rows.size(); i++) {
            EconomyLeaderboard.Entry e = rows.get(i);
            float y = px(TOP_Y - i * ROW_H);
            Formatting rankColor = i == 0 ? Formatting.GOLD : i == 1 ? Formatting.WHITE : i == 2 ? Formatting.GOLD : Formatting.GRAY;
            Text left = Text.literal((i + 1) + " ").formatted(rankColor)
                    .copy().append(Text.literal(trim(e.name())).formatted(Formatting.WHITE));
            String bal = compact(e.balance());
            drawLeft(left, matrices, vertexConsumers, fullLight, px(MARGIN), y);
            drawRight(Text.literal(bal).formatted(Formatting.YELLOW), matrices, vertexConsumers, fullLight,
                    px(MARGIN + WIDTH), y);
        }
        matrices.pop();
    }

    private float px(float blocks) {
        return blocks / SCALE; // convert a block offset into pre-scale text pixels
    }

    private void drawLeft(Text t, MatrixStack m, VertexConsumerProvider vc, int light, float x, float y) {
        text.draw(t, x, y, 0xFFFFFF, false, m.peek().getPositionMatrix(), vc,
                TextRenderer.TextLayerType.NORMAL, 0, light);
    }

    private void drawRight(Text t, MatrixStack m, VertexConsumerProvider vc, int light, float rightX, float y) {
        drawLeft(t, m, vc, light, rightX - text.getWidth(t), y);
    }

    private void drawCentered(Text t, MatrixStack m, VertexConsumerProvider vc, int light, float y) {
        drawLeft(t, m, vc, light, px(MARGIN + WIDTH / 2f) - text.getWidth(t) / 2f, y);
    }

    private static String trim(String name) {
        return name.length() > 10 ? name.substring(0, 9) + "…" : name;
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
