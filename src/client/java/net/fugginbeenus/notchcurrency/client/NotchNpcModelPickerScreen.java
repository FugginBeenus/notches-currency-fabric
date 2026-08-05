package net.fugginbeenus.notchcurrency.client;

import net.fabricmc.loader.api.FabricLoader;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.registry.ModEntities;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import org.jetbrains.annotations.Nullable;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Model picker for the NPC editor: a searchable, scrollable grid of live model previews. Includes the
 * Humanoid default, the APP.ly model (if installed), and every registered <b>living</b> entity type
 * (mobs/animals/monsters) as a disguise option. Selecting applies the model and returns to the editor.
 */
public class NotchNpcModelPickerScreen extends Screen {

    /**
     * One pickable model. The preview entity is built lazily on first draw and cached: building one
     * per registered entity type up front was the source of the open-the-picker lag on big modpacks.
     */
    private static final class Entry {
        final String id, label;
        @Nullable final EntityType<?> type;      // null => an NPC model (humanoid / APP.ly)
        @Nullable final String npcModel, npcSkin;
        @Nullable LivingEntity preview;          // cached once created

        Entry(String id, String label, @Nullable EntityType<?> type, @Nullable String npcModel, @Nullable String npcSkin) {
            this.id = id;
            this.label = label;
            this.type = type;
            this.npcModel = npcModel;
            this.npcSkin = npcSkin;
        }

        String id() { return id; }
        String label() { return label; }
    }

    private static final int W = 320, H = 250;
    private static final int COLS = 5, TILE_W = 60, TILE_H = 56;
    private static final int GRID_TOP = 48, VISIBLE_ROWS = 3;

    private final NotchNpcEditorScreen editor;
    private final List<Entry> all = new ArrayList<>();
    private List<Entry> filtered = new ArrayList<>();

    private int px, py, gridX, gridY;
    private TextFieldWidget search;
    private int scrollRow = 0;
    private boolean draggingScroll = false;

    public NotchNpcModelPickerScreen(NotchNpcEditorScreen editor) {
        super(Text.literal("Choose Model"));
        this.editor = editor;
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
        gridX = px + 10;
        gridY = py + GRID_TOP;

        if (all.isEmpty()) buildEntries();
        filtered = new ArrayList<>(all);

        search = new TextFieldWidget(this.textRenderer, px + 60, py + 29, W - 100, 12, Text.literal("Search"));
        search.setDrawsBackground(false);
        search.setChangedListener(this::refilter);
        addDrawableChild(search);
        setInitialFocus(search);
    }

    private void buildEntries() {
        // No entities are created here: just the list. Previews are built lazily as tiles are drawn.
        all.add(new Entry(NotchNpcEntity.MODEL_HUMANOID, "Humanoid", null, NotchNpcEntity.MODEL_HUMANOID, "1"));
        if (FabricLoader.getInstance().isModLoaded("apply")) {
            all.add(new Entry(NotchNpcEntity.MODEL_APPLY, "APP.ly", null, NotchNpcEntity.MODEL_APPLY, "default"));
        }
        List<Entry> mobs = new ArrayList<>();
        for (EntityType<?> type : Registries.ENTITY_TYPE) {
            Identifier id = Registries.ENTITY_TYPE.getId(type);
            if (id == null || !isLivingLike(type, id)) continue;
            mobs.add(new Entry("entity:" + id, type.getName().getString(), type, null, null));
        }
        mobs.sort((a, b) -> a.label().compareToIgnoreCase(b.label()));
        all.addAll(mobs);
    }

    /**
     * Worth offering as a model, judged without building one: building every registered type is what
     * used to make this screen crawl on a big modpack.
     *
     * <p>Spawn group is the cheap signal: living things have a real one, boats and arrows are MISC.
     * That misses two cases, so both are let through. Vanilla's armour stand is MISC but perfectly
     * usable. And mods routinely register bosses and their own NPCs as MISC precisely so they never
     * spawn on their own, excluding those would hide most of what a modpack has to offer. Anything
     * that slips through and isn't really a mob simply has no preview, and falls back to the humanoid
     * if it's picked.
     */
    private static boolean isLivingLike(EntityType<?> type, Identifier id) {
        if (type.getSpawnGroup() != SpawnGroup.MISC) return true;
        if (type == EntityType.ARMOR_STAND) return true;
        return !"minecraft".equals(id.getNamespace());
    }

    /** The preview entity for a tile, built + cached on first use (null if this type won't construct). */
    @Nullable
    private LivingEntity preview(Entry e) {
        if (e.preview != null) return e.preview;
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return null;
        try {
            if (e.type != null) {
                if (e.type.create(world) instanceof LivingEntity le) {
                    faceForward(le);
                    e.preview = le;
                }
            } else {
                e.preview = makeNpc(world, e.npcModel, e.npcSkin);
            }
        } catch (Exception ignored) {
            // A type that can't be built on the client stays without a preview (drawn as "?").
        }
        return e.preview;
    }

    private NotchNpcEntity makeNpc(ClientWorld world, String model, String skinValue) {
        NotchNpcEntity npc = new NotchNpcEntity(ModEntities.NOTCH_NPC, world);
        npc.setModelId(model);
        npc.setSkinType(NotchNpcEntity.MODEL_APPLY.equals(model) ? NotchNpcEntity.SKIN_VARIANT : NotchNpcEntity.SKIN_PRESET);
        npc.setSkinValue(skinValue);
        faceForward(npc);
        return npc;
    }

    private static void faceForward(LivingEntity le) {
        le.setYaw(180);
        le.prevYaw = 180;
        le.bodyYaw = le.prevBodyYaw = 180;
        le.headYaw = le.prevHeadYaw = 180;
    }

    private void refilter(String q) {
        String s = q == null ? "" : q.toLowerCase(Locale.ROOT).trim();
        filtered = new ArrayList<>();
        for (Entry e : all) {
            if (s.isEmpty() || e.label().toLowerCase(Locale.ROOT).contains(s) || e.id().toLowerCase(Locale.ROOT).contains(s)) {
                filtered.add(e);
            }
        }
        scrollRow = 0;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        //? if >=1.21 {
        /*renderInGameBackground(ctx);
        *///?} else {
        this.renderBackground(ctx);
        //?}
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.textRenderer, "Choose Model", px + W / 2, py + 8);

        ctx.drawText(this.textRenderer, "Search:", px + 12, py + 30, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, px + 56, py + 26, W - 92, 14, NotchTheme.DEEP);

        int totalRows = (filtered.size() + COLS - 1) / COLS;
        int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);
        if (scrollRow > maxScroll) scrollRow = maxScroll;

        int gridH = VISIBLE_ROWS * TILE_H;
        ctx.enableScissor(gridX, gridY, gridX + COLS * TILE_W, gridY + gridH);
        for (int i = 0; i < filtered.size(); i++) {
            int row = i / COLS - scrollRow;
            int col = i % COLS;
            if (row < 0 || row >= VISIBLE_ROWS) continue;
            int tx = gridX + col * TILE_W;
            int ty = gridY + row * TILE_H;
            drawTile(ctx, filtered.get(i), tx, ty, over(mouseX, mouseY, tx, ty, TILE_W, TILE_H));
        }
        ctx.disableScissor();

        // Scrollbar on the right of the grid.
        if (maxScroll > 0) {
            int sbX = gridX + COLS * TILE_W + 1, sbW = 6;
            NotchWidgets.inset(ctx, sbX, gridY, sbW, gridH, NotchTheme.DEEP);
            int thumbH = Math.max(14, gridH * VISIBLE_ROWS / totalRows);
            int thumbY = gridY + (gridH - thumbH) * scrollRow / maxScroll;
            NotchWidgets.button(ctx, sbX, thumbY, sbW, thumbH, over(mouseX, mouseY, sbX, gridY, sbW, gridH), false);
        }

        NotchWidgets.neutralButton(ctx, this.textRenderer, px + W / 2 - 40, py + H - 22, 80, 16, "Back",
                over(mouseX, mouseY, px + W / 2 - 40, py + H - 22, 80, 16));

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawTile(DrawContext ctx, Entry e, int tx, int ty, boolean hover) {
        NotchWidgets.inset(ctx, tx + 2, ty + 2, TILE_W - 4, TILE_H - 4, hover ? NotchTheme.SLOT_FILL : NotchTheme.DEEP);
        int cx = tx + TILE_W / 2;
        LivingEntity model = preview(e);
        try {
            if (model == null) throw new IllegalStateException("no preview");
            // Fit each preview inside its tile using BOTH dimensions (so wide/tall mobs don't bleed
            // into neighbouring tiles), leaving room for the label under it.
            float h = Math.max(0.5f, model.getHeight());
            float w = Math.max(0.5f, model.getWidth());
            int size = (int) Math.max(3, Math.min((TILE_H - 20) / h, (TILE_W - 10) / w));
            net.fugginbeenus.notchcurrency.compat.Render.drawEntityAt(ctx, cx, ty + TILE_H - 16, size, 0f, 0f, model);
        } catch (Exception ignored) {
            NotchWidgets.centerText(ctx, this.textRenderer, "?", cx, ty + 20, NotchTheme.TEXT_MUTED, false);
        }
        NotchWidgets.centerText(ctx, this.textRenderer, fit(e.label()), cx, ty + TILE_H - 12, NotchTheme.TEXT_DARK, false);
    }

    private String fit(String s) {
        return this.textRenderer.getWidth(s) <= TILE_W - 6 ? s : this.textRenderer.trimToWidth(s, TILE_W - 10) + "..";
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            if (over(mx, my, px + W / 2 - 40, py + H - 22, 80, 16)) {
                NotchWidgets.click();
                MinecraftClient.getInstance().setScreen(editor);
                return true;
            }
            // Scrollbar drag.
            int gridH = VISIBLE_ROWS * TILE_H, sbX = gridX + COLS * TILE_W + 1;
            if (mx >= sbX && mx < sbX + 6 && my >= gridY && my < gridY + gridH) {
                draggingScroll = true;
                scrollbarTo(my, gridH);
                return true;
            }
            if (mx >= gridX && mx < gridX + COLS * TILE_W && my >= gridY && my < gridY + VISIBLE_ROWS * TILE_H) {
                int col = (mx - gridX) / TILE_W;
                int row = (my - gridY) / TILE_H + scrollRow;
                int idx = row * COLS + col;
                if (idx >= 0 && idx < filtered.size()) {
                    NotchWidgets.click();
                    editor.applyModel(filtered.get(idx).id());
                    MinecraftClient.getInstance().setScreen(editor);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    //? if >=1.21 {
    /*public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double amount) {
    *///?} else {
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    //?}
        int totalRows = (filtered.size() + COLS - 1) / COLS;
        int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);
        scrollRow = Math.max(0, Math.min(maxScroll, scrollRow - (int) Math.signum(amount)));
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (draggingScroll) {
            scrollbarTo(mouseY, VISIBLE_ROWS * TILE_H);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingScroll = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /** Map a mouse-Y over the scrollbar track to a scroll row. */
    private void scrollbarTo(double my, int gridH) {
        int totalRows = (filtered.size() + COLS - 1) / COLS;
        int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);
        if (maxScroll == 0) { scrollRow = 0; return; }
        float t = (float) ((my - gridY) / gridH);
        scrollRow = Math.max(0, Math.min(maxScroll, Math.round(t * maxScroll)));
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
        // No 1.21 menu blur behind the mod's screens. They draw crisp panels over the world.
    }
    *///?}

    //? if >=1.21 {
    /*@Override
    public void renderBackground(net.minecraft.client.gui.DrawContext ctx, int mouseX, int mouseY, float delta) {
        // Drawn manually at the top of render(). This screen paints its panel after the darkening,
        // but the 1.21 base render would darken over the finished panel (super.render comes last here).
    }
    *///?}
}
