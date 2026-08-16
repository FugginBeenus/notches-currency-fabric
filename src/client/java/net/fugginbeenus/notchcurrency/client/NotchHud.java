package net.fugginbeenus.notchcurrency.client;

import net.minecraft.network.chat.Component;
//? if >=1.21.11 {
/*import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
*///?} else {
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
//?}
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

//? if >=1.21.11 {
/*public final class NotchHud implements HudElement {
*///?} else {
public final class NotchHud implements HudRenderCallback {
//?}

    private static long BALANCE = 0;
    private static final int X_NUDGE = 0;
    private static final int Y_NUDGE = -27;
    private static final int GAP = 4;
    private static final int ICON_PX = 9;
    private static final boolean USE_ITEM_ICON = false;
    private static final ResourceLocation COIN_HUD_TEX =
            NotchCurrency.id("textures/item/coin.png");
    private static long chatClashUntil = 0;
    public static void noteChatMessage(net.minecraft.network.chat.Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        double chatScale = mc.options.chatScale().get();
        int lineEnd = 4 + (int) Math.ceil(mc.font.width(message) * chatScale);
        int anchorRight = mc.getWindow().getGuiScaledWidth() / 2 + 91 + X_NUDGE;
        int balanceW = mc.font.width(String.valueOf(BALANCE));
        int hudLeft = anchorRight - balanceW - GAP - (USE_ITEM_ICON ? 16 : ICON_PX);
        if (lineEnd >= hudLeft) {
            chatClashUntil = System.currentTimeMillis() + 10_000;
        }
    }

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
        final int iconSize = USE_ITEM_ICON ? 16 : ICON_PX;
        final int iconX = textX - GAP - iconSize;
        final int iconY = rowY + (9 - iconSize) / 2;

        if (USE_ITEM_ICON) {
            ItemStack stack = new ItemStack(ModItems.NOTCH_COIN);
            ctx.renderItem(stack, iconX, iconY);
            ctx.renderItemDecorations(mc.font, stack, iconX, iconY);
        } else {
            //? if >=1.21.11 {
            /*
            ctx.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, COIN_HUD_TEX,
                    iconX, iconY, 0f, 0f, ICON_PX, ICON_PX, ICON_PX, ICON_PX);
            *///?} else {
            ctx.blit(COIN_HUD_TEX, iconX, iconY,
                    0, 0, ICON_PX, ICON_PX, ICON_PX, ICON_PX);
            //?}
        }

        ctx.drawString(mc.font, s, textX, textY, 0xFFFFFFFF, true);
    }

    private static boolean shouldHide(Minecraft mc) {
        //? if <26.1 {
        if (mc.options.hideGui) return true;
        //?}

        //? if >=1.21 {
        /*if (System.currentTimeMillis() < chatClashUntil) return true;
        *///?}

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