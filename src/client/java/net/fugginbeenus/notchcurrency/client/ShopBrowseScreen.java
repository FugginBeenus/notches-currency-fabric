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
    private static final int LIST_X = 8, LIST_Y = 22, ROW_W = 148, ROW_H = 20, ROW_STEP = 21;
    private static final int SB_X = 160, SB_Y = 22, SB_W = 8, SB_H = 126;
    private static final int PV_X = 174, PV_Y = 22, PV_W = 68, PV_H = 126;

    private static final ItemStack COIN =
            new ItemStack(net.fugginbeenus.notchcurrency.registry.ModItems.NOTCH_COIN);

    private final NpcPreviewWidget preview = new NpcPreviewWidget();
    private boolean draggingScroll;

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
                        ItemStack barterStack, int stock) {}

    private Cell cell(int i) {
        ItemStack s = menu.rowStack(i);
        if (s.isEmpty()) return null;
        CompoundTag t = StackData.getData(s);
        if (!net.fugginbeenus.notchcurrency.compat.Nbt.hasUuid(t, "nc_lid")) return null;
        ItemStack barter = t.contains("nc_bstack") ? StackData.readStack(t.getCompound("nc_bstack")) : ItemStack.EMPTY;
        return new Cell(s, net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(t, "nc_lid"), t.getInt("nc_price"), t.getString("nc_bname"),
                t.getInt("nc_bcount"), barter, t.getInt("nc_stock"));
    }

    private int rowY(int i) { return this.topPos + LIST_Y + i * ROW_STEP; }
    private int pages() { return Math.max(1, menu.prop(ShopBrowseScreenHandler.P_TOTAL_PAGES)); }
    private int page() { return menu.prop(ShopBrowseScreenHandler.P_PAGE); }
    private boolean open() { return menu.prop(ShopBrowseScreenHandler.P_STATUS) == ShopBrowseScreenHandler.STATUS_OPEN; }

    //? if >=26.1 {
    /*@Override
    protected void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = this.leftPos, y = this.topPos;
        NotchWidgets.panel(ctx, x, y, W, H);
        // Owners can color the title with &-codes in the shop name ("&6Golden Goods").
        NotchWidgets.title(ctx, this.font, NotchWidgets.colorize(menu.shopName()), x + W / 2, y + 7);

        boolean canBuy = open();

        // Recessed containers that visually separate the parts: the trade list, and the portrait.
        NotchWidgets.inset(ctx, x + 6, y + 20, 152, 130, NotchTheme.PANEL_MID);
        NotchWidgets.inset(ctx, x + 172, y + 20, 72, 130, NotchTheme.PANEL_MID);

        // Trade rows, vanilla villager style: cost icons (with counts) → arrow → the item for sale.
        // Names and stock live in the hover tooltip, so the row itself stays clean.
        for (int i = 0; i < ShopBrowseScreenHandler.VIS_ROWS; i++) {
            Cell c = cell(i);
            if (c == null) continue;
            int ry = rowY(i);
            boolean hover = canBuy && c.stock() > 0 && over(mouseX, mouseY, x + LIST_X, ry, ROW_W, ROW_H);
            NotchWidgets.button(ctx, x + LIST_X, ry, ROW_W, ROW_H, hover, false);

            int ix = x + LIST_X + 4;
            if (c.price() > 0) {
                // Coin cost with the vanilla stack-count renderer, like emeralds in villager trades.
                ctx.renderItem(COIN, ix, ry + 2);
                ctx.renderItemDecorations(this.font, COIN, ix, ry + 2, NotchWidgets.compactCount(c.price()));
                ix += 28;
            }
            if (!c.barterStack().isEmpty()) {
                ctx.renderItem(c.barterStack(), ix, ry + 2);
                ctx.renderItemDecorations(this.font, c.barterStack(), ix, ry + 2);
            }

            // Arrow (crossed out in red when sold out), then the sale item with its stack count.
            arrow(ctx, x + LIST_X + ROW_W - 40, ry + 6, NotchTheme.TEXT_MUTED);
            if (c.stock() <= 0) cross(ctx, x + LIST_X + ROW_W - 38, ry + 5);
            ctx.renderItem(c.icon(), x + LIST_X + ROW_W - 20, ry + 2);
            ctx.renderItemDecorations(this.font, c.icon(), x + LIST_X + ROW_W - 20, ry + 2);
        }

        // Scrollbar (page-based).
        NotchWidgets.slot(ctx, x + SB_X, y + SB_Y, SB_W, SB_H);
        int tp = pages();
        if (tp > 1) {
            int th = thumbH(), ty = thumbY();
            NotchWidgets.button(ctx, x + SB_X + 1, ty, SB_W - 2, th, false, false);
        }

        // Framed NPC portrait: waist-up bust for the humanoid model.
        preview.drawBust(ctx, x + PV_X, y + PV_Y, PV_W, PV_H, menu.npcId());

        // Divider separating the shop from the player's inventory.
        NotchWidgets.divider(ctx, x + 8, y + 153, W - 16);

        // Player inventory slots.
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
            ctx.fill(x + 7, y + 21, x + 157, y + 149, 0xC0202020);
            NotchWidgets.centerText(ctx, this.font, msg, x + 82, y + 80, NotchTheme.TEXT_RED, true);
        }
    }

    private static void arrow(GuiGraphics ctx, int x, int y, int color) {
        ctx.fill(x, y + 3, x + 10, y + 5, color);
        for (int i = 0; i < 4; i++) {
            ctx.fill(x + 10 + i, y + i, x + 11 + i, y + 8 - i, color);
        }
    }

    private static void cross(GuiGraphics ctx, int x, int y) {
        for (int i = 0; i < 8; i++) {
            ctx.fill(x + i, y + i, x + i + 2, y + i + 2, NotchTheme.ACCENT_RED);
            ctx.fill(x + 7 - i, y + i, x + 9 - i, y + i + 2, NotchTheme.ACCENT_RED);
        }
    }

    private int thumbH() { return Math.max(18, SB_H / pages()); }
    private int thumbY() {
        int tp = pages();
        if (tp <= 1) return this.topPos + SB_Y;
        return this.topPos + SB_Y + (int) ((page() / (double) (tp - 1)) * (SB_H - thumbH()));
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
        // Trade-row tooltip.
        for (int i = 0; i < ShopBrowseScreenHandler.VIS_ROWS; i++) {
            Cell c = cell(i);
            if (c != null && over(mouseX, mouseY, leftPos + LIST_X, rowY(i), ROW_W, ROW_H)) {
                List<Component> lines = new ArrayList<>();
                lines.add(c.icon().getHoverName());
                lines.add(NotchWidgets.priceText(c.price(), c.barterName(), c.barterCount()));
                lines.add(Component.literal(c.stock() > 0 ? "Stock: " + c.stock() : "Sold out")
                        .withStyle(c.stock() > 0 ? ChatFormatting.GRAY : ChatFormatting.RED));
                if (open() && c.stock() > 0) {
                    lines.add(Component.literal("Click to buy · Shift = a stack").withStyle(ChatFormatting.DARK_GRAY));
                }
                ctx.renderComponentTooltip(this.font, lines, mouseX, mouseY);
                return;
            }
        }
        // Greeting tooltip on the portrait.
        String greeting = menu.greeting();
        if (!greeting.isEmpty() && over(mouseX, mouseY, leftPos + PV_X, topPos + PV_Y, PV_W, PV_H)) {
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
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            // Buy a trade.
            if (open()) {
                for (int i = 0; i < ShopBrowseScreenHandler.VIS_ROWS; i++) {
                    Cell c = cell(i);
                    // One buy = one of the stacks shown on the row, so stock has to cover a whole one.
                    int bundle = c == null ? 1 : Math.max(1, c.icon().getCount());
                    if (c != null && c.stock() >= bundle && over(mx, my, leftPos + LIST_X, rowY(i), ROW_W, ROW_H)) {
                        // Shift still means "grab a lot", measured in bundles so it tops out around a
                        // stack of items the way it did when every listing sold singles.
                        int qty = net.fugginbeenus.notchcurrency.compat.Render.shiftDown()
                                ? Math.max(1, Math.min(c.stock() / bundle, Math.max(1, 64 / bundle)))
                                : 1;
                        NotchWidgets.click();
                        NotchPacketsClient.sendShopPurchase(menu.shopId(), c.listingId(), qty);
                        return true;
                    }
                }
            }
            // Scrollbar.
            if (pages() > 1 && over(mx, my, leftPos + SB_X, topPos + SB_Y, SB_W, SB_H)) {
                if (my < thumbY()) { NotchWidgets.tick(); clickButton(0); return true; }
                if (my >= thumbY() + thumbH()) { NotchWidgets.tick(); clickButton(1); return true; }
                draggingScroll = true;
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
    *///?} else {
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
    //?}
        if (draggingScroll && pages() > 1) {
            int rel = (int) mouseY - (this.topPos + SB_Y) - thumbH() / 2;
            int track = SB_H - thumbH();
            int target = track <= 0 ? 0 : Math.round(rel / (float) track * (pages() - 1));
            target = Math.max(0, Math.min(pages() - 1, target));
            if (target != page()) clickButton(target > page() ? 1 : 0);
            return true;
        }
        //? if >=1.21.11 {
        /*return super.mouseDragged(event, dx, dy);
        *///?} else {
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
        //?}
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
    *///?} else {
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
    //?}
        draggingScroll = false;
        //? if >=1.21.11 {
        /*return super.mouseReleased(event);
        *///?} else {
        return super.mouseReleased(mouseX, mouseY, button);
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

    //? if >=1.21 {
    /*@Override
    protected void renderBlurredBackground(float delta) {
        // No 1.21 menu blur behind the mod's screens. They draw crisp panels over the world.
    }
    *///?}
}
