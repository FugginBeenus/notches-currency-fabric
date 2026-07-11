package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.shop.ShopManageScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The owner-side shop hub: earnings with one-click collect, name/greeting editing, the open/closed
 * switch, rent status, and every listing (paginated — all 27 reachable). Prices use the coin glyph;
 * hovering a listing shows a full tooltip. Rows open the listing editor. All edits apply instantly.
 */
public class ShopManageScreen extends HandledScreen<ShopManageScreenHandler> {

    private static final int W = 256, H = 244;
    private static final int ROW_X = 8, ROW_W = 240, ROW_H = 18, ROW_STEP = 19, ROWS_Y = 110;

    private static final ItemStack COIN =
            new ItemStack(net.fugginbeenus.notchcurrency.registry.ModItems.NOTCH_COIN);

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
        nameField = new TextFieldWidget(this.textRenderer, this.x + 45, this.y + 61, 160, 10, Text.literal("Name"));
        nameField.setMaxLength(48); // room for &-color codes in the title ("&6Golden Goods")
        nameField.setDrawsBackground(false);
        nameField.setText(oldName);
        addDrawableChild(nameField);

        String oldGreet = greetField == null ? handler.greeting() : greetField.getText();
        greetField = new TextFieldWidget(this.textRenderer, this.x + 45, this.y + 79, 160, 10, Text.literal("Greeting"));
        greetField.setMaxLength(128);
        greetField.setDrawsBackground(false);
        greetField.setPlaceholder(Text.literal("shown to shoppers").formatted(Formatting.DARK_GRAY));
        greetField.setText(oldGreet);
        addDrawableChild(greetField);
    }

    private record Row(ItemStack icon, UUID listingId, int price, String barterName, int barterCount,
                       ItemStack barterStack, int stock) {}

    private Row row(int i) {
        ItemStack stack = handler.rowStack(i);
        if (stack.isEmpty()) return null;
        NbtCompound t = stack.getNbt();
        if (t == null || !t.containsUuid("nc_lid")) return null;
        ItemStack barter = t.contains("nc_bstack") ? ItemStack.fromNbt(t.getCompound("nc_bstack")) : ItemStack.EMPTY;
        return new Row(stack, t.getUuid("nc_lid"), t.getInt("nc_price"),
                t.getString("nc_bname"), t.getInt("nc_bcount"), barter, t.getInt("nc_stock"));
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x, y = this.y;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.textRenderer, "Shop Manager", x + W / 2, y + 8);

        // Open/closed toggle (shows the state) + status + rent.
        boolean open = handler.prop(ShopManageScreenHandler.P_OPEN) != 0;
        boolean rentPaused = handler.prop(ShopManageScreenHandler.P_RENT_PAUSED) != 0;
        if (open) {
            NotchWidgets.primaryButton(ctx, this.textRenderer, x + 8, y + 20, 60, 14, "Open",
                    over(mouseX, mouseY, x + 8, y + 20, 60, 14));
        } else {
            NotchWidgets.dangerButton(ctx, this.textRenderer, x + 8, y + 20, 60, 14, "Closed",
                    over(mouseX, mouseY, x + 8, y + 20, 60, 14));
        }
        String status = rentPaused ? "Rent overdue — paused" : open ? "Selling" : "Sales off";
        int statusColor = rentPaused ? NotchTheme.TEXT_RED : open ? NotchTheme.TEXT_GREEN : NotchTheme.TEXT_MUTED;
        ctx.drawText(this.textRenderer, status, x + 74, y + 24, statusColor, false);
        int rentCost = handler.prop(ShopManageScreenHandler.P_RENT_COST);
        if (rentCost > 0) {
            MutableText rent = Text.literal("Rent ").append(NotchCurrency.coins(rentCost));
            ctx.drawText(this.textRenderer, rent, x + 248 - this.textRenderer.getWidth(rent), y + 24,
                    NotchTheme.TEXT_MUTED, false);
        }

        // Earnings pill + collect.
        long pending = handler.pendingBalance();
        int barterItems = handler.prop(ShopManageScreenHandler.P_BARTER_COUNT);
        MutableText earnings = Text.literal("Earnings ").append(NotchCurrency.coins(pending));
        if (barterItems > 0) earnings.append(Text.literal(" +" + barterItems + " barter"));
        NotchWidgets.pill(ctx, x + 8, y + 40, 160, 15);
        ctx.drawText(this.textRenderer, earnings, x + 14, y + 44, NotchTheme.TEXT_GOLD, false);
        boolean canCollect = pending > 0 || barterItems > 0;
        if (canCollect) {
            NotchWidgets.goldButton(ctx, this.textRenderer, x + 176, y + 40, 72, 15, "Collect",
                    over(mouseX, mouseY, x + 176, y + 40, 72, 15));
        } else {
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + 176, y + 40, 72, 15, "Collect", false);
        }

        // Name + greeting rows (fields draw on top of the insets).
        ctx.drawText(this.textRenderer, "Name", x + 10, y + 62, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + 42, y + 59, 166, 14, NotchTheme.DEEP);
        NotchWidgets.neutralButton(ctx, this.textRenderer, x + 212, y + 59, 36, 14, "Set",
                over(mouseX, mouseY, x + 212, y + 59, 36, 14));
        ctx.drawText(this.textRenderer, "Greet", x + 10, y + 80, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + 42, y + 77, 166, 14, NotchTheme.DEEP);
        NotchWidgets.neutralButton(ctx, this.textRenderer, x + 212, y + 77, 36, 14, "Set",
                over(mouseX, mouseY, x + 212, y + 77, 36, 14));

        NotchWidgets.divider(ctx, x + 8, y + 94, W - 16);

        // Listings header + pager.
        int count = handler.prop(ShopManageScreenHandler.P_COUNT);
        ctx.drawText(this.textRenderer, "LISTINGS (" + count + "/27)", x + 10, y + 99, NotchTheme.TEXT_DARK, false);
        int pageCount = handler.prop(ShopManageScreenHandler.P_TOTAL_PAGES);
        if (pageCount > 1) {
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + 196, y + 97, 13, 11, "<",
                    over(mouseX, mouseY, x + 196, y + 97, 13, 11));
            NotchWidgets.centerText(ctx, this.textRenderer,
                    (handler.prop(ShopManageScreenHandler.P_PAGE) + 1) + "/" + pageCount,
                    x + 220, y + 99, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + 233, y + 97, 13, 11, ">",
                    over(mouseX, mouseY, x + 233, y + 97, 13, 11));
        }

        // Listing rows.
        boolean any = false;
        for (int i = 0; i < ShopManageScreenHandler.ROWS; i++) {
            Row row = row(i);
            if (row == null) continue;
            any = true;
            int ry = rowY(i);
            // Vanilla-trade card, same as the browse screen: price icons -> arrow -> item.
            NotchWidgets.inset(ctx, x + ROW_X, ry, ROW_W, ROW_H, NotchTheme.DEEP);
            if (row.price() > 0) {
                ctx.drawItem(COIN, x + ROW_X + 3, ry + 1);
                ctx.drawItemInSlot(this.textRenderer, COIN, x + ROW_X + 3, ry + 1,
                        NotchWidgets.compactCount(row.price()));
            }
            if (!row.barterStack().isEmpty()) {
                ctx.drawItemInSlot(this.textRenderer, row.barterStack(), x + ROW_X + 23, ry + 1);
            }
            if (row.price() <= 0 && row.barterStack().isEmpty()) {
                ctx.drawText(this.textRenderer, "free", x + ROW_X + 6, ry + 5, NotchTheme.TEXT_MUTED, false);
            }
            NotchWidgets.arrowRight(ctx, x + ROW_X + 45, ry + 5, NotchTheme.TEXT_MUTED);
            ctx.drawItemInSlot(this.textRenderer, row.icon(), x + ROW_X + 64, ry + 1);
            String s = "x" + row.stock();
            ctx.drawText(this.textRenderer, s, x + ROW_X + 200 - this.textRenderer.getWidth(s), ry + 5,
                    row.stock() > 0 ? NotchTheme.TEXT_LIGHT : NotchTheme.TEXT_RED, false);
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + ROW_X + 204, ry + 1, 32, 15, "Edit",
                    over(mouseX, mouseY, x + ROW_X + 204, ry + 1, 32, 15));
        }
        if (!any) {
            NotchWidgets.centerText(ctx, this.textRenderer, "No listings yet — add one below.",
                    x + W / 2, y + ROWS_Y + 40, NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.primaryButton(ctx, this.textRenderer, x + 8, y + 224, 240, 15, "+ New Listing",
                over(mouseX, mouseY, x + 8, y + 224, 240, 15));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        // Full price/stock tooltip when hovering a listing (left of the Edit button).
        for (int i = 0; i < ShopManageScreenHandler.ROWS; i++) {
            Row row = row(i);
            if (row == null) continue;
            int ry = rowY(i);
            if (over(mouseX, mouseY, x + ROW_X, ry, ROW_W - 40, ROW_H)) {
                List<Text> lines = new ArrayList<>();
                lines.add(row.icon().getName());
                lines.add(NotchWidgets.priceText(row.price(), row.barterName(), row.barterCount()));
                lines.add(Text.literal(row.stock() > 0 ? "Stock: " + row.stock() : "Out of stock")
                        .formatted(row.stock() > 0 ? Formatting.GRAY : Formatting.RED));
                ctx.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            if ((handler.pendingBalance() > 0 || handler.prop(ShopManageScreenHandler.P_BARTER_COUNT) > 0)
                    && over(mx, my, x + 176, y + 40, 72, 15)) {
                NotchWidgets.click();
                NotchPacketsClient.sendShopWithdraw(handler.shopId());
                return true;
            }
            if (over(mx, my, x + 8, y + 20, 60, 14)) {
                NotchWidgets.click();
                NotchPacketsClient.sendShopManageAction(ShopManageScreenHandler.ACTION_TOGGLE_OPEN, "", null);
                return true;
            }
            if (over(mx, my, x + 212, y + 59, 36, 14)) {
                NotchWidgets.click();
                NotchPacketsClient.sendShopManageAction(ShopManageScreenHandler.ACTION_RENAME,
                        nameField.getText().trim(), null);
                return true;
            }
            if (over(mx, my, x + 212, y + 77, 36, 14)) {
                NotchWidgets.click();
                NotchPacketsClient.sendShopManageAction(ShopManageScreenHandler.ACTION_GREETING,
                        greetField.getText().trim(), null);
                return true;
            }
            int pageCount = handler.prop(ShopManageScreenHandler.P_TOTAL_PAGES);
            if (pageCount > 1) {
                if (over(mx, my, x + 196, y + 97, 13, 11)) { NotchWidgets.tick(); clickButton(0); return true; }
                if (over(mx, my, x + 233, y + 97, 13, 11)) { NotchWidgets.tick(); clickButton(1); return true; }
            }
            for (int i = 0; i < ShopManageScreenHandler.ROWS; i++) {
                Row row = row(i);
                if (row != null && over(mx, my, x + ROW_X + 204, rowY(i) + 1, 32, 15)) {
                    NotchWidgets.click();
                    NotchPacketsClient.sendShopManageAction(ShopManageScreenHandler.ACTION_EDIT_LISTING,
                            "", row.listingId());
                    return true;
                }
            }
            if (over(mx, my, x + 8, y + 224, 240, 15)) {
                NotchWidgets.click();
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Keep the screen from closing / hotbar-swapping while typing in a focused field.
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, nameField, greetField)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
