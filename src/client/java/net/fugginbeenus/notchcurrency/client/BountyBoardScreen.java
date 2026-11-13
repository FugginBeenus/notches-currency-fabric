package net.fugginbeenus.notchcurrency.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.fugginbeenus.notchcurrency.client.bounty.BoardArt;
import net.fugginbeenus.notchcurrency.client.bounty.BookPaper;
import net.fugginbeenus.notchcurrency.client.bounty.PaperNote;
import net.fugginbeenus.notchcurrency.client.bounty.Requester;
import net.fugginbeenus.notchcurrency.compat.NetClient;
import net.fugginbeenus.notchcurrency.compat.Render;
import net.fugginbeenus.notchcurrency.compat.StackData;
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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class BountyBoardScreen extends AbstractContainerScreen<BountyBoardScreenHandler> {

    private static final int W = 240, H = 290;
    private static final int CORK_X = 5, CORK_W = 230, TOP_Y = 5, TOP_H = 146, LOW_Y = 157, LOW_H = 128;
    private static final int TAKEN_Y = 169, ROW_W = 220;
    private static final int BTN_W = 48, BTN_H = 14;
    private static final int PG_LX = 88, PG_RX = 134, PG_Y = 270, PG_BW = 18, PG_H = 14;
    private static final int POST_X = 188, POST_Y = 7, POST_W = 44, POST_H = 14;
    private static final long SLIDE_MS = 380L, SETTLE_MS = 1400L, ROW_SALT = 0x5DEECE66DL;
    private static final int[] ROW_OFF = {0, 1, -1, 1, 0};
    private static final int[][] SPOTS = {{12, 24, 104, 60}, {124, 23, 104, 61}, {13, 88, 103, 60}, {124, 89, 104, 59}};
    private static final int WANTED = BoardArt.WANTED, LABEL = BoardArt.LABEL;

    private record Note(int slot, int index, int x, int y, int w, int h, ItemStack stack) {}

    private final ItemStack[] shown = new ItemStack[BountyBoardScreenHandler.OFFER_SLOTS];
    private ItemStack[] leaving = new ItemStack[BountyBoardScreenHandler.OFFER_SLOTS];
    private int shownPage = -1, leavingPage, slideDir;
    private long slideT0;
    private boolean slid;
    private final Map<String, PaperNote> papers = new HashMap<>();
    private int paperVersion = -1;

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
        BoardArt.label(ctx, "Available", x + 12, y + 11);
        if (canPost()) {
            NotchWidgets.neutralButton(ctx, this.font, x + POST_X, y + POST_Y, POST_W, POST_H, "Post",
                    over(mouseX, mouseY, x + POST_X, y + POST_Y, POST_W, POST_H));
        }
        int totalPages = menu.prop(BountyBoardScreenHandler.P_TOTAL_PAGES);
        if (totalPages > 1) {
            NotchWidgets.pager(ctx, this.font, x + PG_LX, x + PG_RX, y + PG_Y, PG_BW, PG_H, page + 1, totalPages,
                    over(mouseX, mouseY, x + PG_LX, y + PG_Y, PG_BW, PG_H),
                    over(mouseX, mouseY, x + PG_RX, y + PG_Y, PG_BW, PG_H));
        }

        boolean full = countTaken() >= menu.prop(BountyBoardScreenHandler.P_TAKE_LIMIT);
        long u = now - slideT0;
        ctx.enableScissor(x + CORK_X, y + TOP_Y, x + CORK_X + CORK_W, y + TOP_Y + TOP_H);
        if (slid && u < SLIDE_MS) {
            double e = 1 - Math.pow(1 - u / (double) SLIDE_MS, 3);
            int span = CORK_W + 6;
            for (Note n : notesFor(leaving, (int) Math.round(-slideDir * span * e))) {
                drawNote(ctx, n, false, full, swing(now, n.index(), leavingPage));
            }
            for (Note n : notesFor(shown, (int) Math.round(slideDir * span * (1 - e)))) {
                drawNote(ctx, n, false, full, swing(now, n.index(), page));
            }
        } else {
            List<Note> notes = notesFor(shown, 0);
            for (Note n : notes) {
                boolean hover = over(mouseX, mouseY, n.x(), n.y(), n.w(), n.h());
                drawNote(ctx, n, hover, full, swing(now, n.index(), page));
            }
            if (notes.isEmpty()) {
                NotchWidgets.centerText(ctx, this.font, "All done - check back later!",
                        x + W / 2, y + TOP_Y + TOP_H / 2 - 4, LABEL, true);
            }
        }
        ctx.disableScissor();

        String yours = "Your bounties " + countTaken() + "/" + menu.prop(BountyBoardScreenHandler.P_TAKE_LIMIT);
        BoardArt.label(ctx, yours, x + 12, y + LOW_Y + 5);
        NotchWidgets.badge(ctx, this.font, x + 16 + NotchWidgets.smallCapsWidth(yours), y + LOW_Y + 3, countReady());
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

    private List<Note> notesFor(ItemStack[] stacks, int offsetX) {
        List<Note> out = new ArrayList<>();
        int k = 0;
        for (int i = 0; i < stacks.length && k < SPOTS.length; i++) {
            if (stacks[i] == null || stacks[i].isEmpty() || !StackData.hasData(stacks[i])) continue;
            int[] r = SPOTS[k];
            out.add(new Note(i, k, this.leftPos + r[0] + offsetX, this.topPos + r[1], r[2], r[3], stacks[i]));
            k++;
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

    private PaperNote paper(UUID id, long salt, int w, int h) {
        BookPaper.Sheet sheet = BookPaper.sheet();
        if (sheet.version != paperVersion) {
            releasePapers();
            paperVersion = sheet.version;
        }
        String key = id + "/" + salt + "/" + w + "x" + h;
        PaperNote p = papers.get(key);
        if (p == null) {
            p = PaperNote.bake(id.getMostSignificantBits() ^ id.getLeastSignificantBits() ^ salt, w, h, sheet);
            papers.put(key, p);
        }
        return p;
    }

    private void releasePapers() {
        for (PaperNote p : papers.values()) p.release();
        papers.clear();
    }

    @Override
    public void removed() {
        releasePapers();
        super.removed();
    }

    private static UUID bid(CompoundTag t) {
        if (net.fugginbeenus.notchcurrency.compat.Nbt.hasUuid(t, "bid")) {
            return net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(t, "bid");
        }
        return UUID.nameUUIDFromBytes(t.getString("desc").getBytes(StandardCharsets.UTF_8));
    }

    private void drawBoard(GuiGraphics ctx, int x, int y) {
        BoardArt.frame(ctx, x, y, W, H);
        BoardArt.cork(ctx, x + CORK_X, y + TOP_Y, CORK_W, TOP_H);
        BoardArt.cork(ctx, x + CORK_X, y + LOW_Y, CORK_W, LOW_H);
        BoardArt.logAcross(ctx, x + CORK_X, y + TOP_Y + TOP_H + 1, CORK_W, 6);
    }

    private static void seal(GuiGraphics ctx, int cx, int cy, int color) {
        disc(ctx, cx, cy, 4, shade(color, 0.6f));
        disc(ctx, cx, cy, 3, color);
        ctx.fill(cx - 2, cy - 2, cx, cy - 1, shade(color, 1.35f));
        ctx.fill(cx - 1, cy - 1, cx + 2, cy + 2, shade(color, 0.8f));
        ctx.fill(cx, cy, cx + 1, cy + 1, shade(color, 0.6f));
    }

    private static void disc(GuiGraphics ctx, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int w = (int) Math.round(Math.sqrt((double) r * r - (double) dy * dy));
            ctx.fill(cx - w, cy + dy, cx + w + 1, cy + dy + 1, color);
        }
    }

    private static int shade(int c, float f) {
        int r = Math.min(255, Math.round(((c >> 16) & 255) * f));
        int g = Math.min(255, Math.round(((c >> 8) & 255) * f));
        int b = Math.min(255, Math.round((c & 255) * f));
        return 0xFF000000 | (r << 16) | (g << 8) | b;
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

    private String timeLeft(long exp) {
        if (exp <= 0 || this.minecraft == null || this.minecraft.level == null) return "";
        long mins = Math.max(0, (exp - this.minecraft.level.getGameTime()) / 20L / 60L);
        return mins >= 60 ? mins / 60 + "h left" : mins + "m left";
    }

    private void drawNote(GuiGraphics ctx, Note n, boolean hover, boolean full, float angle) {
        CompoundTag t = StackData.getData(n.stack());
        BookPaper.Sheet sheet = BookPaper.sheet();
        UUID id = bid(t);
        String type = t.getString("typ");
        BountyRarity rarity = BountyRarity.fromString(t.getString("rar"));
        int x = n.x(), y = n.y(), w = n.w(), h = n.h(), pin = x + w / 2;
        PaperNote paper = paper(id, 0L, w, h);
        Render.pushGui(ctx);
        Render.translateGui(ctx, pin, y);
        Render.rotateGui(ctx, angle);
        Render.translateGui(ctx, -pin, -y);
        paper.draw(ctx, x, y);
        NotchWidgets.smallCaps(ctx, Requester.heading(type), x + 6, y + 7, "KILL".equals(type) ? WANTED : sheet.ink);
        if (rarity != BountyRarity.COMMON) seal(ctx, x + w - 14, y + 9, rarity.accentArgb());
        List<String> lines = wrap(t.getString("desc"), w - 14, 2);
        for (int k = 0; k < lines.size(); k++) {
            ctx.drawString(this.font, lines.get(k), x + 6, y + 15 + k * 9, sheet.ink, false);
        }
        int by = y + 16 + lines.size() * 9;
        if (by + 8 <= y + h - 20) {
            String who = Requester.name(id), poster = t.getString("poster");
            String line = poster.isEmpty() ? "by " + who + ", " + Requester.trade(type, t.getString("tgt"), id) : "by " + poster;
            if (poster.isEmpty() && this.font.width(line) > w - 12) line = "by " + who;
            ctx.drawString(this.font, this.font.plainSubstrByWidth(line, w - 12), x + 6, by, sheet.faint, false);
        }
        long coins = t.getLong("rewc");
        ItemStack item = t.contains("rews") ? StackData.readStack(t.getCompound("rews")) : ItemStack.EMPTY;
        int rx = x + 5, ry = y + h - 20;
        if (coins > 0) {
            ctx.renderItem(NotchWidgets.coinStack(), rx, ry);
            ctx.renderItemDecorations(this.font, NotchWidgets.coinStack(), rx, ry, NotchWidgets.compactCount(coins));
            rx += 18;
        }
        if (!item.isEmpty()) {
            ctx.renderItem(item, rx, ry);
            ctx.renderItemDecorations(this.font, item, rx, ry);
        }
        String left = timeLeft(t.getLong("exp"));
        if (!left.isEmpty()) ctx.drawString(this.font, left, x + w - 19 - this.font.width(left), y + h - 14, sheet.faint, false);
        boolean own = t.getBoolean("own");
        if (hover) stamp(ctx, x + w / 2, y + h / 2, own ? "TAKE DOWN" : full ? "FULL" : "TAKE", own || full);
        Render.popGui(ctx);
        BoardArt.pin(ctx, pin, y + 2);
    }

    private void stamp(GuiGraphics ctx, int cx, int cy, String text, boolean full) {
        int w = this.font.width(text) + 8, h = 12, x = cx - w / 2, y = cy - h / 2;
        ctx.fill(x, y, x + w, y + h, full ? 0xFF6E2018 : 0xFF1C5A24);
        ctx.fill(x + 1, y + 1, x + w - 1, y + h - 1, full ? 0xFFB03A2E : 0xFF2E8B3A);
        ctx.drawString(this.font, text, x + 4, y + 2, 0xFFFFFFFF, true);
    }

    private int rowX(int i) { return this.leftPos + 10 + ROW_OFF[i % ROW_OFF.length]; }
    private int rowH() { return Math.max(menu.prop(BountyBoardScreenHandler.P_TAKE_LIMIT), countTaken()) >= 5 ? 18 : 20; }
    private int rowY(int i) { return this.topPos + TAKEN_Y + i * (rowH() + 2); }
    private int buttonY(int i) { return rowY(i) + (rowH() - BTN_H) / 2; }
    private int buttonX(int i) { return rowX(i) + ROW_W - 64; }

    private void drawTakenRow(GuiGraphics ctx, int i, ItemStack stack, int mouseX, int mouseY) {
        if (stack.isEmpty() || !StackData.hasData(stack)) return;
        CompoundTag t = StackData.getData(stack);
        BookPaper.Sheet sheet = BookPaper.sheet();
        BountyRarity rarity = BountyRarity.fromString(t.getString("rar"));
        boolean kill = "KILL".equals(t.getString("typ"));
        int prog = t.getInt("prog"), req = t.getInt("req");
        int nx = rowX(i), ry = rowY(i), btnX = buttonX(i), rh = rowH(), mid = ry + rh / 2, line = ry + (rh - 8) / 2;
        paper(bid(t), ROW_SALT, ROW_W, rh).draw(ctx, nx, ry);
        if (rarity != BountyRarity.COMMON) seal(ctx, nx + 9, mid, rarity.accentArgb());

        int rewX = btnX - 2;
        ItemStack item = t.contains("rews") ? StackData.readStack(t.getCompound("rews")) : ItemStack.EMPTY;
        long coins = t.getLong("rewc");
        if (!item.isEmpty()) {
            rewX -= 18;
            ctx.renderItem(item, rewX, ry + (rh - 16) / 2);
            ctx.renderItemDecorations(this.font, item, rewX, ry + (rh - 16) / 2);
        }
        if (coins > 0) {
            rewX -= 18;
            ctx.renderItem(NotchWidgets.coinStack(), rewX, ry + (rh - 16) / 2);
            ctx.renderItemDecorations(this.font, NotchWidgets.coinStack(), rewX, ry + (rh - 16) / 2, NotchWidgets.compactCount(coins));
        }

        int textX = nx + 17, room = rewX - textX - 4;
        String task = this.font.plainSubstrByWidth(t.getString("desc"), room);
        if (kill) {
            ctx.drawString(this.font, task, textX, mid - 6, sheet.ink, false);
            int barW = Math.max(20, Math.min(70, room - 26));
            int fill = req <= 0 ? barW - 2 : (int) ((barW - 2) * Math.min(1f, prog / (float) req));
            ctx.fill(textX, mid + 3, textX + barW, mid + 6, 0xFF2A2A2A);
            ctx.fill(textX + 1, mid + 4, textX + barW - 1, mid + 5, 0xFF4A4A4A);
            ctx.fill(textX + 1, mid + 4, textX + 1 + fill, mid + 5, 0xFF80D830);
            ctx.drawString(this.font, prog + "/" + req, textX + barW + 4, mid + 1, sheet.faint, false);
        } else {
            ctx.drawString(this.font, task, textX, line, sheet.ink, false);
        }

        if (kill && prog < req) {
            String left = timeLeft(t.getLong("exp"));
            if (!left.isEmpty()) ctx.drawString(this.font, left, nx + ROW_W - 19 - this.font.width(left), line, sheet.faint, false);
        } else {
            boolean hov = over(mouseX, mouseY, btnX, buttonY(i), BTN_W, BTN_H);
            NotchWidgets.primaryButton(ctx, this.font, btnX, buttonY(i), BTN_W, BTN_H, kill ? "Collect" : "Turn in", hov);
        }
        BoardArt.pin(ctx, nx + ROW_W / 2, ry + 1);
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
            for (Note n : notesFor(shown, 0)) {
                if (over(mx, my, n.x(), n.y(), n.w(), n.h())) return n.stack();
            }
        }
        for (int i = 0; i < BountyBoardScreenHandler.TAKEN_SLOTS; i++) {
            if (over(mx, my, rowX(i), rowY(i), ROW_W, rowH()) && !menu.takenStack(i).isEmpty()) return menu.takenStack(i);
        }
        return ItemStack.EMPTY;
    }

    private boolean canPost() {
        return menu.prop(BountyBoardScreenHandler.P_CAN_POST) == 1;
    }

    private void drawHoverDetails(GuiGraphics ctx, int mouseX, int mouseY) {
        if (canPost() && over(mouseX, mouseY, this.leftPos + POST_X, this.topPos + POST_Y, POST_W, POST_H)) {
            ctx.renderComponentTooltip(this.font, List.of(
                    Component.literal("Post a bounty").withStyle(ChatFormatting.WHITE),
                    Component.literal("Put up a reward to get a job done.").withStyle(ChatFormatting.GRAY),
                    Component.literal("Someone takes it from the board.").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
            return;
        }
        ItemStack stack = hovered(mouseX, mouseY);
        if (stack.isEmpty() || !StackData.hasData(stack)) return;
        CompoundTag t = StackData.getData(stack);

        BountyRarity rarity = BountyRarity.fromString(t.getString("rar"));
        UUID id = bid(t);
        List<Component> lines = new ArrayList<>();
        lines.add(Component.literal(t.getString("desc")).withStyle(rarity.color()));
        lines.add(Component.literal("Reward: " + t.getString("rew")).withStyle(ChatFormatting.GOLD));
        lines.add(Component.literal(rarity.name().charAt(0) + rarity.name().substring(1).toLowerCase()).withStyle(rarity.color()));
        String poster = t.getString("poster");
        lines.add(Component.literal("Posted by " + (poster.isEmpty() ? Requester.name(id) + ", "
                + Requester.trade(t.getString("typ"), t.getString("tgt"), id) : poster)).withStyle(ChatFormatting.GRAY));
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
        if (button == InputConstants.MOUSE_BUTTON_LEFT) {
            int mx = (int) mouseX, my = (int) mouseY;
            if (canPost() && this.minecraft != null && this.minecraft.gameMode != null
                    && over(mx, my, this.leftPos + POST_X, this.topPos + POST_Y, POST_W, POST_H)) {
                NotchWidgets.click();
                this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, 2);
                return true;
            }
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
                for (Note n : notesFor(shown, 0)) {
                    if (over(mx, my, n.x(), n.y(), n.w(), n.h())) {
                        action(n.stack(), StackData.getData(n.stack()).getBoolean("own") ? 3 : 0);
                        return true;
                    }
                }
            }
            for (int i = 0; i < BountyBoardScreenHandler.TAKEN_SLOTS; i++) {
                ItemStack s = menu.takenStack(i);
                if (s.isEmpty() || !over(mx, my, buttonX(i), buttonY(i), BTN_W, BTN_H)) continue;
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
