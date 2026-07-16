package net.fugginbeenus.notchcurrency.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/**
 * Balance HUD anchored to the right edge of the hotbar; text grows left and pushes the icon left.
 *
 * It auto-hides when vanilla HUD elements would overlap it, like:
 * - Underwater air bubbles
 * - Mounted on a creature with lots of hearts (e.g. horses with > 1 row of HP)
 */
public final class NotchHud implements HudRenderCallback {

    private static long BALANCE = 0;

    // Nudges
    private static final int X_NUDGE = 0;     // +right / -left
    private static final int Y_NUDGE = -27;   // relative to hotbar top; ~-10 sits over hunger row

    private static final int GAP = 4;         // between text and icon
    private static final int ICON_PX = 9;     // 9px HUD sprite size (for texture mode)
    private static final boolean USE_ITEM_ICON = false; // true = draw 16px item icon

    // Only used if USE_ITEM_ICON == false
    private static final Identifier COIN_HUD_TEX =
            NotchCurrency.id("textures/item/coin.png");

    // On 1.21 HUD callbacks draw AFTER chat, so the balance would sit on top of long chat lines.
    // When a game message is wide enough to reach under the HUD, hide it for chat's fade-out.
    private static long chatClashUntil = 0;

    /** Called for every game chat message (from ClientInit); only consulted on 1.21. */
    public static void noteChatMessage(net.minecraft.text.Text message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null) return;
        // Where the chat line ends, in screen pixels (chat draws at x=4, scaled by the chat option).
        double chatScale = mc.options.getChatScale().getValue();
        int lineEnd = 4 + (int) Math.ceil(mc.textRenderer.getWidth(message) * chatScale);
        // The HUD's leftmost pixel: the icon, left of the right-aligned balance text.
        int anchorRight = mc.getWindow().getScaledWidth() / 2 + 91 + X_NUDGE;
        int balanceW = mc.textRenderer.getWidth(String.valueOf(BALANCE));
        int hudLeft = anchorRight - balanceW - GAP - (USE_ITEM_ICON ? 16 : ICON_PX);
        if (lineEnd >= hudLeft) {
            chatClashUntil = System.currentTimeMillis() + 10_000; // vanilla chat fade
        }
    }

    public static void setBalance(long value) {
        BALANCE = value;
    }

    /** Latest balance the client has been told about; also used by the ATM screen. */
    public static long getBalance() {
        return BALANCE;
    }

    @Override
    //? if >=1.21 {
    /*public void onHudRender(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tickCounter) {
    *///?} else {
    public void onHudRender(DrawContext ctx, float tickDelta) {
    //?}
        final MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;
        if (shouldHide(mc)) return;

        final int sw = mc.getWindow().getScaledWidth();
        final int sh = mc.getWindow().getScaledHeight();

        // Vanilla hotbar bounds
        final int hotbarW = 182, hotbarH = 22;
        final int hotbarX = (sw / 2) - (hotbarW / 2);
        final int hotbarY = sh - hotbarH - 1;

        // Anchor: RIGHT edge of the hotbar (keep everything inside)
        final int anchorRight = hotbarX + hotbarW + X_NUDGE;
        final int rowY = hotbarY + Y_NUDGE;

        // Text measures
        final String s = String.valueOf(BALANCE);
        final int textW = mc.textRenderer.getWidth(s);
        final int textH = mc.textRenderer.fontHeight;

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
            ctx.drawItem(stack, iconX, iconY);
            ctx.drawItemInSlot(mc.textRenderer, stack, iconX, iconY);
        } else {
            // Uses a raw HUD sprite at assets/notchcurrency/textures/item/coin.png
            ctx.drawTexture(COIN_HUD_TEX, iconX, iconY,
                    0, 0, ICON_PX, ICON_PX, ICON_PX, ICON_PX);
        }

        ctx.drawText(mc.textRenderer, s, textX, textY, 0xFFFFFF, true);
    }

    /**
     * Decide when the balance HUD should hide to avoid overlapping vanilla HUD bars.
     */
    private static boolean shouldHide(MinecraftClient mc) {
        if (mc.options.hudHidden) return true;

        //? if >=1.21 {
        /*if (System.currentTimeMillis() < chatClashUntil) return true;
        *///?}

        var player = mc.player;
        if (player == null) return true;

        // Underwater bubbles: hide while air bar is visible
        // (non-creative, non-spectator, actually losing air)
        if (!player.getAbilities().creativeMode
                && !player.isSpectator()
                && player.getAir() < player.getMaxAir()) {
            return true;
        }

        // Hide when riding ANY vehicle (horses, boats, minecarts, modded aircraft, etc.)
        // This prevents overlap with mount health bars, vehicle fuel/speed bars, etc.
        if (player.hasVehicle()) {
            return true;
        }

        return false;
    }
}