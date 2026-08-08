package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.ReceiptState;
import net.fugginbeenus.notchcurrency.economy.ReceiptsScreenHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class ReceiptsScreen extends AbstractContainerScreen<ReceiptsScreenHandler> {

    private static final int W = 256, H = 222;
    private static final int ROW_X = 8, ROW_W = 240, ROW_H = 18, ROWS_Y = 28, PER_PAGE = 9;

    private int page = 0;

    public ReceiptsScreen(ReceiptsScreenHandler handler, Inventory inv, Component title) {
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

    private int totalPages() {
        return Math.max(1, (menu.rows.size() + PER_PAGE - 1) / PER_PAGE);
    }

    private static String ago(long time) {
        long secs = Math.max(0, (System.currentTimeMillis() - time) / 1000);
        if (secs < 60) return secs + "s ago";
        long mins = secs / 60;
        if (mins < 60) return mins + "m ago";
        long hours = mins / 60;
        if (hours < 24) return hours + "h ago";
        return (hours / 24) + "d ago";
    }

    private static String label(ReceiptState.Receipt r) {
        if (r.detail() != null && !r.detail().isEmpty()) return r.detail();
        String s = r.reason().toLowerCase().replace('_', ' ');
        return s.isEmpty() ? "transaction" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
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
        NotchWidgets.title(ctx, this.font, "Receipts", x + W / 2, y + 8);

        if (page >= totalPages()) page = totalPages() - 1;

        int pageCount = totalPages();
        if (pageCount > 1) {
            NotchWidgets.neutralButton(ctx, this.font, x + 190, y + 16, 13, 12, "<",
                    over(mouseX, mouseY, x + 190, y + 16, 13, 12));
            NotchWidgets.centerText(ctx, this.font, (page + 1) + "/" + pageCount,
                    x + 214, y + 18, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.font, x + 227, y + 16, 13, 12, ">",
                    over(mouseX, mouseY, x + 227, y + 16, 13, 12));
        }

        if (menu.rows.isEmpty()) {
            NotchWidgets.centerText(ctx, this.font, "No transactions yet.",
                    x + W / 2, y + H / 2, NotchTheme.TEXT_MUTED, false);
            return;
        }

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= menu.rows.size()) break;
            ReceiptState.Receipt r = menu.rows.get(idx);
            int ry = y + ROWS_Y + i * ROW_H;
            NotchWidgets.inset(ctx, x + ROW_X, ry, ROW_W, ROW_H - 1, NotchTheme.DEEP);

            String amount = (r.delta() > 0 ? "+" : "") + r.delta() + "c";
            int color = r.delta() > 0 ? NotchTheme.TEXT_GREEN : NotchTheme.TEXT_RED;
            ctx.drawString(this.font, amount, x + ROW_X + 6, ry + 5, color, false);

            String lbl = label(r);
            if (lbl.length() > 26) lbl = lbl.substring(0, 25) + "…";
            ctx.drawString(this.font, lbl, x + ROW_X + 74, ry + 5, NotchTheme.TEXT_LIGHT, false);

            String t = ago(r.time());
            ctx.drawString(this.font, t, x + ROW_X + ROW_W - 6 - this.font.width(t), ry + 5,
                    NotchTheme.TEXT_MUTED, false);
        }
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
        if (button == 0 && totalPages() > 1) {
            int mx = (int) mouseX, my = (int) mouseY;
            if (over(mx, my, leftPos + 190, topPos + 16, 13, 12)) { NotchWidgets.tick(); page = Math.max(0, page - 1); return true; }
            if (over(mx, my, leftPos + 227, topPos + 16, 13, 12)) { NotchWidgets.tick(); page = Math.min(totalPages() - 1, page + 1); return true; }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
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
