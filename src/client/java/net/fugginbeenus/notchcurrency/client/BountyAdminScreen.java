package net.fugginbeenus.notchcurrency.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyAdminScreenHandler;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

/**
 * Op-only bounty setup GUI (opened via {@code /bounty admin}). Edit the board settings and place
 * decree items in the slots to gate which categories generate (empty = all). Save &amp; Apply
 * persists; Regenerate rolls a fresh set now.
 */
public class BountyAdminScreen extends HandledScreen<BountyAdminScreenHandler> {

    private static final int W = 200, H = 246;
    private static final int FIELD_X = 120, FIELD_W = 68, FIELD_H = 14;
    private static final int ACTIVE_Y = 70, LIMIT_Y = 86, DUR_Y = 102;
    private static final int TG_X = 120, TG_Y = 54, TG_W = 68, TG_H = 14;
    private static final int SV_X = 12, SV_Y = 120, SV_W = 176, SV_H = 16;
    private static final int RG_X = 12, RG_Y = 140, RG_W = 176, RG_H = 14;

    private TextFieldWidget activeField, limitField, durField;
    private boolean enabledToggle = false;
    private boolean prefilled = false;

    public BountyAdminScreen(BountyAdminScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = W;
        this.backgroundHeight = H;
        this.titleX = -1000;
        this.playerInventoryTitleX = -1000;
    }

    @Override
    protected void init() {
        super.init();
        activeField = digitField(ACTIVE_Y);
        limitField = digitField(LIMIT_Y);
        durField = digitField(DUR_Y);
        addDrawableChild(activeField);
        addDrawableChild(limitField);
        addDrawableChild(durField);
    }

    private TextFieldWidget digitField(int fy) {
        TextFieldWidget f = new TextFieldWidget(this.textRenderer, this.x + FIELD_X + 2, this.y + fy + 3,
                FIELD_W - 4, FIELD_H - 5, Text.empty());
        f.setMaxLength(6);
        f.setDrawsBackground(false);
        f.setTextPredicate(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        return f;
    }

    private void prefillIfReady() {
        if (prefilled) return;
        if (handler.prop(BountyAdminScreenHandler.A_DURATION) <= 0) return;
        activeField.setText(String.valueOf(handler.prop(BountyAdminScreenHandler.A_ACTIVE)));
        limitField.setText(String.valueOf(handler.prop(BountyAdminScreenHandler.A_LIMIT)));
        durField.setText(String.valueOf(handler.prop(BountyAdminScreenHandler.A_DURATION)));
        enabledToggle = handler.prop(BountyAdminScreenHandler.A_ENABLED) == 1;
        prefilled = true;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        prefillIfReady();
        final int x = this.x, y = this.y;
        NotchWidgets.panel(ctx, x, y, W, H);

        NotchWidgets.title(ctx, this.textRenderer, "Bounty Setup", x + W / 2, y + 8);

        ctx.drawText(this.textRenderer, "Decrees (empty = all categories):", x + 10, y + 22, NotchTheme.TEXT_DARK, false);
        for (int i = 0; i < BountyAdminScreenHandler.DECREE_SLOTS; i++) {
            NotchWidgets.slot(ctx, x + BountyAdminScreenHandler.DECREE_X - 1 + i * 18, y + BountyAdminScreenHandler.DECREE_Y - 1);
        }

        ctx.drawText(this.textRenderer, "Enabled:", x + 12, y + TG_Y + 3, NotchTheme.TEXT_DARK, false);
        boolean tgHov = over(mouseX, mouseY, x + TG_X, y + TG_Y, TG_W, TG_H);
        if (enabledToggle) {
            NotchWidgets.primaryButton(ctx, this.textRenderer, x + TG_X, y + TG_Y, TG_W, TG_H, "ON", tgHov);
        } else {
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + TG_X, y + TG_Y, TG_W, TG_H, "OFF", tgHov);
        }

        field(ctx, x, y, "Live bounties:", ACTIVE_Y);
        field(ctx, x, y, "Take limit:", LIMIT_Y);
        field(ctx, x, y, "Duration (min):", DUR_Y);

        NotchWidgets.primaryButton(ctx, this.textRenderer, x + SV_X, y + SV_Y, SV_W, SV_H, "Save & Apply",
                over(mouseX, mouseY, x + SV_X, y + SV_Y, SV_W, SV_H));
        NotchWidgets.neutralButton(ctx, this.textRenderer, x + RG_X, y + RG_Y, RG_W, RG_H, "Regenerate Now",
                over(mouseX, mouseY, x + RG_X, y + RG_Y, RG_W, RG_H));

        NotchWidgets.divider(ctx, x + 8, y + 160, W - 16);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                NotchWidgets.slot(ctx, x + BountyAdminScreenHandler.INV_X + col * 18 - 1,
                        y + BountyAdminScreenHandler.INV_Y + row * 18 - 1);
            }
        }
        for (int col = 0; col < 9; col++) {
            NotchWidgets.slot(ctx, x + BountyAdminScreenHandler.INV_X + col * 18 - 1,
                    y + BountyAdminScreenHandler.HOTBAR_Y - 1);
        }
    }

    private void field(DrawContext ctx, int x, int y, String label, int fy) {
        ctx.drawText(this.textRenderer, label, x + 12, y + fy + 3, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + FIELD_X, y + fy, FIELD_W, FIELD_H, NotchTheme.DEEP);
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        // No default labels.
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.client != null && this.client.interactionManager != null) {
            int mx = (int) mouseX, my = (int) mouseY;
            if (over(mx, my, this.x + TG_X, this.y + TG_Y, TG_W, TG_H)) { NotchWidgets.tick(); enabledToggle = !enabledToggle; return true; }
            if (over(mx, my, this.x + SV_X, this.y + SV_Y, SV_W, SV_H)) { NotchWidgets.click(); save(); return true; }
            if (over(mx, my, this.x + RG_X, this.y + RG_Y, RG_W, RG_H)) {
                NotchWidgets.click();
                this.client.interactionManager.clickButton(this.handler.syncId, 0);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void save() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(enabledToggle);
        buf.writeVarInt((int) parse(activeField.getText(), 5));
        buf.writeVarInt((int) parse(limitField.getText(), 3));
        buf.writeVarInt((int) parse(durField.getText(), 30));
        ClientPlayNetworking.send(NotchPackets.BOUNTY_ADMIN_SAVE, buf);
    }

    private long parse(String s, long fallback) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, activeField, limitField, durField)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
