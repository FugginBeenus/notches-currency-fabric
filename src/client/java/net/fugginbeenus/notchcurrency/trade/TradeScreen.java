package net.fugginbeenus.notchcurrency.trade;

import net.fugginbeenus.notchcurrency.compat.NetClient;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;

public class TradeScreen extends AbstractContainerScreen<TradeScreenHandler> {

    private static final ResourceLocation TEX = NotchCurrency.id("textures/gui/trade.png");

    // === Panel (your texture) ===
    private static final int TEX_U = 2, TEX_V = 2;
    private static final int PANEL_W = 226, PANEL_H = 215;
    private static final int TEX_W = 256, TEX_H = 256;

    // === Slot and UI layout (match handler/texture) ===
    private static final int SELF_GRID_X = 34;
    private static final int SELF_GRID_Y = 25;
    private static final int OTHER_GRID_X = 142;
    private static final int OTHER_GRID_Y = 25;

    private static final int PLAYER_INV_X = 34;
    private static final int PLAYER_INV_Y = 135;
    private static final int HOTBAR_Y     = PLAYER_INV_Y + 58;

    // Confirm (Ready/Unready) click zone (over your green art)
    private static final int CONFIRM_W = 55, CONFIRM_H = 20;
    private static final int CONFIRM_X = (PANEL_W - CONFIRM_W) / 2;
    private static final int CONFIRM_Y = 110;

    // Money pills (under each 3×3)
    private static final int PILL_W = 74, PILL_H = 16;
    private static final int LEFT_PILL_X  = SELF_GRID_X;
    private static final int LEFT_PILL_Y  = SELF_GRID_Y + 54 + 13;
    private static final int RIGHT_PILL_X = OTHER_GRID_X;
    private static final int RIGHT_PILL_Y = OTHER_GRID_Y + 54 + 13;

    // ---- New tweak knobs (safe to edit) ----
    // Nudge whole fields (relative to pill positions)
    private static final int LEFT_PILL_DX  = 2;  // +right / -left
    private static final int LEFT_PILL_DY  = -6;  // +down  / -up
    private static final int RIGHT_PILL_DX = 2;
    private static final int RIGHT_PILL_DY = -6;

    // Component padding inside each pill
    private static final int FIELD_INSET_X = 5;
    private static final int FIELD_INSET_Y = 1;

    // TextField size (kept narrower than pill)
    private static final int FIELD_W = 64, FIELD_H = 14;

    // Nudge the drawn CONFIRM/READY label (hitbox stays the same)
    private static final int CONFIRM_LABEL_DX = 2;
    private static final int CONFIRM_LABEL_DY = 1;

    // We keep the baked-in green button visible
    @SuppressWarnings("unused")
    private static final boolean HIDE_BUTTON_ART = false;

    private EditBox selfMoneyField;   // editable
    private EditBox otherMoneyField;  // read-only
    private boolean ready = false;

    public TradeScreen(TradeScreenHandler handler, Inventory inv, Component title) {
        super(handler, inv, title);
        this.imageWidth  = PANEL_W;
        this.imageHeight = PANEL_H;
        this.titleLabelX = this.titleLabelY = 9999;
        this.inventoryLabelX = this.inventoryLabelY = 9999;
    }

    @Override
    protected void init() {
        super.init();
        final int x = (this.width - imageWidth) / 2;
        final int y = (this.height - imageHeight) / 2;

        // --- Left (your offer) field ---
        selfMoneyField = new EditBox(
                this.font,
                x + LEFT_PILL_X + LEFT_PILL_DX + FIELD_INSET_X,
                y + LEFT_PILL_Y + LEFT_PILL_DY + FIELD_INSET_Y,
                FIELD_W, FIELD_H,
                Component.literal(""));
        selfMoneyField.setMaxLength(10);
        selfMoneyField.setValue("0");
        selfMoneyField.setBordered(false); // transparent: show pill art
        selfMoneyField.setVisible(true);
        selfMoneyField.setEditable(true);
        selfMoneyField.setResponder(s -> sendUpdate());
        addRenderableWidget(selfMoneyField);

        // --- Right (their offer) field (read-only mirror) ---
        otherMoneyField = new EditBox(
                this.font,
                x + RIGHT_PILL_X + RIGHT_PILL_DX + FIELD_INSET_X,
                y + RIGHT_PILL_Y + RIGHT_PILL_DY + FIELD_INSET_Y,
                FIELD_W, FIELD_H,
                Component.literal(""));
        otherMoneyField.setBordered(false);
        otherMoneyField.setEditable(false);
        otherMoneyField.setVisible(true);
        otherMoneyField.setFocused(false);
        addRenderableWidget(otherMoneyField);

        // --- Invisible Confirm/Ready hitbox over the green button art ---
        addRenderableWidget(new InvisibleButton(
                x + CONFIRM_X, y + CONFIRM_Y, CONFIRM_W, CONFIRM_H,
                this::toggleReady));

        setInitialFocus(selfMoneyField);
    }

    private void toggleReady() {
        ready = !ready;
        sendUpdate();
    }

    private void sendUpdate() {
        int money = 0;
        try {
            money = Integer.parseInt(selfMoneyField.getValue().trim());
        } catch (NumberFormatException ignored) {}
        var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        buf.writeVarInt(Math.max(0, money));
        buf.writeBoolean(ready);
        NetClient.sendToServer(NotchPackets.TRADE_UPDATE, buf);
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        // Keep the other player's amount mirrored into the right field
        int other = this.menu.getProperties().get(1);
        String want = Integer.toString(other);
        if (!want.equals(otherMoneyField.getValue())) {
            otherMoneyField.setValue(want);
        }

        //? if <1.21 {
        this.renderBackground(ctx);
        //?}
        super.render(ctx, mouseX, mouseY, delta);

        // Draw "CONFIRM"/"READY" label (independent nudge from hitbox)
        int bx = (this.width - imageWidth) / 2 + CONFIRM_X + CONFIRM_LABEL_DX;
        int by = (this.height - imageHeight) / 2 + CONFIRM_Y + CONFIRM_LABEL_DY;
        String label = ready ? "READY" : "CONFIRM";
        int lw = this.font.width(label);
        int lx = bx + (CONFIRM_W - lw) / 2;
        int ly = by + (CONFIRM_H - this.font.lineHeight) / 2;
        int color = ready ? 0x4CF06C : 0xFFFFFF; // green when ready
        ctx.drawString(this.font, label, lx, ly, color, false);

        renderTooltip(ctx, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;

        // Draw full panel background (includes baked-in button)
        ctx.blit(TEX, x, y, TEX_U, TEX_V, PANEL_W, PANEL_H, TEX_W, TEX_H);
    }

    // ----- Invisible clickable widget (renders nothing) -----
    private static final class InvisibleButton extends AbstractButton {
        private final Runnable onClick;

        InvisibleButton(int x, int y, int w, int h, Runnable onClick) {
            super(x, y, w, h, Component.empty());
            this.onClick = onClick;
        }
        @Override public void onPress() { onClick.run(); }
        //? if >=1.21 {
        /*@Override protected void renderWidget(GuiGraphics ctx, int mouseX, int mouseY, float delta) { }
        *///?} else {
        @Override protected void renderWidget(GuiGraphics ctx, int mouseX, int mouseY, float delta) { /* invisible */ }
        //?}
        @Override protected void updateWidgetNarration(NarrationElementOutput builder) { /* no narration */ }
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
    *///?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
        // Keep the screen from closing / hotbar-swapping while typing in a focused field.
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, selfMoneyField, otherMoneyField)) return true;
        //? if >=1.21.11 {
        /*return super.keyPressed(event);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
    }

    //? if >=1.21 {
    /*@Override
    protected void renderBlurredBackground(float delta) {
        // No 1.21 menu blur behind the mod's screens. They draw crisp panels over the world.
    }
    *///?}
}
