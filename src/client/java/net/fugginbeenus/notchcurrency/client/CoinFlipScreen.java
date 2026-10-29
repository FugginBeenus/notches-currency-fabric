package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.compat.NetClient;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.gambling.CoinFlipScreenHandler;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class CoinFlipScreen extends AbstractContainerScreen<CoinFlipScreenHandler> {

    private static final int W = 200, H = 204;
    private static final int COIN_CY = 58, COIN_HALF = 24; // 16px coin drawn at 3x = 48px
    private static final int BAL_Y = 14;
    private static final int HEADS_X = 20, TAILS_X = 104, SIDE_Y = 90, SIDE_W = 76, SIDE_H = 18;
    private static final int FIELD_X = 46, FIELD_Y = 142, FIELD_W = 140, FIELD_H = 14;
    private static final int FLIP_X = 14, FLIP_Y = 160, FLIP_W = 172, FLIP_H = 18;

    private EditBox betField;
    private boolean selectedHeads = true;

    private String errorMsg;
    private long errorUntilMs;

    public CoinFlipScreen(CoinFlipScreenHandler handler, Inventory inv, Component title) {
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
        betField = new EditBox(this.font, this.leftPos + FIELD_X + 2, this.topPos + FIELD_Y + 3,
                FIELD_W - 4, FIELD_H - 5, Component.literal("Bet"));
        betField.setMaxLength(12);
        betField.setBordered(false);
        net.fugginbeenus.notchcurrency.compat.Render.setFilter(betField, s -> s.isEmpty() || s.chars().allMatch(Character::isDigit));
        int min = menu.prop(CoinFlipScreenHandler.P_MIN);
        betField.setValue(Integer.toString(Math.max(1, min)));
        addRenderableWidget(betField);
    }

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = this.leftPos, y = this.topPos;
        final long now = System.currentTimeMillis();
        drawTable(ctx, x, y);

        long bal = menu.prop(CoinFlipScreenHandler.P_BAL) & 0xFFFFFFFFL;
        String balText = Long.toString(bal);
        int balW = 18 + this.font.width(balText) - 1 + 4;
        NotchWidgets.coinField(ctx, this.font, x + W / 2 - balW / 2, y + BAL_Y, balText, NotchWidgets.COIN_GOLD);

        int ccx = x + W / 2, ccy = y + COIN_CY;
        for (int j = -2; j <= 2; j++) {
            int hw = (int) Math.round(17 * Math.sqrt(1 - (j * j) / 9.0));
            ctx.fill(ccx - hw, ccy + 27 + j, ccx + hw + 1, ccy + 28 + j, 0x801A5432);
        }
        drawGlow(ctx, ccx, ccy, now);
        drawSparkles(ctx, ccx, ccy, now);
        net.fugginbeenus.notchcurrency.compat.Render.pushGui(ctx);
        net.fugginbeenus.notchcurrency.compat.Render.translateGui(ctx, ccx - COIN_HALF, ccy - COIN_HALF);
        net.fugginbeenus.notchcurrency.compat.Render.scaleGui(ctx, 3f, 3f);
        ctx.renderItem(new ItemStack(selectedHeads ? ModItems.NOTCH_COIN : ModItems.COIN_TAILS), 0, 0);
        net.fugginbeenus.notchcurrency.compat.Render.popGui(ctx);

        boolean hHover = over(mouseX, mouseY, x + HEADS_X, y + SIDE_Y, SIDE_W, SIDE_H);
        boolean tHover = over(mouseX, mouseY, x + TAILS_X, y + SIDE_Y, SIDE_W, SIDE_H);
        NotchWidgets.tab(ctx, this.font, x + HEADS_X, y + SIDE_Y, SIDE_W, SIDE_H, "Heads", selectedHeads, hHover);
        NotchWidgets.tab(ctx, this.font, x + TAILS_X, y + SIDE_Y, SIDE_W, SIDE_H, "Tails", !selectedHeads, tHover);

        int payout = menu.prop(CoinFlipScreenHandler.P_PAYOUT);
        long bet = betValue();
        if (errorMsg != null && now < errorUntilMs) {
            NotchWidgets.centerText(ctx, this.font, errorMsg, x + W / 2, y + 116, NotchTheme.TEXT_RED, true);
        } else if (bet > 0) {
            long net = Math.round(bet * (payout / 100.0)) - bet;
            NotchWidgets.centerText(ctx, this.font, "Call it right: +" + net + " " + NotchWidgets.coinName(),
                    x + W / 2, y + 114, 0xFF9CF08A, true);
            NotchWidgets.centerText(ctx, this.font, "Call it wrong: -" + bet + " " + NotchWidgets.coinName(),
                    x + W / 2, y + 124, 0xFFFF9A8E, true);
        } else {
            NotchWidgets.centerText(ctx, this.font, "Enter a bet below.",
                    x + W / 2, y + 118, 0xFFB8D8C0, false);
        }

        ctx.fill(x + 8, y + 136, x + W - 8, y + 137, NotchTheme.FELT_LO);
        ctx.fill(x + 8, y + 137, x + W - 8, y + 138, NotchTheme.FELT_HI);

        ctx.drawString(this.font, "Bet:", x + 16, y + FIELD_Y + 3, NotchTheme.CREAM, false);
        NotchWidgets.roundBox(ctx, x + FIELD_X, y + FIELD_Y, FIELD_W, FIELD_H, NotchTheme.OUTLINE);
        ctx.fill(x + FIELD_X + 1, y + FIELD_Y + 1, x + FIELD_X + FIELD_W - 1, y + FIELD_Y + FIELD_H - 1, 0xFF123B23);
        ctx.fill(x + FIELD_X + 1, y + FIELD_Y + 1, x + FIELD_X + FIELD_W - 1, y + FIELD_Y + 2, 0xFF0B2615);
        ctx.fill(x + FIELD_X + 1, y + FIELD_Y + FIELD_H - 2, x + FIELD_X + FIELD_W - 1, y + FIELD_Y + FIELD_H - 1, NotchTheme.FELT_LO);

        boolean flipHover = over(mouseX, mouseY, x + FLIP_X, y + FLIP_Y, FLIP_W, FLIP_H);
        NotchWidgets.primaryButton(ctx, this.font, x + FLIP_X, y + FLIP_Y, FLIP_W, FLIP_H, "FLIP", flipHover);

        NotchWidgets.centerText(ctx, this.font, "A true 50/50. Win pays " + payout + "%.",
                x + W / 2, y + 184, 0xFFB8D8C0, false);
        NotchWidgets.title(ctx, this.font, "Coin Flip", x + W / 2, y + 8, NotchWidgets.Plate.MARQUEE);
        //? if >=26.1 {
        /*
        super.extractContents(ctx, mouseX, mouseY, delta);
        *///?}
    }

    private void drawTable(GuiGraphics ctx, int x, int y) {
        NotchWidgets.roundBox(ctx, x, y, W, H, NotchTheme.OUTLINE);
        ctx.fill(x + 1, y + 1, x + W - 1, y + H - 1, NotchTheme.TWOOD);
        ctx.fill(x + 1, y + 1, x + W - 1, y + 2, NotchTheme.TWOOD_HI);
        ctx.fill(x + 1, y + 1, x + 2, y + H - 1, NotchTheme.TWOOD_HI);
        ctx.fill(x + 1, y + H - 2, x + W - 1, y + H - 1, NotchTheme.TWOOD_LO);
        ctx.fill(x + W - 2, y + 2, x + W - 1, y + H - 1, NotchTheme.TWOOD_LO);
        for (int gx = x + 10; gx < x + W - 14; gx += 27) {
            ctx.fill(gx, y + 3, gx + 10, y + 4, 0xFF73492A);
            ctx.fill(gx + 6, y + H - 4, gx + 16, y + H - 3, 0xFF73492A);
        }
        NotchWidgets.outline(ctx, x + 5, y + 5, W - 10, H - 10, NotchTheme.TWOOD_LO);
        NotchWidgets.outline(ctx, x + 6, y + 6, W - 12, H - 12, NotchTheme.BLOCK_GOLD);
        ctx.fill(x + 7, y + 7, x + W - 7, y + H - 7, NotchTheme.FELT);
        ctx.fill(x + 7, y + 7, x + W - 7, y + 8, NotchTheme.FELT_LO);
        ctx.fill(x + 7, y + 7, x + 8, y + H - 7, NotchTheme.FELT_LO);
        pip(ctx, x + 9, y + 9);
        pip(ctx, x + W - 13, y + 9);
        pip(ctx, x + 9, y + H - 13);
        pip(ctx, x + W - 13, y + H - 13);
    }

    private static void pip(GuiGraphics ctx, int x, int y) {
        ctx.fill(x + 1, y, x + 3, y + 4, NotchTheme.BLOCK_GOLD);
        ctx.fill(x, y + 1, x + 4, y + 3, NotchTheme.BLOCK_GOLD);
        ctx.fill(x + 1, y, x + 2, y + 1, NotchTheme.BLOCK_GOLD_HI);
        ctx.fill(x, y + 1, x + 1, y + 2, NotchTheme.BLOCK_GOLD_HI);
        ctx.fill(x + 2, y + 3, x + 3, y + 4, NotchTheme.BLOCK_GOLD_LO);
        ctx.fill(x + 3, y + 2, x + 4, y + 3, NotchTheme.BLOCK_GOLD_LO);
    }

    private void drawGlow(GuiGraphics ctx, int cx, int cy, long now) {
        double s = 0.5 + 0.5 * Math.sin(now / 300.0);
        int gr = COIN_HALF + 3 + (int) (4 * s);
        int a = 0x22 + (int) (0x30 * s);
        fillCircle(ctx, cx, cy, gr, (a << 24) | 0xF3B93F);
    }

    private void fillCircle(GuiGraphics ctx, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int w = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            ctx.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, color);
        }
    }

    private void drawSparkles(GuiGraphics ctx, int cx, int cy, long now) {
        int d = COIN_HALF + 6;
        int[][] pts = {{-d, -d + 3}, {d, -d + 6}, {-d + 4, d - 3}, {d - 3, d - 1}};
        for (int i = 0; i < pts.length; i++) {
            boolean on = ((now / 260) + i) % 3 == 0;
            int px = cx + pts[i][0], py = cy + pts[i][1];
            int c = on ? 0xFFFFF3B0 : 0x55FFF3B0;
            ctx.fill(px - 1, py, px + 2, py + 1, c);
            ctx.fill(px, py - 1, px + 1, py + 2, c);
        }
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    private void setError(String msg) {
        errorMsg = msg;
        errorUntilMs = System.currentTimeMillis() + 2500L;
    }

    private void playErr() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BASS.value(), 0.6f));
    }

    @Override
    protected void renderLabels(GuiGraphics ctx, int mouseX, int mouseY) {
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
            int mx = (int) mouseX, my = (int) mouseY;
            if (over(mx, my, this.leftPos + HEADS_X, this.topPos + SIDE_Y, SIDE_W, SIDE_H)) { NotchWidgets.tick(); selectedHeads = true; return true; }
            if (over(mx, my, this.leftPos + TAILS_X, this.topPos + SIDE_Y, SIDE_W, SIDE_H)) { NotchWidgets.tick(); selectedHeads = false; return true; }
            if (over(mx, my, this.leftPos + FLIP_X, this.topPos + FLIP_Y, FLIP_W, FLIP_H)) {
                NotchWidgets.click();
                if (menu.prop(CoinFlipScreenHandler.P_ENABLED) == 0) { setError("Gambling is disabled."); playErr(); return true; }
                long bet = betValue();
                long bal = menu.prop(CoinFlipScreenHandler.P_BAL) & 0xFFFFFFFFL;
                int min = menu.prop(CoinFlipScreenHandler.P_MIN);
                int max = menu.prop(CoinFlipScreenHandler.P_MAX);
                if (bet < min || bet > max) { setError("Bet must be " + min + "-" + max + " " + NotchWidgets.coinName() + "."); playErr(); return true; }
                if (bet > bal) { setError("Not enough " + NotchWidgets.coinName() + " for that bet."); playErr(); return true; }
                FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
                buf.writeBoolean(selectedHeads);
                buf.writeVarLong(bet);
                NetClient.sendToServer(NotchPackets.COINFLIP_FLIP, buf);
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private long betValue() {
        try {
            return Long.parseLong(betField.getValue().trim());
        } catch (NumberFormatException e) {
            return 0L;
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
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, betField)) return true;
        //? if >=1.21.11 {
        /*return super.keyPressed(event);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
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
