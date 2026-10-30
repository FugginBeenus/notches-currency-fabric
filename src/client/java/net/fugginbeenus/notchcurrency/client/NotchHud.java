package net.fugginbeenus.notchcurrency.client;

//? if >=1.21.11 {
/*import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
*///?} else {
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
//?}
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

//? if >=1.21.11 {
/*public final class NotchHud implements HudElement {
*///?} else {
public final class NotchHud implements HudRenderCallback {
//?}

    private static long BALANCE = 0;
    private static final int X_NUDGE = 0;
    private static final int Y_NUDGE = -27;
    private static final int GAP = 4;
    private static final String COIN_GLYPH = "\uE000";

    public static void setBalance(long value) {
        BALANCE = value;
    }
    public static long getBalance() {
        return BALANCE;
    }

    @Override
    //? if >=26.1 {
    /*public void extractRenderState(GuiGraphics ctx, net.minecraft.client.DeltaTracker tickCounter) {
    *///?} elif >=1.21.11 {
    /*public void render(GuiGraphics ctx, net.minecraft.client.DeltaTracker tickCounter) {
    *///?} elif >=1.21 {
    /*public void onHudRender(GuiGraphics ctx, net.minecraft.client.DeltaTracker tickCounter) {
    *///?} else {
    public void onHudRender(GuiGraphics ctx, float tickDelta) {
    //?}
        final Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) return;
        if (shouldHide(mc)) return;

        final int sw = mc.getWindow().getGuiScaledWidth();
        final int sh = mc.getWindow().getGuiScaledHeight();
        final int hotbarW = 182, hotbarH = 22;
        final int hotbarX = (sw / 2) - (hotbarW / 2);
        final int hotbarY = sh - hotbarH - 1;
        final int anchorRight = hotbarX + hotbarW + X_NUDGE;
        final int rowY = hotbarY + Y_NUDGE;
        final String s = String.valueOf(BALANCE);
        final int textW = mc.font.width(s);
        final int textH = mc.font.lineHeight;
        final int textX = anchorRight - textW;
        final int textY = rowY + (9 - textH) / 2;
        final int iconX = textX - GAP - (mc.font.width(COIN_GLYPH) - 1);

        ctx.drawString(mc.font, COIN_GLYPH, iconX, textY, 0xFFFFFFFF, false);
        ctx.drawString(mc.font, s, textX, textY, 0xFFFFFFFF, true);
    }

    private static boolean shouldHide(Minecraft mc) {
        //? if <26.1 {
        if (mc.options.hideGui) return true;
        //?}

        var player = mc.player;
        if (player == null) return true;

        // hide under de water
        if (!player.getAbilities().instabuild
                && !player.isSpectator()
                && player.getAirSupply() < player.getMaxAirSupply()) {
            return true;
        }

        // hide on mounts
        if (player.isPassenger()) {
            return true;
        }

        return false;
    }
}