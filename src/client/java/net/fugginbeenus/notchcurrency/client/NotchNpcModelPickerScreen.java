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
import net.minecraft.registry.Registries;
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

    private record Entry(String id, String label, LivingEntity preview) {}

    private static final int W = 320, H = 250;
    private static final int COLS = 5, TILE_W = 60, TILE_H = 56;
    private static final int GRID_TOP = 48, VISIBLE_ROWS = 3;

    private final NotchNpcEditorScreen editor;
    private final List<Entry> all = new ArrayList<>();
    private List<Entry> filtered = new ArrayList<>();

    private int px, py, gridX, gridY;
    private TextFieldWidget search;
    private int scrollRow = 0;

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
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) return;
        all.add(new Entry(NotchNpcEntity.MODEL_HUMANOID, "Humanoid", makeNpc(world, NotchNpcEntity.MODEL_HUMANOID, "1")));
        if (FabricLoader.getInstance().isModLoaded("apply")) {
            all.add(new Entry(NotchNpcEntity.MODEL_APPLY, "APP.ly", makeNpc(world, NotchNpcEntity.MODEL_APPLY, "default")));
        }
        List<Entry> mobs = new ArrayList<>();
        for (EntityType<?> type : Registries.ENTITY_TYPE) {
            Identifier id = Registries.ENTITY_TYPE.getId(type);
            if (id == null) continue;
            try {
                Entity e = type.create(world);
                if (e instanceof LivingEntity le) {
                    faceForward(le);
                    mobs.add(new Entry("entity:" + id, type.getName().getString(), le));
                }
            } catch (Exception ignored) {
                // Skip entity types that can't be constructed on the client.
            }
        }
        mobs.sort((a, b) -> a.label().compareToIgnoreCase(b.label()));
        all.addAll(mobs);
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
        this.renderBackground(ctx);
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

        if (maxScroll > 0) {
            NotchWidgets.centerText(ctx, this.textRenderer, "scroll — " + (scrollRow + 1) + "/" + (maxScroll + 1),
                    px + W / 2, py + GRID_TOP + gridH + 3, NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.neutralButton(ctx, this.textRenderer, px + W / 2 - 40, py + H - 22, 80, 16, "Back",
                over(mouseX, mouseY, px + W / 2 - 40, py + H - 22, 80, 16));

        super.render(ctx, mouseX, mouseY, delta);
    }

    private void drawTile(DrawContext ctx, Entry e, int tx, int ty, boolean hover) {
        NotchWidgets.inset(ctx, tx + 2, ty + 2, TILE_W - 4, TILE_H - 4, hover ? NotchTheme.SLOT_FILL : NotchTheme.DEEP);
        int cx = tx + TILE_W / 2;
        try {
            // Scale each preview to its bounding box so big mobs don't overflow their tile.
            float dim = Math.max(0.5f, Math.max(e.preview().getHeight(), e.preview().getWidth()));
            int size = (int) Math.max(4, Math.min(30, 26f / dim));
            InventoryScreen.drawEntity(ctx, cx, ty + TILE_H - 15, size, 0f, 0f, e.preview());
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
                MinecraftClient.getInstance().setScreen(editor);
                return true;
            }
            if (mx >= gridX && mx < gridX + COLS * TILE_W && my >= gridY && my < gridY + VISIBLE_ROWS * TILE_H) {
                int col = (mx - gridX) / TILE_W;
                int row = (my - gridY) / TILE_H + scrollRow;
                int idx = row * COLS + col;
                if (idx >= 0 && idx < filtered.size()) {
                    editor.applyModel(filtered.get(idx).id());
                    MinecraftClient.getInstance().setScreen(editor);
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        int totalRows = (filtered.size() + COLS - 1) / COLS;
        int maxScroll = Math.max(0, totalRows - VISIBLE_ROWS);
        scrollRow = Math.max(0, Math.min(maxScroll, scrollRow - (int) Math.signum(amount)));
        return true;
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
