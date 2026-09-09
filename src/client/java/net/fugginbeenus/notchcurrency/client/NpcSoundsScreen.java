package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.npc.NpcSounds;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;
import java.util.UUID;

public class NpcSoundsScreen extends Screen {

    private static final int W = 328, H = 250;
    private static final int ROW_Y = 40, ROW_H = 22;
    private static final int BOX_X = 74, BOX_W = 174, PICK_X = 254, PICK_W = 60;

    private static final String[] LABELS = {"Voice", "Ambient", "Hurt", "Death", "Step", "Angry"};

    private static final String[][] BANK = {
            {"entity.villager.ambient", "entity.villager.trade", "entity.wandering_trader.ambient",
             "entity.pillager.ambient", "entity.piglin.ambient", "entity.witch.ambient",
             "entity.evoker.ambient", "entity.allay.ambient_without_item", "entity.cat.ambient",
             "entity.wolf.ambient", "entity.parrot.ambient"},
            {"block.anvil.use", "block.campfire.crackle", "block.fire.ambient", "block.water.ambient",
             "block.beehive.work", "block.bell.resonate", "block.brewing_stand.brew",
             "block.enchantment_table.use", "block.grindstone.use", "entity.cow.ambient",
             "entity.chicken.ambient", "block.note_block.harp"},
            {"entity.villager.hurt", "entity.player.hurt", "entity.pillager.hurt",
             "entity.iron_golem.hurt", "entity.zombie.hurt"},
            {"entity.villager.death", "entity.player.death", "entity.pillager.death",
             "entity.iron_golem.death", "entity.zombie.death"},
            {"block.stone.step", "block.wood.step", "block.gravel.step", "block.metal.step",
             "block.wool.step", "block.sand.step"},
            {"entity.villager.no", "entity.pillager.celebrate", "entity.wolf.growl",
             "entity.ravager.roar", "entity.iron_golem.attack"},
    };

    private final UUID npcId;
    private final java.util.function.Consumer<NpcSounds> onBack;
    private NpcSounds sounds;

    private int px, py;
    private int pickingFor = 1;
    private final EditBox[] fields = new EditBox[6];

    public NpcSoundsScreen(UUID npcId, NpcSounds sounds, java.util.function.Consumer<NpcSounds> onBack) {
        super(Component.literal("Sounds"));
        this.npcId = npcId;
        this.sounds = sounds == null ? NpcSounds.empty() : sounds;
        this.onBack = onBack;
    }

    private String valueAt(int i) {
        return switch (i) {
            case 0 -> sounds.voice();
            case 1 -> sounds.ambient();
            case 2 -> sounds.hurt();
            case 3 -> sounds.death();
            case 4 -> sounds.step();
            default -> sounds.angry();
        };
    }

    private void pullFields() {
        sounds = sounds.withVoice(fields[0].getValue().trim())
                .withAmbient(fields[1].getValue().trim())
                .withHurt(fields[2].getValue().trim())
                .withDeath(fields[3].getValue().trim())
                .withStep(fields[4].getValue().trim())
                .withAngry(fields[5].getValue().trim());
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
        for (int i = 0; i < 6; i++) {
            String old = fields[i] == null ? valueAt(i) : fields[i].getValue();
            EditBox box = new EditBox(this.font, px + BOX_X + 4, py + ROW_Y + i * ROW_H + 4,
                    BOX_W - 8, 10, Component.empty());
            box.setMaxLength(120);
            box.setBordered(false);
            box.setHint(Component.literal("none").withStyle(ChatFormatting.DARK_GRAY));
            box.setValue(old);
            fields[i] = box;
            addRenderableWidget(box);
        }
    }

    private boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    private int rowY(int i) { return py + ROW_Y + i * ROW_H; }

    private void cycle(int i) {
        String[] bank = BANK[i];
        String current = fields[i].getValue().trim();
        int at = -1;
        for (int k = 0; k < bank.length; k++) {
            if (bank[k].equals(current)) { at = k; break; }
        }
        fields[i].setValue(bank[(at + 1) % bank.length]);
    }

    private void save() {
        pullFields();
        NotchPacketsClient.sendNpcSounds(npcId, sounds);
    }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    //?}
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.font, "Sounds", px + W / 2, py + 8);
        NotchWidgets.centerText(ctx, this.font, "Type a sound id, or press Pick to cycle good ones.",
                px + W / 2, py + 22, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.divider(ctx, px + 8, py + 34, W - 16);

        List<Component> tip = null;
        for (int i = 0; i < 6; i++) {
            int ry = rowY(i);
            ctx.drawString(this.font, LABELS[i], px + 12, ry + 4, NotchTheme.TEXT_DARK, false);
            NotchWidgets.inset(ctx, px + BOX_X, ry, BOX_W, 14, NotchTheme.DEEP);
            boolean pickHover = over(mouseX, mouseY, px + PICK_X, ry, PICK_W, 14);
            NotchWidgets.neutralButton(ctx, this.font, px + PICK_X, ry, PICK_W, 14, "Pick", pickHover);
            if (i == pickingFor) {
                NotchWidgets.centerText(ctx, this.font, ">", px + PICK_X - 6, ry + 4,
                        NotchTheme.TEXT_DARK, false);
            }
            if (over(mouseX, mouseY, px + 12, ry, 56, 14)) tip = help(i);
            IdSuggest.draw(ctx, this.font, fields[i], IdSuggest.KIND_SOUND);
        }
        for (int i = 0; i < 6; i++) {
            IdSuggest.tip(ctx, this.font, fields[i], IdSuggest.KIND_SOUND, mouseX, mouseY);
        }

        int ay = py + ROW_Y + 6 * ROW_H + 4;
        NotchWidgets.divider(ctx, px + 8, ay - 6, W - 16);
        ctx.drawString(this.font, "Ambient every", px + 12, ay + 4, NotchTheme.TEXT_DARK, false);
        boolean minus = over(mouseX, mouseY, px + 88, ay, 16, 14);
        boolean plus = over(mouseX, mouseY, px + 140, ay, 16, 14);
        NotchWidgets.neutralButton(ctx, this.font, px + 88, ay, 16, 14, "-", minus);
        NotchWidgets.centerText(ctx, this.font,
                sounds.every() <= 0 ? "off" : sounds.every() + "s",
                px + 122, ay + 4, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.font, px + 140, ay, 16, 14, "+", plus);

        boolean modeHover = over(mouseX, mouseY, px + 166, ay, 68, 14);
        NotchWidgets.neutralButton(ctx, this.font, px + 166, ay, 68, 14,
                sounds.random() ? "Random" : "Exact", modeHover);
        if (modeHover) {
            tip = List.of(
                    Component.literal("Random or Exact").withStyle(ChatFormatting.WHITE),
                    Component.literal("Exact plays on the dot, good for a").withStyle(ChatFormatting.GRAY),
                    Component.literal("hammering blacksmith.").withStyle(ChatFormatting.GRAY),
                    Component.literal("Random varies the gap, good for chatter.").withStyle(ChatFormatting.GRAY));
        }
        if (minus || plus) {
            tip = List.of(
                    Component.literal("Ambient every").withStyle(ChatFormatting.WHITE),
                    Component.literal("The gap between ambient sounds.").withStyle(ChatFormatting.GRAY),
                    Component.literal("Set it to off to stop them.").withStyle(ChatFormatting.DARK_GRAY));
        }

        int fy = ay + 22;
        ctx.drawString(this.font, "Pitch", px + 12, fy + 4, NotchTheme.TEXT_DARK, false);
        boolean pDown = over(mouseX, mouseY, px + 88, fy, 16, 14);
        boolean pUp = over(mouseX, mouseY, px + 140, fy, 16, 14);
        NotchWidgets.neutralButton(ctx, this.font, px + 88, fy, 16, 14, "-", pDown);
        NotchWidgets.centerText(ctx, this.font, sounds.pitch() + "%",
                px + 122, fy + 4, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.font, px + 140, fy, 16, 14, "+", pUp);
        if (pDown || pUp) {
            tip = List.of(
                    Component.literal("Pitch").withStyle(ChatFormatting.WHITE),
                    Component.literal("Low for big and slow, high for small").withStyle(ChatFormatting.GRAY),
                    Component.literal("and quick. It shifts every sound here.").withStyle(ChatFormatting.GRAY));
        }

        int by = py + H - 26;
        boolean customHover = over(mouseX, mouseY, px + W - 108, fy, 96, 14);
        NotchWidgets.neutralButton(ctx, this.font, px + W - 108, fy, 96, 14, "Custom...", customHover);
        if (customHover) {
            tip = List.of(
                    Component.literal("Custom sounds").withStyle(ChatFormatting.WHITE),
                    Component.literal("Bring in your own .ogg files and share").withStyle(ChatFormatting.GRAY),
                    Component.literal("them with everyone on the server.").withStyle(ChatFormatting.GRAY));
        }
        NotchWidgets.primaryButton(ctx, this.font, px + W / 2 - 104, by, 100, 16, "Save",
                over(mouseX, mouseY, px + W / 2 - 104, by, 100, 16));
        NotchWidgets.neutralButton(ctx, this.font, px + W / 2 + 4, by, 100, 16, "Back",
                over(mouseX, mouseY, px + W / 2 + 4, by, 100, 16));

        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
        if (tip != null) ctx.renderComponentTooltip(this.font, tip, mouseX, mouseY);
    }

    private List<Component> help(int i) {
        return switch (i) {
            case 0 -> List.of(
                    Component.literal("Voice").withStyle(ChatFormatting.WHITE),
                    Component.literal("A short sound when it is spoken to,").withStyle(ChatFormatting.GRAY),
                    Component.literal("and on every line it says.").withStyle(ChatFormatting.GRAY));
            case 1 -> List.of(
                    Component.literal("Ambient").withStyle(ChatFormatting.WHITE),
                    Component.literal("Plays on a timer while it stands there.").withStyle(ChatFormatting.GRAY),
                    Component.literal("An anvil for a blacksmith, a crackle").withStyle(ChatFormatting.GRAY),
                    Component.literal("for a cook.").withStyle(ChatFormatting.GRAY));
            case 2 -> List.of(
                    Component.literal("Hurt").withStyle(ChatFormatting.WHITE),
                    Component.literal("Plays when it takes a hit.").withStyle(ChatFormatting.GRAY));
            case 3 -> List.of(
                    Component.literal("Death").withStyle(ChatFormatting.WHITE),
                    Component.literal("Plays when it dies.").withStyle(ChatFormatting.GRAY));
            case 4 -> List.of(
                    Component.literal("Step").withStyle(ChatFormatting.WHITE),
                    Component.literal("Replaces its footsteps. Metal makes").withStyle(ChatFormatting.GRAY),
                    Component.literal("it sound heavy.").withStyle(ChatFormatting.GRAY));
            default -> List.of(
                    Component.literal("Angry").withStyle(ChatFormatting.WHITE),
                    Component.literal("Plays when it lands a hit on something.").withStyle(ChatFormatting.GRAY));
        };
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
            for (int i = 0; i < 6; i++) {
                if (over(mx, my, px + PICK_X, rowY(i), PICK_W, 14)) {
                    NotchWidgets.click();
                    cycle(i);
                    pickingFor = i;
                    return true;
                }
                if (over(mx, my, px + BOX_X, rowY(i), BOX_W, 14)) pickingFor = i;
            }
            int ay = py + ROW_Y + 6 * ROW_H + 4;
            int step = net.fugginbeenus.notchcurrency.compat.Render.shiftDown() ? 10 : 1;
            if (over(mx, my, px + 88, ay, 16, 14)) {
                NotchWidgets.click();
                pullFields();
                sounds = sounds.withEvery(Math.max(0, sounds.every() - step));
                return true;
            }
            if (over(mx, my, px + 140, ay, 16, 14)) {
                NotchWidgets.click();
                pullFields();
                sounds = sounds.withEvery(Math.min(600, sounds.every() + step));
                return true;
            }
            if (over(mx, my, px + 166, ay, 68, 14)) {
                NotchWidgets.click();
                pullFields();
                sounds = sounds.withRandom(!sounds.random());
                return true;
            }
            int fy = ay + 22;
            int pStep = net.fugginbeenus.notchcurrency.compat.Render.shiftDown() ? 5 : 10;
            if (over(mx, my, px + 88, fy, 16, 14)) {
                NotchWidgets.click();
                pullFields();
                sounds = sounds.withPitch(Math.max(50, sounds.pitch() - pStep));
                return true;
            }
            if (over(mx, my, px + 140, fy, 16, 14)) {
                NotchWidgets.click();
                pullFields();
                sounds = sounds.withPitch(Math.min(200, sounds.pitch() + pStep));
                return true;
            }
            if (over(mx, my, px + W - 108, fy, 96, 14)) {
                NotchWidgets.click();
                pullFields();
                final NpcSoundsScreen self = this;
                final int slot = pickingFor;
                net.minecraft.client.Minecraft.getInstance().setScreen(
                        new net.fugginbeenus.notchcurrency.client.npcsound.NpcSoundManageScreen(
                                chosen -> {
                                    self.fields[Math.max(0, slot)].setValue(chosen);
                                    net.minecraft.client.Minecraft.getInstance().setScreen(self);
                                },
                                () -> net.minecraft.client.Minecraft.getInstance().setScreen(self)));
                return true;
            }
            int by = py + H - 26;
            if (over(mx, my, px + W / 2 - 104, by, 100, 16)) {
                NotchWidgets.click();
                save();
                return true;
            }
            if (over(mx, my, px + W / 2 + 4, by, 100, 16)) {
                NotchWidgets.click();
                pullFields();
                if (onBack != null) onBack.accept(sounds); else this.onClose();
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
        if (key == 258 || key == 262) {
            for (EditBox box : fields) {
                if (box != null && box.isFocused() && IdSuggest.accept(box, IdSuggest.KIND_SOUND)) {
                    return true;
                }
            }
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
