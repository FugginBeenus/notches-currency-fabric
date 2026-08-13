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
 *
 * <p>The window is vanilla chest width. It was half again as wide when the Inbox needed a column
 * explaining who sent what; parcels carry that on their own tooltips now, so the column went and
 * the window came in with it.
 */
public final class MailLayout {

    private MailLayout() {}

    public static final int W = 176, H = 222;

    /** The tab strip, and the first row of content under it. */
    public static final int TAB_X = 8, TAB_W = 78, TAB_GAP = 4, TAB_Y = 18, TAB_H = 16;
    public static final int CONTENT_Y = 38;
    public static final int CONTENT_BOTTOM = 126;

    /** The Inbox grid, which lines up with the inventory below it. */
    public static final int SLOTS_X = 8, SLOTS_Y = 42;

    /** The player's inventory. Identical on both tabs, on purpose. */
    public static final int INV_LABEL_Y = 130;
    public static final int INV_X = 8, INV_Y = 140, HOTBAR_Y = 198;
}
