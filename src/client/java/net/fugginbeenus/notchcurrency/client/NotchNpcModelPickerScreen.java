package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.fugginbeenus.notchcurrency.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NotchNpcModelPickerScreen extends Screen {

    private static final class Entry {
        final String id, label;
        @Nullable final EntityType<?> type;
        @Nullable final String npcModel, npcSkin;
        @Nullable LivingEntity preview;

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
    private static boolean stale = false;
    public static void markStale() {
        stale = true;
    }
    private final NotchNpcEditorScreen editor;
    private final List<Entry> all = new ArrayList<>();
    private List<Entry> filtered = new ArrayList<>();
    private int px, py, gridX, gridY;
    private EditBox search;
    private int scrollRow = 0;
    private boolean draggingScroll = false;

    public NotchNpcModelPickerScreen(NotchNpcEditorScreen editor) {
        super(Component.literal("Choose Model"));
        this.editor = editor;
    }

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;
        gridX = px + 10;
        gridY = py + GRID_TOP;

        if (all.isEmpty() || stale) {
            all.clear();
            buildEntries();
            stale = false;
        }
        filtered = new ArrayList<>(all);

        search = new EditBox(this.font, px + 60, py + 29, W - 100, 12, Component.literal("Search"));
        search.setBordered(false);
        search.setResponder(this::refilter);
        addRenderableWidget(search);
        setInitialFocus(search);
    }

    private void buildEntries() {
        all.add(new Entry(NotchNpcEntity.MODEL_HUMANOID, "Humanoid", null, NotchNpcEntity.MODEL_HUMANOID, "1"));
        all.add(new Entry(NotchNpcEntity.MODEL_APPLY, "APP.ly", null, NotchNpcEntity.MODEL_APPLY, "default"));
        for (var bundle : net.fugginbeenus.notchcurrency.npcmodel.NpcModelRegistry.all()) {
            String modelId = net.fugginbeenus.notchcurrency.npcmodel.NpcModelBundle.modelIdFor(bundle.id());
            all.add(new Entry(modelId, bundle.displayName(), null, modelId, "default"));
        }
        List<Entry> mobs = new ArrayList<>();
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
            if (id == null || !isLivingLike(type, id)) continue;
            mobs.add(new Entry("entity:" + id, type.getDescription().getString(), type, null, null));
        }
        mobs.sort((a, b) -> a.label().compareToIgnoreCase(b.label()));
        all.addAll(mobs);
    }

    private static boolean isLivingLike(EntityType<?> type, ResourceLocation id) {
        if (type.getCategory() != MobCategory.MISC) return true;
        if (type == EntityType.ARMOR_STAND) return true;
        return !"minecraft".equals(id.getNamespace());
    }

    @Nullable
    private LivingEntity preview(Entry e) {
        if (e.preview != null) return e.preview;
        ClientLevel world = Minecraft.getInstance().level;
        if (world == null) return null;
        try {
            if (e.type != null) {
                if (net.fugginbeenus.notchcurrency.compat.Render.createDetached(world, e.type)
                        instanceof LivingEntity le) {
                    faceForward(le);
                    e.preview = le;
                }
            } else {
                e.preview = makeNpc(world, e.npcModel, e.npcSkin);
            }
        } catch (Exception ignored) {
        }
        return e.preview;
    }

    private NotchNpcEntity makeNpc(ClientLevel world, String model, String skinValue) {
        NotchNpcEntity npc = new NotchNpcEntity(ModEntities.NOTCH_NPC, world);
        npc.setModelId(model);
        npc.setSkinType(NotchNpcEntity.MODEL_APPLY.equals(model) ? NotchNpcEntity.SKIN_VARIANT : NotchNpcEntity.SKIN_PRESET);
        npc.setSkinValue(skinValue);
        faceForward(npc);
        return npc;
    }

    private static void faceForward(LivingEntity le) {
        le.setId(-1);
        le.setYRot(180);
        le.yRotO = 180;
        le.yBodyRot = le.yBodyRotO = 180;
        le.yHeadRot = le.yHeadRotO = 180;
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
        NotchWidgets.title(ctx, this.font, "Choose Model", px + W / 2, py + 8);

        ctx.drawString(this.font, "Search:", px + 12, py + 30, NotchTheme.TEXT_DARK, false);
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
            int sbX = gridX + COLS * TILE_W + 1, sbW = 6;
            NotchWidgets.inset(ctx, sbX, gridY, sbW, gridH, NotchTheme.DEEP);
            int thumbH = Math.max(14, gridH * VISIBLE_ROWS / totalRows);
            int thumbY = gridY + (gridH - thumbH) * scrollRow / maxScroll;
            NotchWidgets.button(ctx, sbX, thumbY, sbW, thumbH, over(mouseX, mouseY, sbX, gridY, sbW, gridH), false);
        }

        NotchWidgets.primaryButton(ctx, this.font, px + W / 2 + 4, py + H - 22, 90, 16, "Manage models",
                over(mouseX, mouseY, px + W / 2 + 4, py + H - 22, 90, 16));
        NotchWidgets.neutralButton(ctx, this.font, px + W / 2 - 94, py + H - 22, 90, 16, "Back",
                over(mouseX, mouseY, px + W / 2 - 94, py + H - 22, 90, 16));

        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
    }

    private void drawTile(GuiGraphics ctx, Entry e, int tx, int ty, boolean hover) {
        NotchWidgets.inset(ctx, tx + 2, ty + 2, TILE_W - 4, TILE_H - 4, hover ? NotchTheme.SLOT_FILL : NotchTheme.DEEP);
        int cx = tx + TILE_W / 2;
        LivingEntity model = preview(e);
        try {
            if (model == null) throw new IllegalStateException("no preview");
            float extent = Math.max(0.5f, Math.max(model.getBbHeight(), model.getBbWidth()));
            int size = (int) Math.max(3, Math.min((TILE_H - 22) / extent, (TILE_W - 12) / extent));
            net.fugginbeenus.notchcurrency.compat.Render.drawEntityAt(ctx, cx, ty + TILE_H - 16, size, 0f, 0f, model);
        } catch (Exception ignored) {
            NotchWidgets.centerText(ctx, this.font, "?", cx, ty + 20, NotchTheme.TEXT_MUTED, false);
        }
        NotchWidgets.centerText(ctx, this.font, fit(e.label()), cx, ty + TILE_H - 12, NotchTheme.TEXT_DARK, false);
    }

    private String fit(String s) {
        return this.font.width(s) <= TILE_W - 6 ? s : this.font.plainSubstrByWidth(s, TILE_W - 10) + "..";
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
            if (over(mx, my, px + W / 2 + 4, py + H - 22, 90, 16)) {
                NotchWidgets.click();
                Minecraft.getInstance().setScreen(
                        new net.fugginbeenus.notchcurrency.client.npcmodel.NpcModelManageScreen(this));
                return true;
            }
            if (over(mx, my, px + W / 2 - 94, py + H - 22, 90, 16)) {
                NotchWidgets.click();
                Minecraft.getInstance().setScreen(editor);
                return true;
            }
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
                    Minecraft.getInstance().setScreen(editor);
                    return true;
                }
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
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

    //? if >=1.21.11 {
    /*@Override
    public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
    *///?} else {
    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
    //?}
        if (draggingScroll) {
            scrollbarTo(mouseY, VISIBLE_ROWS * TILE_H);
            return true;
        }
        //? if >=1.21.11 {
        /*return super.mouseDragged(event, dx, dy);
        *///?} else {
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
        //?}
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        double mouseX = event.x(), mouseY = event.y();
        int button = event.button();
    *///?} else {
    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
    //?}
        draggingScroll = false;
        //? if >=1.21.11 {
        /*return super.mouseReleased(event);
        *///?} else {
        return super.mouseReleased(mouseX, mouseY, button);
        //?}
    }

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
    public boolean isPauseScreen() {
        return false;
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

    //? if >=1.21 {
    /*@Override
    public void renderBackground(net.minecraft.client.gui.GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    }
    *///?}
}
