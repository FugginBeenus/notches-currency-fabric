package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.shop.ShopBrowseScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;

import java.util.UUID;

/**
 * The buyer-side shop browser: pick a listing, set a quantity, see the full price (coins, barter
 * items, or both), and buy. Code-drawn in the NotchWidgets style; stock and pages update live while
 * the screen is open. Replaces the old PNG-textured 6-listing browser.
 */
public class ShopBrowseScreen extends HandledScreen<ShopBrowseScreenHandler> {

    private static final int W = 240, H = 228;
    private static final int ROW_X = 8, ROW_W = 224, ROW_H = 24, ROW_STEP = 25, ROWS_Y = 31;

    private int selectedRow = -1;
    private int qty = 1;

    public ShopBrowseScreen(ShopBrowseScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = W;
        this.backgroundHeight = H;
        this.titleX = -1000;
        this.playerInventoryTitleX = -1000;
    }

    private int rowY(int i) { return this.y + ROWS_Y + i * ROW_STEP; }

    /** Everything a row needs, unpacked from the carrier stack's NBT. */
    private record Row(ItemStack icon, UUID listingId, int price, String barterName, int barterCount, int stock) {}

    private Row row(int i) {
        ItemStack stack = handler.rowStack(i);
        if (stack.isEmpty()) return null;
        NbtCompound t = stack.getNbt();
        if (t == null || !t.containsUuid("nc_lid")) return null;
        return new Row(stack, t.getUuid("nc_lid"), t.getInt("nc_price"),
                t.getString("nc_bname"), t.getInt("nc_bcount"), t.getInt("nc_stock"));
    }

    private static String priceLine(int price, String barterName, int barterCount, int mult) {
        StringBuilder sb = new StringBuilder();
        if (price > 0) sb.append((long) price * mult).append("c");
        if (barterCount > 0 && !barterName.isEmpty()) {
            if (sb.length() > 0) sb.append(" + ");
            sb.append((long) barterCount * mult).append("×").append(barterName);
        }
        return sb.length() > 0 ? sb.toString() : "free";
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x, y = this.y;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.textRenderer, handler.shopName(), x + W / 2, y + 8);

        // Greeting (left) + pager (right).
        String greeting = handler.greeting();
        if (!greeting.isEmpty()) {
            if (greeting.length() > 34) greeting = greeting.substring(0, 33) + "…";
            ctx.drawText(this.textRenderer, greeting, x + 10, y + 19, NotchTheme.TEXT_MUTED, false);
        }
        int pageCount = handler.prop(ShopBrowseScreenHandler.P_TOTAL_PAGES);
        if (pageCount > 1) {
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + 186, y + 17, 13, 12, "<",
                    over(mouseX, mouseY, x + 186, y + 17, 13, 12));
            NotchWidgets.centerText(ctx, this.textRenderer,
                    (handler.prop(ShopBrowseScreenHandler.P_PAGE) + 1) + "/" + pageCount,
                    x + 210, y + 19, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + 219, y + 17, 13, 12, ">",
                    over(mouseX, mouseY, x + 219, y + 17, 13, 12));
        }

        int status = handler.prop(ShopBrowseScreenHandler.P_STATUS);
        boolean open = status == ShopBrowseScreenHandler.STATUS_OPEN;

        // Listing rows.
        boolean any = false;
        for (int i = 0; i < ShopBrowseScreenHandler.ROWS; i++) {
            Row row = row(i);
            if (row == null) continue;
            any = true;
            int ry = rowY(i);
            boolean hover = over(mouseX, mouseY, x + ROW_X, ry, ROW_W, ROW_H);
            NotchWidgets.inset(ctx, x + ROW_X, ry, ROW_W, ROW_H,
                    hover ? NotchTheme.SLOT_FILL : NotchTheme.DEEP);
            if (i == selectedRow) {
                // Green selection frame.
                ctx.fill(x + ROW_X, ry, x + ROW_X + ROW_W, ry + 1, NotchTheme.ACCENT_GREEN);
                ctx.fill(x + ROW_X, ry + ROW_H - 1, x + ROW_X + ROW_W, ry + ROW_H, NotchTheme.ACCENT_GREEN);
                ctx.fill(x + ROW_X, ry, x + ROW_X + 1, ry + ROW_H, NotchTheme.ACCENT_GREEN);
                ctx.fill(x + ROW_X + ROW_W - 1, ry, x + ROW_X + ROW_W, ry + ROW_H, NotchTheme.ACCENT_GREEN);
            }
            ctx.drawItem(row.icon(), x + ROW_X + 4, ry + 4);
            String name = row.icon().getName().getString();
            if (name.length() > 24) name = name.substring(0, 23) + "…";
            ctx.drawText(this.textRenderer, name, x + ROW_X + 24, ry + 3, NotchTheme.TEXT_LIGHT, false);
            ctx.drawText(this.textRenderer, priceLine(row.price(), row.barterName(), row.barterCount(), 1),
                    x + ROW_X + 24, ry + 13, NotchTheme.TEXT_MUTED, false);
            if (row.stock() > 0) {
                String s = "x" + row.stock();
                ctx.drawText(this.textRenderer, s, x + ROW_X + ROW_W - 6 - this.textRenderer.getWidth(s),
                        ry + 8, NotchTheme.TEXT_LIGHT, false);
            } else {
                ctx.drawText(this.textRenderer, "sold out", x + ROW_X + ROW_W - 6 - this.textRenderer.getWidth("sold out"),
                        ry + 8, NotchTheme.TEXT_RED, false);
            }
        }
        if (!any) {
            NotchWidgets.centerText(ctx, this.textRenderer, "This shop has nothing for sale yet.",
                    x + W / 2, y + 100, NotchTheme.TEXT_MUTED, false);
        }

        // Total + buy bar.
        Row sel = selectedRow >= 0 ? row(selectedRow) : null;
        if (!open) {
            NotchWidgets.centerText(ctx, this.textRenderer,
                    status == ShopBrowseScreenHandler.STATUS_RENT_PAUSED
                            ? "Sales are paused while the shop owes rent."
                            : "This shop is currently closed.",
                    x + W / 2, y + 192, NotchTheme.TEXT_RED, false);
        } else if (sel != null) {
            NotchWidgets.centerText(ctx, this.textRenderer,
                    "Total: " + priceLine(sel.price(), sel.barterName(), sel.barterCount(), qty),
                    x + W / 2, y + 192, NotchTheme.TEXT_DARK, false);
        } else {
            NotchWidgets.centerText(ctx, this.textRenderer, "Select something to buy.",
                    x + W / 2, y + 192, NotchTheme.TEXT_MUTED, false);
        }

        boolean canBuy = open && sel != null && sel.stock() > 0;
        NotchWidgets.neutralButton(ctx, this.textRenderer, x + 51, y + 204, 16, 16, "-",
                over(mouseX, mouseY, x + 51, y + 204, 16, 16));
        NotchWidgets.inset(ctx, x + 71, y + 204, 32, 16, NotchTheme.DEEP);
        NotchWidgets.centerText(ctx, this.textRenderer, String.valueOf(qty), x + 87, y + 208, NotchTheme.TEXT_LIGHT, false);
        NotchWidgets.neutralButton(ctx, this.textRenderer, x + 107, y + 204, 16, 16, "+",
                over(mouseX, mouseY, x + 107, y + 204, 16, 16));
        if (canBuy) {
            NotchWidgets.primaryButton(ctx, this.textRenderer, x + 131, y + 204, 58, 16, "Buy",
                    over(mouseX, mouseY, x + 131, y + 204, 58, 16));
        } else {
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + 131, y + 204, 58, 16, "Buy", false);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        // No mouseover tooltips: the carrier slots live off-screen and rows draw their own info.
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            int pageCount = handler.prop(ShopBrowseScreenHandler.P_TOTAL_PAGES);
            if (pageCount > 1) {
                if (over(mx, my, x + 186, y + 17, 13, 12)) { clickButton(0); return true; }
                if (over(mx, my, x + 219, y + 17, 13, 12)) { clickButton(1); return true; }
            }
            for (int i = 0; i < ShopBrowseScreenHandler.ROWS; i++) {
                if (row(i) != null && over(mx, my, x + ROW_X, rowY(i), ROW_W, ROW_H)) {
                    selectedRow = (selectedRow == i) ? -1 : i;
                    qty = 1;
                    return true;
                }
            }
            Row sel = selectedRow >= 0 ? row(selectedRow) : null;
            int step = hasShiftDown() ? 10 : 1;
            if (over(mx, my, x + 51, y + 204, 16, 16)) {
                qty = Math.max(1, qty - step);
                return true;
            }
            if (over(mx, my, x + 107, y + 204, 16, 16)) {
                int cap = sel != null ? Math.max(1, sel.stock()) : 999;
                qty = Math.min(cap, qty + step);
                return true;
            }
            if (sel != null && sel.stock() > 0
                    && handler.prop(ShopBrowseScreenHandler.P_STATUS) == ShopBrowseScreenHandler.STATUS_OPEN
                    && over(mx, my, x + 131, y + 204, 58, 16)) {
                NotchPacketsClient.sendShopPurchase(handler.shopId(), sel.listingId(), Math.min(qty, sel.stock()));
                qty = 1;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void clickButton(int id) {
        ScreenHandler sh = this.handler;
        if (this.client != null && this.client.interactionManager != null) {
            this.client.interactionManager.clickButton(sh.syncId, id);
        }
        selectedRow = -1;
        qty = 1;
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }
}
