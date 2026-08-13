package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.UUID;

/**
 * The Inbox and Outbox headers, drawn the same on both screens so they read as one window.
 *
 * <p>They are two menus behind the scenes. One holds a view of the mail, the other holds a parcel
 * being made up, and slots cannot be shuffled between tabs without fighting every version's screen
 * code. Clicking the other tab asks the server to open the other menu, which is a screen swap the
 * player reads as a tab change.
 */
public final class MailTabs {

    private MailTabs() {}

    public static final int INBOX = 0, OUTBOX = 1;

    private static final int TAB_W = net.fugginbeenus.notchcurrency.mail.MailLayout.TAB_W;
    private static final int TAB_H = net.fugginbeenus.notchcurrency.mail.MailLayout.TAB_H;
    private static final int TAB_Y = net.fugginbeenus.notchcurrency.mail.MailLayout.TAB_Y;
    private static final int TAB_X = net.fugginbeenus.notchcurrency.mail.MailLayout.TAB_X;
    private static final int TAB_GAP = net.fugginbeenus.notchcurrency.mail.MailLayout.TAB_GAP;

    /**
     * Who the Outbox should be aimed at, when the mailbox was somebody else's.
     *
     * <p>It rides along with the tab switch and comes back on the packet, so tabbing to the Inbox
     * and back does not lose the recipient. It is dropped when the window is closed for real, so a
     * later parcel is never quietly aimed at whoever was last on screen.
     */
    private static UUID aim;
    private static boolean switchingTabs;

    public static void aimAt(UUID recipient) {
        aim = recipient;
    }

    /** Called by both tabs when their screen goes away, to tell a swap from a close. */
    public static void screenClosed() {
        if (switchingTabs) switchingTabs = false;
        else aim = null;
    }

    public static UUID aim() {
        return aim;
    }

    public static void draw(GuiGraphics ctx, Font font, int px, int py, int active, int mouseX, int mouseY) {
        drawOne(ctx, font, px + TAB_X, py + TAB_Y, "Inbox", active == INBOX, mouseX, mouseY);
        drawOne(ctx, font, px + TAB_X + TAB_W + TAB_GAP, py + TAB_Y, "Outbox", active == OUTBOX, mouseX, mouseY);
    }

    private static void drawOne(GuiGraphics ctx, Font font, int x, int y, String label,
                                boolean active, int mouseX, int mouseY) {
        boolean hover = !active && over(mouseX, mouseY, x, y, TAB_W, TAB_H);
        if (active) {
            NotchWidgets.primaryButton(ctx, font, x, y, TAB_W, TAB_H, label, false);
        } else {
            NotchWidgets.neutralButton(ctx, font, x, y, TAB_W, TAB_H, label, hover);
        }
    }

    /** @return true when a tab was clicked, and the swap has been asked for. */
    public static boolean click(int px, int py, int active, int mx, int my) {
        if (active != INBOX && over(mx, my, px + TAB_X, py + TAB_Y, TAB_W, TAB_H)) {
            NotchWidgets.click();
            switchingTabs = true;
            NotchPacketsClient.sendMailTab(INBOX, aim);
            return true;
        }
        if (active != OUTBOX
                && over(mx, my, px + TAB_X + TAB_W + TAB_GAP, py + TAB_Y, TAB_W, TAB_H)) {
            NotchWidgets.click();
            switchingTabs = true;
            NotchPacketsClient.sendMailTab(OUTBOX, aim);
            return true;
        }
        return false;
    }

    private static boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
