package net.fugginbeenus.notchcurrency.client.ui;

public final class NotchTheme {

    private NotchTheme() {}

    // ---- Panel / chrome (the beveled gray scheme) ----
    public static final int PANEL_FACE   = 0xFFD0D0D0; // main light fill
    public static final int PANEL_MID    = 0xFF9D9D9D; // secondary fill / borders
    public static final int SLOT_FILL    = 0xFF8B8B8B; // slot interior
    public static final int HIGHLIGHT    = 0xFFFFFFFF; // top-left bevel
    public static final int EDGE         = 0xFF474747; // bottom-right bevel / outer edge
    public static final int INSET_SHADOW = 0xFF686868; // slot inset shadow
    public static final int DEEP         = 0xFF2C2C2C; // deep inset (e.g. value boxes)
    public static final int OUTLINE      = 0xFF000000; // hard outline

    // ---- Accents ----
    public static final int ACCENT_GREEN = 0xFF61A04F; // good / confirm
    public static final int GREEN_HI     = 0xFF8FD07A; // green button highlight
    public static final int GREEN_LO     = 0xFF3C6E2F; // green button shadow
    public static final int ACCENT_RED   = 0xFFB23030; // bad / cancel / destructive
    public static final int RED_HI       = 0xFFD86060; // red button highlight
    public static final int RED_LO       = 0xFF7A1818; // red button shadow
    public static final int ACCENT_GOLD  = 0xFFE0A526; // jackpot / prize accent
    public static final int GOLD_HI      = 0xFFFFD56B; // gold button highlight
    public static final int GOLD_LO      = 0xFFAA7410; // gold button shadow
    public static final int ACCENT_TAN   = 0xFFBEB49B; // label banner

    // ---- Component (matches the values the original screens already use) ----
    public static final int TEXT_DARK    = 0xFF404040; // labels on the light panel
    public static final int TEXT_LIGHT   = 0xFFFFFFFF;
    public static final int TEXT_MUTED   = 0xFF686868;
    public static final int TEXT_GOLD    = 0xFFFFD700; // prices
    public static final int TEXT_GREEN   = 0xFF55FF55; // in stock
    public static final int TEXT_RED     = 0xFFFF5555; // out of stock / error
}
