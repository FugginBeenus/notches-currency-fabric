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
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class AuctionHouseScreen extends AbstractContainerScreen<AuctionHouseScreenHandler> {

    private static final ResourceLocation TEX =
            NotchCurrency.id("textures/gui/auction/main_browser.png");

    // popup texture (small user listings window drawn on top)
    private static final ResourceLocation USER_POPUP_TEX =
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

    private void debugOutline(GuiGraphics ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    // Buttons
    private Button prevButton;
    private Button nextButton;
    private Button myListingsButton;
    private Button filterStarButton;
    private Button sortTypeButton;
    private Button helpButton;
    private Button reloadButton;
    private Button listItemButton;
    private Button raffleButton;

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
                              Inventory inv,
                              Component title) {
        super(handler, inv, title);
        this.imageWidth = TEX_W;
        this.imageHeight = TEX_H;

        this.titleLabelX = -1000;
        this.inventoryLabelX = -1000;
    }

    @Override
    protected void init() {
        super.init();

        int xNudge = 40;
        int yNudge = 10;
        this.leftPos = (this.width - this.imageWidth) / 2 + xNudge;
        this.topPos = (this.height - this.imageHeight) / 2 + yNudge;

        this.clearWidgets();
        showUserPopup = false;

        // "My Listings" opens overlay
        myListingsButton = Button.builder(Component.empty(), b -> {
                    showUserPopup = true;
                })
                .bounds(this.leftPos + MY_X, this.topPos + MY_Y, MY_W, MY_H)
                .build();
        myListingsButton.setAlpha(0.0f);
        addRenderableWidget(myListingsButton);

        // Top-right buttons
        helpButton = Button.builder(Component.empty(), b -> {
            if (this.minecraft != null && this.minecraft.player != null) {
                // Create clickable link message
                Component linkText = Component.literal("Click here for the Notch Currency Guide")
                        .withStyle(style -> style
                                .withColor(ChatFormatting.GOLD)
                                .withUnderlined(true)
                                .withClickEvent(net.fugginbeenus.notchcurrency.compat.Chat.openUrl("https://github.com/FugginBeenus/NotchCurrency/wiki"))
                                .withHoverEvent(net.fugginbeenus.notchcurrency.compat.Chat.showText(Component.literal("Open the Notch Currency wiki")))
                        );

                Component message = Component.literal("Need help? ").withStyle(ChatFormatting.YELLOW).append(linkText);
                this.minecraft.player.displayClientMessage(message, false);
            }
        }).bounds(this.leftPos + HELP_X, this.topPos + HELP_Y, HELP_W, HELP_H).build();
        helpButton.setAlpha(0.0f);
        addRenderableWidget(helpButton);

        reloadButton = Button.builder(Component.empty(), b -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 4); // reload
                    }
                })
                .bounds(this.leftPos + RELOAD_X, this.topPos + RELOAD_Y, RELOAD_W, RELOAD_H)
                .build();
        reloadButton.setAlpha(0.0f);
        addRenderableWidget(reloadButton);

        // Pagination arrows
        prevButton = Button.builder(Component.empty(), b -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0); // prev
                    }
                })
                .bounds(this.leftPos + PREV_X, this.topPos + PREV_Y, PREV_W, PREV_H)
                .build();
        prevButton.setAlpha(0.0f);
        addRenderableWidget(prevButton);

        nextButton = Button.builder(Component.empty(), b -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1); // next
                    }
                })
                .bounds(this.leftPos + NEXT_X, this.topPos + NEXT_Y, NEXT_W, NEXT_H)
                .build();
        nextButton.setAlpha(0.0f);
        addRenderableWidget(nextButton);

        // Star (filters)
        filterStarButton = Button.builder(Component.empty(), b -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 2); // filter
                    }
                })
                .bounds(this.leftPos + STAR_X, this.topPos + STAR_Y, STAR_W, STAR_H)
                .build();
        filterStarButton.setAlpha(0.0f);
        addRenderableWidget(filterStarButton);

        // Clock (sort)
        sortTypeButton = Button.builder(Component.empty(), b -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 3); // sort
                    }
                })
                .bounds(this.leftPos + CLOCK_X, this.topPos + CLOCK_Y, CLOCK_W, CLOCK_H)
                .build();
        sortTypeButton.setAlpha(0.0f);
        addRenderableWidget(sortTypeButton);

        // List-an-item: open the listing screen (server replaces this screen with it).
        listItemButton = Button.builder(Component.empty(), b -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 6); // list item
                    }
                })
                .bounds(this.leftPos + LIST_X, this.topPos + LIST_Y, LIST_W, LIST_H)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal("List an item for auction")))
                .build();
        listItemButton.setAlpha(0.0f);
        addRenderableWidget(listItemButton);

        // Raffle: the server opens the raffle screen, which replaces this one.
        raffleButton = Button.builder(Component.empty(), b -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 5); // raffle
                    }
                })
                .bounds(this.leftPos + RAFFLE_X, this.topPos + RAFFLE_Y, RAFFLE_W, RAFFLE_H)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal("Open the raffle")))
                .build();
        raffleButton.setAlpha(0.0f);
        addRenderableWidget(raffleButton);
    }

    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
        final int x = this.leftPos, y = this.topPos;

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
        int slotsX = this.leftPos + POPUP_LOCAL_X + POPUP_SLOT_LOCAL_X;
        int slotsY = this.topPos + POPUP_LOCAL_Y + POPUP_SLOT_LOCAL_Y;
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

    private void greenBar(GuiGraphics ctx, int bx, int by, int w, int h, boolean hov) {
        NotchWidgets.colorButton(ctx, bx, by, w, h, NotchTheme.ACCENT_GREEN, 0xFF8FD07A, 0xFF3C6E2F, hov);
        String s = "MY LISTINGS";
        // white text + black drop-shadow for readability
        ctx.drawString(this.font, s, bx + (w - this.font.width(s)) / 2,
                by + (h - 8) / 2, 0xFFFFFFFF, true);
    }

    private void helpButton(GuiGraphics ctx, int bx, int by, int w, int h, boolean hov) {
        NotchWidgets.button(ctx, bx, by, w, h, hov, false);
        String s = "?";
        ctx.drawString(this.font, s, bx + (w - this.font.width(s)) / 2,
                by + (h - 8) / 2, NotchTheme.TEXT_DARK, false);
    }

    private void wideArrow(GuiGraphics ctx, int bx, int by, int w, int h, boolean hov, boolean left) {
        // Round only the outer corners so prev | page-box | next read as one bar.
        NotchWidgets.buttonSel(ctx, bx, by, w, h, hov, left, !left, left, !left);
        int cx = bx + w / 2, cy = by + h / 2;
        for (int i = 0; i <= 4; i++) {
            int half = 4 - i;
            if (left) ctx.fill(cx + 2 - i, cy - half, cx + 3 - i, cy + half + 1, NotchTheme.TEXT_DARK);
            else      ctx.fill(cx - 2 + i, cy - half, cx - 1 + i, cy + half + 1, NotchTheme.TEXT_DARK);
        }
    }

    private void redX(GuiGraphics ctx, int bx, int by, int w, int h) {
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

    private void iconButton(GuiGraphics ctx, int bx, int by, int w, int h, boolean hov, String[] icon) {
        NotchWidgets.button(ctx, bx, by, w, h, hov, false);
        drawIcon(ctx, bx, by, w, h, icon, NotchTheme.TEXT_DARK);
    }

    private void drawIcon(GuiGraphics ctx, int bx, int by, int bw, int bh, String[] rows, int color) {
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
    protected void renderLabels(GuiGraphics ctx, int mouseX, int mouseY) {
        int page = menu.getPage() + 1;
        int total = Math.max(1, menu.getTotalPages());
        String txt = page + "/" + total;

        // drawForeground coordinates are relative to the GUI, not screen
        ctx.drawCenteredString(
                this.font,
                Component.literal(txt),
                PAGE_TEXT_X,
                PAGE_TEXT_Y,
                0xFFFFFF
        );
    }

    private void drawListTooltip(GuiGraphics ctx,
                                 int mouseX, int mouseY,
                                 Component title,
                                 String[] options,
                                 String activeLabel) {
        // Push Z-level to render above inventory items
        var matrices = ctx.pose();
        matrices.pushPose();
        matrices.translate(0.0F, 0.0F, 400.0F);

        int maxWidth = this.font.width(title);

        for (String opt : options) {
            int w = this.font.width("• " + opt);
            if (w > maxWidth) {
                maxWidth = w;
            }
        }

        int lineHeight = this.font.lineHeight + 2;
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

        ctx.drawString(this.font, title, textX, textY, 0xFFFFFF);
        textY += lineHeight;

        ctx.fill(x + 2, textY - 2, x + boxWidth - 2, textY - 1, 0xFF404040);

        for (String opt : options) {
            boolean isActive = opt.equals(activeLabel);
            int color = isActive ? 0xFFFFD37F : 0xFFE0E0E0;

            ctx.drawString(
                    this.font,
                    Component.literal("• " + opt),
                    textX,
                    textY,
                    color
            );
            textY += lineHeight;
        }

        matrices.popPose();
    }

    @Override
    protected void renderTooltip(GuiGraphics ctx, int mouseX, int mouseY) {
        // Popup: show a cancel hint over the player's own listings; block other tooltips beneath.
        if (showUserPopup) {
            if (hoveredPopupSlot(mouseX, mouseY) >= 0) return; // tooltip drawn in render(), above the popup
            int px = this.leftPos + POPUP_LOCAL_X;
            int py = this.topPos + POPUP_LOCAL_Y;
            if (mouseX >= px && mouseX < px + POPUP_W &&
                    mouseY >= py && mouseY < py + POPUP_H) {
                return;
            }
        }

        // Sort button tooltip
        int sortX = this.leftPos + CLOCK_X;
        int sortY = this.topPos + CLOCK_Y;
        if (mouseX >= sortX && mouseX < sortX + CLOCK_W &&
                mouseY >= sortY && mouseY < sortY + CLOCK_H) {
            drawListTooltip(
                    ctx,
                    mouseX,
                    mouseY,
                    Component.literal("Sort by"),
                    SORT_OPTIONS,
                    menu.getSortLabel()
            );
            return;
        }

        // Filter button tooltip
        int filterX = this.leftPos + STAR_X;
        int filterY = this.topPos + STAR_Y;
        if (mouseX >= filterX && mouseX < filterX + STAR_W &&
                mouseY >= filterY && mouseY < filterY + STAR_H) {
            drawListTooltip(
                    ctx,
                    mouseX,
                    mouseY,
                    Component.literal("Show items"),
                    FILTER_OPTIONS,
                    menu.getFilterLabel()
            );
            return;
        }

        // Custom tooltip for AH listing slots (top grid)
        if (this.hoveredSlot != null
                && this.hoveredSlot.hasItem()
                && this.hoveredSlot.getContainerSlot() < AuctionHouseScreenHandler.LISTING_SIZE) {

            ItemStack stack = this.hoveredSlot.getItem();
            CompoundTag tag = StackData.getData(stack);

            if (tag.contains("nc_price", Tag.TAG_LONG)
                    && tag.contains("nc_seller", Tag.TAG_STRING)) {

                long startPrice = tag.getLong("nc_price");
                String seller = tag.getString("nc_seller");

                long created = tag.contains("nc_created", Tag.TAG_LONG)
                        ? tag.getLong("nc_created")
                        : 0L;
                long expires = tag.contains("nc_expires", Tag.TAG_LONG)
                        ? tag.getLong("nc_expires")
                        : 0L;

                long highestBid = tag.contains("nc_highest_bid", Tag.TAG_LONG)
                        ? tag.getLong("nc_highest_bid")
                        : 0L;
                String highestBidder = tag.contains("nc_highest_bidder", Tag.TAG_STRING)
                        ? tag.getString("nc_highest_bidder")
                        : null;

                // Timed auction if expires > 0
                boolean timedAuction = expires > 0L;

                // Compute status + time left for timed auctions only
                Component statusLine = null;
                Component timeLine = null;
                long now = 0L;

                if (timedAuction && this.minecraft != null && this.minecraft.level != null) {
                    now = this.minecraft.level.getGameTime();  // match server's tick clock
                    long remaining = expires - now;
                    if (remaining <= 0L) {
                        statusLine = Component.literal("Status: Expired").withStyle(ChatFormatting.RED);
                        timeLine = Component.literal("Time left: 0m").withStyle(ChatFormatting.DARK_GRAY);
                    } else {
                        statusLine = Component.literal("Status: Active").withStyle(ChatFormatting.GREEN);

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

                        timeLine = Component.literal("Time left: " + sb).withStyle(ChatFormatting.DARK_GRAY);
                    }
                }

                // Rarity coloring (boost Notch coin to "Legendary")
                ChatFormatting rarityColor;
                String rarityName;

                if (stack.is(ModItems.NOTCH_COIN)) {
                    rarityColor = ChatFormatting.GOLD;
                    rarityName = "Legendary";
                } else {
                    switch (stack.getRarity()) {
                        case UNCOMMON:
                            rarityColor = ChatFormatting.GREEN;
                            rarityName = "Uncommon";
                            break;
                        case RARE:
                            rarityColor = ChatFormatting.BLUE;
                            rarityName = "Rare";
                            break;
                        case EPIC:
                            rarityColor = ChatFormatting.LIGHT_PURPLE;
                            rarityName = "Epic";
                            break;
                        case COMMON:
                        default:
                            rarityColor = ChatFormatting.GRAY;
                            rarityName = "Common";
                            break;
                    }
                }

                // Price to display: for auctions, show current bid or start; for buy-now, fixed price
                long displayPrice = (timedAuction && highestBid > 0L) ? highestBid : startPrice;

                List<Component> lines = new ArrayList<>();

                // Check if shift is held for detailed item view
                boolean shiftHeld = Screen.hasShiftDown();

                if (shiftHeld) {
                    // === SHIFT HELD: Show item details with compact auction banner ===

                    // Compact auction banner at top
                    MutableComponent bannerLine = Component.literal("⚡ ").withStyle(ChatFormatting.YELLOW)
                            .append(Component.literal(String.valueOf(displayPrice) + " ").withStyle(ChatFormatting.GOLD))
                            .append(Component.literal("\uE000"))
                            .append(Component.literal(" from ").withStyle(ChatFormatting.GRAY))
                            .append(Component.literal(seller).withStyle(ChatFormatting.WHITE))
                            .append(Component.literal(" - ").withStyle(ChatFormatting.GRAY));

                    if (timedAuction) {
                        bannerLine.append(Component.literal("Bid").withStyle(ChatFormatting.YELLOW));
                    } else {
                        bannerLine.append(Component.literal("Buy").withStyle(ChatFormatting.GREEN));
                    }
                    lines.add(bannerLine);

                    // Separator
                    lines.add(Component.literal("─────────────").withStyle(ChatFormatting.DARK_GRAY));

                    // Get vanilla item tooltip lines
                    // Create a clean copy WITHOUT auction NBT so AuctionTooltips callback won't modify it
                    ItemStack cleanStack = stack.copy();
                    if (StackData.hasData(cleanStack)) {
                        CompoundTag cleanTag = StackData.editData(cleanStack);
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
                            CompoundTag display = cleanTag.getCompound("display");
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
                    cleanStack.remove(net.minecraft.core.component.DataComponents.LORE);
                    *///?}

                    //? if >=1.21 {
                    /*List<Component> vanillaLines = cleanStack.getTooltipLines(
                            net.minecraft.world.item.Item.TooltipContext.EMPTY,
                            this.minecraft != null ? this.minecraft.player : null,
                            this.minecraft != null && this.minecraft.options.advancedItemTooltips
                                    ? net.minecraft.world.item.TooltipFlag.ADVANCED
                                    : net.minecraft.world.item.TooltipFlag.NORMAL
                    );
                    *///?} else {
                    List<Component> vanillaLines = cleanStack.getTooltipLines(
                            this.minecraft != null ? this.minecraft.player : null,
                            this.minecraft != null && this.minecraft.options.advancedItemTooltips
                                    ? TooltipFlag.Default.ADVANCED
                                    : TooltipFlag.Default.NORMAL
                    );
                    //?}

                    // Add vanilla lines (includes item name, enchantments, etc.)
                    for (Component line : vanillaLines) {
                        lines.add(line);
                    }

                    // Hint to release shift
                    lines.add(Component.literal(""));
                    lines.add(Component.literal("[Release Shift] Auction info").withStyle(ChatFormatting.DARK_GRAY));

                } else {
                    // === DEFAULT: Show compact auction tooltip ===

                    // Line 1: rarity colored name
                    Component nameLine = stack.getHoverName().copy().withStyle(rarityColor);
                    lines.add(nameLine);

                    // Price line
                    MutableComponent priceLine = Component.empty()
                            .append(Component.literal("Price: ").withStyle(ChatFormatting.GOLD))
                            .append(Component.literal(String.valueOf(displayPrice) + " ").withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal("\uE000"));
                    lines.add(priceLine);

                    // For timed auction, also show highest bid explicitly if any
                    if (timedAuction && highestBid > 0L) {
                        MutableComponent bidLine = Component.empty()
                                .append(Component.literal("Highest bid: ").withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(String.valueOf(highestBid)).withStyle(ChatFormatting.AQUA));
                        if (highestBidder != null && !highestBidder.isEmpty()) {
                            bidLine.append(Component.literal(" by " + highestBidder).withStyle(ChatFormatting.GRAY));
                        }
                        lines.add(bidLine);
                    }

                    // Seller
                    lines.add(
                            Component.literal("Seller: " + seller)
                                    .withStyle(ChatFormatting.GRAY)
                    );

                    // Rarity
                    lines.add(
                            Component.literal("Rarity: " + rarityName).withStyle(rarityColor)
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
                                Component.literal("Left-click: open bid in chat")
                                        .withStyle(ChatFormatting.YELLOW)
                        );
                        lines.add(
                                Component.literal("Or use /ah bid <id> <amount>")
                                        .withStyle(ChatFormatting.DARK_GRAY)
                        );
                    } else {
                        lines.add(
                                Component.literal("Click to buy")
                                        .withStyle(ChatFormatting.YELLOW)
                        );
                    }

                    // Hint to hold shift for item details
                    lines.add(Component.literal("[Shift] Item details").withStyle(ChatFormatting.DARK_GRAY));
                }

                // Draw tooltip
                ctx.renderComponentTooltip(this.font, lines, mouseX, mouseY);

                return;
            }
        }

        // Fallback: normal behaviour
        super.renderTooltip(ctx, mouseX, mouseY);
    }



    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        //? if <1.21 {
        this.renderBackground(ctx);
        //?}
        super.render(ctx, mouseX, mouseY, delta);
        this.renderTooltip(ctx, mouseX, mouseY);

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
    private void drawBidPopup(GuiGraphics ctx, int mouseX, int mouseY) {
        var matrices = ctx.pose();
        matrices.pushPose();
        matrices.translate(0.0F, 0.0F, 500.0F);

        ctx.fill(0, 0, this.width, this.height, 0x88000000);

        int px = this.leftPos + BID_X, py = this.topPos + BID_Y;
        NotchWidgets.panel(ctx, px, py, BID_W, BID_H);

        NotchWidgets.title(ctx, this.font, "Place a Bid", px + BID_W / 2, py + 6);
        NotchWidgets.centerText(ctx, this.font, bidItemName, px + BID_W / 2, py + 18, NotchTheme.TEXT_LIGHT, true);

        long cur = bidHighest > 0 ? bidHighest : bidStartPrice;
        long min = cur + 1;
        ctx.drawString(this.font, (bidHighest > 0 ? "Current bid: " : "Start price: ") + cur,
                px + 10, py + 32, NotchTheme.TEXT_DARK, false);
        ctx.drawString(this.font, "Min next bid: " + min, px + 10, py + 42, NotchTheme.TEXT_DARK, false);

        // Input box + manual text/cursor.
        NotchWidgets.inset(ctx, px + 10, py + 52, BID_W - 20, 14, NotchTheme.DEEP);
        boolean blink = (System.currentTimeMillis() / 500) % 2 == 0;
        if (bidInput.isEmpty()) {
            ctx.drawString(this.font, Component.literal("amount").withStyle(ChatFormatting.DARK_GRAY),
                    px + 14, py + 55, 0xFF555555, false);
            if (blink) ctx.drawString(this.font, "_", px + 14, py + 55, 0xFFFFFFFF, false);
        } else {
            ctx.drawString(this.font, bidInput + (blink ? "_" : ""), px + 14, py + 55, 0xFFFFFFFF, false);
        }

        NotchWidgets.primaryButton(ctx, this.font, px + 10, py + 68, 58, 14, "Bid",
                over(mouseX, mouseY, px + 10, py + 68, 58, 14));
        NotchWidgets.dangerButton(ctx, this.font, px + 72, py + 68, 58, 14, "Cancel",
                over(mouseX, mouseY, px + 72, py + 68, 58, 14));

        matrices.popPose();
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
            if (this.minecraft != null && this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(Component.literal("Bid must be at least " + min + ".").withStyle(ChatFormatting.RED), false);
            }
            return;
        }
        if (bidListingId != null) {
            FriendlyByteBuf buf = PacketByteBufs.create();
            buf.writeUUID(bidListingId);
            buf.writeVarLong(amount);
            NetClient.sendToServer(NotchPackets.BID_REQUEST, buf);
        }
        showBidPopup = false;
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
    *///?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
        if (showBidPopup) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) { showBidPopup = false; return true; }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) { submitBid(); return true; }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
                if (!bidInput.isEmpty()) bidInput = bidInput.substring(0, bidInput.length() - 1);
                return true;
            }
            return true; // consume all keys while bidding
        }
        //? if >=1.21.11 {
        /*return super.keyPressed(event);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        char chr = (char) event.codepoint();
        int modifiers = event.modifiers();
    *///?} else {
    @Override
    public boolean charTyped(char chr, int modifiers) {
    //?}
        if (showBidPopup) {
            if (Character.isDigit(chr) && bidInput.length() < 12) bidInput += chr;
            return true;
        }
        //? if >=1.21.11 {
        /*return super.charTyped(event);
        *///?} else {
        return super.charTyped(chr, modifiers);
        //?}
    }

    /** Cancel hint for a hovered popup listing, drawn above the popup (Z 600). */
    private void drawPopupCancelTooltip(GuiGraphics ctx, int mouseX, int mouseY) {
        int slot = hoveredPopupSlot(mouseX, mouseY);
        if (slot < 0) return;
        ItemStack stack = menu.getUserPopupInventory().getItem(slot);
        if (stack.isEmpty()) return;

        List<Component> lines = new ArrayList<>();
        lines.add(stack.getHoverName().copy().withStyle(ChatFormatting.WHITE));
        CompoundTag tag = StackData.getData(stack);
        if (tag.contains("nc_price", Tag.TAG_LONG)) {
            lines.add(Component.literal("Price: " + tag.getLong("nc_price") + " " + NotchWidgets.coinName()).withStyle(ChatFormatting.GOLD));
        }
        lines.add(Component.literal("Click to cancel & reclaim").withStyle(ChatFormatting.RED));

        var matrices = ctx.pose();
        matrices.pushPose();
        matrices.translate(0.0F, 0.0F, 600.0F);
        ctx.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        matrices.popPose();
    }

    private void drawUserPopup(GuiGraphics ctx) {
        var matrices = ctx.pose();
        matrices.pushPose();
        matrices.translate(0.0F, 0.0F, 500.0F);

        int dimX1 = this.leftPos + DIM_MARGIN_LEFT;
        int dimY1 = this.topPos + DIM_MARGIN_TOP;
        int dimX2 = this.leftPos + this.imageWidth - DIM_MARGIN_RIGHT;
        int dimY2 = this.topPos + this.imageHeight - DIM_MARGIN_BOTTOM;
        ctx.fill(dimX1, dimY1, dimX2, dimY2, POPUP_DIM_COLOR);

        int px = this.leftPos + POPUP_LOCAL_X;
        int py = this.topPos + POPUP_LOCAL_Y;

        // Popup window (code-drawn; original was 107x70).
        NotchWidgets.panel(ctx, px, py, 107, 70);

        if (this.minecraft != null && this.minecraft.player != null) {
            String name = this.minecraft.player.getName().getString();
            ctx.drawString(
                    this.font,
                    name,
                    px + POPUP_NAME_X,
                    py + POPUP_NAME_Y,
                    NotchTheme.TEXT_DARK,
                    false
            );
        }

        // Red close button.
        redX(ctx, px + POPUP_CLOSE_X, py + POPUP_CLOSE_Y, POPUP_CLOSE_W, POPUP_CLOSE_H);

        SimpleContainer inv = menu.getUserPopupInventory();
        int index = 0;
        for (int row = 0; row < AuctionHouseScreenHandler.POPUP_ROWS; row++) {
            for (int col = 0; col < AuctionHouseScreenHandler.POPUP_COLUMNS; col++) {
                int sx = px + POPUP_SLOT_LOCAL_X + col * POPUP_SLOT_SPACING;
                int sy = py + POPUP_SLOT_LOCAL_Y + row * POPUP_SLOT_SPACING;

                NotchWidgets.slot(ctx, sx, sy);

                ItemStack stack = inv.getItem(index++);
                if (!stack.isEmpty()) {
                    ctx.renderItem(stack, sx + 1, sy + 1);
                    ctx.renderItemDecorations(this.font, stack, sx + 1, sy + 1);
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

        matrices.popPose();
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
        // Bid prompt swallows all clicks while open.
        if (showBidPopup) {
            int px = this.leftPos + BID_X, py = this.topPos + BID_Y;
            if (over((int) mouseX, (int) mouseY, px + 10, py + 68, 58, 14)) { submitBid(); return true; }
            if (over((int) mouseX, (int) mouseY, px + 72, py + 68, 58, 14)) { showBidPopup = false; return true; }
            return true;
        }

        // Popup handling (unchanged)
        if (showUserPopup) {
            int px = this.leftPos + POPUP_LOCAL_X;
            int py = this.topPos + POPUP_LOCAL_Y;

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
                ItemStack stack = menu.getUserPopupInventory().getItem(slot);
                CompoundTag tag = StackData.getData(stack);
                if (!stack.isEmpty() && net.fugginbeenus.notchcurrency.compat.Nbt.hasUuid(tag, "nc_listing_id")) {
                    FriendlyByteBuf buf = PacketByteBufs.create();
                    buf.writeUUID(net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(tag, "nc_listing_id"));
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
        if (button == 0 && !showUserPopup && this.minecraft != null && this.minecraft.player != null) {
            Slot clickedSlot = this.hoveredSlot;
            if (clickedSlot != null
                    && clickedSlot.hasItem()
                    && clickedSlot.getContainerSlot() < AuctionHouseScreenHandler.LISTING_SIZE
                    && this.isHovering(clickedSlot.x, clickedSlot.y, 16, 16, mouseX, mouseY)) {

                ItemStack stack = clickedSlot.getItem();
                CompoundTag tag = StackData.getData(stack);

                if (tag.contains("nc_seller", Tag.TAG_STRING)
                        && tag.contains("nc_expires", Tag.TAG_LONG)) {

                    long expires = tag.getLong("nc_expires");
                    String seller = tag.getString("nc_seller");
                    String selfName = this.minecraft.player.getName().getString();

                    // Only block BUY-NOW listings (expires <= 0), not timed auctions
                    if (expires <= 0L && seller.equals(selfName)) {
                        this.minecraft.player.displayClientMessage(
                                Component.literal("You can't buy your own listing.")
                                        .withStyle(ChatFormatting.RED),
                                false
                        );
                        // Swallow the click; do not send it to the server
                        return true;
                    }
                }
            }
        }

        // Let HandledScreen send click to server (for buy-now behaviour, inventory, etc.)
        //? if >=1.21.11 {
        /*boolean handled = super.mouseClicked(event, doubleClick);
        *///?} else {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        //?}

        // Left click on a TIMED auction => open bid command in chat (existing behaviour)
        if (button == 0 && !showUserPopup && this.minecraft != null) {
            // Use focusedSlot instead of getSlotAt (getSlotAt is private in 1.20.1)
            Slot clicked = this.hoveredSlot;
            if (clicked != null
                    && clicked.hasItem()
                    && clicked.getContainerSlot() < AuctionHouseScreenHandler.LISTING_SIZE
                    && this.isHovering(clicked.x, clicked.y, 16, 16, mouseX, mouseY)) {

                ItemStack stack = clicked.getItem();
                CompoundTag tag = StackData.getData(stack);
                if (tag.contains("nc_expires", Tag.TAG_LONG)
                        && net.fugginbeenus.notchcurrency.compat.Nbt.hasUuid(tag, "nc_listing_id")) {

                    long expires = tag.getLong("nc_expires");

                    // Timed auction (expires > 0): open the in-GUI bid prompt (no id in chat).
                    if (expires > 0L) {
                        bidListingId = net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(tag, "nc_listing_id");
                        bidItemName = stack.getHoverName().getString();
                        bidStartPrice = tag.getLong("nc_price");
                        bidHighest = tag.contains("nc_highest_bid", Tag.TAG_LONG)
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
    protected void renderBlurredBackground(float delta) {
        // No 1.21 menu blur behind the mod's screens. They draw crisp panels over the world.
    }
    *///?}
}
