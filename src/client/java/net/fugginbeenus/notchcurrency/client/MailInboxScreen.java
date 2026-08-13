package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.mail.MailInboxMenu;
import net.fugginbeenus.notchcurrency.mail.MailLayout;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import java.util.List;
import java.util.UUID;

/**
 * The Inbox tab: what is waiting, as slots to drag out of.
 *
 * <p>Same size and same inventory position as the Outbox, so switching tabs moves nothing under the
 * player's cursor.
 *
 * <p>Two things do not fit in a slot. Coins, because an auction payout has no item to put anywhere,
 * so they get a line and a button of their own. And who a parcel came from, which shows in the left
 * column as the cursor passes over it.
 */
public class MailInboxScreen extends AbstractContainerScreen<MailInboxMenu> {

    /** What the slots cannot say for themselves, in slot order. */
    public record Label(String sender, String note) {}

    private static final int W = MailLayout.W, H = MailLayout.H;
    private static final int SIDE_X = MailLayout.SIDE_X, SIDE_W = MailLayout.SIDE_W;
    private static final UUID JUST_THE_COINS = new UUID(0L, 0L);

    // Handed over by the packet that opens the tab: a menu screen is built by Minecraft, so there is
    // nowhere to pass our own arguments through.
    private static long coinsWaiting;
    private static int parcelsWaiting;
    private static List<Label> labels = List.of();

    public MailInboxScreen(MailInboxMenu handler, Inventory inv, Component title) {
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

    public static void setSummary(long coins, int parcels, List<Label> slotLabels) {
        coinsWaiting = coins;
        parcelsWaiting = parcels;
        labels = slotLabels;
    }

    private int collectX() { return this.leftPos + SIDE_X + 4; }
    private int collectY() { return this.topPos + 86; }

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = this.leftPos, y = this.topPos;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.font, this.title.getString(), x + W / 2, y + 8);
        MailTabs.draw(ctx, this.font, x, y, MailTabs.INBOX, mouseX, mouseY);

        // Left: the money, and whatever the cursor is over.
        ctx.drawString(this.font, "Waiting", x + SIDE_X + 2, y + MailLayout.HEADING_Y,
                NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + SIDE_X, y + 52, SIDE_W, 54, NotchTheme.PANEL_MID);
        if (coinsWaiting > 0L) {
            ctx.drawString(this.font, coinsWaiting + " coins", x + SIDE_X + 6, y + 58,
                    NotchTheme.TEXT_GOLD, false);
            ctx.drawString(this.font, "owed to you", x + SIDE_X + 6, y + 70,
                    NotchTheme.TEXT_MUTED, false);
            NotchWidgets.primaryButton(ctx, this.font, collectX(), collectY(), SIDE_W - 8, 14,
                    "Collect", over(mouseX, mouseY, collectX(), collectY(), SIDE_W - 8, 14));
        } else {
            ctx.drawString(this.font, "No coins owed.", x + SIDE_X + 6, y + 58,
                    NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.inset(ctx, x + SIDE_X, y + 112, SIDE_W, 62, NotchTheme.PANEL_MID);
        drawHovered(ctx, x, y);

        // Right: the parcels themselves.
        ctx.drawString(this.font, "Parcels", x + MailInboxMenu.INBOX_X + 2, y + MailLayout.HEADING_Y,
                NotchTheme.TEXT_DARK, false);
        if (parcelsWaiting > MailInboxMenu.INBOX_SLOTS) {
            ctx.drawString(this.font, MailInboxMenu.INBOX_SLOTS + " of " + parcelsWaiting + " shown",
                    x + MailInboxMenu.INBOX_X + 50, y + MailLayout.HEADING_Y, NotchTheme.TEXT_MUTED, false);
        }
        for (int i = 0; i < MailInboxMenu.INBOX_SLOTS; i++) {
            NotchWidgets.slot(ctx,
                    x + MailInboxMenu.INBOX_X + (i % MailInboxMenu.COLS) * 18 - 1,
                    y + MailInboxMenu.INBOX_Y + (i / MailInboxMenu.COLS) * 18 - 1);
        }
        if (parcelsWaiting == 0) {
            NotchWidgets.centerText(ctx, this.font, "Nothing waiting.",
                    x + MailInboxMenu.INBOX_X + MailInboxMenu.COLS * 9, y + 90,
                    NotchTheme.TEXT_MUTED, false);
        }

        MailPostScreen.drawInventory(ctx, x, y, this.font);
        //? if >=26.1 {
        /*super.extractContents(ctx, mouseX, mouseY, delta);
        *///?}
    }

    /** Who the parcel under the cursor is from, and anything they wrote with it. */
    private void drawHovered(GuiGraphics ctx, int x, int y) {
        // The menu index, not the slot's own index: that one counts within its container, so an
        // inventory slot would collide with a parcel slot and label someone else's boots.
        int index = this.hoveredSlot == null ? -1 : this.menu.slots.indexOf(this.hoveredSlot);
        if (index < 0 || index >= MailInboxMenu.INBOX_SLOTS || index >= labels.size()
                || !this.hoveredSlot.hasItem()) {
            NotchWidgets.centerText(ctx, this.font, "Hover a parcel", x + SIDE_X + SIDE_W / 2,
                    y + 130, NotchTheme.TEXT_MUTED, false);
            NotchWidgets.centerText(ctx, this.font, "to see who sent it.", x + SIDE_X + SIDE_W / 2,
                    y + 142, NotchTheme.TEXT_MUTED, false);
            return;
        }

        Label label = labels.get(index);
        ctx.drawString(this.font, "From", x + SIDE_X + 6, y + 118, NotchTheme.TEXT_MUTED, false);
        ctx.drawString(this.font, fit(label.sender(), SIDE_W - 12), x + SIDE_X + 6, y + 128,
                NotchTheme.TEXT_DARK, false);
        if (label.note().isEmpty()) return;

        int line = y + 142;
        for (String wrapped : wrap(label.note(), SIDE_W - 12, 3)) {
            ctx.drawString(this.font, wrapped, x + SIDE_X + 6, line, NotchTheme.TEXT_DARK, false);
            line += 10;
        }
    }

    /** Plain strings rather than Font.split, whose draw call differs across versions. */
    private List<String> wrap(String text, int room, int maxLines) {
        List<String> lines = new java.util.ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            if (this.font.width(candidate) <= room) {
                line = new StringBuilder(candidate);
                continue;
            }
            if (line.length() > 0) lines.add(line.toString());
            if (lines.size() == maxLines) return lines;
            // A single word too long to fit is cut rather than left to run off the panel.
            line = new StringBuilder(this.font.width(word) <= room
                    ? word : this.font.plainSubstrByWidth(word, room));
        }
        if (line.length() > 0 && lines.size() < maxLines) lines.add(line.toString());
        return lines;
    }

    private String fit(String text, int room) {
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
            if (MailTabs.click(this.leftPos, this.topPos, MailTabs.INBOX, mx, my)) return true;
            if (coinsWaiting > 0L && over(mx, my, collectX(), collectY(), SIDE_W - 8, 14)) {
                NotchWidgets.click();
                NotchPacketsClient.sendMailTake(JUST_THE_COINS);
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private static boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
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
        //? if >=26.1 {
        /*extractTooltip(ctx, mouseX, mouseY);
        *///?} else {
        renderTooltip(ctx, mouseX, mouseY);
        //?}
    }

    @Override
    public void removed() {
        super.removed();
        MailTabs.screenClosed();
    }
}
