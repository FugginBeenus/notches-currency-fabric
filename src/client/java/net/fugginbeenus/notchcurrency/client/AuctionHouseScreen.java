package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.compat.NetClient;
import net.fugginbeenus.notchcurrency.auction.AuctionHouseScreenHandler;
import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
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
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class AuctionHouseScreen extends AbstractContainerScreen<AuctionHouseScreenHandler> {

    private static final int W = 178;
    private static final int H = 219;
    private static final int MY_X = 8;
    private static final int MY_Y = 14;
    private static final int MY_W = 80;
    private static final int MY_H = 17;
    private static final int HELP_X = 153;
    private static final int HELP_Y = 14;
    private static final int HELP_W = 17;
    private static final int HELP_H = 17;
    private static final int RELOAD_X = 134;
    private static final int RELOAD_Y = 14;
    private static final int RELOAD_W = 17;
    private static final int RELOAD_H = 17;
    private static final int PG_LX = 8;
    private static final int PG_RX = 116;
    private static final int PG_Y = 110;
    private static final int PG_BW = 16;
    private static final int PG_H = 17;
    private static final int STAR_X = 134;
    private static final int STAR_Y = 110;
    private static final int STAR_W = 17;
    private static final int STAR_H = 17;
    private static final int CLOCK_X = 153;
    private static final int CLOCK_Y = 110;
    private static final int CLOCK_W = 17;
    private static final int CLOCK_H = 17;
    private static final int LIST_X = 96;
    private static final int LIST_Y = 14;
    private static final int LIST_W = 17;
    private static final int LIST_H = 17;
    private static final int RAFFLE_X = 115;
    private static final int RAFFLE_Y = 14;
    private static final int RAFFLE_W = 17;
    private static final int RAFFLE_H = 17;
    private static final int POPUP_W = 107;
    private static final int POPUP_H = 69;
    private static final int POPUP_LOCAL_X = (W - POPUP_W) / 2;
    private static final int POPUP_LOCAL_Y = 36;
    private static final int POPUP_CLOSE_X = 85;
    private static final int POPUP_CLOSE_Y = 6;
    private static final int POPUP_CLOSE_W = 14;
    private static final int POPUP_CLOSE_H = 15;
    private static final int POPUP_SLOT_LOCAL_X = 9;
    private static final int POPUP_SLOT_LOCAL_Y = 25;
    private static final int POPUP_SLOT_SIZE = 18;
    private static final int POPUP_SLOT_SPACING = 18;
    private static final int POPUP_DIM_COLOR = 0x66000000;
    private static final boolean DEBUG_MAIN = false;
    private static final boolean DEBUG_POPUP_BUTTON = false;
    private static final boolean DEBUG_POPUP_SLOTS = false;
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

    private Button prevButton;
    private Button nextButton;
    private Button myListingsButton;
    private Button filterStarButton;
    private Button sortTypeButton;
    private Button helpButton;
    private Button reloadButton;
    private Button listItemButton;
    private Button raffleButton;
    private boolean showUserPopup = false;
    private static final int BID_W = 150, BID_H = 110, BID_X = (W - BID_W) / 2, BID_Y = (H - BID_H) / 2,
            BID_BTN_W = (BID_W - 24) / 2;
    private boolean showBidPopup = false;
    private UUID bidListingId = null;
    private String bidItemName = "";
    private long bidStartPrice = 0L;
    private long bidHighest = 0L;
    private String bidInput = "";

    public AuctionHouseScreen(AuctionHouseScreenHandler handler,
                              Inventory inv,
                              Component title) {
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

    @Override
    protected void init() {
        super.init();

        this.clearWidgets();
        showUserPopup = false;
        myListingsButton = Button.builder(Component.empty(), b -> {
                    showUserPopup = true;
                })
                .bounds(this.leftPos + MY_X, this.topPos + MY_Y, MY_W, MY_H)
                .build();
        myListingsButton.setAlpha(0.0f);
        addRenderableWidget(myListingsButton);

        helpButton = Button.builder(Component.empty(), b -> {
            if (this.minecraft != null && this.minecraft.player != null) {
                Component linkText = Component.literal("Click here for the Notch Currency Guide")
                        .withStyle(style -> style
                                .withColor(ChatFormatting.GOLD)
                                .withUnderlined(true)
                                .withClickEvent(net.fugginbeenus.notchcurrency.compat.Chat.openUrl("https://github.com/FugginBeenus/NotchCurrency/wiki"))
                                .withHoverEvent(net.fugginbeenus.notchcurrency.compat.Chat.showText(Component.literal("Open the Notch Currency wiki")))
                        );

                Component message = Component.literal("Need help? ").withStyle(ChatFormatting.YELLOW).append(linkText);
                net.fugginbeenus.notchcurrency.compat.Msg.chat(this.minecraft.player, message);
            }
        }).bounds(this.leftPos + HELP_X, this.topPos + HELP_Y, HELP_W, HELP_H)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal("Get the guide link in chat")))
                .build();
        helpButton.setAlpha(0.0f);
        addRenderableWidget(helpButton);

        reloadButton = Button.builder(Component.empty(), b -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 4);
                    }
                })
                .bounds(this.leftPos + RELOAD_X, this.topPos + RELOAD_Y, RELOAD_W, RELOAD_H)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal("Refresh the list")))
                .build();
        reloadButton.setAlpha(0.0f);
        addRenderableWidget(reloadButton);

        prevButton = Button.builder(Component.empty(), b -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    }
                })
                .bounds(this.leftPos + PG_LX, this.topPos + PG_Y, PG_BW, PG_H)
                .build();
        prevButton.setAlpha(0.0f);
        addRenderableWidget(prevButton);

        nextButton = Button.builder(Component.empty(), b -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                    }
                })
                .bounds(this.leftPos + PG_RX, this.topPos + PG_Y, PG_BW, PG_H)
                .build();
        nextButton.setAlpha(0.0f);
        addRenderableWidget(nextButton);

        filterStarButton = Button.builder(Component.empty(), b -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 2);
                    }
                })
                .bounds(this.leftPos + STAR_X, this.topPos + STAR_Y, STAR_W, STAR_H)
                .build();
        filterStarButton.setAlpha(0.0f);
        addRenderableWidget(filterStarButton);

        sortTypeButton = Button.builder(Component.empty(), b -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 3);
                    }
                })
                .bounds(this.leftPos + CLOCK_X, this.topPos + CLOCK_Y, CLOCK_W, CLOCK_H)
                .build();
        sortTypeButton.setAlpha(0.0f);
        addRenderableWidget(sortTypeButton);

        listItemButton = Button.builder(Component.empty(), b -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 6);
                    }
                })
                .bounds(this.leftPos + LIST_X, this.topPos + LIST_Y, LIST_W, LIST_H)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal("List an item for auction")))
                .build();
        listItemButton.setAlpha(0.0f);
        addRenderableWidget(listItemButton);

        raffleButton = Button.builder(Component.empty(), b -> {
                    if (this.minecraft != null && this.minecraft.gameMode != null) {
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 5);
                    }
                })
                .bounds(this.leftPos + RAFFLE_X, this.topPos + RAFFLE_Y, RAFFLE_W, RAFFLE_H)
                .tooltip(net.minecraft.client.gui.components.Tooltip.create(
                        Component.literal("Open the raffle")))
                .build();
        raffleButton.setAlpha(0.0f);
        addRenderableWidget(raffleButton);
    }

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = this.leftPos, y = this.topPos;

        final int gridY = AuctionHouseScreenHandler.LISTING_START_Y;
        final int invY = AuctionHouseScreenHandler.PLAYER_INV_START_Y;

        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.font, "Auction House", x + W / 2, y + 7, NotchWidgets.Plate.BRASS);
        greenBar(ctx, x + MY_X, y + MY_Y, MY_W, MY_H,
                over(mouseX, mouseY, x + MY_X, y + MY_Y, MY_W, MY_H));
        iconButton(ctx, x + LIST_X, y + LIST_Y, LIST_W, LIST_H,
                over(mouseX, mouseY, x + LIST_X, y + LIST_Y, LIST_W, LIST_H), ICON_PLUS);
        iconButton(ctx, x + RAFFLE_X, y + RAFFLE_Y, RAFFLE_W, RAFFLE_H,
                over(mouseX, mouseY, x + RAFFLE_X, y + RAFFLE_Y, RAFFLE_W, RAFFLE_H), ICON_TICKET);
        iconButton(ctx, x + RELOAD_X, y + RELOAD_Y, RELOAD_W, RELOAD_H,
                over(mouseX, mouseY, x + RELOAD_X, y + RELOAD_Y, RELOAD_W, RELOAD_H), ICON_RELOAD);
        iconButton(ctx, x + HELP_X, y + HELP_Y, HELP_W, HELP_H,
                over(mouseX, mouseY, x + HELP_X, y + HELP_Y, HELP_W, HELP_H), ICON_HELP);
        for (int row = 0; row < AuctionHouseScreenHandler.LISTING_ROWS; row++) {
            for (int col = 0; col < AuctionHouseScreenHandler.LISTING_COLUMNS; col++) {
                NotchWidgets.slot(ctx, x + 9 + col * 18 - 1, y + gridY + row * 18 - 1);
            }
        }
        NotchWidgets.pager(ctx, this.font, x + PG_LX, x + PG_RX, y + PG_Y, PG_BW, PG_H,
                menu.getPage() + 1, Math.max(1, menu.getTotalPages()),
                over(mouseX, mouseY, x + PG_LX, y + PG_Y, PG_BW, PG_H),
                over(mouseX, mouseY, x + PG_RX, y + PG_Y, PG_BW, PG_H));
        iconButton(ctx, x + STAR_X, y + STAR_Y, STAR_W, STAR_H,
                over(mouseX, mouseY, x + STAR_X, y + STAR_Y, STAR_W, STAR_H), ICON_STAR);
        iconButton(ctx, x + CLOCK_X, y + CLOCK_Y, CLOCK_W, CLOCK_H,
                over(mouseX, mouseY, x + CLOCK_X, y + CLOCK_Y, CLOCK_W, CLOCK_H), ICON_SORT);
        NotchWidgets.divider(ctx, x + 8, y + invY - 6, W - 16);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                NotchWidgets.slot(ctx, x + 9 + col * 18 - 1, y + invY + row * 18 - 1);
            }
        }
        for (int col = 0; col < 9; col++) {
            NotchWidgets.slot(ctx, x + 9 + col * 18 - 1, y + invY + 58 - 1);
        }
        //? if >=26.1 {
        /*
        super.extractContents(ctx, mouseX, mouseY, delta);
        *///?}
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

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
        int tx = bx + (w - NotchWidgets.smallCapsWidth(s)) / 2, ty = by + (h - 5) / 2;
        NotchWidgets.smallCaps(ctx, s, tx + 1, ty + 1, 0xFF2C5A22);
        NotchWidgets.smallCaps(ctx, s, tx, ty, 0xFFFFFFFF);
    }

    private void redX(GuiGraphics ctx, int bx, int by, int w, int h, boolean hov) {
        NotchWidgets.colorButton(ctx, bx, by, w, h, NotchTheme.ACCENT_RED, NotchTheme.RED_HI, NotchTheme.RED_LO, hov);
        int cx = bx + w / 2, cy = by + h / 2;
        for (int i = -2; i <= 2; i++) {
            ctx.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, NotchTheme.TEXT_LIGHT);
            ctx.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, NotchTheme.TEXT_LIGHT);
        }
    }

    private void iconButton(GuiGraphics ctx, int bx, int by, int w, int h, boolean hov, Icon icon) {
        NotchWidgets.button(ctx, bx, by, w, h, hov, false);
        int iw = 0;
        for (String row : icon.rows()) iw = Math.max(iw, row.length());
        int ox = bx + (w - iw) / 2, oy = by + (h - icon.rows().length) / 2;
        for (int r = 0; r < icon.rows().length; r++) {
            String row = icon.rows()[r];
            for (int c = 0; c < row.length(); c++) {
                int k = icon.marks().indexOf(row.charAt(c));
                if (k >= 0) ctx.fill(ox + c, oy + r, ox + c + 1, oy + r + 1, icon.colors()[k]);
            }
        }
    }

    private record Icon(String marks, int[] colors, String... rows) {}

    private static final Icon ICON_PLUS = new Icon("ogG", new int[]{0xFF2C5A22, 0xFF61A04F, 0xFF8FD07A},
            "...ooo...",
            "...oGo...",
            "...ogo...",
            "oooogoooo",
            "oGGgggggo",
            "oooogoooo",
            "...ogo...",
            "...ogo...",
            "...ooo...");
    private static final Icon ICON_TICKET = new Icon("orcy", new int[]{0xFF962A24, 0xFFD8433A, 0xFFF5ECD2, 0xFFF3B93F},
            "ooooooooo",
            "orrrrrrro",
            ".rcccccr.",
            ".rccyccr.",
            ".rcccccr.",
            "orrrrrrro",
            "ooooooooo");
    private static final Icon ICON_RELOAD = new Icon("o", new int[]{0xFF3A3A3A},
            "...ooo.o.",
            ".oo...oo.",
            ".o...ooo.",
            "o........",
            "o........",
            "o.......o",
            ".o.....o.",
            ".oo...oo.",
            "...ooo...");
    private static final Icon ICON_HELP = new Icon("o", new int[]{0xFF3A3A3A},
            ".ooooo.",
            "oo...oo",
            "oo...oo",
            "....oo.",
            "...oo..",
            "..ooo..",
            "..ooo..",
            ".......",
            "..ooo..");
    private static final Icon ICON_STAR = new Icon("oyYs", new int[]{0xFF8A5A1E, 0xFFF3B93F, 0xFFFFE08A, 0xFFC4842A},
            "....o....",
            "...oYo...",
            "...oYo...",
            "ooooyoooo",
            "oYYyyyyso",
            ".oyyyyso.",
            "..oyyso..",
            ".oysoyso.",
            ".ooo.ooo.");
    private static final Icon ICON_SORT = new Icon("ow", new int[]{0xFF3A3A3A, 0xFFF5ECD2},
            "..ooo....o.",
            ".owwwo...o.",
            "owwowwo..o.",
            "owwoowo..o.",
            "owwwwwo.ooo",
            ".owwwo...o.",
            "..ooo......");

    private void drawListTooltip(GuiGraphics ctx,
                                 int mouseX, int mouseY,
                                 Component title,
                                 String[] options,
                                 String activeLabel) {
        net.fugginbeenus.notchcurrency.compat.Render.pushGui(ctx);
        net.fugginbeenus.notchcurrency.compat.Render.translateGui(ctx, 0.0F, 0.0F);

        int maxWidth = this.font.width(title);

        for (String opt : options) {
            int w = this.font.width("• " + opt);
            if (w > maxWidth) {
                maxWidth = w;
            }
        }

        int lineHeight = this.font.lineHeight + 2;
        int lines = 1 + options.length;
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
        ctx.drawString(this.font, title, textX, textY, 0xFFFFFFFF);
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

        net.fugginbeenus.notchcurrency.compat.Render.popGui(ctx);
    }

    //? if >=26.1 {
    /*@Override
    protected void extractTooltip(GuiGraphics ctx, int mouseX, int mouseY) {
    *///?} else {
    @Override
    protected void renderTooltip(GuiGraphics ctx, int mouseX, int mouseY) {
    //?}
        if (showUserPopup || showBidPopup) return;
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

                boolean timedAuction = expires > 0L;
                Component statusLine = null;
                Component timeLine = null;
                long now = 0L;

                if (timedAuction && this.minecraft != null && this.minecraft.level != null) {
                    now = this.minecraft.level.getGameTime();
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

                long displayPrice = (timedAuction && highestBid > 0L) ? highestBid : startPrice;
                List<Component> lines = new ArrayList<>();
                boolean shiftHeld = net.fugginbeenus.notchcurrency.compat.Render.shiftDown();

                if (shiftHeld) {
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
                    lines.add(Component.literal("─────────────").withStyle(ChatFormatting.DARK_GRAY));
                    ItemStack cleanStack = stack.copy();
                    if (StackData.hasData(cleanStack)) {
                        CompoundTag cleanTag = StackData.editData(cleanStack);
                        cleanTag.remove("nc_price");
                        cleanTag.remove("nc_seller");
                        cleanTag.remove("nc_created");
                        cleanTag.remove("nc_expires");
                        cleanTag.remove("nc_highest_bid");
                        cleanTag.remove("nc_highest_bidder");
                        cleanTag.remove("nc_listing_id");

                        //? if <1.21 {
                        if (cleanTag.contains("display", 10)) {
                            CompoundTag display = cleanTag.getCompound("display");
                            display.remove("Lore");
                            if (display.isEmpty()) {
                                cleanTag.remove("display");
                            }
                        }
                        //?}

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

                    for (Component line : vanillaLines) {
                        lines.add(line);
                    }
                    lines.add(Component.literal(""));
                    lines.add(Component.literal("[Release Shift] Auction info").withStyle(ChatFormatting.DARK_GRAY));

                } else {
                    Component nameLine = stack.getHoverName().copy().withStyle(rarityColor);
                    lines.add(nameLine);
                    MutableComponent priceLine = Component.empty()
                            .append(Component.literal("Price: ").withStyle(ChatFormatting.GOLD))
                            .append(Component.literal(String.valueOf(displayPrice) + " ").withStyle(ChatFormatting.YELLOW))
                            .append(Component.literal("\uE000"));
                    lines.add(priceLine);
                    if (timedAuction && highestBid > 0L) {
                        MutableComponent bidLine = Component.empty()
                                .append(Component.literal("Highest bid: ").withStyle(ChatFormatting.AQUA))
                                .append(Component.literal(String.valueOf(highestBid)).withStyle(ChatFormatting.AQUA));
                        if (highestBidder != null && !highestBidder.isEmpty()) {
                            bidLine.append(Component.literal(" by " + highestBidder).withStyle(ChatFormatting.GRAY));
                        }
                        lines.add(bidLine);
                    }
                    lines.add(
                            Component.literal("Seller: " + seller)
                                    .withStyle(ChatFormatting.GRAY)
                    );
                    lines.add(
                            Component.literal("Rarity: " + rarityName).withStyle(rarityColor)
                    );
                    if (timedAuction && statusLine != null) {
                        lines.add(statusLine);
                        if (timeLine != null) {
                            lines.add(timeLine);
                        }
                    }
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
                    lines.add(Component.literal("[Shift] Item details").withStyle(ChatFormatting.DARK_GRAY));
                }
                ctx.renderComponentTooltip(this.font, lines, mouseX, mouseY);

                return;
            }
        }

        super.renderTooltip(ctx, mouseX, mouseY);
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
        this.renderTooltip(ctx, mouseX, mouseY);

        if (showUserPopup) {
            drawUserPopup(ctx, mouseX, mouseY);
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

    private void drawBidPopup(GuiGraphics ctx, int mouseX, int mouseY) {
        net.fugginbeenus.notchcurrency.compat.Render.pushGui(ctx);
        net.fugginbeenus.notchcurrency.compat.Render.translateGui(ctx, 0.0F, 0.0F);
        ctx.fill(0, 0, this.width, this.height, 0x88000000);
        int px = this.leftPos + BID_X, py = this.topPos + BID_Y;
        NotchWidgets.panel(ctx, px, py, BID_W, BID_H);
        NotchWidgets.title(ctx, this.font, "Place a Bid", px + BID_W / 2, py + 6, NotchWidgets.Plate.BRASS);
        String name = bidItemName;
        while (name.length() > 3 && this.font.width(name) > BID_W - 16) name = name.substring(0, name.length() - 2) + "…";
        NotchWidgets.centerText(ctx, this.font, name, px + BID_W / 2, py + 15, NotchTheme.TEXT_DARK, false);
        long cur = bidHighest > 0 ? bidHighest : bidStartPrice;
        bidRow(ctx, px, py + 28, bidHighest > 0 ? "Current bid" : "Start price", cur);
        bidRow(ctx, px, py + 47, "Min next bid", cur + 1);

        NotchWidgets.coinInput(ctx, px + 10, py + 69, BID_W - 20);
        if (bidInput.isEmpty()) {
            ctx.drawString(this.font, "amount", px + 28, py + 74, 0xFF555555, false);
        } else {
            ctx.drawString(this.font, bidInput, px + 28, py + 74, NotchWidgets.COIN_GOLD, false);
        }
        if ((System.currentTimeMillis() / 500) % 2 == 0) {
            ctx.drawString(this.font, "_", px + 28 + this.font.width(bidInput), py + 74, 0xFFFFFFFF, false);
        }

        NotchWidgets.primaryButton(ctx, this.font, px + 10, py + 90, BID_BTN_W, 14, "Bid",
                over(mouseX, mouseY, px + 10, py + 90, BID_BTN_W, 14));
        NotchWidgets.dangerButton(ctx, this.font, px + 14 + BID_BTN_W, py + 90, BID_BTN_W, 14, "Cancel",
                over(mouseX, mouseY, px + 14 + BID_BTN_W, py + 90, BID_BTN_W, 14));

        net.fugginbeenus.notchcurrency.compat.Render.popGui(ctx);
    }

    private void bidRow(GuiGraphics ctx, int px, int ry, String label, long amount) {
        ctx.drawString(this.font, label, px + 10, ry + 5, NotchTheme.TEXT_DARK, false);
        String text = String.format(java.util.Locale.ROOT, "%,d", amount);
        int w = 18 + this.font.width(text) - 1 + 4;
        NotchWidgets.coinField(ctx, this.font, px + BID_W - 10 - w, ry, text, NotchWidgets.COIN_GOLD);
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
                net.fugginbeenus.notchcurrency.compat.Msg.chat(this.minecraft.player, Component.literal("Bid must be at least " + min + ".").withStyle(ChatFormatting.RED));
            }
            return;
        }
        if (bidListingId != null) {
            FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
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
        if (showUserPopup && keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            showUserPopup = false;
            return true;
        }
        if (showBidPopup) {
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) { showBidPopup = false; return true; }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER) { submitBid(); return true; }
            if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE) {
                if (!bidInput.isEmpty()) bidInput = bidInput.substring(0, bidInput.length() - 1);
                return true;
            }
            return true;
        }
        //? if >=1.21.11 {
        /*return super.keyPressed(event);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
    }

    //? if >=26.1 {
    /*@Override
    public boolean charTyped(net.minecraft.client.input.CharacterEvent event) {
        char chr = (char) event.codepoint();
        int modifiers = 0;
    *///?} elif >=1.21.11 {
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
        net.fugginbeenus.notchcurrency.compat.Render.pushGui(ctx);
        net.fugginbeenus.notchcurrency.compat.Render.translateGui(ctx, 0.0F, 0.0F);
        ctx.renderComponentTooltip(this.font, lines, mouseX, mouseY);
        net.fugginbeenus.notchcurrency.compat.Render.popGui(ctx);
    }

    private void drawUserPopup(GuiGraphics ctx, int mouseX, int mouseY) {
        net.fugginbeenus.notchcurrency.compat.Render.pushGui(ctx);
        net.fugginbeenus.notchcurrency.compat.Render.translateGui(ctx, 0.0F, 0.0F);

        ctx.fill(this.leftPos + 1, this.topPos + 1, this.leftPos + W - 1, this.topPos + H - 1, POPUP_DIM_COLOR);

        int px = this.leftPos + POPUP_LOCAL_X;
        int py = this.topPos + POPUP_LOCAL_Y;
        NotchWidgets.panel(ctx, px, py, POPUP_W, POPUP_H);
        NotchWidgets.smallCaps(ctx, "MY LISTINGS", px + POPUP_SLOT_LOCAL_X, py + POPUP_CLOSE_Y + 5, NotchTheme.TEXT_DARK);
        redX(ctx, px + POPUP_CLOSE_X, py + POPUP_CLOSE_Y, POPUP_CLOSE_W, POPUP_CLOSE_H,
                over(mouseX, mouseY, px + POPUP_CLOSE_X, py + POPUP_CLOSE_Y, POPUP_CLOSE_W, POPUP_CLOSE_H));

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

        net.fugginbeenus.notchcurrency.compat.Render.popGui(ctx);
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
        if (showBidPopup) {
            int px = this.leftPos + BID_X, py = this.topPos + BID_Y;
            if (over((int) mouseX, (int) mouseY, px + 10, py + 90, BID_BTN_W, 14)) {
                NotchWidgets.click();
                submitBid();
                return true;
            }
            if (over((int) mouseX, (int) mouseY, px + 14 + BID_BTN_W, py + 90, BID_BTN_W, 14)) {
                NotchWidgets.click();
                showBidPopup = false;
                return true;
            }
            return true;
        }
        if (showUserPopup) {
            int px = this.leftPos + POPUP_LOCAL_X;
            int py = this.topPos + POPUP_LOCAL_Y;
            if (over((int) mouseX, (int) mouseY, px + POPUP_CLOSE_X, py + POPUP_CLOSE_Y, POPUP_CLOSE_W, POPUP_CLOSE_H)
                    || !over((int) mouseX, (int) mouseY, px, py, POPUP_W, POPUP_H)) {
                NotchWidgets.click();
                showUserPopup = false;
                return true;
            }
            int slot = hoveredPopupSlot(mouseX, mouseY);
            if (slot >= 0) {
                ItemStack stack = menu.getUserPopupInventory().getItem(slot);
                CompoundTag tag = StackData.getData(stack);
                if (!stack.isEmpty() && net.fugginbeenus.notchcurrency.compat.Nbt.hasUuid(tag, "nc_listing_id")) {
                    FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
                    buf.writeUUID(net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(tag, "nc_listing_id"));
                    NetClient.sendToServer(NotchPackets.AUCTION_CANCEL, buf);
                }
                return true;
            }
            return true;
        }
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
                    if (expires <= 0L && seller.equals(selfName)) {
                        net.fugginbeenus.notchcurrency.compat.Msg.chat(this.minecraft.player, Component.literal("You can't buy your own listing.")
                                        .withStyle(ChatFormatting.RED));
                        return true;
                    }
                }
            }
        }

        //? if >=1.21.11 {
        /*boolean handled = super.mouseClicked(event, doubleClick);
        *///?} else {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        //?}
        if (button == 0 && !showUserPopup && this.minecraft != null) {
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
    //? if >=1.21.11 {
    /*@Override
    protected void renderBlurredBackground(net.minecraft.client.gui.GuiGraphics ctx) {
    }
    *///?} elif >=1.21 {
    /*@Override
    protected void renderBlurredBackground(float delta) {
    }
    *///?}
}
