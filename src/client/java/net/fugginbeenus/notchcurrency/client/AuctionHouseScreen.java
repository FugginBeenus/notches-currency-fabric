package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.auction.AuctionHouseScreenHandler;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
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
import net.minecraft.screen.slot.Slot;
import net.minecraft.client.gui.screen.ChatScreen;


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
    private static final int PREV_W = 35;
    private static final int PREV_H = 17;

    private static final int NEXT_X = 98;    // right arrow
    private static final int NEXT_Y = 102;
    private static final int NEXT_W = 35;
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

    // popup state (overlay only)
    private boolean showUserPopup = false;

    // --- tooltip coin placement ---
    private boolean priceTooltipActive = false;
    private int priceTooltipCoinX = 0;
    private int priceTooltipCoinY = 0;

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
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        ctx.drawTexture(TEX, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

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
    }

    @Override
    protected void drawMouseoverTooltip(DrawContext ctx, int mouseX, int mouseY) {
        priceTooltipActive = false;

        // Block tooltips under the popup
        if (showUserPopup) {
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
            NbtCompound tag = stack.getNbt();

            if (tag != null
                    && tag.contains("nc_price", NbtElement.LONG_TYPE)
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

                List<Text> lines = new ArrayList<>();

                // Line 1: rarity colored name
                Text nameLine = stack.getName().copy().formatted(rarityColor);
                lines.add(nameLine);

                // Price line: for auctions, show current bid or start; for buy-now, fixed price
                long displayPrice = (timedAuction && highestBid > 0L) ? highestBid : startPrice;

                MutableText priceLine = Text.empty()
                        .append(Text.literal("Price: ").formatted(Formatting.GOLD))
                        .append(Text.literal(String.valueOf(displayPrice)).formatted(Formatting.YELLOW));
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

                // Draw tooltip
                ctx.drawTooltip(this.textRenderer, lines, mouseX, mouseY);

                // --- Compute actual tooltip box to align coin properly ---
                int maxWidth = 0;
                for (Text line : lines) {
                    int w = this.textRenderer.getWidth(line);
                    if (w > maxWidth) {
                        maxWidth = w;
                    }
                }

                int lineHeight = this.textRenderer.fontHeight + 2;
                int boxWidth = maxWidth + 8;
                int boxHeight = lines.size() * lineHeight + 8;

                int tooltipX = mouseX + 12;
                int tooltipY = mouseY - 12;

                if (tooltipX + boxWidth > this.width) {
                    tooltipX = this.width - boxWidth - 4;
                }
                if (tooltipY + boxHeight > this.height) {
                    tooltipY = this.height - boxHeight - 4;
                }

                int textLeft = tooltipX + 4;
                int priceLineIndex = 1; // price line is second line
                int priceLineY = tooltipY + 4 + lineHeight * priceLineIndex;

                int priceWidth = this.textRenderer.getWidth(priceLine);

                priceTooltipCoinX = textLeft + priceWidth - 5;
                priceTooltipCoinY = priceLineY - 7;
                priceTooltipActive = true;

                return;
            }
        }

        // Fallback: normal behaviour
        super.drawMouseoverTooltip(ctx, mouseX, mouseY);
    }



    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(ctx, mouseX, mouseY);

        if (priceTooltipActive) {
            var matrices = ctx.getMatrices();
            matrices.push();
            matrices.translate(0.0F, 0.0F, 400.0F);

            float scale = 0.55F;
            matrices.translate(priceTooltipCoinX + 8, priceTooltipCoinY + 8, 0.0F);
            matrices.scale(scale, scale, 1.0F);

            ctx.drawItem(new ItemStack(ModItems.NOTCH_COIN), -8, -8);

            matrices.pop();
        }

        if (showUserPopup) {
            drawUserPopup(ctx);
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
        ctx.drawTexture(USER_POPUP_TEX, px, py, 0, 0, POPUP_W, POPUP_H);

        if (this.client != null && this.client.player != null) {
            String name = this.client.player.getName().getString();
            ctx.drawText(
                    this.textRenderer,
                    name,
                    px + POPUP_NAME_X,
                    py + POPUP_NAME_Y,
                    0x404040,
                    false
            );
        }

        SimpleInventory inv = handler.getUserPopupInventory();
        int index = 0;
        for (int row = 0; row < AuctionHouseScreenHandler.POPUP_ROWS; row++) {
            for (int col = 0; col < AuctionHouseScreenHandler.POPUP_COLUMNS; col++) {
                int sx = px + POPUP_SLOT_LOCAL_X + col * POPUP_SLOT_SPACING;
                int sy = py + POPUP_SLOT_LOCAL_Y + row * POPUP_SLOT_SPACING;

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
                NbtCompound tag = stack.getNbt();

                if (tag != null
                        && tag.contains("nc_seller", NbtElement.STRING_TYPE)
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
                NbtCompound tag = stack.getNbt();
                if (tag != null
                        && tag.contains("nc_expires", NbtElement.LONG_TYPE)
                        && tag.containsUuid("nc_listing_id")) {

                    long expires = tag.getLong("nc_expires");

                    // Timed auction only (expires > 0): open /ah bid in chat
                    if (expires > 0L) {
                        UUID id = tag.getUuid("nc_listing_id");
                        String cmd = "/ah bid " + id.toString() + " ";

                        this.client.setScreen(new ChatScreen(cmd));
                        return true;
                    }
                }
            }
        }

        return handled;
    }
}