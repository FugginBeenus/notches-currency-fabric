package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.compat.NetClient;
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
    private static final int CORK_X = 7, CORK_Y = 36, CORK_W = 226, CORK_H = 118;
    private static final int TAKEN_Y = 172, ROW_STEP = 22, ROW_W = 220, ROW_H = 20;
    private static final int BTN_W = 50, BTN_H = 14;
    private static final int PG_LX = 160, PG_RX = 212, PG_Y = 19, PG_BW = 18, PG_H = 15;
    private static final long SLIDE_MS = 380L, SETTLE_MS = 1400L;
    private static final int[] ROW_OFF = {1, -1, 0, 1, -1};
    private static final int[][][] LAYOUTS = {
            {{63, 30, 100, 58}},
            {{8, 30, 100, 56}, {118, 28, 100, 58}},
            {{6, 4, 104, 52}, {118, 2, 102, 54}, {60, 62, 106, 52}},
            {{6, 4, 104, 52}, {118, 2, 102, 54}, {10, 62, 98, 52}, {114, 61, 106, 53}},
            {{4, 4, 80, 52}, {90, 2, 62, 54}, {158, 6, 64, 50}, {6, 62, 102, 52}, {116, 60, 106, 54}},
            {{5, 3, 64, 54}, {76, 7, 84, 50}, {166, 2, 56, 54}, {4, 62, 116, 52}, {128, 60, 94, 54}},
    };

    private record Note(int slot, int index, int x, int y, int w, int h, ItemStack stack) {}

    private final ItemStack[] shown = new ItemStack[BountyBoardScreenHandler.OFFER_SLOTS];
    private ItemStack[] leaving = new ItemStack[BountyBoardScreenHandler.OFFER_SLOTS];
    private int shownPage = -1, leavingPage, slideDir;
    private long slideT0;
    private boolean slid;

    public BountyBoardScreen(BountyBoardScreenHandler handler, Inventory inv, Component title) {
        //? if >=26.1 {
        /*super(handler, inv, title, W, H);
        *///?} else {
        super(handler, inv, title);
        this.imageWidth = W;
        this.imageHeight = H;
        //?}
        this.titleLabelX = -1000;
        this.inventoryLabelX = -1000;
    }

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = this.leftPos, y = this.topPos;
        final long now = System.currentTimeMillis();
        int page = menu.prop(BountyBoardScreenHandler.P_PAGE);
        if (shownPage >= 0 && page != shownPage) {
            leaving = shown.clone();
            leavingPage = shownPage;
            slideDir = page > shownPage ? 1 : -1;
            slideT0 = now;
            slid = true;
        }
        shownPage = page;
        for (int i = 0; i < shown.length; i++) shown[i] = menu.offerStack(i);

        drawBoard(ctx, x, y);
        plaque(ctx, x + 10, y + 21, "Available");
        int totalPages = menu.prop(BountyBoardScreenHandler.P_TOTAL_PAGES);
        if (totalPages > 1) {
            NotchWidgets.pager(ctx, this.font, x + PG_LX, x + PG_RX, y + PG_Y, PG_BW, PG_H, page + 1, totalPages,
                    over(mouseX, mouseY, x + PG_LX, y + PG_Y, PG_BW, PG_H),
                    over(mouseX, mouseY, x + PG_RX, y + PG_Y, PG_BW, PG_H));
        }

        boolean full = countTaken() >= menu.prop(BountyBoardScreenHandler.P_TAKE_LIMIT);
        long u = now - slideT0;
        ctx.enableScissor(x + CORK_X - 1, y + CORK_Y - 2, x + CORK_X + CORK_W + 1, y + CORK_Y + CORK_H);
        if (slid && u < SLIDE_MS) {
            double e = 1 - Math.pow(1 - u / (double) SLIDE_MS, 3);
            int span = CORK_W + 6;
            for (Note n : notesFor(leaving, leavingPage, (int) Math.round(-slideDir * span * e))) {
                drawNote(ctx, n, leavingPage, false, full, swing(now, n.index(), leavingPage));
            }
            for (Note n : notesFor(shown, page, (int) Math.round(slideDir * span * (1 - e)))) {
                drawNote(ctx, n, page, false, full, swing(now, n.index(), page));
            }
        } else {
            List<Note> notes = notesFor(shown, page, 0);
            for (Note n : notes) {
                boolean hover = over(mouseX, mouseY, n.x(), n.y(), n.w(), n.h());
                drawNote(ctx, n, page, hover, full, swing(now, n.index(), page));
            }
            if (notes.isEmpty()) {
                NotchWidgets.centerText(ctx, this.font, "All done - check back later!",
                        x + W / 2, y + CORK_Y + CORK_H / 2 - 4, 0xFFE8DCC0, true);
            }
        }
        ctx.disableScissor();

        int taken = countTaken();
        int pw = plaque(ctx, x + 10, y + TAKEN_Y - 12,
                "Your bounties " + taken + "/" + menu.prop(BountyBoardScreenHandler.P_TAKE_LIMIT));
        NotchWidgets.badge(ctx, this.font, x + 10 + pw - 4, y + TAKEN_Y - 15, countReady());
        for (int i = 0; i < BountyBoardScreenHandler.TAKEN_SLOTS; i++) {
            drawTakenRow(ctx, i, menu.takenStack(i), mouseX, mouseY);
        }
        NotchWidgets.title(ctx, this.font, "Bounty Board", x + W / 2, y + 8, NotchWidgets.Plate.PARCHMENT);
        //? if >=26.1 {
        /*
        super.extractContents(ctx, mouseX, mouseY, delta);
        *///?}
    }

    private int countTaken() {
        int n = 0;
        for (int i = 0; i < BountyBoardScreenHandler.TAKEN_SLOTS; i++) {
            if (!menu.takenStack(i).isEmpty()) n++;
        }
        return n;
    }

    private int countReady() {
        int n = 0;
        for (int i = 0; i < BountyBoardScreenHandler.TAKEN_SLOTS; i++) {
            ItemStack s = menu.takenStack(i);
            if (s.isEmpty() || !StackData.hasData(s)) continue;
            CompoundTag t = StackData.getData(s);
            if ("KILL".equals(t.getString("typ")) && t.getInt("prog") >= t.getInt("req")) n++;
        }
        return n;
    }

    private List<Note> notesFor(ItemStack[] stacks, int page, int offsetX) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < stacks.length; i++) {
            if (stacks[i] != null && !stacks[i].isEmpty() && StackData.hasData(stacks[i])) slots.add(i);
        }
        List<Note> out = new ArrayList<>();
        if (slots.isEmpty()) return out;
        int[][] lay = slots.size() >= 5 ? LAYOUTS[page % 2 == 0 ? 4 : 5] : LAYOUTS[slots.size() - 1];
        for (int k = 0; k < slots.size() && k < lay.length; k++) {
            int[] r = lay[k];
            out.add(new Note(slots.get(k), k, this.leftPos + CORK_X + r[0] + offsetX, this.topPos + CORK_Y + r[1],
                    r[2], r[3], stacks[slots.get(k)]));
        }
        return out;
    }

    private float swing(long now, int index, int page) {
        if (!slid) return 0f;
        long u = now - slideT0;
        if (u < 0 || u > SLIDE_MS + SETTLE_MS) return 0f;
        double phase = ((index * 7 + page * 3) % 5) * 0.1, amp = 1.7 + ((index * 5 + page) % 3) * 0.3;
        double dx;
        if (u < SLIDE_MS) {
            dx = slideDir * amp * 0.8 * Math.sin(Math.PI * u / SLIDE_MS);
        } else {
            double w = u - SLIDE_MS;
            dx = -slideDir * amp * Math.exp(-w / 240.0) * Math.sin(w / 85.0 + phase);
        }
        return (float) Math.toDegrees(Math.atan2(dx, 55.0));
    }

    private void drawBoard(GuiGraphics ctx, int x, int y) {
        NotchWidgets.roundBox(ctx, x, y, W, H, NotchTheme.OUTLINE);
        ctx.fill(x + 1, y + 1, x + W - 1, y + H - 1, NotchTheme.WOOD);
        ctx.fill(x + 1, y + 1, x + W - 1, y + 2, NotchTheme.WOOD_HI);
        ctx.fill(x + 1, y + 1, x + 2, y + H - 1, NotchTheme.WOOD_HI);
        ctx.fill(x + 1, y + H - 2, x + W - 1, y + H - 1, NotchTheme.WOOD_LO);
        ctx.fill(x + W - 2, y + 2, x + W - 1, y + H - 1, NotchTheme.WOOD_LO);
        NotchWidgets.outline(ctx, x + 5, y + 5, W - 10, H - 10, NotchTheme.WOOD_DK);
        ctx.fill(x + 6, y + 6, x + W - 6, y + H - 6, NotchTheme.CORK);
        ctx.fill(x + 6, y + 6, x + W - 6, y + 7, NotchTheme.CORK_DK);
        ctx.fill(x + 6, y + 6, x + 7, y + H - 6, NotchTheme.CORK_DK);
        int r = 0;
        for (int yy = y + 11; yy < y + H - 8; yy += 9, r++) {
            for (int xx = x + 11 + (r % 2 == 1 ? 5 : 0); xx < x + W - 8; xx += 10) {
                ctx.fill(xx, yy, xx + 1, yy + 1, NotchTheme.CORK_DK);
            }
        }
    }

    private int plaque(GuiGraphics ctx, int x, int y, String text) {
        int w = NotchWidgets.smallCapsWidth(text) + 14, h = 11;
        NotchWidgets.roundBox(ctx, x, y, w, h, 0xFF2A1A0E);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, NotchTheme.WOOD);
        ctx.fill(x + 1, y + 1, x + w - 1, y + 2, NotchTheme.WOOD_HI);
        ctx.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, NotchTheme.WOOD_LO);
        ctx.fill(x + 3, y + 5, x + 4, y + 6, 0xFF3A3A3A);
        ctx.fill(x + w - 4, y + 5, x + w - 3, y + 6, 0xFF3A3A3A);
        NotchWidgets.smallCaps(ctx, text, x + 7, y + 3, 0xFF3A2410);
        return w;
    }

    private static int lighten(int c) {
        int r = Math.min(255, ((c >> 16) & 0xFF) + 16), g = Math.min(255, ((c >> 8) & 0xFF) + 16), b = Math.min(255, (c & 0xFF) + 16);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static int darker(int c) {
        int r = ((c >> 16) & 0xFF) * 7 / 10, g = ((c >> 8) & 0xFF) * 7 / 10, b = (c & 0xFF) * 7 / 10;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private static void stampSquare(GuiGraphics ctx, int x, int y, int color) {
        ctx.fill(x, y, x + 4, y + 4, color);
        ctx.fill(x + 1, y + 1, x + 3, y + 3, darker(color));
    }

    private static void waxSeal(GuiGraphics ctx, int cx, int cy) {
        circle(ctx, cx, cy, 3, 0xFF8C2C1E);
        circle(ctx, cx, cy, 2, 0xFFAF3A28);
        ctx.fill(cx - 1, cy - 1, cx, cy, 0xFFD86048);
        ctx.fill(cx, cy, cx + 1, cy + 1, 0xFF722317);
    }

    private static void circle(GuiGraphics ctx, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int w = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            ctx.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, color);
        }
    }

    private List<String> wrap(String text, int width, int maxLines) {
        List<String> out = new ArrayList<>();
        String line = "";
        for (String word : text.split(" ")) {
            String next = line.isEmpty() ? word : line + " " + word;
            if (line.isEmpty() || this.font.width(next) <= width) {
                line = next;
            } else {
                out.add(line);
                line = word;
            }
        }
        if (!line.isEmpty()) out.add(line);
        for (int i = 0; i < out.size(); i++) {
            if (this.font.width(out.get(i)) > width) out.set(i, this.font.plainSubstrByWidth(out.get(i), width));
        }
        if (out.size() > maxLines) {
            out = new ArrayList<>(out.subList(0, maxLines));
            String last = out.get(maxLines - 1);
            out.set(maxLines - 1, this.font.plainSubstrByWidth(last, width - this.font.width("...")) + "...");
        }
        return out;
    }

    private void drawNote(GuiGraphics ctx, Note n, int page, boolean hover, boolean full, float angle) {
        CompoundTag t = StackData.getData(n.stack());
        BountyRarity rarity = BountyRarity.fromString(t.getString("rar"));
        boolean epic = rarity == BountyRarity.EPIC;
        boolean tan = (n.index() + page) % 2 == 1;
        int paper = epic ? 0xFFF6E2B0 : tan ? NotchTheme.PAPER_TAN : NotchTheme.PAPER;
        int edge = epic ? 0xFFE2C688 : tan ? NotchTheme.PAPER_TAN_EDGE : NotchTheme.PAPER_EDGE;
        if (hover) paper = lighten(paper);
        int x = n.x(), y = n.y(), w = n.w(), h = n.h(), pin = x + w / 2;
        net.fugginbeenus.notchcurrency.compat.Render.pushGui(ctx);
        net.fugginbeenus.notchcurrency.compat.Render.translateGui(ctx, pin, y);
        net.fugginbeenus.notchcurrency.compat.Render.rotateGui(ctx, angle);
        net.fugginbeenus.notchcurrency.compat.Render.translateGui(ctx, -pin, -y);
        ctx.fill(x + 1, y + h, x + w, y + h + 1, 0xFF3A2C1E);
        ctx.fill(x, y, x + w, y + h, paper);
        ctx.fill(x, y + h - 1, x + w, y + h, edge);
        ctx.fill(x + w - 1, y, x + w, y + h, edge);
        if (epic) NotchWidgets.outline(ctx, x + 2, y + 2, w - 4, h - 4, NotchTheme.BLOCK_GOLD);
        stampSquare(ctx, x + w - 9, y + 5, rarity.accentArgb());
        List<String> lines = wrap(t.getString("desc"), w - 18, Math.max(1, (h - 26) / 10));
        for (int k = 0; k < lines.size(); k++) {
            ctx.drawString(this.font, lines.get(k), x + 5, y + 6 + k * 10, NotchTheme.NOTE_INK, false);
        }
        long coins = t.getLong("rewc");
        ItemStack item = t.contains("rews") ? StackData.readStack(t.getCompound("rews")) : ItemStack.EMPTY;
        int rx = x + 4;
        if (coins > 0) {
            ctx.renderItem(NotchWidgets.coinStack(), rx, y + h - 20);
            ctx.renderItemDecorations(this.font, NotchWidgets.coinStack(), rx, y + h - 20, NotchWidgets.compactCount(coins));
            rx += 19;
        }
        if (!item.isEmpty()) {
            ctx.renderItem(item, rx, y + h - 20);
            ctx.renderItemDecorations(this.font, item, rx, y + h - 20);
        }
        if (epic) waxSeal(ctx, x + w - 9, y + h - 10);
        if (hover) {
            int top = 4 + lines.size() * 10, sy = top + Math.max(0, (h - 20 - top - 11) / 2);
            NotchWidgets.stamp(ctx, this.font, x + w / 2, y + sy, full ? "FULL" : "TAKE",
                    full ? 0xFFB03A2E : 0xFF2E8B3A, full ? 0x40B03A2E : 0x402E8B3A);
        }
        net.fugginbeenus.notchcurrency.compat.Render.popGui(ctx);
        ctx.fill(pin - 1, y - 1, pin + 1, y + 1, 0xFF5A606E);
        ctx.fill(pin, y, pin + 1, y + 1, 0xFF282C36);
    }

    private int rowX(int i) { return this.leftPos + 10 + ROW_OFF[i % ROW_OFF.length]; }
    private int rowY(int i) { return this.topPos + TAKEN_Y + i * ROW_STEP; }
    private int buttonX(int i) { return rowX(i) + ROW_W - BTN_W - 3; }

    private void drawTakenRow(GuiGraphics ctx, int i, ItemStack stack, int mouseX, int mouseY) {
        if (stack.isEmpty() || !StackData.hasData(stack)) return;
        CompoundTag t = StackData.getData(stack);
        BountyRarity rarity = BountyRarity.fromString(t.getString("rar"));
        boolean epic = rarity == BountyRarity.EPIC;
        boolean kill = "KILL".equals(t.getString("typ"));
        int prog = t.getInt("prog"), req = t.getInt("req");
        int nx = rowX(i), ry = rowY(i), btnX = buttonX(i);
        boolean tan = i % 2 == 1;
        int paper = epic ? 0xFFF6E2B0 : tan ? NotchTheme.PAPER_TAN : NotchTheme.PAPER;
        int edge = epic ? 0xFFE2C688 : tan ? NotchTheme.PAPER_TAN_EDGE : NotchTheme.PAPER_EDGE;
        boolean rowHover = over(mouseX, mouseY, nx, ry, ROW_W, ROW_H);
        ctx.fill(nx + 1, ry + ROW_H, nx + ROW_W, ry + ROW_H + 1, 0xFF3A2C1E);
        ctx.fill(nx, ry, nx + ROW_W, ry + ROW_H, rowHover ? lighten(paper) : paper);
        ctx.fill(nx, ry + ROW_H - 1, nx + ROW_W, ry + ROW_H, edge);
        ctx.fill(nx + ROW_W - 1, ry, nx + ROW_W, ry + ROW_H, edge);
        if (epic) NotchWidgets.outline(ctx, nx + 2, ry + 2, ROW_W - 4, ROW_H - 4, NotchTheme.BLOCK_GOLD);
        stampSquare(ctx, nx + 4, ry + 8, rarity.accentArgb());

        int rewX = btnX - 6;
        ItemStack item = t.contains("rews") ? StackData.readStack(t.getCompound("rews")) : ItemStack.EMPTY;
        long coins = t.getLong("rewc");
        if (!item.isEmpty()) {
            rewX -= 20;
            ctx.renderItem(item, rewX, ry + 2);
            ctx.renderItemDecorations(this.font, item, rewX, ry + 2);
        }
        if (coins > 0) {
            rewX -= 22;
            ctx.renderItem(NotchWidgets.coinStack(), rewX, ry + 2);
            ctx.renderItemDecorations(this.font, NotchWidgets.coinStack(), rewX, ry + 2, NotchWidgets.compactCount(coins));
        }
        if (epic) waxSeal(ctx, rewX - 8, ry + 10);

        int textX = nx + 11;
        String task = t.getString("desc") + (kill ? "  " + prog + "/" + req : "");
        ctx.drawString(this.font, this.font.plainSubstrByWidth(task, rewX - textX - 12), textX, ry + 6,
                NotchTheme.NOTE_INK, false);
        if (kill) {
            int barW = Math.min(80, rewX - textX - 12);
            int fill = req <= 0 ? barW - 2 : (int) ((barW - 2) * Math.min(1f, prog / (float) req));
            NotchWidgets.outline(ctx, textX, ry + 15, barW, 4, 0xFF8A7A62);
            ctx.fill(textX + 1, ry + 16, textX + barW - 1, ry + 18, edge);
            ctx.fill(textX + 1, ry + 16, textX + 1 + fill, ry + 18, rarity.accentArgb());
        }

        boolean hov = over(mouseX, mouseY, btnX, ry + 3, BTN_W, BTN_H);
        if (kill && prog < req) {
            long exp = t.getLong("exp");
            String left = "...";
            if (exp > 0 && this.minecraft != null && this.minecraft.level != null) {
                left = Math.max(0, (exp - this.minecraft.level.getGameTime()) / 20L / 60L) + "m";
            }
            ctx.drawString(this.font, left, btnX + BTN_W - this.font.width(left) - 2, ry + 6, 0xFF8A7A62, false);
        } else {
            NotchWidgets.primaryButton(ctx, this.font, btnX, ry + 3, BTN_W, BTN_H, kill ? "Collect" : "Turn in", hov);
        }
        int pin = nx + ROW_W / 2;
        ctx.fill(pin - 1, ry - 1, pin + 1, ry + 1, 0xFF5A606E);
        ctx.fill(pin, ry, pin + 1, ry + 1, 0xFF282C36);
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    private boolean sliding() {
        return slid && System.currentTimeMillis() - slideT0 < SLIDE_MS;
    }

    @Override
    protected void renderLabels(GuiGraphics ctx, int mouseX, int mouseY) {
    }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    //?}
        //? if <1.21 {
        this.renderBackground(ctx);
        //?}
        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
        drawHoverDetails(ctx, mouseX, mouseY);
    }

    private ItemStack hovered(int mx, int my) {
        if (!sliding()) {
            for (Note n : notesFor(shown, shownPage, 0)) {
                if (over(mx, my, n.x(), n.y(), n.w(), n.h())) return n.stack();
            }
        }
        for (int i = 0; i < BountyBoardScreenHandler.TAKEN_SLOTS; i++) {
            if (over(mx, my, rowX(i), rowY(i), ROW_W, ROW_H) && !menu.takenStack(i).isEmpty()) return menu.takenStack(i);
        }
        return ItemStack.EMPTY;
    }

    private void drawHoverDetails(GuiGraphics ctx, int mouseX, int mouseY) {
        ItemStack stack = hovered(mouseX, mouseY);
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

    //? if >=1.21.11 {
    /*@Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
    *///?} else {
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
    //?}
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            if (menu.prop(BountyBoardScreenHandler.P_TOTAL_PAGES) > 1 && this.minecraft != null && this.minecraft.gameMode != null) {
                if (over(mx, my, this.leftPos + PG_LX, this.topPos + PG_Y, PG_BW, PG_H)) {
                    if (menu.prop(BountyBoardScreenHandler.P_PAGE) > 0) {
                        NotchWidgets.tick();
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 0);
                    }
                    return true;
                }
                if (over(mx, my, this.leftPos + PG_RX, this.topPos + PG_Y, PG_BW, PG_H)) {
                    if (menu.prop(BountyBoardScreenHandler.P_PAGE) + 1 < menu.prop(BountyBoardScreenHandler.P_TOTAL_PAGES)) {
                        NotchWidgets.tick();
                        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 1);
                    }
                    return true;
                }
            }
            if (!sliding()) {
                for (Note n : notesFor(shown, shownPage, 0)) {
                    if (over(mx, my, n.x(), n.y(), n.w(), n.h())) {
                        action(n.stack(), 0);
                        return true;
                    }
                }
            }
            for (int i = 0; i < BountyBoardScreenHandler.TAKEN_SLOTS; i++) {
                ItemStack s = menu.takenStack(i);
                if (s.isEmpty() || !over(mx, my, buttonX(i), rowY(i) + 3, BTN_W, BTN_H)) continue;
                if (!StackData.hasData(s)) continue;
                CompoundTag t = StackData.getData(s);
                boolean kill = "KILL".equals(t.getString("typ"));
                if (kill && t.getInt("prog") < t.getInt("req")) return true;
                action(s, kill ? 1 : 2);
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private void action(ItemStack stack, int action) {
        CompoundTag t = StackData.getData(stack);
        if (!net.fugginbeenus.notchcurrency.compat.Nbt.hasUuid(t, "bid")) return;
        NotchWidgets.click();
        UUID id = net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(t, "bid");
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(id);
        buf.writeVarInt(action);
        NetClient.sendToServer(NotchPackets.BOUNTY_ACTION, buf);
    }

    //? if >=1.21.11 {
    /*@Override
    protected void renderBlurredBackground(net.minecraft.client.gui.GuiGraphics ctx) {
    }
    *///?} elif >=1.21 {
    /*@Override
    protected void renderBlurredBackground(float delta) {
    }
    *///?}
}
