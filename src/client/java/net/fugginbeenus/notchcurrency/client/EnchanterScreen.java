package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.enchanter.EnchanterManager;
import net.fugginbeenus.notchcurrency.economy.enchanter.EnchanterScreenHandler;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class EnchanterScreen extends AbstractContainerScreen<EnchanterScreenHandler> {

    private static final int W = 256, H = 238;
    private static final int TAB_Y = 46, TAB_H = 14;
    private static final int LIST_X = 10, LIST_Y = 66, CARD_W = 210, CARD_H = 26, CARD_STEP = 28, VISIBLE = 3;
    private static final int SB_X = 226, SB_Y = 66, SB_W = 8, SB_H = 80;

    private static ItemStack coin;

    private static ItemStack coin() {
        // Built on first use: from 26.2 an ItemStack cannot be made before item components are bound,
        // and a static field would run while the class loads, which can be earlier than that.
        if (coin == null) coin = new ItemStack(net.fugginbeenus.notchcurrency.registry.ModItems.NOTCH_COIN);
        return coin;
    }

    private int tab = 0; // 0 upgrades / 1 extract / 2 uncraft
    private int scroll = 0;
    private boolean draggingScroll;

    // Uncraft preview cache (recipe scans are not free: recompute only when the item changes).
    private ItemStack planFor = ItemStack.EMPTY;
    private EnchanterManager.UncraftPlan plan;

    public EnchanterScreen(EnchanterScreenHandler handler, Inventory inv, Component title) {
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

    private record Card(Enchantment ench, int level, long cost, ItemStack book) {}

    private static ItemStack bookOf(Enchantment ench, int level) {
        return net.fugginbeenus.notchcurrency.compat.Ench.enchantedBook(ench, level);
    }

    private List<Card> cards() {
        ItemStack stack = menu.inputStack();
        List<Card> cards = new ArrayList<>();
        if (stack.isEmpty()) return cards;
        if (tab == 1) {
            for (Map.Entry<Enchantment, Integer> e : net.fugginbeenus.notchcurrency.compat.Ench.get(stack).entrySet()) {
                cards.add(new Card(e.getKey(), e.getValue(),
                        EnchanterManager.extractPrice(e.getKey(), e.getValue(), menu.pricing()),
                        bookOf(e.getKey(), e.getValue())));
            }
        } else if (tab == 0) {
            for (EnchanterManager.Offer offer : EnchanterManager.upgradeOffers(stack, menu.treasureAllowedProp())) {
                cards.add(new Card(offer.enchantment(), offer.level(),
                        EnchanterManager.upgradeCost(offer.enchantment(), offer.level(), menu.pricing()),
                        bookOf(offer.enchantment(), offer.level())));
            }
        }
        return cards;
    }

    private EnchanterManager.UncraftPlan uncraftPlan() {
        ItemStack stack = menu.inputStack();
        if (!ItemStack.matches(stack, planFor)) {
            planFor = stack.copy();
            Minecraft c = Minecraft.getInstance();
            plan = (c.level == null) ? null : EnchanterManager.uncraftPlan(stack, c.level);
        }
        return plan;
    }

    private int cardY(int v) { return this.topPos + LIST_Y + v * CARD_STEP; }

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = this.leftPos, y = this.topPos;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.font, "Enchanter", x + W / 2, y + 8);

        // Input slot + item summary + repair.
        NotchWidgets.slot(ctx, x + EnchanterScreenHandler.INPUT_X - 1, y + EnchanterScreenHandler.INPUT_Y - 1);
        ItemStack stack = menu.inputStack();
        if (stack.isEmpty()) {
            ctx.drawString(this.font, "Insert an item", x + 34, y + 26, NotchTheme.TEXT_MUTED, false);
        } else {
            String name = stack.getHoverName().getString();
            while (name.length() > 3 && this.font.width(name) > 112) name = name.substring(0, name.length() - 2) + "…";
            ctx.drawString(this.font, name, x + 34, y + 21, NotchTheme.TEXT_DARK, false);
            String condition = stack.isDamageableItem()
                    ? (stack.getMaxDamage() - stack.getDamageValue()) + "/" + stack.getMaxDamage() + " durability"
                    : "no wear";
            ctx.drawString(this.font, condition, x + 34, y + 31, NotchTheme.TEXT_MUTED, false);
        }
        int repairCost = menu.repairCostProp();
        String repairLabel = repairCost > 0 ? "Repair · " + NotchWidgets.compactCount(repairCost) : "No repairs";
        if (repairCost > 0) {
            NotchWidgets.primaryButton(ctx, this.font, x + 156, y + 21, 92, 18, repairLabel,
                    over(mouseX, mouseY, x + 156, y + 21, 92, 18));
        } else {
            NotchWidgets.neutralButton(ctx, this.font, x + 156, y + 21, 92, 18, repairLabel, false);
        }

        // Service tabs.
        String[] tabs = {"Upgrades", "Extract", "Uncraft"};
        for (int i = 0; i < 3; i++) {
            int tx = x + 8 + i * 82;
            boolean hover = over(mouseX, mouseY, tx, y + TAB_Y, 76, TAB_H);
            if (i == tab) NotchWidgets.primaryButton(ctx, this.font, tx, y + TAB_Y, 76, TAB_H, tabs[i], hover);
            else NotchWidgets.neutralButton(ctx, this.font, tx, y + TAB_Y, 76, TAB_H, tabs[i], hover);
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

    private void drawCards(GuiGraphics ctx, int mouseX, int mouseY, ItemStack stack) {
        final int x = this.leftPos, y = this.topPos;
        List<Card> cards = cards();
        clampScroll(cards.size());
        if (cards.isEmpty()) {
            String hint = stack.isEmpty() ? "Insert an item above."
                    : tab == 1 ? "No enchantments to extract." : "Nothing left to upgrade.";
            NotchWidgets.centerText(ctx, this.font, hint, x + W / 2, y + LIST_Y + 34, NotchTheme.TEXT_MUTED, false);
            return;
        }
        for (int v = 0; v < VISIBLE; v++) {
            int i = scroll + v;
            if (i >= cards.size()) break;
            Card c = cards.get(i);
            int cy = cardY(v);
            boolean hover = over(mouseX, mouseY, x + LIST_X, cy, CARD_W, CARD_H);
            NotchWidgets.button(ctx, x + LIST_X, cy, CARD_W, CARD_H, hover, false);
            ctx.renderItem(c.book(), x + LIST_X + 4, cy + 5);

            String name = net.fugginbeenus.notchcurrency.compat.Ench.name(c.ench(), c.level()).getString();
            while (name.length() > 3 && this.font.width(name) > CARD_W - 52) name = name.substring(0, name.length() - 2) + "…";
            ctx.drawString(this.font, name, x + LIST_X + 24, cy + 9,
                    net.fugginbeenus.notchcurrency.compat.Ench.isTreasure(c.ench()) ? 0xFF9A5CC6 : NotchTheme.TEXT_DARK, false);

            ctx.renderItem(coin(), x + LIST_X + CARD_W - 22, cy + 5);
            ctx.renderItemDecorations(this.font, coin(), x + LIST_X + CARD_W - 22, cy + 5,
                    NotchWidgets.compactCount(c.cost()));
        }

        // Scrollbar.
        NotchWidgets.slot(ctx, x + SB_X, y + SB_Y, SB_W, SB_H);
        if (cards.size() > VISIBLE) {
            int th = thumbH(cards.size());
            NotchWidgets.button(ctx, x + SB_X + 1, thumbY(cards.size()), SB_W - 2, th, false, false);
        }
    }

    private void drawUncraft(GuiGraphics ctx, int mouseX, int mouseY, ItemStack stack) {
        final int x = this.leftPos, y = this.topPos;
        if (stack.isEmpty()) {
            NotchWidgets.centerText(ctx, this.font, "Insert an item above.", x + W / 2, y + LIST_Y + 34,
                    NotchTheme.TEXT_MUTED, false);
            return;
        }
        EnchanterManager.UncraftPlan p = uncraftPlan();
        if (p == null) {
            if (stack.isDamaged()) {
                NotchWidgets.centerText(ctx, this.font, "Repair it first -", x + W / 2, y + LIST_Y + 28,
                        NotchTheme.TEXT_MUTED, false);
                NotchWidgets.centerText(ctx, this.font, "Worn gear can't be salvaged.", x + W / 2,
                        y + LIST_Y + 40, NotchTheme.TEXT_MUTED, false);
            } else {
                NotchWidgets.centerText(ctx, this.font, "No crafting recipe to reverse.", x + W / 2,
                        y + LIST_Y + 34, NotchTheme.TEXT_MUTED, false);
            }
            return;
        }
        String head = (p.consumed() > 1 ? p.consumed() + "× breaks" : "Breaks") + " back into:";
        ctx.drawString(this.font, head, x + 12, y + 68, NotchTheme.TEXT_DARK, false);
        int ix = x + 12;
        for (ItemStack ret : p.returns()) {
            if (ix > x + 210) break;
            NotchWidgets.slot(ctx, ix - 1, y + 79);
            ctx.renderItem(ret, ix, y + 80);
            ctx.renderItemDecorations(this.font, ret, ix, y + 80);
            ix += 20;
        }
        if (stack.isEnchanted()) {
            ctx.drawString(this.font, "(enchantments are lost)", x + 12, y + 101, NotchTheme.TEXT_RED, false);
        }
        NotchWidgets.primaryButton(ctx, this.font, x + 12, y + 126, 150, 18,
                "Uncraft · " + NotchWidgets.compactCount(menu.uncraftCostProp()),
                over(mouseX, mouseY, x + 12, y + 126, 150, 18));
    }

    private int thumbH(int count) { return Math.max(14, SB_H * VISIBLE / Math.max(VISIBLE, count)); }

    private int thumbY(int count) {
        int max = Math.max(1, count - VISIBLE);
        return this.topPos + SB_Y + (int) ((scroll / (double) max) * (SB_H - thumbH(count)));
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
        // Card tooltip: full name, description, price.
        if (tab != 2) {
            List<Card> cards = cards();
            for (int v = 0; v < VISIBLE; v++) {
                int i = scroll + v;
                if (i >= cards.size()) break;
                if (over(mouseX, mouseY, leftPos + LIST_X, cardY(v), CARD_W, CARD_H)) {
                    Card c = cards.get(i);
                    // The vanilla enchanted-book tooltip, plus the price and the click hint.
                    List<Component> lines = new ArrayList<>(getTooltipFromItem(
                            Minecraft.getInstance(), c.book()));
                    lines.add(NotchWidgets.priceText(c.cost(), "", 0));
                    lines.add(Component.literal(tab == 1 ? "Click to extract onto a book" : "Click to apply")
                            .withStyle(ChatFormatting.DARK_GRAY));
                    ctx.renderComponentTooltip(this.font, lines, mouseX, mouseY);
                    return;
                }
            }
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
            if (menu.repairCostProp() > 0 && over(mx, my, leftPos + 156, topPos + 21, 92, 18)) {
                NotchWidgets.click();
                NotchPacketsClient.sendEnchanterAction(EnchanterScreenHandler.ACTION_REPAIR, "");
                return true;
            }
            for (int i = 0; i < 3; i++) {
                if (over(mx, my, leftPos + 8 + i * 82, topPos + TAB_Y, 76, TAB_H)) {
                    if (tab != i) NotchWidgets.tick();
                    tab = i;
                    scroll = 0;
                    return true;
                }
            }
            if (tab == 2) {
                if (uncraftPlan() != null && over(mx, my, leftPos + 12, topPos + 126, 150, 18)) {
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
                    if (over(mx, my, leftPos + LIST_X, cardY(v), CARD_W, CARD_H)) {
                        String id = String.valueOf(net.fugginbeenus.notchcurrency.compat.Ench.idOf(cards.get(i).ench()));
                        NotchWidgets.click();
                        NotchPacketsClient.sendEnchanterAction(tab == 1
                                ? EnchanterScreenHandler.ACTION_EXTRACT
                                : EnchanterScreenHandler.ACTION_UPGRADE, id);
                        return true;
                    }
                }
                int count = cards.size();
                if (count > VISIBLE && over(mx, my, leftPos + SB_X, topPos + SB_Y, SB_W, SB_H)) {
                    if (my < thumbY(count)) { NotchWidgets.tick(); scroll--; }
                    else if (my >= thumbY(count) + thumbH(count)) { NotchWidgets.tick(); scroll++; }
                    else draggingScroll = true;
                    clampScroll(count);
                    return true;
                }
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
        if (draggingScroll) {
            int count = cards().size();
            int track = SB_H - thumbH(count);
            if (track > 0) {
                int rel = (int) mouseY - (this.topPos + SB_Y) - thumbH(count) / 2;
                scroll = Math.round(rel / (float) track * Math.max(0, count - VISIBLE));
                clampScroll(count);
            }
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
