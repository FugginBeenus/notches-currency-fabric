package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.cosmetic.CosmeticShopScreenHandler;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

/**
 * The cosmetics shop: a paginated list of cosmetics with prices and a Buy button (or an "Owned"
 * tag for one-time cosmetics you already have). Code-drawn in the NotchWidgets style; owned state
 * updates live after a purchase.
 */
public class CosmeticShopScreen extends HandledScreen<CosmeticShopScreenHandler> {

    private static final int W = 240, H = 216;
    private static final int ROW_X = 8, ROW_W = 224, ROW_H = 24, ROW_STEP = 25, ROWS_Y = 30;

    public CosmeticShopScreen(CosmeticShopScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = W;
        this.backgroundHeight = H;
        this.titleX = -1000;
        this.playerInventoryTitleX = -1000;
    }

    private int rowY(int i) { return this.y + ROWS_Y + i * ROW_STEP; }

    private record Row(ItemStack icon, String id, String name, long price, boolean owned) {}

    private Row row(int i) {
        ItemStack stack = handler.rowStack(i);
        if (stack.isEmpty()) return null;
        NbtCompound t = stack.getNbt();
        if (t == null || !t.contains("nc_cid")) return null;
        return new Row(stack, t.getString("nc_cid"), t.getString("nc_name"),
                t.getLong("nc_price"), t.getBoolean("nc_owned"));
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x, y = this.y;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.textRenderer, "Cosmetics", x + W / 2, y + 8);

        int pageCount = handler.prop(CosmeticShopScreenHandler.P_TOTAL_PAGES);
        if (pageCount > 1) {
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + 186, y + 16, 13, 12, "<",
                    over(mouseX, mouseY, x + 186, y + 16, 13, 12));
            NotchWidgets.centerText(ctx, this.textRenderer,
                    (handler.prop(CosmeticShopScreenHandler.P_PAGE) + 1) + "/" + pageCount,
                    x + 210, y + 18, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + 219, y + 16, 13, 12, ">",
                    over(mouseX, mouseY, x + 219, y + 16, 13, 12));
        }

        boolean any = false;
        for (int i = 0; i < CosmeticShopScreenHandler.ROWS; i++) {
            Row row = row(i);
            if (row == null) continue;
            any = true;
            int ry = rowY(i);
            boolean hover = over(mouseX, mouseY, x + ROW_X, ry, ROW_W, ROW_H);
            NotchWidgets.inset(ctx, x + ROW_X, ry, ROW_W, ROW_H, hover ? NotchTheme.SLOT_FILL : NotchTheme.DEEP);
            ctx.drawItem(row.icon(), x + ROW_X + 4, ry + 4);
            String name = row.name();
            if (name.length() > 24) name = name.substring(0, 23) + "…";
            ctx.drawText(this.textRenderer, name, x + ROW_X + 24, ry + 3, NotchTheme.TEXT_LIGHT, false);
            ctx.drawText(this.textRenderer, row.price() + " coins", x + ROW_X + 24, ry + 13, NotchTheme.TEXT_MUTED, false);
            if (row.owned()) {
                NotchWidgets.neutralButton(ctx, this.textRenderer, x + ROW_X + ROW_W - 62, ry + 4, 56, 16, "Owned", false);
            } else {
                NotchWidgets.primaryButton(ctx, this.textRenderer, x + ROW_X + ROW_W - 62, ry + 4, 56, 16, "Buy",
                        over(mouseX, mouseY, x + ROW_X + ROW_W - 62, ry + 4, 56, 16));
            }
        }
        if (!any) {
            NotchWidgets.centerText(ctx, this.textRenderer, "No cosmetics on offer right now.",
                    x + W / 2, y + 100, NotchTheme.TEXT_MUTED, false);
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
            int pageCount = handler.prop(CosmeticShopScreenHandler.P_TOTAL_PAGES);
            if (pageCount > 1) {
                if (over(mx, my, x + 186, y + 16, 13, 12)) { clickButton(0); return true; }
                if (over(mx, my, x + 219, y + 16, 13, 12)) { clickButton(1); return true; }
            }
            for (int i = 0; i < CosmeticShopScreenHandler.ROWS; i++) {
                Row row = row(i);
                if (row != null && !row.owned()
                        && over(mx, my, x + ROW_X + ROW_W - 62, rowY(i) + 4, 56, 16)) {
                    NotchPacketsClient.sendCosmeticBuy(row.id());
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
