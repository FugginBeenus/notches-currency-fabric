package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.mail.MailInboxMenu;
import net.fugginbeenus.notchcurrency.mail.MailLayout;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class MailInboxScreen extends AbstractContainerScreen<MailInboxMenu> {

    private static final int TAKE_W = 56, TAKE_H = 16;

    public MailInboxScreen(MailInboxMenu handler, Inventory inv, Component title) {
        //? if >=26.1 {
        /*super(handler, inv, title, MailLayout.W, MailLayout.H);
        *///?} else {
        super(handler, inv, title);
        this.imageWidth = MailLayout.W;
        this.imageHeight = MailLayout.H;
        //?}
        this.titleLabelX = -1000;
        this.inventoryLabelX = -1000;
    }

    private int takeX() { return this.leftPos + MailLayout.W - 8 - TAKE_W; }
    private int takeY() { return this.topPos + 118; }

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = this.leftPos, y = this.topPos;
        NotchWidgets.panel(ctx, x, y, MailLayout.W, MailLayout.H);
        NotchWidgets.title(ctx, this.font, this.title.getString(), x + MailLayout.W / 2, y + 6);
        MailTabs.draw(ctx, this.font, x, y, MailTabs.INBOX, mouseX, mouseY);

        for (int i = 0; i < MailInboxMenu.INBOX_SLOTS; i++) {
            NotchWidgets.slot(ctx,
                    x + MailLayout.SLOTS_X + (i % MailInboxMenu.COLS) * 18 - 1,
                    y + MailLayout.SLOTS_Y + (i / MailInboxMenu.COLS) * 18 - 1);
        }

        int waiting = this.menu.waiting();
        ctx.drawString(this.font, summary(waiting), x + 8, y + 122,
                waiting == 0 ? NotchTheme.TEXT_MUTED : NotchTheme.TEXT_DARK, false);
        if (waiting > 0) {
            NotchWidgets.primaryButton(ctx, this.font, takeX(), takeY(), TAKE_W, TAKE_H, "Take all",
                    over(mouseX, mouseY, takeX(), takeY(), TAKE_W, TAKE_H));
        }

        MailPostScreen.drawInventory(ctx, x, y, this.font);
        //? if >=26.1 {
        /*super.extractContents(ctx, mouseX, mouseY, delta);
        *///?}
    }

    private String summary(int waiting) {
        if (waiting <= 0) return "Nothing waiting.";
        if (waiting > MailInboxMenu.INBOX_SLOTS) {
            return MailInboxMenu.INBOX_SLOTS + " of " + waiting + " parcels";
        }
        return waiting + (waiting == 1 ? " parcel waiting" : " parcels waiting");
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
            if (MailTabs.click(this.leftPos, this.topPos, MailTabs.INBOX, mx, my)) return true;
            if (this.menu.waiting() > 0 && over(mx, my, takeX(), takeY(), TAKE_W, TAKE_H)) {
                NotchWidgets.click();
                NotchPacketsClient.sendMailTakeAll();
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private static boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
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
        //? if >=26.1 {
        /*extractTooltip(ctx, mouseX, mouseY);
        *///?} else {
        renderTooltip(ctx, mouseX, mouseY);
        //?}
    }

    @Override
    public void removed() {
        super.removed();
        MailTabs.screenClosed();
    }
}
