package net.fugginbeenus.notchcurrency.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fugginbeenus.notchcurrency.auction.AuctionListingScreenHandler;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * The "List an Item" screen: drop an item into the input slot, type a price, pick a sale type
 * (Buy Now or a 1/3/7-day auction via a clear segmented selector), and hit List It. Code-drawn
 * in the {@link NotchWidgets} style; the server reads the slot and creates the listing.
 */
public class AuctionListingScreen extends HandledScreen<AuctionListingScreenHandler> {

    private static final int W = 176, H = 224;

    private static final int PRICE_X = 8, PRICE_Y = 54, PRICE_W = 160, PRICE_H = 16;

    // Segmented sale-type selector.
    private static final int SEG_Y = 86, SEG_H = 15;
    private static final int[] DURATIONS = {0, 1, 3, 7};
    private static final String[] SEG_LABELS = {"Buy Now", "1d", "3d", "7d"};
    private static final int[] SEG_X = {8, 72, 104, 136};
    private static final int[] SEG_W = {60, 28, 28, 28};

    private static final int LIST_X = 8, LIST_Y = 116, LIST_W = 160, LIST_H = 18;

    private int durationIndex = 0;
    private TextFieldWidget priceField;

    public AuctionListingScreen(AuctionListingScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = W;
        this.backgroundHeight = H;
        this.titleX = -1000;
        this.playerInventoryTitleX = -1000;
    }

    @Override
    protected void init() {
        super.init();
        priceField = new TextFieldWidget(this.textRenderer, this.x + PRICE_X + 2, this.y + PRICE_Y + 4,
                PRICE_W - 4, PRICE_H - 6, Text.literal("Price"));
        priceField.setMaxLength(12);
        priceField.setDrawsBackground(false);
        priceField.setPlaceholder(Text.literal("Type a price…").formatted(Formatting.DARK_GRAY));
        priceField.setTextPredicate(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        addDrawableChild(priceField);
        setInitialFocus(priceField);
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x, y = this.y;
        NotchWidgets.panel(ctx, x, y, W, H);

        NotchWidgets.title(ctx, this.textRenderer, "List an Item", x + W / 2, y + 8);

        // Input slot + label.
        ctx.drawText(this.textRenderer, "Item to sell:", x + 16, y + AuctionListingScreenHandler.INPUT_Y + 4,
                NotchTheme.TEXT_DARK, false);
        NotchWidgets.slot(ctx, x + AuctionListingScreenHandler.INPUT_X - 1, y + AuctionListingScreenHandler.INPUT_Y - 1);

        // Price box (TextFieldWidget renders its text on top).
        ctx.drawText(this.textRenderer, "Price (coins):", x + 8, y + 44, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + PRICE_X, y + PRICE_Y, PRICE_W, PRICE_H, NotchTheme.DEEP);

        // Auction-duration segmented selector (selected = green, others = grey).
        ctx.drawText(this.textRenderer, "Auction Duration", x + 8, y + 76, NotchTheme.TEXT_DARK, false);
        for (int i = 0; i < DURATIONS.length; i++) {
            boolean hov = over(mouseX, mouseY, x + SEG_X[i], y + SEG_Y, SEG_W[i], SEG_H);
            if (i == durationIndex) {
                NotchWidgets.primaryButton(ctx, this.textRenderer, x + SEG_X[i], y + SEG_Y, SEG_W[i], SEG_H, SEG_LABELS[i], hov);
            } else {
                NotchWidgets.neutralButton(ctx, this.textRenderer, x + SEG_X[i], y + SEG_Y, SEG_W[i], SEG_H, SEG_LABELS[i], hov);
            }
        }
        String hint = currentDays() == 0 ? "Instant sale at your price."
                : "Highest bid after " + currentDays() + " day" + (currentDays() == 1 ? "" : "s") + " wins.";
        ctx.drawText(this.textRenderer, hint, x + 8, y + 104, NotchTheme.TEXT_MUTED, false);

        // Fee note — the listing fee scales with price, so recompute it from the typed value.
        long typedPrice = 0;
        try {
            String pt = priceField.getText();
            if (!pt.isEmpty()) typedPrice = Long.parseLong(pt);
        } catch (NumberFormatException ignored) {
        }
        long fee = handler.feeFor(typedPrice);
        if (fee > 0 || handler.feePercent() > 0 || handler.feeFlat() > 0) {
            String note = "Fee: " + fee + " coins"
                    + (handler.feePercent() > 0 ? " (" + handler.feePercent() + "% + " + handler.feeFlat() + ")" : "");
            ctx.drawText(this.textRenderer, note, x + LIST_X + 4, y + LIST_Y - 10, NotchTheme.TEXT_MUTED, false);
        }

        // List It button.
        NotchWidgets.primaryButton(ctx, this.textRenderer, x + LIST_X, y + LIST_Y, LIST_W, LIST_H, "List It!",
                over(mouseX, mouseY, x + LIST_X, y + LIST_Y, LIST_W, LIST_H));

        NotchWidgets.divider(ctx, x + 8, y + 136, W - 16);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                NotchWidgets.slot(ctx, x + AuctionListingScreenHandler.INV_X + col * 18 - 1,
                        y + AuctionListingScreenHandler.INV_Y + row * 18 - 1);
            }
        }
        for (int col = 0; col < 9; col++) {
            NotchWidgets.slot(ctx, x + AuctionListingScreenHandler.INV_X + col * 18 - 1,
                    y + AuctionListingScreenHandler.HOTBAR_Y - 1);
        }
    }

    private int currentDays() {
        return DURATIONS[durationIndex];
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        // No default labels.
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            for (int i = 0; i < DURATIONS.length; i++) {
                if (over((int) mouseX, (int) mouseY, this.x + SEG_X[i], this.y + SEG_Y, SEG_W[i], SEG_H)) {
                    durationIndex = i;
                    return true;
                }
            }
            if (over((int) mouseX, (int) mouseY, this.x + LIST_X, this.y + LIST_Y, LIST_W, LIST_H)) {
                submit();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void submit() {
        long price;
        try {
            price = Long.parseLong(priceField.getText().trim());
        } catch (NumberFormatException e) {
            price = 0L;
        }
        if (price <= 0) {
            if (this.client != null && this.client.player != null) {
                this.client.player.sendMessage(Text.literal("Enter a price above 0.").formatted(Formatting.RED), false);
            }
            return;
        }
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarLong(price);
        buf.writeVarInt(currentDays());
        ClientPlayNetworking.send(NotchPackets.AUCTION_LIST, buf);
        priceField.setText("");
    }
}
