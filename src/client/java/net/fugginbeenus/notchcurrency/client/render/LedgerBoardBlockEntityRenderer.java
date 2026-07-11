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
 * Draws the live leaderboard flat on the Ledger Board's tablet, Create-display-board style:
 * face-locked (not billboard), full-bright, no shadow, left-aligned rows printed on the surface.
 * Uses the vanilla wall-sign transform (rotate by the opposite facing, step out to the front plane).
 * The five tunables up top set the plate placement — nudge if it sits off the tablet.
 */
public class LedgerBoardBlockEntityRenderer implements BlockEntityRenderer<LedgerBoardBlockEntity> {

    // --- tunables ---
    private static final float DEPTH = 0.129f;  // OUT to the tablet front plane (model tablet min z = 6/16)
    private static final float PLATE_Y = 0.46f; // raise the origin to the plaque centre (blocks)
    private static final float SCALE = 0.0083f; // text scale (small — the tablet is narrow)
    private static final int TOP = -34;         // header baseline (text px; negative = up)
    private static final int LINE_H = 10;       // line spacing (text px)

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
        matrices.translate(0.5, 0.5, 0.5);
        // Face the FACING direction, flat on the front plane; -X,-Y scale keeps it upright + unmirrored.
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-facing.asRotation()));
        matrices.translate(0.0, PLATE_Y, DEPTH);
        matrices.scale(-SCALE, -SCALE, SCALE);

        int lightBright = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        for (int i = 0; i < lines.size(); i++) {
            Text line = lines.get(i);
            // Full-bright, no shadow — the Create display-board look. Centred on the plate.
            text.draw(line, -text.getWidth(line) / 2f, TOP + i * LINE_H, 0xFFFFFFFF, false,
                    matrices.peek().getPositionMatrix(), vertexConsumers,
                    TextRenderer.TextLayerType.NORMAL, 0, lightBright);
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
        return true; // text spans up into the upper half
    }
}
