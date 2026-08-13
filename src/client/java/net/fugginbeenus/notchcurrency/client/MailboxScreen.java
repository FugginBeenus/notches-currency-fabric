package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

/**
 * What is waiting in a mailbox.
 *
 * <p>A list rather than a grid of slots, because an entry here is not only an item: an auction sale
 * pays coins, and every entry carries who sent it and why. None of that fits in a slot, and a player
 * looking at five parcels wants to know which is which before taking any of them.
 */
public class MailboxScreen extends Screen {

    /**
     * One waiting entry, as the server described it.
     *
     * <p>The item arrives as an id and a count rather than a stack: the wire shape of a stack has
     * changed twice across the versions this mod builds for, and an icon and a name is all that is
     * drawn here.
     */
    public record Entry(UUID id, String sender, String note, String itemId, int count,
                        String itemName, long coins) {

        public boolean hasItem() {
            return !itemId.isEmpty() && count > 0;
        }
    }

    private static final UUID TAKE_EVERYTHING = new UUID(0L, 0L);

    private static final int W = 300, H = 216;
    private static final int LIST_X = 10, LIST_Y = 40, ROW_H = 26, VISIBLE = 5;
    private static final int TAKE_W = 44, TAKE_H = 16;

    private final String boxOwner;
    private final List<Entry> entries;

    private int px, py;
    private int scroll;

    public MailboxScreen(String boxOwner, List<Entry> entries) {
        super(Component.literal("Mailbox"));
        this.boxOwner = boxOwner == null ? "" : boxOwner;
        this.entries = entries;
    }

    /** Kept so a refresh after taking something can hold on to the title. */
    public String owner() {
        return boxOwner;
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
        int maxScroll = Math.max(0, entries.size() - VISIBLE);
        if (scroll > maxScroll) scroll = maxScroll;
    }

    private int rowY(int i) {
        return py + LIST_Y + i * ROW_H;
    }

    private int takeX() {
        return px + W - LIST_X - TAKE_W;
    }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    //?}
        //? if >=1.21 {
        /*renderTransparentBackground(ctx);
        *///?} else {
        this.renderBackground(ctx);
        //?}
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.font, boxOwner.isEmpty() ? "Mailbox" : boxOwner + "'s Mailbox",
                px + W / 2, py + 8);

        if (entries.isEmpty()) {
            NotchWidgets.centerText(ctx, this.font, "Nothing waiting.",
                    px + W / 2, py + H / 2 - 12, NotchTheme.TEXT_MUTED, false);
            NotchWidgets.centerText(ctx, this.font, "Sales, winnings and parcels all arrive here.",
                    px + W / 2, py + H / 2, NotchTheme.TEXT_MUTED, false);
        } else {
            NotchWidgets.centerText(ctx, this.font,
                    entries.size() + (entries.size() == 1 ? " item waiting" : " items waiting"),
                    px + W / 2, py + 24, NotchTheme.TEXT_MUTED, false);
        }

        for (int i = 0; i < VISIBLE && i + scroll < entries.size(); i++) {
            drawRow(ctx, entries.get(i + scroll), rowY(i), mouseX, mouseY);
        }

        if (entries.size() > VISIBLE) {
            NotchWidgets.centerText(ctx, this.font,
                    (scroll + 1) + "-" + Math.min(entries.size(), scroll + VISIBLE) + " of " + entries.size(),
                    px + W / 2, py + LIST_Y + VISIBLE * ROW_H + 4, NotchTheme.TEXT_MUTED, false);
        }

        boolean anything = !entries.isEmpty();
        if (anything) {
            NotchWidgets.primaryButton(ctx, this.font, px + LIST_X, py + H - 26, 120, 16, "Take all",
                    over(mouseX, mouseY, px + LIST_X, py + H - 26, 120, 16));
        }
        NotchWidgets.neutralButton(ctx, this.font, px + W - LIST_X - 90, py + H - 26, 90, 16, "Close",
                over(mouseX, mouseY, px + W - LIST_X - 90, py + H - 26, 90, 16));

        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
    }

    private void drawRow(GuiGraphics ctx, Entry e, int y, int mouseX, int mouseY) {
        NotchWidgets.inset(ctx, px + LIST_X, y, W - LIST_X * 2, ROW_H - 3, NotchTheme.DEEP);

        int textX = px + LIST_X + 6;
        if (e.hasItem()) {
            ItemStack icon = iconFor(e);
            if (!icon.isEmpty()) {
                ctx.renderItem(icon, px + LIST_X + 4, y + 4);
                textX = px + LIST_X + 25;
            }
        }

        // What it is: the item and how many, or the amount of money.
        String what = e.hasItem()
                ? (e.count() > 1 ? e.itemName() + " x" + e.count() : e.itemName())
                : e.coins() + " coins";
        if (e.hasItem() && e.coins() > 0L) what = what + " + " + e.coins() + " coins";
        ctx.drawString(this.font, fit(what, TAKE_W + 12), textX, y + 3, NotchTheme.TEXT_LIGHT, false);

        String from = e.note().isEmpty() ? "from " + e.sender() : e.sender() + ": " + e.note();
        ctx.drawString(this.font, fit(from, TAKE_W + 12), textX, y + 13, NotchTheme.TEXT_MUTED, false);

        NotchWidgets.primaryButton(ctx, this.font, takeX(), y + 3, TAKE_W, TAKE_H, "Take",
                over(mouseX, mouseY, takeX(), y + 3, TAKE_W, TAKE_H));
    }

    /** The icon, rebuilt from the id. An item this client does not know simply gets no picture. */
    private ItemStack iconFor(Entry e) {
        try {
            var id = net.fugginbeenus.notchcurrency.compat.Reg.parse(e.itemId());
            var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
            return item == null ? ItemStack.EMPTY : new ItemStack(item, Math.max(1, e.count()));
        } catch (Exception ignored) {
            return ItemStack.EMPTY;
        }
    }

    private String fit(String text, int reserved) {
        int room = W - LIST_X * 2 - 32 - reserved;
        return this.font.width(text) <= room ? text : this.font.plainSubstrByWidth(text, room - 6) + "..";
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
            for (int i = 0; i < VISIBLE && i + scroll < entries.size(); i++) {
                if (over(mx, my, takeX(), rowY(i) + 3, TAKE_W, TAKE_H)) {
                    NotchWidgets.click();
                    NotchPacketsClient.sendMailTake(entries.get(i + scroll).id());
                    return true;
                }
            }
            if (!entries.isEmpty() && over(mx, my, px + LIST_X, py + H - 26, 120, 16)) {
                NotchWidgets.click();
                NotchPacketsClient.sendMailTake(TAKE_EVERYTHING);
                return true;
            }
            if (over(mx, my, px + W - LIST_X - 90, py + H - 26, 90, 16)) {
                NotchWidgets.click();
                this.onClose();
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
        int maxScroll = Math.max(0, entries.size() - VISIBLE);
        scroll = Math.max(0, Math.min(maxScroll, scroll - (int) Math.signum(amount)));
        return true;
    }

    private static boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean isPauseScreen() { return false; }

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

    //? if >=1.21 {
    /*@Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        // Drawn manually at the top of render(). This screen paints its panel after the darkening,
        // but the 1.21 base render would darken over the finished panel (super.render comes last here).
    }
    *///?}
}
