package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.block.BoardColor;
import net.fugginbeenus.notchcurrency.client.bounty.BoardArt;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.compat.NetClient;
import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.economy.bounty.FactionBoardScreenHandler;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.npc.faction.FactionBoards;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public class FactionBoardScreen extends BountyBoardScreen {

    private static final int LINK_X = 8, LINK_W = 52, LINK_H = 14;

    private static final long CONFIRM_MS = 3000L;
    private static final int SUB_MAX_W = 136;

    private final FactionBoardScreenHandler board;
    private UUID armed;
    private long armedAt;

    public FactionBoardScreen(FactionBoardScreenHandler handler, Inventory inv, Component title) {
        super(handler, inv, title);
        this.board = handler;
    }

    private CompoundTag head() {
        ItemStack h = board.header();
        return StackData.hasData(h) ? StackData.getData(h) : new CompoundTag();
    }

    private boolean overLink(int mx, int my) {
        return over(mx, my, this.leftPos + LINK_X, this.topPos + PG_Y, LINK_W, LINK_H);
    }

    @Override
    protected void drawTitle(GuiGraphics ctx, int x, int y) {
        CompoundTag h = head();
        String name = h.getString("fname");
        int cloth = h.contains("cloth") ? h.getInt("cloth") : BoardColor.NONE.argb();
        NotchWidgets.clothTitle(ctx, this.font, name.isEmpty() ? "Faction Board" : name, x + W / 2, y + 8, cloth);
        String sub = h.getString("fsub");
        if (sub.isEmpty()) return;
        while (sub.length() > 4 && NotchWidgets.smallCapsWidth(sub) > SUB_MAX_W) {
            sub = sub.substring(0, sub.length() - 4) + "...";
        }
        BoardArt.label(ctx, sub, x + W / 2 - NotchWidgets.smallCapsWidth(sub) / 2, y + 12);
    }

    @Override
    protected String offersLabel() {
        return "Jobs";
    }

    @Override
    protected String yoursLabel() {
        return "Your jobs " + countTaken();
    }

    @Override
    protected String emptyText() {
        return "No jobs here yet.";
    }

    @Override
    protected boolean isFull() {
        return false;
    }

    @Override
    protected String stampText(CompoundTag t, boolean full) {
        if (t.getBoolean("own")) return "TAKE DOWN";
        if (!head().getBoolean("member")) return "MEMBERS ONLY";
        return t.getBoolean("short") ? "BANK SHORT" : "TAKE";
    }

    @Override
    protected int noteAction(CompoundTag t) {
        return t.getBoolean("quest") ? FactionBoards.TAKE : super.noteAction(t);
    }

    @Override
    protected boolean rowReady(CompoundTag t) {
        if (!t.getBoolean("quest")) return super.rowReady(t);
        return t.getBoolean("handin") && t.getBoolean("ready");
    }

    private static UUID idOf(CompoundTag t) {
        return net.fugginbeenus.notchcurrency.compat.Nbt.hasUuid(t, "bid")
                ? net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(t, "bid") : null;
    }

    private boolean armed(CompoundTag t) {
        UUID id = idOf(t);
        return id != null && id.equals(armed) && System.currentTimeMillis() - armedAt < CONFIRM_MS;
    }

    @Override
    protected String rowButton(CompoundTag t) {
        if (!t.getBoolean("quest")) return super.rowButton(t);
        if (rowReady(t)) return "Turn in";
        return armed(t) ? "Sure?" : "Give up";
    }

    @Override
    protected boolean rowDanger(CompoundTag t) {
        return t.getBoolean("quest") && !rowReady(t);
    }

    @Override
    protected int rowAction(CompoundTag t) {
        if (!t.getBoolean("quest")) return super.rowAction(t);
        return rowReady(t) ? FactionBoards.TURN_IN : FactionBoards.GIVE_UP;
    }

    @Override
    protected void rowClicked(ItemStack stack, CompoundTag t) {
        if (!t.getBoolean("quest")) {
            super.rowClicked(stack, t);
        } else if (rowReady(t)) {
            action(stack, FactionBoards.TURN_IN);
        } else if (armed(t)) {
            armed = null;
            action(stack, FactionBoards.GIVE_UP);
        } else {
            armed = idOf(t);
            armedAt = System.currentTimeMillis();
            NotchWidgets.click();
        }
    }

    @Override
    protected void sendAction(UUID id, int action) {
        FriendlyByteBuf buf = net.fugginbeenus.notchcurrency.compat.Net.buf();
        buf.writeUUID(id);
        buf.writeVarInt(action);
        NetClient.sendToServer(NotchPackets.FACTION_BOARD_ACTION, buf);
    }

    @Override
    protected void drawExtras(GuiGraphics ctx, int x, int y, int mouseX, int mouseY) {
        CompoundTag h = head();
        if (h.getBoolean("bank")) {
            String text = "Bank " + NotchWidgets.compactCount(h.getLong("bal"));
            BoardArt.label(ctx, text, x + W - 12 - NotchWidgets.smallCapsWidth(text), y + LOW_Y + 5);
        }
        if (h.getBoolean("admin")) {
            NotchWidgets.neutralButton(ctx, this.font, x + LINK_X, y + PG_Y, LINK_W, LINK_H, "Faction",
                    overLink(mouseX, mouseY));
        }
    }

    @Override
    protected boolean clickExtras(int mx, int my) {
        if (!head().getBoolean("admin") || !overLink(mx, my)) return false;
        if (this.minecraft == null || this.minecraft.gameMode == null) return false;
        NotchWidgets.click();
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, FactionBoardScreenHandler.BTN_RELINK);
        return true;
    }

    @Override
    protected boolean tooltipExtras(GuiGraphics ctx, int mx, int my) {
        if (!head().getBoolean("admin") || !overLink(mx, my)) return false;
        ctx.renderComponentTooltip(this.font, List.of(
                Component.literal("Change faction").withStyle(ChatFormatting.WHITE),
                Component.literal("Pick which faction owns this board.").withStyle(ChatFormatting.GRAY),
                Component.literal("Admins only.").withStyle(ChatFormatting.DARK_GRAY)), mx, my);
        return true;
    }
}
