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

    private static final int SLOTS_X = 43, SLOTS_Y = 17, COL_X = 41, COL_W = 94, MONEY_Y = 49;
    public static final int WITHDRAW_FIELD_X = 41;
    public static final int WITHDRAW_FIELD_Y = 84;
    public static final int WITHDRAW_FIELD_W = 62;
    public static final int WITHDRAW_FIELD_H = 16;
    public static final int WITHDRAW_BUTTON_X = 107;
    public static final int WITHDRAW_BUTTON_Y = 84;
    public static final int WITHDRAW_BUTTON_W = 28;
    public static final int WITHDRAW_BUTTON_H = 16;

    private EditBox withdrawField;
    private Button withdrawButton;

    public ATMScreen(ATMTestScreenHandler handler, Inventory inv, Component title) {
        //? if >=26.1 {
        /*super(handler, inv, title, 176, 200);
        *///?} else {
        super(handler, inv, title);
        this.imageWidth = 176;
        this.imageHeight = 200;
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

        withdrawField = new EditBox(
                this.font,
                x + WITHDRAW_FIELD_X + 4,
                y + WITHDRAW_FIELD_Y + 4,
                WITHDRAW_FIELD_W - 8,
                WITHDRAW_FIELD_H - 6,
                Component.literal("Withdraw")
        );
        withdrawField.setMaxLength(12);
        withdrawField.setValue("");
        withdrawField.setBordered(false);
        withdrawField.setTextColor(0xFFFFFFFF);

        this.addRenderableWidget(withdrawField);
        this.setInitialFocus(withdrawField);
        withdrawField.setFocused(true);

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
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = (this.width - this.imageWidth) / 2;
        final int y = (this.height - this.imageHeight) / 2;
        final int cx = x + this.imageWidth / 2;

        NotchWidgets.panel(ctx, x, y, this.imageWidth, this.imageHeight);

        int sx = x + SLOTS_X, sy = y + SLOTS_Y;
        NotchWidgets.roundBox(ctx, sx - 2, sy - 2, 94, 22, NotchTheme.OUTLINE);
        ctx.fill(sx - 1, sy - 1, sx + 91, sy + 19, NotchTheme.ATM_GOLD_LO);
        ctx.fill(sx - 1, sy - 1, sx + 91, sy, NotchTheme.ATM_GOLD);
        for (int i = 0; i < 5; i++) {
            NotchWidgets.slot(ctx, sx + i * 18, sy);
        }
        ctx.fill(cx - 1, y + 40, cx + 1, y + 43, NotchTheme.SLOT_FILL);
        for (int i = 0; i < 3; i++) {
            ctx.fill(cx - 3 + i, y + 43 + i, cx + 3 - i, y + 44 + i, NotchTheme.SLOT_FILL);
        }

        long bal = NotchHud.getBalance();
        String text = String.format(java.util.Locale.ROOT, "%,d", bal);
        if (this.font.width(text) > COL_W - 24) text = NotchWidgets.compactCount(bal);
        moneyBar(ctx, x + COL_X, y + MONEY_Y, text);

        NotchWidgets.smallCaps(ctx, "Withdraw", x + WITHDRAW_FIELD_X, y + 77, NotchTheme.TEXT_MUTED);
        NotchWidgets.inset(ctx, x + WITHDRAW_FIELD_X, y + WITHDRAW_FIELD_Y,
                WITHDRAW_FIELD_W, WITHDRAW_FIELD_H, NotchTheme.DEEP);
        int bx = x + WITHDRAW_BUTTON_X, by = y + WITHDRAW_BUTTON_Y;
        boolean hov = mouseX >= bx && mouseX < bx + WITHDRAW_BUTTON_W
                && mouseY >= by && mouseY < by + WITHDRAW_BUTTON_H;
        withdrawButton(ctx, bx, by, hov);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                NotchWidgets.slot(ctx, x + 7 + col * 18, y + 112 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            NotchWidgets.slot(ctx, x + 7 + col * 18, y + 170);
        }
        NotchWidgets.title(ctx, this.font, "ATM", cx, y + 6, NotchWidgets.Plate.BRASS);
        //? if >=26.1 {
        /*super.extractContents(ctx, mouseX, mouseY, delta);
        *///?}
    }

    private void moneyBar(GuiGraphics ctx, int bx, int by, String text) {
        int fx = bx + 8, fw = COL_W - 8;
        NotchWidgets.roundBox(ctx, fx, by + 3, fw, 11, NotchTheme.OUTLINE);
        ctx.fill(fx + 1, by + 4, fx + fw - 1, by + 13, NotchTheme.DEEP);
        ctx.fill(fx + 1, by + 4, fx + fw - 1, by + 5, 0xFF181818);
        ctx.renderItem(NotchWidgets.coinStack(), bx, by);
        int tw = this.font.width(text) - 1;
        ctx.drawString(this.font, text, bx + 18 + (COL_W - 21 - tw) / 2, by + 5, NotchWidgets.COIN_GOLD, true);
    }

    private static void withdrawButton(GuiGraphics ctx, int bx, int by, boolean hovered) {
        NotchWidgets.colorButton(ctx, bx, by, WITHDRAW_BUTTON_W, WITHDRAW_BUTTON_H,
                NotchTheme.ACCENT_GREEN, NotchTheme.GREEN_HI, NotchTheme.GREEN_LO, hovered);
        int cx = bx + WITHDRAW_BUTTON_W / 2;
        downArrow(ctx, cx + 1, by + 1, NotchTheme.GREEN_LO);
        downArrow(ctx, cx, by, NotchTheme.TEXT_LIGHT);
    }

    private static void downArrow(GuiGraphics ctx, int cx, int by, int color) {
        ctx.fill(cx - 1, by + 3, cx + 1, by + 8, color);
        for (int i = 0; i < 4; i++) {
            ctx.fill(cx - 4 + i, by + 8 + i, cx + 4 - i, by + 9 + i, color);
        }
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
    // 26.2 dropped the modifier bits from a character event; nothing here reads them.
    //? if >=26.1 {
    /*@Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        char chr = (char) event.codepoint();
        int modifiers = 0;
    *///?} elif >=1.21.11 {
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
