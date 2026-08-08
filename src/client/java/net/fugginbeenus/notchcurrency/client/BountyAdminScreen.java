package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.compat.NetClient;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyAdminScreenHandler;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class BountyAdminScreen extends AbstractContainerScreen<BountyAdminScreenHandler> {

    private static final int W = 200, H = 246;
    private static final int FIELD_X = 120, FIELD_W = 68, FIELD_H = 14;
    private static final int ACTIVE_Y = 70, LIMIT_Y = 86, DUR_Y = 102;
    private static final int TG_X = 120, TG_Y = 54, TG_W = 68, TG_H = 14;
    private static final int SV_X = 12, SV_Y = 120, SV_W = 176, SV_H = 16;
    private static final int RG_X = 12, RG_Y = 140, RG_W = 176, RG_H = 14;

    private EditBox activeField, limitField, durField;
    private boolean enabledToggle = false;
    private boolean prefilled = false;

    public BountyAdminScreen(BountyAdminScreenHandler handler, Inventory inv, Component title) {
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

    @Override
    protected void init() {
        super.init();
        activeField = digitField(ACTIVE_Y);
        limitField = digitField(LIMIT_Y);
        durField = digitField(DUR_Y);
        addRenderableWidget(activeField);
        addRenderableWidget(limitField);
        addRenderableWidget(durField);
    }

    private EditBox digitField(int fy) {
        EditBox f = new EditBox(this.font, this.leftPos + FIELD_X + 2, this.topPos + fy + 3,
                FIELD_W - 4, FIELD_H - 5, Component.empty());
        f.setMaxLength(6);
        f.setBordered(false);
        net.fugginbeenus.notchcurrency.compat.Render.setFilter(f, s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        return f;
    }

    private void prefillIfReady() {
        if (prefilled) return;
        if (menu.prop(BountyAdminScreenHandler.A_DURATION) <= 0) return;
        activeField.setValue(String.valueOf(menu.prop(BountyAdminScreenHandler.A_ACTIVE)));
        limitField.setValue(String.valueOf(menu.prop(BountyAdminScreenHandler.A_LIMIT)));
        durField.setValue(String.valueOf(menu.prop(BountyAdminScreenHandler.A_DURATION)));
        enabledToggle = menu.prop(BountyAdminScreenHandler.A_ENABLED) == 1;
        prefilled = true;
    }

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        prefillIfReady();
        final int x = this.leftPos, y = this.topPos;
        NotchWidgets.panel(ctx, x, y, W, H);

        NotchWidgets.title(ctx, this.font, "Bounty Setup", x + W / 2, y + 8);

        ctx.drawString(this.font, "Decrees (empty = all categories):", x + 10, y + 22, NotchTheme.TEXT_DARK, false);
        for (int i = 0; i < BountyAdminScreenHandler.DECREE_SLOTS; i++) {
            NotchWidgets.slot(ctx, x + BountyAdminScreenHandler.DECREE_X - 1 + i * 18, y + BountyAdminScreenHandler.DECREE_Y - 1);
        }

        ctx.drawString(this.font, "Enabled:", x + 12, y + TG_Y + 3, NotchTheme.TEXT_DARK, false);
        boolean tgHov = over(mouseX, mouseY, x + TG_X, y + TG_Y, TG_W, TG_H);
        if (enabledToggle) {
            NotchWidgets.primaryButton(ctx, this.font, x + TG_X, y + TG_Y, TG_W, TG_H, "ON", tgHov);
        } else {
            NotchWidgets.neutralButton(ctx, this.font, x + TG_X, y + TG_Y, TG_W, TG_H, "OFF", tgHov);
        }

        field(ctx, x, y, "Live bounties:", ACTIVE_Y);
        field(ctx, x, y, "Take limit:", LIMIT_Y);
        field(ctx, x, y, "Duration (min):", DUR_Y);

        NotchWidgets.primaryButton(ctx, this.font, x + SV_X, y + SV_Y, SV_W, SV_H, "Save & Apply",
                over(mouseX, mouseY, x + SV_X, y + SV_Y, SV_W, SV_H));
        NotchWidgets.neutralButton(ctx, this.font, x + RG_X, y + RG_Y, RG_W, RG_H, "Regenerate Now",
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

    private void field(GuiGraphics ctx, int x, int y, String label, int fy) {
        ctx.drawString(this.font, label, x + 12, y + fy + 3, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + FIELD_X, y + fy, FIELD_W, FIELD_H, NotchTheme.DEEP);
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    @Override
    protected void renderLabels(GuiGraphics ctx, int mouseX, int mouseY) {
        // No default labels.
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
            int mx = (int) mouseX, my = (int) mouseY;
            if (over(mx, my, this.leftPos + TG_X, this.topPos + TG_Y, TG_W, TG_H)) { NotchWidgets.tick(); enabledToggle = !enabledToggle; return true; }
            if (over(mx, my, this.leftPos + SV_X, this.topPos + SV_Y, SV_W, SV_H)) { NotchWidgets.click(); save(); return true; }
            if (over(mx, my, this.leftPos + RG_X, this.topPos + RG_Y, RG_W, RG_H)) {
                NotchWidgets.click();
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private void save() {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeBoolean(enabledToggle);
        buf.writeVarInt((int) parse(activeField.getValue(), 5));
        buf.writeVarInt((int) parse(limitField.getValue(), 3));
        buf.writeVarInt((int) parse(durField.getValue(), 30));
        NetClient.sendToServer(NotchPackets.BOUNTY_ADMIN_SAVE, buf);
    }

    private long parse(String s, long fallback) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
    *///?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, activeField, limitField, durField)) return true;
        //? if >=1.21.11 {
        /*return super.keyPressed(event);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
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
