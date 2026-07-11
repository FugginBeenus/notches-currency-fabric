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
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Draws the live leaderboard for the Ledger Board as a camera-facing billboard hovering over the
 * tablet. (A flat face-locked draw kept rendering into the block; the billboard is the reliable
 * transform and reads from any angle.) Full-bright glowing text on a faint plate — the Create-ish
 * look. HOVER_Y/SCALE tune the placement.
 */
public class LedgerBoardBlockEntityRenderer implements BlockEntityRenderer<LedgerBoardBlockEntity> {

    private static final float HOVER_Y = 1.2f;  // height above the lower-block origin (blocks)
    private static final float SCALE = 0.016f;  // billboard text scale
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

        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal("TOP BALANCES").formatted(Formatting.GOLD, Formatting.BOLD));
        List<EconomyLeaderboard.Entry> rows = be.rows();
        if (rows.isEmpty()) {
            lines.add(Text.literal("No balances yet").formatted(Formatting.GRAY));
        }
        for (int i = 0; i < rows.size(); i++) {
            EconomyLeaderboard.Entry e = rows.get(i);
            Formatting rank = i == 0 ? Formatting.GOLD : i == 1 ? Formatting.WHITE : i == 2 ? Formatting.YELLOW : Formatting.GRAY;
            MutableText line = Text.literal((i + 1) + " ").formatted(rank)
                    .append(Text.literal(trim(e.name())).formatted(Formatting.AQUA))
                    .append(Text.literal("  " + compact(e.balance())).formatted(Formatting.YELLOW));
            lines.add(line);
        }

        matrices.push();
        matrices.translate(0.5, HOVER_Y, 0.5);
        matrices.multiply(MinecraftClient.getInstance().getEntityRenderDispatcher().getRotation());
        matrices.scale(-SCALE, -SCALE, SCALE);

        int lightBright = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        int top = -(lines.size() * LINE_H) / 2;
        int bg = (int) (MinecraftClient.getInstance().options.getTextBackgroundOpacity(0.35f) * 255f) << 24;
        for (int i = 0; i < lines.size(); i++) {
            Text t = lines.get(i);
            float x = -text.getWidth(t) / 2f;
            float y = top + i * LINE_H;
            text.draw(t, x, y, 0xFFFFFFFF, false, matrices.peek().getPositionMatrix(), vertexConsumers,
                    TextRenderer.TextLayerType.SEE_THROUGH, bg, lightBright);
            text.draw(t, x, y, 0xFFFFFFFF, false, matrices.peek().getPositionMatrix(), vertexConsumers,
                    TextRenderer.TextLayerType.NORMAL, 0, lightBright);
        }
        matrices.pop();
    }

    private static String trim(String name) {
        return name.length() > 9 ? name.substring(0, 8) + "…" : name;
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
