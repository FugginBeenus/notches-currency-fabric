package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.shop.ShopListingEditScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * The listing editor. Drop a SAMPLE of what you're selling (its count = how many per sale), set a
 * coin price and/or a barter sample, then fill the STOCK bin: drop stacks of the item into it and
 * they're pulled into the listing's stock. Samples and unmatched bin items return on close. Prices
 * show the coin glyph with a live "Buyer pays" summary. Widely spaced so nothing overlaps.
 */
public class ShopListingEditScreen extends HandledScreen<ShopListingEditScreenHandler> {

    private static final int W = 210, H = 240;

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
        priceField = new TextFieldWidget(this.textRenderer, this.x + 50, this.y + 50, 92, 10, Text.literal("Price"));
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

    private String fit(String s, int max) {
        while (s.length() > 3 && this.textRenderer.getWidth(s) > max) s = s.substring(0, s.length() - 2) + "…";
        return s;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x, y = this.y;
        boolean editing = handler.hasListing();
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.textRenderer, editing ? "Edit Listing" : "New Listing", x + W / 2, y + 8);

        // SALE sample slot.
        NotchWidgets.slot(ctx, x + ShopListingEditScreenHandler.SALE_X - 1, y + ShopListingEditScreenHandler.SALE_Y - 1);
        ItemStack sale = handler.saleSample();
        if (sale.isEmpty()) {
            String hint = editing && !handler.currentSaleDesc().isEmpty()
                    ? "Selling: " + handler.currentSaleDesc() : "Drop what you sell here";
            ctx.drawText(this.textRenderer, fit(hint, 156), x + 34, y + 25, NotchTheme.TEXT_MUTED, false);
            ctx.drawText(this.textRenderer, editing ? "(empty = keep item)" : "(count = per sale)",
                    x + 34, y + 35, NotchTheme.TEXT_MUTED, false);
        } else {
            ctx.drawText(this.textRenderer, "Each sale gives " + sale.getCount(), x + 34, y + 25, NotchTheme.TEXT_DARK, false);
            ctx.drawText(this.textRenderer, "(sample returns on close)", x + 34, y + 35, NotchTheme.TEXT_MUTED, false);
        }

        // PRICE row (coin glyph after the field).
        ctx.drawText(this.textRenderer, "Price", x + 12, y + 50, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + 46, y + 47, 100, 14, NotchTheme.DEEP);
        ctx.drawText(this.textRenderer, NotchCurrency.coinIcon(), x + 150, y + 50, NotchTheme.TEXT_GOLD, false);

        // BARTER sample slot.
        NotchWidgets.slot(ctx, x + ShopListingEditScreenHandler.BARTER_X - 1, y + ShopListingEditScreenHandler.BARTER_Y - 1);
        ItemStack barter = handler.barterSample();
        if (barter.isEmpty()) {
            String hint = editing && !handler.currentBarterDesc().isEmpty()
                    ? "Barter: " + handler.currentBarterDesc() : "Barter item";
            ctx.drawText(this.textRenderer, fit(hint, 108), x + 34, y + 73, NotchTheme.TEXT_MUTED, false);
            ctx.drawText(this.textRenderer, "(optional)", x + 34, y + 83, NotchTheme.TEXT_MUTED, false);
        } else {
            ctx.drawText(this.textRenderer, "Costs " + barter.getCount() + "× this", x + 34, y + 73, NotchTheme.TEXT_DARK, false);
            ctx.drawText(this.textRenderer, "(sample returns on close)", x + 34, y + 83, NotchTheme.TEXT_MUTED, false);
        }
        if (editing && !handler.currentBarterDesc().isEmpty()) {
            NotchWidgets.dangerButton(ctx, this.textRenderer, x + 150, y + 70, 48, 13, "Clear",
                    over(mouseX, mouseY, x + 150, y + 70, 48, 13));
        }

        // Live "buyer pays" summary (left-aligned so it can't overflow the panel edges).
        MutableText pays = Text.empty();
        boolean anyPrice = false;
        int p = price();
        if (p > 0) { pays.append(NotchCurrency.coins(p)); anyPrice = true; }
        String bn = !barter.isEmpty() ? barter.getName().getString() : "";
        if (bn.length() > 8) bn = bn.substring(0, 7) + "…";
        if (!barter.isEmpty()) {
            if (anyPrice) pays.append(Text.literal(" + "));
            pays.append(Text.literal(barter.getCount() + "× " + bn));
            anyPrice = true;
        } else if (editing && !handler.currentBarterDesc().isEmpty()) {
            if (anyPrice) pays.append(Text.literal(" + "));
            pays.append(Text.literal(handler.currentBarterDesc()));
            anyPrice = true;
        }
        Text summary = anyPrice ? Text.literal("Buyer pays: ").append(pays)
                : Text.literal("Set a coin price and/or a barter item.");
        ctx.drawText(this.textRenderer, summary, x + 12, y + 98, anyPrice ? NotchTheme.TEXT_DARK : NotchTheme.TEXT_MUTED, false);

        // STOCK bin slot.
        NotchWidgets.slot(ctx, x + ShopListingEditScreenHandler.STOCK_X - 1, y + ShopListingEditScreenHandler.STOCK_Y - 1);
        ctx.drawText(this.textRenderer, "Stock: " + handler.stockProp(), x + 34, y + 112, NotchTheme.TEXT_DARK, false);
        if (editing) {
            ctx.drawText(this.textRenderer, "drop stacks here", x + 34, y + 122, NotchTheme.TEXT_MUTED, false);
            boolean canTake = handler.stockProp() > 0;
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + 150, y + 109, 48, 14, "Take",
                    canTake && over(mouseX, mouseY, x + 150, y + 109, 48, 14));
        } else {
            ctx.drawText(this.textRenderer, "save first to add stock", x + 34, y + 122, NotchTheme.TEXT_MUTED, false);
        }

        // Action row.
        NotchWidgets.primaryButton(ctx, this.textRenderer, x + 12, y + 134, 70, 16,
                editing ? "Save" : "Create", over(mouseX, mouseY, x + 12, y + 134, 70, 16));
        if (editing) {
            NotchWidgets.dangerButton(ctx, this.textRenderer, x + 86, y + 134, 50, 16, "Delete",
                    over(mouseX, mouseY, x + 86, y + 134, 50, 16));
        }
        NotchWidgets.neutralButton(ctx, this.textRenderer, x + 150, y + 134, 48, 16, "Back",
                over(mouseX, mouseY, x + 150, y + 134, 48, 16));

        NotchWidgets.divider(ctx, x + 8, y + 154, W - 16);
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
        //? if >=1.21 {
        /*this.renderBackground(ctx, mouseX, mouseY, delta);
        *///?} else {
        this.renderBackground(ctx);
        //?}
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
        // Guidance tooltip for the empty stock bin.
        if (handler.stockSample().isEmpty()
                && over(mouseX, mouseY, x + ShopListingEditScreenHandler.STOCK_X, y + ShopListingEditScreenHandler.STOCK_Y, 16, 16)) {
            List<Text> lines = new ArrayList<>();
            lines.add(Text.literal("Stock bin"));
            lines.add(Text.literal("Drop stacks of the item you're").formatted(Formatting.GRAY));
            lines.add(Text.literal("selling here to add stock.").formatted(Formatting.GRAY));
            ctx.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            boolean editing = handler.hasListing();
            if (over(mx, my, x + 12, y + 134, 70, 16)) {
                NotchWidgets.click();
                NotchPacketsClient.sendShopEditAction(ShopListingEditScreenHandler.ACTION_SAVE, price());
                return true;
            }
            if (editing && over(mx, my, x + 86, y + 134, 50, 16)) {
                NotchWidgets.click();
                NotchPacketsClient.sendShopEditAction(ShopListingEditScreenHandler.ACTION_DELETE, 0);
                return true;
            }
            if (over(mx, my, x + 150, y + 134, 48, 16)) {
                NotchWidgets.click();
                NotchPacketsClient.sendShopEditAction(ShopListingEditScreenHandler.ACTION_BACK, 0);
                return true;
            }
            if (editing && handler.stockProp() > 0 && over(mx, my, x + 150, y + 109, 48, 14)) {
                NotchWidgets.click();
                NotchPacketsClient.sendShopEditAction(ShopListingEditScreenHandler.ACTION_RETURN_STOCK, 0);
                return true;
            }
            if (editing && !handler.currentBarterDesc().isEmpty() && over(mx, my, x + 150, y + 70, 48, 13)) {
                NotchWidgets.click();
                NotchPacketsClient.sendShopEditAction(ShopListingEditScreenHandler.ACTION_CLEAR_BARTER, 0);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Keep the screen from closing / hotbar-swapping while typing in a focused field.
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, priceField)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    //? if >=1.21 {
    /*@Override
    protected void applyBlur(float delta) {
        // No 1.21 menu blur behind the mod's screens — they draw crisp panels over the world.
    }
    *///?}
}
