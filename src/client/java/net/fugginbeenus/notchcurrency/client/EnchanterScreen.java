package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.enchanter.EnchanterManager;
import net.fugginbeenus.notchcurrency.economy.enchanter.EnchanterScreenHandler;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The Enchanter, card edition: drop an item in the slot, then Repair it, buy exact enchant levels
 * (Upgrades), pull an enchant onto a book (Extract), or break the item back into its crafting
 * ingredients (Uncraft). Each offer is a card — icon, name, one-line description, coin price —
 * in a scrollable list. Offers/prices derive from the same code the server validates with.
 */
public class EnchanterScreen extends HandledScreen<EnchanterScreenHandler> {

    private static final int W = 256, H = 238;
    private static final int TAB_Y = 46, TAB_H = 14;
    private static final int LIST_X = 10, LIST_Y = 66, CARD_W = 210, CARD_H = 26, CARD_STEP = 28, VISIBLE = 3;
    private static final int SB_X = 226, SB_Y = 66, SB_W = 8, SB_H = 80;

    private static final ItemStack COIN =
            new ItemStack(net.fugginbeenus.notchcurrency.registry.ModItems.NOTCH_COIN);

    private int tab = 0; // 0 upgrades / 1 extract / 2 uncraft
    private int scroll = 0;
    private boolean draggingScroll;

    // Uncraft preview cache (recipe scans are not free — recompute only when the item changes).
    private ItemStack planFor = ItemStack.EMPTY;
    private EnchanterManager.UncraftPlan plan;

    public EnchanterScreen(EnchanterScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = W;
        this.backgroundHeight = H;
        this.titleX = -1000;
        this.playerInventoryTitleX = -1000;
    }

    /** One card: the enchantment, the level shown, its cost, and the book stack (icon + tooltip). */
    private record Card(Enchantment ench, int level, long cost, ItemStack book) {}

    private static ItemStack bookOf(Enchantment ench, int level) {
        return net.fugginbeenus.notchcurrency.compat.Ench.enchantedBook(ench, level);
    }

    private List<Card> cards() {
        ItemStack stack = handler.inputStack();
        List<Card> cards = new ArrayList<>();
        if (stack.isEmpty()) return cards;
        if (tab == 1) {
            for (Map.Entry<Enchantment, Integer> e : net.fugginbeenus.notchcurrency.compat.Ench.get(stack).entrySet()) {
                cards.add(new Card(e.getKey(), e.getValue(),
                        EnchanterManager.extractPrice(e.getKey(), e.getValue(), handler.pricing()),
                        bookOf(e.getKey(), e.getValue())));
            }
        } else if (tab == 0) {
            for (EnchanterManager.Offer offer : EnchanterManager.upgradeOffers(stack, handler.treasureAllowedProp())) {
                cards.add(new Card(offer.enchantment(), offer.level(),
                        EnchanterManager.upgradeCost(offer.enchantment(), offer.level(), handler.pricing()),
                        bookOf(offer.enchantment(), offer.level())));
            }
        }
        return cards;
    }

    private EnchanterManager.UncraftPlan uncraftPlan() {
        ItemStack stack = handler.inputStack();
        if (!ItemStack.areEqual(stack, planFor)) {
            planFor = stack.copy();
            MinecraftClient c = MinecraftClient.getInstance();
            plan = (c.world == null) ? null : EnchanterManager.uncraftPlan(stack, c.world);
        }
        return plan;
    }

    private int cardY(int v) { return this.y + LIST_Y + v * CARD_STEP; }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x, y = this.y;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.textRenderer, "Enchanter", x + W / 2, y + 8);

        // Input slot + item summary + repair.
        NotchWidgets.slot(ctx, x + EnchanterScreenHandler.INPUT_X - 1, y + EnchanterScreenHandler.INPUT_Y - 1);
        ItemStack stack = handler.inputStack();
        if (stack.isEmpty()) {
            ctx.drawText(this.textRenderer, "Insert an item", x + 34, y + 26, NotchTheme.TEXT_MUTED, false);
        } else {
            String name = stack.getName().getString();
            while (name.length() > 3 && this.textRenderer.getWidth(name) > 112) name = name.substring(0, name.length() - 2) + "…";
            ctx.drawText(this.textRenderer, name, x + 34, y + 21, NotchTheme.TEXT_DARK, false);
            String condition = stack.isDamageable()
                    ? (stack.getMaxDamage() - stack.getDamage()) + "/" + stack.getMaxDamage() + " durability"
                    : "no wear";
            ctx.drawText(this.textRenderer, condition, x + 34, y + 31, NotchTheme.TEXT_MUTED, false);
        }
        int repairCost = handler.repairCostProp();
        String repairLabel = repairCost > 0 ? "Repair · " + NotchWidgets.compactCount(repairCost) : "No repairs";
        if (repairCost > 0) {
            NotchWidgets.primaryButton(ctx, this.textRenderer, x + 156, y + 21, 92, 18, repairLabel,
                    over(mouseX, mouseY, x + 156, y + 21, 92, 18));
        } else {
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + 156, y + 21, 92, 18, repairLabel, false);
        }

        // Service tabs.
        String[] tabs = {"Upgrades", "Extract", "Uncraft"};
        for (int i = 0; i < 3; i++) {
            int tx = x + 8 + i * 82;
            boolean hover = over(mouseX, mouseY, tx, y + TAB_Y, 76, TAB_H);
            if (i == tab) NotchWidgets.primaryButton(ctx, this.textRenderer, tx, y + TAB_Y, 76, TAB_H, tabs[i], hover);
            else NotchWidgets.neutralButton(ctx, this.textRenderer, tx, y + TAB_Y, 76, TAB_H, tabs[i], hover);
        }

        // Recessed container for the service area.
        NotchWidgets.inset(ctx, x + 6, y + 63, 234, 86, NotchTheme.PANEL_MID);

        if (tab == 2) {
            drawUncraft(ctx, mouseX, mouseY, stack);
        } else {
            drawCards(ctx, mouseX, mouseY, stack);
        }

        NotchWidgets.divider(ctx, x + 8, y + 151, W - 16);
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

    private void drawCards(DrawContext ctx, int mouseX, int mouseY, ItemStack stack) {
        final int x = this.x, y = this.y;
        List<Card> cards = cards();
        clampScroll(cards.size());
        if (cards.isEmpty()) {
            String hint = stack.isEmpty() ? "Insert an item above."
                    : tab == 1 ? "No enchantments to extract." : "Nothing left to upgrade.";
            NotchWidgets.centerText(ctx, this.textRenderer, hint, x + W / 2, y + LIST_Y + 34, NotchTheme.TEXT_MUTED, false);
            return;
        }
        for (int v = 0; v < VISIBLE; v++) {
            int i = scroll + v;
            if (i >= cards.size()) break;
            Card c = cards.get(i);
            int cy = cardY(v);
            boolean hover = over(mouseX, mouseY, x + LIST_X, cy, CARD_W, CARD_H);
            NotchWidgets.button(ctx, x + LIST_X, cy, CARD_W, CARD_H, hover, false);
            ctx.drawItem(c.book(), x + LIST_X + 4, cy + 5);

            String name = net.fugginbeenus.notchcurrency.compat.Ench.name(c.ench(), c.level()).getString();
            while (name.length() > 3 && this.textRenderer.getWidth(name) > CARD_W - 52) name = name.substring(0, name.length() - 2) + "…";
            ctx.drawText(this.textRenderer, name, x + LIST_X + 24, cy + 9,
                    net.fugginbeenus.notchcurrency.compat.Ench.isTreasure(c.ench()) ? 0xFF9A5CC6 : NotchTheme.TEXT_DARK, false);

            ctx.drawItem(COIN, x + LIST_X + CARD_W - 22, cy + 5);
            ctx.drawItemInSlot(this.textRenderer, COIN, x + LIST_X + CARD_W - 22, cy + 5,
                    NotchWidgets.compactCount(c.cost()));
        }

        // Scrollbar.
        NotchWidgets.slot(ctx, x + SB_X, y + SB_Y, SB_W, SB_H);
        if (cards.size() > VISIBLE) {
            int th = thumbH(cards.size());
            NotchWidgets.button(ctx, x + SB_X + 1, thumbY(cards.size()), SB_W - 2, th, false, false);
        }
    }

    private void drawUncraft(DrawContext ctx, int mouseX, int mouseY, ItemStack stack) {
        final int x = this.x, y = this.y;
        if (stack.isEmpty()) {
            NotchWidgets.centerText(ctx, this.textRenderer, "Insert an item above.", x + W / 2, y + LIST_Y + 34,
                    NotchTheme.TEXT_MUTED, false);
            return;
        }
        EnchanterManager.UncraftPlan p = uncraftPlan();
        if (p == null) {
            if (stack.isDamaged()) {
                NotchWidgets.centerText(ctx, this.textRenderer, "Repair it first —", x + W / 2, y + LIST_Y + 28,
                        NotchTheme.TEXT_MUTED, false);
                NotchWidgets.centerText(ctx, this.textRenderer, "worn gear can't be salvaged.", x + W / 2,
                        y + LIST_Y + 40, NotchTheme.TEXT_MUTED, false);
            } else {
                NotchWidgets.centerText(ctx, this.textRenderer, "No crafting recipe to reverse.", x + W / 2,
                        y + LIST_Y + 34, NotchTheme.TEXT_MUTED, false);
            }
            return;
        }
        String head = (p.consumed() > 1 ? p.consumed() + "× breaks" : "Breaks") + " back into:";
        ctx.drawText(this.textRenderer, head, x + 12, y + 68, NotchTheme.TEXT_DARK, false);
        int ix = x + 12;
        for (ItemStack ret : p.returns()) {
            if (ix > x + 210) break;
            NotchWidgets.slot(ctx, ix - 1, y + 79);
            ctx.drawItem(ret, ix, y + 80);
            ctx.drawItemInSlot(this.textRenderer, ret, ix, y + 80);
            ix += 20;
        }
        if (stack.hasEnchantments()) {
            ctx.drawText(this.textRenderer, "(enchantments are lost)", x + 12, y + 101, NotchTheme.TEXT_RED, false);
        }
        NotchWidgets.primaryButton(ctx, this.textRenderer, x + 12, y + 126, 150, 18,
                "Uncraft · " + NotchWidgets.compactCount(handler.uncraftCostProp()),
                over(mouseX, mouseY, x + 12, y + 126, 150, 18));
    }

    private int thumbH(int count) { return Math.max(14, SB_H * VISIBLE / Math.max(VISIBLE, count)); }

    private int thumbY(int count) {
        int max = Math.max(1, count - VISIBLE);
        return this.y + SB_Y + (int) ((scroll / (double) max) * (SB_H - thumbH(count)));
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
        // Card tooltip: full name, description, price.
        if (tab != 2) {
            List<Card> cards = cards();
            for (int v = 0; v < VISIBLE; v++) {
                int i = scroll + v;
                if (i >= cards.size()) break;
                if (over(mouseX, mouseY, x + LIST_X, cardY(v), CARD_W, CARD_H)) {
                    Card c = cards.get(i);
                    // The vanilla enchanted-book tooltip, plus the price and the click hint.
                    List<Text> lines = new ArrayList<>(getTooltipFromItem(
                            MinecraftClient.getInstance(), c.book()));
                    lines.add(NotchWidgets.priceText(c.cost(), "", 0));
                    lines.add(Text.literal(tab == 1 ? "Click to extract onto a book" : "Click to apply")
                            .formatted(Formatting.DARK_GRAY));
                    ctx.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
                    return;
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            if (handler.repairCostProp() > 0 && over(mx, my, x + 156, y + 21, 92, 18)) {
                NotchWidgets.click();
                NotchPacketsClient.sendEnchanterAction(EnchanterScreenHandler.ACTION_REPAIR, "");
                return true;
            }
            for (int i = 0; i < 3; i++) {
                if (over(mx, my, x + 8 + i * 82, y + TAB_Y, 76, TAB_H)) {
                    if (tab != i) NotchWidgets.tick();
                    tab = i;
                    scroll = 0;
                    return true;
                }
            }
            if (tab == 2) {
                if (uncraftPlan() != null && over(mx, my, x + 12, y + 126, 150, 18)) {
                    planFor = ItemStack.EMPTY; // force a re-quote after the server changes the slot
                    NotchWidgets.click();
                    NotchPacketsClient.sendEnchanterAction(EnchanterScreenHandler.ACTION_UNCRAFT, "");
                    return true;
                }
            } else {
                List<Card> cards = cards();
                for (int v = 0; v < VISIBLE; v++) {
                    int i = scroll + v;
                    if (i >= cards.size()) break;
                    if (over(mx, my, x + LIST_X, cardY(v), CARD_W, CARD_H)) {
                        String id = String.valueOf(net.fugginbeenus.notchcurrency.compat.Ench.idOf(cards.get(i).ench()));
                        NotchWidgets.click();
                        NotchPacketsClient.sendEnchanterAction(tab == 1
                                ? EnchanterScreenHandler.ACTION_EXTRACT
                                : EnchanterScreenHandler.ACTION_UPGRADE, id);
                        return true;
                    }
                }
                int count = cards.size();
                if (count > VISIBLE && over(mx, my, x + SB_X, y + SB_Y, SB_W, SB_H)) {
                    if (my < thumbY(count)) { NotchWidgets.tick(); scroll--; }
                    else if (my >= thumbY(count) + thumbH(count)) { NotchWidgets.tick(); scroll++; }
                    else draggingScroll = true;
                    clampScroll(count);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (draggingScroll) {
            int count = cards().size();
            int track = SB_H - thumbH(count);
            if (track > 0) {
                int rel = (int) mouseY - (this.y + SB_Y) - thumbH(count) / 2;
                scroll = Math.round(rel / (float) track * Math.max(0, count - VISIBLE));
                clampScroll(count);
            }
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
    //? if >=1.21 {
    /*public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
    *///?} else {
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    //?}
        if (tab != 2) {
            scroll -= (int) Math.signum(amount);
            clampScroll(cards().size());
            return true;
        }
        //? if >=1.21 {
        /*return super.mouseScrolled(mouseX, mouseY, horizontalAmount, amount);
        *///?} else {
        return super.mouseScrolled(mouseX, mouseY, amount);
        //?}
    }

    private void clampScroll(int count) {
        scroll = Math.max(0, Math.min(scroll, Math.max(0, count - VISIBLE)));
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    //? if >=1.21 {
    /*@Override
    protected void applyBlur(float delta) {
        // No 1.21 menu blur behind the mod's screens — they draw crisp panels over the world.
    }
    *///?}
}
