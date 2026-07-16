package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.compat.NetClient;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.raffle.RaffleAdminScreenHandler;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Op-only raffle setup GUI (opened via {@code /raffle admin}). Inventory is shown so the admin
 * can drag a template item into the "new prize" slot; the read-only slot shows what's currently
 * configured. Fields cover the coins pool, ticket price, house cut and draw interval (in IRL
 * days); Save &amp; Apply persists, Draw Now / Reset act immediately.
 */
public class RaffleAdminScreen extends HandledScreen<RaffleAdminScreenHandler> {

    private static final int W = 200, H = 250;

    private static final int FIELD_X = 108, FIELD_W = 80, FIELD_H = 14;
    private static final int[] FIELD_Y = {42, 58, 74, 90};
    private static final String[] FIELD_LABELS = {"Prize coins:", "Ticket price:", "House cut %:", "Draw (days):"};

    private static final int CLR_X = 68, CLR_Y = 22, CLR_W = 80, CLR_H = 16;
    private static final int TG_X = 108, TG_Y = 106, TG_W = 80, TG_H = 14;
    private static final int SV_X = 12, SV_Y = 124, SV_W = 176, SV_H = 16;
    private static final int DR_X = 12, DR_Y = 144, DR_W = 86, DR_H = 16;
    private static final int RS_X = 102, RS_Y = 144, RS_W = 86, RS_H = 16;

    private TextFieldWidget coinsField, priceField, cutField, daysField;
    private boolean enabledToggle = false;
    private boolean prefilled = false;

    public RaffleAdminScreen(RaffleAdminScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = W;
        this.backgroundHeight = H;
        this.titleX = -1000;
        this.playerInventoryTitleX = -1000;
    }

    @Override
    protected void init() {
        super.init();
        coinsField = digitField(FIELD_Y[0]);
        priceField = digitField(FIELD_Y[1]);
        cutField = digitField(FIELD_Y[2]);
        daysField = digitField(FIELD_Y[3]);
        addDrawableChild(coinsField);
        addDrawableChild(priceField);
        addDrawableChild(cutField);
        addDrawableChild(daysField);
    }

    private TextFieldWidget digitField(int fy) {
        TextFieldWidget f = new TextFieldWidget(this.textRenderer, this.x + FIELD_X + 2, this.y + fy + 3,
                FIELD_W - 4, FIELD_H - 5, Text.empty());
        f.setMaxLength(12);
        f.setDrawsBackground(false);
        f.setTextPredicate(s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        return f;
    }

    private void prefillIfReady() {
        if (prefilled) return;
        if (handler.prop(RaffleAdminScreenHandler.A_PRICE) <= 0) return;
        coinsField.setText(String.valueOf(handler.prop(RaffleAdminScreenHandler.A_COINS)));
        priceField.setText(String.valueOf(handler.prop(RaffleAdminScreenHandler.A_PRICE)));
        cutField.setText(String.valueOf(handler.prop(RaffleAdminScreenHandler.A_CUT)));
        daysField.setText(String.valueOf(handler.prop(RaffleAdminScreenHandler.A_INTERVAL) / 1440)); // min → days
        enabledToggle = handler.prop(RaffleAdminScreenHandler.A_ENABLED) == 1;
        prefilled = true;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        prefillIfReady();
        final int x = this.x, y = this.y;
        NotchWidgets.panel(ctx, x, y, W, H);

        NotchWidgets.title(ctx, this.textRenderer, "Raffle Setup", x + W / 2, y + 8);

        // Prize: read-only current (left) → interactive new (right) → Clear.
        ctx.drawText(this.textRenderer, "Prize:", x + 12, y + 12, NotchTheme.TEXT_DARK, false);
        NotchWidgets.slot(ctx, x + RaffleAdminScreenHandler.CUR_X - 1, y + RaffleAdminScreenHandler.CUR_Y - 1);
        NotchWidgets.slot(ctx, x + RaffleAdminScreenHandler.INPUT_X - 1, y + RaffleAdminScreenHandler.INPUT_Y - 1);
        ctx.drawText(this.textRenderer, ">", x + 34, y + 26, NotchTheme.TEXT_DARK, false);
        NotchWidgets.dangerButton(ctx, this.textRenderer, x + CLR_X, y + CLR_Y, CLR_W, CLR_H, "Clear",
                over(mouseX, mouseY, x + CLR_X, y + CLR_Y, CLR_W, CLR_H));

        // Param fields.
        for (int i = 0; i < FIELD_Y.length; i++) {
            ctx.drawText(this.textRenderer, FIELD_LABELS[i], x + 12, y + FIELD_Y[i] + 3, NotchTheme.TEXT_DARK, false);
            NotchWidgets.inset(ctx, x + FIELD_X, y + FIELD_Y[i], FIELD_W, FIELD_H, NotchTheme.DEEP);
        }

        // Active toggle (ON green / OFF grey) — master on/off for the raffle.
        ctx.drawText(this.textRenderer, "Active:", x + 12, y + TG_Y + 3, NotchTheme.TEXT_DARK, false);
        if (enabledToggle) {
            NotchWidgets.primaryButton(ctx, this.textRenderer, x + TG_X, y + TG_Y, TG_W, TG_H, "ON",
                    over(mouseX, mouseY, x + TG_X, y + TG_Y, TG_W, TG_H));
        } else {
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + TG_X, y + TG_Y, TG_W, TG_H, "OFF",
                    over(mouseX, mouseY, x + TG_X, y + TG_Y, TG_W, TG_H));
        }

        // Actions.
        NotchWidgets.primaryButton(ctx, this.textRenderer, x + SV_X, y + SV_Y, SV_W, SV_H, "Save & Apply",
                over(mouseX, mouseY, x + SV_X, y + SV_Y, SV_W, SV_H));
        NotchWidgets.neutralButton(ctx, this.textRenderer, x + DR_X, y + DR_Y, DR_W, DR_H, "Draw Now",
                over(mouseX, mouseY, x + DR_X, y + DR_Y, DR_W, DR_H));
        NotchWidgets.dangerButton(ctx, this.textRenderer, x + RS_X, y + RS_Y, RS_W, RS_H, "Reset Round",
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
        return mx >= this.x + sx && mx < this.x + sx + 16 && my >= this.y + sy && my < this.y + sy + 16;
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        // No default labels.
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        // Explain the two prize boxes on hover (when empty; item tooltips show otherwise).
        if (overItem(mouseX, mouseY, RaffleAdminScreenHandler.CUR_X, RaffleAdminScreenHandler.CUR_Y)
                && handler.getCurrentPrizeStack().isEmpty()) {
            ctx.drawTooltip(this.textRenderer, java.util.List.of(
                    Text.literal("Current prize").formatted(Formatting.WHITE),
                    Text.literal("None set (coins-only)").formatted(Formatting.GRAY)), mouseX, mouseY);
        } else if (overItem(mouseX, mouseY, RaffleAdminScreenHandler.INPUT_X, RaffleAdminScreenHandler.INPUT_Y)
                && handler.getSlot(1).getStack().isEmpty()) {
            ctx.drawTooltip(this.textRenderer, java.util.List.of(
                    Text.literal("New prize").formatted(Formatting.WHITE),
                    Text.literal("Drop an item here, then").formatted(Formatting.GRAY),
                    Text.literal("Save & Apply, to set it.").formatted(Formatting.GRAY)), mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && this.client != null && this.client.interactionManager != null) {
            int mx = (int) mouseX, my = (int) mouseY;
            if (over(mx, my, this.x + CLR_X, this.y + CLR_Y, CLR_W, CLR_H)) { NotchWidgets.click(); click(0); return true; }
            if (over(mx, my, this.x + DR_X, this.y + DR_Y, DR_W, DR_H)) { NotchWidgets.click(); click(1); return true; }
            if (over(mx, my, this.x + RS_X, this.y + RS_Y, RS_W, RS_H)) { NotchWidgets.click(); click(2); return true; }
            if (over(mx, my, this.x + TG_X, this.y + TG_Y, TG_W, TG_H)) { NotchWidgets.tick(); enabledToggle = !enabledToggle; return true; }
            if (over(mx, my, this.x + SV_X, this.y + SV_Y, SV_W, SV_H)) { NotchWidgets.click(); save(); return true; }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void click(int id) {
        this.client.interactionManager.clickButton(this.handler.syncId, id);
    }

    private void save() {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeVarLong(parse(priceField.getText(), 1));
        buf.writeVarInt((int) parse(cutField.getText(), 0));
        buf.writeVarInt((int) parse(daysField.getText(), 0));
        buf.writeBoolean(enabledToggle);
        buf.writeVarLong(parse(coinsField.getText(), 0));
        NetClient.sendToServer(NotchPackets.RAFFLE_ADMIN_SAVE, buf);
    }

    private long parse(String s, long fallback) {
        try {
            return Long.parseLong(s.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, coinsField, priceField, cutField, daysField)) return true;
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    //? if >=1.21 {
    /*@Override
    protected void applyBlur(float delta) {
        // No 1.21 menu blur behind the mod's screens — they draw crisp panels over the world.
    }
    *///?}
}
