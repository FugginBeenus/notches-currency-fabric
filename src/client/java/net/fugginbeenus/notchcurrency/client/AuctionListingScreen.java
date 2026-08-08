package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.compat.NetClient;
import net.fugginbeenus.notchcurrency.auction.AuctionListingScreenHandler;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AuctionListingScreen extends AbstractContainerScreen<AuctionListingScreenHandler> {

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
    private EditBox priceField;

    public AuctionListingScreen(AuctionListingScreenHandler handler, Inventory inv, Component title) {
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
        priceField = new EditBox(this.font, this.leftPos + PRICE_X + 2, this.topPos + PRICE_Y + 4,
                PRICE_W - 4, PRICE_H - 6, Component.literal("Price"));
        priceField.setMaxLength(12);
        priceField.setBordered(false);
        priceField.setHint(Component.literal("Type a price…").withStyle(ChatFormatting.DARK_GRAY));
        priceField.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        addRenderableWidget(priceField);
        setInitialFocus(priceField);
    }

    //? if >=26.1 {
    /*@Override
    protected void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = this.leftPos, y = this.topPos;
        NotchWidgets.panel(ctx, x, y, W, H);

        NotchWidgets.title(ctx, this.font, "List an Item", x + W / 2, y + 8);

        // Input slot + label.
        ctx.drawString(this.font, "Item to sell:", x + 16, y + AuctionListingScreenHandler.INPUT_Y + 4,
                NotchTheme.TEXT_DARK, false);
        NotchWidgets.slot(ctx, x + AuctionListingScreenHandler.INPUT_X - 1, y + AuctionListingScreenHandler.INPUT_Y - 1);

        // Price box (TextFieldWidget renders its text on top).
        ctx.drawString(this.font, "Price (" + NotchWidgets.coinName() + "):", x + 8, y + 44, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + PRICE_X, y + PRICE_Y, PRICE_W, PRICE_H, NotchTheme.DEEP);

        // Auction-duration segmented selector (selected = green, others = grey).
        ctx.drawString(this.font, "Auction Duration", x + 8, y + 76, NotchTheme.TEXT_DARK, false);
        for (int i = 0; i < DURATIONS.length; i++) {
            boolean hov = over(mouseX, mouseY, x + SEG_X[i], y + SEG_Y, SEG_W[i], SEG_H);
            if (i == durationIndex) {
                NotchWidgets.primaryButton(ctx, this.font, x + SEG_X[i], y + SEG_Y, SEG_W[i], SEG_H, SEG_LABELS[i], hov);
            } else {
                NotchWidgets.neutralButton(ctx, this.font, x + SEG_X[i], y + SEG_Y, SEG_W[i], SEG_H, SEG_LABELS[i], hov);
            }
        }
        String hint = currentDays() == 0 ? "Instant sale at your price."
                : "Highest bid after " + currentDays() + " day" + (currentDays() == 1 ? "" : "s") + " wins.";
        ctx.drawString(this.font, hint, x + 8, y + 104, NotchTheme.TEXT_MUTED, false);

        // Fee note: the listing fee scales with price, so recompute it from the typed value.
        long typedPrice = 0;
        try {
            String pt = priceField.getValue();
            if (!pt.isEmpty()) typedPrice = Long.parseLong(pt);
        } catch (NumberFormatException ignored) {
        }
        long fee = menu.feeFor(typedPrice);
        if (fee > 0 || menu.feePercent() > 0 || menu.feeFlat() > 0) {
            String note = "Fee: " + fee + " " + NotchWidgets.coinName()
                    + (menu.feePercent() > 0 ? " (" + menu.feePercent() + "% + " + menu.feeFlat() + ")" : "");
            ctx.drawString(this.font, note, x + LIST_X + 4, y + LIST_Y - 10, NotchTheme.TEXT_MUTED, false);
        }

        // List It button.
        NotchWidgets.primaryButton(ctx, this.font, x + LIST_X, y + LIST_Y, LIST_W, LIST_H, "List It!",
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
    protected void renderLabels(GuiGraphics ctx, int mouseX, int mouseY) {
        // No default labels.
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
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
        if (button == 0) {
            for (int i = 0; i < DURATIONS.length; i++) {
                if (over((int) mouseX, (int) mouseY, this.leftPos + SEG_X[i], this.topPos + SEG_Y, SEG_W[i], SEG_H)) {
                    if (durationIndex != i) NotchWidgets.tick();
                    durationIndex = i;
                    return true;
                }
            }
            if (over((int) mouseX, (int) mouseY, this.leftPos + LIST_X, this.topPos + LIST_Y, LIST_W, LIST_H)) {
                NotchWidgets.click();
                submit();
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private void submit() {
        long price;
        try {
            price = Long.parseLong(priceField.getValue().trim());
        } catch (NumberFormatException e) {
            price = 0L;
        }
        if (price <= 0) {
            if (this.minecraft != null && this.minecraft.player != null) {
                net.fugginbeenus.notchcurrency.compat.Msg.chat(this.minecraft.player, Component.literal("Enter a price above 0.").withStyle(ChatFormatting.RED));
            }
            return;
        }
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeVarLong(price);
        buf.writeVarInt(currentDays());
        NetClient.sendToServer(NotchPackets.AUCTION_LIST, buf);
        priceField.setValue("");
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
    *///?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
        // Keep the screen from closing / hotbar-swapping while typing in a focused field.
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, priceField)) return true;
        //? if >=1.21.11 {
        /*return super.keyPressed(event);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
    }

    //? if >=1.21 {
    /*@Override
    protected void renderBlurredBackground(float delta) {
        // No 1.21 menu blur behind the mod's screens. They draw crisp panels over the world.
    }
    *///?}
}
