package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.trade.TradeOffersScreenHandler;
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
 * The trade-offers board, in the live-trade screen's language: each offer reads as a give → get
 * exchange — the offered item, an arrow, then the coins and/or item wanted in return — with the
 * from/to detail in the hover tooltip. Offers you can accept up top (paginated), your own open
 * offers below with a cancel button. Accept/cancel go by offer id through TRADE_OFFER_ACTION.
 */
public class TradeOffersScreen extends HandledScreen<TradeOffersScreenHandler> {

    private static final int W = 248, H = 236;
    private static final int ROW_X = 8, ROW_W = 232, ROW_H = 20;
    private static final int IN_Y = 34, MINE_Y = 152, ROW_STEP = 21;

    private static final ItemStack COIN =
            new ItemStack(net.fugginbeenus.notchcurrency.registry.ModItems.NOTCH_COIN);

    // Action ids (mirror the packet).
    private static final int ACTION_ACCEPT = 0, ACTION_CANCEL = 1;

    public TradeOffersScreen(TradeOffersScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = W;
        this.backgroundHeight = H;
        this.titleX = -1000;
        this.playerInventoryTitleX = -1000;
    }

    private int inY(int i) { return this.y + IN_Y + i * ROW_STEP; }
    private int mineY(int i) { return this.y + MINE_Y + i * ROW_STEP; }

    private record Row(ItemStack icon, UUID id, java.util.List<ItemStack> gives, long giveCoins,
                       long price, java.util.List<ItemStack> wants, String from, String target) {}

    private static java.util.List<ItemStack> readStacks(NbtCompound t, String key) {
        java.util.List<ItemStack> out = new ArrayList<>();
        net.minecraft.nbt.NbtList list = t.getList(key, net.minecraft.nbt.NbtElement.COMPOUND_TYPE);
        for (int i = 0; i < list.size(); i++) {
            ItemStack st = StackData.readStack(list.getCompound(i));
            if (!st.isEmpty()) out.add(st);
        }
        return out;
    }

    private Row row(ItemStack stack) {
        if (stack.isEmpty()) return null;
        NbtCompound t = StackData.getData(stack);
        if (!t.containsUuid("nc_oid")) return null;
        return new Row(stack, t.getUuid("nc_oid"), readStacks(t, "nc_gives"), t.getLong("nc_gcoins"),
                t.getLong("nc_price"), readStacks(t, "nc_wants"),
                t.getString("nc_from"), t.getString("nc_target"));
    }

    /** Give → get, drawn like the live-trade screen: icons with counts, not text. */
    private void drawRow(DrawContext ctx, Row r, int ry, boolean mine, int mouseX, int mouseY) {
        int x = this.x;
        boolean hover = over(mouseX, mouseY, x + ROW_X, ry, ROW_W, ROW_H);
        NotchWidgets.button(ctx, x + ROW_X, ry, ROW_W, ROW_H, hover, false);

        // The give side: attached coins, then up to two item stacks, then a "+N" for the rest.
        int gx = x + ROW_X + 4;
        if (r.giveCoins() > 0) {
            ctx.drawItem(COIN, gx, ry + 2);
            ctx.drawItemInSlot(this.textRenderer, COIN, gx, ry + 2, NotchWidgets.compactCount(r.giveCoins()));
            gx += 22;
        }
        int shown = 0;
        for (ItemStack st : r.gives()) {
            if (shown >= 2 || gx > x + ROW_X + 48) break;
            ctx.drawItem(st, gx, ry + 2);
            ctx.drawItemInSlot(this.textRenderer, st, gx, ry + 2);
            gx += 22;
            shown++;
        }
        int extra = r.gives().size() - shown;
        if (extra > 0) {
            ctx.drawText(this.textRenderer, "+" + extra, gx, ry + 6, NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.arrowRight(ctx, x + ROW_X + 86, ry + 6, NotchTheme.TEXT_MUTED);

        // What they want back: coins, then up to two item stacks, then a "+N" for the rest.
        int ix = x + ROW_X + 106;
        if (r.price() > 0) {
            ctx.drawItem(COIN, ix, ry + 2);
            ctx.drawItemInSlot(this.textRenderer, COIN, ix, ry + 2, NotchWidgets.compactCount(r.price()));
            ix += 22;
        }
        int wantsShown = 0;
        for (ItemStack st : r.wants()) {
            if (wantsShown >= 2 || ix > x + ROW_X + 150) break;
            ctx.drawItem(st, ix, ry + 2);
            ctx.drawItemInSlot(this.textRenderer, st, ix, ry + 2);
            ix += 22;
            wantsShown++;
        }
        int wantsExtra = r.wants().size() - wantsShown;
        if (wantsExtra > 0) {
            ctx.drawText(this.textRenderer, "+" + wantsExtra, ix, ry + 6, NotchTheme.TEXT_MUTED, false);
        }
        if (r.price() <= 0 && r.wants().isEmpty()) {
            ctx.drawText(this.textRenderer, "free", ix, ry + 6, NotchTheme.TEXT_MUTED, false);
        }

        int bx = x + ROW_X + ROW_W - 54;
        if (mine) {
            NotchWidgets.dangerButton(ctx, this.textRenderer, bx, ry + 2, 50, 16, "Cancel",
                    over(mouseX, mouseY, bx, ry + 2, 50, 16));
        } else {
            NotchWidgets.primaryButton(ctx, this.textRenderer, bx, ry + 2, 50, 16, "Accept",
                    over(mouseX, mouseY, bx, ry + 2, 50, 16));
        }
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x, y = this.y;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.textRenderer, "Trade Offers", x + W / 2, y + 8);

        // Incoming header + pager.
        ctx.drawText(this.textRenderer, "OFFERS FOR YOU", x + 10, y + 24, NotchTheme.TEXT_DARK, false);
        int pageCount = handler.prop(TradeOffersScreenHandler.P_TOTAL_PAGES);
        if (pageCount > 1) {
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + 190, y + 22, 13, 11, "<",
                    over(mouseX, mouseY, x + 190, y + 22, 13, 11));
            NotchWidgets.centerText(ctx, this.textRenderer,
                    (handler.prop(TradeOffersScreenHandler.P_PAGE) + 1) + "/" + pageCount,
                    x + 214, y + 24, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + 227, y + 22, 13, 11, ">",
                    over(mouseX, mouseY, x + 227, y + 22, 13, 11));
        }
        boolean anyIn = false;
        for (int i = 0; i < TradeOffersScreenHandler.INCOMING; i++) {
            Row r = row(handler.incomingStack(i));
            if (r == null) continue;
            anyIn = true;
            drawRow(ctx, r, inY(i), false, mouseX, mouseY);
        }
        if (!anyIn) {
            NotchWidgets.centerText(ctx, this.textRenderer, "No offers waiting for you.",
                    x + W / 2, y + IN_Y + 20, NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.divider(ctx, x + 8, y + MINE_Y - 8, W - 16);
        ctx.drawText(this.textRenderer, "YOUR OFFERS", x + 10, y + MINE_Y - 6, NotchTheme.TEXT_DARK, false);
        boolean anyMine = false;
        for (int i = 0; i < TradeOffersScreenHandler.OUTGOING; i++) {
            Row r = row(handler.outgoingStack(i));
            if (r == null) continue;
            anyMine = true;
            drawRow(ctx, r, mineY(i), true, mouseX, mouseY);
        }
        if (!anyMine) {
            NotchWidgets.centerText(ctx, this.textRenderer, "You have no open offers. Use /trade offer to create one.",
                    x + W / 2, y + MINE_Y + 14, NotchTheme.TEXT_MUTED, false);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);
        // Row tooltip: the full exchange, spelled out.
        for (int i = 0; i < TradeOffersScreenHandler.INCOMING; i++) {
            if (tooltipFor(ctx, row(handler.incomingStack(i)), inY(i), false, mouseX, mouseY)) return;
        }
        for (int i = 0; i < TradeOffersScreenHandler.OUTGOING; i++) {
            if (tooltipFor(ctx, row(handler.outgoingStack(i)), mineY(i), true, mouseX, mouseY)) return;
        }
    }

    private boolean tooltipFor(DrawContext ctx, Row r, int ry, boolean mine, int mouseX, int mouseY) {
        // Only over the row body — the Accept/Cancel button explains itself.
        if (r == null || !over(mouseX, mouseY, x + ROW_X, ry, ROW_W - 58, ROW_H)) return false;
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal(mine ? "They receive:" : "You receive:").formatted(Formatting.GRAY));
        for (ItemStack st : r.gives()) {
            lines.add(Text.literal("  " + st.getCount() + "× ").append(st.getName()));
        }
        if (r.giveCoins() > 0) {
            lines.add(Text.literal("  ").append(NotchWidgets.priceText(r.giveCoins(), "", 0)));
        }
        lines.add(Text.literal("For:").formatted(Formatting.GRAY));
        if (r.price() > 0) {
            lines.add(Text.literal("  ").append(NotchWidgets.priceText(r.price(), "", 0)));
        }
        for (ItemStack st : r.wants()) {
            lines.add(Text.literal("  " + st.getCount() + "× ").append(st.getName()));
        }
        if (r.price() <= 0 && r.wants().isEmpty()) {
            lines.add(Text.literal("  nothing — it's free").formatted(Formatting.GRAY));
        }
        lines.add(Text.literal(mine ? (r.target().isEmpty() ? "Open — anyone can accept" : "Reserved for " + r.target())
                : "From " + r.from()).formatted(Formatting.DARK_GRAY));
        ctx.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        return true;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            int pageCount = handler.prop(TradeOffersScreenHandler.P_TOTAL_PAGES);
            if (pageCount > 1) {
                if (over(mx, my, x + 190, y + 22, 13, 11)) { NotchWidgets.tick(); clickButton(0); return true; }
                if (over(mx, my, x + 227, y + 22, 13, 11)) { NotchWidgets.tick(); clickButton(1); return true; }
            }
            int bxIn = x + ROW_X + ROW_W - 54;
            for (int i = 0; i < TradeOffersScreenHandler.INCOMING; i++) {
                Row r = row(handler.incomingStack(i));
                if (r != null && over(mx, my, bxIn, inY(i) + 2, 50, 16)) {
                    NotchWidgets.click();
                    NotchPacketsClient.sendTradeOfferAction(r.id(), ACTION_ACCEPT);
                    return true;
                }
            }
            for (int i = 0; i < TradeOffersScreenHandler.OUTGOING; i++) {
                Row r = row(handler.outgoingStack(i));
                if (r != null && over(mx, my, bxIn, mineY(i) + 2, 50, 16)) {
                    NotchWidgets.click();
                    NotchPacketsClient.sendTradeOfferAction(r.id(), ACTION_CANCEL);
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
