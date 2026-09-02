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
    private static final int PILL_W = 150, PILL_H = 26, PILL_GAP = 4;
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
        int bg = alpha << 24;

        String corner = cfg.bountyTrackerCorner == null ? "TOP_RIGHT" : cfg.bountyTrackerCorner.toUpperCase();
        int totalH = live.size() * (PILL_H + PILL_GAP) - PILL_GAP;
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
        for (Entry e : live) {
            fillRound(ctx, 0, py, PILL_W, PILL_H, 1, bg);

            ItemStack icon = e.kill() ? sword() : stackOf(e.targetItemId());
            ctx.renderItem(icon, 4, py + 5);

            int accent = BountyRarity.fromString(e.rarity()).accentArgb();
            int have = e.kill() ? e.prog() : countInInventory(client, e.targetItemId());
            int req = Math.max(1, e.req());
            boolean done = have >= req;
            String count = Math.min(have, req) + "/" + req;
            int cw = tr.width(count);
            String desc = tr.plainSubstrByWidth(e.desc(), PILL_W - 26 - cw - 10);
            ctx.drawString(tr, desc, 25, py + 5, 0xFFFFFFFF, true);
            ctx.drawString(tr, count, PILL_W - 6 - cw, py + 5, done ? 0xFF7FDF7F : 0xFFDDDDDD, true);

            int barW = PILL_W - 31 - 30;
            int fill = (int) (barW * Math.min(1f, have / (float) req));
            ctx.fill(25, py + 17, 25 + barW, py + 19, 0x80101820);
            ctx.fill(25, py + 17, 25 + fill, py + 19, done ? 0xFF55BB55 : accent);
            if (e.expiry() > 0) {
                long mins = Math.max(0, (e.expiry() - now) / 20L / 60L);
                String time = done ? "ready!" : mins + "m";
                int tw = tr.width(time);
                ctx.drawString(tr, time, PILL_W - 6 - tw, py + 14, done ? 0xFF7FDF7F : 0xFFB8C4CE, true);
            }
            py += PILL_H + PILL_GAP;
        }

        net.fugginbeenus.notchcurrency.compat.Render.popGui(ctx);
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
