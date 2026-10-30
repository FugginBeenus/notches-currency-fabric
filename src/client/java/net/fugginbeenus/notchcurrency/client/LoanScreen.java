package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.compat.NetClient;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.loan.LoanScreenHandler;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class LoanScreen extends AbstractContainerScreen<LoanScreenHandler> {

    private static final int W = 200, H = 168;
    private static final int FIELD_X = 60, FIELD_Y = 103, FIELD_W = 128;
    private static final int BORROW_X = 12, REPAY_X = 102, BTN_Y = 124, BTN_W = 86, BTN_H = 16;
    private static final int ALL_X = 12, ALL_Y = 144, ALL_W = 176, ALL_H = 14;

    private EditBox amountField;

    public LoanScreen(LoanScreenHandler handler, Inventory inv, Component title) {
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
        amountField = new EditBox(this.font, this.leftPos + FIELD_X + 18, this.topPos + FIELD_Y + 5,
                FIELD_W - 21, 9, Component.literal("Amount"));
        amountField.setMaxLength(12);
        amountField.setBordered(false);
        amountField.setTextColor(NotchWidgets.COIN_GOLD);
        amountField.setHint(Component.literal("amount").withStyle(ChatFormatting.DARK_GRAY));
        net.fugginbeenus.notchcurrency.compat.Render.setFilter(amountField, s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        addRenderableWidget(amountField);
        setInitialFocus(amountField);
    }

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = this.leftPos, y = this.topPos;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.font, "Loans", x + W / 2, y + 8, NotchWidgets.Plate.BRASS);

        if (menu.prop(LoanScreenHandler.P_ENABLED) == 0) {
            NotchWidgets.centerText(ctx, this.font, "Loans are disabled.", x + W / 2, y + 40, NotchTheme.TEXT_MUTED, false);
            return;
        }

        long debt = menu.prop(LoanScreenHandler.P_DEBT) & 0xFFFFFFFFL;
        long max = menu.prop(LoanScreenHandler.P_MAX) & 0xFFFFFFFFL;
        int interest = menu.prop(LoanScreenHandler.P_INTEREST);
        int term = menu.prop(LoanScreenHandler.P_TERM);
        int daysLeft = menu.prop(LoanScreenHandler.P_DAYS_LEFT);

        moneyRow(ctx, "You owe", debt, y + 21);
        moneyRow(ctx, "Borrowing limit", max, y + 39);
        ctx.drawString(this.font, "Interest: " + interest + "% per cycle", x + 12, y + 58, NotchTheme.TEXT_DARK, false);
        ctx.drawString(this.font, "Term: " + term + " days", x + 12, y + 70, NotchTheme.TEXT_DARK, false);

        if (debt <= 0) {
            ctx.drawString(this.font, "No active loan.", x + 12, y + 84, NotchTheme.TEXT_MUTED, false);
        } else if (daysLeft <= 0) {
            ctx.drawString(this.font, Component.literal("⚠ OVERDUE - penalty interest!").withStyle(ChatFormatting.RED), x + 12, y + 84, 0xFFFFFFFF, false);
        } else {
            ctx.drawString(this.font, "Due in " + daysLeft + " day" + (daysLeft == 1 ? "" : "s"), x + 12, y + 84, NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.divider(ctx, x + 8, y + 97, W - 16);

        ctx.drawString(this.font, "Amount:", x + 12, y + FIELD_Y + 5, NotchTheme.TEXT_DARK, false);
        NotchWidgets.coinInput(ctx, x + FIELD_X, y + FIELD_Y, FIELD_W);

        NotchWidgets.primaryButton(ctx, this.font, x + BORROW_X, y + BTN_Y, BTN_W, BTN_H, "Borrow",
                over(mouseX, mouseY, x + BORROW_X, y + BTN_Y, BTN_W, BTN_H));
        NotchWidgets.primaryButton(ctx, this.font, x + REPAY_X, y + BTN_Y, BTN_W, BTN_H, "Repay",
                over(mouseX, mouseY, x + REPAY_X, y + BTN_Y, BTN_W, BTN_H));
        NotchWidgets.neutralButton(ctx, this.font, x + ALL_X, y + ALL_Y, ALL_W, ALL_H, "Repay Everything",
                over(mouseX, mouseY, x + ALL_X, y + ALL_Y, ALL_W, ALL_H));
        //? if >=26.1 {
        /*
        super.extractContents(ctx, mouseX, mouseY, delta);
        *///?}
    }

    private void moneyRow(GuiGraphics ctx, String label, long amount, int ry) {
        ctx.drawString(this.font, label, this.leftPos + 12, ry + 5, NotchTheme.TEXT_DARK, false);
        String text = String.format(java.util.Locale.ROOT, "%,d", amount);
        int w = 18 + this.font.width(text) - 1 + 4;
        NotchWidgets.coinField(ctx, this.font, this.leftPos + W - 12 - w, ry, text, NotchWidgets.COIN_GOLD);
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    @Override
    protected void renderLabels(GuiGraphics ctx, int mouseX, int mouseY) {
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
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            if (over(mx, my, this.leftPos + BORROW_X, this.topPos + BTN_Y, BTN_W, BTN_H)) { NotchWidgets.click(); send(0, amount()); return true; }
            if (over(mx, my, this.leftPos + REPAY_X, this.topPos + BTN_Y, BTN_W, BTN_H)) { NotchWidgets.click(); send(1, amount()); return true; }
            if (over(mx, my, this.leftPos + ALL_X, this.topPos + ALL_Y, ALL_W, ALL_H)) { NotchWidgets.click(); send(1, 0); return true; }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private long amount() {
        try {
            return Long.parseLong(amountField.getValue().trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void send(int action, long amount) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeVarInt(action);
        buf.writeVarLong(amount);
        NetClient.sendToServer(NotchPackets.LOAN_ACTION, buf);
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
    *///?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, amountField)) return true;
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
