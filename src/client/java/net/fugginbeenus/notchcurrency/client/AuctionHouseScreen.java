package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.compat.NetClient;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fugginbeenus.notchcurrency.auction.AuctionHouseScreenHandler;
import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
//? if <1.21 {
import net.minecraft.client.item.TooltipContext;
//?}
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class AuctionHouseScreen extends HandledScreen<AuctionHouseScreenHandler> {

    private static final Identifier TEX =
            NotchCurrency.id("textures/gui/auction/main_browser.png");

    // popup texture (small user listings window drawn on top)
    private static final Identifier USER_POPUP_TEX =
            NotchCurrency.id("textures/gui/auction/userauctions.png");

    // Texture size
    private static final int TEX_W = 256;
    private static final int TEX_H = 256;

    // === MAIN AUCTION GUI LAYOUT ===

    // "My Listings" bar
    private static final int MY_X = 8;
    private static final int MY_Y = 7;
    private static final int MY_W = 80;
    private static final int MY_H = 17;

    // Top-right buttons (question mark + reload)
    private static final int HELP_X = 153;   // ? icon
    private static final int HELP_Y = 7;
    private static final int HELP_W = 16;
    private static final int HELP_H = 17;

    private static final int RELOAD_X = 135; // circular arrow
    private static final int RELOAD_Y = 7;
    private static final int RELOAD_W = 16;
    private static final int RELOAD_H = 17;

    // Pagination bar + arrows
    private static final int PREV_X = 9;     // left arrow
    private static final int PREV_Y = 102;
    private static final int PREV_W = 41;
    private static final int PREV_H = 17;

    // prev | page-box | next are all PREV_W wide and share edges as one bar
    private static final int NEXT_X = PREV_X + PREV_W * 2; // 91
    private static final int NEXT_Y = 102;
    private static final int NEXT_W = 41;
    private static final int NEXT_H = 17;

    // Star (filters)
    private static final int STAR_X = 135;
    private static final int STAR_Y = 102;
    private static final int STAR_W = 16;
    private static final int STAR_H = 17;

    // Clock + line (sort-by-type)
    private static final int CLOCK_X = 153;
    private static final int CLOCK_Y = 102;
    private static final int CLOCK_W = 16;
    private static final int CLOCK_H = 17;

    // New top-bar buttons: same 16×17 footprint as reload/help, evenly spaced (18px pitch)
    // so list / raffle / reload / help read as one consistent row of icon buttons.
    private static final int LIST_X = 99;    // list-an-item (+) button
    private static final int LIST_Y = 7;
    private static final int LIST_W = 16;
    private static final int LIST_H = 17;

    private static final int RAFFLE_X = 117; // raffle (ticket) button
    private static final int RAFFLE_Y = 7;
    private static final int RAFFLE_W = 16;
    private static final int RAFFLE_H = 17;

    // Page text ("1/8") inside the black bar - centered in the bar
    // Black box is at x44-x97, y103-y117 in texture
    private static final int PAGE_TEXT_X = 70;  // Center of black box ((44+97)/2 = 70)
    private static final int PAGE_TEXT_Y = 107; // Vertically centered in black box

    // === USER LISTINGS POPUP LAYOUT ===

    // size of popup window (top-left portion of userauctions.png)
    private static final int POPUP_W = 176;
    private static final int POPUP_H = 96;

    // where the popup sits relative to the main AH texture
    private static final int POPUP_LOCAL_X = 40;
    private static final int POPUP_LOCAL_Y = 24 + 15;

    // player name inside popup
    private static final int POPUP_NAME_X = 12;
    private static final int POPUP_NAME_Y = 10;

    // red X close button inside popup
    private static final int POPUP_CLOSE_X = 83;
    private static final int POPUP_CLOSE_Y = 5;
    private static final int POPUP_CLOSE_W = 14;
    private static final int POPUP_CLOSE_H = 15;

    // popup slot layout (2x5 grid)
    private static final int POPUP_SLOT_LOCAL_X = 9;
    private static final int POPUP_SLOT_LOCAL_Y = 22;
    private static final int POPUP_SLOT_SIZE = 18;
    private static final int POPUP_SLOT_SPACING = 18;

    // Dimming overlay over the big GUI
    private static final int POPUP_DIM_COLOR = 0x66000000;

    private static final int DIM_MARGIN_LEFT = 1;
    private static final int DIM_MARGIN_TOP = 1;
    private static final int DIM_MARGIN_RIGHT = 79;
    private static final int DIM_MARGIN_BOTTOM = 47;

    // debug flags
    private static final boolean DEBUG_MAIN = false;
    private static final boolean DEBUG_POPUP_BUTTON = false;
    private static final boolean DEBUG_POPUP_SLOTS = false;

    // Tooltip option labels
    private static final String[] SORT_OPTIONS = new String[]{
            "Most recent",
            "Ending soon",
            "Highest price",
            "Lowest price",
            "Item name"
    };

    private static final String[] FILTER_OPTIONS = new String[]{
            "All",
            "Blocks",
            "Furniture",
            "Mobs",
            "Gear",
            "Seasonal",
            "Valuables",
            "Books",
            "Other"
    };

    private void debugOutline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    // Buttons
    private ButtonWidget prevButton;
    private ButtonWidget nextButton;
    private ButtonWidget myListingsButton;
    private ButtonWidget filterStarButton;
    private ButtonWidget sortTypeButton;
    private ButtonWidget helpButton;
    private ButtonWidget reloadButton;
    private ButtonWidget listItemButton;
    private ButtonWidget raffleButton;

    // popup state (overlay only)
    private boolean showUserPopup = false;

    // bid prompt overlay (opened by clicking a timed auction; hides the raw listing id)
    private static final int BID_X = 19, BID_Y = 62, BID_W = 140, BID_H = 86;
    private boolean showBidPopup = false;
    private UUID bidListingId = null;
    private String bidItemName = "";
    private long bidStartPrice = 0L;
    private long bidHighest = 0L;
    private String bidInput = "";

    public AuctionHouseScreen(AuctionHouseScreenHandler handler,
                              PlayerInventory inv,
                              Text title) {
        super(handler, inv, title);
        this.backgroundWidth = TEX_W;
        this.backgroundHeight = TEX_H;

        this.titleX = -1000;
        this.playerInventoryTitleX = -1000;
    }

    @Override
    protected void init() {
        super.init();

        int xNudge = 40;
        int yNudge = 10;
        this.x = (this.width - this.backgroundWidth) / 2 + xNudge;
        this.y = (this.height - this.backgroundHeight) / 2 + yNudge;

        this.clearChildren();
        showUserPopup = false;

        // "My Listings" opens overlay
        myListingsButton = ButtonWidget.builder(Text.empty(), b -> {
                    showUserPopup = true;
                })
                .dimensions(this.x + MY_X, this.y + MY_Y, MY_W, MY_H)
                .build();
        myListingsButton.setAlpha(0.0f);
        addDrawableChild(myListingsButton);

        // Top-right buttons
        helpButton = ButtonWidget.builder(Text.empty(), b -> {
            if (this.client != null && this.client.player != null) {
                // Create clickable link message
                Text linkText = Text.literal("Click here for the Notch Currency Guide")
                        .styled(style -> style
                                .withColor(Formatting.GOLD)
                                .withUnderline(true)
                                .withClickEvent(new net.minecraft.text.ClickEvent(
                                        net.minecraft.text.ClickEvent.Action.OPEN_URL,
                                        "https://github.com/FugginBeenus/NotchCurrency/wiki"
                                ))
                                .withHoverEvent(new net.minecraft.text.HoverEvent(
                                        net.minecraft.text.HoverEvent.Action.SHOW_TEXT,
                                        Text.literal("Open the Notch Currency wiki")
                                ))
                        );

                Text message = Text.literal("Need help? ").formatted(Formatting.YELLOW).append(linkText);
                this.client.player.sendMessage(message, false);
            }
        }).dimensions(this.x + HELP_X, this.y + HELP_Y, HELP_W, HELP_H).build();
        helpButton.setAlpha(0.0f);
        addDrawableChild(helpButton);

        reloadButton = ButtonWidget.builder(Text.empty(), b -> {
                    if (this.client != null && this.client.interactionManager != null) {
                        this.client.interactionManager.clickButton(this.handler.syncId, 4); // reload
                    }
                })
                .dimensions(this.x + RELOAD_X, this.y + RELOAD_Y, RELOAD_W, RELOAD_H)
                .build();
        reloadButton.setAlpha(0.0f);
        addDrawableChild(reloadButton);

        // Pagination arrows
        prevButton = ButtonWidget.builder(Text.empty(), b -> {
                    if (this.client != null && this.client.interactionManager != null) {
                        this.client.interactionManager.clickButton(this.handler.syncId, 0); // prev
                    }
                })
                .dimensions(this.x + PREV_X, this.y + PREV_Y, PREV_W, PREV_H)
                .build();
        prevButton.setAlpha(0.0f);
        addDrawableChild(prevButton);

        nextButton = ButtonWidget.builder(Text.empty(), b -> {
                    if (this.client != null && this.client.interactionManager != null) {
                        this.client.interactionManager.clickButton(this.handler.syncId, 1); // next
                    }
                })
                .dimensions(this.x + NEXT_X, this.y + NEXT_Y, NEXT_W, NEXT_H)
                .build();
        nextButton.setAlpha(0.0f);
        addDrawableChild(nextButton);

        // Star (filters)
        filterStarButton = ButtonWidget.builder(Text.empty(), b -> {
                    if (this.client != null && this.client.interactionManager != null) {
                        this.client.interactionManager.clickButton(this.handler.syncId, 2); // filter
                    }
                })
                .dimensions(this.x + STAR_X, this.y + STAR_Y, STAR_W, STAR_H)
                .build();
        filterStarButton.setAlpha(0.0f);
        addDrawableChild(filterStarButton);

        // Clock (sort)
        sortTypeButton = ButtonWidget.builder(Text.empty(), b -> {
                    if (this.client != null && this.client.interactionManager != null) {
                        this.client.interactionManager.clickButton(this.handler.syncId, 3); // sort
                    }
                })
                .dimensions(this.x + CLOCK_X, this.y + CLOCK_Y, CLOCK_W, CLOCK_H)
                .build();
        sortTypeButton.setAlpha(0.0f);
        addDrawableChild(sortTypeButton);

        // List-an-item: open the listing screen (server replaces this screen with it).
        listItemButton = ButtonWidget.builder(Text.empty(), b -> {
                    if (this.client != null && this.client.interactionManager != null) {
                        this.client.interactionManager.clickButton(this.handler.syncId, 6); // list item
                    }
                })
                .dimensions(this.x + LIST_X, this.y + LIST_Y, LIST_W, LIST_H)
                .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(
                        Text.literal("List an item for auction")))
                .build();
        listItemButton.setAlpha(0.0f);
        addDrawableChild(listItemButton);

        // Raffle: the server opens the raffle screen, which replaces this one.
        raffleButton = ButtonWidget.builder(Text.empty(), b -> {
                    if (this.client != null && this.client.interactionManager != null) {
                        this.client.interactionManager.clickButton(this.handler.syncId, 5); // raffle
                    }
                })
                .dimensions(this.x + RAFFLE_X, this.y + RAFFLE_Y, RAFFLE_W, RAFFLE_H)
                .tooltip(net.minecraft.client.gui.tooltip.Tooltip.of(
                        Text.literal("Open the raffle")))
                .build();
        raffleButton.setAlpha(0.0f);
        addDrawableChild(raffleButton);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x, y = this.y;

        // Window (measured 178x210 from the original texture).
        NotchWidgets.panel(ctx, x, y, 178, 210);

        // "MY LISTINGS" green bar.
        greenBar(ctx, x + MY_X, y + MY_Y, MY_W, MY_H,
                over(mouseX, mouseY, x + MY_X, y + MY_Y, MY_W, MY_H));

        // List-item (+) and Raffle (ticket) buttons in the free top-bar space.
        iconButton(ctx, x + LIST_X, y + LIST_Y, LIST_W, LIST_H,
                over(mouseX, mouseY, x + LIST_X, y + LIST_Y, LIST_W, LIST_H), ICON_PLUS);
        iconButton(ctx, x + RAFFLE_X, y + RAFFLE_Y, RAFFLE_W, RAFFLE_H,
                over(mouseX, mouseY, x + RAFFLE_X, y + RAFFLE_Y, RAFFLE_W, RAFFLE_H), ICON_TICKET);

        // Reload + Help (code-drawn).
        iconButton(ctx, x + RELOAD_X, y + RELOAD_Y, RELOAD_W, RELOAD_H,
                over(mouseX, mouseY, x + RELOAD_X, y + RELOAD_Y, RELOAD_W, RELOAD_H), ICON_RELOAD);
        helpButton(ctx, x + HELP_X, y + HELP_Y, HELP_W, HELP_H,
                over(mouseX, mouseY, x + HELP_X, y + HELP_Y, HELP_W, HELP_H));

        // Listing grid (9 x 4).
        for (int row = 0; row < AuctionHouseScreenHandler.LISTING_ROWS; row++) {
            for (int col = 0; col < AuctionHouseScreenHandler.LISTING_COLUMNS; col++) {
                NotchWidgets.slot(ctx, x + 9 + col * 18 - 1, y + 28 + row * 18 - 1);
            }
        }

        // Toolbar: prev / next arrows, page box, star (filter), clock (sort).
        wideArrow(ctx, x + PREV_X, y + PREV_Y, PREV_W, PREV_H,
                over(mouseX, mouseY, x + PREV_X, y + PREV_Y, PREV_W, PREV_H), true);
        wideArrow(ctx, x + NEXT_X, y + NEXT_Y, NEXT_W, NEXT_H,
                over(mouseX, mouseY, x + NEXT_X, y + NEXT_Y, NEXT_W, NEXT_H), false);
        // Flat dark page box, same width/height as the arrows, between them.
        ctx.fill(x + PREV_X + PREV_W, y + PREV_Y, x + NEXT_X, y + PREV_Y + PREV_H, NotchTheme.DEEP);
        iconButton(ctx, x + STAR_X, y + STAR_Y, STAR_W, STAR_H,
                over(mouseX, mouseY, x + STAR_X, y + STAR_Y, STAR_W, STAR_H), ICON_STAR);
        iconButton(ctx, x + CLOCK_X, y + CLOCK_Y, CLOCK_W, CLOCK_H,
                over(mouseX, mouseY, x + CLOCK_X, y + CLOCK_Y, CLOCK_W, CLOCK_H), ICON_CLOCK);

        // Player inventory + hotbar.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                NotchWidgets.slot(ctx, x + 9 + col * 18 - 1, y + 127 + row * 18 - 1);
            }
        }
        for (int col = 0; col < 9; col++) {
            NotchWidgets.slot(ctx, x + 9 + col * 18 - 1, y + 185 - 1);
        }
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    /** Which popup listing slot (0..POPUP_SIZE-1) is under the cursor, or -1. */
    private int hoveredPopupSlot(double mouseX, double mouseY) {
        int slotsX = this.x + POPUP_LOCAL_X + POPUP_SLOT_LOCAL_X;
        int slotsY = this.y + POPUP_LOCAL_Y + POPUP_SLOT_LOCAL_Y;
        for (int row = 0; row < AuctionHouseScreenHandler.POPUP_ROWS; row++) {
            for (int col = 0; col < AuctionHouseScreenHandler.POPUP_COLUMNS; col++) {
                int sx = slotsX + col * POPUP_SLOT_SPACING;
                int sy = slotsY + row * POPUP_SLOT_SPACING;
                if (mouseX >= sx && mouseX < sx + POPUP_SLOT_SIZE
                        && mouseY >= sy && mouseY < sy + POPUP_SLOT_SIZE) {
                    return row * AuctionHouseScreenHandler.POPUP_COLUMNS + col;
                }
            }
        }
        return -1;
    }

    private void greenBar(DrawContext ctx, int bx, int by, int w, int h, boolean hov) {
        NotchWidgets.colorButton(ctx, bx, by, w, h, NotchTheme.ACCENT_GREEN, 0xFF8FD07A, 0xFF3C6E2F, hov);
        String s = "MY LISTINGS";
        // white text + black drop-shadow for readability
        ctx.drawText(this.textRenderer, s, bx + (w - this.textRenderer.getWidth(s)) / 2,
                by + (h - 8) / 2, 0xFFFFFFFF, true);
    }

    private void helpButton(DrawContext ctx, int bx, int by, int w, int h, boolean hov) {
        NotchWidgets.button(ctx, bx, by, w, h, hov, false);
        String s = "?";
        ctx.drawText(this.textRenderer, s, bx + (w - this.textRenderer.getWidth(s)) / 2,
                by + (h - 8) / 2, NotchTheme.TEXT_DARK, false);
    }

    private void wideArrow(DrawContext ctx, int bx, int by, int w, int h, boolean hov, boolean left) {
        // Round only the outer corners so prev | page-box | next read as one bar.
        NotchWidgets.buttonSel(ctx, bx, by, w, h, hov, left, !left, left, !left);
        int cx = bx + w / 2, cy = by + h / 2;
        for (int i = 0; i <= 4; i++) {
            int half = 4 - i;
            if (left) ctx.fill(cx + 2 - i, cy - half, cx + 3 - i, cy + half + 1, NotchTheme.TEXT_DARK);
            else      ctx.fill(cx - 2 + i, cy - half, cx - 1 + i, cy + half + 1, NotchTheme.TEXT_DARK);
        }
    }

    private void redX(DrawContext ctx, int bx, int by, int w, int h) {
        ctx.fill(bx, by, bx + w, by + h, NotchTheme.OUTLINE);
        ctx.fill(bx + 1, by + 1, bx + w - 1, by + h - 1, 0xFFB23030);
        ctx.fill(bx + 1, by + 1, bx + w - 1, by + 2, 0xFFD86060);
        ctx.fill(bx + 1, by + 1, bx + 2, by + h - 1, 0xFFD86060);
        ctx.fill(bx + 1, by + h - 2, bx + w - 1, by + h - 1, 0xFF7A1818);
        ctx.fill(bx + w - 2, by + 1, bx + w - 1, by + h - 1, 0xFF7A1818);
        int cx = bx + w / 2, cy = by + h / 2;
        for (int i = -2; i <= 2; i++) {
            ctx.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, NotchTheme.TEXT_LIGHT);
            ctx.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, NotchTheme.TEXT_LIGHT);
        }
    }

    private void iconButton(DrawContext ctx, int bx, int by, int w, int h, boolean hov, String[] icon) {
        NotchWidgets.button(ctx, bx, by, w, h, hov, false);
        drawIcon(ctx, bx, by, w, h, icon, NotchTheme.TEXT_DARK);
    }

    /** Centers a 1-bit-per-char glyph bitmap (rows of '#') inside the button. */
    private void drawIcon(DrawContext ctx, int bx, int by, int bw, int bh, String[] rows, int color) {
        int iw = rows[0].length(), ih = rows.length;
        int ox = bx + (bw - iw) / 2;
        int oy = by + (bh - ih) / 2;
        for (int r = 0; r < ih; r++) {
            String row = rows[r];
            for (int c = 0; c < iw; c++) {
                if (row.charAt(c) == '#') {
                    ctx.fill(ox + c, oy + r, ox + c + 1, oy + r + 1, color);
                }
            }
        }
    }

    private static final String[] ICON_STAR = {
            "....#....",
            "...###...",
            "#########",
            ".#######.",
            "..#####..",
            ".##.#.##.",
            ".#.....#.",
    };
    private static final String[] ICON_CLOCK = {
            "..#####..",
            ".#.....#.",
            "#...#...#",
            "#...#...#",
            "#...####.",
            "#.......#",
            ".#.....#.",
            "..#####..",
    };
    private static final String[] ICON_RELOAD = {
            "...###.#.",
            "..#...###",
            ".#.....#.",
            "#........",
            "#........",
            ".#.....#.",
            "..#####..",
    };
    private static final String[] ICON_PLUS = {
            "...#...",
            "...#...",
            "...#...",
            "#######",
            "...#...",
            "...#...",
            "...#...",
    };
    private static final String[] ICON_TICKET = {
            "#########",
            "#.......#",
            "#.#####.#",
            "#.......#",
            "#########",
    };

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        int page = handler.getPage() + 1;
        int total = Math.max(1, handler.getTotalPages());
        String txt = page + "/" + total;

        // drawForeground coordinates are relative to the GUI, not screen
        ctx.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal(txt),
                PAGE_TEXT_X,
                PAGE_TEXT_Y,
                0xFFFFFF
        );
    }

    private void drawListTooltip(DrawContext ctx,
                                 int mouseX, int mouseY,
                                 Text title,
                                 String[] options,
                                 String activeLabel) {
        // Push Z-level to render above inventory items
        var matrices = ctx.getMatrices();
        matrices.push();
        matrices.translate(0.0F, 0.0F, 400.0F);

        int maxWidth = this.textRenderer.getWidth(title);

        for (String opt : options) {
            int w = this.textRenderer.getWidth("• " + opt);
            if (w > maxWidth) {
                maxWidth = w;
            }
        }

        int lineHeight = this.textRenderer.fontHeight + 2;
        int lines = 1 + options.length; // title + options
        int boxWidth = maxWidth + 8;
        int boxHeight = lines * lineHeight + 8;

        int x = mouseX + 8;
        int y = mouseY + 8;

        if (x + boxWidth > this.width) {
            x = this.width - boxWidth - 4;
        }
        if (y + boxHeight > this.height) {
            y = this.height - boxHeight - 4;
        }

        ctx.fill(x, y, x + boxWidth, y + boxHeight, 0xF0101010);

        int textX = x + 4;
        int textY = y + 4;

        ctx.drawTextWithShadow(this.textRenderer, title, textX, textY, 0xFFFFFF);
        textY += lineHeight;

        ctx.fill(x + 2, textY - 2, x + boxWidth - 2, textY - 1, 0xFF404040);

        for (String opt : options) {
            boolean isActive = opt.equals(activeLabel);
            int color = isActive ? 0xFFFFD37F : 0xFFE0E0E0;

            ctx.drawTextWithShadow(
                    this.textRenderer,
                    Text.literal("• " + opt),
                    textX,
                    textY,
                    color
            );
            textY += lineHeight;
        }

        matrices.pop();
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext ctx, int mouseX, int mouseY) {
        // Popup: show a cancel hint over the player's own listings; block other tooltips beneath.
        if (showUserPopup) {
            if (hoveredPopupSlot(mouseX, mouseY) >= 0) return; // tooltip drawn in render(), above the popup
            int px = this.x + POPUP_LOCAL_X;
            int py = this.y + POPUP_LOCAL_Y;
            if (mouseX >= px && mouseX < px + POPUP_W &&
                    mouseY >= py && mouseY < py + POPUP_H) {
                return;
            }
        }

        // Sort button tooltip
        int sortX = this.x + CLOCK_X;
        int sortY = this.y + CLOCK_Y;
        if (mouseX >= sortX && mouseX < sortX + CLOCK_W &&
                mouseY >= sortY && mouseY < sortY + CLOCK_H) {
            drawListTooltip(
                    ctx,
                    mouseX,
                    mouseY,
                    Text.literal("Sort by"),
                    SORT_OPTIONS,
                    handler.getSortLabel()
            );
            return;
        }

        // Filter button tooltip
        int filterX = this.x + STAR_X;
        int filterY = this.y + STAR_Y;
        if (mouseX >= filterX && mouseX < filterX + STAR_W &&
                mouseY >= filterY && mouseY < filterY + STAR_H) {
            drawListTooltip(
                    ctx,
                    mouseX,
                    mouseY,
                    Text.literal("Show items"),
                    FILTER_OPTIONS,
                    handler.getFilterLabel()
            );
            return;
        }

        // Custom tooltip for AH listing slots (top grid)
        if (this.focusedSlot != null
                && this.focusedSlot.hasStack()
                && this.focusedSlot.getIndex() < AuctionHouseScreenHandler.LISTING_SIZE) {

            ItemStack stack = this.focusedSlot.getStack();
            NbtCompound tag = StackData.getData(stack);

            if (tag.contains("nc_price", NbtElement.LONG_TYPE)
                    && tag.contains("nc_seller", NbtElement.STRING_TYPE)) {

                long startPrice = tag.getLong("nc_price");
                String seller = tag.getString("nc_seller");

                long created = tag.contains("nc_created", NbtElement.LONG_TYPE)
                        ? tag.getLong("nc_created")
                        : 0L;
                long expires = tag.contains("nc_expires", NbtElement.LONG_TYPE)
                        ? tag.getLong("nc_expires")
                        : 0L;

                long highestBid = tag.contains("nc_highest_bid", NbtElement.LONG_TYPE)
                        ? tag.getLong("nc_highest_bid")
                        : 0L;
                String highestBidder = tag.contains("nc_highest_bidder", NbtElement.STRING_TYPE)
                        ? tag.getString("nc_highest_bidder")
                        : null;

                // Timed auction if expires > 0
                boolean timedAuction = expires > 0L;

                // Compute status + time left for timed auctions only
                Text statusLine = null;
                Text timeLine = null;
                long now = 0L;

                if (timedAuction && this.client != null && this.client.world != null) {
                    now = this.client.world.getTime();  // match server's tick clock
                    long remaining = expires - now;
                    if (remaining <= 0L) {
                        statusLine = Text.literal("Status: Expired").formatted(Formatting.RED);
                        timeLine = Text.literal("Time left: 0m").formatted(Formatting.DARK_GRAY);
                    } else {
                        statusLine = Text.literal("Status: Active").formatted(Formatting.GREEN);

                        long seconds = remaining / 20L;
                        long days = seconds / 86400L;
                        seconds %= 86400L;
                        long hours = seconds / 3600L;
                        seconds %= 3600L;
                        long minutes = seconds / 60L;

                        StringBuilder sb = new StringBuilder();
                        if (days > 0) sb.append(days).append("d ");
                        if (hours > 0 || days > 0) sb.append(hours).append("h ");
                        sb.append(minutes).append("m");

                        timeLine = Text.literal("Time left: " + sb).formatted(Formatting.DARK_GRAY);
                    }
                }

                // Rarity coloring (boost Notch coin to "Legendary")
                Formatting rarityColor;
                String rarityName;

                if (stack.isOf(ModItems.NOTCH_COIN)) {
                    rarityColor = Formatting.GOLD;
                    rarityName = "Legendary";
                } else {
                    switch (stack.getRarity()) {
                        case UNCOMMON:
                            rarityColor = Formatting.GREEN;
                            rarityName = "Uncommon";
                            break;
                        case RARE:
                            rarityColor = Formatting.BLUE;
                            rarityName = "Rare";
                            break;
                        case EPIC:
                            rarityColor = Formatting.LIGHT_PURPLE;
                            rarityName = "Epic";
                            break;
                        case COMMON:
                        default:
                            rarityColor = Formatting.GRAY;
                            rarityName = "Common";
                            break;
                    }
                }

                // Price to display: for auctions, show current bid or start; for buy-now, fixed price
                long displayPrice = (timedAuction && highestBid > 0L) ? highestBid : startPrice;

                List<Text> lines = new ArrayList<>();

                // Check if shift is held for detailed item view
                boolean shiftHeld = Screen.hasShiftDown();

                if (shiftHeld) {
                    // === SHIFT HELD: Show item details with compact auction banner ===

                    // Compact auction banner at top
                    MutableText bannerLine = Text.literal("⚡ ").formatted(Formatting.YELLOW)
                            .append(Text.literal(String.valueOf(displayPrice) + " ").formatted(Formatting.GOLD))
                            .append(Text.literal("\uE000"))
                            .append(Text.literal(" from ").formatted(Formatting.GRAY))
                            .append(Text.literal(seller).formatted(Formatting.WHITE))
                            .append(Text.literal(" - ").formatted(Formatting.GRAY));

                    if (timedAuction) {
                        bannerLine.append(Text.literal("Bid").formatted(Formatting.YELLOW));
                    } else {
                        bannerLine.append(Text.literal("Buy").formatted(Formatting.GREEN));
                    }
                    lines.add(bannerLine);

                    // Separator
                    lines.add(Text.literal("─────────────").formatted(Formatting.DARK_GRAY));

                    // Get vanilla item tooltip lines
                    // Create a clean copy WITHOUT auction NBT so AuctionTooltips callback won't modify it
                    ItemStack cleanStack = stack.copy();
                    if (StackData.hasData(cleanStack)) {
                        NbtCompound cleanTag = StackData.editData(cleanStack);
                        // Remove ALL auction-specific tags
                        cleanTag.remove("nc_price");
                        cleanTag.remove("nc_seller");
                        cleanTag.remove("nc_created");
                        cleanTag.remove("nc_expires");
                        cleanTag.remove("nc_highest_bid");
                        cleanTag.remove("nc_highest_bidder");
                        cleanTag.remove("nc_listing_id");

                        //? if <1.21 {
                        // Also remove the display lore that was added server-side
                        if (cleanTag.contains("display", 10)) {
                            NbtCompound display = cleanTag.getCompound("display");
                            display.remove("Lore");
                            if (display.isEmpty()) {
                                cleanTag.remove("display");
                            }
                        }
                        //?}

                        // If tag is now empty, remove it entirely
                        if (cleanTag.isEmpty()) {
                            StackData.clearData(cleanStack);
                        } else {
                            StackData.commitData(cleanStack, cleanTag);
                        }
                    }
                    //? if >=1.21 {
                    /*// The server-side lore rides the LORE component on 1.21.
                    cleanStack.remove(net.minecraft.component.DataComponentTypes.LORE);
                    *///?}

                    //? if >=1.21 {
                    /*List<Text> vanillaLines = cleanStack.getTooltip(
                            net.minecraft.item.Item.TooltipContext.DEFAULT,
                            this.client != null ? this.client.player : null,
                            this.client != null && this.client.options.advancedItemTooltips
                                    ? net.minecraft.item.tooltip.TooltipType.ADVANCED
                                    : net.minecraft.item.tooltip.TooltipType.BASIC
                    );
                    *///?} else {
                    List<Text> vanillaLines = cleanStack.getTooltip(
                            this.client != null ? this.client.player : null,
                            this.client != null && this.client.options.advancedItemTooltips
                                    ? TooltipContext.Default.ADVANCED
                                    : TooltipContext.Default.BASIC
                    );
                    //?}

                    // Add vanilla lines (includes item name, enchantments, etc.)
                    for (Text line : vanillaLines) {
                        lines.add(line);
                    }

                    // Hint to release shift
                    lines.add(Text.literal(""));
                    lines.add(Text.literal("[Release Shift] Auction info").formatted(Formatting.DARK_GRAY));

                } else {
                    // === DEFAULT: Show compact auction tooltip ===

                    // Line 1: rarity colored name
                    Text nameLine = stack.getName().copy().formatted(rarityColor);
                    lines.add(nameLine);

                    // Price line
                    MutableText priceLine = Text.empty()
                            .append(Text.literal("Price: ").formatted(Formatting.GOLD))
                            .append(Text.literal(String.valueOf(displayPrice) + " ").formatted(Formatting.YELLOW))
                            .append(Text.literal("\uE000"));
                    lines.add(priceLine);

                    // For timed auction, also show highest bid explicitly if any
                    if (timedAuction && highestBid > 0L) {
                        MutableText bidLine = Text.empty()
                                .append(Text.literal("Highest bid: ").formatted(Formatting.AQUA))
                                .append(Text.literal(String.valueOf(highestBid)).formatted(Formatting.AQUA));
                        if (highestBidder != null && !highestBidder.isEmpty()) {
                            bidLine.append(Text.literal(" by " + highestBidder).formatted(Formatting.GRAY));
                        }
                        lines.add(bidLine);
                    }

                    // Seller
                    lines.add(
                            Text.literal("Seller: " + seller)
                                    .formatted(Formatting.GRAY)
                    );

                    // Rarity
                    lines.add(
                            Text.literal("Rarity: " + rarityName).formatted(rarityColor)
                    );

                    // Status + time for timed auctions
                    if (timedAuction && statusLine != null) {
                        lines.add(statusLine);
                        if (timeLine != null) {
                            lines.add(timeLine);
                        }
                    }

                    // Hints
                    if (timedAuction) {
                        lines.add(
                                Text.literal("Left-click: open bid in chat")
                                        .formatted(Formatting.YELLOW)
                        );
                        lines.add(
                                Text.literal("Or use /ah bid <id> <amount>")
                                        .formatted(Formatting.DARK_GRAY)
                        );
                    } else {
                        lines.add(
                                Text.literal("Click to buy")
                                        .formatted(Formatting.YELLOW)
                        );
                    }

                    // Hint to hold shift for item details
                    lines.add(Text.literal("[Shift] Item details").formatted(Formatting.DARK_GRAY));
                }

                // Draw tooltip
                ctx.drawTooltip(this.textRenderer, lines, mouseX, mouseY);

                return;
            }
        }

        // Fallback: normal behaviour
        super.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }



    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        //? if <1.21 {
        this.renderBackground(ctx);
        //?}
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);

        if (showUserPopup) {
            drawUserPopup(ctx);
            drawPopupCancelTooltip(ctx, mouseX, mouseY);
        }

        if (showBidPopup) {
            drawBidPopup(ctx, mouseX, mouseY);
        }

        if (DEBUG_MAIN) {
            debugOutline(ctx, helpButton.getX(), helpButton.getY(),
                    helpButton.getWidth(), helpButton.getHeight(), 0xFFFF0000);
            debugOutline(ctx, reloadButton.getX(), reloadButton.getY(),
                    reloadButton.getWidth(), reloadButton.getHeight(), 0xFFFFFF00);
            debugOutline(ctx, myListingsButton.getX(), myListingsButton.getY(),
                    myListingsButton.getWidth(), myListingsButton.getHeight(), 0xFF00FF00);
            debugOutline(ctx, prevButton.getX(), prevButton.getY(),
                    prevButton.getWidth(), prevButton.getHeight(), 0xFF00FFFF);
            debugOutline(ctx, nextButton.getX(), nextButton.getY(),
                    nextButton.getWidth(), nextButton.getHeight(), 0xFF00FFFF);
            debugOutline(ctx, filterStarButton.getX(), filterStarButton.getY(),
                    filterStarButton.getWidth(), filterStarButton.getHeight(), 0xFF8800FF);
            debugOutline(ctx, sortTypeButton.getX(), sortTypeButton.getY(),
                    sortTypeButton.getWidth(), sortTypeButton.getHeight(), 0xFF888888);
        }
    }

    /** Bid prompt drawn above everything (Z 500); manual text input keeps the listing id hidden. */
    private void drawBidPopup(DrawContext ctx, int mouseX, int mouseY) {
        var matrices = ctx.getMatrices();
        matrices.push();
        matrices.translate(0.0F, 0.0F, 500.0F);

        ctx.fill(0, 0, this.width, this.height, 0x88000000);

        int px = this.x + BID_X, py = this.y + BID_Y;
        NotchWidgets.panel(ctx, px, py, BID_W, BID_H);

        NotchWidgets.title(ctx, this.textRenderer, "Place a Bid", px + BID_W / 2, py + 6);
        NotchWidgets.centerText(ctx, this.textRenderer, bidItemName, px + BID_W / 2, py + 18, NotchTheme.TEXT_LIGHT, true);

        long cur = bidHighest > 0 ? bidHighest : bidStartPrice;
        long min = cur + 1;
        ctx.drawText(this.textRenderer, (bidHighest > 0 ? "Current bid: " : "Start price: ") + cur,
                px + 10, py + 32, NotchTheme.TEXT_DARK, false);
        ctx.drawText(this.textRenderer, "Min next bid: " + min, px + 10, py + 42, NotchTheme.TEXT_DARK, false);

        // Input box + manual text/cursor.
        NotchWidgets.inset(ctx, px + 10, py + 52, BID_W - 20, 14, NotchTheme.DEEP);
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        if (bidInput.isEmpty()) {
            ctx.drawText(this.textRenderer, Text.literal("amount").formatted(Formatting.DARK_GRAY),
                    px + 14, py + 55, 0xFF555555, false);
            if (blink) ctx.drawText(this.textRenderer, "_", px + 14, py + 55, 0xFFFFFFFF, false);
        } else {
            ctx.drawText(this.textRenderer, bidInput + (blink ? "_" : ""), px + 14, py + 55, 0xFFFFFFFF, false);
        }

        NotchWidgets.primaryButton(ctx, this.textRenderer, px + 10, py + 68, 58, 14, "Bid",
                over(mouseX, mouseY, px + 10, py + 68, 58, 14));
        NotchWidgets.dangerButton(ctx, this.textRenderer, px + 72, py + 68, 58, 14, "Cancel",
                over(mouseX, mouseY, px + 72, py + 68, 58, 14));

        matrices.pop();
    }

    private void submitBid() {
        long amount;
        try {
            amount = Long.parseLong(bidInput.trim());
        } catch (NumberFormatException e) {
            amount = 0L;
        }
        long min = (bidHighest > 0 ? bidHighest : bidStartPrice) + 1;
        if (amount < min) {
            if (this.client != null && this.client.player != null) {
                this.client.player.sendMessage(Text.literal("Bid must be at least " + min + ".").formatted(Formatting.RED), false);
            }
            return;
        }
        if (bidListingId != null) {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeUuid(bidListingId);
            buf.writeVarLong(amount);
            NetClient.sendToServer(NotchPackets.BID_REQUEST, buf);
        }
        showBidPopup = false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (showBidPopup) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) { showBidPopup = false; return true; }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) { submitBid(); return true; }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
                if (!bidInput.isEmpty()) bidInput = bidInput.substring(0, bidInput.length() - 1);
                return true;
            }
            return true; // consume all keys while bidding
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (showBidPopup) {
            if (Character.isDigit(chr) && bidInput.length() < 12) bidInput += chr;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    /** Cancel hint for a hovered popup listing, drawn above the popup (Z 600). */
    private void drawPopupCancelTooltip(DrawContext ctx, int mouseX, int mouseY) {
        int slot = hoveredPopupSlot(mouseX, mouseY);
        if (slot < 0) return;
        ItemStack stack = handler.getUserPopupInventory().getStack(slot);
        if (stack.isEmpty()) return;

        List<Text> lines = new ArrayList<>();
        lines.add(stack.getName().copy().formatted(Formatting.WHITE));
        NbtCompound tag = StackData.getData(stack);
        if (tag.contains("nc_price", NbtElement.LONG_TYPE)) {
            lines.add(Text.literal("Price: " + tag.getLong("nc_price") + " " + NotchWidgets.coinName()).formatted(Formatting.GOLD));
        }
        lines.add(Text.literal("Click to cancel & reclaim").formatted(Formatting.RED));

        var matrices = ctx.getMatrices();
        matrices.push();
        matrices.translate(0.0F, 0.0F, 600.0F);
        ctx.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        matrices.pop();
    }

    private void drawUserPopup(DrawContext ctx) {
        var matrices = ctx.getMatrices();
        matrices.push();
        matrices.translate(0.0F, 0.0F, 500.0F);

        int dimX1 = this.x + DIM_MARGIN_LEFT;
        int dimY1 = this.y + DIM_MARGIN_TOP;
        int dimX2 = this.x + this.backgroundWidth - DIM_MARGIN_RIGHT;
        int dimY2 = this.y + this.backgroundHeight - DIM_MARGIN_BOTTOM;
        ctx.fill(dimX1, dimY1, dimX2, dimY2, POPUP_DIM_COLOR);

        int px = this.x + POPUP_LOCAL_X;
        int py = this.y + POPUP_LOCAL_Y;

        // Popup window (code-drawn; original was 107x70).
        NotchWidgets.panel(ctx, px, py, 107, 70);

        if (this.client != null && this.client.player != null) {
            String name = this.client.player.getName().getString();
            ctx.drawText(
                    this.textRenderer,
                    name,
                    px + POPUP_NAME_X,
                    py + POPUP_NAME_Y,
                    NotchTheme.TEXT_DARK,
                    false
            );
        }

        // Red close button.
        redX(ctx, px + POPUP_CLOSE_X, py + POPUP_CLOSE_Y, POPUP_CLOSE_W, POPUP_CLOSE_H);

        SimpleInventory inv = handler.getUserPopupInventory();
        int index = 0;
        for (int row = 0; row < AuctionHouseScreenHandler.POPUP_ROWS; row++) {
            for (int col = 0; col < AuctionHouseScreenHandler.POPUP_COLUMNS; col++) {
                int sx = px + POPUP_SLOT_LOCAL_X + col * POPUP_SLOT_SPACING;
                int sy = py + POPUP_SLOT_LOCAL_Y + row * POPUP_SLOT_SPACING;

                NotchWidgets.slot(ctx, sx, sy);

                ItemStack stack = inv.getStack(index++);
                if (!stack.isEmpty()) {
                    ctx.drawItem(stack, sx + 1, sy + 1);
                    ctx.drawItemInSlot(this.textRenderer, stack, sx + 1, sy + 1);
                }

                if (DEBUG_POPUP_SLOTS) {
                    debugOutline(ctx, sx, sy, POPUP_SLOT_SIZE, POPUP_SLOT_SIZE, 0xFFFFFFFF);
                }
            }
        }

        if (DEBUG_POPUP_BUTTON) {
            debugOutline(ctx,
                    px + POPUP_CLOSE_X,
                    py + POPUP_CLOSE_Y,
                    POPUP_CLOSE_W,
                    POPUP_CLOSE_H,
                    0xFFFF0000);
        }

        matrices.pop();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Bid prompt swallows all clicks while open.
        if (showBidPopup) {
            int px = this.x + BID_X, py = this.y + BID_Y;
            if (over((int) mouseX, (int) mouseY, px + 10, py + 68, 58, 14)) { submitBid(); return true; }
            if (over((int) mouseX, (int) mouseY, px + 72, py + 68, 58, 14)) { showBidPopup = false; return true; }
            return true;
        }

        // Popup handling (unchanged)
        if (showUserPopup) {
            int px = this.x + POPUP_LOCAL_X;
            int py = this.y + POPUP_LOCAL_Y;

            // red close button
            int cx1 = px + POPUP_CLOSE_X;
            int cy1 = py + POPUP_CLOSE_Y;
            int cx2 = cx1 + POPUP_CLOSE_W;
            int cy2 = cy1 + POPUP_CLOSE_H;

            if (mouseX >= cx1 && mouseX < cx2 && mouseY >= cy1 && mouseY < cy2) {
                showUserPopup = false;
                return true;
            }

            // Click one of your own listings to cancel it (item is returned to you).
            int slot = hoveredPopupSlot(mouseX, mouseY);
            if (slot >= 0) {
                ItemStack stack = handler.getUserPopupInventory().getStack(slot);
                NbtCompound tag = StackData.getData(stack);
                if (!stack.isEmpty() && tag.containsUuid("nc_listing_id")) {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeUuid(tag.getUuid("nc_listing_id"));
                    NetClient.sendToServer(NotchPackets.AUCTION_CANCEL, buf);
                }
                return true;
            }

            // swallow clicks inside popup
            if (mouseX >= px && mouseX < px + POPUP_W &&
                    mouseY >= py && mouseY < py + POPUP_H) {
                return true;
            }
        }

        // --- New: client-side warning when trying to buy your own UNTIMED listing ---
        if (button == 0 && !showUserPopup && this.client != null && this.client.player != null) {
            Slot clickedSlot = this.focusedSlot;
            if (clickedSlot != null
                    && clickedSlot.hasStack()
                    && clickedSlot.getIndex() < AuctionHouseScreenHandler.LISTING_SIZE
                    && this.isPointWithinBounds(clickedSlot.x, clickedSlot.y, 16, 16, mouseX, mouseY)) {

                ItemStack stack = clickedSlot.getStack();
                NbtCompound tag = StackData.getData(stack);

                if (tag.contains("nc_seller", NbtElement.STRING_TYPE)
                        && tag.contains("nc_expires", NbtElement.LONG_TYPE)) {

                    long expires = tag.getLong("nc_expires");
                    String seller = tag.getString("nc_seller");
                    String selfName = this.client.player.getName().getString();

                    // Only block BUY-NOW listings (expires <= 0), not timed auctions
                    if (expires <= 0L && seller.equals(selfName)) {
                        this.client.player.sendMessage(
                                Text.literal("You can't buy your own listing.")
                                        .formatted(Formatting.RED),
                                false
                        );
                        // Swallow the click; do not send it to the server
                        return true;
                    }
                }
            }
        }

        // Let HandledScreen send click to server (for buy-now behaviour, inventory, etc.)
        boolean handled = super.mouseClicked(mouseX, mouseY, button);

        // Left click on a TIMED auction => open bid command in chat (existing behaviour)
        if (button == 0 && !showUserPopup && this.client != null) {
            // Use focusedSlot instead of getSlotAt (getSlotAt is private in 1.20.1)
            Slot clicked = this.focusedSlot;
            if (clicked != null
                    && clicked.hasStack()
                    && clicked.getIndex() < AuctionHouseScreenHandler.LISTING_SIZE
                    && this.isPointWithinBounds(clicked.x, clicked.y, 16, 16, mouseX, mouseY)) {

                ItemStack stack = clicked.getStack();
                NbtCompound tag = StackData.getData(stack);
                if (tag.contains("nc_expires", NbtElement.LONG_TYPE)
                        && tag.containsUuid("nc_listing_id")) {

                    long expires = tag.getLong("nc_expires");

                    // Timed auction (expires > 0): open the in-GUI bid prompt (no id in chat).
                    if (expires > 0L) {
                        bidListingId = tag.getUuid("nc_listing_id");
                        bidItemName = stack.getName().getString();
                        bidStartPrice = tag.getLong("nc_price");
                        bidHighest = tag.contains("nc_highest_bid", NbtElement.LONG_TYPE)
                                ? tag.getLong("nc_highest_bid") : 0L;
                        bidInput = "";
                        showBidPopup = true;
                        return true;
                    }
                }
            }
        }

        return handled;
    }

    //? if >=1.21 {
    /*@Override
    protected void applyBlur(float delta) {
        // No 1.21 menu blur behind the mod's screens. They draw crisp panels over the world.
    }
    *///?}
}
