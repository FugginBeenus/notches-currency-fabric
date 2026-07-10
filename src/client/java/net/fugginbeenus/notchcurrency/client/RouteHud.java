package net.fugginbeenus.notchcurrency.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fugginbeenus.notchcurrency.item.RoutePlannerItem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;

/**
 * Transparent route-planning overlay: while the player holds a bound route tool, a translucent
 * panel at the top of the screen shows which NPC the route is for, the live waypoint count, and
 * the three controls. Replaces the old chat-message-only flow.
 */
public final class RouteHud implements HudRenderCallback {

    private static final int PAD = 6;

    @Override
    public void onHudRender(DrawContext ctx, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.options.hudHidden) return;

        ItemStack held = client.player.getMainHandStack();
        if (!(held.getItem() instanceof RoutePlannerItem)) {
            held = client.player.getOffHandStack();
            if (!(held.getItem() instanceof RoutePlannerItem)) return;
        }

        NbtCompound nbt = held.getNbt();
        String npcName = (nbt != null && nbt.contains(RoutePlannerItem.NPC_NAME_KEY))
                ? nbt.getString(RoutePlannerItem.NPC_NAME_KEY) : "NPC";
        int count = nbt != null ? nbt.getInt(RoutePlannerItem.COUNT_KEY) : 0;

        String title = "Patrol route — " + npcName;
        String countLine = "Waypoints: " + count + "/16" + (count < 2 ? "  (need 2+)" : "");
        String[] hints = {
                "Right-click ground — add waypoint",
                "Sneak + right-click — undo last",
                "Right-click the air — confirm & finish",
        };

        var tr = client.textRenderer;
        int w = tr.getWidth(title);
        w = Math.max(w, tr.getWidth(countLine));
        for (String s : hints) w = Math.max(w, tr.getWidth(s));
        w += PAD * 2;
        int h = PAD * 2 + 10 + 12 + hints.length * 10;

        int x = (ctx.getScaledWindowWidth() - w) / 2;
        int y = 6;

        ctx.fill(x, y, x + w, y + h, 0x90101010); // translucent — the world stays visible
        ctx.fill(x, y, x + w, y + 1, 0x60FFFFFF);
        ctx.fill(x, y + h - 1, x + w, y + h, 0x60000000);

        int ty = y + PAD;
        ctx.drawText(tr, title, x + PAD, ty, 0xFFFFD700, true);
        ty += 12;
        ctx.drawText(tr, countLine, x + PAD, ty, count < 2 ? 0xFFFFAA55 : 0xFF7FDF7F, true);
        ty += 12;
        for (String s : hints) {
            ctx.drawText(tr, s, x + PAD, ty, 0xFFCCCCCC, true);
            ty += 10;
        }
    }
}
