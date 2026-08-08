package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.raffle.RaffleScreenHandler;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class RaffleScreen extends AbstractContainerScreen<RaffleScreenHandler> {

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

    public RaffleScreen(RaffleScreenHandler handler, Inventory inv, Component title) {
        //? if >=26.1 {
        /*super(handler, inv, title, W, H);
        *///?} else {
        super(handler, inv, title);
        this.imageWidth = W;
        this.imageHeight = H;
        //?}
        this.titleLabelX = -1000;
        this.inventoryLabelX = -1000;
    }

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = this.leftPos, y = this.topPos;
        NotchWidgets.panel(ctx, x, y, W, H);

        // Raffle is the one screen that keeps a gold title (its identity).
        int round = menu.prop(RaffleScreenHandler.P_ROUND);
        ctx.drawCenteredString(this.font,
                Component.literal("RAFFLE #" + round).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                x + W / 2, y + 8, 0xFFFFFFFF);

        boolean enabled = menu.prop(RaffleScreenHandler.P_ENABLED) == 1;

        if (!enabled) {
            drawPlaceholder(ctx, x, y);
        } else {
            boolean hasItem = menu.prop(RaffleScreenHandler.P_HAS_ITEM) == 1;
            ItemStack prize = hasItem ? menu.getPrizeStack() : new ItemStack(ModItems.NOTCH_COIN);

            drawGlow(ctx, x + CASE_X + CASE_W / 2, y + CASE_Y + CASE_H / 2);
            drawCase(ctx, x + CASE_X, y + CASE_Y, CASE_W, CASE_H, false);

            net.fugginbeenus.notchcurrency.compat.Render.pushGui(ctx);
            net.fugginbeenus.notchcurrency.compat.Render.translateGui(ctx, x + CASE_X + CASE_W / 2f - 16f, y + CASE_Y + CASE_H / 2f - 16f);
            net.fugginbeenus.notchcurrency.compat.Render.scaleGui(ctx, 2f, 2f);
            ctx.renderItem(prize, 0, 0);
            if (prize.getCount() > 1) ctx.renderItemDecorations(this.font, prize, 0, 0);
            net.fugginbeenus.notchcurrency.compat.Render.popGui(ctx);

            // Prize name: white hero text (readable on the light panel).
            String prizeName = hasItem ? prize.getHoverName().getString() : "Coin Jackpot";
            NotchWidgets.centerText(ctx, this.font, prizeName, x + W / 2 - 0, y + 80, NotchTheme.TEXT_LIGHT, true);

            // Total coin prize = ticket pot + the admin's guaranteed coins pool.
            long pot = menu.prop(RaffleScreenHandler.P_POT) & 0xFFFFFFFFL;
            long guaranteed = menu.prop(RaffleScreenHandler.P_COINS_POOL) & 0xFFFFFFFFL;
            NotchWidgets.centerText(ctx, this.font, "Prize: " + (pot + guaranteed) + " " + NotchWidgets.coinName(), x + W / 2, y + 92, NotchTheme.TEXT_DARK, false);

            NotchWidgets.divider(ctx, x + 12, y + 104, W - 24);

            int price = menu.prop(RaffleScreenHandler.P_PRICE);
            int yours = menu.prop(RaffleScreenHandler.P_YOURS);
            int total = menu.prop(RaffleScreenHandler.P_TOTAL);
            String odds = (total > 0 && yours > 0) ? String.format("%.1f%%", 100.0 * yours / total) : "0%";

            ctx.drawString(this.font, "Ticket price: " + price + " " + NotchWidgets.coinName(), x + 14, y + 112, NotchTheme.TEXT_DARK, false);
            ctx.drawString(this.font, "Your entries: " + yours + "  (" + odds + " to win)", x + 14, y + 124, NotchTheme.TEXT_DARK, false);
            ctx.drawString(this.font, "Total entries: " + total, x + 14, y + 136, NotchTheme.TEXT_DARK, false);

            NotchWidgets.primaryButton(ctx, this.font, x + BUY1_X, y + BUY_Y, BUY_W, BUY_H, "Buy 1", over(mouseX, mouseY, x + BUY1_X, y + BUY_Y, BUY_W, BUY_H));
            NotchWidgets.primaryButton(ctx, this.font, x + BUY5_X, y + BUY_Y, BUY_W, BUY_H, "Buy 5", over(mouseX, mouseY, x + BUY5_X, y + BUY_Y, BUY_W, BUY_H));
            NotchWidgets.primaryButton(ctx, this.font, x + BUY10_X, y + BUY_Y, BUY_W, BUY_H, "Buy 10", over(mouseX, mouseY, x + BUY10_X, y + BUY_Y, BUY_W, BUY_H));
        }

        if (menu.prop(RaffleScreenHandler.P_HAS_CLAIM) == 1) {
            NotchWidgets.goldButton(ctx, this.font, x + CLAIM_X, y + CLAIM_Y, CLAIM_W, CLAIM_H,
                    "CLAIM PRIZE", over(mouseX, mouseY, x + CLAIM_X, y + CLAIM_Y, CLAIM_W, CLAIM_H));
        }

        if (menu.prop(RaffleScreenHandler.P_LOSERS) > 0 && menu.prop(RaffleScreenHandler.P_CAN_REDEEM) == 1) {
            NotchWidgets.neutralButton(ctx, this.font, x + REDEEM_X, y + REDEEM_Y, REDEEM_W, REDEEM_H,
                    "Redeem an old ticket", over(mouseX, mouseY, x + REDEEM_X, y + REDEEM_Y, REDEEM_W, REDEEM_H));
        }
    }

    private void drawPlaceholder(GuiGraphics ctx, int x, int y) {
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
        NotchWidgets.centerText(ctx, this.font, "No raffle running", x + W / 2, y + 86, NotchTheme.TEXT_DARK, false);
        NotchWidgets.centerText(ctx, this.font, "Check back soon!", x + W / 2, y + 100, NotchTheme.TEXT_MUTED, false);
    }

    private void drawGlow(GuiGraphics ctx, int cx, int cy) {
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

    private void sparkle(GuiGraphics ctx, int x, int y, int s, int color) {
        ctx.fill(x - s, y, x + s + 1, y + 1, color);
        ctx.fill(x, y - s, x + 1, y + s + 1, color);
    }

    private void drawCase(GuiGraphics ctx, int x, int y, int w, int h, boolean muted) {
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
    protected void renderLabels(GuiGraphics ctx, int mouseX, int mouseY) {
        // No default labels.
    }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    //?}
        //? if <1.21 {
        this.renderBackground(ctx);
        //?}
        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}

        if (menu.prop(RaffleScreenHandler.P_ENABLED) == 1
                && menu.prop(RaffleScreenHandler.P_HAS_ITEM) == 1
                && over(mouseX, mouseY, this.leftPos + CASE_X, this.topPos + CASE_Y, CASE_W, CASE_H)) {
            ItemStack prize = menu.getPrizeStack();
            if (!prize.isEmpty()) {
                ctx.renderTooltip(this.font, prize, mouseX, mouseY);
            }
        }
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
    *///?} else {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
    //?}
        if (button == 0 && this.minecraft != null && this.minecraft.gameMode != null) {
            int id = buttonAt(mouseX, mouseY);
            if (id >= 0) {
                NotchWidgets.click();
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private int buttonAt(double mx, double my) {
        boolean enabled = menu.prop(RaffleScreenHandler.P_ENABLED) == 1;
        if (enabled) {
            if (over((int) mx, (int) my, this.leftPos + BUY1_X, this.topPos + BUY_Y, BUY_W, BUY_H)) return 0;
            if (over((int) mx, (int) my, this.leftPos + BUY5_X, this.topPos + BUY_Y, BUY_W, BUY_H)) return 1;
            if (over((int) mx, (int) my, this.leftPos + BUY10_X, this.topPos + BUY_Y, BUY_W, BUY_H)) return 2;
        }
        if (menu.prop(RaffleScreenHandler.P_HAS_CLAIM) == 1
                && over((int) mx, (int) my, this.leftPos + CLAIM_X, this.topPos + CLAIM_Y, CLAIM_W, CLAIM_H)) return 3;
        if (menu.prop(RaffleScreenHandler.P_LOSERS) > 0 && menu.prop(RaffleScreenHandler.P_CAN_REDEEM) == 1
                && over((int) mx, (int) my, this.leftPos + REDEEM_X, this.topPos + REDEEM_Y, REDEEM_W, REDEEM_H)) return 4;
        return -1;
    }

    // The blur hook is handed the graphics now instead of the partial tick.
    //? if >=1.21.11 {
    /*@Override
    protected void renderBlurredBackground(net.minecraft.client.gui.GuiGraphics ctx) {
        // No 1.21 menu blur behind the mod's screens. They draw crisp panels over the world.
    }
    *///?} elif >=1.21 {
    /*@Override
    protected void renderBlurredBackground(float delta) {
        // No 1.21 menu blur behind the mod's screens. They draw crisp panels over the world.
    }
    *///?}
}
