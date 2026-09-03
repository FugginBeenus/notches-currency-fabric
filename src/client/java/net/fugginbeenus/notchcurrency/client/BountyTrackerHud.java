package net.fugginbeenus.notchcurrency.client;

//? if >=1.21.11 {
/*import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
*///?} else {
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
//?}
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyRarity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

//? if >=1.21.11 {
/*public final class BountyTrackerHud implements HudElement {
*///?} else {
public final class BountyTrackerHud implements HudRenderCallback {
//?}

    public record Entry(String desc, boolean kill, String targetItemId, int prog, int req,
                        long expiry, String rarity, boolean quest,
                        String questKey, boolean handIn, String giver) {}

    public static List<Entry> entries() { return entries; }

    private static List<Entry> entries = new ArrayList<>();
    private static boolean visible = true;
    private static final int PILL_W = 176, PILL_H = 26, PILL_GAP = 4;
    private static ItemStack sword;
    private static ItemStack sword() {

        if (sword == null) sword = new ItemStack(Items.IRON_SWORD);
        return sword;
    }

    public static void setEntries(List<Entry> list) {
        entries = list;
    }
    public static void toggle() {
        visible = !visible;
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
        Minecraft client = Minecraft.getInstance();
        //? if >=26.1 {
        /*if (!visible || client.player == null || client.level == null) return;
        *///?} else {
        if (!visible || client.player == null || client.level == null || client.options.hideGui) return;
        //?}

        long now = client.level.getGameTime();
        List<Entry> live = new ArrayList<>();
        for (Entry e : entries) {
            if (e.expiry() <= 0 || e.expiry() > now) live.add(e);
        }
        if (live.isEmpty()) return;

        NotchConfig.Hud cfg = NotchConfigIO.get().hud;
        float s = clamp(cfg.bountyTrackerScale, 50, 200) / 100f;
        int alpha = (int) (clamp(cfg.bountyTrackerOpacity, 0, 100) / 100f * 255) & 0xFF;
        int bg = (alpha << 24) | hex(cfg.bountyTrackerBackColour, 0x000000);
        int textArgb = 0xFF000000 | hex(cfg.bountyTrackerTextColour, 0xFFFFFF);
        int doneArgb = 0xFF000000 | hex(cfg.bountyTrackerDoneColour, 0x55BB55);

        String corner = cfg.bountyTrackerCorner == null ? "TOP_RIGHT" : cfg.bountyTrackerCorner.toUpperCase();
        int[] rowH = new int[live.size()];
        int totalH = -PILL_GAP;
        for (int i = 0; i < live.size(); i++) {
            rowH[i] = PILL_H + (cfg.bountyTrackerWrap && wraps(client, live.get(i)) ? 9 : 0);
            totalH += rowH[i] + PILL_GAP;
        }
        int scaledW = Math.round(PILL_W * s), scaledH = Math.round(totalH * s);
        int sw = ctx.guiWidth(), sh = ctx.guiHeight();
        int x = corner.contains("CENTER") ? (sw - scaledW) / 2 + cfg.bountyTrackerX
                : corner.contains("LEFT") ? cfg.bountyTrackerX
                : sw - scaledW - cfg.bountyTrackerX;
        int y = corner.contains("BOTTOM") ? sh - scaledH - cfg.bountyTrackerY : cfg.bountyTrackerY;

        net.fugginbeenus.notchcurrency.compat.Render.pushGui(ctx);
        net.fugginbeenus.notchcurrency.compat.Render.translateGui(ctx, x, y);
        net.fugginbeenus.notchcurrency.compat.Render.scaleGui(ctx, s, s);

        var tr = client.font;
        int py = 0;
        for (int idx = 0; idx < live.size(); idx++) {
            Entry e = live.get(idx);
            int ph = rowH[idx];
            fillRound(ctx, 0, py, PILL_W, ph, 1, bg);

            ItemStack icon = e.kill() ? sword() : stackOf(e.targetItemId());
            ctx.renderItem(icon, 4, py + 5);

            int accent = BountyRarity.fromString(e.rarity()).accentArgb();
            int have = e.prog();
            int req = Math.max(1, e.req());
            boolean done = have >= req;
            String count = Math.min(have, req) + "/" + req;
            int cw = tr.width(count);
            int room = PILL_W - 26 - cw - 10;
            String desc = e.desc();
            int extra = 0;
            if (tr.width(desc) > room) {
                if (cfg.bountyTrackerWrap) {
                    String head = tr.plainSubstrByWidth(desc, room);
                    int cut = head.lastIndexOf(' ');
                    if (cut > 4) head = head.substring(0, cut);
                    String rest = desc.substring(head.length()).trim();
                    if (tr.width(rest) > room) rest = tr.plainSubstrByWidth(rest, room - tr.width("...")) + "...";
                    ctx.drawString(tr, head, 25, py + 5, textArgb, true);
                    ctx.drawString(tr, rest, 25, py + 14, textArgb, true);
                    extra = 9;
                    desc = null;
                } else {
                    desc = tr.plainSubstrByWidth(desc, room - tr.width("...")) + "...";
                }
            }
            if (desc != null) ctx.drawString(tr, desc, 25, py + 5, textArgb, true);
            ctx.drawString(tr, count, PILL_W - 6 - cw, py + 5, done ? doneArgb : 0xFFDDDDDD, true);

            if (cfg.bountyTrackerShowBar) {
                int barW = PILL_W - 31 - 30;
                int fill = (int) (barW * Math.min(1f, have / (float) req));
                ctx.fill(25, py + 17 + extra, 25 + barW, py + 19 + extra, 0x80101820);
                ctx.fill(25, py + 17 + extra, 25 + fill, py + 19 + extra, done ? doneArgb : accent);
            }
            if (e.expiry() > 0) {
                long mins = Math.max(0, (e.expiry() - now) / 20L / 60L);
                String time = done ? "ready!" : mins + "m";
                int tw = tr.width(time);
                ctx.drawString(tr, time, PILL_W - 6 - tw, py + 14 + extra, done ? doneArgb : 0xFFB8C4CE, true);
            }
            py += ph + PILL_GAP;
        }

        net.fugginbeenus.notchcurrency.compat.Render.popGui(ctx);
    }

    private static boolean wraps(Minecraft client, Entry e) {
        String count = Math.min(e.prog(), Math.max(1, e.req())) + "/" + Math.max(1, e.req());
        return client.font.width(e.desc()) > PILL_W - 26 - client.font.width(count) - 10;
    }

    private static int hex(String raw, int fallback) {
        if (raw == null) return fallback;
        String t = raw.trim().replace("#", "");
        try {
            return Integer.parseInt(t, 16) & 0xFFFFFF;
        } catch (NumberFormatException notAColour) {
            return fallback;
        }
    }

    private static void fillRound(GuiGraphics ctx, int x, int y, int w, int h, int r, int argb) {
        r = (r <= 1) ? 1 : Math.min(r, Math.min(w, h) / 2);
        if (r == 1) {
            if (w <= 2 || h <= 2) {
                ctx.fill(x, y, x + w, y + h, argb);
                return;
            }
            ctx.fill(x, y + 1, x + w, y + h - 1, argb);
            ctx.fill(x + 1, y, x + w - 1, y + 1, argb);
            ctx.fill(x + 1, y + h - 1, x + w - 1, y + h, argb);
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
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        Item item = id == null ? Items.PAPER : BuiltInRegistries.ITEM.get(id);
        return new ItemStack(item == Items.AIR ? Items.PAPER : item);
    }

    private static int countInInventory(Minecraft client, String itemId) {
        ResourceLocation id = ResourceLocation.tryParse(itemId);
        if (id == null) return 0;
        Item item = BuiltInRegistries.ITEM.get(id);
        int n = 0;
        for (int i = 0; i < client.player.getInventory().getContainerSize(); i++) {
            ItemStack st = client.player.getInventory().getItem(i);
            if (st.is(item)) n += st.getCount();
        }
        return n;
    }
}
