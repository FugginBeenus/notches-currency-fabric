package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.client.ui.NpcPreviewWidget;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.shop.ShopBrowseScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * The buyer-side shop, drawn entirely in code as a vanilla villager-style trade screen: a scrollable
 * list of trade rows (item + coin/barter price) on the left, a scrollbar, a framed NPC portrait on
 * the right, and the player inventory below. Click a trade to buy one (Shift = a stack). Styled with
 * NotchTheme so it reads like a stock trading screen but in the mod's look.
 */
public class ShopBrowseScreen extends HandledScreen<ShopBrowseScreenHandler> {

    private static final int W = 248, H = 240;
    private static final int LIST_X = 8, LIST_Y = 22, ROW_W = 148, ROW_H = 20, ROW_STEP = 21;
    private static final int SB_X = 160, SB_Y = 22, SB_W = 8, SB_H = 126;
    private static final int PV_X = 174, PV_Y = 22, PV_W = 68, PV_H = 126;

    /** The coin item, drawn as the cost icon in trade rows (like vanilla's emerald). */
    private static final ItemStack COIN =
            new ItemStack(net.fugginbeenus.notchcurrency.registry.ModItems.NOTCH_COIN);

    private final NpcPreviewWidget preview = new NpcPreviewWidget();
    private boolean draggingScroll;

    public ShopBrowseScreen(ShopBrowseScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = W;
        this.backgroundHeight = H;
        this.titleX = -1000;
        this.playerInventoryTitleX = -1000;
    }

    private record Cell(ItemStack icon, UUID listingId, int price, String barterName, int barterCount,
                        ItemStack barterStack, int stock) {}

    private Cell cell(int i) {
        ItemStack s = handler.rowStack(i);
        if (s.isEmpty()) return null;
        NbtCompound t = StackData.getData(s);
        if (!t.containsUuid("nc_lid")) return null;
        ItemStack barter = t.contains("nc_bstack") ? StackData.readStack(t.getCompound("nc_bstack")) : ItemStack.EMPTY;
        return new Cell(s, t.getUuid("nc_lid"), t.getInt("nc_price"), t.getString("nc_bname"),
                t.getInt("nc_bcount"), barter, t.getInt("nc_stock"));
    }

    private int rowY(int i) { return this.y + LIST_Y + i * ROW_STEP; }
    private int pages() { return Math.max(1, handler.prop(ShopBrowseScreenHandler.P_TOTAL_PAGES)); }
    private int page() { return handler.prop(ShopBrowseScreenHandler.P_PAGE); }
    private boolean open() { return handler.prop(ShopBrowseScreenHandler.P_STATUS) == ShopBrowseScreenHandler.STATUS_OPEN; }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x, y = this.y;
        NotchWidgets.panel(ctx, x, y, W, H);
        // Owners can color the title with &-codes in the shop name ("&6Golden Goods").
        NotchWidgets.title(ctx, this.textRenderer, NotchWidgets.colorize(handler.shopName()), x + W / 2, y + 7);

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
                ctx.drawItem(COIN, ix, ry + 2);
                ctx.drawItemInSlot(this.textRenderer, COIN, ix, ry + 2, NotchWidgets.compactCount(c.price()));
                ix += 28;
            }
            if (!c.barterStack().isEmpty()) {
                ctx.drawItem(c.barterStack(), ix, ry + 2);
                ctx.drawItemInSlot(this.textRenderer, c.barterStack(), ix, ry + 2);
            }

            // Arrow (crossed out in red when sold out), then the sale item with its stack count.
            arrow(ctx, x + LIST_X + ROW_W - 40, ry + 6, NotchTheme.TEXT_MUTED);
            if (c.stock() <= 0) cross(ctx, x + LIST_X + ROW_W - 38, ry + 5);
            ctx.drawItem(c.icon(), x + LIST_X + ROW_W - 20, ry + 2);
            ctx.drawItemInSlot(this.textRenderer, c.icon(), x + LIST_X + ROW_W - 20, ry + 2);
        }

        // Scrollbar (page-based).
        NotchWidgets.slot(ctx, x + SB_X, y + SB_Y, SB_W, SB_H);
        int tp = pages();
        if (tp > 1) {
            int th = thumbH(), ty = thumbY();
            NotchWidgets.button(ctx, x + SB_X + 1, ty, SB_W - 2, th, false, false);
        }

        // Framed NPC portrait — waist-up bust for the humanoid model.
        preview.drawBust(ctx, x + PV_X, y + PV_Y, PV_W, PV_H, handler.npcId());

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
            String msg = handler.prop(ShopBrowseScreenHandler.P_STATUS) == ShopBrowseScreenHandler.STATUS_RENT_PAUSED
                    ? "Sales paused (rent due)" : "Shop is closed";
            ctx.fill(x + 7, y + 21, x + 157, y + 149, 0xC0202020);
            NotchWidgets.centerText(ctx, this.textRenderer, msg, x + 82, y + 80, NotchTheme.TEXT_RED, true);
        }
    }

    /** A small right-pointing arrow, like the vanilla trade arrow. */
    private static void arrow(DrawContext ctx, int x, int y, int color) {
        ctx.fill(x, y + 3, x + 10, y + 5, color);
        for (int i = 0; i < 4; i++) {
            ctx.fill(x + 10 + i, y + i, x + 11 + i, y + 8 - i, color);
        }
    }

    /** A red cross over the arrow for sold-out trades. */
    private static void cross(DrawContext ctx, int x, int y) {
        for (int i = 0; i < 8; i++) {
            ctx.fill(x + i, y + i, x + i + 2, y + i + 2, NotchTheme.ACCENT_RED);
            ctx.fill(x + 7 - i, y + i, x + 9 - i, y + i + 2, NotchTheme.ACCENT_RED);
        }
    }

    private int thumbH() { return Math.max(18, SB_H / pages()); }
    private int thumbY() {
        int tp = pages();
        if (tp <= 1) return this.y + SB_Y;
        return this.y + SB_Y + (int) ((page() / (double) (tp - 1)) * (SB_H - thumbH()));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        // Trade-row tooltip.
        for (int i = 0; i < ShopBrowseScreenHandler.VIS_ROWS; i++) {
            Cell c = cell(i);
            if (c != null && over(mouseX, mouseY, x + LIST_X, rowY(i), ROW_W, ROW_H)) {
                List<Text> lines = new ArrayList<>();
                lines.add(c.icon().getName());
                lines.add(NotchWidgets.priceText(c.price(), c.barterName(), c.barterCount()));
                lines.add(Text.literal(c.stock() > 0 ? "Stock: " + c.stock() : "Sold out")
                        .formatted(c.stock() > 0 ? Formatting.GRAY : Formatting.RED));
                if (open() && c.stock() > 0) {
                    lines.add(Text.literal("Click to buy · Shift = a stack").formatted(Formatting.DARK_GRAY));
                }
                ctx.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
                return;
            }
        }
        // Greeting tooltip on the portrait.
        String greeting = handler.greeting();
        if (!greeting.isEmpty() && over(mouseX, mouseY, x + PV_X, y + PV_Y, PV_W, PV_H)) {
            ctx.drawTooltip(this.textRenderer, Text.literal(greeting), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            // Buy a trade.
            if (open()) {
                for (int i = 0; i < ShopBrowseScreenHandler.VIS_ROWS; i++) {
                    Cell c = cell(i);
                    if (c != null && c.stock() > 0 && over(mx, my, x + LIST_X, rowY(i), ROW_W, ROW_H)) {
                        int qty = hasShiftDown() ? Math.min(c.stock(), 64) : 1;
                        NotchWidgets.click();
                        NotchPacketsClient.sendShopPurchase(handler.shopId(), c.listingId(), qty);
                        return true;
                    }
                }
            }
            // Scrollbar.
            if (pages() > 1 && over(mx, my, x + SB_X, y + SB_Y, SB_W, SB_H)) {
                if (my < thumbY()) { NotchWidgets.tick(); clickButton(0); return true; }
                if (my >= thumbY() + thumbH()) { NotchWidgets.tick(); clickButton(1); return true; }
                draggingScroll = true;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (draggingScroll && pages() > 1) {
            int rel = (int) mouseY - (this.y + SB_Y) - thumbH() / 2;
            int track = SB_H - thumbH();
            int target = track <= 0 ? 0 : Math.round(rel / (float) track * (pages() - 1));
            target = Math.max(0, Math.min(pages() - 1, target));
            if (target != page()) clickButton(target > page() ? 1 : 0);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScroll = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (pages() > 1) {
            if (amount < 0) clickButton(1);
            else if (amount > 0) clickButton(0);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
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
