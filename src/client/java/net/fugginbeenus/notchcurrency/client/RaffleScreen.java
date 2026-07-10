package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.raffle.RaffleScreenHandler;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The raffle panel — a compact, code-drawn showcase: a pulsing gold frame + corner sparkles
 * behind a 2x prize display, the prize name + pot, your odds, and big buy / claim / redeem
 * buttons. Coins-only raffles show a giant Notch Coin; item raffles show the prize item (hover
 * for full item info). With no raffle running it shows a clean clock placeholder while still
 * letting winners claim.
 */
public class RaffleScreen extends HandledScreen<RaffleScreenHandler> {

    private static final int W = 198;
    private static final int H = 218;

    private static final int CASE_X = 73, CASE_Y = 22, CASE_W = 52, CASE_H = 52;

    private static final int BUY_Y = 156, BUY_H = 18;
    private static final int BUY1_X = 14, BUY5_X = 72, BUY10_X = 130, BUY_W = 54;
    private static final int CLAIM_X = 14, CLAIM_Y = 178, CLAIM_W = 170, CLAIM_H = 18;
    private static final int REDEEM_X = 14, REDEEM_Y = 199, REDEEM_W = 170, REDEEM_H = 16;

    // 9x9 clock used for the "no raffle" placeholder.
    private static final String[] CLOCK = {
            "..#####..",
            ".#..#..#.",
            "#...#...#",
            "#...#...#",
            "#...####.",
            "#.......#",
            "#.......#",
            ".#.....#.",
            "..#####..",
    };

    public RaffleScreen(RaffleScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = W;
        this.backgroundHeight = H;
        this.titleX = -1000;
        this.playerInventoryTitleX = -1000;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x, y = this.y;
        NotchWidgets.panel(ctx, x, y, W, H);

        // Raffle is the one screen that keeps a gold title (its identity).
        int round = handler.prop(RaffleScreenHandler.P_ROUND);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("RAFFLE #" + round).formatted(Formatting.GOLD, Formatting.BOLD),
                x + W / 2, y + 8, 0xFFFFFFFF);

        boolean enabled = handler.prop(RaffleScreenHandler.P_ENABLED) == 1;

        if (!enabled) {
            drawPlaceholder(ctx, x, y);
        } else {
            boolean hasItem = handler.prop(RaffleScreenHandler.P_HAS_ITEM) == 1;
            ItemStack prize = hasItem ? handler.getPrizeStack() : new ItemStack(ModItems.NOTCH_COIN);

            drawGlow(ctx, x + CASE_X + CASE_W / 2, y + CASE_Y + CASE_H / 2);
            drawCase(ctx, x + CASE_X, y + CASE_Y, CASE_W, CASE_H, false);

            var m = ctx.getMatrices();
            m.push();
            m.translate(x + CASE_X + CASE_W / 2f - 16f, y + CASE_Y + CASE_H / 2f - 16f, 0f);
            m.scale(2f, 2f, 1f);
            ctx.drawItem(prize, 0, 0);
            if (prize.getCount() > 1) ctx.drawItemInSlot(this.textRenderer, prize, 0, 0);
            m.pop();

            // Prize name: white hero text (readable on the light panel).
            String prizeName = hasItem ? prize.getName().getString() : "Coin Jackpot";
            NotchWidgets.centerText(ctx, this.textRenderer, prizeName, x + W / 2 - 0, y + 80, NotchTheme.TEXT_LIGHT, true);

            // Total coin prize = ticket pot + the admin's guaranteed coins pool.
            long pot = handler.prop(RaffleScreenHandler.P_POT) & 0xFFFFFFFFL;
            long guaranteed = handler.prop(RaffleScreenHandler.P_COINS_POOL) & 0xFFFFFFFFL;
            NotchWidgets.centerText(ctx, this.textRenderer, "Prize: " + (pot + guaranteed) + " coins", x + W / 2, y + 92, NotchTheme.TEXT_DARK, false);

            NotchWidgets.divider(ctx, x + 12, y + 104, W - 24);

            int price = handler.prop(RaffleScreenHandler.P_PRICE);
            int yours = handler.prop(RaffleScreenHandler.P_YOURS);
            int total = handler.prop(RaffleScreenHandler.P_TOTAL);
            String odds = (total > 0 && yours > 0) ? String.format("%.1f%%", 100.0 * yours / total) : "0%";

            ctx.drawText(this.textRenderer, "Ticket price: " + price + " coins", x + 14, y + 112, NotchTheme.TEXT_DARK, false);
            ctx.drawText(this.textRenderer, "Your entries: " + yours + "  (" + odds + " to win)", x + 14, y + 124, NotchTheme.TEXT_DARK, false);
            ctx.drawText(this.textRenderer, "Total entries: " + total, x + 14, y + 136, NotchTheme.TEXT_DARK, false);

            NotchWidgets.primaryButton(ctx, this.textRenderer, x + BUY1_X, y + BUY_Y, BUY_W, BUY_H, "Buy 1", over(mouseX, mouseY, x + BUY1_X, y + BUY_Y, BUY_W, BUY_H));
            NotchWidgets.primaryButton(ctx, this.textRenderer, x + BUY5_X, y + BUY_Y, BUY_W, BUY_H, "Buy 5", over(mouseX, mouseY, x + BUY5_X, y + BUY_Y, BUY_W, BUY_H));
            NotchWidgets.primaryButton(ctx, this.textRenderer, x + BUY10_X, y + BUY_Y, BUY_W, BUY_H, "Buy 10", over(mouseX, mouseY, x + BUY10_X, y + BUY_Y, BUY_W, BUY_H));
        }

        if (handler.prop(RaffleScreenHandler.P_HAS_CLAIM) == 1) {
            NotchWidgets.goldButton(ctx, this.textRenderer, x + CLAIM_X, y + CLAIM_Y, CLAIM_W, CLAIM_H,
                    "CLAIM PRIZE", over(mouseX, mouseY, x + CLAIM_X, y + CLAIM_Y, CLAIM_W, CLAIM_H));
        }

        if (handler.prop(RaffleScreenHandler.P_LOSERS) > 0 && handler.prop(RaffleScreenHandler.P_CAN_REDEEM) == 1) {
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + REDEEM_X, y + REDEEM_Y, REDEEM_W, REDEEM_H,
                    "Redeem an old ticket", over(mouseX, mouseY, x + REDEEM_X, y + REDEEM_Y, REDEEM_W, REDEEM_H));
        }
    }

    /** Clean "nothing running" state: a muted case with a faded clock + a short message. */
    private void drawPlaceholder(DrawContext ctx, int x, int y) {
        drawCase(ctx, x + CASE_X, y + CASE_Y, CASE_W, CASE_H, true);
        int cx = x + CASE_X + CASE_W / 2, cy = y + CASE_Y + CASE_H / 2;
        // 9x9 clock at 3x, centered.
        int scale = 3;
        int ox = cx - (9 * scale) / 2, oy = cy - (9 * scale) / 2;
        for (int r = 0; r < CLOCK.length; r++) {
            String row = CLOCK[r];
            for (int c = 0; c < row.length(); c++) {
                if (row.charAt(c) == '#') {
                    ctx.fill(ox + c * scale, oy + r * scale, ox + c * scale + scale, oy + r * scale + scale, 0xFFAAAAAA);
                }
            }
        }
        NotchWidgets.centerText(ctx, this.textRenderer, "No raffle running", x + W / 2, y + 86, NotchTheme.TEXT_DARK, false);
        NotchWidgets.centerText(ctx, this.textRenderer, "Check back soon!", x + W / 2, y + 100, NotchTheme.TEXT_MUTED, false);
    }

    /** Pulsing gold frame ring + twinkling corner sparkles behind the showcase. */
    private void drawGlow(DrawContext ctx, int cx, int cy) {
        float t = (System.currentTimeMillis() % 2000L) / 2000f;
        float pulse = 0.5f + 0.5f * (float) Math.sin(t * Math.PI * 2);
        int a = Math.min((int) (50 + 110 * pulse), 255);
        int gold = (a << 24) | 0x00FFD700;

        int half = 29;
        ctx.fill(cx - half, cy - half, cx + half, cy - half + 2, gold);
        ctx.fill(cx - half, cy + half - 2, cx + half, cy + half, gold);
        ctx.fill(cx - half, cy - half, cx - half + 2, cy + half, gold);
        ctx.fill(cx + half - 2, cy - half, cx + half, cy + half, gold);

        int s = 2 + Math.round(2 * pulse);
        int twinkle = (Math.min((int) (120 + 135 * pulse), 255) << 24) | 0x00FFFFFF;
        sparkle(ctx, cx - half, cy - half, s, twinkle);
        sparkle(ctx, cx + half, cy - half, s, twinkle);
        sparkle(ctx, cx - half, cy + half, s, twinkle);
        sparkle(ctx, cx + half, cy + half, s, twinkle);
    }

    private void sparkle(DrawContext ctx, int x, int y, int s, int color) {
        ctx.fill(x - s, y, x + s + 1, y + 1, color);
        ctx.fill(x, y - s, x + 1, y + s + 1, color);
    }

    /** Dark display case with a gold inner border (or a muted grey one when {@code muted}). */
    private void drawCase(DrawContext ctx, int x, int y, int w, int h, boolean muted) {
        ctx.fill(x, y, x + w, y + h, NotchTheme.OUTLINE);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, muted ? 0xFF555555 : NotchTheme.DEEP);
        int border = muted ? NotchTheme.INSET_SHADOW : NotchTheme.TEXT_GOLD;
        ctx.fill(x + 3, y + 3, x + w - 3, y + 4, border);
        ctx.fill(x + 3, y + h - 4, x + w - 3, y + h - 3, border);
        ctx.fill(x + 3, y + 3, x + 4, y + h - 3, border);
        ctx.fill(x + w - 4, y + 3, x + w - 3, y + h - 3, border);
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        // No default labels.
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);

        if (handler.prop(RaffleScreenHandler.P_ENABLED) == 1
                && handler.prop(RaffleScreenHandler.P_HAS_ITEM) == 1
                && over(mouseX, mouseY, this.x + CASE_X, this.y + CASE_Y, CASE_W, CASE_H)) {
            ItemStack prize = handler.getPrizeStack();
            if (!prize.isEmpty()) {
                ctx.drawItemTooltip(this.textRenderer, prize, mouseX, mouseY);
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.client != null && this.client.interactionManager != null) {
            int id = buttonAt(mouseX, mouseY);
            if (id >= 0) {
                NotchWidgets.click();
                this.client.interactionManager.clickButton(this.handler.syncId, id);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private int buttonAt(double mx, double my) {
        boolean enabled = handler.prop(RaffleScreenHandler.P_ENABLED) == 1;
        if (enabled) {
            if (over((int) mx, (int) my, this.x + BUY1_X, this.y + BUY_Y, BUY_W, BUY_H)) return 0;
            if (over((int) mx, (int) my, this.x + BUY5_X, this.y + BUY_Y, BUY_W, BUY_H)) return 1;
            if (over((int) mx, (int) my, this.x + BUY10_X, this.y + BUY_Y, BUY_W, BUY_H)) return 2;
        }
        if (handler.prop(RaffleScreenHandler.P_HAS_CLAIM) == 1
                && over((int) mx, (int) my, this.x + CLAIM_X, this.y + CLAIM_Y, CLAIM_W, CLAIM_H)) return 3;
        if (handler.prop(RaffleScreenHandler.P_LOSERS) > 0 && handler.prop(RaffleScreenHandler.P_CAN_REDEEM) == 1
                && over((int) mx, (int) my, this.x + REDEEM_X, this.y + REDEEM_Y, REDEEM_W, REDEEM_H)) return 4;
        return -1;
    }
}
