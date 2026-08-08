package net.fugginbeenus.notchcurrency.client;

import net.minecraft.network.chat.Component;
//? if >=26.1 {
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

//? if >=26.1 {
/*public final class NotchHud implements HudElement {
*///?} else {
public final class NotchHud implements HudRenderCallback {
//?}

    private static long BALANCE = 0;

    // Nudges
    private static final int X_NUDGE = 0;     // +right / -left
    private static final int Y_NUDGE = -27;   // relative to hotbar top; ~-10 sits over hunger row

    private static final int GAP = 4;         // between text and icon
    private static final int ICON_PX = 9;     // 9px HUD sprite size (for texture mode)
    private static final boolean USE_ITEM_ICON = false; // true = draw 16px item icon

    // Only used if USE_ITEM_ICON == false
    private static final ResourceLocation COIN_HUD_TEX =
            NotchCurrency.id("textures/item/coin.png");

    // On 1.21 HUD callbacks draw AFTER chat, so the balance would sit on top of long chat lines.
    // When a game message is wide enough to reach under the HUD, hide it for chat's fade-out.
    private static long chatClashUntil = 0;

    public static void noteChatMessage(net.minecraft.network.chat.Component message) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) return;
        // Where the chat line ends, in screen pixels (chat draws at x=4, scaled by the chat option).
        double chatScale = mc.options.chatScale().get();
        int lineEnd = 4 + (int) Math.ceil(mc.font.width(message) * chatScale);
        // The HUD's leftmost pixel: the icon, left of the right-aligned balance text.
        int anchorRight = mc.getWindow().getGuiScaledWidth() / 2 + 91 + X_NUDGE;
        int balanceW = mc.font.width(String.valueOf(BALANCE));
        int hudLeft = anchorRight - balanceW - GAP - (USE_ITEM_ICON ? 16 : ICON_PX);
        if (lineEnd >= hudLeft) {
            chatClashUntil = System.currentTimeMillis() + 10_000; // vanilla chat fade
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

        // Vanilla hotbar bounds
        final int hotbarW = 182, hotbarH = 22;
        final int hotbarX = (sw / 2) - (hotbarW / 2);
        final int hotbarY = sh - hotbarH - 1;

        // Anchor: RIGHT edge of the hotbar (keep everything inside)
        final int anchorRight = hotbarX + hotbarW + X_NUDGE;
        final int rowY = hotbarY + Y_NUDGE;

        // Component measures
        final String s = String.valueOf(BALANCE);
        final int textW = mc.font.width(s);
        final int textH = mc.font.lineHeight;

        // Right-align the text to the anchor (so it never goes past the hotbar edge)
        final int textX = anchorRight - textW;
        final int textY = rowY + (9 - textH) / 2; // align with hunger-row height (~9px)

        // Icon sits to the LEFT of the text, and is pushed left as text grows
        final int iconSize = USE_ITEM_ICON ? 16 : ICON_PX;
        final int iconX = textX - GAP - iconSize;
        final int iconY = rowY + (9 - iconSize) / 2;

        if (USE_ITEM_ICON) {
            // Uses your Notch Coin item model/texture (most reliable)
            ItemStack stack = new ItemStack(ModItems.NOTCH_COIN);
            ctx.renderItem(stack, iconX, iconY);
            ctx.renderItemDecorations(mc.font, stack, iconX, iconY);
        } else {
            // Uses a raw HUD sprite at assets/notchcurrency/textures/item/coin.png
            ctx.blit(COIN_HUD_TEX, iconX, iconY,
                    0, 0, ICON_PX, ICON_PX, ICON_PX, ICON_PX);
        }

        ctx.drawString(mc.font, s, textX, textY, 0xFFFFFF, true);
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

        // Underwater bubbles: hide while air bar is visible
        // (non-creative, non-spectator, actually losing air)
        if (!player.getAbilities().instabuild
                && !player.isSpectator()
                && player.getAirSupply() < player.getMaxAirSupply()) {
            return true;
        }

        // Hide when riding ANY vehicle (horses, boats, minecarts, modded aircraft, etc.)
        // This prevents overlap with mount health bars, vehicle fuel/speed bars, etc.
        if (player.isPassenger()) {
            return true;
        }

        return false;
    }
}