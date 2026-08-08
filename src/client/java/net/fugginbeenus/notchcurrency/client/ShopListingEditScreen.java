package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.shop.ShopListingEditScreenHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;

public class ShopListingEditScreen extends AbstractContainerScreen<ShopListingEditScreenHandler> {

    private static final int W = 210, H = 240;

    private EditBox priceField;

    public ShopListingEditScreen(ShopListingEditScreenHandler handler, Inventory inv, Component title) {
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

    @Override
    protected void init() {
        super.init();
        String old = priceField == null
                ? (menu.priceProp() > 0 ? String.valueOf(menu.priceProp()) : "")
                : priceField.getValue();
        priceField = new EditBox(this.font, this.leftPos + 50, this.topPos + 50, 92, 10, Component.literal("Price"));
        priceField.setMaxLength(7);
        priceField.setBordered(false);
        priceField.setHint(Component.literal("0").withStyle(ChatFormatting.DARK_GRAY));
        net.fugginbeenus.notchcurrency.compat.Render.setFilter(priceField, s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        priceField.setValue(old);
        addRenderableWidget(priceField);
    }

    private int price() {
        try {
            return priceField.getValue().isEmpty() ? 0 : Integer.parseInt(priceField.getValue());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String fit(String s, int max) {
        while (s.length() > 3 && this.font.width(s) > max) s = s.substring(0, s.length() - 2) + "…";
        return s;
    }

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = this.leftPos, y = this.topPos;
        boolean editing = menu.hasListing();
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.font, editing ? "Edit Listing" : "New Listing", x + W / 2, y + 8);

        // SALE sample slot.
        NotchWidgets.slot(ctx, x + ShopListingEditScreenHandler.SALE_X - 1, y + ShopListingEditScreenHandler.SALE_Y - 1);
        ItemStack sale = menu.saleSample();
        if (sale.isEmpty()) {
            String hint = editing && !menu.currentSaleDesc().isEmpty()
                    ? "Selling: " + menu.currentSaleDesc() : "Drop what you sell here";
            ctx.drawString(this.font, fit(hint, 156), x + 34, y + 25, NotchTheme.TEXT_MUTED, false);
            ctx.drawString(this.font, editing ? "(empty = keep item)" : "(count = per sale)",
                    x + 34, y + 35, NotchTheme.TEXT_MUTED, false);
        } else {
            ctx.drawString(this.font, "Each sale gives " + sale.getCount(), x + 34, y + 25, NotchTheme.TEXT_DARK, false);
            ctx.drawString(this.font, "(sample returns on close)", x + 34, y + 35, NotchTheme.TEXT_MUTED, false);
        }

        // PRICE row (coin glyph after the field).
        ctx.drawString(this.font, "Price", x + 12, y + 50, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + 46, y + 47, 100, 14, NotchTheme.DEEP);
        ctx.drawString(this.font, NotchCurrency.coinIcon(), x + 150, y + 50, NotchTheme.TEXT_GOLD, false);

        // BARTER sample slot.
        NotchWidgets.slot(ctx, x + ShopListingEditScreenHandler.BARTER_X - 1, y + ShopListingEditScreenHandler.BARTER_Y - 1);
        ItemStack barter = menu.barterSample();
        if (barter.isEmpty()) {
            String hint = editing && !menu.currentBarterDesc().isEmpty()
                    ? "Barter: " + menu.currentBarterDesc() : "Barter item";
            ctx.drawString(this.font, fit(hint, 108), x + 34, y + 73, NotchTheme.TEXT_MUTED, false);
            ctx.drawString(this.font, "(optional)", x + 34, y + 83, NotchTheme.TEXT_MUTED, false);
        } else {
            ctx.drawString(this.font, "Costs " + barter.getCount() + "× this", x + 34, y + 73, NotchTheme.TEXT_DARK, false);
            ctx.drawString(this.font, "(sample returns on close)", x + 34, y + 83, NotchTheme.TEXT_MUTED, false);
        }
        if (editing && !menu.currentBarterDesc().isEmpty()) {
            NotchWidgets.dangerButton(ctx, this.font, x + 150, y + 70, 48, 13, "Clear",
                    over(mouseX, mouseY, x + 150, y + 70, 48, 13));
        }

        // Live "buyer pays" summary (left-aligned so it can't overflow the panel edges).
        MutableComponent pays = Component.empty();
        boolean anyPrice = false;
        int p = price();
        if (p > 0) { pays.append(NotchCurrency.coins(p)); anyPrice = true; }
        String bn = !barter.isEmpty() ? barter.getHoverName().getString() : "";
        if (bn.length() > 8) bn = bn.substring(0, 7) + "…";
        if (!barter.isEmpty()) {
            if (anyPrice) pays.append(Component.literal(" + "));
            pays.append(Component.literal(barter.getCount() + "× " + bn));
            anyPrice = true;
        } else if (editing && !menu.currentBarterDesc().isEmpty()) {
            if (anyPrice) pays.append(Component.literal(" + "));
            pays.append(Component.literal(menu.currentBarterDesc()));
            anyPrice = true;
        }
        Component summary = anyPrice ? Component.literal("Buyer pays: ").append(pays)
                : Component.literal("Set a coin price and/or a barter item.");
        ctx.drawString(this.font, summary, x + 12, y + 98, anyPrice ? NotchTheme.TEXT_DARK : NotchTheme.TEXT_MUTED, false);

        // STOCK bin slot.
        NotchWidgets.slot(ctx, x + ShopListingEditScreenHandler.STOCK_X - 1, y + ShopListingEditScreenHandler.STOCK_Y - 1);
        ctx.drawString(this.font, "Stock: " + menu.stockProp(), x + 34, y + 112, NotchTheme.TEXT_DARK, false);
        if (editing) {
            ctx.drawString(this.font, "drop stacks here", x + 34, y + 122, NotchTheme.TEXT_MUTED, false);
            boolean canTake = menu.stockProp() > 0;
            NotchWidgets.neutralButton(ctx, this.font, x + 150, y + 109, 48, 14, "Take",
                    canTake && over(mouseX, mouseY, x + 150, y + 109, 48, 14));
        } else {
            ctx.drawString(this.font, "save first to add stock", x + 34, y + 122, NotchTheme.TEXT_MUTED, false);
        }

        // Action row.
        NotchWidgets.primaryButton(ctx, this.font, x + 12, y + 134, 70, 16,
                editing ? "Save" : "Create", over(mouseX, mouseY, x + 12, y + 134, 70, 16));
        if (editing) {
            NotchWidgets.dangerButton(ctx, this.font, x + 86, y + 134, 50, 16, "Delete",
                    over(mouseX, mouseY, x + 86, y + 134, 50, 16));
        }
        NotchWidgets.neutralButton(ctx, this.font, x + 150, y + 134, 48, 16, "Back",
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
        this.renderTooltip(ctx, mouseX, mouseY);
        // Guidance tooltip for the empty stock bin.
        if (menu.stockSample().isEmpty()
                && over(mouseX, mouseY, leftPos + ShopListingEditScreenHandler.STOCK_X, topPos + ShopListingEditScreenHandler.STOCK_Y, 16, 16)) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.literal("Stock bin"));
            lines.add(Component.literal("Drop stacks of the item you're").withStyle(ChatFormatting.GRAY));
            lines.add(Component.literal("selling here to add stock.").withStyle(ChatFormatting.GRAY));
            ctx.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        }
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
            boolean editing = menu.hasListing();
            if (over(mx, my, leftPos + 12, topPos + 134, 70, 16)) {
                NotchWidgets.click();
                NotchPacketsClient.sendShopEditAction(ShopListingEditScreenHandler.ACTION_SAVE, price());
                return true;
            }
            if (editing && over(mx, my, leftPos + 86, topPos + 134, 50, 16)) {
                NotchWidgets.click();
                NotchPacketsClient.sendShopEditAction(ShopListingEditScreenHandler.ACTION_DELETE, 0);
                return true;
            }
            if (over(mx, my, leftPos + 150, topPos + 134, 48, 16)) {
                NotchWidgets.click();
                NotchPacketsClient.sendShopEditAction(ShopListingEditScreenHandler.ACTION_BACK, 0);
                return true;
            }
            if (editing && menu.stockProp() > 0 && over(mx, my, leftPos + 150, topPos + 109, 48, 14)) {
                NotchWidgets.click();
                NotchPacketsClient.sendShopEditAction(ShopListingEditScreenHandler.ACTION_RETURN_STOCK, 0);
                return true;
            }
            if (editing && !menu.currentBarterDesc().isEmpty() && over(mx, my, leftPos + 150, topPos + 70, 48, 13)) {
                NotchWidgets.click();
                NotchPacketsClient.sendShopEditAction(ShopListingEditScreenHandler.ACTION_CLEAR_BARTER, 0);
                return true;
            }
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

    //? if >=1.21.11 {
    /*@Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
    *///?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
        // Keep the screen from closing / hotbar-swapping while typing in a focused field.
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, priceField)) return true;
        //? if >=1.21.11 {
        /*return super.keyPressed(event);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
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
