package net.fugginbeenus.notchcurrency.client.npcmodel;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.npcmodel.NpcModelRegistry;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Turns a Blockbench export into a model an NPC can wear, without anybody writing JSON.
 *
 * <p>Every field is a picker over what is actually in the import folder, or over the clips actually
 * in the animation file that was picked. There is nothing here to spell correctly: the failure this
 * design is built to avoid is typing a clip name that has to match exactly and getting no
 * explanation when it does not.
 *
 * <p>The checks run when Create is pressed, not when the files are loaded later, so a wrong export
 * mode or a mismatched texture is caught while the person who can fix it is still looking at it.
 */
public class NpcModelCreateScreen extends Screen {

    private static final int W = 300, H = 224;
    private static final int LABEL_X = 12, CTRL_X = 96, CTRL_W = 192, ROW_H = 16;
    private static final String NONE = "(none)";

    private final Screen parent;

    private int px, py;
    private EditBox nameBox;

    private List<String> models = List.of(), textures = List.of(), anims = List.of();
    private List<String> clips = List.of();
    private int modelAt, textureAt, animAt, idleAt, walkAt, specialAt;

    private String status = "";
    private boolean statusIsError;

    /** Which installed model the Remove side is pointed at, and whether it has been confirmed. */
    private int removeAt;
    private boolean confirmingRemove;

    public NpcModelCreateScreen(Screen parent) {
        super(Component.literal("NPC Models"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;

        String old = nameBox == null ? "" : nameBox.getValue();
        nameBox = new EditBox(this.font, px + CTRL_X + 4, py + rowY(0) + 4, CTRL_W - 8, 10,
                Component.literal("Name"));
        nameBox.setMaxLength(32);
        nameBox.setBordered(false);
        nameBox.setHint(Component.literal("Town Guard").withStyle(ChatFormatting.DARK_GRAY));
        nameBox.setValue(old);
        addRenderableWidget(nameBox);

        rescan();
    }

    private int rowY(int row) {
        return 30 + row * (ROW_H + 6);
    }

    /** Reads the import folder again, keeping the current picks where the files still exist. */
    private void rescan() {
        String model = pick(models, modelAt), texture = pick(textures, textureAt);
        String anim = pick(anims, animAt);

        models = filesIn(name -> name.endsWith(".geo.json"));
        textures = filesIn(name -> name.endsWith(".png"));

        List<String> foundAnims = filesIn(name ->
                (name.endsWith(".json") && !name.endsWith(".geo.json")));
        anims = new ArrayList<>();
        anims.add(NONE);
        anims.addAll(foundAnims);

        modelAt = Math.max(0, models.indexOf(model));
        textureAt = Math.max(0, textures.indexOf(texture));
        animAt = Math.max(0, anims.indexOf(anim));
        reloadClips();
    }

    /** The clips inside whichever animation file is currently picked. */
    private void reloadClips() {
        String anim = pick(anims, animAt);
        List<String> found = new ArrayList<>();
        found.add(NONE);
        if (anim != null && !NONE.equals(anim)) {
            found.addAll(NpcModelLoader.clipsIn(NpcModelLoader.importDir().resolve(anim)));
        }
        clips = found;
        idleAt = Math.min(idleAt, clips.size() - 1);
        walkAt = Math.min(walkAt, clips.size() - 1);
        specialAt = Math.min(specialAt, clips.size() - 1);

        // A file with clips named the obvious way should not need three more clicks.
        if (idleAt == 0) idleAt = Math.max(0, indexEndingWith(clips, "idle"));
        if (walkAt == 0) walkAt = Math.max(0, indexEndingWith(clips, "walk"));
    }

    private static int indexEndingWith(List<String> list, String tail) {
        for (int i = 0; i < list.size(); i++) {
            String value = list.get(i);
            if (value.equals(tail) || value.endsWith("." + tail)) return i;
        }
        return 0;
    }

    private List<String> filesIn(java.util.function.Predicate<String> matches) {
        List<String> out = new ArrayList<>();
        try (Stream<Path> files = Files.list(NpcModelLoader.importDir())) {
            files.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(matches)
                    .sorted(Comparator.naturalOrder())
                    .forEach(out::add);
        } catch (Exception noFolder) {
            // Nothing dropped in yet, which is not an error.
        }
        return out;
    }

    private static String pick(List<String> list, int at) {
        return list.isEmpty() ? null : list.get(Math.floorMod(at, list.size()));
    }

    /** What the folder will be called, worked out from the name so there is no second field. */
    private String derivedId() {
        String name = nameBox == null ? "" : nameBox.getValue().strip().toLowerCase(Locale.ROOT);
        StringBuilder id = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (Character.isLetterOrDigit(c) && c < 128) id.append(c);
            else if (c == ' ' || c == '_' || c == '-') id.append('_');
        }
        while (id.length() > 0 && id.charAt(0) == '_') id.deleteCharAt(0);
        return id.toString();
    }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    //?}
        //? if >=1.21 {
        /*renderTransparentBackground(ctx);
        *///?} else {
        this.renderBackground(ctx);
        //?}
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.font, "NPC Models", px + W / 2, py + 8);

        row(ctx, 0, "Name", null, mouseX, mouseY);
        NotchWidgets.inset(ctx, px + CTRL_X, py + rowY(0), CTRL_W, 14, NotchTheme.DEEP);

        String id = derivedId();
        ctx.drawString(this.font, id.isEmpty() ? "" : "saves as: " + id,
                px + CTRL_X, py + rowY(0) + 16, NotchTheme.TEXT_MUTED, false);

        row(ctx, 1, "Model", label(models, modelAt, "no .geo.json in _import"), mouseX, mouseY);
        row(ctx, 2, "Texture", label(textures, textureAt, "no .png in _import"), mouseX, mouseY);
        row(ctx, 3, "Animations", label(anims, animAt, NONE), mouseX, mouseY);
        row(ctx, 4, "Idle", label(clips, idleAt, NONE), mouseX, mouseY);
        row(ctx, 5, "Walk", label(clips, walkAt, NONE), mouseX, mouseY);
        row(ctx, 6, "Flourish", label(clips, specialAt, NONE), mouseX, mouseY);

        if (!status.isEmpty()) {
            for (String line : wrap(status, W - 24)) {
                ctx.drawString(this.font, line, px + LABEL_X, py + 156,
                        statusIsError ? 0xFFD05A5A : 0xFF6AC46A, false);
                break; // one line, the rest is in the log
            }
        }
        drawRemoveRow(ctx, mouseX, mouseY);

        int by = py + H - 26;
        NotchWidgets.neutralButton(ctx, this.font, px + 12, by, 76, 16, "Import folder",
                over(mouseX, mouseY, px + 12, by, 76, 16));
        NotchWidgets.neutralButton(ctx, this.font, px + 92, by, 52, 16, "Refresh",
                over(mouseX, mouseY, px + 92, by, 52, 16));
        NotchWidgets.neutralButton(ctx, this.font, px + 148, by, 60, 16, "Cancel",
                over(mouseX, mouseY, px + 148, by, 60, 16));
        NotchWidgets.primaryButton(ctx, this.font, px + 212, by, 76, 16, "Create",
                over(mouseX, mouseY, px + 212, by, 76, 16));

        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
    }

    /** Removing an installed model, kept on the same screen because it is the same subject. */
    private void drawRemoveRow(GuiGraphics ctx, int mouseX, int mouseY) {
        var installed = NpcModelRegistry.all();
        if (installed.isEmpty()) {
            ctx.drawString(this.font, "Put Blockbench exports in the import folder, then Refresh.",
                    px + LABEL_X, py + 172, NotchTheme.TEXT_MUTED, false);
            return;
        }

        var bundle = installed.get(Math.floorMod(removeAt, installed.size()));
        ctx.drawString(this.font, "Installed", px + LABEL_X, py + 172, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.font, px + CTRL_X, py + 168, CTRL_W - 62, 14,
                fit(bundle.displayName(), CTRL_W - 70),
                over(mouseX, mouseY, px + CTRL_X, py + 168, CTRL_W - 62, 14));
        int rx = px + CTRL_X + CTRL_W - 58;
        NotchWidgets.dangerButton(ctx, this.font, rx, py + 168, 58, 14,
                confirmingRemove ? "Sure?" : "Remove", over(mouseX, mouseY, rx, py + 168, 58, 14));
    }

    private void row(GuiGraphics ctx, int index, String label, String value, int mouseX, int mouseY) {
        int y = py + rowY(index);
        ctx.drawString(this.font, label, px + LABEL_X, y + 4, NotchTheme.TEXT_DARK, false);
        if (value == null) return; // the name row draws its own box
        NotchWidgets.neutralButton(ctx, this.font, px + CTRL_X, y, CTRL_W, 14,
                fit(value, CTRL_W - 8), over(mouseX, mouseY, px + CTRL_X, y, CTRL_W, 14));
    }

    private String label(List<String> list, int at, String whenEmpty) {
        String value = pick(list, at);
        return value == null ? whenEmpty : value;
    }

    private String fit(String text, int room) {
        return this.font.width(text) <= room ? text : this.font.plainSubstrByWidth(text, room - 6) + "..";
    }

    private List<String> wrap(String text, int room) {
        return List.of(this.font.width(text) <= room
                ? text : this.font.plainSubstrByWidth(text, room - 6) + "..");
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
        int step = button == 1 ? -1 : 1; // right-click walks back through a long list

        if (button == 0 || button == 1) {
            if (rowHit(mx, my, 1)) { modelAt += step; return click(); }
            if (rowHit(mx, my, 2)) { textureAt += step; return click(); }
            if (rowHit(mx, my, 3)) { animAt += step; reloadClips(); return click(); }
            if (rowHit(mx, my, 4)) { idleAt += step; return click(); }
            if (rowHit(mx, my, 5)) { walkAt += step; return click(); }
            if (rowHit(mx, my, 6)) { specialAt += step; return click(); }
        }

        var installed = NpcModelRegistry.all();
        if ((button == 0 || button == 1) && !installed.isEmpty()
                && over(mx, my, px + CTRL_X, py + 168, CTRL_W - 62, 14)) {
            removeAt += step;
            confirmingRemove = false;
            return click();
        }
        if (button == 0 && !installed.isEmpty()
                && over(mx, my, px + CTRL_X + CTRL_W - 58, py + 168, 58, 14)) {
            removeSelected(installed);
            return click();
        }

        if (button == 0) {
            int by = py + H - 26;
            if (over(mx, my, px + 12, by, 76, 16)) {
                openImportFolder();
                return click();
            }
            if (over(mx, my, px + 92, by, 52, 16)) {
                rescan();
                setStatus("Looked again.", false);
                return click();
            }
            if (over(mx, my, px + 148, by, 60, 16)) {
                Minecraft.getInstance().setScreen(parent);
                return true;
            }
            if (over(mx, my, px + 212, by, 76, 16)) {
                create();
                return click();
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private boolean rowHit(int mx, int my, int index) {
        return over(mx, my, px + CTRL_X, py + rowY(index), CTRL_W, 14);
    }

    private boolean click() {
        NotchWidgets.click();
        return true;
    }

    /** Opens the folder in the desktop's own file browser, as vanilla does for resource packs. */
    private void openImportFolder() {
        try {
            Files.createDirectories(NpcModelLoader.importDir());
            // Util moved package at 1.21.11, not at 26 like most of the rest.
            //? if >=1.21.11 {
            /*net.minecraft.util.Util.getPlatform().openPath(NpcModelLoader.importDir());
            *///?} else {
            net.minecraft.Util.getPlatform().openFile(NpcModelLoader.importDir().toFile());
            //?}
        } catch (Exception e) {
            setStatus("Could not open the folder: " + e.getMessage(), true);
        }
    }

    private void create() {
        String name = nameBox.getValue().strip();
        String id = derivedId();
        String model = pick(models, modelAt);
        String texture = pick(textures, textureAt);
        String anim = pick(anims, animAt);
        String idle = pick(clips, idleAt), walk = pick(clips, walkAt), special = pick(clips, specialAt);

        if (name.isEmpty() || id.isEmpty()) {
            setStatus("Give it a name first.", true);
            return;
        }
        if (NpcModelRegistry.forModelId("npc:" + id) != null
                || Files.isDirectory(NpcModelLoader.modelsDir().resolve(id))) {
            setStatus("A model called " + id + " already exists.", true);
            return;
        }
        if (model == null) {
            setStatus("No model chosen. Put a .geo.json in the import folder.", true);
            return;
        }
        if (texture == null) {
            setStatus("No texture chosen. Put a .png in the import folder.", true);
            return;
        }

        Path importDir = NpcModelLoader.importDir();
        String problem = NpcModelLoader.problemWith(importDir.resolve(model), importDir.resolve(texture));
        if (problem != null) {
            setStatus(problem, true);
            return;
        }

        try {
            Path folder = NpcModelLoader.modelsDir().resolve(id);
            Files.createDirectories(folder);
            Files.copy(importDir.resolve(model), folder.resolve("model.geo.json"),
                    StandardCopyOption.REPLACE_EXISTING);
            Files.copy(importDir.resolve(texture), folder.resolve("texture.png"),
                    StandardCopyOption.REPLACE_EXISTING);
            if (anim != null && !NONE.equals(anim)) {
                Files.copy(importDir.resolve(anim), folder.resolve("animation.json"),
                        StandardCopyOption.REPLACE_EXISTING);
            }
            Files.writeString(folder.resolve("npc.json"), manifest(name, idle, walk, special));
        } catch (Exception e) {
            setStatus("Could not write the model: " + e.getMessage(), true);
            return;
        }

        NpcModelPacks.reload(Minecraft.getInstance(), true);
        net.fugginbeenus.notchcurrency.client.NotchNpcModelPickerScreen.markStale();
        Minecraft.getInstance().setScreen(parent);
    }

    /**
     * Takes a model out, on the second click.
     *
     * <p>Two clicks because there is no undo: the folder goes, and a model somebody spent an evening
     * on should not be one misplaced click away from gone.
     */
    private void removeSelected(java.util.List<net.fugginbeenus.notchcurrency.npcmodel.NpcModelBundle> installed) {
        var bundle = installed.get(Math.floorMod(removeAt, installed.size()));
        if (!confirmingRemove) {
            confirmingRemove = true;
            setStatus("Click Remove again to delete " + bundle.displayName() + ".", false);
            return;
        }
        confirmingRemove = false;

        String problem = NpcModelLoader.delete(bundle.id());
        if (problem != null) {
            setStatus("Could not remove it: " + problem, true);
            return;
        }
        NpcModelPacks.reload(Minecraft.getInstance(), false);
        net.fugginbeenus.notchcurrency.client.NotchNpcModelPickerScreen.markStale();
        removeAt = 0;
        setStatus("Removed " + bundle.displayName() + ". Any NPC wearing it falls back.", false);
    }

    private static String manifest(String name, String idle, String walk, String special) {
        StringBuilder json = new StringBuilder("{\n");
        json.append("  \"format\": 1,\n");
        json.append("  \"name\": \"").append(name.replace("\"", "")).append("\",\n");
        json.append("  \"clips\": {\n");
        json.append("    \"idle\": \"").append(clean(idle)).append("\",\n");
        json.append("    \"walk\": \"").append(clean(walk)).append("\",\n");
        json.append("    \"special\": [");
        if (!clean(special).isEmpty()) json.append('"').append(clean(special)).append('"');
        json.append("]\n  }\n}\n");
        return json.toString();
    }

    private static String clean(String clip) {
        return clip == null || NONE.equals(clip) ? "" : clip;
    }

    private void setStatus(String line, boolean error) {
        status = line;
        statusIsError = error;
    }

    private static boolean over(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
    *///?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, nameBox)) return true;
        //? if >=1.21.11 {
        /*return super.keyPressed(event);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    //? if >=1.21.11 {
    /*@Override
    protected void renderBlurredBackground(GuiGraphics ctx) {
        // No menu blur behind the mod's screens.
    }
    *///?} elif >=1.21 {
    /*@Override
    protected void renderBlurredBackground(float delta) {
    }
    *///?}
}
