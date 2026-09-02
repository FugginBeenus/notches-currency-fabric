package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import java.util.List;
import java.util.UUID;

public class NpcLooksScreen extends Screen {

    private static final int W = 300, H = 244;
    private static final int PREV_X = 8, PREV_Y = 46, PREV_W = 100, PREV_H = 170;
    private static final float BASE_W = 0.6f, BASE_H = 1.95f;
    private static final int RX = 116;
    private static final int TAB_Y = 22, TAB_H = 16, TAB_W = 44;

    public record Shade(String name, int rgb) {}

    public static final List<Shade> SHADES = List.of(
            new Shade("None", -1),
            new Shade("Red", 0xFF6B6B),
            new Shade("Orange", 0xFFA24B),
            new Shade("Gold", 0xFFD24B),
            new Shade("Green", 0x7BD86B),
            new Shade("Teal", 0x4BD8C0),
            new Shade("Blue", 0x6BA8FF),
            new Shade("Purple", 0xB07BFF),
            new Shade("Pink", 0xFF8FD0),
            new Shade("Grey", 0x9AA3A8),
            new Shade("Dark", 0x5A6166)
    );

    private final UUID npcId;
    private int statsBits;
    private int px, py;
    private int tab = 0;
    private boolean showBox = true;
    private static final String[] TABS = {"Colour", "Size", "Name"};
    private static final int BIT_GLOWING = 4, BIT_NAMEPLATE = 8, BIT_INVISIBLE = 128, BIT_BUBBLE = 2048;

    public NpcLooksScreen(UUID npcId, int statsBits) {
        super(Component.literal("Appearance"));
        this.npcId = npcId;
        this.statsBits = statsBits;
    }

    private void toggleBit(int bit) {
        statsBits ^= bit;
        NotchPacketsClient.sendNpcSetStats(npcId, statsBits);
    }

    private void drawBit(GuiGraphics ctx, int mouseX, int mouseY, int y, int bit, String label) {
        boolean on = (statsBits & bit) != 0;
        boolean hov = over(mouseX, mouseY, px + RX, y, 172, 14);
        if (on) NotchWidgets.primaryButton(ctx, this.font, px + RX, y, 172, 14, label, hov);
        else NotchWidgets.neutralButton(ctx, this.font, px + RX, y, 172, 14, label, hov);
    }

    private void pushShape(NotchNpcEntity npc, float x, float y, float z, float nameOff, float bodyOff) {
        if (npc == null) return;
        NotchPacketsClient.sendNpcSetAppearance(npcId, npc.getModelId(), npc.getSkinType(),
                npc.getSkinValue(), npc.isSlim(), x, y, z, nameOff, bodyOff);
    }

    private void stepper(GuiGraphics ctx, int mouseX, int mouseY, int y, String label, String value) {
        ctx.drawString(this.font, label, px + RX, y + 3, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.font, px + RX + 56, y, 16, 14, "-",
                over(mouseX, mouseY, px + RX + 56, y, 16, 14));
        NotchWidgets.centerText(ctx, this.font, value, px + RX + 100, y + 3, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.font, px + RX + 140, y, 16, 14, "+",
                over(mouseX, mouseY, px + RX + 140, y, 16, 14));
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
    }

    private NotchNpcEntity findNpc() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return null;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof NotchNpcEntity npc && npc.getUUID().equals(npcId)) return npc;
        }
        return null;
    }

    private static String shadeName(int rgb) {
        for (Shade s : SHADES) {
            if (s.rgb() == rgb) return s.name();
        }
        return "Custom";
    }

    private static int nextShade(int rgb) {
        for (int i = 0; i < SHADES.size(); i++) {
            if (SHADES.get(i).rgb() == rgb) return SHADES.get((i + 1) % SHADES.size()).rgb();
        }
        return SHADES.get(1).rgb();
    }

    private int step(int mx, int my, int y) {
        if (over(mx, my, px + RX + 56, y, 16, 14)) return -1;
        if (over(mx, my, px + RX + 140, y, 16, 14)) return 1;
        return 0;
    }

    private boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    //?}
        NotchNpcEntity npc = findNpc();

        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.font, "Appearance", px + W / 2, py + 8);

        for (int i = 0; i < TABS.length; i++) {
            int tx = px + 9 + i * 47;
            boolean hov = over(mouseX, mouseY, tx, py + TAB_Y, TAB_W, TAB_H);
            if (i == tab) NotchWidgets.primaryButton(ctx, this.font, tx, py + TAB_Y, TAB_W, TAB_H, TABS[i], hov);
            else NotchWidgets.neutralButton(ctx, this.font, tx, py + TAB_Y, TAB_W, TAB_H, TABS[i], hov);
        }
        NotchWidgets.divider(ctx, px + 8, py + 42, W - 16);

        NotchWidgets.inset(ctx, px + PREV_X, py + PREV_Y, PREV_W, PREV_H, NotchTheme.DEEP);
        if (npc != null) {
            float oldYaw = npc.getYRot(), oldBody = npc.yBodyRot;
            boolean wasInvisible = npc.isInvisible();
            boolean hidden = (statsBits & BIT_INVISIBLE) != 0;
            npc.setYRot(180);
            npc.yBodyRot = 180;
            npc.setInvisible(hidden);
            float lookX = (px + PREV_X + PREV_W / 2f) - mouseX;
            float lookY = (py + PREV_Y + 40f) - mouseY;
            net.fugginbeenus.notchcurrency.compat.Render.drawEntityAt(ctx,
                    px + PREV_X + PREV_W / 2, py + PREV_Y + PREV_H - 16, 42, lookX, lookY, npc);
            npc.setYRot(oldYaw);
            npc.yBodyRot = oldBody;
            npc.setInvisible(wasInvisible);

            net.fugginbeenus.notchcurrency.compat.Render.pushGuiOverlay(ctx);
            if ((statsBits & BIT_GLOWING) != 0) {
                int glow = 0xFF7BE0FF;
                int gx0 = px + PREV_X, gy0 = py + PREV_Y, gx1 = gx0 + PREV_W, gy1 = gy0 + PREV_H;
                ctx.fill(gx0, gy0, gx1, gy0 + 2, glow);
                ctx.fill(gx0, gy1 - 2, gx1, gy1, glow);
                ctx.fill(gx0, gy0, gx0 + 2, gy1, glow);
                ctx.fill(gx1 - 2, gy0, gx1, gy1, glow);
            }
            if (hidden) {
                NotchWidgets.centerText(ctx, this.font, "Invisible", px + PREV_X + PREV_W / 2,
                        py + PREV_Y + PREV_H / 2 - 8, NotchTheme.TEXT_MUTED, false);
                NotchWidgets.centerText(ctx, this.font, "to players", px + PREV_X + PREV_W / 2,
                        py + PREV_Y + PREV_H / 2 + 2, NotchTheme.TEXT_MUTED, false);
            }

            if (tab == 1 && showBox) {
                int cx = px + PREV_X + PREV_W / 2;
                int baseY = py + PREV_Y + PREV_H - 16;
                int bw = Math.max(4, Math.round(BASE_W * npc.getHitboxWidth() * 42f));
                int bh = Math.max(4, Math.round(BASE_H * npc.getHitboxHeight() * 42f));
                int x0 = cx - bw / 2, x1 = cx + bw / 2, y0 = baseY - bh, y1 = baseY;
                int line = 0xFF62D0FF;
                ctx.fill(x0, y0, x1, y0 + 1, line);
                ctx.fill(x0, y1 - 1, x1, y1, line);
                ctx.fill(x0, y0, x0 + 1, y1, line);
                ctx.fill(x1 - 1, y0, x1, y1, line);
            }
            net.fugginbeenus.notchcurrency.compat.Render.popGuiOverlay(ctx);
        } else {
            NotchWidgets.centerText(ctx, this.font, "Preview",
                    px + PREV_X + PREV_W / 2, py + PREV_Y + PREV_H / 2, NotchTheme.TEXT_MUTED, false);
        }

        if (tab == 0) {
            int tint = npc == null ? -1 : npc.getTint();
            ctx.drawString(this.font, "Tint", px + RX, py + 53, NotchTheme.TEXT_DARK, false);
            NotchWidgets.inset(ctx, px + RX + 34, py + 50, 18, 14, tint == -1 ? NotchTheme.DEEP : 0xFF000000 | tint);
            NotchWidgets.neutralButton(ctx, this.font, px + RX + 56, py + 50, 116, 14, shadeName(tint),
                    over(mouseX, mouseY, px + RX + 56, py + 50, 116, 14));

            float alpha = npc == null ? 1f : npc.getAlpha();
            stepper(ctx, mouseX, mouseY, py + 72, "Fade", Math.round(alpha * 100) + "%");

            drawBit(ctx, mouseX, mouseY, py + 96, BIT_GLOWING, "Glowing");
            drawBit(ctx, mouseX, mouseY, py + 116, BIT_INVISIBLE, "Invisible");
            ctx.drawString(this.font, "Fade low makes a ghost.", px + RX, py + 140,
                    NotchTheme.TEXT_MUTED, false);
        } else if (tab == 1) {
            float[] vals = {npc == null ? 1f : npc.getHitboxWidth(), npc == null ? 1f : npc.getHitboxHeight()};
            stepper(ctx, mouseX, mouseY, py + 50, "Hitbox W", String.format("%.2fx", vals[0]));
            stepper(ctx, mouseX, mouseY, py + 68, "Hitbox H", String.format("%.2fx", vals[1]));

            float[] sc = npc == null ? new float[]{1f, 1f, 1f}
                    : new float[]{npc.npcScale(), npc.getScaleY(), npc.getScaleZ()};
            stepper(ctx, mouseX, mouseY, py + 92, "Model W", String.format("%.1fx", sc[0]));
            stepper(ctx, mouseX, mouseY, py + 110, "Model H", String.format("%.1fx", sc[1]));
            stepper(ctx, mouseX, mouseY, py + 128, "Model D", String.format("%.1fx", sc[2]));
            stepper(ctx, mouseX, mouseY, py + 146, "Body Y",
                    String.format("%+.2f", npc == null ? 0f : npc.getBodyOffset()));

            boolean boxHover = over(mouseX, mouseY, px + RX, py + 22, 60, 14);
            boolean showHover = over(mouseX, mouseY, px + W - 74, py + 170, 62, 14);
            if (showBox) {
                NotchWidgets.primaryButton(ctx, this.font, px + W - 74, py + 170, 62, 14, "Hitbox on", showHover);
            } else {
                NotchWidgets.neutralButton(ctx, this.font, px + W - 74, py + 170, 62, 14, "Hitbox off", showHover);
            }
        } else {
            drawBit(ctx, mouseX, mouseY, py + 50, BIT_NAMEPLATE, "Nameplate");
            drawBit(ctx, mouseX, mouseY, py + 68, BIT_BUBBLE, "Talk bubble");
            stepper(ctx, mouseX, mouseY, py + 92, "Name Y",
                    String.format("%+.1f", npc == null ? 0f : npc.getNameOffset()));
            ctx.drawString(this.font, "Name Y lifts the label.", px + RX, py + 116,
                    NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.primaryButton(ctx, this.font, px + RX, py + 218, W - RX - 12, 18, "Back to Editor",
                over(mouseX, mouseY, px + RX, py + 218, W - RX - 12, 18));

        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
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
        int mx = (int) mouseX, my = (int) mouseY;
        if (button == 0) {
            NotchNpcEntity npc = findNpc();

            for (int i = 0; i < TABS.length; i++) {
                if (over(mx, my, px + 9 + i * 47, py + TAB_Y, TAB_W, TAB_H)) {
                    NotchWidgets.click();
                    tab = i;
                    return true;
                }
            }

            if (tab == 1 && over(mx, my, px + W - 74, py + 170, 62, 14)) {
                NotchWidgets.click();
                showBox = !showBox;
                return true;
            }

            if (tab == 0) {
                if (over(mx, my, px + RX + 56, py + 50, 116, 14)) {
                    NotchWidgets.click();
                    NotchPacketsClient.sendNpcLooks(npcId, 0, nextShade(npc == null ? -1 : npc.getTint()));
                    return true;
                }
                int fade = step(mx, my, py + 72);
                if (fade != 0) {
                    NotchWidgets.click();
                    float a = (npc == null ? 1f : npc.getAlpha()) + fade * 0.1f;
                    NotchPacketsClient.sendNpcLooks(npcId, 1, Math.round(Mth.clamp(a, 0.1f, 1.0f) * 100));
                    return true;
                }
                if (over(mx, my, px + RX, py + 96, 172, 14)) { NotchWidgets.click(); toggleBit(BIT_GLOWING); return true; }
                if (over(mx, my, px + RX, py + 116, 172, 14)) { NotchWidgets.click(); toggleBit(BIT_INVISIBLE); return true; }
            } else if (tab == 1) {
                for (int i = 0; i < 2; i++) {
                    int d = step(mx, my, py + 50 + i * 18);
                    if (d == 0) continue;
                    NotchWidgets.click();
                    float v = npc == null ? 1f : (i == 0 ? npc.getHitboxWidth() : npc.getHitboxHeight());
                    NotchPacketsClient.sendNpcLooks(npcId, i == 0 ? 2 : 3,
                            Math.round(Mth.clamp(v + d * 0.1f, 0.25f, 4.0f) * 100));
                    return true;
                }
                if (npc == null) return true;
                float sx = npc.npcScale(), sy = npc.getScaleY(), sz = npc.getScaleZ();
                for (int i = 0; i < 3; i++) {
                    int d = step(mx, my, py + 92 + i * 18);
                    if (d == 0) continue;
                    NotchWidgets.click();
                    float cur = i == 0 ? sx : i == 1 ? sy : sz;
                    float set = Mth.clamp(Math.round((cur + d * 0.1f) * 10f) / 10f, 0.3f, 3.0f);
                    if (i == 0) sx = set; else if (i == 1) sy = set; else sz = set;
                    pushShape(npc, sx, sy, sz, npc.getNameOffset(), npc.getBodyOffset());
                    return true;
                }
                int body = step(mx, my, py + 146);
                if (body != 0) {
                    NotchWidgets.click();
                    float b = Mth.clamp(npc.getBodyOffset() + body * 0.05f, -2.0f, 2.0f);
                    pushShape(npc, sx, sy, sz, npc.getNameOffset(), b);
                    return true;
                }
            } else {
                if (over(mx, my, px + RX, py + 50, 172, 14)) { NotchWidgets.click(); toggleBit(BIT_NAMEPLATE); return true; }
                if (over(mx, my, px + RX, py + 68, 172, 14)) { NotchWidgets.click(); toggleBit(BIT_BUBBLE); return true; }
                int nameY = step(mx, my, py + 92);
                if (nameY != 0 && npc != null) {
                    NotchWidgets.click();
                    float n = Mth.clamp(npc.getNameOffset() + nameY * 0.1f, -2.0f, 2.0f);
                    pushShape(npc, npc.npcScale(), npc.getScaleY(), npc.getScaleZ(), n, npc.getBodyOffset());
                    return true;
                }
            }

            if (over(mx, my, px + RX, py + 218, W - RX - 12, 18)) {
                NotchWidgets.click();
                NotchPacketsClient.sendNpcEditorReopen(npcId, 0);
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
