package net.fugginbeenus.notchcurrency.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyRarity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

/**
 * The positional bounty tracker, styled on the Location Tooltip HUD: one vector-drawn pill per
 * taken bounty — black translucent background with 1px "vanilla" rounded corners, an item icon
 * (the delivery item, or a sword for kill bounties), the task, a live progress bar and countdown.
 * Kill counts come synced; delivery counts read your own inventory so they tick up as you gather.
 *
 * Anchor (6 positions), offsets, scale and opacity live in the {@code hud} section of
 * config/notchcurrency.json, editable in-game via ModMenu → Notch Currency → HUD. The HUD reads
 * the LOCAL file, so on a server every player positions it around their own modded HUD elements.
 * The tracker keybind toggles it and it hides itself when you carry no bounties.
 */
public final class BountyTrackerHud implements HudRenderCallback {

    /** One tracked bounty as synced by BOUNTY_TRACKER. */
    public record Entry(String desc, boolean kill, String targetItemId, int prog, int req,
                        long expiry, String rarity) {}

    private static List<Entry> entries = new ArrayList<>();
    private static boolean visible = true;

    private static final int PILL_W = 150, PILL_H = 26, PILL_GAP = 4;

    private static final ItemStack SWORD = new ItemStack(Items.IRON_SWORD);

    public static void setEntries(List<Entry> list) {
        entries = list;
    }

    public static void toggle() {
        visible = !visible;
    }

    @Override
    public void onHudRender(DrawContext ctx, float tickDelta) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!visible || client.player == null || client.world == null || client.options.hudHidden) return;

        long now = client.world.getTime();
        List<Entry> live = new ArrayList<>();
        for (Entry e : entries) {
            if (e.expiry() <= 0 || e.expiry() > now) live.add(e);
        }
        if (live.isEmpty()) return;

        NotchConfig.Hud cfg = NotchConfigIO.get().hud;
        float s = clamp(cfg.bountyTrackerScale, 50, 200) / 100f;
        int alpha = (int) (clamp(cfg.bountyTrackerOpacity, 0, 100) / 100f * 255) & 0xFF;
        int bg = alpha << 24; // black at the configured opacity, Location Tooltip style

        // Anchor the scaled footprint, then render unscaled under a matrix scale.
        String corner = cfg.bountyTrackerCorner == null ? "TOP_RIGHT" : cfg.bountyTrackerCorner.toUpperCase();
        int totalH = live.size() * (PILL_H + PILL_GAP) - PILL_GAP;
        int scaledW = Math.round(PILL_W * s), scaledH = Math.round(totalH * s);
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        int x = corner.contains("CENTER") ? (sw - scaledW) / 2 + cfg.bountyTrackerX
                : corner.contains("LEFT") ? cfg.bountyTrackerX
                : sw - scaledW - cfg.bountyTrackerX;
        int y = corner.contains("BOTTOM") ? sh - scaledH - cfg.bountyTrackerY : cfg.bountyTrackerY;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(s, s, 1);

        var tr = client.textRenderer;
        int py = 0;
        for (Entry e : live) {
            fillRound(ctx, 0, py, PILL_W, PILL_H, 1, bg);

            // Icon: the delivery item, or a sword for kill bounties.
            ItemStack icon = e.kill() ? SWORD : stackOf(e.targetItemId());
            ctx.drawItem(icon, 4, py + 5);

            // Task + count.
            int accent = BountyRarity.fromString(e.rarity()).accentArgb();
            int have = e.kill() ? e.prog() : countInInventory(client, e.targetItemId());
            int req = Math.max(1, e.req());
            boolean done = have >= req;
            String count = Math.min(have, req) + "/" + req;
            int cw = tr.getWidth(count);
            String desc = tr.trimToWidth(e.desc(), PILL_W - 26 - cw - 10);
            ctx.drawText(tr, desc, 25, py + 5, 0xFFFFFFFF, true);
            ctx.drawText(tr, count, PILL_W - 6 - cw, py + 5, done ? 0xFF7FDF7F : 0xFFDDDDDD, true);

            // Progress bar + countdown.
            int barW = PILL_W - 31 - 30;
            int fill = (int) (barW * Math.min(1f, have / (float) req));
            ctx.fill(25, py + 17, 25 + barW, py + 19, 0x80101820);
            ctx.fill(25, py + 17, 25 + fill, py + 19, done ? 0xFF55BB55 : accent);
            if (e.expiry() > 0) {
                long mins = Math.max(0, (e.expiry() - now) / 20L / 60L);
                String time = done ? "ready!" : mins + "m";
                int tw = tr.getWidth(time);
                ctx.drawText(tr, time, PILL_W - 6 - tw, py + 14, done ? 0xFF7FDF7F : 0xFFB8C4CE, true);
            }
            py += PILL_H + PILL_GAP;
        }

        ctx.getMatrices().pop();
    }

    /* ---------------- Location Tooltip's vector pill helpers ---------------- */

    /** Rounded rect; when r<=1 uses the crisp 1px "vanilla" corner treatment. */
    private static void fillRound(DrawContext ctx, int x, int y, int w, int h, int r, int argb) {
        r = (r <= 1) ? 1 : Math.min(r, Math.min(w, h) / 2);
        if (r == 1) {
            if (w <= 2 || h <= 2) {
                ctx.fill(x, y, x + w, y + h, argb);
                return;
            }
            ctx.fill(x, y + 1, x + w, y + h - 1, argb);       // middle band
            ctx.fill(x + 1, y, x + w - 1, y + 1, argb);       // top row minus corner px
            ctx.fill(x + 1, y + h - 1, x + w - 1, y + h, argb); // bottom row minus corner px
            return;
        }
        int x2 = x + w, y2 = y + h;
        ctx.fill(x + r, y, x2 - r, y2, argb);
        ctx.fill(x, y + r, x + r, y2 - r, argb);
        ctx.fill(x2 - r, y + r, x2, y2 - r, argb);
        ctx.fill(x, y, x + r, y + r, argb);
        ctx.fill(x2 - r, y, x2, y + r, argb);
        ctx.fill(x, y2 - r, x + r, y2, argb);
        ctx.fill(x2 - r, y2 - r, x2, y2, argb);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static ItemStack stackOf(String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        Item item = id == null ? Items.PAPER : Registries.ITEM.get(id);
        return new ItemStack(item == Items.AIR ? Items.PAPER : item);
    }

    private static int countInInventory(MinecraftClient client, String itemId) {
        Identifier id = Identifier.tryParse(itemId);
        if (id == null) return 0;
        Item item = Registries.ITEM.get(id);
        int n = 0;
        for (int i = 0; i < client.player.getInventory().size(); i++) {
            ItemStack st = client.player.getInventory().getStack(i);
            if (st.isOf(item)) n += st.getCount();
        }
        return n;
    }
}
