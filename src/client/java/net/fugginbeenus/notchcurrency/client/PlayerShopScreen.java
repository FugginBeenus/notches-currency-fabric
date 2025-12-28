package net.fugginbeenus.notchcurrency.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.shop.PlayerShopScreenHandler;
import net.fugginbeenus.notchcurrency.shop.ShopListing;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * Shop GUI with responsive slot-based interaction.
 */
public class PlayerShopScreen extends HandledScreen<PlayerShopScreenHandler> {

    private static final Identifier BROWSER_TEXTURE = NotchCurrency.id("textures/gui/shop/shop_browser.png");
    private static final Identifier MANAGE_TEXTURE = NotchCurrency.id("textures/gui/shop/shop_manage.png");

    private static final int BROWSER_WIDTH = 176;
    private static final int BROWSER_HEIGHT = 237;
    private static final int BROWSER_TEX_WIDTH = 275;
    private static final int BROWSER_TEX_HEIGHT = 256;

    private static final int TRADE_ROW_U = 178;
    private static final int TRADE_ROW_V = 49;
    private static final int TRADE_ROW_WIDTH = 95;
    private static final int TRADE_ROW_HEIGHT = 23;

    private static final int ARROW_U = 178;
    private static final int ARROW_V = 74;
    private static final int ARROW_WIDTH = 14;
    private static final int ARROW_HEIGHT = 9;
    private static final int ARROW_DISABLED_V = 84;

    private static final int COIN_U = 178;
    private static final int COIN_V = 31;
    private static final int COIN_SIZE = 13;

    private static final int MANAGE_WIDTH = 256;
    private static final int MANAGE_HEIGHT = 256;
    private static final int MANAGE_TEX_SIZE = 256;

    private static final int VISIBLE_ROWS = 6;

    private final PlayerShopScreenHandler.Mode mode;
    private final List<TextFieldWidget> priceFields = new ArrayList<>();
    private int hoveredBrowserRow = -1;

    public PlayerShopScreen(PlayerShopScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.mode = handler.getMode();

        if (mode == PlayerShopScreenHandler.Mode.BROWSE) {
            this.backgroundWidth = BROWSER_WIDTH;
            this.backgroundHeight = BROWSER_HEIGHT;
        } else {
            this.backgroundWidth = MANAGE_WIDTH;
            this.backgroundHeight = MANAGE_HEIGHT;
        }
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = 1000;
        this.playerInventoryTitleX = 1000;

        if (mode == PlayerShopScreenHandler.Mode.MANAGE) {
            initPriceFields();
        }
    }

    private void initPriceFields() {
        priceFields.clear();

        final int FIELD_X = 38;
        final int FIELD_WIDTH = 48;
        final int FIELD_HEIGHT = 12;
        final int[] ROW_Y = {17, 39, 61, 83, 105, 127};

        List<ShopListing> listings = handler.getListings();

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            TextFieldWidget field = new TextFieldWidget(
                    this.textRenderer,
                    this.x + FIELD_X, this.y + ROW_Y[row],
                    FIELD_WIDTH, FIELD_HEIGHT,
                    Text.empty()
            );

            field.setDrawsBackground(false);
            field.setMaxLength(7);
            field.setEditableColor(0xFFFFFF);

            if (row < listings.size()) {
                field.setText(String.valueOf(listings.get(row).getCoinPrice()));
            } else {
                field.setText("100");
            }

            priceFields.add(field);
            this.addDrawableChild(field);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);

        if (mode == PlayerShopScreenHandler.Mode.BROWSE) {
            updateBrowserHover(mouseX, mouseY);
        }
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        if (mode == PlayerShopScreenHandler.Mode.BROWSE) {
            drawBrowserBackground(context);
        } else {
            drawManageBackground(context);
        }
    }

    private void drawBrowserBackground(DrawContext context) {
        context.drawTexture(BROWSER_TEXTURE, this.x, this.y, 0, 0,
                BROWSER_WIDTH, BROWSER_HEIGHT, BROWSER_TEX_WIDTH, BROWSER_TEX_HEIGHT);

        String shopName = handler.getShopName();
        if (shopName != null && !shopName.isEmpty()) {
            context.drawText(this.textRenderer, shopName, this.x + 8, this.y + 6, 0x404040, false);
        }

        List<ShopListing> listings = handler.getListings();
        final int ROW_START_X = 7;
        final int ROW_START_Y = 18;
        final int ROW_HEIGHT = 23;

        for (int row = 0; row < Math.min(listings.size(), VISIBLE_ROWS); row++) {
            ShopListing listing = listings.get(row);
            int rowY = this.y + ROW_START_Y + (row * ROW_HEIGHT);

            context.drawTexture(BROWSER_TEXTURE, this.x + ROW_START_X, rowY,
                    TRADE_ROW_U, TRADE_ROW_V, TRADE_ROW_WIDTH, TRADE_ROW_HEIGHT,
                    BROWSER_TEX_WIDTH, BROWSER_TEX_HEIGHT);

            ItemStack displayItem = listing.getItemForSale().copy();
            context.drawItem(displayItem, this.x + ROW_START_X + 4, rowY + 3);
            context.drawItemInSlot(this.textRenderer, displayItem, this.x + ROW_START_X + 4, rowY + 3);

            String priceStr = String.valueOf(listing.getCoinPrice());
            context.drawText(this.textRenderer, priceStr, this.x + ROW_START_X + 26, rowY + 7, 0xFFD700, false);

            context.drawTexture(BROWSER_TEXTURE,
                    this.x + ROW_START_X + 26 + textRenderer.getWidth(priceStr) + 2, rowY + 5,
                    COIN_U, COIN_V, COIN_SIZE, COIN_SIZE, BROWSER_TEX_WIDTH, BROWSER_TEX_HEIGHT);

            String stockStr = "x" + listing.getStockQuantity();
            context.drawText(this.textRenderer, stockStr, this.x + ROW_START_X + 70, rowY + 7,
                    listing.isInStock() ? 0x55FF55 : 0xFF5555, false);

            int arrowV = listing.isInStock() ? ARROW_V : ARROW_DISABLED_V;
            context.drawTexture(BROWSER_TEXTURE, this.x + 110, rowY + 7,
                    ARROW_U, arrowV, ARROW_WIDTH, ARROW_HEIGHT, BROWSER_TEX_WIDTH, BROWSER_TEX_HEIGHT);
        }
    }

    private void drawManageBackground(DrawContext context) {
        context.drawTexture(MANAGE_TEXTURE, this.x, this.y, 0, 0,
                MANAGE_WIDTH, MANAGE_HEIGHT, MANAGE_TEX_SIZE, MANAGE_TEX_SIZE);

        // Balance display
        long balance = handler.getShopBalance();
        String balanceStr = String.valueOf(balance);
        context.drawText(this.textRenderer, balanceStr, this.x + 198, this.y + 176, 0xFFFFFF, false);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // Don't draw default labels
    }

    private void updateBrowserHover(int mouseX, int mouseY) {
        hoveredBrowserRow = -1;
        final int ROW_START_X = 7;
        final int ROW_START_Y = 18;
        final int ROW_HEIGHT = 23;
        final int ROW_WIDTH = 120;

        for (int row = 0; row < VISIBLE_ROWS; row++) {
            int rowY = this.y + ROW_START_Y + (row * ROW_HEIGHT);
            if (mouseX >= this.x + ROW_START_X && mouseX < this.x + ROW_START_X + ROW_WIDTH &&
                    mouseY >= rowY && mouseY < rowY + ROW_HEIGHT) {
                hoveredBrowserRow = row;
                break;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // Price field clicks FIRST - check if click is in a price field area
        if (mode == PlayerShopScreenHandler.Mode.MANAGE && button == 0) {
            for (int i = 0; i < priceFields.size(); i++) {
                TextFieldWidget field = priceFields.get(i);
                if (mouseX >= field.getX() && mouseX < field.getX() + field.getWidth() &&
                        mouseY >= field.getY() && mouseY < field.getY() + field.getHeight()) {
                    // Unfocus all other fields
                    for (TextFieldWidget other : priceFields) {
                        other.setFocused(false);
                    }
                    field.setFocused(true);
                    field.mouseClicked(mouseX, mouseY, button);
                    return true;
                }
            }
        }

        // Withdraw button (manage mode)
        if (mode == PlayerShopScreenHandler.Mode.MANAGE && button == 0) {
            int withdrawX = this.x + 137;
            int withdrawY = this.y + 209;
            if (mouseX >= withdrawX && mouseX < withdrawX + 24 &&
                    mouseY >= withdrawY && mouseY < withdrawY + 18) {
                withdrawBalance();
                return true;
            }
        }

        // Buy from browser
        if (mode == PlayerShopScreenHandler.Mode.BROWSE && button == 0 && hoveredBrowserRow >= 0) {
            List<ShopListing> listings = handler.getListings();
            if (hoveredBrowserRow < listings.size()) {
                ShopListing listing = listings.get(hoveredBrowserRow);
                if (listing.isInStock()) {
                    buyListing(listing);
                }
            }
            return true;
        }

        // Unfocus price fields if clicking elsewhere
        for (TextFieldWidget field : priceFields) {
            field.setFocused(false);
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // If a price field is focused, handle input there first
        for (TextFieldWidget field : priceFields) {
            if (field.isFocused()) {
                if (keyCode == 256) { // Escape - unfocus but don't close
                    field.setFocused(false);
                    return true;
                }
                // Let the field handle the key
                if (field.keyPressed(keyCode, scanCode, modifiers)) {
                    return true;
                }
                // Block inventory key (E) and other keys while typing
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        for (TextFieldWidget field : priceFields) {
            if (field.isFocused()) {
                if (Character.isDigit(chr)) {
                    return field.charTyped(chr, modifiers);
                }
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    private void buyListing(ShopListing listing) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(handler.getShopId());
        buf.writeUuid(listing.getId());
        ClientPlayNetworking.send(NotchPackets.SHOP_PURCHASE, buf);
    }

    private void withdrawBalance() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(handler.getShopId());
        ClientPlayNetworking.send(NotchPackets.SHOP_WITHDRAW, buf);
    }

    @Override
    public void close() {
        // Save all listings when closing manage mode
        if (mode == PlayerShopScreenHandler.Mode.MANAGE) {
            saveListings();
        }
        super.close();
    }

    private void saveListings() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(handler.getShopId());

        // Count valid rows
        int rowCount = 0;
        for (int row = 0; row < 6; row++) {
            int saleIndex = row * 3 + 1;
            if (saleIndex < handler.slots.size()) {
                ItemStack saleSlot = handler.slots.get(saleIndex).getStack();
                if (!saleSlot.isEmpty()) {
                    rowCount++;
                }
            }
        }

        buf.writeVarInt(rowCount);

        // Write each row's data
        for (int row = 0; row < 6; row++) {
            int barterIndex = row * 3;
            int saleIndex = row * 3 + 1;
            int stockIndex = row * 3 + 2;

            if (saleIndex >= handler.slots.size()) continue;

            ItemStack barterSlot = handler.slots.get(barterIndex).getStack();
            ItemStack saleSlot = handler.slots.get(saleIndex).getStack();
            ItemStack stockSlot = handler.slots.get(stockIndex).getStack();

            if (saleSlot.isEmpty()) continue;

            // Get price from text field
            int coinPrice = 100;
            if (row < priceFields.size()) {
                try {
                    coinPrice = Integer.parseInt(priceFields.get(row).getText());
                } catch (NumberFormatException ignored) {}
            }

            // Stock is ONLY from stock slot - sale slot just shows what item type
            int totalStock = stockSlot.isEmpty() ? 0 : stockSlot.getCount();

            buf.writeVarInt(row);
            buf.writeVarInt(coinPrice);
            buf.writeItemStack(barterSlot.isEmpty() ? ItemStack.EMPTY : barterSlot);
            buf.writeVarInt(barterSlot.isEmpty() ? 0 : barterSlot.getCount());
            buf.writeItemStack(saleSlot);
            buf.writeVarInt(totalStock);
        }

        ClientPlayNetworking.send(NotchPackets.SHOP_SAVE_LISTINGS, buf);
    }
}