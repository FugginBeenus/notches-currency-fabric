package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.shop.ShopListingEditScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The listing editor: drop a SAMPLE of what you're selling (its count = how many per sale), set a
 * coin price and/or a barter item sample, save, then deposit stock. Samples are never consumed —
 * they come back when the screen closes. Replaces the old 3-raw-slots-per-row manage grid.
 */
public class ShopListingEditScreen extends HandledScreen<ShopListingEditScreenHandler> {

    private static final int W = 176, H = 224;

    private TextFieldWidget priceField;

    public ShopListingEditScreen(ShopListingEditScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = W;
        this.backgroundHeight = H;
        this.titleX = -1000;
        this.playerInventoryTitleX = -1000;
    }

    @Override
    protected void init() {
        super.init();
        String old = priceField == null
                ? (handler.priceProp() > 0 ? String.valueOf(handler.priceProp()) : "")
                : priceField.getText();
        priceField = new TextFieldWidget(this.textRenderer, this.x + 46, this.y + 49, 76, 10, Text.literal("Price"));
        priceField.setMaxLength(7);
        priceField.setDrawsBackground(false);
        priceField.setPlaceholder(Text.literal("0").formatted(Formatting.DARK_GRAY));
        priceField.setTextPredicate(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        priceField.setText(old);
        addDrawableChild(priceField);
    }

    private int price() {
        try {
            return priceField.getText().isEmpty() ? 0 : Integer.parseInt(priceField.getText());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x, y = this.y;
        boolean editing = handler.hasListing();
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.textRenderer, editing ? "Edit Listing" : "New Listing", x + W / 2, y + 8);

        // Sale sample slot.
        NotchWidgets.slot(ctx, x + ShopListingEditScreenHandler.SALE_X - 1, y + ShopListingEditScreenHandler.SALE_Y - 1);
        ItemStack sale = handler.saleSample();
        if (sale.isEmpty()) {
            String hint = editing && !handler.currentSaleDesc().isEmpty()
                    ? "Selling: " + handler.currentSaleDesc()
                    : "Drop a sample of what you sell";
            if (hint.length() > 24) hint = hint.substring(0, 23) + "…";
            ctx.drawText(this.textRenderer, hint, x + 34, y + 23, NotchTheme.TEXT_MUTED, false);
            ctx.drawText(this.textRenderer, editing ? "(empty = keep current item)" : "(count = items per sale)",
                    x + 34, y + 33, NotchTheme.TEXT_MUTED, false);
        } else {
            ctx.drawText(this.textRenderer, "Each sale gives " + sale.getCount(), x + 34, y + 23, NotchTheme.TEXT_DARK, false);
            ctx.drawText(this.textRenderer, "(sample returns on close)", x + 34, y + 33, NotchTheme.TEXT_MUTED, false);
        }

        // Coin price.
        ctx.drawText(this.textRenderer, "Price", x + 12, y + 49, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + 42, y + 46, 84, 14, NotchTheme.DEEP);
        ctx.drawText(this.textRenderer, "coins", x + 132, y + 49, NotchTheme.TEXT_MUTED, false);

        // Barter sample slot.
        NotchWidgets.slot(ctx, x + ShopListingEditScreenHandler.BARTER_X - 1, y + ShopListingEditScreenHandler.BARTER_Y - 1);
        ItemStack barter = handler.barterSample();
        if (barter.isEmpty()) {
            String hint = editing && !handler.currentBarterDesc().isEmpty()
                    ? "Barter: " + handler.currentBarterDesc()
                    : "and/or barter item (optional)";
            if (hint.length() > 24) hint = hint.substring(0, 23) + "…";
            ctx.drawText(this.textRenderer, hint, x + 34, y + 71, NotchTheme.TEXT_MUTED, false);
        } else {
            ctx.drawText(this.textRenderer, "Costs " + barter.getCount() + "× this item", x + 34, y + 71,
                    NotchTheme.TEXT_DARK, false);
        }
        if (editing && !handler.currentBarterDesc().isEmpty()) {
            NotchWidgets.dangerButton(ctx, this.textRenderer, x + 128, y + 68, 40, 13, "Clear",
                    over(mouseX, mouseY, x + 128, y + 68, 40, 13));
        }

        // Live "buyer pays" summary.
        StringBuilder pays = new StringBuilder();
        int p = price();
        if (p > 0) pays.append(p).append("c");
        String barterName = !barter.isEmpty() ? barter.getName().getString()
                : (editing ? handler.currentBarterDesc() : "");
        if (!barter.isEmpty()) {
            if (pays.length() > 0) pays.append(" + ");
            pays.append(barter.getCount()).append("×").append(barterName);
        } else if (editing && !handler.currentBarterDesc().isEmpty()) {
            if (pays.length() > 0) pays.append(" + ");
            pays.append(handler.currentBarterDesc());
        }
        String summary = pays.length() > 0 ? "Buyer pays: " + pays : "Set a coin price and/or barter item.";
        if (summary.length() > 30) summary = summary.substring(0, 29) + "…";
        NotchWidgets.centerText(ctx, this.textRenderer, summary, x + W / 2, y + 90,
                pays.length() > 0 ? NotchTheme.TEXT_DARK : NotchTheme.TEXT_MUTED, false);

        // Stock row.
        ctx.drawText(this.textRenderer, "Stock: " + handler.stockProp(), x + 12, y + 104, NotchTheme.TEXT_DARK, false);
        if (handler.hasListing()) {
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + 62, y + 100, 62, 14, "Deposit",
                    over(mouseX, mouseY, x + 62, y + 100, 62, 14));
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + 128, y + 100, 40, 14, "Take",
                    over(mouseX, mouseY, x + 128, y + 100, 40, 14));
        } else {
            NotchWidgets.centerText(ctx, this.textRenderer, "save first, then add stock", x + 114, y + 104,
                    NotchTheme.TEXT_MUTED, false);
        }

        // Action row.
        NotchWidgets.primaryButton(ctx, this.textRenderer, x + 8, y + 118, 66, 15,
                editing ? "Save" : "Create", over(mouseX, mouseY, x + 8, y + 118, 66, 15));
        if (editing) {
            NotchWidgets.dangerButton(ctx, this.textRenderer, x + 78, y + 118, 44, 15, "Delete",
                    over(mouseX, mouseY, x + 78, y + 118, 44, 15));
        }
        NotchWidgets.neutralButton(ctx, this.textRenderer, x + 126, y + 118, 42, 15, "Back",
                over(mouseX, mouseY, x + 126, y + 118, 42, 15));

        NotchWidgets.divider(ctx, x + 8, y + 136, W - 16);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                NotchWidgets.slot(ctx, x + ShopListingEditScreenHandler.INV_X + col * 18 - 1,
                        y + ShopListingEditScreenHandler.INV_Y + row * 18 - 1);
            }
        }
        for (int col = 0; col < 9; col++) {
            NotchWidgets.slot(ctx, x + ShopListingEditScreenHandler.INV_X + col * 18 - 1,
                    y + ShopListingEditScreenHandler.HOTBAR_Y - 1);
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
            boolean editing = handler.hasListing();
            if (over(mx, my, x + 8, y + 118, 66, 15)) {
                NotchPacketsClient.sendShopEditAction(ShopListingEditScreenHandler.ACTION_SAVE, price());
                return true;
            }
            if (editing && over(mx, my, x + 78, y + 118, 44, 15)) {
                NotchPacketsClient.sendShopEditAction(ShopListingEditScreenHandler.ACTION_DELETE, 0);
                return true;
            }
            if (over(mx, my, x + 126, y + 118, 42, 15)) {
                NotchPacketsClient.sendShopEditAction(ShopListingEditScreenHandler.ACTION_BACK, 0);
                return true;
            }
            if (editing && over(mx, my, x + 62, y + 100, 62, 14)) {
                NotchPacketsClient.sendShopEditAction(ShopListingEditScreenHandler.ACTION_DEPOSIT, 0);
                return true;
            }
            if (editing && over(mx, my, x + 128, y + 100, 40, 14)) {
                NotchPacketsClient.sendShopEditAction(ShopListingEditScreenHandler.ACTION_RETURN_STOCK, 0);
                return true;
            }
            if (editing && !handler.currentBarterDesc().isEmpty() && over(mx, my, x + 128, y + 68, 40, 13)) {
                NotchPacketsClient.sendShopEditAction(ShopListingEditScreenHandler.ACTION_CLEAR_BARTER, 0);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }
}
