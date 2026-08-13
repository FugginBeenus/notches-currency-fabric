package net.fugginbeenus.notchcurrency.mail;

/**
 * Where things sit in the mailbox window.
 *
 * <p>The Inbox and Outbox are two menus pretending to be one window, so anything they share lives
 * here rather than being written twice. The player's own inventory in particular: if it moved by a
 * pixel between tabs, every tab click would shift the world under their cursor.
 *
 * <p>Kept on the server side of the mod because the menus need the slot positions too, and a slot
 * whose menu and screen disagree is a slot that draws in one place and clicks in another.
 */
public final class MailLayout {

    private MailLayout() {}

    /** The window itself. Both tabs are the same size, so the panel never jumps. */
    public static final int W = 296, H = 232;

    /** The tab strip, and the first row of content under it. */
    public static final int TAB_Y = 22, TAB_H = 16;
    public static final int HEADING_Y = 42;

    /** The left column: the recipient list on one tab, what is waiting on the other. */
    public static final int SIDE_X = 8, SIDE_W = 100;

    /** The right column, which both tabs fill with slots. */
    public static final int MAIN_X = 118;
    public static final int SLOTS_Y = 62;

    /** The player's inventory. Identical on both tabs, on purpose. */
    public static final int INV_LABEL_Y = 142;
    public static final int INV_X = MAIN_X, INV_Y = 152, HOTBAR_Y = 210;
}
