package net.fugginbeenus.notchcurrency.client;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyBoardScreenHandler;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyRarity;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Bounty board GUI (Bountiful-style, take-first): the top section lists available offers with a
 * [Take] button; the bottom lists your taken bounties with live progress/timers and a
 * [Collect]/[Turn in] button. Code-drawn in the {@link NotchWidgets} style with rarity accents +
 * reward icons; row details show on hover. Actions are sent to the server by bounty id.
 */
public class BountyBoardScreen extends HandledScreen<BountyBoardScreenHandler> {

    private static final int W = 240, H = 226;
    private static final int ROW_H = 16;
    private static final int OFFERS_Y = 36;
    private static final int TAKEN_Y = 138;
    private static final int BTN_W = 50, BTN_H = 13;

    public BountyBoardScreen(BountyBoardScreenHandler handler, PlayerInventory inv, Text title) {
        super(handler, inv, title);
        this.backgroundWidth = W;
        this.backgroundHeight = H;
        this.titleX = -1000;
        this.playerInventoryTitleX = -1000;
    }

    @Override
    protected void drawBackground(DrawContext ctx, float delta, int mouseX, int mouseY) {
        final int x = this.x, y = this.y;
        NotchWidgets.panel(ctx, x, y, W, H);

        NotchWidgets.title(ctx, this.textRenderer, "Bounty Board", x + W / 2, y + 8);

        ctx.drawText(this.textRenderer, "AVAILABLE", x + 10, y + 24, NotchTheme.TEXT_MUTED, false);
        int totalPages = handler.prop(BountyBoardScreenHandler.P_TOTAL_PAGES);
        if (totalPages > 1) {
            int page = handler.prop(BountyBoardScreenHandler.P_PAGE);
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + W - 74, y + 22, 13, 12, "<",
                    over(mouseX, mouseY, x + W - 74, y + 22, 13, 12));
            NotchWidgets.centerText(ctx, this.textRenderer, (page + 1) + "/" + totalPages, x + W - 42, y + 24, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.textRenderer, x + W - 23, y + 22, 13, 12, ">",
                    over(mouseX, mouseY, x + W - 23, y + 22, 13, 12));
        }
        boolean anyOffer = false;
        for (int i = 0; i < BountyBoardScreenHandler.OFFER_SLOTS; i++) {
            if (!handler.offerStack(i).isEmpty()) anyOffer = true;
            drawRow(ctx, x, y + OFFERS_Y + i * ROW_H, handler.offerStack(i), mouseX, mouseY);
        }
        if (!anyOffer) {
            NotchWidgets.centerText(ctx, this.textRenderer, "All done — check back later!",
                    x + W / 2, y + OFFERS_Y + 14, NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.divider(ctx, x + 8, y + TAKEN_Y - 8, W - 16);
        int taken = countTaken();
        ctx.drawText(this.textRenderer, "YOUR BOUNTIES (" + taken + "/" + handler.prop(BountyBoardScreenHandler.P_TAKE_LIMIT) + ")",
                x + 10, y + TAKEN_Y - 4, NotchTheme.TEXT_MUTED, false);
        for (int i = 0; i < BountyBoardScreenHandler.TAKEN_SLOTS; i++) {
            drawRow(ctx, x, y + TAKEN_Y + 8 + i * ROW_H, handler.takenStack(i), mouseX, mouseY);
        }
    }

    private int countTaken() {
        int n = 0;
        for (int i = 0; i < BountyBoardScreenHandler.TAKEN_SLOTS; i++) {
            if (!handler.takenStack(i).isEmpty()) n++;
        }
        return n;
    }

    /** Draw one bounty row; returns nothing but the click handler mirrors the layout. */
    private void drawRow(DrawContext ctx, int x, int y, ItemStack stack, int mouseX, int mouseY) {
        NbtCompound t = stack.getNbt();
        if (stack.isEmpty() || t == null) return;

        BountyRarity rarity = BountyRarity.fromString(t.getString("rar"));
        boolean mine = t.getBoolean("mine");
        boolean kill = "KILL".equals(t.getString("typ"));
        int prog = t.getInt("prog"), req = t.getInt("req");

        // rarity accent bar + reward icon
        ctx.fill(x + 6, y + 1, x + 8, y + ROW_H - 1, rarity.accentArgb());
        ctx.drawItem(stack, x + 10, y);

        // task text
        String task = t.getString("desc");
        if (mine && kill) task += "  " + prog + "/" + req;
        int textX = x + 30;
        int btnX = x + W - 8 - BTN_W;
        String trimmed = this.textRenderer.trimToWidth(task, btnX - textX - 6);
        ctx.drawText(this.textRenderer, trimmed, textX, y + 4, NotchTheme.TEXT_DARK, false);

        // action button
        boolean hov = over(mouseX, mouseY, btnX, y + 1, BTN_W, BTN_H);
        if (!mine) {
            NotchWidgets.primaryButton(ctx, this.textRenderer, btnX, y + 1, BTN_W, BTN_H, "Take", hov);
        } else if (kill) {
            if (prog >= req) NotchWidgets.primaryButton(ctx, this.textRenderer, btnX, y + 1, BTN_W, BTN_H, "Collect", hov);
            else ctx.drawText(this.textRenderer, "…", btnX + BTN_W - 8, y + 4, NotchTheme.TEXT_MUTED, false);
        } else {
            NotchWidgets.primaryButton(ctx, this.textRenderer, btnX, y + 1, BTN_W, BTN_H, "Turn in", hov);
        }
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    @Override
    protected void drawForeground(DrawContext ctx, int mouseX, int mouseY) {
        // No default labels.
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx);
        super.render(ctx, mouseX, mouseY, delta);
        drawHoverDetails(ctx, mouseX, mouseY);
    }

    /** Hover a row to see reward + time left + rarity. */
    private void drawHoverDetails(DrawContext ctx, int mouseX, int mouseY) {
        int[] hit = rowAt(mouseX, mouseY);
        if (hit == null) return;
        ItemStack stack = hit[0] == 0 ? handler.offerStack(hit[1]) : handler.takenStack(hit[1]);
        NbtCompound t = stack.getNbt();
        if (stack.isEmpty() || t == null) return;

        BountyRarity rarity = BountyRarity.fromString(t.getString("rar"));
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal(t.getString("desc")).formatted(rarity.color()));
        lines.add(Text.literal("Reward: " + t.getString("rew")).formatted(Formatting.GOLD));
        lines.add(Text.literal(rarity.name().charAt(0) + rarity.name().substring(1).toLowerCase()).formatted(rarity.color()));
        long exp = t.getLong("exp");
        if (exp > 0 && this.client != null && this.client.world != null) {
            long mins = Math.max(0, (exp - this.client.world.getTime()) / 20L / 60L);
            lines.add(Text.literal((t.getBoolean("mine") ? "Finish in " : "Rotates in ") + mins + "m").formatted(Formatting.DARK_GRAY));
        }
        ctx.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
    }

    /** Which row is under the cursor: {section (0=offer,1=taken), index}, or null. */
    private int[] rowAt(int mx, int my) {
        for (int i = 0; i < BountyBoardScreenHandler.OFFER_SLOTS; i++) {
            int ry = this.y + OFFERS_Y + i * ROW_H;
            if (my >= ry && my < ry + ROW_H && mx >= this.x + 6 && mx < this.x + W - 6
                    && !handler.offerStack(i).isEmpty()) return new int[]{0, i};
        }
        for (int i = 0; i < BountyBoardScreenHandler.TAKEN_SLOTS; i++) {
            int ry = this.y + TAKEN_Y + 8 + i * ROW_H;
            if (my >= ry && my < ry + ROW_H && mx >= this.x + 6 && mx < this.x + W - 6
                    && !handler.takenStack(i).isEmpty()) return new int[]{1, i};
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            int btnX = this.x + W - 8 - BTN_W;

            // Page nav.
            if (handler.prop(BountyBoardScreenHandler.P_TOTAL_PAGES) > 1 && this.client != null && this.client.interactionManager != null) {
                if (over(mx, my, this.x + W - 74, this.y + 22, 13, 12)) {
                    this.client.interactionManager.clickButton(this.handler.syncId, 0);
                    return true;
                }
                if (over(mx, my, this.x + W - 23, this.y + 22, 13, 12)) {
                    this.client.interactionManager.clickButton(this.handler.syncId, 1);
                    return true;
                }
            }

            // Offers → Take.
            for (int i = 0; i < BountyBoardScreenHandler.OFFER_SLOTS; i++) {
                int ry = this.y + OFFERS_Y + i * ROW_H;
                ItemStack s = handler.offerStack(i);
                if (!s.isEmpty() && over(mx, my, btnX, ry + 1, BTN_W, BTN_H)) {
                    action(s, 0);
                    return true;
                }
            }
            // Taken → Collect / Turn in.
            for (int i = 0; i < BountyBoardScreenHandler.TAKEN_SLOTS; i++) {
                int ry = this.y + TAKEN_Y + 8 + i * ROW_H;
                ItemStack s = handler.takenStack(i);
                if (s.isEmpty() || !over(mx, my, btnX, ry + 1, BTN_W, BTN_H)) continue;
                NbtCompound t = s.getNbt();
                if (t == null) continue;
                boolean kill = "KILL".equals(t.getString("typ"));
                if (kill && t.getInt("prog") < t.getInt("req")) return true; // not ready
                action(s, kill ? 1 : 2);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void action(ItemStack stack, int action) {
        NbtCompound t = stack.getNbt();
        if (t == null || !t.containsUuid("bid")) return;
        UUID id = t.getUuid("bid");
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeUuid(id);
        buf.writeVarInt(action);
        ClientPlayNetworking.send(NotchPackets.BOUNTY_ACTION, buf);
    }
}
