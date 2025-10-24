package net.fugginbeenus.notchcurrency.trade;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class TradeScreen extends HandledScreen<TradeScreenHandler> {

    private static final Identifier TEX = new Identifier("notchcurrency", "textures/gui/trade.png");

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

    // Text padding inside each pill
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

    private TextFieldWidget selfMoneyField;   // editable
    private TextFieldWidget otherMoneyField;  // read-only
    private boolean ready = false;

    public TradeScreen(TradeScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth  = PANEL_W;
        this.backgroundHeight = PANEL_H;
        this.titleX = this.titleY = 9999;
        this.playerInventoryTitleX = this.playerInventoryTitleY = 9999;
    }

    @Override
    protected void init() {
        super.init();
        final int x = (this.width - backgroundWidth) / 2;
        final int y = (this.height - backgroundHeight) / 2;

        // --- Left (your offer) field ---
        selfMoneyField = new TextFieldWidget(
                this.textRenderer,
                x + LEFT_PILL_X + LEFT_PILL_DX + FIELD_INSET_X,
                y + LEFT_PILL_Y + LEFT_PILL_DY + FIELD_INSET_Y,
                FIELD_W, FIELD_H,
                Text.literal(""));
        selfMoneyField.setMaxLength(10);
        selfMoneyField.setText("0");
        selfMoneyField.setDrawsBackground(false); // transparent: show pill art
        selfMoneyField.setVisible(true);
        selfMoneyField.setEditable(true);
        selfMoneyField.setChangedListener(s -> sendUpdate());
        addDrawableChild(selfMoneyField);

        // --- Right (their offer) field (read-only mirror) ---
        otherMoneyField = new TextFieldWidget(
                this.textRenderer,
                x + RIGHT_PILL_X + RIGHT_PILL_DX + FIELD_INSET_X,
                y + RIGHT_PILL_Y + RIGHT_PILL_DY + FIELD_INSET_Y,
                FIELD_W, FIELD_H,
                Text.literal(""));
        otherMoneyField.setDrawsBackground(false);
        otherMoneyField.setEditable(false);
        otherMoneyField.setVisible(true);
        otherMoneyField.setFocused(false);
        addDrawableChild(otherMoneyField);

        // --- Invisible Confirm/Ready hitbox over the green button art ---
        addDrawableChild(new InvisibleButton(
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
            money = Integer.parseInt(selfMoneyField.getText().trim());
        } catch (NumberFormatException ignored) {}
        var buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
        buf.writeVarInt(Math.max(0, money));
        buf.writeBoolean(ready);
        ClientPlayNetworking.send(NotchPackets.TRADE_UPDATE, buf);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Keep the other player's amount mirrored into the right field
        int other = this.handler.getProperties().get(1);
        String want = Integer.toString(other);
        if (!want.equals(otherMoneyField.getText())) {
            otherMoneyField.setText(want);
        }

        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);

        // Draw "CONFIRM"/"READY" label (independent nudge from hitbox)
        int bx = (this.width - backgroundWidth) / 2 + CONFIRM_X + CONFIRM_LABEL_DX;
        int by = (this.height - backgroundHeight) / 2 + CONFIRM_Y + CONFIRM_LABEL_DY;
        String label = ready ? "READY" : "CONFIRM";
        int lw = this.textRenderer.getWidth(label);
        int lx = bx + (CONFIRM_W - lw) / 2;
        int ly = by + (CONFIRM_H - this.textRenderer.fontHeight) / 2;
        int color = ready ? 0x4CF06C : 0xFFFFFF; // green when ready
        ctx.drawText(this.textRenderer, label, lx, ly, color, false);

        drawMouseoverTooltip(ctx, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        int x = (this.width - backgroundWidth) / 2;
        int y = (this.height - backgroundHeight) / 2;

        // Draw full panel background (includes baked-in button)
        ctx.drawTexture(TEX, x, y, TEX_U, TEX_V, PANEL_W, PANEL_H, TEX_W, TEX_H);
    }

    // ----- Invisible clickable widget (renders nothing) -----
    private static final class InvisibleButton extends PressableWidget {
        private final Runnable onClick;

        InvisibleButton(int x, int y, int w, int h, Runnable onClick) {
            super(x, y, w, h, Text.empty());
            this.onClick = onClick;
        }
        @Override public void onPress() { onClick.run(); }
        @Override protected void renderButton(DrawContext ctx, int mouseX, int mouseY, float delta) { /* invisible */ }
        @Override protected void appendClickableNarrations(NarrationMessageBuilder builder) { /* no narration */ }
    }
}
