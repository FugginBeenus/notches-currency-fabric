package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.client.ui.NpcPreviewWidget;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.shop.ShopBrowseScreenHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShopBrowseScreen extends AbstractContainerScreen<ShopBrowseScreenHandler> {

    private static final int W = 248, H = 240;
    private static final int LIST_X = 8, LIST_Y = 22, ROW_W = 158, ROW_H = 20, ROW_STEP = 21;
    private static final int PV_X = 174, PV_Y = 22, PV_W = 68, PV_H = 126;
    private static final int PG_LX = 174, PG_RX = 228, PG_Y = 136, PG_BW = 14, PG_H = 13;

    private static ItemStack coin;

    private static ItemStack coin() {
        if (coin == null) coin = new ItemStack(net.fugginbeenus.notchcurrency.registry.ModItems.NOTCH_COIN);
        return coin;
    }

    private final NpcPreviewWidget preview = new NpcPreviewWidget();

    public ShopBrowseScreen(ShopBrowseScreenHandler handler, Inventory inv, Component title) {
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

    private record Cell(ItemStack icon, UUID listingId, int price, String barterName, int barterCount,
                        ItemStack barterStack, int stock, int pays) {
        boolean unlimited() { return stock < 0; }
        boolean forSale() { return price > 0 || barterCount > 0; }
        boolean has(int bundle) { return forSale() && (unlimited() || stock >= bundle); }
        int bundle() { return Math.max(1, icon.getCount()); }
        boolean canSell() { return has(bundle()); }
    }

    private Cell cell(int i) {
        ItemStack s = menu.rowStack(i);
        if (s.isEmpty()) return null;
        CompoundTag t = StackData.getData(s);
        if (!net.fugginbeenus.notchcurrency.compat.Nbt.hasUuid(t, "nc_lid")) return null;
        ItemStack barter = t.contains("nc_bstack") ? StackData.readStack(t.getCompound("nc_bstack")) : ItemStack.EMPTY;
        return new Cell(s, net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(t, "nc_lid"), t.getInt("nc_price"), t.getString("nc_bname"),
                t.getInt("nc_bcount"), barter, t.getInt("nc_stock"), t.getInt("nc_pays"));
    }

    private int rowY(int i) { return this.topPos + LIST_Y + i * ROW_STEP; }
    private int pages() { return Math.max(1, menu.prop(ShopBrowseScreenHandler.P_TOTAL_PAGES)); }
    private int page() { return menu.prop(ShopBrowseScreenHandler.P_PAGE); }
    private boolean open() { return menu.prop(ShopBrowseScreenHandler.P_STATUS) == ShopBrowseScreenHandler.STATUS_OPEN; }

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = this.leftPos, y = this.topPos;
        NotchWidgets.panel(ctx, x, y, W, H);
        net.fugginbeenus.notchcurrency.client.ui.Signs.draw(ctx, this.font,
                net.fugginbeenus.notchcurrency.shop.SignStyles.read(menu::prop, ShopBrowseScreenHandler.P_SIGN,
                        net.fugginbeenus.notchcurrency.shop.SignStyles.SHOP_DEFAULT),
                net.fugginbeenus.notchcurrency.shop.SignStyles.SHOP_DEFAULT,
                NotchWidgets.colorize(menu.shopName()), x + W / 2, y + 7 - 14);

        boolean canBuy = open();
        NotchWidgets.inset(ctx, x + 6, y + 20, 162, 130, NotchTheme.PANEL_MID);
        NotchWidgets.inset(ctx, x + 172, y + 20, 72, previewH() + 4, NotchTheme.PANEL_MID);

        for (int i = 0; i < ShopBrowseScreenHandler.VIS_ROWS; i++) {
            Cell c = cell(i);
            if (c == null) continue;
            int ry = rowY(i);
            boolean buyOnly = !c.forSale() && c.pays() > 0;
            boolean hover = canBuy && (c.canSell() || buyOnly) && over(mouseX, mouseY, x + LIST_X, ry, ROW_W, ROW_H);
            boolean soldOut = !buyOnly && !c.canSell();
            if (soldOut) NotchWidgets.disabledButton(ctx, x + LIST_X, ry, ROW_W, ROW_H);
            else NotchWidgets.button(ctx, x + LIST_X, ry, ROW_W, ROW_H, hover, false);

            int ix = x + LIST_X + 4;
            if (buyOnly) {
                ctx.renderItem(c.icon(), ix, ry + 2);
                ctx.renderItemDecorations(this.font, c.icon(), ix, ry + 2);
                int fx = x + LIST_X + ROW_W - 5 - NotchWidgets.coinFieldWidth(this.font, c.pays());
                arrow(ctx, fx - 19, ry + 6, NotchTheme.TEXT_MUTED);
                NotchWidgets.coinField(ctx, this.font, coin(), fx, ry + 2, c.pays(), NotchWidgets.COIN_GREEN);
                continue;
            }
            if (c.price() > 0) {
                ix += NotchWidgets.coinField(ctx, this.font, coin(), ix, ry + 2, c.price(), NotchWidgets.COIN_GOLD) + 4;
                if (!c.barterStack().isEmpty()) {
                    ctx.drawString(this.font, "+", ix, ry + 6, NotchTheme.TEXT_MUTED, false);
                    ix += 7;
                }
            }
            if (!c.barterStack().isEmpty()) {
                ctx.renderItem(c.barterStack(), ix, ry + 2);
                ctx.renderItemDecorations(this.font, c.barterStack(), ix, ry + 2);
            }

            if (soldOut) {
                NotchWidgets.stamp(ctx, this.font, x + LIST_X + ROW_W / 2 + 12, ry + 5, "SOLD OUT", NotchTheme.ACCENT_RED, 0x26B23030);
                ctx.renderItem(c.icon(), x + LIST_X + ROW_W - 20, ry + 2);
                continue;
            }
            arrow(ctx, x + LIST_X + ROW_W - 40, ry + 6, NotchTheme.TEXT_MUTED);
            ctx.renderItem(c.icon(), x + LIST_X + ROW_W - 20, ry + 2);
            ctx.renderItemDecorations(this.font, c.icon(), x + LIST_X + ROW_W - 20, ry + 2);
        }

        if (pages() > 1) {
            NotchWidgets.pager(ctx, this.font, x + PG_LX, x + PG_RX, y + PG_Y, PG_BW, PG_H, page() + 1, pages(),
                    over(mouseX, mouseY, x + PG_LX, y + PG_Y, PG_BW, PG_H),
                    over(mouseX, mouseY, x + PG_RX, y + PG_Y, PG_BW, PG_H));
        }

        preview.drawBust(ctx, x + PV_X, y + PV_Y, PV_W, previewH(), menu.npcId());
        NotchWidgets.divider(ctx, x + 8, y + 153, W - 16);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                NotchWidgets.slot(ctx, x + 43 + col * 18 - 1, y + 158 + row * 18 - 1);
            }
        }
        for (int col = 0; col < 9; col++) {
            NotchWidgets.slot(ctx, x + 43 + col * 18 - 1, y + 158 + 58 - 1);
        }

        if (!canBuy) {
            String msg = menu.prop(ShopBrowseScreenHandler.P_STATUS) == ShopBrowseScreenHandler.STATUS_RENT_PAUSED
                    ? "Sales paused (rent due)" : "Shop is closed";
            ctx.fill(x + 7, y + 21, x + 167, y + 149, 0xC0202020);
            NotchWidgets.centerText(ctx, this.font, msg, x + 87, y + 80, NotchTheme.TEXT_RED, true);
        }
        //? if >=26.1 {
        /*
        super.extractContents(ctx, mouseX, mouseY, delta);
        *///?}
    }

    private static void arrow(GuiGraphics ctx, int x, int y, int color) {
        ctx.fill(x, y + 3, x + 10, y + 5, color);
        for (int i = 0; i < 4; i++) {
            ctx.fill(x + 10 + i, y + i, x + 11 + i, y + 8 - i, color);
        }
    }

    private int previewH() { return pages() > 1 ? PV_H - 16 : PV_H; }

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
        for (int i = 0; i < ShopBrowseScreenHandler.VIS_ROWS; i++) {
            Cell c = cell(i);
            if (c != null && over(mouseX, mouseY, leftPos + LIST_X, rowY(i), ROW_W, ROW_H)) {
                List<Component> lines = new ArrayList<>();
                boolean buyOnly = !c.forSale() && c.pays() > 0;
                lines.add(c.icon().getHoverName());
                if (buyOnly) {
                    lines.add(Component.literal("The shop only buys this one").withStyle(ChatFormatting.GRAY));
                } else {
                    lines.add(NotchWidgets.priceText(c.price(), c.barterName(), c.barterCount()));
                    lines.add(Component.literal(c.unlimited() ? "Always in stock"
                                    : c.stock() >= c.bundle() ? "Stock: " + c.stock()
                                    : c.stock() > 0 ? "Stock: " + c.stock() + ", a sale needs " + c.bundle()
                                    : "Sold out")
                            .withStyle(c.unlimited() ? ChatFormatting.GOLD
                                    : c.stock() >= c.bundle() ? ChatFormatting.GRAY : ChatFormatting.RED));
                }
                if (open() && c.canSell()) {
                    lines.add(Component.literal("Click to buy · Shift = a stack").withStyle(ChatFormatting.DARK_GRAY));
                }
                if (open() && c.pays() > 0) {
                    lines.add(Component.literal("Shop pays " + c.pays() + " each").withStyle(ChatFormatting.GREEN));
                    lines.add(Component.literal("Right-click to sell · Shift = 16").withStyle(ChatFormatting.DARK_GRAY));
                }
                ctx.renderComponentTooltip(this.font, lines, mouseX, mouseY);
                return;
            }
        }
        String greeting = menu.greeting();
        if (!greeting.isEmpty() && over(mouseX, mouseY, leftPos + PV_X, topPos + PV_Y, PV_W, previewH())) {
            ctx.renderTooltip(this.font, Component.literal(greeting), mouseX, mouseY);
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
        if (button == 1 && open()) {
            int rmx = (int) mouseX, rmy = (int) mouseY;
            for (int i = 0; i < ShopBrowseScreenHandler.VIS_ROWS; i++) {
                Cell c = cell(i);
                if (c == null || c.pays() <= 0) continue;
                if (!over(rmx, rmy, leftPos + LIST_X, rowY(i), ROW_W, ROW_H)) continue;
                int qty = net.fugginbeenus.notchcurrency.compat.Render.shiftDown() ? 16 : 1;
                NotchWidgets.click();
                NotchPacketsClient.sendShopSell(menu.shopId(), c.listingId(), qty);
                return true;
            }
        }
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            if (open()) {
                for (int i = 0; i < ShopBrowseScreenHandler.VIS_ROWS; i++) {
                    Cell c = cell(i);
                    if (c != null && c.canSell() && over(mx, my, leftPos + LIST_X, rowY(i), ROW_W, ROW_H)) {
                        int bundle = c.bundle();
                        int maxByStock = c.unlimited() ? Integer.MAX_VALUE : c.stock() / bundle;
                        int qty = net.fugginbeenus.notchcurrency.compat.Render.shiftDown()
                                ? Math.max(1, Math.min(maxByStock, Math.max(1, 64 / bundle)))
                                : 1;
                        NotchWidgets.click();
                        NotchPacketsClient.sendShopPurchase(menu.shopId(), c.listingId(), qty);
                        return true;
                    }
                }
            }
            if (pages() > 1 && over(mx, my, leftPos + PG_LX, topPos + PG_Y, PG_BW, PG_H)) {
                if (page() > 0) { NotchWidgets.tick(); clickButton(0); }
                return true;
            }
            if (pages() > 1 && over(mx, my, leftPos + PG_RX, topPos + PG_Y, PG_BW, PG_H)) {
                if (page() + 1 < pages()) { NotchWidgets.tick(); clickButton(1); }
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    @Override
    //? if >=1.21 {
    /*public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
    *///?} else {
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    //?}
        if (pages() > 1) {
            if (amount < 0) clickButton(1);
            else if (amount > 0) clickButton(0);
            return true;
        }
        //? if >=1.21 {
        /*return super.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
        *///?} else {
        return super.mouseScrolled(mouseX, mouseY, amount);
        //?}
    }

    private void clickButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    //? if >=1.21.11 {
    /*@Override
    protected void renderBlurredBackground(net.minecraft.client.gui.GuiGraphics ctx) {
    }
    *///?} elif >=1.21 {
    /*@Override
    protected void renderBlurredBackground(float delta) {
    }
    *///?}
}
