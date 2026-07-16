package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.Entity;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.List;
import java.util.UUID;

/**
 * The in-game NPC conversation view: a live portrait of the talking NPC, its name, the current node's
 * text in a speech panel, and the choice buttons (locked choices show greyed). Clicking a choice asks
 * the server, which sends the next node or a close signal.
 */
public class NpcDialogueScreen extends Screen {

    private static final int W = 280, H = 190;
    private static final int PORTRAIT_X = 10, PORTRAIT_Y = 26, PORTRAIT_W = 70, PORTRAIT_H = 120;
    private static final int TEXT_X = 90, TEXT_Y = 30, TEXT_W = 180;
    private static final int CHOICE_H = 16;

    protected final UUID npcId;
    protected final String npcName;
    protected final String nodeId;
    private final String text;
    protected final int[] indices;
    protected final String[] labels;
    protected final boolean[] enabled;

    private int px, py;
    private List<OrderedText> wrapped;
    private NotchNpcEntity portrait;
    protected boolean chose = false;

    public NpcDialogueScreen(UUID npcId, String npcName, String nodeId, String text,
                             int[] indices, String[] labels, boolean[] enabled) {
        super(Text.literal(npcName));
        this.npcId = npcId;
        this.npcName = npcName;
        this.nodeId = nodeId;
        this.text = text;
        this.indices = indices;
        this.labels = labels;
        this.enabled = enabled;
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
        wrapped = this.textRenderer.wrapLines(StringVisitable.plain(text), TEXT_W);
    }

    private int choiceY(int i) {
        return py + H - 12 - (labels.length - i) * (CHOICE_H + 3);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        //? if >=1.21 {
        /*renderInGameBackground(ctx);
        *///?} else {
        this.renderBackground(ctx);
        //?}
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.textRenderer, npcName, px + W / 2, py + 8);
        if (!bannerText().isEmpty()) {
            NotchWidgets.centerText(ctx, this.textRenderer, bannerText(), px + W / 2, py + 18,
                    NotchTheme.TEXT_MUTED, false);
        }

        // Portrait of the actual NPC (looks toward the cursor, like the editor preview).
        NotchWidgets.inset(ctx, px + PORTRAIT_X, py + PORTRAIT_Y, PORTRAIT_W, PORTRAIT_H, NotchTheme.DEEP);
        NotchNpcEntity npc = findNpc();
        if (npc != null) {
            float oldYaw = npc.getYaw(), oldBody = npc.bodyYaw;
            npc.setYaw(180);
            npc.bodyYaw = 180;
            net.fugginbeenus.notchcurrency.compat.Render.drawEntityAt(ctx, px + PORTRAIT_X + PORTRAIT_W / 2, py + PORTRAIT_Y + PORTRAIT_H - 12, 34,
                    (px + PORTRAIT_X + PORTRAIT_W / 2f) - mouseX, (py + PORTRAIT_Y + 30f) - mouseY, npc);
            npc.setYaw(oldYaw);
            npc.bodyYaw = oldBody;
        }

        // Speech text.
        int ty = py + TEXT_Y;
        for (OrderedText line : wrapped) {
            ctx.drawText(this.textRenderer, line, px + TEXT_X, ty, NotchTheme.TEXT_DARK, false);
            ty += 10;
            if (ty > choiceY(0) - 12) break; // don't run into the buttons
        }

        // Choices.
        for (int i = 0; i < labels.length; i++) {
            int cy = choiceY(i);
            boolean hover = enabled[i] && over(mouseX, mouseY, px + TEXT_X, cy, TEXT_W, CHOICE_H);
            if (enabled[i]) {
                NotchWidgets.primaryButton(ctx, this.textRenderer, px + TEXT_X, cy, TEXT_W, CHOICE_H, labels[i], hover);
            } else {
                NotchWidgets.button(ctx, px + TEXT_X, cy, TEXT_W, CHOICE_H, false, false);
                NotchWidgets.centerText(ctx, this.textRenderer, labels[i] + " (locked)",
                        px + TEXT_X + TEXT_W / 2, cy + (CHOICE_H - 8) / 2, NotchTheme.TEXT_MUTED, false);
            }
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && !chose) {
            int mx = (int) mouseX, my = (int) mouseY;
            for (int i = 0; i < labels.length; i++) {
                if (enabled[i] && over(mx, my, px + TEXT_X, choiceY(i), TEXT_W, CHOICE_H)) {
                    NotchWidgets.tick();
                    onChoice(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    /** A choice was clicked ({@code i} = index into the visible arrays). The live screen asks the
     *  server; the preview subclass navigates its local tree instead. */
    protected void onChoice(int i) {
        chose = true; // one click per page; the server sends the next page or closes
        NotchPacketsClient.sendNpcDialogueChoice(npcId, nodeId, indices[i]);
    }

    /** Optional muted line under the title (the preview marks itself with it). */
    protected String bannerText() {
        return "";
    }

    private NotchNpcEntity findNpc() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.world == null) return null;
        if (portrait != null && !portrait.isRemoved()) return portrait;
        for (Entity e : c.world.getEntities()) {
            if (e instanceof NotchNpcEntity n && n.getUuid().equals(npcId)) {
                portrait = n;
                return n;
            }
        }
        return null;
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    //? if >=1.21 {
    /*@Override
    protected void applyBlur(float delta) {
        // No 1.21 menu blur behind the mod's screens — they draw crisp panels over the world.
    }
    *///?}

    //? if >=1.21 {
    /*@Override
    public void renderBackground(net.minecraft.client.gui.DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Drawn manually at the top of render() — this screen paints its panel after the darkening,
        // but the 1.21 base render would darken over the finished panel (super.render comes last here).
    }
    *///?}
}
