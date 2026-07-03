package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.enchanter.EnchanterManager;
import net.fugginbeenus.notchcurrency.economy.enchanter.EnchanterScreenHandler;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Enchanter screen: drop an item in the slot, then repair it, buy the exact enchantment level
 * you want (Upgrades), or pull an enchantment off onto a book (Extract). Offers and prices are
 * derived from the slot item with the same code the server validates with, so what you see is what
 * you're charged.
 */
public class EnchanterScreen extends HandledScreen<EnchanterScreenHandler> {

    private static final int W = 176, H = 224;
    private static final int ROW_X = 8, ROW_W = 160, ROW_H = 14, VISIBLE_ROWS = 5;
    private static final int LIST_Y = 62, ROW_STEP = 15;
    private static final int REPAIR_X = 100, REPAIR_Y = 22, REPAIR_W = 68, REPAIR_H = 16;
    private static final int TAB_Y = 44, TAB_H = 14;

    private boolean extractMode = false;
    private int scroll = 0;

    public EnchanterScreen(EnchanterScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = W;
        this.backgroundHeight = H;
        this.titleX = -1000;
        this.playerInventoryTitleX = -1000;
    }

    /** The rows for the current mode: (enchantment, level shown, cost). */
    private record Row(Enchantment ench, int level, long cost) {}

    private List<Row> rows() {
        ItemStack stack = handler.inputStack();
        List<Row> rows = new ArrayList<>();
        if (stack.isEmpty()) return rows;
        if (extractMode) {
            for (Map.Entry<Enchantment, Integer> e : EnchantmentHelper.get(stack).entrySet()) {
                rows.add(new Row(e.getKey(), e.getValue(), handler.extractCostProp()));
            }
        } else {
            for (EnchanterManager.Offer offer : EnchanterManager.upgradeOffers(stack, handler.treasureAllowedProp())) {
                rows.add(new Row(offer.enchantment(), offer.level(),
                        EnchanterManager.upgradeCost(offer.enchantment(), offer.level(), handler.multiplierProp())));
            }
        }
        return rows;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x, y = this.y;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.textRenderer, "Enchanter", x + W / 2, y + 8);

        // Input slot + item summary + repair button.
        NotchWidgets.slot(ctx, x + EnchanterScreenHandler.INPUT_X - 1, y + EnchanterScreenHandler.INPUT_Y - 1);
        ItemStack stack = handler.inputStack();
        if (stack.isEmpty()) {
            ctx.drawText(this.textRenderer, "Insert an item", x + 34, y + 26, NotchTheme.TEXT_MUTED, false);
        } else {
            String name = stack.getName().getString();
            if (name.length() > 15) name = name.substring(0, 14) + "…";
            ctx.drawText(this.textRenderer, name, x + 34, y + 21, NotchTheme.TEXT_DARK, false);
            String condition = stack.isDamageable()
                    ? (stack.getMaxDamage() - stack.getDamage()) + "/" + stack.getMaxDamage() + " durability"
                    : "unbreakable";
            ctx.drawText(this.textRenderer, condition, x + 34, y + 31, NotchTheme.TEXT_MUTED, false);
        }
        int repairCost = handler.repairCostProp();
        String repairLabel = repairCost > 0 ? "Fix " + repairCost + "c" : "No repairs";
        boolean repairHover = repairCost > 0 && over(mouseX, mouseY, x + REPAIR_X, y + REPAIR_Y, REPAIR_W, REPAIR_H);
        if (repairCost > 0) {
            NotchWidgets.primaryButton(ctx, this.textRenderer, x + REPAIR_X, y + REPAIR_Y, REPAIR_W, REPAIR_H, repairLabel, repairHover);
        } else {
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + REPAIR_X, y + REPAIR_Y, REPAIR_W, REPAIR_H, repairLabel, false);
        }

        // Mode tabs.
        boolean upHover = over(mouseX, mouseY, x + 8, y + TAB_Y, 76, TAB_H);
        boolean exHover = over(mouseX, mouseY, x + 92, y + TAB_Y, 76, TAB_H);
        if (!extractMode) NotchWidgets.primaryButton(ctx, this.textRenderer, x + 8, y + TAB_Y, 76, TAB_H, "Upgrades", upHover);
        else NotchWidgets.neutralButton(ctx, this.textRenderer, x + 8, y + TAB_Y, 76, TAB_H, "Upgrades", upHover);
        if (extractMode) NotchWidgets.primaryButton(ctx, this.textRenderer, x + 92, y + TAB_Y, 76, TAB_H, "Extract", exHover);
        else NotchWidgets.neutralButton(ctx, this.textRenderer, x + 92, y + TAB_Y, 76, TAB_H, "Extract", exHover);

        // Offer rows.
        List<Row> rows = rows();
        clampScroll(rows.size());
        if (rows.isEmpty()) {
            String hint = stack.isEmpty() ? "Insert an item above."
                    : extractMode ? "No enchantments to extract." : "Nothing left to upgrade.";
            NotchWidgets.centerText(ctx, this.textRenderer, hint, x + W / 2, y + LIST_Y + 30, NotchTheme.TEXT_MUTED, false);
        }
        for (int v = 0; v < VISIBLE_ROWS; v++) {
            int i = scroll + v;
            if (i >= rows.size()) break;
            Row row = rows.get(i);
            int ry = y + LIST_Y + v * ROW_STEP;
            String label = row.ench().getName(row.level()).getString() + " — " + row.cost() + "c";
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + ROW_X, ry, ROW_W, ROW_H, label,
                    over(mouseX, mouseY, x + ROW_X, ry, ROW_W, ROW_H));
        }
        if (rows.size() > VISIBLE_ROWS) {
            NotchWidgets.centerText(ctx, this.textRenderer, (scroll + 1) + "-" + Math.min(scroll + VISIBLE_ROWS, rows.size())
                    + " of " + rows.size() + " (scroll)", x + W / 2, y + 128, NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.divider(ctx, x + 8, y + 136, W - 16);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                NotchWidgets.slot(ctx, x + EnchanterScreenHandler.INV_X + col * 18 - 1,
                        y + EnchanterScreenHandler.INV_Y + row * 18 - 1);
            }
        }
        for (int col = 0; col < 9; col++) {
            NotchWidgets.slot(ctx, x + EnchanterScreenHandler.INV_X + col * 18 - 1,
                    y + EnchanterScreenHandler.HOTBAR_Y - 1);
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
            if (handler.repairCostProp() > 0 && over(mx, my, x + REPAIR_X, y + REPAIR_Y, REPAIR_W, REPAIR_H)) {
                NotchPacketsClient.sendEnchanterAction(EnchanterScreenHandler.ACTION_REPAIR, "");
                return true;
            }
            if (over(mx, my, x + 8, y + TAB_Y, 76, TAB_H)) {
                extractMode = false;
                scroll = 0;
                return true;
            }
            if (over(mx, my, x + 92, y + TAB_Y, 76, TAB_H)) {
                extractMode = true;
                scroll = 0;
                return true;
            }
            List<Row> rows = rows();
            for (int v = 0; v < VISIBLE_ROWS; v++) {
                int i = scroll + v;
                if (i >= rows.size()) break;
                int ry = y + LIST_Y + v * ROW_STEP;
                if (over(mx, my, x + ROW_X, ry, ROW_W, ROW_H)) {
                    String id = String.valueOf(Registries.ENCHANTMENT.getId(rows.get(i).ench()));
                    NotchPacketsClient.sendEnchanterAction(extractMode
                            ? EnchanterScreenHandler.ACTION_EXTRACT
                            : EnchanterScreenHandler.ACTION_UPGRADE, id);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        scroll -= (int) Math.signum(amount);
        clampScroll(rows().size());
        return true;
    }

    private void clampScroll(int rowCount) {
        scroll = Math.max(0, Math.min(scroll, Math.max(0, rowCount - VISIBLE_ROWS)));
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }
}
