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

    private static int BALANCE = 0;

    // Nudges
    private static final int X_NUDGE = 0;     // +right / -left
    private static final int Y_NUDGE = -27;   // relative to hotbar top; ~-10 sits over hunger row

    private static final int GAP = 4;         // between text and icon
    private static final int ICON_PX = 9;     // 9px HUD sprite size (for texture mode)
    private static final boolean USE_ITEM_ICON = false; // true = draw 16px item icon

    // Only used if USE_ITEM_ICON == false
    private static final Identifier COIN_HUD_TEX =
            new Identifier(NotchCurrency.MOD_ID, "textures/item/coin.png");

    public static void setBalance(int value) {
        BALANCE = value;
    }

    @Override
    public void onHudRender(DrawContext ctx, float tickDelta) {
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

        var player = mc.player;
        if (player == null) return true;

        // Underwater bubbles: hide while air bar is visible
        // (non-creative, non-spectator, actually losing air)
        if (!player.getAbilities().creativeMode
                && !player.isSpectator()
                && player.getAir() < player.getMaxAir()) {
            return true;
        }

        // Riding a mount with a lot of HP (horse hearts row)
        if (player.hasVehicle() && player.getVehicle() instanceof LivingEntity mount) {
            float hp = mount.getMaxHealth();
            // Each row is 20 HP (10 hearts); hide if > 1 row
            if (hp > 20.0f) {
                return true;
            }
        }

        return false;
    }
}
