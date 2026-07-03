package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.shop.ShopManageScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

/**
 * The owner-side shop hub: earnings at a glance with one-click collect, name/greeting editing,
 * the open/closed switch, rent status, and every listing (paginated — all 27 reachable, not just
 * the first 6 like the old screen). Rows open the listing editor. All edits apply instantly.
 */
public class ShopManageScreen extends HandledScreen<ShopManageScreenHandler> {

    private static final int W = 256, H = 244;
    private static final int ROW_X = 8, ROW_W = 240, ROW_H = 18, ROW_STEP = 19, ROWS_Y = 100;

    private TextFieldWidget nameField;
    private TextFieldWidget greetField;

    public ShopManageScreen(ShopManageScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = W;
        this.backgroundHeight = H;
        this.titleX = -1000;
        this.playerInventoryTitleX = -1000;
    }

    private int rowY(int i) { return this.y + ROWS_Y + i * ROW_STEP; }

    @Override
    protected void init() {
        super.init();
        String oldName = nameField == null ? handler.shopName() : nameField.getText();
        nameField = new TextFieldWidget(this.textRenderer, this.x + 48, this.y + 51, 122, 10, Text.literal("Name"));
        nameField.setMaxLength(32);
        nameField.setDrawsBackground(false);
        nameField.setText(oldName);
        addDrawableChild(nameField);

        String oldGreet = greetField == null ? handler.greeting() : greetField.getText();
        greetField = new TextFieldWidget(this.textRenderer, this.x + 48, this.y + 69, 122, 10, Text.literal("Greeting"));
        greetField.setMaxLength(128);
        greetField.setDrawsBackground(false);
        greetField.setPlaceholder(Text.literal("greeting shown to shoppers").formatted(Formatting.DARK_GRAY));
        greetField.setText(oldGreet);
        addDrawableChild(greetField);
    }

    private record Row(ItemStack icon, UUID listingId, int price, String barterName, int barterCount, int stock) {}

    private Row row(int i) {
        ItemStack stack = handler.rowStack(i);
        if (stack.isEmpty()) return null;
        NbtCompound t = stack.getNbt();
        if (t == null || !t.containsUuid("nc_lid")) return null;
        return new Row(stack, t.getUuid("nc_lid"), t.getInt("nc_price"),
                t.getString("nc_bname"), t.getInt("nc_bcount"), t.getInt("nc_stock"));
    }

    private static String priceSummary(Row r) {
        StringBuilder sb = new StringBuilder();
        if (r.price() > 0) sb.append(r.price()).append("c");
        if (r.barterCount() > 0 && !r.barterName().isEmpty()) {
            if (sb.length() > 0) sb.append(" + ");
            sb.append(r.barterCount()).append("×").append(r.barterName());
        }
        return sb.length() > 0 ? sb.toString() : "free";
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x, y = this.y;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.textRenderer, "Shop Manager", x + W / 2, y + 8);

        // Status line: open state left, rent right.
        boolean open = handler.prop(ShopManageScreenHandler.P_OPEN) != 0;
        boolean rentPaused = handler.prop(ShopManageScreenHandler.P_RENT_PAUSED) != 0;
        String status = rentPaused ? "Rent overdue — sales paused" : open ? "Open for business" : "Closed";
        int statusColor = rentPaused ? NotchTheme.TEXT_RED : open ? NotchTheme.TEXT_GREEN : NotchTheme.TEXT_MUTED;
        ctx.drawText(this.textRenderer, status, x + 10, y + 20, statusColor, false);
        int rentCost = handler.prop(ShopManageScreenHandler.P_RENT_COST);
        if (rentCost > 0) {
            String rent = "Rent: " + rentCost + "c/cycle";
            ctx.drawText(this.textRenderer, rent, x + W - 10 - this.textRenderer.getWidth(rent), y + 20,
                    NotchTheme.TEXT_MUTED, false);
        }

        // Earnings + collect.
        long pending = handler.pendingBalance();
        int barterItems = handler.prop(ShopManageScreenHandler.P_BARTER_COUNT);
        String earnings = "Earnings: " + pending + "c" + (barterItems > 0 ? " + " + barterItems + " barter stack"
                + (barterItems == 1 ? "" : "s") : "");
        NotchWidgets.pill(ctx, x + 8, y + 30, 172, 15);
        ctx.drawText(this.textRenderer, earnings, x + 14, y + 34, NotchTheme.TEXT_GOLD, false);
        boolean canCollect = pending > 0 || barterItems > 0;
        if (canCollect) {
            NotchWidgets.goldButton(ctx, this.textRenderer, x + 184, y + 30, 64, 15, "Collect",
                    over(mouseX, mouseY, x + 184, y + 30, 64, 15));
        } else {
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + 184, y + 30, 64, 15, "Collect", false);
        }

        // Name + greeting rows (fields draw on top of the insets).
        ctx.drawText(this.textRenderer, "Name", x + 10, y + 51, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + 44, y + 48, 130, 14, NotchTheme.DEEP);
        NotchWidgets.neutralButton(ctx, this.textRenderer, x + 178, y + 48, 32, 14, "Set",
                over(mouseX, mouseY, x + 178, y + 48, 32, 14));
        if (open) {
            NotchWidgets.primaryButton(ctx, this.textRenderer, x + 214, y + 48, 34, 14, "Open",
                    over(mouseX, mouseY, x + 214, y + 48, 34, 14));
        } else {
            NotchWidgets.dangerButton(ctx, this.textRenderer, x + 214, y + 48, 34, 14, "Closed",
                    over(mouseX, mouseY, x + 214, y + 48, 34, 14));
        }
        ctx.drawText(this.textRenderer, "Greet", x + 10, y + 69, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + 44, y + 66, 130, 14, NotchTheme.DEEP);
        NotchWidgets.neutralButton(ctx, this.textRenderer, x + 178, y + 66, 32, 14, "Set",
                over(mouseX, mouseY, x + 178, y + 66, 32, 14));

        NotchWidgets.divider(ctx, x + 8, y + 84, W - 16);

        // Listings header + pager.
        int count = handler.prop(ShopManageScreenHandler.P_COUNT);
        ctx.drawText(this.textRenderer, "LISTINGS (" + count + "/27)", x + 10, y + 89, NotchTheme.TEXT_DARK, false);
        int pageCount = handler.prop(ShopManageScreenHandler.P_TOTAL_PAGES);
        if (pageCount > 1) {
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + 196, y + 87, 13, 11, "<",
                    over(mouseX, mouseY, x + 196, y + 87, 13, 11));
            NotchWidgets.centerText(ctx, this.textRenderer,
                    (handler.prop(ShopManageScreenHandler.P_PAGE) + 1) + "/" + pageCount,
                    x + 220, y + 89, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + 233, y + 87, 13, 11, ">",
                    over(mouseX, mouseY, x + 233, y + 87, 13, 11));
        }

        // Listing rows.
        boolean any = false;
        for (int i = 0; i < ShopManageScreenHandler.ROWS; i++) {
            Row row = row(i);
            if (row == null) continue;
            any = true;
            int ry = rowY(i);
            NotchWidgets.inset(ctx, x + ROW_X, ry, ROW_W, ROW_H, NotchTheme.DEEP);
            ctx.drawItem(row.icon(), x + ROW_X + 2, ry + 1);
            String name = row.icon().getName().getString();
            if (name.length() > 14) name = name.substring(0, 13) + "…";
            ctx.drawText(this.textRenderer, name, x + ROW_X + 22, ry + 5, NotchTheme.TEXT_LIGHT, false);
            String price = priceSummary(row);
            if (price.length() > 16) price = price.substring(0, 15) + "…";
            ctx.drawText(this.textRenderer, price, x + ROW_X + 106, ry + 5, NotchTheme.TEXT_MUTED, false);
            String s = "x" + row.stock();
            ctx.drawText(this.textRenderer, s, x + ROW_X + 200 - this.textRenderer.getWidth(s), ry + 5,
                    row.stock() > 0 ? NotchTheme.TEXT_LIGHT : NotchTheme.TEXT_RED, false);
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + ROW_X + 204, ry + 2, 34, 14, "Edit",
                    over(mouseX, mouseY, x + ROW_X + 204, ry + 2, 34, 14));
        }
        if (!any) {
            NotchWidgets.centerText(ctx, this.textRenderer, "No listings yet — add one below.",
                    x + W / 2, y + ROWS_Y + 50, NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.primaryButton(ctx, this.textRenderer, x + 48, y + 220, 160, 16, "+ New Listing",
                over(mouseX, mouseY, x + 48, y + 220, 160, 16));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            if ((handler.pendingBalance() > 0 || handler.prop(ShopManageScreenHandler.P_BARTER_COUNT) > 0)
                    && over(mx, my, x + 184, y + 30, 64, 15)) {
                NotchPacketsClient.sendShopWithdraw(handler.shopId());
                return true;
            }
            if (over(mx, my, x + 178, y + 48, 32, 14)) {
                NotchPacketsClient.sendShopManageAction(ShopManageScreenHandler.ACTION_RENAME,
                        nameField.getText().trim(), null);
                return true;
            }
            if (over(mx, my, x + 214, y + 48, 34, 14)) {
                NotchPacketsClient.sendShopManageAction(ShopManageScreenHandler.ACTION_TOGGLE_OPEN, "", null);
                return true;
            }
            if (over(mx, my, x + 178, y + 66, 32, 14)) {
                NotchPacketsClient.sendShopManageAction(ShopManageScreenHandler.ACTION_GREETING,
                        greetField.getText().trim(), null);
                return true;
            }
            int pageCount = handler.prop(ShopManageScreenHandler.P_TOTAL_PAGES);
            if (pageCount > 1) {
                if (over(mx, my, x + 196, y + 87, 13, 11)) { clickButton(0); return true; }
                if (over(mx, my, x + 233, y + 87, 13, 11)) { clickButton(1); return true; }
            }
            for (int i = 0; i < ShopManageScreenHandler.ROWS; i++) {
                Row row = row(i);
                if (row != null && over(mx, my, x + ROW_X + 204, rowY(i) + 2, 34, 14)) {
                    NotchPacketsClient.sendShopManageAction(ShopManageScreenHandler.ACTION_EDIT_LISTING,
                            "", row.listingId());
                    return true;
                }
            }
            if (over(mx, my, x + 48, y + 220, 160, 16)) {
                NotchPacketsClient.sendShopManageAction(ShopManageScreenHandler.ACTION_NEW_LISTING, "", null);
                return true;
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
