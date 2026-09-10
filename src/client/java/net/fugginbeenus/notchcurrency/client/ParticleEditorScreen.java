package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.npc.particle.ParticleEffect;
import net.fugginbeenus.notchcurrency.npc.particle.ParticleLayer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

public class ParticleEditorScreen extends Screen {

    private static final int W = 400, H = 226;
    private static final int TAB_Y = 22, TAB_H = 13;
    private static final int STRIP_X = 10, STRIP_W = 70, STRIP_Y = 40, STRIP_H = 16;
    private static final int ROW_X = 88, ROW_W = 212, ROW_Y = 40, ROW_H = 16;
    private static final int LABEL_W = 70, CTL_X = ROW_X + LABEL_W;
    private static final int PREV_X = 306, PREV_Y = 40, PREV_W = 86, PREV_H = 150;

    private static final int R_PARTICLE = 0, R_COLOUR = 1, R_FADE = 2, R_DUST_SIZE = 3, R_DRAW = 4,
            R_SHAPE = 5, R_SIZE = 6, R_ANCHOR = 7, R_COUNT = 8, R_LIFE = 9, R_RATE = 10,
            R_SPEED = 11, R_SPREAD = 12, R_MOTION = 13, R_MOTION_SPEED = 14, R_HOLD = 15,
            R_NUDGE_X = 16, R_NUDGE_Y = 17, R_NUDGE_Z = 18;

    private static final String[] LABEL = {
            "Particle", "Colour", "Fade to", "Dust size", "Draw as", "Shape", "Size", "Anchor",
            "Count", "Life", "Rate", "Speed", "Spread", "Motion", "Motion speed", "Frame hold",
            "Nudge X", "Nudge Y", "Nudge Z"
    };

    private static final String[] TABS = {"Look", "Shape", "Move", "Spray"};
    private static final int[][] TAB_ROWS = {
            {R_PARTICLE, R_COLOUR, R_FADE, R_DUST_SIZE, R_HOLD},
            {R_DRAW, R_SHAPE, R_SIZE, R_ANCHOR, R_NUDGE_X, R_NUDGE_Y, R_NUDGE_Z},
            {R_MOTION, R_MOTION_SPEED},
            {R_COUNT, R_LIFE, R_RATE, R_SPEED, R_SPREAD}
    };

    private final ParticleEffect effect;
    private int px, py, layerAt, tab;
    private boolean hidden;
    private EditBox particleField;
    private String status = "";
    private float spinTicks;
    private List<Component> tipLines;
    private net.fugginbeenus.notchcurrency.entity.NotchNpcEntity mannequin;
    private final net.minecraft.util.RandomSource dotRng =
            net.minecraft.util.RandomSource.create(1337L);

    public ParticleEditorScreen(String name, CompoundTag existing) {
        super(Component.literal("Effect"));
        this.effect = existing == null ? new ParticleEffect(name) : ParticleEffect.fromNbt(existing);
        if (this.effect.isEmpty()) this.effect.addLayer();
    }

    private ParticleLayer layer() {
        ParticleLayer l = effect.layer(layerAt);
        return l == null ? effect.layer(0) : l;
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
        particleField = new EditBox(this.font, px + CTL_X + 3, py + ROW_Y + 4,
                ROW_W - LABEL_W - 50, 10, Component.empty());
        particleField.setMaxLength(80);
        particleField.setBordered(false);
        particleField.setHint(Component.literal("notchcurrency:glyph").withStyle(ChatFormatting.DARK_GRAY));
        particleField.setValue(layer().particle());
        addRenderableWidget(particleField);
        particleField.setVisible(tab == 0 && !hidden);
    }

    private boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private boolean rowShown(int r) {
        ParticleLayer l = layer();
        return switch (r) {
            case R_COLOUR -> l.takesTint();
            case R_FADE -> l.takesSecondColour();
            case R_DUST_SIZE -> l.takesColour();
            case R_HOLD -> l.takesHold();
            case R_SIZE, R_MOTION, R_MOTION_SPEED -> l.shape() != ParticleLayer.SHAPE_POINT;
            case R_RATE, R_SPEED, R_SPREAD -> !l.solid();
            default -> true;
        };
    }

    private List<Integer> visibleRows() {
        List<Integer> out = new ArrayList<>();
        for (int r : TAB_ROWS[tab]) if (rowShown(r)) out.add(r);
        return out;
    }

    private int rowY(int slot) {
        int extra = tab == 0 && layer().goesItsOwnWay() && slot > 0 ? 10 : 0;
        return py + ROW_Y + slot * ROW_H + extra;
    }

    private String valueOf(int r) {
        ParticleLayer l = layer();
        return switch (r) {
            case R_COLOUR -> shadeName(l.colour(), l);
            case R_FADE -> shadeName(l.colourTo(), l);
            case R_DUST_SIZE -> String.format("%.1f", l.dustSize());
            case R_DRAW -> l.solid() ? "Solid shape" : "Spray";
            case R_SHAPE -> ParticleLayer.SHAPE_NAMES[l.shape()];
            case R_SIZE -> String.format("%.1f blocks", l.size());
            case R_ANCHOR -> ParticleLayer.ANCHOR_NAMES[l.anchor()];
            case R_COUNT -> String.valueOf(l.count());
            case R_LIFE -> l.life() == 0 ? "natural" : l.life() + "t";
            case R_RATE -> l.rate() + "t " + perSecond(l.rate());
            case R_SPEED -> String.format("%.2f", l.speed());
            case R_SPREAD -> String.format("%.2f", l.spreadX());
            case R_MOTION -> ParticleLayer.MOTION_NAMES[l.motion()];
            case R_MOTION_SPEED -> String.format("%.1f", l.motionSpeed());
            case R_HOLD -> l.hold() + "t";
            case R_NUDGE_X -> String.format("%.2f", l.nudgeX());
            case R_NUDGE_Y -> String.format("%.2f", l.nudgeY());
            case R_NUDGE_Z -> String.format("%.2f", l.nudgeZ());
            default -> "";
        };
    }

    private static String perSecond(int ticks) {
        double n = 20.0 / Math.max(1, ticks);
        return n >= 1.0 ? "(" + Math.round(n) + "/s)" : "(" + String.format("%.1f", ticks / 20.0) + "s)";
    }

    private static String shadeName(int rgb, ParticleLayer l) {
        if (rgb == ParticleLayer.RAINBOW) return "Rainbow";
        if (rgb == -1) return l.isOurs() ? "Auto" : "White";
        for (NpcLooksScreen.Shade s : NpcLooksScreen.SHADES) {
            if (s.rgb() == rgb) return s.name();
        }
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    private static int cycleShade(int rgb, int dir, boolean allowRainbow) {
        List<NpcLooksScreen.Shade> all = NpcLooksScreen.SHADES;
        List<Integer> ring = new ArrayList<>();
        ring.add(-1);
        for (int i = 1; i < all.size(); i++) ring.add(all.get(i).rgb());
        if (allowRainbow) ring.add(ParticleLayer.RAINBOW);
        int at = ring.indexOf(rgb);
        if (at < 0) at = 0;
        return ring.get(Math.floorMod(at + dir, ring.size()));
    }

    private void bump(int r, int dir, boolean big) {
        ParticleLayer l = layer();
        float f = big ? 0.25f : 0.05f;
        switch (r) {
            case R_COLOUR -> l.setColour(cycleShade(l.colour(), dir, l.isOurs()));
            case R_FADE -> l.setColourTo(cycleShade(l.colourTo(), dir, false));
            case R_DUST_SIZE -> l.setDustSize(l.dustSize() + dir * (big ? 0.5f : 0.1f));
            case R_DRAW -> {
                boolean on = !l.solid();
                l.setSolid(on);
                if (on && l.shape() == ParticleLayer.SHAPE_POINT) l.setShape(ParticleLayer.SHAPE_RING);
                if (on && l.count() < 12) l.setCount(16);
                if (!on && l.count() > 8) l.setCount(2);
            }
            case R_SHAPE -> l.setShape(l.shape() + dir);
            case R_SIZE -> l.setSize(l.size() + dir * (big ? 0.5f : 0.1f));
            case R_ANCHOR -> l.setAnchor(l.anchor() + dir);
            case R_COUNT -> l.setCount(l.count() + dir * (big ? 10 : 1));
            case R_LIFE -> l.setLife(l.life() + dir * (big ? 10 : 1));
            case R_RATE -> l.setRate(l.rate() + dir * (big ? 10 : 1));
            case R_SPEED -> l.setSpeed(l.speed() + dir * (big ? 0.1f : 0.02f));
            case R_SPREAD -> {
                float v = l.spreadX() + dir * f;
                l.setSpread(v, v, v);
            }
            case R_MOTION -> l.setMotion(l.motion() + dir);
            case R_MOTION_SPEED -> l.setMotionSpeed(l.motionSpeed() + dir * (big ? 1.0f : 0.2f));
            case R_HOLD -> l.setHold(l.hold() + dir * (big ? 5 : 1));
            case R_NUDGE_X -> l.setNudge(l.nudgeX() + dir * f, l.nudgeY(), l.nudgeZ());
            case R_NUDGE_Y -> l.setNudge(l.nudgeX(), l.nudgeY() + dir * f, l.nudgeZ());
            case R_NUDGE_Z -> l.setNudge(l.nudgeX(), l.nudgeY(), l.nudgeZ() + dir * f);
            default -> { }
        }
    }

    private void pullParticle() {
        String typed = particleField.getValue().trim();
        if (!typed.isEmpty()) layer().setParticle(typed);
    }

    private boolean isWorn() {
        var npc = wornNpc();
        return npc != null && npc.getParticleFx().equalsIgnoreCase(effect.name());
    }

    private net.fugginbeenus.notchcurrency.entity.NotchNpcEntity wornNpc() {
        java.util.UUID id = ParticleDesignerScreen.cameFromNpc;
        if (id == null) return null;
        var mc = this.minecraft;
        if (mc == null || mc.level == null) return null;
        for (net.minecraft.world.entity.Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc
                    && npc.getUUID().equals(id)) return npc;
        }
        return null;
    }

    private net.fugginbeenus.notchcurrency.entity.NotchNpcEntity mannequin() {
        var mc = this.minecraft;
        if (mc == null || mc.level == null) return null;
        if (mannequin == null || mannequin.isRemoved()) {
            mannequin = new net.fugginbeenus.notchcurrency.entity.NotchNpcEntity(
                    net.fugginbeenus.notchcurrency.registry.ModEntities.NOTCH_NPC, mc.level);
            mannequin.setId(-1);
            mannequin.setYRot(180);
            mannequin.yRotO = 180;
            mannequin.yBodyRot = mannequin.yBodyRotO = 180;
            mannequin.yHeadRot = mannequin.yHeadRotO = 180;
            mannequin.setNpcPose(net.fugginbeenus.notchcurrency.entity.NotchNpcEntity.POSE_STANDING);
            mannequin.setPoseAnim(net.fugginbeenus.notchcurrency.entity.NotchNpcEntity.ANIM_STATUE);
            mannequin.setCustomName(null);
        }
        return mannequin;
    }

    private void drawPreview(GuiGraphics ctx) {
        NotchWidgets.inset(ctx, px + PREV_X, py + PREV_Y, PREV_W, PREV_H, NotchTheme.DEEP);
        var doll = mannequin();
        if (doll == null) return;

        int cx = px + PREV_X + PREV_W / 2;
        int feet = py + PREV_Y + PREV_H - 22;
        int scale = 30;
        net.fugginbeenus.notchcurrency.compat.Render.drawEntityAt(ctx, cx, feet, scale, 0f, 0f, doll);

        net.fugginbeenus.notchcurrency.compat.Render.pushGuiOverlay(ctx);
        int time = (int) spinTicks;
        for (int li = 0; li < effect.layerCount(); li++) {
            ParticleLayer l = effect.layer(li);
            boolean active = li == layerAt;
            int rgb = l.colour();
            if (rgb == -1) rgb = ParticleLayer.defaultTint(l.particle());
            if (rgb == -1) rgb = 0x8FE3EA;
            int alpha = active ? 0xFF000000 : 0x66000000;

            var base = net.fugginbeenus.notchcurrency.npc.particle.ParticleSprayer.localAnchor(1.8f, l);
            double phase = net.fugginbeenus.notchcurrency.npc.particle.ParticleSprayer.spinPhase(time, l);
            double lift = net.fugginbeenus.notchcurrency.npc.particle.ParticleSprayer.riseLift(time, l);

            int dots = Math.min(l.count(), 40);
            dotRng.setSeed(1337L + li);
            for (int i = 0; i < dots; i++) {
                var off = l.shape() == ParticleLayer.SHAPE_POINT
                        ? net.minecraft.world.phys.Vec3.ZERO
                        : net.fugginbeenus.notchcurrency.npc.particle.ParticleSprayer
                                .shapeOffset(l, i, dots, phase, dotRng);
                int dx = cx + (int) Math.round((base.x + off.x) * scale);
                int dy = feet - (int) Math.round((base.y + off.y + lift) * scale);
                if (dx < px + PREV_X + 1 || dx > px + PREV_X + PREV_W - 2) continue;
                if (dy < py + PREV_Y + 1 || dy > py + PREV_Y + PREV_H - 2) continue;
                int colour = rgb == ParticleLayer.RAINBOW
                        ? net.minecraft.util.Mth.hsvToRgb((float) i / Math.max(1, dots), 0.85f, 1.0f) & 0xFFFFFF
                        : rgb;
                int r = off.z > 0.05 ? 1 : 2;
                ctx.fill(dx - r + 1, dy - r + 1, dx + r, dy + r, alpha | colour);
            }
        }
        net.fugginbeenus.notchcurrency.compat.Render.popGuiOverlay(ctx);
        NotchWidgets.centerText(ctx, this.font, "layer " + (layerAt + 1),
                px + PREV_X + PREV_W / 2, py + PREV_Y + PREV_H - 10, NotchTheme.TEXT_MUTED, false);
    }

    private static String shortName(String id) {
        String cut = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        return cut.length() > 7 ? cut.substring(0, 7) : cut;
    }

    private int buttonX(int i) { return px + 10 + i * 78; }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    //?}
        spinTicks += delta;
        particleField.setVisible(tab == 0 && !hidden);

        if (hidden) {
            int bx = px + W / 2 - 60, by = py + H - 20;
            NotchWidgets.primaryButton(ctx, this.font, bx, by, 120, 16, "Show editor",
                    over(mouseX, mouseY, bx, by, 120, 16));
            //? if >=26.1 {
            /*super.extractRenderState(ctx, mouseX, mouseY, delta);
            *///?} else {
            super.render(ctx, mouseX, mouseY, delta);
            //?}
            return;
        }

        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.font, effect.name(), px + W / 2, py + 7);

        for (int i = 0; i < TABS.length; i++) {
            int tx = px + 10 + i * 58;
            boolean hover = over(mouseX, mouseY, tx, py + TAB_Y, 54, TAB_H);
            if (i == tab) NotchWidgets.primaryButton(ctx, this.font, tx, py + TAB_Y, 54, TAB_H, TABS[i], hover);
            else NotchWidgets.neutralButton(ctx, this.font, tx, py + TAB_Y, 54, TAB_H, TABS[i], hover);
        }
        NotchWidgets.divider(ctx, px + 8, py + 36, W - 16);

        drawPreview(ctx);

        for (int i = 0; i < ParticleEffect.MAX_LAYERS; i++) {
            int sy = py + STRIP_Y + i * STRIP_H;
            ParticleLayer l = effect.layer(i);
            String label = l == null ? "+ add" : (i + 1) + " " + shortName(l.particle());
            boolean hover = over(mouseX, mouseY, px + STRIP_X, sy, STRIP_W, STRIP_H - 2);
            if (l != null && i == layerAt) {
                NotchWidgets.primaryButton(ctx, this.font, px + STRIP_X, sy, STRIP_W, STRIP_H - 2, label, hover);
            } else {
                NotchWidgets.neutralButton(ctx, this.font, px + STRIP_X, sy, STRIP_W, STRIP_H - 2, label, hover);
            }
        }
        if (effect.layerCount() > 1) {
            int dy = py + STRIP_Y + ParticleEffect.MAX_LAYERS * STRIP_H + 4;
            NotchWidgets.dangerButton(ctx, this.font, px + STRIP_X, dy, STRIP_W, 14, "Drop",
                    over(mouseX, mouseY, px + STRIP_X, dy, STRIP_W, 14));
        }

        List<Component> tip = null;
        List<Integer> rows = visibleRows();
        for (int slot = 0; slot < rows.size(); slot++) {
            int r = rows.get(slot);
            int ry = rowY(slot);
            ctx.drawString(this.font, LABEL[r], px + ROW_X, ry + 4, NotchTheme.TEXT_DARK, false);
            if (r == R_PARTICLE) {
                NotchWidgets.inset(ctx, px + CTL_X, ry, ROW_W - LABEL_W - 44, 14, NotchTheme.DEEP);
                NotchWidgets.primaryButton(ctx, this.font, px + ROW_X + ROW_W - 40, ry, 40, 14, "Pick",
                        over(mouseX, mouseY, px + ROW_X + ROW_W - 40, ry, 40, 14));
                IdSuggest.draw(ctx, this.font, particleField, IdSuggest.KIND_PARTICLE);
                if (layer().goesItsOwnWay()) {
                    String twin = layer().tameTwin();
                    String note = twin == null ? "ignores where it is put"
                            : "ignores where it is put. Use " + twin.substring(14) + "?";
                    ctx.drawString(this.font, note, px + ROW_X + 2, ry + 16, NotchTheme.TEXT_RED, false);
                }
                continue;
            }
            boolean minus = over(mouseX, mouseY, px + CTL_X, ry, 14, 14);
            boolean plus = over(mouseX, mouseY, px + ROW_X + ROW_W - 14, ry, 14, 14);
            NotchWidgets.neutralButton(ctx, this.font, px + CTL_X, ry, 14, 14, "-", minus);
            NotchWidgets.neutralButton(ctx, this.font, px + ROW_X + ROW_W - 14, ry, 14, 14, "+", plus);
            int mid = px + CTL_X + (ROW_W - LABEL_W) / 2;
            NotchWidgets.centerText(ctx, this.font, valueOf(r), mid, ry + 4, NotchTheme.TEXT_DARK, false);
            if (r == R_COLOUR || r == R_FADE) {
                int rgb = r == R_COLOUR ? layer().colour() : layer().colourTo();
                if (rgb == -1) rgb = ParticleLayer.defaultTint(layer().particle());
                if (rgb >= 0) {
                    ctx.fill(px + ROW_X + ROW_W - 30, ry + 2, px + ROW_X + ROW_W - 18, ry + 12,
                            0xFF000000 | rgb);
                }
            }
        }

        if (!status.isEmpty()) {
            NotchWidgets.centerText(ctx, this.font, status, px + 150, py + H - 34,
                    NotchTheme.ACCENT_GREEN, false);
        }

        int by = py + H - 22;
        String[] names = {"Test", isWorn() ? "Live" : "Wear", "Hide", "Save", "Back"};
        for (int i = 0; i < names.length; i++) {
            boolean hover = over(mouseX, mouseY, buttonX(i), by, 72, 16);
            if (i == 1 && isWorn()) NotchWidgets.goldButton(ctx, this.font, buttonX(i), by, 72, 16, names[i], hover);
            else if (i == 0 || i == 3) NotchWidgets.primaryButton(ctx, this.font, buttonX(i), by, 72, 16, names[i], hover);
            else NotchWidgets.neutralButton(ctx, this.font, buttonX(i), by, 72, 16, names[i], hover);
        }
        if (over(mouseX, mouseY, buttonX(1), by, 72, 16)) {
            tip = isWorn()
                    ? List.of(Component.literal("Live").withStyle(ChatFormatting.GOLD),
                        Component.literal("Running on your NPC behind this window.").withStyle(ChatFormatting.GRAY),
                        Component.literal("Save and it updates within a second.").withStyle(ChatFormatting.DARK_GRAY))
                    : List.of(Component.literal("Wear").withStyle(ChatFormatting.WHITE),
                        Component.literal("Put this on the NPC so it runs all the time.").withStyle(ChatFormatting.GRAY));
        } else if (over(mouseX, mouseY, buttonX(2), by, 72, 16)) {
            tip = List.of(Component.literal("Hide").withStyle(ChatFormatting.WHITE),
                    Component.literal("Tuck the editor away to look at the NPC.").withStyle(ChatFormatting.GRAY));
        }
        if (tip != null) tipLines = tip;

        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
        if (tab == 0) IdSuggest.tip(ctx, this.font, particleField, IdSuggest.KIND_PARTICLE, mouseX, mouseY);
        if (tipLines != null) {
            ctx.renderComponentTooltip(this.font, tipLines, mouseX, mouseY);
            tipLines = null;
        }
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
        if (hidden) {
            if (button == 0 && over(mx, my, px + W / 2 - 60, py + H - 20, 120, 16)) {
                NotchWidgets.click();
                hidden = false;
            }
            return true;
        }
        if (button == 0 || button == 1) {
            int dir = button == 1 ? -1 : 1;
            boolean big = net.fugginbeenus.notchcurrency.compat.Render.shiftDown();

            for (int i = 0; i < TABS.length; i++) {
                if (button == 0 && over(mx, my, px + 10 + i * 58, py + TAB_Y, 54, TAB_H)) {
                    NotchWidgets.click();
                    pullParticle();
                    tab = i;
                    return true;
                }
            }

            for (int i = 0; i < ParticleEffect.MAX_LAYERS; i++) {
                int sy = py + STRIP_Y + i * STRIP_H;
                if (!over(mx, my, px + STRIP_X, sy, STRIP_W, STRIP_H - 2)) continue;
                NotchWidgets.click();
                pullParticle();
                if (effect.layer(i) == null) {
                    if (effect.addLayer() != null) layerAt = effect.layerCount() - 1;
                } else {
                    layerAt = i;
                }
                particleField.setValue(layer().particle());
                return true;
            }
            if (effect.layerCount() > 1) {
                int dy = py + STRIP_Y + ParticleEffect.MAX_LAYERS * STRIP_H + 4;
                if (over(mx, my, px + STRIP_X, dy, STRIP_W, 14)) {
                    NotchWidgets.click();
                    effect.removeLayer(layerAt);
                    layerAt = Math.max(0, Math.min(layerAt, effect.layerCount() - 1));
                    particleField.setValue(layer().particle());
                    return true;
                }
            }

            if (tab == 0 && button == 0 && over(mx, my, px + ROW_X + ROW_W - 40, rowY(0), 40, 14)) {
                NotchWidgets.click();
                pullParticle();
                final ParticleEditorScreen self = this;
                net.minecraft.client.Minecraft.getInstance().setScreen(new ParticlePickerScreen(
                        layer().particle(),
                        chosen -> {
                            self.layer().setParticle(chosen);
                            self.particleField.setValue(chosen);
                            net.minecraft.client.Minecraft.getInstance().setScreen(self);
                        },
                        () -> net.minecraft.client.Minecraft.getInstance().setScreen(self)));
                return true;
            }
            if (tab == 0 && layer().goesItsOwnWay() && layer().tameTwin() != null
                    && over(mx, my, px + ROW_X, rowY(0) + 15, ROW_W, 10)) {
                NotchWidgets.click();
                layer().setParticle(layer().tameTwin());
                particleField.setValue(layer().particle());
                return true;
            }

            List<Integer> rows = visibleRows();
            for (int slot = 0; slot < rows.size(); slot++) {
                int r = rows.get(slot);
                if (r == R_PARTICLE) continue;
                int ry = rowY(slot);
                if (over(mx, my, px + CTL_X, ry, 14, 14)) {
                    NotchWidgets.tick();
                    bump(r, -dir, big);
                    return true;
                }
                if (over(mx, my, px + ROW_X + ROW_W - 14, ry, 14, 14)) {
                    NotchWidgets.tick();
                    bump(r, dir, big);
                    return true;
                }
            }

            int by = py + H - 22;
            if (button == 0 && over(mx, my, buttonX(0), by, 72, 16)) {
                NotchWidgets.click();
                pullParticle();
                var doll = wornNpc();
                if (doll == null || this.minecraft == null) {
                    status = "Open this from an NPC to test it.";
                } else {
                    net.fugginbeenus.notchcurrency.client.particle.NpcFxRenderer.burst(this.minecraft, doll, effect);
                    status = "Fired on your NPC.";
                }
                return true;
            }
            if (button == 0 && over(mx, my, buttonX(1), by, 72, 16)) {
                NotchWidgets.click();
                pullParticle();
                java.util.UUID id = ParticleDesignerScreen.cameFromNpc;
                var npc = wornNpc();
                if (id == null || npc == null) {
                    status = "Open this from an NPC to wear it.";
                } else if (isWorn()) {
                    npc.setParticleFx("");
                    NotchPacketsClient.sendNpcSetFx(id, "");
                    status = "Taken off.";
                } else {
                    NotchPacketsClient.sendParticleSave(effect.toNbt());
                    npc.setParticleFx(effect.name());
                    NotchPacketsClient.sendNpcSetFx(id, effect.name());
                    status = "Running on your NPC.";
                }
                return true;
            }
            if (button == 0 && over(mx, my, buttonX(2), by, 72, 16)) {
                NotchWidgets.click();
                pullParticle();
                hidden = true;
                return true;
            }
            if (button == 0 && over(mx, my, buttonX(3), by, 72, 16)) {
                NotchWidgets.click();
                pullParticle();
                NotchPacketsClient.sendParticleSave(effect.toNbt());
                status = "Saved.";
                return true;
            }
            if (button == 0 && over(mx, my, buttonX(4), by, 72, 16)) {
                NotchWidgets.click();
                NotchPacketsClient.sendParticleDesign();
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int key = event.key();
    *///?} else {
    @Override
    public boolean keyPressed(int key, int scan, int mods) {
    //?}
        if ((key == 258 || key == 262) && particleField != null && particleField.isFocused()
                && IdSuggest.accept(particleField, IdSuggest.KIND_PARTICLE)) {
            pullParticle();
            return true;
        }
        //? if >=1.21.11 {
        /*return super.keyPressed(event);
        *///?} else {
        return super.keyPressed(key, scan, mods);
        //?}
    }

    @Override
    public boolean isPauseScreen() { return false; }

    //? if >=1.21.11 {
    /*@Override
    protected void renderBlurredBackground(net.minecraft.client.gui.GuiGraphics ctx) {
    }
    *///?} elif >=1.21 {
    /*@Override
    protected void renderBlurredBackground(float delta) {
    }
    *///?}

    //? if >=1.21 {
    /*@Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    }
    *///?}
}
