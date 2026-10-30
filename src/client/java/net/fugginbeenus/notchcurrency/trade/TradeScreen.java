package net.fugginbeenus.notchcurrency.trade;

import net.fugginbeenus.notchcurrency.compat.NetClient;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;

public class TradeScreen extends AbstractContainerScreen<TradeScreenHandler> {

    private static final int PANEL_W = 228, PANEL_H = 215;
    private static final int SELF_GRID_X = 34, OTHER_GRID_X = 142, GRID_Y = 25;
    private static final int PLAYER_INV_X = 34, PLAYER_INV_Y = 135, HOTBAR_Y = PLAYER_INV_Y + 58;
    private static final int LABEL_Y = 15, MONEY_Y = 84, MONEY_W = 70;
    private static final int SELF_MONEY_X = 33, OTHER_MONEY_X = 125;
    private static final int CONFIRM_W = 60, CONFIRM_H = 16;
    private static final int CONFIRM_X = (PANEL_W - CONFIRM_W) / 2, CONFIRM_Y = 108;
    private static final int READY_INK = 0xFF2E8B3A;

    private EditBox selfMoneyField;
    private boolean ready = false;

    public TradeScreen(TradeScreenHandler handler, Inventory inv, Component title) {
        //? if >=26.1 {
        /*super(handler, inv, title, PANEL_W, PANEL_H);
        *///?} else {
        super(handler, inv, title);
        this.imageWidth = PANEL_W;
        this.imageHeight = PANEL_H;
        //?}
        this.titleLabelX = this.titleLabelY = 9999;
        this.inventoryLabelX = this.inventoryLabelY = 9999;
    }

    @Override
    protected void init() {
        super.init();
        final int x = (this.width - imageWidth) / 2;
        final int y = (this.height - imageHeight) / 2;

        selfMoneyField = new EditBox(this.font, x + SELF_MONEY_X + 18, y + MONEY_Y + 5,
                MONEY_W - 21, 10, Component.literal(""));
        selfMoneyField.setMaxLength(10);
        selfMoneyField.setValue("0");
        selfMoneyField.setBordered(false);
        selfMoneyField.setTextColor(NotchWidgets.COIN_GOLD);
        selfMoneyField.setResponder(s -> sendUpdate());
        addRenderableWidget(selfMoneyField);
        addRenderableWidget(new InvisibleButton(
                x + CONFIRM_X, y + CONFIRM_Y, CONFIRM_W, CONFIRM_H,
                this::toggleReady));

        setInitialFocus(selfMoneyField);
    }

    private boolean selfReady() {
        return this.menu.getProperties().get(2) == 1;
    }

    private boolean otherReady() {
        return this.menu.getProperties().get(3) == 1;
    }

    private void toggleReady() {
        ready = !selfReady();
        sendUpdate();
    }

    private void sendUpdate() {
        int money = 0;
        try {
            money = Integer.parseInt(selfMoneyField.getValue().trim());
        } catch (NumberFormatException ignored) {}
        var buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeVarInt(Math.max(0, money));
        buf.writeBoolean(ready);
        NetClient.sendToServer(NotchPackets.TRADE_UPDATE, buf);
    }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    //?}
        ready = selfReady();
        //? if <1.21 {
        this.renderBackground(ctx);
        //?}
        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        extractTooltip(ctx, mouseX, mouseY);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        renderTooltip(ctx, mouseX, mouseY);
        //?}
    }

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        int x = (this.width - imageWidth) / 2;
        int y = (this.height - imageHeight) / 2;

        NotchWidgets.panel(ctx, x, y, PANEL_W, PANEL_H);
        side(ctx, x + SELF_GRID_X - 1, y, "You", selfReady());
        side(ctx, x + OTHER_GRID_X - 1, y, "Them", otherReady());
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 3; c++) {
                NotchWidgets.slot(ctx, x + SELF_GRID_X - 1 + c * 18, y + GRID_Y - 1 + r * 18);
                NotchWidgets.slot(ctx, x + OTHER_GRID_X - 1 + c * 18, y + GRID_Y - 1 + r * 18);
            }
        }
        int ax = x + PANEL_W / 2 - 7;
        NotchWidgets.arrowRight(ctx, ax, y + 42, NotchTheme.SLOT_FILL);
        arrowLeft(ctx, ax, y + 53, NotchTheme.SLOT_FILL);

        moneyPill(ctx, x + SELF_MONEY_X, y + MONEY_Y);
        moneyPill(ctx, x + OTHER_MONEY_X, y + MONEY_Y);
        long other = this.menu.getProperties().get(1);
        String shown = Long.toString(other);
        if (this.font.width(shown) > MONEY_W - 22) shown = NotchWidgets.compactCount(other);
        ctx.drawString(this.font, shown, x + OTHER_MONEY_X + 18, y + MONEY_Y + 5, NotchWidgets.COIN_GOLD, true);

        int bx = x + CONFIRM_X, by = y + CONFIRM_Y;
        boolean hov = mouseX >= bx && mouseX < bx + CONFIRM_W && mouseY >= by && mouseY < by + CONFIRM_H;
        if (selfReady()) {
            NotchWidgets.button(ctx, bx, by, CONFIRM_W, CONFIRM_H, hov, false);
            NotchWidgets.centerText(ctx, this.font, "Ready", bx + CONFIRM_W / 2, by + (CONFIRM_H - 8) / 2,
                    READY_INK, false);
        } else {
            NotchWidgets.primaryButton(ctx, this.font, bx, by, CONFIRM_W, CONFIRM_H, "Confirm", hov);
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                NotchWidgets.slot(ctx, x + PLAYER_INV_X - 1 + col * 18, y + PLAYER_INV_Y - 1 + row * 18);
            }
        }
        for (int col = 0; col < 9; col++) {
            NotchWidgets.slot(ctx, x + PLAYER_INV_X - 1 + col * 18, y + HOTBAR_Y - 1);
        }
        NotchWidgets.title(ctx, this.font, this.title.getString(), x + PANEL_W / 2, y + 6, NotchWidgets.Plate.BRASS);
        //? if >=26.1 {
        /*
        super.extractContents(ctx, mouseX, mouseY, delta);
        *///?}
    }

    private static void side(GuiGraphics ctx, int gx, int y, String label, boolean isReady) {
        NotchWidgets.smallCaps(ctx, label, gx, y + LABEL_Y, NotchTheme.TEXT_MUTED);
        if (isReady) {
            NotchWidgets.smallCaps(ctx, "Ready", gx + 54 - NotchWidgets.smallCapsWidth("Ready"), y + LABEL_Y, READY_INK);
        }
    }

    private static void moneyPill(GuiGraphics ctx, int mx, int my) {
        int fx = mx + 8, fw = MONEY_W - 8;
        NotchWidgets.roundBox(ctx, fx, my + 3, fw, 11, NotchTheme.OUTLINE);
        ctx.fill(fx + 1, my + 4, fx + fw - 1, my + 13, NotchTheme.DEEP);
        ctx.fill(fx + 1, my + 4, fx + fw - 1, my + 5, 0xFF181818);
        ctx.renderItem(NotchWidgets.coinStack(), mx, my);
    }

    private static void arrowLeft(GuiGraphics ctx, int x, int y, int color) {
        ctx.fill(x + 4, y + 3, x + 14, y + 5, color);
        for (int i = 0; i < 4; i++) {
            ctx.fill(x + 3 - i, y + i, x + 4 - i, y + 8 - i, color);
        }
    }

    private static final class InvisibleButton extends AbstractButton {
        private final Runnable onClick;

        InvisibleButton(int x, int y, int w, int h, Runnable onClick) {
            super(x, y, w, h, Component.empty());
            this.onClick = onClick;
        }
        //? if >=1.21.11 {
        /*@Override public void onPress(net.minecraft.client.input.InputWithModifiers input) { onClick.run(); }
        *///?} else {
        @Override public void onPress() { onClick.run(); }
        //?}
        //? if >=26.1 {
        /*@Override protected void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) { }
        *///?} elif >=1.21.11 {
        /*@Override protected void renderContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) { }
        *///?} elif >=1.21 {
        /*@Override protected void renderWidget(GuiGraphics ctx, int mouseX, int mouseY, float delta) { }
        *///?} else {
        @Override protected void renderWidget(GuiGraphics ctx, int mouseX, int mouseY, float delta) { }
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
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, selfMoneyField)) return true;
        //? if >=1.21.11 {
        /*return super.keyPressed(event);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
    }

    //? if >=1.21.11 {
    /*@Override
    protected void renderBlurredBackground(net.minecraft.client.gui.GuiGraphics ctx) {
    }
    *///?} elif >=1.21 {
    /*@Override
    protected void renderBlurredBackground(float delta) {
    }
    *///?}
}
