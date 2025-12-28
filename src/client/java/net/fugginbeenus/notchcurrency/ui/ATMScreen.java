package net.fugginbeenus.notchcurrency.ui;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class ATMScreen extends HandledScreen<ATMTestScreenHandler> {

    private static final Identifier ATM_BG =
            new Identifier(NotchCurrency.MOD_ID, "textures/gui/atm.png");

    // === BALANCE PILL (top dark bar with coin + balance text) ===

    // Outer pill frame
    private static final int PILL_X = 36;
    private static final int PILL_Y = 52;
    private static final int PILL_W = 104;
    private static final int PILL_H = 16;

    // Coin icon inside the pill
    private static final int PILL_ICON_W = 10;
    private static final int PILL_ICON_H = 10;
    private static final int PILL_ICON_X = PILL_X + 4;
    private static final int PILL_ICON_Y = PILL_Y + 3;

    // Text layout
    private static final int TEXT_LEFT_PADDING = 4;
    private static final int RIGHT_MARGIN      = 6;
    private static final int TEXT_X_OFFSET     = 8;
    private static final int TEXT_Y_OFFSET     = 2;

    // === WITHDRAW ROW (black bar + green button under the pill) ===

    // Black input bar
    public static final int WITHDRAW_FIELD_X = 48;
    public static final int WITHDRAW_FIELD_Y = 90;
    public static final int WITHDRAW_FIELD_W = 62;
    public static final int WITHDRAW_FIELD_H = 16;

    // Green arrow button
    public static final int WITHDRAW_BUTTON_W = 29;
    public static final int WITHDRAW_BUTTON_H = 17;
    public static final int WITHDRAW_BUTTON_X = WITHDRAW_FIELD_X + WITHDRAW_FIELD_W - -7;
    public static final int WITHDRAW_BUTTON_Y = WITHDRAW_FIELD_Y + 1;

    // Debug toggles
    private static final boolean DEBUG_WITHDRAW_BUTTON_BOUNDS = false;
    private static final boolean DEBUG_WITHDRAW_FIELD_BOUNDS  = false;

    // ---------------------------------------------------------------------

    private TextFieldWidget withdrawField;
    private ButtonWidget withdrawButton;

    public ATMScreen(ATMTestScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth  = 176;
        this.backgroundHeight = 200; // taller texture

        this.titleX = 9999;
        this.titleY = 9999;
        this.playerInventoryTitleX = 9999;
        this.playerInventoryTitleY = 9999;
    }

    @Override
    protected void init() {
        super.init();

        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;

        // ---- Withdraw amount text field (sits on the black bar) ----
        withdrawField = new TextFieldWidget(
                this.textRenderer,
                x + WITHDRAW_FIELD_X,
                y + WITHDRAW_FIELD_Y,
                WITHDRAW_FIELD_W,
                WITHDRAW_FIELD_H,
                Text.literal("Withdraw")
        );
        withdrawField.setMaxLength(12);
        withdrawField.setText("");
        withdrawField.setDrawsBackground(false); // let the black bar texture show
        withdrawField.setEditableColor(0xFFFFFF);

        // IMPORTANT: add as drawable child so it renders and receives input
        this.addDrawableChild(withdrawField);
        this.setInitialFocus(withdrawField);
        withdrawField.setFocused(true); // ensure caret is blinking immediately

        // ---- Withdraw button (click area over the green arrow) ----
        withdrawButton = this.addDrawableChild(
                ButtonWidget.builder(Text.empty(), btn -> onWithdrawClicked())
                        .dimensions(
                                x + WITHDRAW_BUTTON_X,
                                y + WITHDRAW_BUTTON_Y,
                                WITHDRAW_BUTTON_W,
                                WITHDRAW_BUTTON_H
                        )
                        .build()
        );
        // Make the button visually invisible but keep the hitbox
        withdrawButton.setAlpha(0.0f);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = (this.width - this.backgroundWidth) / 2;
        final int y = (this.height - this.backgroundHeight) / 2;

        // Draw GUI panel
        ctx.drawTexture(ATM_BG, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight);

        // ----- Balance text inside the pill -----
        final String bal = String.valueOf(this.handler.getSyncedBalance());
        final int textH = this.textRenderer.fontHeight;
        final int textW = this.textRenderer.getWidth(bal);

        final int minLeft = x + PILL_ICON_X + PILL_ICON_W + TEXT_LEFT_PADDING + TEXT_X_OFFSET;
        final int maxRight = x + PILL_X + PILL_W - RIGHT_MARGIN;

        int drawX = minLeft;
        if (drawX + textW > maxRight) {
            drawX = Math.max(minLeft, maxRight - textW);
        }

        final int drawY = y + PILL_Y + (PILL_H - textH) / 2 + TEXT_Y_OFFSET;
        ctx.drawText(this.textRenderer, bal, drawX, drawY, 0xFFFFFF, true);

        // ----- Debug outline for withdraw button hitbox -----
        if (DEBUG_WITHDRAW_BUTTON_BOUNDS) {
            int bx1 = x + WITHDRAW_BUTTON_X;
            int by1 = y + WITHDRAW_BUTTON_Y;
            int bx2 = bx1 + WITHDRAW_BUTTON_W;
            int by2 = by1 + WITHDRAW_BUTTON_H;

            int color = 0x80FF0000; // semi-transparent red

            ctx.fill(bx1, by1, bx2, by1 + 1, color);       // top
            ctx.fill(bx1, by2 - 1, bx2, by2, color);       // bottom
            ctx.fill(bx1, by1, bx1 + 1, by2, color);       // left
            ctx.fill(bx2 - 1, by1, bx2, by2, color);       // right
        }

        // ----- Debug outline for withdraw field hitbox -----
        if (DEBUG_WITHDRAW_FIELD_BOUNDS) {
            int fx1 = x + WITHDRAW_FIELD_X;
            int fy1 = y + WITHDRAW_FIELD_Y;
            int fx2 = fx1 + WITHDRAW_FIELD_W;
            int fy2 = fy1 + WITHDRAW_FIELD_H;

            int color = 0x8000FF00; // semi-transparent green

            ctx.fill(fx1, fy1, fx2, fy1 + 1, color);       // top
            ctx.fill(fx1, fy2 - 1, fx2, fy2, color);       // bottom
            ctx.fill(fx1, fy1, fx1 + 1, fy2, color);       // left
            ctx.fill(fx2 - 1, fy1, fx2, fy2, color);       // right
        }
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        super.drawForeground(ctx, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        int slotIndex = this.focusedSlot != null ? this.focusedSlot.id : -1;
        if (slotIndex >= 0 && slotIndex < 5) {
            if (client != null && client.player != null) {
                client.player.playSound(
                        net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(),
                        0.6f, 1.0f
                );
            }
        }
        return handled;
    }

    // Text field typing
    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (withdrawField != null && withdrawField.charTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (withdrawField != null && withdrawField.keyPressed(keyCode, scanCode, modifiers)) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                onWithdrawClicked();
                return true;
            }
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    // ---------------------------------------------------------------------
    // Withdraw button logic: sends the requested amount to the server.
    // ---------------------------------------------------------------------
    private void onWithdrawClicked() {
        if (client == null || client.player == null) return;
        if (withdrawField == null) return;

        String raw = withdrawField.getText().trim();
        if (raw.isEmpty()) return;

        int amount;
        try {
            amount = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return;
        }

        if (amount <= 0) return;

        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(amount);
        ClientPlayNetworking.send(NotchPackets.ATM_WITHDRAW, buf);
    }
}
