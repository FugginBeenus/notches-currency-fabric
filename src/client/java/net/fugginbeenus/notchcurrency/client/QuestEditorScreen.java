package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyRarity;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyType;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;

public class QuestEditorScreen extends Screen {

    private static final int W = 300, H = 296;
    private static final int LX = 12, FX = 96, FW = 150;

    public static java.util.UUID cameFromNpc = null;
    public static boolean cameFromDesigner = false;

    private final String key;
    private BountyType type = BountyType.KILL;
    private boolean repeatable;
    private boolean handIn;
    private boolean factionOnly;
    private EditBox factionField;
    private EditBox rewardItemField;
    private EditBox rewardCountField;
    private EditBox nextField;
    private int px, py;

    private EditBox targetField;
    private EditBox npcField;
    private EditBox countField;
    private EditBox coinsField;
    private EditBox descField;
    private EditBox needsField;
    private EditBox radiusField;

    private final java.util.List<String> allQuests;

    public QuestEditorScreen(String key, CompoundTag existing, java.util.List<String> allQuests) {
        super(Component.literal("Quest"));
        this.allQuests = allQuests == null ? java.util.List.of() : allQuests;
        this.key = key == null ? "" : key;
        if (existing != null) {
            try {
                this.type = BountyType.valueOf(existing.getString("Type"));
            } catch (IllegalArgumentException unknownType) {
                this.type = BountyType.KILL;
            }
            this.repeatable = existing.getBoolean("Repeatable");
            this.handIn = existing.getBoolean("HandIn");
            this.factionOnly = existing.getBoolean("FactionOnly") || !existing.getString("NeedsFaction").isBlank();
            this.loadFaction = existing.getString("NeedsFaction");
            this.loadNext = existing.getString("NextQuest");
            if (existing.contains("RewardItem")) {
                var st = net.fugginbeenus.notchcurrency.compat.StackData.readStack(existing.getCompound("RewardItem"));
                if (!st.isEmpty()) {
                    this.loadRewardItem = net.minecraft.core.registries.BuiltInRegistries.ITEM
                            .getKey(st.getItem()).toString();
                    this.loadRewardCount = st.getCount();
                }
            }
            this.loadTarget = existing.getString("Target");
            this.loadNpc = existing.getString("TargetText");
            this.loadCount = existing.getInt("Required");
            this.loadCoins = existing.getLong("RewardCoins");
            this.loadDesc = existing.getString("Desc");
            this.loadNeeds = existing.getString("NeedsQuest");
        }
    }

    private String loadTarget = "", loadNpc = "", loadDesc = "", loadNeeds = "", loadFaction = "", loadNext = "", loadRewardItem = "";
    private int loadRewardCount = 1;
    private int loadCount = 1;
    private long loadCoins = 0L;

    @Override
    protected void init() {
        px = (this.width - W) / 2;
        py = (this.height - H) / 2;

        targetField = field(py + 68, FW, loadTarget, "minecraft:zombie");
        npcField = field(py + 88, FW - 44, loadNpc, "who, or x y z r");
        countField = field(py + 108, 60, loadCount <= 0 ? "1" : Integer.toString(loadCount), "1");
        coinsField = fieldAt(px + FX, py + 128, 44, loadCoins <= 0 ? "" : Long.toString(loadCoins), "0");
        descField = field(py + 148, FW, loadDesc, "shown to the player");
        needsField = field(py + 168, FW - 44, loadNeeds, "another quest");
        factionField = field(py + 216, 56, loadFaction, "any");
        rewardItemField = fieldAt(px + FX + 48, py + 128, 66, loadRewardItem, "item id");
        rewardCountField = fieldAt(px + FX + 118, py + 128, 32,
                loadRewardCount <= 0 ? "" : Integer.toString(loadRewardCount), "1");
        nextField = field(py + 236, FW - 44, loadNext, "quest that follows");
        radiusField = field(py + 108, 60, loadRadius(), "6");
        syncFields();
    }

    private EditBox field(int y, int w, String value, String hint) {
        return fieldAt(px + FX, y, w, value, hint);
    }

    private EditBox fieldAt(int x, int y, int w, String value, String hint) {
        EditBox box = new EditBox(this.font, x + 3, y + 3, w - 6, 10, Component.empty());
        box.setMaxLength(120);
        box.setBordered(false);
        box.setHint(Component.literal(hint).withStyle(net.minecraft.ChatFormatting.DARK_GRAY));
        box.setValue(value == null ? "" : value);
        addRenderableWidget(box);
        return box;
    }

    private void syncFields() {
        targetField.setVisible(type != BountyType.TALK_TO && type != BountyType.VISIT);
        npcField.setVisible(type.usesNpc() || type == BountyType.VISIT);
        countField.setVisible(type != BountyType.TALK_TO && type != BountyType.VISIT);
        radiusField.setVisible(type == BountyType.VISIT);
        factionField.setVisible(factionOnly);
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
        NotchWidgets.panel(ctx, px, py, W, H);
        NotchWidgets.title(ctx, this.font, "Quest", px + W / 2, py + 8);
        NotchWidgets.centerText(ctx, this.font, key.isBlank() ? "(no name)" : key,
                px + W / 2, py + 22, NotchTheme.TEXT_MUTED, false);
        NotchWidgets.divider(ctx, px + 8, py + 34, W - 16);

        ctx.drawString(this.font, "Goal:", px + LX, py + 48, NotchTheme.TEXT_DARK, false);
        NotchWidgets.neutralButton(ctx, this.font, px + FX, py + 44, FW, 14, type.label(),
                over(mouseX, mouseY, px + FX, py + 44, FW, 14));

        if (targetField.isVisible()) {
            ctx.drawString(this.font, type.usesItem() ? "Item:" : "Mob:", px + LX, py + 72,
                    NotchTheme.TEXT_DARK, false);
            NotchWidgets.inset(ctx, px + FX, py + 68, FW, 14, NotchTheme.DEEP);
        }
        if (npcField.isVisible()) {
            ctx.drawString(this.font, type == BountyType.VISIT ? "Spot:" : "NPC:", px + LX, py + 92,
                    NotchTheme.TEXT_DARK, false);
            NotchWidgets.inset(ctx, px + FX, py + 88, FW - 44, 14, NotchTheme.DEEP);
            NotchWidgets.neutralButton(ctx, this.font, px + FX + FW - 40, py + 88, 40, 14,
                    type == BountyType.VISIT ? "Here" : "Pick",
                    over(mouseX, mouseY, px + FX + FW - 40, py + 88, 40, 14));
        }
        if (countField.isVisible()) {
            ctx.drawString(this.font, "How many:", px + LX, py + 112, NotchTheme.TEXT_DARK, false);
            NotchWidgets.inset(ctx, px + FX, py + 108, 60, 14, NotchTheme.DEEP);
        }
        if (radiusField.isVisible()) {
            ctx.drawString(this.font, "How close:", px + LX, py + 112, NotchTheme.TEXT_DARK, false);
            NotchWidgets.inset(ctx, px + FX, py + 108, 60, 14, NotchTheme.DEEP);
            ctx.drawString(this.font, "blocks", px + FX + 66, py + 112, NotchTheme.TEXT_MUTED, false);
        }
        ctx.drawString(this.font, "Pays:", px + LX, py + 132, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, px + FX, py + 128, 44, 14, NotchTheme.DEEP);
        NotchWidgets.inset(ctx, px + FX + 48, py + 128, 66, 14, NotchTheme.DEEP);
        NotchWidgets.inset(ctx, px + FX + 118, py + 128, 32, 14, NotchTheme.DEEP);
        if (over(mouseX, mouseY, px + LX, py + 128, FX + FW - LX, 14)) {
            ctx.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("Pays").withStyle(net.minecraft.ChatFormatting.WHITE),
                    Component.literal("Coins, then an item id, then how many of it.").withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.literal("Leave the item empty to pay coins only.").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)),
                    mouseX, mouseY);
        }
        ctx.drawString(this.font, "Text:", px + LX, py + 152, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, px + FX, py + 148, FW, 14, NotchTheme.DEEP);
        ctx.drawString(this.font, "Needs:", px + LX, py + 172, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, px + FX, py + 168, FW - 44, 14, NotchTheme.DEEP);
        NotchWidgets.neutralButton(ctx, this.font, px + FX + FW - 40, py + 168, 40, 14, "Pick",
                over(mouseX, mouseY, px + FX + FW - 40, py + 168, 40, 14));
        if (over(mouseX, mouseY, px + LX, py + 168, FX + FW - LX, 14)) {
            ctx.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("Needs").withStyle(net.minecraft.ChatFormatting.WHITE),
                    Component.literal("Name another quest and this one is only given").withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.literal("once the player has finished that one.").withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.literal("Leave it empty and anyone can start this.").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)),
                    mouseX, mouseY);
        }

        NotchWidgets.divider(ctx, px + 8, py + 188, W - 16);
        boolean repHover = over(mouseX, mouseY, px + FX, py + 196, 72, 14);
        ctx.drawString(this.font, "Repeat:", px + LX, py + 200, NotchTheme.TEXT_DARK, false);
        if (repeatable) {
            NotchWidgets.primaryButton(ctx, this.font, px + FX, py + 196, 72, 14, "Repeats", repHover);
        } else {
            NotchWidgets.neutralButton(ctx, this.font, px + FX, py + 196, 72, 14, "Once only", repHover);
        }
        boolean forced = type == BountyType.FETCH;
        boolean endHover = over(mouseX, mouseY, px + FX + 78, py + 196, 72, 14);
        NotchWidgets.neutralButton(ctx, this.font, px + FX + 78, py + 196, 72, 14,
                (handIn || forced) ? "Hand back" : "Pays now", endHover && !forced);
        ctx.drawString(this.font, "Then:", px + LX, py + 240, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, px + FX, py + 236, FW - 44, 14, NotchTheme.DEEP);
        NotchWidgets.neutralButton(ctx, this.font, px + FX + FW - 40, py + 236, 40, 14, "Pick",
                over(mouseX, mouseY, px + FX + FW - 40, py + 236, 40, 14));
        if (over(mouseX, mouseY, px + LX, py + 236, FX + FW - LX, 14)) {
            ctx.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("Then").withStyle(net.minecraft.ChatFormatting.WHITE),
                    Component.literal("Name a quest and finishing this one hands it").withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.literal("over straight away. That is how a chain runs.").withStyle(net.minecraft.ChatFormatting.GRAY)),
                    mouseX, mouseY);
        }
        ctx.drawString(this.font, "Who:", px + LX, py + 220, NotchTheme.TEXT_DARK, false);
        boolean whoHover = over(mouseX, mouseY, px + FX, py + 216, 88, 14);
        if (factionOnly) {
            NotchWidgets.primaryButton(ctx, this.font, px + FX, py + 216, 88, 14, "Faction", whoHover);
            NotchWidgets.inset(ctx, px + FX + 92, py + 216, 56, 14, NotchTheme.DEEP);
            NotchWidgets.neutralButton(ctx, this.font, px + FX + 152, py + 216, 40, 14, "Pick",
                    over(mouseX, mouseY, px + FX + 152, py + 216, 40, 14));
        } else {
            NotchWidgets.neutralButton(ctx, this.font, px + FX, py + 216, 88, 14, "Anyone", whoHover);
        }
        if (whoHover) {
            ctx.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("Who").withStyle(net.minecraft.ChatFormatting.WHITE),
                    Component.literal("Faction: only faction members can take it, and").withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.literal("faction mates within 48 blocks share the progress.").withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.literal("Leave the box empty for any faction.").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)),
                    mouseX, mouseY);
        }
        if (endHover) {
            ctx.renderComponentTooltip(this.font, java.util.List.of(
                    Component.literal("Ends").withStyle(net.minecraft.ChatFormatting.WHITE),
                    Component.literal("On its own: pays the moment the goal is met.").withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.literal("Hand it back: the player returns to an NPC with").withStyle(net.minecraft.ChatFormatting.GRAY),
                    Component.literal("a Turn in quest action.").withStyle(net.minecraft.ChatFormatting.GRAY),
                    forced ? Component.literal("Collect always hands back, so someone gets the items.")
                            .withStyle(net.minecraft.ChatFormatting.RED)
                            : Component.literal("").withStyle(net.minecraft.ChatFormatting.DARK_GRAY)),
                    mouseX, mouseY);
        }

        NotchWidgets.primaryButton(ctx, this.font, px + 40, py + H - 26, 100, 16, "Save & Back",
                over(mouseX, mouseY, px + 40, py + H - 26, 100, 16));
        NotchWidgets.neutralButton(ctx, this.font, px + 160, py + H - 26, 100, 16, "Back",
                over(mouseX, mouseY, px + 160, py + H - 26, 100, 16));

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
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            if (over(mx, my, px + FX, py + 44, FW, 14)) {
                NotchWidgets.click();
                BountyType[] all = BountyType.values();
                type = all[(type.ordinal() + 1) % all.length];
                syncFields();
                return true;
            }
            if (npcField.isVisible() && over(mx, my, px + FX + FW - 40, py + 88, 40, 14)) {
                NotchWidgets.click();
                if (type == BountyType.VISIT) {
                    if (this.minecraft != null && this.minecraft.player != null) {
                        var p = this.minecraft.player;
                        npcField.setValue(net.minecraft.util.Mth.floor(p.getX()) + " "
                                + net.minecraft.util.Mth.floor(p.getY()) + " "
                                + net.minecraft.util.Mth.floor(p.getZ()));
                    }
                } else {
                    npcField.setValue(NpcNames.next(npcField.getValue(), false));
                }
                return true;
            }
            if (over(mx, my, px + FX + FW - 40, py + 168, 40, 14)) {
                NotchWidgets.click();
                needsField.setValue(nextQuestName(needsField.getValue()));
                return true;
            }
            if (over(mx, my, px + FX, py + 196, 72, 14)) {
                NotchWidgets.click();
                repeatable = !repeatable;
                return true;
            }
            if (over(mx, my, px + FX + 78, py + 196, 72, 14) && type != BountyType.FETCH) {
                NotchWidgets.click();
                handIn = !handIn;
                return true;
            }
            if (over(mx, my, px + FX, py + 216, 88, 14)) {
                NotchWidgets.click();
                factionOnly = !factionOnly;
                if (!factionOnly) factionField.setValue("");
                syncFields();
                return true;
            }
            if (over(mx, my, px + FX + FW - 40, py + 236, 40, 14)) {
                NotchWidgets.click();
                nextField.setValue(nextQuestName(nextField.getValue()));
                return true;
            }
            if (factionOnly && over(mx, my, px + FX + 152, py + 216, 40, 14)) {
                NotchWidgets.click();
                factionField.setValue(QuestNames.nextFaction(factionField.getValue()));
                return true;
            }
            if (over(mx, my, px + 40, py + H - 26, 100, 16)) {
                NotchWidgets.click();
                save();
                return true;
            }
            if (over(mx, my, px + 160, py + H - 26, 100, 16)) {
                NotchWidgets.click();
                back();
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private void save() {
        CompoundTag o = new CompoundTag();
        net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(o, "Id",
                net.fugginbeenus.notchcurrency.economy.bounty.Bounty.idForKey(key));
        o.putString("Type", type.name());
        String target = targetField.getValue().trim();
        o.putString("Target", target.isEmpty() ? "minecraft:air" : target);
        o.putInt("Required", parseInt(countField.getValue(), 1));
        o.putLong("RewardCoins", parseInt(coinsField.getValue(), 0));
        String rewardId = rewardItemField.getValue().trim();
        if (!rewardId.isEmpty()) {
            var id = net.minecraft.resources.ResourceLocation.tryParse(rewardId);
            if (id != null) {
                var item = net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id);
                var stack = new net.minecraft.world.item.ItemStack(item,
                        Math.max(1, parseInt(rewardCountField.getValue(), 1)));
                if (!stack.isEmpty()) {
                    o.put("RewardItem", net.fugginbeenus.notchcurrency.compat.StackData.writeStack(stack));
                }
            }
        }
        o.putString("NextQuest", nextField.getValue().trim());
        o.putString("Rarity", BountyRarity.COMMON.name());
        o.putBoolean("Repeatable", repeatable);
        o.putBoolean("HandIn", handIn);
        o.putBoolean("FactionOnly", factionOnly);
        o.putString("NeedsFaction", factionOnly ? factionField.getValue().trim() : "");
        o.putLong("Expires", 0L);
        o.putString("Desc", descField.getValue().trim());
        o.putBoolean("Quest", true);
        o.putString("QuestKey", key);
        String spot = npcField.getValue().trim();
        if (type == BountyType.VISIT) {
            String[] parts = spot.split("\\s+");
            if (parts.length >= 3) {
                spot = parts[0] + " " + parts[1] + " " + parts[2] + " "
                        + Math.max(1, parseInt(radiusField.getValue(), 6));
            }
        }
        o.putString("TargetText", spot);
        o.putString("NeedsQuest", needsField.getValue().trim());
        NotchPacketsClient.sendQuestSave(o);
        back();
    }

    private void back() {
        if (cameFromNpc != null) {
            NotchPacketsClient.sendNpcActionsOpen(cameFromNpc);
            cameFromNpc = null;
            cameFromDesigner = false;
            return;
        }
        if (cameFromDesigner) {
            cameFromDesigner = false;
            NotchPacketsClient.sendQuestDesign();
            return;
        }
        this.onClose();
    }

    private String loadRadius() {
        String[] parts = loadNpc.trim().split("\\s+");
        return parts.length >= 4 ? parts[3] : "6";
    }

    private String nextQuestName(String current) {
        java.util.List<String> options = new java.util.ArrayList<>();
        options.add("");
        for (String name : allQuests) {
            if (!name.equalsIgnoreCase(key) && !options.contains(name)) options.add(name);
        }
        int at = options.indexOf(current == null ? "" : current.trim());
        return options.get((at < 0 ? 0 : at + 1) % options.size());
    }

    private static int parseInt(String raw, int fallback) {
        try {
            return Math.max(0, Integer.parseInt(raw.trim()));
        } catch (NumberFormatException notANumber) {
            return fallback;
        }
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
