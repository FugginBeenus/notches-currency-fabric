package net.fugginbeenus.notchcurrency.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.item.RoutePlannerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public final class RouteHud implements HudRenderCallback {

    private static final int PAD = 6;

    @Override
    //? if >=1.21 {
    /*public void onHudRender(GuiGraphics ctx, net.minecraft.client.DeltaTracker tickCounter) {
    *///?} else {
    public void onHudRender(GuiGraphics ctx, float tickDelta) {
    //?}
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;

        ItemStack held = client.player.getMainHandItem();
        if (!(held.getItem() instanceof RoutePlannerItem)) {
            held = client.player.getOffhandItem();
            if (!(held.getItem() instanceof RoutePlannerItem)) return;
        }

        String npcName = StackData.has(held, RoutePlannerItem.NPC_NAME_KEY)
                ? StackData.getString(held, RoutePlannerItem.NPC_NAME_KEY) : "NPC";
        int count = StackData.getInt(held, RoutePlannerItem.COUNT_KEY);

        String title = "Patrol route - " + npcName;
        String countLine = "Waypoints: " + count + "/16" + (count < 2 ? "  (need 2+)" : "");
        String[] hints = {
                "Right-click ground - add waypoint",
                "Sneak + right-click - undo last",
                "Right-click the air - confirm & finish",
        };

        var tr = client.font;
        int w = tr.width(title);
        w = Math.max(w, tr.width(countLine));
        for (String s : hints) w = Math.max(w, tr.width(s));
        w += PAD * 2;
        int h = PAD * 2 + 10 + 12 + hints.length * 10;

        int x = (ctx.guiWidth() - w) / 2;
        int y = 6;

        ctx.fill(x, y, x + w, y + h, 0x90101010); // translucent: the world stays visible
        ctx.fill(x, y, x + w, y + 1, 0x60FFFFFF);
        ctx.fill(x, y + h - 1, x + w, y + h, 0x60000000);

        int ty = y + PAD;
        ctx.drawString(tr, title, x + PAD, ty, 0xFFFFD700, true);
        ty += 12;
        ctx.drawString(tr, countLine, x + PAD, ty, count < 2 ? 0xFFFFAA55 : 0xFF7FDF7F, true);
        ty += 12;
        for (String s : hints) {
            ctx.drawString(tr, s, x + PAD, ty, 0xFFCCCCCC, true);
            ty += 10;
        }
    }
}
