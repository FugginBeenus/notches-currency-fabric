package net.fugginbeenus.notchcurrency.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Draws the ATM texture and centers the balance text inside the black pill.
 */
public class ATMScreen extends HandledScreen<ATMTestScreenHandler> {

    // Your GUI background (you said file is atm.png)
    private static final Identifier ATM_BG =
            new Identifier(NotchCurrency.MOD_ID, "textures/gui/atm.png");

    // ---- Tweak these to match your art exactly ----
// --- balance “pill” box on your texture (relative to GUI origin) ---
    private static final int PILL_X = 0;   // ← left edge of the pill
    private static final int PILL_Y = 50;   // ← top edge of the pill
    private static final int PILL_W = 136;  // ← pill width
    private static final int PILL_H = 22;   // ← pill height
    // ------------------------------------------------

    public ATMScreen(ATMTestScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = 176;
        this.backgroundHeight = 166;
    }

    @Override
    protected void init() {
        super.init();
        // Hide default titles (“Deposit” and “Inventory”) so the art looks clean
        this.titleX = 10000;
        this.playerInventoryTitleX = 10000;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // 1) Draw full background
        ctx.drawTexture(ATM_BG, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight);

        // 2) Draw balance centered inside the black pill
        String bal = String.valueOf(this.handler.getBalance());
        int textW = this.textRenderer.getWidth(bal);
        int textH = this.textRenderer.fontHeight;

        int pillLeft   = x + PILL_X;
        int pillTop    = y + PILL_Y;
        int pillCenterX = pillLeft + (PILL_W / 2);
        int pillCenterY = pillTop  + (PILL_H / 2);

        int drawX = pillCenterX - (textW / 2);
        int drawY = pillCenterY - (textH / 2);

        ctx.drawText(this.textRenderer, bal, drawX, drawY, 0xFFFFFFFF, true);

        // (Optional) If you later add an icon sprite, draw it here:
        // Identifier ICON = new Identifier(NotchCurrency.MOD_ID, "textures/gui/notch_gem.png");
        // ctx.drawTexture(ICON, pillLeft + 6, pillTop + 6, 0, 0, 9, 9, 9, 9);
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        // intentionally empty (we hide titles in init)
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }
}
