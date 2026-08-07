package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.compat.NetClient;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyBoardScreenHandler;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyRarity;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class BountyBoardScreen extends AbstractContainerScreen<BountyBoardScreenHandler> {

    private static final int W = 240, H = 290;
    private static final int ROW_H = 22;   // row pitch; the row body is ROW_H - 2
    private static final int OFFERS_Y = 36;
    private static final int TAKEN_Y = 164;
    private static final int BTN_W = 50, BTN_H = 14;

    public BountyBoardScreen(BountyBoardScreenHandler handler, Inventory inv, Component title) {
        super(handler, inv, title);
        this.imageWidth = W;
        this.imageHeight = H;
        this.titleLabelX = -1000;
        this.inventoryLabelX = -1000;
    }

    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
        final int x = this.leftPos, y = this.topPos;
        NotchWidgets.panel(ctx, x, y, W, H);

        NotchWidgets.title(ctx, this.font, "Bounty Board", x + W / 2, y + 8);

        ctx.drawString(this.font, "AVAILABLE", x + 10, y + 24, NotchTheme.TEXT_MUTED, false);
        int totalPages = menu.prop(BountyBoardScreenHandler.P_TOTAL_PAGES);
        if (totalPages > 1) {
            int page = menu.prop(BountyBoardScreenHandler.P_PAGE);
            NotchWidgets.neutralButton(ctx, this.font, x + W - 74, y + 22, 13, 12, "<",
                    over(mouseX, mouseY, x + W - 74, y + 22, 13, 12));
            NotchWidgets.centerText(ctx, this.font, (page + 1) + "/" + totalPages, x + W - 42, y + 24, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.font, x + W - 23, y + 22, 13, 12, ">",
                    over(mouseX, mouseY, x + W - 23, y + 22, 13, 12));
        }
        boolean anyOffer = false;
        for (int i = 0; i < BountyBoardScreenHandler.OFFER_SLOTS; i++) {
            if (!menu.offerStack(i).isEmpty()) anyOffer = true;
            drawRow(ctx, x, y + OFFERS_Y + i * ROW_H, menu.offerStack(i), mouseX, mouseY);
        }
        if (!anyOffer) {
            NotchWidgets.centerText(ctx, this.font, "All done - check back later!",
                    x + W / 2, y + OFFERS_Y + 14, NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.divider(ctx, x + 8, y + TAKEN_Y - 8, W - 16);
        int taken = countTaken();
        ctx.drawString(this.font, "YOUR BOUNTIES (" + taken + "/" + menu.prop(BountyBoardScreenHandler.P_TAKE_LIMIT) + ")",
                x + 10, y + TAKEN_Y - 4, NotchTheme.TEXT_MUTED, false);
        for (int i = 0; i < BountyBoardScreenHandler.TAKEN_SLOTS; i++) {
            drawRow(ctx, x, y + TAKEN_Y + 8 + i * ROW_H, menu.takenStack(i), mouseX, mouseY);
        }
    }

    private int countTaken() {
        int n = 0;
        for (int i = 0; i < BountyBoardScreenHandler.TAKEN_SLOTS; i++) {
            if (!menu.takenStack(i).isEmpty()) n++;
        }
        return n;
    }

    private static final ItemStack COIN =
            new ItemStack(net.fugginbeenus.notchcurrency.registry.ModItems.NOTCH_COIN);

    private void drawRow(GuiGraphics ctx, int x, int y, ItemStack stack, int mouseX, int mouseY) {
        if (stack.isEmpty() || !StackData.hasData(stack)) return;
        CompoundTag t = StackData.getData(stack);

        BountyRarity rarity = BountyRarity.fromString(t.getString("rar"));
        boolean mine = t.getBoolean("mine");
        boolean kill = "KILL".equals(t.getString("typ"));
        int prog = t.getInt("prog"), req = t.getInt("req");
        int btnX = x + W - 8 - BTN_W;

        // Raised row with hover feedback + a rarity accent bar down the left edge.
        boolean rowHover = over(mouseX, mouseY, x + 6, y, W - 12, ROW_H - 2);
        NotchWidgets.button(ctx, x + 6, y, W - 12, ROW_H - 2, rowHover, false);
        ctx.fill(x + 8, y + 2, x + 10, y + ROW_H - 4, rarity.accentArgb());

        // Reward icons, right-aligned before the button: coins first, then the reward item.
        long rewCoins = t.getLong("rewc");
        ItemStack rewItem = t.contains("rews") ? StackData.readStack(t.getCompound("rews")) : ItemStack.EMPTY;
        int rewX = btnX - 6;
        if (!rewItem.isEmpty()) {
            rewX -= 20;
            ctx.renderItem(rewItem, rewX, y + 2);
            ctx.renderItemDecorations(this.font, rewItem, rewX, y + 2);
        }
        if (rewCoins > 0) {
            rewX -= 22;
            ctx.renderItem(COIN, rewX, y + 2);
            ctx.renderItemDecorations(this.font, COIN, rewX, y + 2, NotchWidgets.compactCount(rewCoins));
        }

        // Task text (with a live progress bar on kill bounties you've taken).
        String task = t.getString("desc");
        if (mine && kill) task += "  " + prog + "/" + req;
        int textX = x + 14;
        String trimmed = this.font.plainSubstrByWidth(task, rewX - textX - 6);
        ctx.drawString(this.font, trimmed, textX, y + 6, NotchTheme.TEXT_DARK, false);
        if (mine && kill) {
            int barW = Math.min(90, rewX - textX - 8);
            int fill = req <= 0 ? barW : (int) (barW * Math.min(1f, prog / (float) req));
            ctx.fill(textX, y + 16, textX + barW, y + 18, 0xFF3A3A3A);
            ctx.fill(textX, y + 16, textX + fill, y + 18, rarity.accentArgb());
        }

        // Action button (or the time left while a kill bounty is in progress).
        boolean hov = over(mouseX, mouseY, btnX, y + 3, BTN_W, BTN_H);
        if (!mine) {
            NotchWidgets.primaryButton(ctx, this.font, btnX, y + 3, BTN_W, BTN_H, "Take", hov);
        } else if (kill && prog < req) {
            long exp = t.getLong("exp");
            String left = "…";
            if (exp > 0 && this.minecraft != null && this.minecraft.level != null) {
                left = Math.max(0, (exp - this.minecraft.level.getGameTime()) / 20L / 60L) + "m";
            }
            int lw = this.font.width(left);
            ctx.drawString(this.font, left, btnX + BTN_W - lw - 2, y + 6, NotchTheme.TEXT_MUTED, false);
        } else {
            NotchWidgets.primaryButton(ctx, this.font, btnX, y + 3, BTN_W, BTN_H,
                    kill ? "Collect" : "Turn in", hov);
        }
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    @Override
    protected void renderLabels(GuiGraphics ctx, int mouseX, int mouseY) {
        // No default labels.
    }

    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
        //? if <1.21 {
        this.renderBackground(ctx);
        //?}
        super.render(ctx, mouseX, mouseY, delta);
        drawHoverDetails(ctx, mouseX, mouseY);
    }

    private void drawHoverDetails(GuiGraphics ctx, int mouseX, int mouseY) {
        int[] hit = rowAt(mouseX, mouseY);
        if (hit == null) return;
        ItemStack stack = hit[0] == 0 ? menu.offerStack(hit[1]) : menu.takenStack(hit[1]);
        if (stack.isEmpty() || !StackData.hasData(stack)) return;
        CompoundTag t = StackData.getData(stack);

        BountyRarity rarity = BountyRarity.fromString(t.getString("rar"));
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(t.getString("desc")).withStyle(rarity.color()));
        lines.add(Component.literal("Reward: " + t.getString("rew")).withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal(rarity.name().charAt(0) + rarity.name().substring(1).toLowerCase()).withStyle(rarity.color()));
        long exp = t.getLong("exp");
        if (exp > 0 && this.minecraft != null && this.minecraft.level != null) {
            long mins = Math.max(0, (exp - this.minecraft.level.getGameTime()) / 20L / 60L);
            lines.add(Component.literal((t.getBoolean("mine") ? "Finish in " : "Rotates in ") + mins + "m").withStyle(ChatFormatting.DARK_GRAY));
        }
        ctx.renderComponentTooltip(this.font, lines, mouseX, mouseY);
    }

    private int[] rowAt(int mx, int my) {
        for (int i = 0; i < BountyBoardScreenHandler.OFFER_SLOTS; i++) {
            int ry = this.topPos + OFFERS_Y + i * ROW_H;
            if (my >= ry && my < ry + ROW_H && mx >= this.leftPos + 6 && mx < this.leftPos + W - 6
                    && !menu.offerStack(i).isEmpty()) return new int[]{0, i};
        }
        for (int i = 0; i < BountyBoardScreenHandler.TAKEN_SLOTS; i++) {
            int ry = this.topPos + TAKEN_Y + 8 + i * ROW_H;
            if (my >= ry && my < ry + ROW_H && mx >= this.leftPos + 6 && mx < this.leftPos + W - 6
                    && !menu.takenStack(i).isEmpty()) return new int[]{1, i};
        }
        return null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            int btnX = this.leftPos + W - 8 - BTN_W;

            // Page nav.
            if (menu.prop(BountyBoardScreenHandler.P_TOTAL_PAGES) > 1 && this.minecraft != null && this.minecraft.gameMode != null) {
                if (over(mx, my, this.leftPos + W - 74, this.topPos + 22, 13, 12)) {
                    NotchWidgets.tick();
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    return true;
                }
                if (over(mx, my, this.leftPos + W - 23, this.topPos + 22, 13, 12)) {
                    NotchWidgets.tick();
                    this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                    return true;
                }
            }

            // Offers → Take.
            for (int i = 0; i < BountyBoardScreenHandler.OFFER_SLOTS; i++) {
                int ry = this.topPos + OFFERS_Y + i * ROW_H;
                ItemStack s = menu.offerStack(i);
                if (!s.isEmpty() && over(mx, my, btnX, ry + 3, BTN_W, BTN_H)) {
                    action(s, 0);
                    return true;
                }
            }
            // Taken → Collect / Turn in.
            for (int i = 0; i < BountyBoardScreenHandler.TAKEN_SLOTS; i++) {
                int ry = this.topPos + TAKEN_Y + 8 + i * ROW_H;
                ItemStack s = menu.takenStack(i);
                if (s.isEmpty() || !over(mx, my, btnX, ry + 3, BTN_W, BTN_H)) continue;
                if (!StackData.hasData(s)) continue;
                CompoundTag t = StackData.getData(s);
                boolean kill = "KILL".equals(t.getString("typ"));
                if (kill && t.getInt("prog") < t.getInt("req")) return true; // not ready
                action(s, kill ? 1 : 2);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void action(ItemStack stack, int action) {
        CompoundTag t = StackData.getData(stack);
        if (!t.hasUUID("bid")) return;
        NotchWidgets.click();
        UUID id = t.getUUID("bid");
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeUUID(id);
        buf.writeVarInt(action);
        NetClient.sendToServer(NotchPackets.BOUNTY_ACTION, buf);
    }

    //? if >=1.21 {
    /*@Override
    protected void applyBlur(float delta) {
        // No 1.21 menu blur behind the mod's screens. They draw crisp panels over the world.
    }
    *///?}
}
