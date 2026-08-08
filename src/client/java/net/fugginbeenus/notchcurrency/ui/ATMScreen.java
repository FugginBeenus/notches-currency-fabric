package net.fugginbeenus.notchcurrency.ui;

import net.fugginbeenus.notchcurrency.compat.NetClient;
import net.fugginbeenus.notchcurrency.client.NotchHud;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.lwjgl.glfw.GLFW;

public class ATMScreen extends AbstractContainerScreen<ATMTestScreenHandler> {

    // === BALANCE PILL (top dark bar with coin + balance text) ===

    // Outer pill frame
    private static final int PILL_W = 74;
    private static final int PILL_H = 16;
    private static final int PILL_X = (176 - PILL_W) / 2; // centered on the 176-wide panel
    private static final int PILL_Y = 52;

    // Coin icon inside the pill
    private static final int PILL_ICON_W = 10;
    private static final int PILL_ICON_H = 10;
    private static final int PILL_ICON_X = PILL_X + 4;
    private static final int PILL_ICON_Y = PILL_Y + 3;

    // Component layout
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

    private EditBox withdrawField;
    private Button withdrawButton;

    public ATMScreen(ATMTestScreenHandler handler, Inventory inv, Component title) {
        //? if >=26.1 {
        /*super(handler, inv, title, 176, 200);
        *///?} else {
        super(handler, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 200; // taller texture
        //?}

        this.titleLabelX = 9999;
        this.titleLabelY = 9999;
        this.inventoryLabelX = 9999;
        this.inventoryLabelY = 9999;
    }

    @Override
    protected void init() {
        super.init();

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // ---- Withdraw amount text field (sits on the black bar) ----
        withdrawField = new EditBox(
                this.font,
                x + WITHDRAW_FIELD_X + 4,   // nudge text right off the rounded edge
                y + WITHDRAW_FIELD_Y + 4,   // nudge text down to vertical center
                WITHDRAW_FIELD_W - 7,
                WITHDRAW_FIELD_H,
                Component.literal("Withdraw")
        );
        withdrawField.setMaxLength(12);
        withdrawField.setValue("");
        withdrawField.setBordered(false); // let the black bar texture show
        withdrawField.setTextColor(0xFFFFFF);

        // IMPORTANT: add as drawable child so it renders and receives input
        this.addRenderableWidget(withdrawField);
        this.setInitialFocus(withdrawField);
        withdrawField.setFocused(true); // ensure caret is blinking immediately

        // ---- Withdraw button (click area over the green arrow) ----
        withdrawButton = this.addRenderableWidget(
                Button.builder(Component.empty(), btn -> onWithdrawClicked())
                        .bounds(
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
        this.renderTooltip(ctx, mouseX, mouseY);
    }

    //? if >=26.1 {
    /*@Override
    protected void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = (this.width - this.imageWidth) / 2;
        final int y = (this.height - this.imageHeight) / 2;

        // Window panel: code-drawn from the Notch theme (no texture).
        NotchWidgets.panel(ctx, x, y, this.imageWidth, this.imageHeight);
        ctx.drawString(this.font, Component.literal("ATM"), x + 8, y + 6, NotchTheme.TEXT_DARK, false);

        // Deposit slots (5 across). Item positions mirror the handler; insets sit 1px out.
        for (int i = 0; i < 5; i++) {
            NotchWidgets.slot(ctx, x + 45 + i * 18, y + 17);
        }

        // Balance pill (coin glyph + balance).
        NotchWidgets.pill(ctx, x + PILL_X, y + PILL_Y, PILL_W, PILL_H);
        String label = " " + NotchHud.getBalance();
        int tw = this.font.width(label);
        int tx = x + PILL_X + (PILL_W - tw) / 2;
        int ty = y + PILL_Y + (PILL_H - this.font.lineHeight) / 2 + 1;
        ctx.drawString(this.font, label, tx, ty, NotchTheme.TEXT_GOLD, true);

        // Withdraw amount box: the text field renders its text on top.
        NotchWidgets.inset(ctx, x + WITHDRAW_FIELD_X, y + WITHDRAW_FIELD_Y,
                WITHDRAW_FIELD_W, WITHDRAW_FIELD_H, NotchTheme.DEEP);

        // Withdraw button (green) with a down-arrow.
        int bx = x + WITHDRAW_BUTTON_X, by = y + WITHDRAW_BUTTON_Y;
        boolean hov = mouseX >= bx && mouseX < bx + WITHDRAW_BUTTON_W
                && mouseY >= by && mouseY < by + WITHDRAW_BUTTON_H;
        greenButton(ctx, bx, by, WITHDRAW_BUTTON_W, WITHDRAW_BUTTON_H, hov);

        // Player inventory + hotbar.
        final int invX = x + 8;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                NotchWidgets.slot(ctx, invX + col * 18 - 1, y + 113 + row * 18 - 1);
            }
        }
        for (int col = 0; col < 9; col++) {
            NotchWidgets.slot(ctx, invX + col * 18 - 1, y + 171 - 1);
        }
    }

    private void greenButton(GuiGraphics ctx, int bx, int by, int w, int h, boolean hovered) {
        int face = hovered ? 0xFF6FB85A : NotchTheme.ACCENT_GREEN;
        ctx.fill(bx, by, bx + w, by + h, NotchTheme.OUTLINE);
        ctx.fill(bx + 1, by + 1, bx + w - 1, by + h - 1, face);
        ctx.fill(bx + 1, by + 1, bx + w - 1, by + 2, 0xFF8FD07A);     // light top
        ctx.fill(bx + 1, by + 1, bx + 2, by + h - 1, 0xFF8FD07A);     // light left
        ctx.fill(bx + 1, by + h - 2, bx + w - 1, by + h - 1, 0xFF3C6E2F); // dark bottom
        ctx.fill(bx + w - 2, by + 1, bx + w - 1, by + h - 1, 0xFF3C6E2F); // dark right
        // downward arrow (withdraw = coins come to you)
        int cx = bx + w / 2, cy = by + h / 2 - 2;
        ctx.fill(cx - 3, cy, cx + 4, cy + 1, NotchTheme.TEXT_LIGHT);
        ctx.fill(cx - 2, cy + 1, cx + 3, cy + 2, NotchTheme.TEXT_LIGHT);
        ctx.fill(cx - 1, cy + 2, cx + 2, cy + 3, NotchTheme.TEXT_LIGHT);
        ctx.fill(cx, cy + 3, cx + 1, cy + 4, NotchTheme.TEXT_LIGHT);
    }

    @Override
    protected void renderLabels(GuiGraphics ctx, int mouseX, int mouseY) {
        super.renderLabels(ctx, mouseX, mouseY);
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
        //? if >=1.21.11 {
        /*boolean handled = super.mouseClicked(event, doubleClick);
        *///?} else {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        //?}
        int slotIndex = this.hoveredSlot != null ? this.hoveredSlot.index : -1;
        if (slotIndex >= 0 && slotIndex < 5) {
            if (minecraft != null && minecraft.player != null) {
                minecraft.player.playSound(
                        net.minecraft.sounds.SoundEvents.UI_BUTTON_CLICK.value(),
                        0.6f, 1.0f
                );
            }
        }
        return handled;
    }

    // Component field typing
    //? if >=1.21.11 {
    /*@Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        char chr = (char) event.codepoint();
        int modifiers = event.modifiers();
    *///?} else {
    @Override
    public boolean charTyped(char chr, int modifiers) {
    //?}
        //? if >=1.21.11 {
        /*if (withdrawField != null && withdrawField.charTyped(event)) {
        *///?} else {
        if (withdrawField != null && withdrawField.charTyped(chr, modifiers)) {
        //?}
            return true;
        }
        //? if >=1.21.11 {
        /*return super.charTyped(event);
        *///?} else {
        return super.charTyped(chr, modifiers);
        //?}
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
    *///?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
        //? if >=1.21.11 {
        /*if (withdrawField != null && withdrawField.keyPressed(event)) {
        *///?} else {
        if (withdrawField != null && withdrawField.keyPressed(keyCode, scanCode, modifiers)) {
        //?}
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
                onWithdrawClicked();
                return true;
            }
            return true;
        }
        //? if >=1.21.11 {
        /*return super.keyPressed(event);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
    }

    // ---------------------------------------------------------------------
    // Withdraw button logic: sends the requested amount to the server.
    // ---------------------------------------------------------------------
    private void onWithdrawClicked() {
        if (minecraft == null || minecraft.player == null) return;
        if (withdrawField == null) return;

        String raw = withdrawField.getValue().trim();
        if (raw.isEmpty()) return;

        int amount;
        try {
            amount = Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return;
        }

        if (amount <= 0) return;

        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeVarInt(amount);
        NetClient.sendToServer(NotchPackets.ATM_WITHDRAW, buf);
    }

    //? if >=1.21 {
    /*@Override
    protected void renderBlurredBackground(float delta) {
        // No 1.21 menu blur behind the mod's screens. They draw crisp panels over the world.
    }
    *///?}
}
