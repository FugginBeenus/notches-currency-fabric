package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.ReceiptState;
import net.fugginbeenus.notchcurrency.economy.ReceiptsScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

/**
 * The receipts screen: a paginated, read-only list of the player's recent coin transactions, how
 * much moved (+green earned / −red spent), what for, and how long ago. A static snapshot sent when
 * the screen opened. Purely informational, no interaction beyond paging.
 */
public class ReceiptsScreen extends HandledScreen<ReceiptsScreenHandler> {

    private static final int W = 256, H = 222;
    private static final int ROW_X = 8, ROW_W = 240, ROW_H = 18, ROWS_Y = 28, PER_PAGE = 9;

    private int page = 0;

    public ReceiptsScreen(ReceiptsScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = W;
        this.backgroundHeight = H;
        this.titleX = -1000;
        this.playerInventoryTitleX = -1000;
    }

    private int totalPages() {
        return Math.max(1, (handler.rows.size() + PER_PAGE - 1) / PER_PAGE);
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

    /** Friendly label: prefer the human detail, else a tidied reason name. */
    private static String label(ReceiptState.Receipt r) {
        if (r.detail() != null && !r.detail().isEmpty()) return r.detail();
        String s = r.reason().toLowerCase().replace('_', ' ');
        return s.isEmpty() ? "transaction" : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x, y = this.y;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.textRenderer, "Receipts", x + W / 2, y + 8);

        if (page >= totalPages()) page = totalPages() - 1;

        int pageCount = totalPages();
        if (pageCount > 1) {
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + 190, y + 16, 13, 12, "<",
                    over(mouseX, mouseY, x + 190, y + 16, 13, 12));
            NotchWidgets.centerText(ctx, this.textRenderer, (page + 1) + "/" + pageCount,
                    x + 214, y + 18, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + 227, y + 16, 13, 12, ">",
                    over(mouseX, mouseY, x + 227, y + 16, 13, 12));
        }

        if (handler.rows.isEmpty()) {
            NotchWidgets.centerText(ctx, this.textRenderer, "No transactions yet.",
                    x + W / 2, y + H / 2, NotchTheme.TEXT_MUTED, false);
            return;
        }

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = start + i;
            if (idx >= handler.rows.size()) break;
            ReceiptState.Receipt r = handler.rows.get(idx);
            int ry = y + ROWS_Y + i * ROW_H;
            NotchWidgets.inset(ctx, x + ROW_X, ry, ROW_W, ROW_H - 1, NotchTheme.DEEP);

            String amount = (r.delta() > 0 ? "+" : "") + r.delta() + "c";
            int color = r.delta() > 0 ? NotchTheme.TEXT_GREEN : NotchTheme.TEXT_RED;
            ctx.drawText(this.textRenderer, amount, x + ROW_X + 6, ry + 5, color, false);

            String lbl = label(r);
            if (lbl.length() > 26) lbl = lbl.substring(0, 25) + "…";
            ctx.drawText(this.textRenderer, lbl, x + ROW_X + 74, ry + 5, NotchTheme.TEXT_LIGHT, false);

            String t = ago(r.time());
            ctx.drawText(this.textRenderer, t, x + ROW_X + ROW_W - 6 - this.textRenderer.getWidth(t), ry + 5,
                    NotchTheme.TEXT_MUTED, false);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        //? if <1.21 {
        this.renderBackground(ctx);
        //?}
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && totalPages() > 1) {
            int mx = (int) mouseX, my = (int) mouseY;
            if (over(mx, my, x + 190, y + 16, 13, 12)) { NotchWidgets.tick(); page = Math.max(0, page - 1); return true; }
            if (over(mx, my, x + 227, y + 16, 13, 12)) { NotchWidgets.tick(); page = Math.min(totalPages() - 1, page + 1); return true; }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    //? if >=1.21 {
    /*@Override
    protected void applyBlur(float delta) {
        // No 1.21 menu blur behind the mod's screens. They draw crisp panels over the world.
    }
    *///?}
}
