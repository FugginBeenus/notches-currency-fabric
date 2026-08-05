package net.fugginbeenus.notchcurrency.client.ui;

import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Code-drawn GUI primitives in the Notch Currency style, built from {@link NotchTheme}.
 * These replace static texture backgrounds so screens are editable in code.
 *
 * The look is the classic Minecraft bevel: a body fill, a light highlight on the
 * top/left edges, a darker shadow on the bottom/right, and a black outline. Slots use the
 * same bevel inverted so they read as "pressed in".
 *
 * All coordinates are screen-space; pass the same x/y your screen handler used for slots.
 */
public final class NotchWidgets {

    private NotchWidgets() {}

    /** A raised panel (window background) with slightly rounded corners. */
    public static void panel(DrawContext ctx, int x, int y, int w, int h) {
        final int r = 1;
        fillRound(ctx, x, y, w, h, r, NotchTheme.OUTLINE);
        fillRound(ctx, x + 1, y + 1, w - 2, h - 2, r - 1, NotchTheme.PANEL_FACE);
        // bevel on the straight edges (skipping the rounded corners)
        ctx.fill(x + r, y + 1, x + w - r, y + 2, NotchTheme.HIGHLIGHT);
        ctx.fill(x + 1, y + r, x + 2, y + h - r, NotchTheme.HIGHLIGHT);
        ctx.fill(x + r, y + h - 2, x + w - r, y + h - 1, NotchTheme.EDGE);
        ctx.fill(x + w - 2, y + r, x + w - 1, y + h - r, NotchTheme.EDGE);
    }

    /** A standard 18x18 inventory slot (item draws on top via the screen handler). */
    public static void slot(DrawContext ctx, int x, int y) {
        slot(ctx, x, y, 18, 18);
    }

    /** An inset slot of arbitrary size (pressed-in bevel). */
    public static void slot(DrawContext ctx, int x, int y, int w, int h) {
        ctx.fill(x, y, x + w, y + h, NotchTheme.INSET_SHADOW);
        // dark top + left (inset)
        ctx.fill(x, y, x + w, y + 1, NotchTheme.EDGE);
        ctx.fill(x, y, x + 1, y + h, NotchTheme.EDGE);
        // light bottom + right
        ctx.fill(x + 1, y + h - 1, x + w, y + h, NotchTheme.HIGHLIGHT);
        ctx.fill(x + w - 1, y + 1, x + w, y + h, NotchTheme.HIGHLIGHT);
        // interior
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, NotchTheme.SLOT_FILL);
    }

    /** A clickable button with rounded corners. Lightens on hover, presses in when held. */
    public static void button(DrawContext ctx, int x, int y, int w, int h, boolean hovered, boolean pressed) {
        buttonSel(ctx, x, y, w, h, hovered || pressed, true, true, true, true);
    }

    /** A rounded button in a custom colour, with its own bevel tints. */
    public static void colorButton(DrawContext ctx, int x, int y, int w, int h,
                                   int base, int light, int dark, boolean hovered) {
        final int r = 1;
        int face = hovered ? lighten(base) : base;
        fillRound(ctx, x, y, w, h, r, NotchTheme.OUTLINE);
        fillRound(ctx, x + 1, y + 1, w - 2, h - 2, r - 1, face);
        ctx.fill(x + r, y + 1, x + w - r, y + 2, light);
        ctx.fill(x + 1, y + r, x + 2, y + h - r, light);
        ctx.fill(x + r, y + h - 2, x + w - r, y + h - 1, dark);
        ctx.fill(x + w - 2, y + r, x + w - 1, y + h - r, dark);
    }

    /** An inset value/field box (for text inputs and read-outs), rounded. */
    public static void inset(DrawContext ctx, int x, int y, int w, int h, int fill) {
        final int r = 1;
        fillRound(ctx, x, y, w, h, r, NotchTheme.OUTLINE);
        fillRound(ctx, x + 1, y + 1, w - 2, h - 2, r - 1, fill);
        // inset bevel: dark top/left, light bottom/right (on the straight edges)
        ctx.fill(x + r, y + 1, x + w - r, y + 2, NotchTheme.EDGE);
        ctx.fill(x + 1, y + r, x + 2, y + h - r, NotchTheme.EDGE);
        ctx.fill(x + r, y + h - 2, x + w - r, y + h - 1, NotchTheme.HIGHLIGHT);
        ctx.fill(x + w - 2, y + r, x + w - 1, y + h - r, NotchTheme.HIGHLIGHT);
    }

    /** A dark read-out box (e.g. the balance display) with slight corner rounding. */
    public static void pill(DrawContext ctx, int x, int y, int w, int h) {
        final int r = 1;
        fillRound(ctx, x, y, w, h, r, NotchTheme.OUTLINE);
        fillRound(ctx, x + 1, y + 1, w - 2, h - 2, r - 1, NotchTheme.DEEP);
        ctx.fill(x + r, y + 1, x + w - r, y + 2, NotchTheme.OUTLINE);
    }

    // ---- Labeled buttons (consistent semantics across screens) ----
    // GOOD = green, BAD = red: white text + black shadow ("hero" text).
    // NEUTRAL = grey: black text, no shadow.

    /** Green confirm/positive button (Buy, List, Save, Bid, …). */
    public static void primaryButton(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h, String label, boolean hovered) {
        colorButton(ctx, x, y, w, h, NotchTheme.ACCENT_GREEN, NotchTheme.GREEN_HI, NotchTheme.GREEN_LO, hovered);
        ctx.drawCenteredTextWithShadow(tr, Text.literal(label), x + w / 2, y + (h - 8) / 2, NotchTheme.TEXT_LIGHT);
    }

    /** Red cancel/destructive button (Cancel, Clear, Reset, …). */
    public static void dangerButton(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h, String label, boolean hovered) {
        colorButton(ctx, x, y, w, h, NotchTheme.ACCENT_RED, NotchTheme.RED_HI, NotchTheme.RED_LO, hovered);
        ctx.drawCenteredTextWithShadow(tr, Text.literal(label), x + w / 2, y + (h - 8) / 2, NotchTheme.TEXT_LIGHT);
    }

    /** Gold prize/jackpot button (Claim). */
    public static void goldButton(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h, String label, boolean hovered) {
        colorButton(ctx, x, y, w, h, NotchTheme.ACCENT_GOLD, NotchTheme.GOLD_HI, NotchTheme.GOLD_LO, hovered);
        ctx.drawCenteredTextWithShadow(tr, Text.literal(label), x + w / 2, y + (h - 8) / 2, NotchTheme.TEXT_LIGHT);
    }

    /** Neutral grey button with black, no-shadow text (Draw, day toggles, …). */
    public static void neutralButton(DrawContext ctx, TextRenderer tr, int x, int y, int w, int h, String label, boolean hovered) {
        button(ctx, x, y, w, h, hovered, false);
        centerText(ctx, tr, label, x + w / 2, y + (h - 8) / 2, NotchTheme.TEXT_DARK, false);
    }

    /** Centered text helper (shadow optional). */
    public static void centerText(DrawContext ctx, TextRenderer tr, String label, int cx, int y, int color, boolean shadow) {
        ctx.drawText(tr, label, cx - tr.getWidth(label) / 2, y, color, shadow);
    }

    /** Hero title text: bold white with a black shadow, centered. */
    public static void title(DrawContext ctx, TextRenderer tr, String label, int cx, int y) {
        ctx.drawCenteredTextWithShadow(tr, Text.literal(label).formatted(net.minecraft.util.Formatting.BOLD), cx, y, NotchTheme.TEXT_LIGHT);
    }

    /** A horizontal divider line. */
    public static void divider(DrawContext ctx, int x, int y, int w) {
        ctx.fill(x, y, x + w, y + 1, NotchTheme.EDGE);
        ctx.fill(x, y + 1, x + w, y + 2, NotchTheme.HIGHLIGHT);
    }

    /** Compact number for stack-count overlays: 999, 1k, 9.9k, 120k, 1.2m. Keeps big coin prices
     *  from spilling across the icon. Show the exact value in the tooltip. */
    public static String compactCount(long n) {
        if (n < 1_000) return Long.toString(n);
        if (n < 1_000_000) return compact(n, 1_000, "k");
        if (n < 1_000_000_000) return compact(n, 1_000_000, "m");
        return compact(n, 1_000_000_000, "b");
    }

    private static String compact(long n, long unit, String suffix) {
        long whole = n / unit;
        long tenth = (n % unit) * 10 / unit;
        return (whole < 10 && tenth > 0) ? whole + "." + tenth + suffix : whole + suffix;
    }

    /**
     * The currency's display word for GUI text ("Bet must be 10-1000 ___"). Resolves the coin
     * item's translated name, so a rename from the currency maker (or any resource pack) carries
     * into every screen automatically; the stock "Notch Coin" reads as plain "coins" in amounts.
     */
    public static String coinName() {
        String resolved = net.minecraft.text.Text.translatable("item.notchcurrency.notch_coin").getString();
        return resolved.equals("Notch Coin") || resolved.equals("item.notchcurrency.notch_coin")
                ? "coins" : resolved;
    }

    /** The vanilla button click, for code-drawn buttons (primary actions: Buy, Take, Save…). */
    public static void click() {
        net.minecraft.client.MinecraftClient.getInstance().getSoundManager().play(
                net.minecraft.client.sound.PositionedSoundInstance.master(
                        net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(), 1.0f));
    }

    /** A lighter, higher tick for small controls (tabs, pagers, steppers, nudges). */
    public static void tick() {
        net.minecraft.client.MinecraftClient.getInstance().getSoundManager().play(
                net.minecraft.client.sound.PositionedSoundInstance.master(
                        net.minecraft.sound.SoundEvents.UI_BUTTON_CLICK.value(), 1.35f, 0.5f));
    }

    /** Classic '&' color/format codes rendered as § formatting ("&6Golden &lShop"). Same codes the
     *  dialogue system supports, so owners learn them once. */
    public static String colorize(String s) {
        return s == null ? "" : s.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
    }

    /** A small right-pointing arrow (13×8), like the vanilla trade arrow. */
    public static void arrowRight(DrawContext ctx, int x, int y, int color) {
        ctx.fill(x, y + 3, x + 10, y + 5, color);
        for (int i = 0; i < 4; i++) {
            ctx.fill(x + 10 + i, y + i, x + 11 + i, y + 8 - i, color);
        }
    }

    /** A crisp 7×4 solid triangle centred at (cx, cy), pointing up or down. Renders reliably where a
     *  ▲/▼ glyph might fall back to a missing box. Used for scrollbar-style paging arrows. */
    public static void triangle(DrawContext ctx, int cx, int cy, boolean up, int color) {
        for (int r = 0; r < 4; r++) {
            int half = up ? r : (3 - r);
            ctx.fill(cx - half, cy + r, cx + half + 1, cy + r + 1, color);
        }
    }

    /** A horizontal slider: inset track + draggable handle. {@code t} is 0..1; the screen owns the
     *  drag math (map mouse x across [x+2, x+w-8]). A center tick marks the midpoint. */
    public static void slider(DrawContext ctx, int x, int y, int w, int h, float t, boolean hovered) {
        inset(ctx, x, y, w, h, NotchTheme.DEEP);
        int cx = x + w / 2;
        ctx.fill(cx, y + 2, cx + 1, y + h - 2, NotchTheme.INSET_SHADOW); // center tick
        float clamped = Math.max(0f, Math.min(1f, t));
        int hx = x + 2 + Math.round((w - 10) * clamped);
        button(ctx, hx, y + 1, 8, h - 2, hovered, false);
    }

    private static void fillRound(DrawContext ctx, int x, int y, int w, int h, int r, int color) {
        fillRoundSel(ctx, x, y, w, h, r, color, true, true, true, true);
    }

    /**
     * Filled rectangle with selectable rounded corners (tl/tr/bl/br). The inset follows a
     * quarter-circle so curves read as round, not pointy; non-rounded corners stay square.
     */
    private static void fillRoundSel(DrawContext ctx, int x, int y, int w, int h, int r, int color,
                                     boolean tl, boolean tr, boolean bl, boolean br) {
        if (r <= 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        ctx.fill(x, y + r, x + w, y + h - r, color);
        for (int i = 0; i < r; i++) {
            int d = r - i;
            int span = (int) Math.round(Math.sqrt((double) r * r - (double) d * d));
            int ins = r - span;
            ctx.fill(x + (tl ? ins : 0), y + i, x + w - (tr ? ins : 0), y + i + 1, color);
            ctx.fill(x + (bl ? ins : 0), y + h - 1 - i, x + w - (br ? ins : 0), y + h - i, color);
        }
    }

    /**
     * A button with per-corner rounding. Anatomy matches the original art: a black
     * outer outline, a complete white inner ring, then the grey face (glyph draws on top).
     */
    public static void buttonSel(DrawContext ctx, int x, int y, int w, int h, boolean hovered,
                                 boolean tl, boolean tr, boolean bl, boolean br) {
        final int r = 1;
        int face = hovered ? lighten(NotchTheme.PANEL_FACE) : NotchTheme.PANEL_FACE;
        fillRoundSel(ctx, x, y, w, h, r, NotchTheme.OUTLINE, tl, tr, bl, br);                 // black
        fillRoundSel(ctx, x + 1, y + 1, w - 2, h - 2, r - 1, NotchTheme.HIGHLIGHT, tl, tr, bl, br); // white ring
        fillRoundSel(ctx, x + 2, y + 2, w - 4, h - 4, r - 2, face, tl, tr, bl, br);            // grey face
    }

    private static int lighten(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = Math.min(255, ((argb >> 16) & 0xFF) + 16);
        int g = Math.min(255, ((argb >> 8) & 0xFF) + 16);
        int b = Math.min(255, (argb & 0xFF) + 16);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * A price as display text using the reskinnable coin glyph for the coin part, e.g.
     * "50 ⛁ + 2×Diamond", or "free" when nothing is required. Shared by the shop + cosmetics screens.
     */
    public static Text priceText(long coinPrice, String barterName, int barterCount) {
        MutableText t = Text.empty();
        boolean any = false;
        if (coinPrice > 0) {
            t.append(NotchCurrency.coins(coinPrice));
            any = true;
        }
        if (barterCount > 0 && barterName != null && !barterName.isEmpty()) {
            if (any) t.append(Text.literal(" + "));
            t.append(Text.literal(barterCount + "×" + barterName));
            any = true;
        }
        return any ? t : Text.literal("free");
    }

    /**
     * Guard against the vanilla HandledScreen text-field bug: when a field is focused, a plain
     * letter/number key isn't "handled" by TextFieldWidget.keyPressed, so HandledScreen falls through
     * and closes the screen (inventory key) or swaps the hotbar (number keys). This mirrors vanilla
     * AnvilScreen: while a field is focused, feed the key to it and SWALLOW everything except ESC, so
     * the screen never closes or hotbar-swaps mid-typing. Call from a screen's keyPressed:
     * {@code if (NotchWidgets.typingInField(k, s, m, fieldA, fieldB)) return true;}
     */
    public static boolean typingInField(int keyCode, int scanCode, int modifiers, TextFieldWidget... fields) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) return false; // let the screen close on ESC
        for (TextFieldWidget f : fields) {
            if (f != null && f.isFocused() && f.isVisible()) {
                // Forward only navigation/editing keys (and real Ctrl/Cmd combos) to the field.
                // Plain letters/numbers are inserted via charTyped, so we must NOT forward them here:
                // forwarding a bare 'a' trips the field's select-all and the next char wipes the line.
                boolean combo = (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0;
                boolean editKey = combo
                        || keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE
                        || keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT
                        || keyCode == GLFW.GLFW_KEY_HOME || keyCode == GLFW.GLFW_KEY_END
                        || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER;
                if (editKey) f.keyPressed(keyCode, scanCode, modifiers);
                return true; // swallow everything else so HandledScreen doesn't close/hotbar-swap
            }
        }
        return false;
    }

    /** The multiline-editor version of {@link #typingInField}: while the box is focused, forward
     *  only navigation/editing keys (plain letters insert via charTyped: forwarding a bare 'a'
     *  trips select-all and the next char wipes the text). UP/DOWN/PGUP/PGDN included for line nav;
     *  ENTER included so newlines still work. */
    public static boolean typingInEditBox(int keyCode, int scanCode, int modifiers,
                                          net.minecraft.client.gui.widget.EditBoxWidget box) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) return false; // let the screen close on ESC
        if (box == null || !box.isFocused() || !box.visible) return false;
        boolean combo = (modifiers & (GLFW.GLFW_MOD_CONTROL | GLFW.GLFW_MOD_SUPER)) != 0;
        boolean editKey = combo
                || keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_DELETE
                || keyCode == GLFW.GLFW_KEY_LEFT || keyCode == GLFW.GLFW_KEY_RIGHT
                || keyCode == GLFW.GLFW_KEY_UP || keyCode == GLFW.GLFW_KEY_DOWN
                || keyCode == GLFW.GLFW_KEY_PAGE_UP || keyCode == GLFW.GLFW_KEY_PAGE_DOWN
                || keyCode == GLFW.GLFW_KEY_HOME || keyCode == GLFW.GLFW_KEY_END
                || keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER;
        if (editKey) box.keyPressed(keyCode, scanCode, modifiers);
        return true; // swallow plain characters: charTyped inserts them
    }
}
