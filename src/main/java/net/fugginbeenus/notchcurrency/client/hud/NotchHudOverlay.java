package net.fugginbeenus.notchcurrency.client.hud;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.util.Window;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

@Environment(EnvType.CLIENT)
public class NotchHudOverlay implements HudRenderCallback {

    // small badge behind the number (subtle dark rounded bar)
    private static final Identifier BADGE = new Identifier("notchcurrency","textures/gui/hud_badge.png");
    private static final Identifier ICON  = new Identifier("notchcurrency","textures/gui/notch_icon.png");

    @Override
    public void onHudRender(DrawContext ctx, float tickDelta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.options.hudHidden) return;
        if (mc.currentScreen instanceof HandledScreen) return; // don't draw over inventories

        int scaledW = ctx.getScaledWindowWidth();
        int scaledH = ctx.getScaledWindowHeight();

        // Base hotbar Y in 1.20.1 is ~scaledH - 39. Ride / extra bars push HUD up.
        boolean riding = mc.player.hasVehicle();
        boolean underwater = mc.player.isSubmergedInWater(); // air bar
        int extra = 0;
        if (riding) extra += 10;
        if (underwater) extra += 10;

        int barY = scaledH - 39 - extra;

        String amt = getFormattedNotches(); // TODO hook to your real value
        int textW = mc.textRenderer.getWidth(amt);

        int iconW = 8, iconH = 8;
        int pad = 3;
        int badgeW = textW + pad * 2 + iconW + 3;
        int badgeH = 12;

        int x = scaledW - 6 - badgeW;     // snug to right side of hotbar
        int y = barY - 10;                 // rests above XP number, like in your mock

        // badge
        ctx.drawTexture(BADGE, x, y, 0, 0, badgeW, badgeH, badgeW, badgeH);

        // icon
        ctx.drawTexture(ICON, x + pad, y + (badgeH - iconH) / 2, 0, 0, iconW, iconH, iconW, iconH);

        // amount
        ctx.drawText(mc.textRenderer, amt, x + pad + iconW + 3, y + 2, 0xFFFFFFFF, false);
    }

    private String getFormattedNotches() {
        // Replace with synced value; formatting w/ thousands sep could be done server-side.
        // For now use a dummy client-side value to prove placement.
        return "4,560";
    }
}
