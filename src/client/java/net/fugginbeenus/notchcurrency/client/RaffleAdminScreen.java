package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.compat.NetClient;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.raffle.RaffleAdminScreenHandler;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class RaffleAdminScreen extends AbstractContainerScreen<RaffleAdminScreenHandler> {

    private static final int W = 200, H = 250;

    private static final int FIELD_X = 108, FIELD_W = 80, FIELD_H = 14;
    private static final int[] FIELD_Y = {42, 58, 74, 90};
    private static final String[] FIELD_LABELS = {"Prize coins:", "Ticket price:", "House cut %:", "Draw (days):"};

    private static final int CLR_X = 68, CLR_Y = 22, CLR_W = 80, CLR_H = 16;
    private static final int TG_X = 108, TG_Y = 106, TG_W = 80, TG_H = 14;
    private static final int SV_X = 12, SV_Y = 124, SV_W = 176, SV_H = 16;
    private static final int DR_X = 12, DR_Y = 144, DR_W = 86, DR_H = 16;
    private static final int RS_X = 102, RS_Y = 144, RS_W = 86, RS_H = 16;

    private EditBox coinsField, priceField, cutField, daysField;
    private boolean enabledToggle = false;
    private boolean prefilled = false;

    public RaffleAdminScreen(RaffleAdminScreenHandler handler, Inventory inv, Component title) {
        super(handler, inv, title);
        this.imageWidth = W;
        this.imageHeight = H;
        this.titleLabelX = -1000;
        this.inventoryLabelX = -1000;
    }

    @Override
    protected void init() {
        super.init();
        coinsField = digitField(FIELD_Y[0]);
        priceField = digitField(FIELD_Y[1]);
        cutField = digitField(FIELD_Y[2]);
        daysField = digitField(FIELD_Y[3]);
        addRenderableWidget(coinsField);
        addRenderableWidget(priceField);
        addRenderableWidget(cutField);
        addRenderableWidget(daysField);
    }

    private EditBox digitField(int fy) {
        EditBox f = new EditBox(this.font, this.leftPos + FIELD_X + 2, this.topPos + fy + 3,
                FIELD_W - 4, FIELD_H - 5, Component.empty());
        f.setMaxLength(12);
        f.setBordered(false);
        f.setFilter(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        return f;
    }

    private void prefillIfReady() {
        if (prefilled) return;
        if (menu.prop(RaffleAdminScreenHandler.A_PRICE) <= 0) return;
        coinsField.setValue(String.valueOf(menu.prop(RaffleAdminScreenHandler.A_COINS)));
        priceField.setValue(String.valueOf(menu.prop(RaffleAdminScreenHandler.A_PRICE)));
        cutField.setValue(String.valueOf(menu.prop(RaffleAdminScreenHandler.A_CUT)));
        daysField.setValue(String.valueOf(menu.prop(RaffleAdminScreenHandler.A_INTERVAL) / 1440)); // min → days
        enabledToggle = menu.prop(RaffleAdminScreenHandler.A_ENABLED) == 1;
        prefilled = true;
    }

    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
        prefillIfReady();
        final int x = this.leftPos, y = this.topPos;
        NotchWidgets.panel(ctx, x, y, W, H);

        NotchWidgets.title(ctx, this.font, "Raffle Setup", x + W / 2, y + 8);

        // Prize: read-only current (left) → interactive new (right) → Clear.
        ctx.drawString(this.font, "Prize:", x + 12, y + 12, NotchTheme.TEXT_DARK, false);
        NotchWidgets.slot(ctx, x + RaffleAdminScreenHandler.CUR_X - 1, y + RaffleAdminScreenHandler.CUR_Y - 1);
        NotchWidgets.slot(ctx, x + RaffleAdminScreenHandler.INPUT_X - 1, y + RaffleAdminScreenHandler.INPUT_Y - 1);
        ctx.drawString(this.font, ">", x + 34, y + 26, NotchTheme.TEXT_DARK, false);
        NotchWidgets.dangerButton(ctx, this.font, x + CLR_X, y + CLR_Y, CLR_W, CLR_H, "Clear",
                over(mouseX, mouseY, x + CLR_X, y + CLR_Y, CLR_W, CLR_H));

        // Param fields.
        for (int i = 0; i < FIELD_Y.length; i++) {
            ctx.drawString(this.font, FIELD_LABELS[i], x + 12, y + FIELD_Y[i] + 3, NotchTheme.TEXT_DARK, false);
            NotchWidgets.inset(ctx, x + FIELD_X, y + FIELD_Y[i], FIELD_W, FIELD_H, NotchTheme.DEEP);
        }

        // Active toggle (ON green / OFF grey): master on/off for the raffle.
        ctx.drawString(this.font, "Active:", x + 12, y + TG_Y + 3, NotchTheme.TEXT_DARK, false);
        if (enabledToggle) {
            NotchWidgets.primaryButton(ctx, this.font, x + TG_X, y + TG_Y, TG_W, TG_H, "ON",
                    over(mouseX, mouseY, x + TG_X, y + TG_Y, TG_W, TG_H));
        } else {
            NotchWidgets.neutralButton(ctx, this.font, x + TG_X, y + TG_Y, TG_W, TG_H, "OFF",
                    over(mouseX, mouseY, x + TG_X, y + TG_Y, TG_W, TG_H));
        }

        // Actions.
        NotchWidgets.primaryButton(ctx, this.font, x + SV_X, y + SV_Y, SV_W, SV_H, "Save & Apply",
                over(mouseX, mouseY, x + SV_X, y + SV_Y, SV_W, SV_H));
        NotchWidgets.neutralButton(ctx, this.font, x + DR_X, y + DR_Y, DR_W, DR_H, "Draw Now",
                over(mouseX, mouseY, x + DR_X, y + DR_Y, DR_W, DR_H));
        NotchWidgets.dangerButton(ctx, this.font, x + RS_X, y + RS_Y, RS_W, RS_H, "Reset Round",
                over(mouseX, mouseY, x + RS_X, y + RS_Y, RS_W, RS_H));

        NotchWidgets.divider(ctx, x + 8, y + 164, W - 16);

        // Player inventory + hotbar frames.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                NotchWidgets.slot(ctx, x + RaffleAdminScreenHandler.INV_X + col * 18 - 1,
                        y + RaffleAdminScreenHandler.INV_Y + row * 18 - 1);
            }
        }
        for (int col = 0; col < 9; col++) {
            NotchWidgets.slot(ctx, x + RaffleAdminScreenHandler.INV_X + col * 18 - 1,
                    y + RaffleAdminScreenHandler.HOTBAR_Y - 1);
        }
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    private boolean overItem(int mx, int my, int sx, int sy) {
        return mx >= this.leftPos + sx && mx < this.leftPos + sx + 16 && my >= this.topPos + sy && my < this.topPos + sy + 16;
    }

    @Override
    protected void renderLabels(GuiGraphics ctx, int mouseX, int mouseY) {
        // No default labels.
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        // Explain the two prize boxes on hover (when empty; item tooltips show otherwise).
        if (overItem(mouseX, mouseY, RaffleAdminScreenHandler.CUR_X, RaffleAdminScreenHandler.CUR_Y)
                && menu.getCurrentPrizeStack().isEmpty()) {
            ctx.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("Current prize").withStyle(ChatFormatting.WHITE),
                    Component.literal("None set (coins-only)").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        } else if (overItem(mouseX, mouseY, RaffleAdminScreenHandler.INPUT_X, RaffleAdminScreenHandler.INPUT_Y)
                && menu.getSlot(1).getItem().isEmpty()) {
            ctx.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("New prize").withStyle(ChatFormatting.WHITE),
                    Component.literal("Drop an item here, then").withStyle(ChatFormatting.GRAY),
                    Component.literal("Save & Apply, to set it.").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }
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
        if (button == 0 && this.minecraft != null && this.minecraft.gameMode != null) {
            int mx = (int) mouseX, my = (int) mouseY;
            if (over(mx, my, this.leftPos + CLR_X, this.topPos + CLR_Y, CLR_W, CLR_H)) { NotchWidgets.click(); click(0); return true; }
            if (over(mx, my, this.leftPos + DR_X, this.topPos + DR_Y, DR_W, DR_H)) { NotchWidgets.click(); click(1); return true; }
            if (over(mx, my, this.leftPos + RS_X, this.topPos + RS_Y, RS_W, RS_H)) { NotchWidgets.click(); click(2); return true; }
            if (over(mx, my, this.leftPos + TG_X, this.topPos + TG_Y, TG_W, TG_H)) { NotchWidgets.tick(); enabledToggle = !enabledToggle; return true; }
            if (over(mx, my, this.leftPos + SV_X, this.topPos + SV_Y, SV_W, SV_H)) { NotchWidgets.click(); save(); return true; }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private void click(int id) {
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
    }

    private void save() {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeVarLong(parse(priceField.getValue(), 1));
        buf.writeVarInt((int) parse(cutField.getValue(), 0));
        buf.writeVarInt((int) parse(daysField.getValue(), 0));
        buf.writeBoolean(enabledToggle);
        buf.writeVarLong(parse(coinsField.getValue(), 0));
        NetClient.sendToServer(NotchPackets.RAFFLE_ADMIN_SAVE, buf);
    }

    private long parse(String s, long fallback) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
    *///?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, coinsField, priceField, cutField, daysField)) return true;
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
