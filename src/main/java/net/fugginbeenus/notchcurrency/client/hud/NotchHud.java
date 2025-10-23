package net.fugginbeenus.notchcurrency.client.hud;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

/** Balance HUD anchored to the right edge of the hotbar; text grows left and pushes the icon left. */
public final class NotchHud implements HudRenderCallback {

    private static int BALANCE = 0;

    // Nudges
    private static final int X_NUDGE = 0;     // +right / -left
    private static final int Y_NUDGE = -30;   // relative to hotbar top; ~-10 sits over hunger row

    private static final int GAP = 4;         // between text and icon
    private static final int ICON_PX = 9;     // 9px HUD sprite size
    private static final boolean USE_ITEM_ICON = false; // true = draw 16px item icon

    private static final Identifier COIN_HUD_TEX =
            new Identifier(NotchCurrency.MOD_ID, "textures/gui/hud_coin.png");

    public static void setBalance(int value) {
        BALANCE = value;
    }

    @Override
    public void onHudRender(DrawContext ctx, float tickDelta) {
        final MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) return;

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
        final int textY = rowY + (9 - textH) / 2; // align with the hunger row height (~9px)

        // Icon sits to the LEFT of the text, and is pushed left as text grows
        final int iconX = textX - GAP - ICON_PX;
        final int iconY = rowY + (9 - ICON_PX) / 2;

        if (USE_ITEM_ICON) {
            ctx.drawItem(new ItemStack(ModItems.NOTCH_COIN), iconX, textY + (textH - 16) / 2);
        } else {
            ctx.drawTexture(COIN_HUD_TEX, iconX, iconY, 0, 0, ICON_PX, ICON_PX, ICON_PX, ICON_PX);
        }

        ctx.drawText(mc.textRenderer, s, textX, textY, 0xFFFFFF, true);
    }
}
