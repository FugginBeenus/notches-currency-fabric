package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.trade.TradeOffersScreenHandler;
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

public class TradeOffersScreen extends AbstractContainerScreen<TradeOffersScreenHandler> {

    private static final int W = 248, H = 232;
    private static final int ROW_X = 8, ROW_W = 232, ROW_H = 20;
    private static final int IN_Y = 34, MINE_Y = 142, ROW_STEP = 21;
    private static final int PG_LX = 190, PG_RX = 226, PG_BW = 14, PG_H = 11;

    private static ItemStack coin;
    private static ItemStack coin() {
        if (coin == null) coin = new ItemStack(net.fugginbeenus.notchcurrency.registry.ModItems.NOTCH_COIN);
        return coin;
    }

    private static final int ACTION_ACCEPT = 0, ACTION_CANCEL = 1;

    public TradeOffersScreen(TradeOffersScreenHandler handler, Inventory inv, Component title) {
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

    private int inY(int i) { return this.topPos + IN_Y + i * ROW_STEP; }
    private int mineY(int i) { return this.topPos + MINE_Y + i * ROW_STEP; }

    private record Row(ItemStack icon, UUID id, java.util.List<ItemStack> gives, long giveCoins,
                       long price, java.util.List<ItemStack> wants, String from, String target) {}

    private static java.util.List<ItemStack> readStacks(CompoundTag t, String key) {
        java.util.List<ItemStack> out = new ArrayList<>();
        net.minecraft.nbt.ListTag list = t.getList(key, net.minecraft.nbt.Tag.TAG_COMPOUND);
        for (int i = 0; i < list.size(); i++) {
            ItemStack st = StackData.readStack(list.getCompound(i));
            if (!st.isEmpty()) out.add(st);
        }
        return out;
    }

    private Row row(ItemStack stack) {
        if (stack.isEmpty()) return null;
        CompoundTag t = StackData.getData(stack);
        if (!net.fugginbeenus.notchcurrency.compat.Nbt.hasUuid(t, "nc_oid")) return null;
        return new Row(stack, net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(t, "nc_oid"), readStacks(t, "nc_gives"), t.getLong("nc_gcoins"),
                t.getLong("nc_price"), readStacks(t, "nc_wants"),
                t.getString("nc_from"), t.getString("nc_target"));
    }

    private void drawRow(GuiGraphics ctx, Row r, int ry, boolean mine, int mouseX, int mouseY) {
        int x = this.leftPos;
        boolean hover = over(mouseX, mouseY, x + ROW_X, ry, ROW_W, ROW_H);
        NotchWidgets.button(ctx, x + ROW_X, ry, ROW_W, ROW_H, hover, false);

        int gx = x + ROW_X + 4;
        if (r.giveCoins() > 0) {
            gx += NotchWidgets.coinField(ctx, this.font, coin(), gx, ry + 2, r.giveCoins(), NotchWidgets.COIN_GOLD) + 4;
        }
        int shown = 0;
        for (ItemStack st : r.gives()) {
            if (shown >= 2 || gx > x + ROW_X + 48) break;
            ctx.renderItem(st, gx, ry + 2);
            ctx.renderItemDecorations(this.font, st, gx, ry + 2);
            gx += 22;
            shown++;
        }
        int extra = r.gives().size() - shown;
        if (extra > 0) {
            ctx.drawString(this.font, "+" + extra, gx, ry + 6, NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.arrowRight(ctx, x + ROW_X + 86, ry + 6, NotchTheme.TEXT_MUTED);

        int ix = x + ROW_X + 106;
        if (r.price() > 0) {
            ix += NotchWidgets.coinField(ctx, this.font, coin(), ix, ry + 2, r.price(), NotchWidgets.COIN_GOLD) + 4;
        }
        int wantsShown = 0;
        for (ItemStack st : r.wants()) {
            if (wantsShown >= 2 || ix > x + ROW_X + 150) break;
            ctx.renderItem(st, ix, ry + 2);
            ctx.renderItemDecorations(this.font, st, ix, ry + 2);
            ix += 22;
            wantsShown++;
        }
        int wantsExtra = r.wants().size() - wantsShown;
        if (wantsExtra > 0) {
            ctx.drawString(this.font, "+" + wantsExtra, ix, ry + 6, NotchTheme.TEXT_MUTED, false);
        }
        if (r.price() <= 0 && r.wants().isEmpty()) {
            ctx.drawString(this.font, "free", ix, ry + 6, NotchTheme.TEXT_MUTED, false);
        }

        int bx = x + ROW_X + ROW_W - 54;
        if (mine) {
            NotchWidgets.dangerButton(ctx, this.font, bx, ry + 2, 50, 16, "Cancel",
                    over(mouseX, mouseY, bx, ry + 2, 50, 16));
        } else {
            NotchWidgets.primaryButton(ctx, this.font, bx, ry + 2, 50, 16, "Accept",
                    over(mouseX, mouseY, bx, ry + 2, 50, 16));
        }
    }

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = this.leftPos, y = this.topPos;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.font, "Trade Offers", x + W / 2, y + 8, NotchWidgets.Plate.BRASS);

        section(ctx, "Offers for you", y + IN_Y - 8, TradeOffersScreenHandler.P_PAGE,
                TradeOffersScreenHandler.P_TOTAL_PAGES, mouseX, mouseY);
        boolean anyIn = false;
        for (int i = 0; i < TradeOffersScreenHandler.INCOMING; i++) {
            Row r = row(menu.incomingStack(i));
            if (r == null) continue;
            anyIn = true;
            drawRow(ctx, r, inY(i), false, mouseX, mouseY);
        }
        if (!anyIn) {
            NotchWidgets.centerText(ctx, this.font, "No offers waiting for you.",
                    x + W / 2, y + IN_Y + 34, NotchTheme.TEXT_MUTED, false);
        }

        section(ctx, "Your offers", y + MINE_Y - 8, TradeOffersScreenHandler.P_MINE_PAGE,
                TradeOffersScreenHandler.P_MINE_PAGES, mouseX, mouseY);
        boolean anyMine = false;
        for (int i = 0; i < TradeOffersScreenHandler.OUTGOING; i++) {
            Row r = row(menu.outgoingStack(i));
            if (r == null) continue;
            anyMine = true;
            drawRow(ctx, r, mineY(i), true, mouseX, mouseY);
        }
        if (!anyMine) {
            NotchWidgets.centerText(ctx, this.font, "You have no open offers.",
                    x + W / 2, y + MINE_Y + 28, NotchTheme.TEXT_MUTED, false);
            NotchWidgets.centerText(ctx, this.font, "Make one with /trade offer",
                    x + W / 2, y + MINE_Y + 40, NotchTheme.TEXT_MUTED, false);
        }
        //? if >=26.1 {
        /*
        super.extractContents(ctx, mouseX, mouseY, delta);
        *///?}
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
        // Row tooltip: the full exchange, spelled out.
        for (int i = 0; i < TradeOffersScreenHandler.INCOMING; i++) {
            if (tooltipFor(ctx, row(menu.incomingStack(i)), inY(i), false, mouseX, mouseY)) return;
        }
        for (int i = 0; i < TradeOffersScreenHandler.OUTGOING; i++) {
            if (tooltipFor(ctx, row(menu.outgoingStack(i)), mineY(i), true, mouseX, mouseY)) return;
        }
    }

    private void section(GuiGraphics ctx, String label, int ly, int pageProp, int pagesProp, int mouseX, int mouseY) {
        int x = this.leftPos, pages = menu.prop(pagesProp);
        int ruleEnd = pages > 1 ? PG_LX - 12 : W - 16;
        NotchWidgets.stampLabel(ctx, x + 8, ly, ruleEnd, label);
        if (pages > 1) {
            int py = ly - PG_H / 2;
            NotchWidgets.pager(ctx, this.font, x + PG_LX, x + PG_RX, py, PG_BW, PG_H, menu.prop(pageProp) + 1, pages,
                    over(mouseX, mouseY, x + PG_LX, py, PG_BW, PG_H), over(mouseX, mouseY, x + PG_RX, py, PG_BW, PG_H));
        }
    }

    private boolean pageClick(int mx, int my, int ly, int pagesProp, int backId) {
        if (menu.prop(pagesProp) <= 1) return false;
        int py = topPos + ly - PG_H / 2;
        if (over(mx, my, leftPos + PG_LX, py, PG_BW, PG_H)) { NotchWidgets.tick(); clickButton(backId); return true; }
        if (over(mx, my, leftPos + PG_RX, py, PG_BW, PG_H)) { NotchWidgets.tick(); clickButton(backId + 1); return true; }
        return false;
    }

    private boolean tooltipFor(GuiGraphics ctx, Row r, int ry, boolean mine, int mouseX, int mouseY) {
        if (r == null || !over(mouseX, mouseY, leftPos + ROW_X, ry, ROW_W - 58, ROW_H)) return false;
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(mine ? "They receive:" : "You receive:").withStyle(ChatFormatting.GRAY));
        for (ItemStack st : r.gives()) {
            lines.add(Component.literal("  " + st.getCount() + "× ").append(st.getHoverName()));
        }
        if (r.giveCoins() > 0) {
            lines.add(Component.literal("  ").append(NotchWidgets.priceText(r.giveCoins(), "", 0)));
        }
        lines.add(Component.literal("For:").withStyle(ChatFormatting.GRAY));
        if (r.price() > 0) {
            lines.add(Component.literal("  ").append(NotchWidgets.priceText(r.price(), "", 0)));
        }
        for (ItemStack st : r.wants()) {
            lines.add(Component.literal("  " + st.getCount() + "× ").append(st.getHoverName()));
        }
        if (r.price() <= 0 && r.wants().isEmpty()) {
            lines.add(Component.literal("  nothing - it's free").withStyle(ChatFormatting.GRAY));
        }
        lines.add(Component.literal(mine ? (r.target().isEmpty() ? "Open - anyone can accept" : "Reserved for " + r.target())
                : "From " + r.from()).withStyle(ChatFormatting.DARK_GRAY));
        ctx.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        return true;
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
            if (pageClick(mx, my, IN_Y - 8, TradeOffersScreenHandler.P_TOTAL_PAGES, 0)) return true;
            if (pageClick(mx, my, MINE_Y - 8, TradeOffersScreenHandler.P_MINE_PAGES, 2)) return true;
            int bxIn = leftPos + ROW_X + ROW_W - 54;
            for (int i = 0; i < TradeOffersScreenHandler.INCOMING; i++) {
                Row r = row(menu.incomingStack(i));
                if (r != null && over(mx, my, bxIn, inY(i) + 2, 50, 16)) {
                    NotchWidgets.click();
                    NotchPacketsClient.sendTradeOfferAction(r.id(), ACTION_ACCEPT);
                    return true;
                }
            }
            for (int i = 0; i < TradeOffersScreenHandler.OUTGOING; i++) {
                Row r = row(menu.outgoingStack(i));
                if (r != null && over(mx, my, bxIn, mineY(i) + 2, 50, 16)) {
                    NotchWidgets.click();
                    NotchPacketsClient.sendTradeOfferAction(r.id(), ACTION_CANCEL);
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
