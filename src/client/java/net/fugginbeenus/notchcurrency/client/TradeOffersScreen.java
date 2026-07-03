package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.trade.TradeOffersScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * The trade-offers board: offers you can accept up top (paginated), your own open offers below with
 * a cancel button. Code-drawn; refreshes live. Accept/cancel go by offer id through TRADE_OFFER_ACTION.
 */
public class TradeOffersScreen extends HandledScreen<TradeOffersScreenHandler> {

    private static final int W = 248, H = 236;
    private static final int ROW_X = 8, ROW_W = 232, ROW_H = 20;
    private static final int IN_Y = 34, MINE_Y = 152, ROW_STEP = 21;

    // Action ids (mirror the packet).
    private static final int ACTION_ACCEPT = 0, ACTION_CANCEL = 1;

    public TradeOffersScreen(TradeOffersScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = W;
        this.backgroundHeight = H;
        this.titleX = -1000;
        this.playerInventoryTitleX = -1000;
    }

    private int inY(int i) { return this.y + IN_Y + i * ROW_STEP; }
    private int mineY(int i) { return this.y + MINE_Y + i * ROW_STEP; }

    private record Row(ItemStack icon, UUID id, long price, String reqName, int reqCount, String from, String target) {}

    private Row row(ItemStack stack) {
        if (stack.isEmpty()) return null;
        NbtCompound t = stack.getNbt();
        if (t == null || !t.containsUuid("nc_oid")) return null;
        return new Row(stack, t.getUuid("nc_oid"), t.getLong("nc_price"), t.getString("nc_reqname"),
                t.getInt("nc_reqcount"), t.getString("nc_from"), t.getString("nc_target"));
    }

    private static String priceLine(Row r) {
        StringBuilder sb = new StringBuilder();
        if (r.price() > 0) sb.append(r.price()).append("c");
        if (r.reqCount() > 0 && !r.reqName().isEmpty()) {
            if (sb.length() > 0) sb.append(" + ");
            sb.append(r.reqCount()).append("×").append(r.reqName());
        }
        return sb.length() > 0 ? sb.toString() : "free";
    }

    private void drawRow(DrawContext ctx, Row r, int ry, boolean mine, int mouseX, int mouseY) {
        int x = this.x;
        NotchWidgets.inset(ctx, x + ROW_X, ry, ROW_W, ROW_H, NotchTheme.DEEP);
        ctx.drawItem(r.icon(), x + ROW_X + 2, ry + 2);
        String give = r.icon().getName().getString();
        if (give.length() > 16) give = give.substring(0, 15) + "…";
        ctx.drawText(this.textRenderer, give + " x" + r.icon().getCount(), x + ROW_X + 22, ry + 2, NotchTheme.TEXT_LIGHT, false);
        String sub = "for " + priceLine(r) + (mine
                ? (r.target().isEmpty() ? " · open" : " · to " + r.target())
                : " · from " + r.from());
        if (sub.length() > 34) sub = sub.substring(0, 33) + "…";
        ctx.drawText(this.textRenderer, sub, x + ROW_X + 22, ry + 11, NotchTheme.TEXT_MUTED, false);
        int bx = x + ROW_X + ROW_W - 54;
        if (mine) {
            NotchWidgets.dangerButton(ctx, this.textRenderer, bx, ry + 2, 50, 16, "Cancel",
                    over(mouseX, mouseY, bx, ry + 2, 50, 16));
        } else {
            NotchWidgets.primaryButton(ctx, this.textRenderer, bx, ry + 2, 50, 16, "Accept",
                    over(mouseX, mouseY, bx, ry + 2, 50, 16));
        }
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x, y = this.y;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.textRenderer, "Trade Offers", x + W / 2, y + 8);

        // Incoming header + pager.
        ctx.drawText(this.textRenderer, "OFFERS FOR YOU", x + 10, y + 24, NotchTheme.TEXT_DARK, false);
        int pageCount = handler.prop(TradeOffersScreenHandler.P_TOTAL_PAGES);
        if (pageCount > 1) {
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + 190, y + 22, 13, 11, "<",
                    over(mouseX, mouseY, x + 190, y + 22, 13, 11));
            NotchWidgets.centerText(ctx, this.textRenderer,
                    (handler.prop(TradeOffersScreenHandler.P_PAGE) + 1) + "/" + pageCount,
                    x + 214, y + 24, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + 227, y + 22, 13, 11, ">",
                    over(mouseX, mouseY, x + 227, y + 22, 13, 11));
        }
        boolean anyIn = false;
        for (int i = 0; i < TradeOffersScreenHandler.INCOMING; i++) {
            Row r = row(handler.incomingStack(i));
            if (r == null) continue;
            anyIn = true;
            drawRow(ctx, r, inY(i), false, mouseX, mouseY);
        }
        if (!anyIn) {
            NotchWidgets.centerText(ctx, this.textRenderer, "No offers waiting for you.",
                    x + W / 2, y + IN_Y + 20, NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.divider(ctx, x + 8, y + MINE_Y - 8, W - 16);
        ctx.drawText(this.textRenderer, "YOUR OFFERS", x + 10, y + MINE_Y - 6, NotchTheme.TEXT_DARK, false);
        boolean anyMine = false;
        for (int i = 0; i < TradeOffersScreenHandler.OUTGOING; i++) {
            Row r = row(handler.outgoingStack(i));
            if (r == null) continue;
            anyMine = true;
            drawRow(ctx, r, mineY(i), true, mouseX, mouseY);
        }
        if (!anyMine) {
            NotchWidgets.centerText(ctx, this.textRenderer, "You have no open offers. Use /trade offer to create one.",
                    x + W / 2, y + MINE_Y + 14, NotchTheme.TEXT_MUTED, false);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            int pageCount = handler.prop(TradeOffersScreenHandler.P_TOTAL_PAGES);
            if (pageCount > 1) {
                if (over(mx, my, x + 190, y + 22, 13, 11)) { clickButton(0); return true; }
                if (over(mx, my, x + 227, y + 22, 13, 11)) { clickButton(1); return true; }
            }
            int bxIn = x + ROW_X + ROW_W - 54;
            for (int i = 0; i < TradeOffersScreenHandler.INCOMING; i++) {
                Row r = row(handler.incomingStack(i));
                if (r != null && over(mx, my, bxIn, inY(i) + 2, 50, 16)) {
                    NotchPacketsClient.sendTradeOfferAction(r.id(), ACTION_ACCEPT);
                    return true;
                }
            }
            for (int i = 0; i < TradeOffersScreenHandler.OUTGOING; i++) {
                Row r = row(handler.outgoingStack(i));
                if (r != null && over(mx, my, bxIn, mineY(i) + 2, 50, 16)) {
                    NotchPacketsClient.sendTradeOfferAction(r.id(), ACTION_CANCEL);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void clickButton(int id) {
        if (this.client != null && this.client.interactionManager != null) {
            this.client.interactionManager.clickButton(this.handler.syncId, id);
        }
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }
}
