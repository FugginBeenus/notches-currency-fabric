package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.compat.NetClient;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.loan.LoanScreenHandler;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Loan application GUI: shows your debt, limit, interest and due date, lets you type an amount and
 * borrow or repay, and previews the resulting debt. Code-drawn in the {@link NotchWidgets} style.
 */
public class LoanScreen extends HandledScreen<LoanScreenHandler> {

    private static final int W = 200, H = 158;
    private static final int FIELD_X = 60, FIELD_Y = 92, FIELD_W = 128, FIELD_H = 14;
    private static final int BORROW_X = 12, REPAY_X = 102, BTN_Y = 112, BTN_W = 86, BTN_H = 16;
    private static final int ALL_X = 12, ALL_Y = 132, ALL_W = 176, ALL_H = 14;

    private TextFieldWidget amountField;

    public LoanScreen(LoanScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = W;
        this.backgroundHeight = H;
        this.titleX = -1000;
        this.playerInventoryTitleX = -1000;
    }

    @Override
    protected void init() {
        super.init();
        amountField = new TextFieldWidget(this.textRenderer, this.x + FIELD_X + 2, this.y + FIELD_Y + 3,
                FIELD_W - 4, FIELD_H - 5, Text.literal("Amount"));
        amountField.setMaxLength(12);
        amountField.setDrawsBackground(false);
        amountField.setPlaceholder(Text.literal("amount").formatted(Formatting.DARK_GRAY));
        amountField.setTextPredicate(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        addDrawableChild(amountField);
        setInitialFocus(amountField);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x, y = this.y;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.textRenderer, "Loans", x + W / 2, y + 8);

        if (handler.prop(LoanScreenHandler.P_ENABLED) == 0) {
            NotchWidgets.centerText(ctx, this.textRenderer, "Loans are disabled.", x + W / 2, y + 40, NotchTheme.TEXT_MUTED, false);
            return;
        }

        long debt = handler.prop(LoanScreenHandler.P_DEBT) & 0xFFFFFFFFL;
        long max = handler.prop(LoanScreenHandler.P_MAX) & 0xFFFFFFFFL;
        int interest = handler.prop(LoanScreenHandler.P_INTEREST);
        int term = handler.prop(LoanScreenHandler.P_TERM);
        int daysLeft = handler.prop(LoanScreenHandler.P_DAYS_LEFT);

        ctx.drawText(this.textRenderer, "You owe: " + debt + " " + NotchWidgets.coinName(), x + 12, y + 26, NotchTheme.TEXT_DARK, false);
        ctx.drawText(this.textRenderer, "Borrowing limit: " + max + " " + NotchWidgets.coinName(), x + 12, y + 38, NotchTheme.TEXT_DARK, false);
        ctx.drawText(this.textRenderer, "Interest: " + interest + "% per cycle", x + 12, y + 50, NotchTheme.TEXT_DARK, false);
        ctx.drawText(this.textRenderer, "Term: " + term + " days", x + 12, y + 62, NotchTheme.TEXT_DARK, false);

        if (debt <= 0) {
            ctx.drawText(this.textRenderer, "No active loan.", x + 12, y + 74, NotchTheme.TEXT_MUTED, false);
        } else if (daysLeft <= 0) {
            ctx.drawText(this.textRenderer, Text.literal("⚠ OVERDUE - penalty interest!").formatted(Formatting.RED), x + 12, y + 74, 0xFFFFFFFF, false);
        } else {
            ctx.drawText(this.textRenderer, "Due in " + daysLeft + " day" + (daysLeft == 1 ? "" : "s"), x + 12, y + 74, NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.divider(ctx, x + 8, y + 88, W - 16);

        // Amount field + preview.
        ctx.drawText(this.textRenderer, "Amount:", x + 12, y + FIELD_Y + 3, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + FIELD_X, y + FIELD_Y, FIELD_W, FIELD_H, NotchTheme.DEEP);

        NotchWidgets.primaryButton(ctx, this.textRenderer, x + BORROW_X, y + BTN_Y, BTN_W, BTN_H, "Borrow",
                over(mouseX, mouseY, x + BORROW_X, y + BTN_Y, BTN_W, BTN_H));
        NotchWidgets.primaryButton(ctx, this.textRenderer, x + REPAY_X, y + BTN_Y, BTN_W, BTN_H, "Repay",
                over(mouseX, mouseY, x + REPAY_X, y + BTN_Y, BTN_W, BTN_H));
        NotchWidgets.neutralButton(ctx, this.textRenderer, x + ALL_X, y + ALL_Y, ALL_W, ALL_H, "Repay Everything",
                over(mouseX, mouseY, x + ALL_X, y + ALL_Y, ALL_W, ALL_H));
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
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            if (over(mx, my, this.x + BORROW_X, this.y + BTN_Y, BTN_W, BTN_H)) { NotchWidgets.click(); send(0, amount()); return true; }
            if (over(mx, my, this.x + REPAY_X, this.y + BTN_Y, BTN_W, BTN_H)) { NotchWidgets.click(); send(1, amount()); return true; }
            if (over(mx, my, this.x + ALL_X, this.y + ALL_Y, ALL_W, ALL_H)) { NotchWidgets.click(); send(1, 0); return true; }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private long amount() {
        try {
            return Long.parseLong(amountField.getText().trim());
        } catch (NumberFormatException e) {
            return 0L;
        }
    }

    private void send(int action, long amount) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarInt(action);
        buf.writeVarLong(amount);
        NetClient.sendToServer(NotchPackets.LOAN_ACTION, buf);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Keep the screen from closing / hotbar-swapping while typing in a focused field.
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, amountField)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    //? if >=1.21 {
    /*@Override
    protected void applyBlur(float delta) {
        // No 1.21 menu blur behind the mod's screens. They draw crisp panels over the world.
    }
    *///?}
}
