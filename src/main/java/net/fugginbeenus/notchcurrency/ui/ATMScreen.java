package net.fugginbeenus.notchcurrency.ui;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ATMScreen extends HandledScreen<ATMTestScreenHandler> {
    private static final Identifier ATM_BG = new Identifier(NotchCurrency.MOD_ID, "textures/gui/atm.png");

    // --- Pill bounds (relative to GUI top-left) ---
    private static final int PILL_X = 2;
    private static final int PILL_Y = 53;
    private static final int PILL_W = 124;
    private static final int PILL_H = 16;

    // --- Icon inside the pill (nudge to match your art) ---
    private static final int PILL_ICON_W = 10;
    private static final int PILL_ICON_H = 10;
    private static final int PILL_ICON_X = PILL_X + 6;
    private static final int PILL_ICON_Y = PILL_Y + 3;

    // --- Text layout controls ---
    // We left-align near the icon so text grows toward the right side of the pill.
    private static final int TEXT_LEFT_PADDING = 4;  // gap between icon and text
    private static final int RIGHT_MARGIN = 6;       // keep space from pill's right edge

    // Pixel nudges
    private static final int TEXT_X_OFFSET = 42;      // +right / -left
    private static final int TEXT_Y_OFFSET = 1;      // +down  / -up

    public ATMScreen(ATMTestScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;

        // Hide vanilla titles
        this.titleX = 9999;
        this.titleY = 9999;
        this.playerInventoryTitleX = 9999;
        this.playerInventoryTitleY = 9999;
    }

    @Override
    protected void init() {
        super.init();
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = (this.width - this.backgroundWidth) / 2;
        final int y = (this.height - this.backgroundHeight) / 2;

        // Draw GUI panel
        ctx.drawTexture(ATM_BG, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight);

        // ----- Balance text that grows to the RIGHT -----
        final String bal = String.valueOf(this.handler.getSyncedBalance());
        final int textH = this.textRenderer.fontHeight;
        final int textW = this.textRenderer.getWidth(bal);

        // Left-anchored start (just right of the icon)
        final int minLeft = x + PILL_ICON_X + PILL_ICON_W + TEXT_LEFT_PADDING + TEXT_X_OFFSET;
        // Right limit (stay inside pill)
        final int maxRight = x + PILL_X + PILL_W - RIGHT_MARGIN;

        // Default drawX = minLeft (grow right). If it would exceed maxRight, shift left just enough.
        int drawX = minLeft;
        if (drawX + textW > maxRight) {
            drawX = Math.max(minLeft, maxRight - textW);
        }

        // Vertical center + nudge
        final int drawY = y + PILL_Y + (PILL_H - textH) / 2 + TEXT_Y_OFFSET;

        ctx.drawText(this.textRenderer, bal, drawX, drawY, 0xFFFFFF, true);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        super.drawForeground(ctx, mouseX, mouseY);
    }
}
