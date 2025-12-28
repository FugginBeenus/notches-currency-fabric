package net.fugginbeenus.notchcurrency.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.entity.Entity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shopkeeper Settings Screen - allows owners to customize their shopkeeper NPC.
 * Three tabs: Look, Name, Dialog
 */
@Environment(EnvType.CLIENT)
public class ShopkeeperSettingsScreen extends Screen {

    // Debug flags
    private static final boolean DEBUG_DELETE_BUTTON = false;

    // Textures for each tab
    private static final Identifier LOOK_TEXTURE = new Identifier(NotchCurrency.MOD_ID, "textures/gui/shopkeeper/look_tab.png");
    private static final Identifier NAME_TEXTURE = new Identifier(NotchCurrency.MOD_ID, "textures/gui/shopkeeper/name_tab.png");
    private static final Identifier DIALOG_TEXTURE = new Identifier(NotchCurrency.MOD_ID, "textures/gui/shopkeeper/dialog_tab.png");

    // GUI dimensions
    private static final int GUI_WIDTH = 248;
    private static final int GUI_HEIGHT = 219;
    private static final int TEXTURE_SIZE = 256;

    // Tab button positions (relative to GUI top-left)
    private static final int TAB_Y = 5;
    private static final int TAB_HEIGHT = 15;
    private static final int LOOK_TAB_X = 7;
    private static final int LOOK_TAB_W = 37;
    private static final int NAME_TAB_X = 46;
    private static final int NAME_TAB_W = 37;
    private static final int DIALOG_TAB_X = 85;
    private static final int DIALOG_TAB_W = 45;

    // Bottom button positions (Look tab has DELETE, SHOP, DONE)
    private static final int DELETE_BTN_X = 124;  // DELETE button (Look tab only) - moved left 6px
    private static final int DELETE_BTN_Y = 195;
    private static final int DELETE_BTN_W = 34;   // Widened by 6px
    private static final int DELETE_BTN_H = 16;
    private static final int SHOP_BTN_X = 181;
    private static final int SHOP_BTN_Y = 197;
    private static final int SHOP_BTN_W = 28;
    private static final int SHOP_BTN_H = 16;
    private static final int DONE_BTN_X = 213;
    private static final int DONE_BTN_Y = 197;
    private static final int DONE_BTN_W = 28;
    private static final int DONE_BTN_H = 16;

    // Look tab elements
    private static final int PREVIEW_X = 7;
    private static final int PREVIEW_Y = 28;
    private static final int PREVIEW_W = 93;
    private static final int PREVIEW_H = 156;
    private static final int ARROW_LEFT_X = 7;
    private static final int ARROW_LEFT_Y = 175;
    private static final int ARROW_RIGHT_X = 88;
    private static final int ARROW_RIGHT_Y = 175;
    private static final int ARROW_SIZE = 20;
    // Input fields are in dark boxes from x=120-216
    private static final int PLAYER_SKIN_FIELD_X = 123;  // Right 1px
    private static final int PLAYER_SKIN_FIELD_Y = 44;   // Down 1px
    private static final int SKIN_FIELD_W = 92;
    private static final int SKIN_FIELD_H = 12;
    private static final int PLAYER_SKIN_CHECK_X = 218;
    private static final int PLAYER_SKIN_CHECK_Y = 40;
    private static final int CUSTOM_SKIN_FIELD_X = 122;
    private static final int CUSTOM_SKIN_FIELD_Y = 80;
    private static final int CUSTOM_SKIN_CHECK_X = 218;
    private static final int CUSTOM_SKIN_CHECK_Y = 77;

    // Name tab elements - dark boxes from x=75 to x=171 (96px wide), checkmark at ~176
    private static final int NPC_NAME_FIELD_X = 78;      // 3px padding from left edge
    private static final int NPC_NAME_FIELD_Y = 94;      // Inside first dark box (down 2px)
    private static final int NAME_FIELD_W = 90;          // 96 - 6px padding = 90px
    private static final int NAME_FIELD_H = 12;
    private static final int NPC_NAME_CHECK_X = 176;     // Checkmark position
    private static final int NPC_NAME_CHECK_Y = 87;
    private static final int SHOP_NAME_FIELD_X = 78;     // 3px padding from left edge
    private static final int SHOP_NAME_FIELD_Y = 127;    // Inside second dark box (y124 + 3px)
    private static final int SHOP_NAME_CHECK_X = 176;    // Checkmark position
    private static final int SHOP_NAME_CHECK_Y = 124;

    // Dialog tab elements - 5 text lines based on texture
    private static final int DIALOG_FIELD_X = 9;
    private static final int DIALOG_FIELD_Y_START = 39;  // First line inside dark box (y=36-52)
    private static final int DIALOG_FIELD_W = 228;
    private static final int DIALOG_FIELD_H = 12;
    private static final int DIALOG_FIELD_SPACING = 20;  // Gap between lines
    private static final int DIALOG_LINE_COUNT = 5;
    private static final int DIALOG_CHECK_X = 120;
    private static final int DIALOG_CHECK_Y = 165;

    // State
    private enum Tab { LOOK, NAME, DIALOG }
    private Tab currentTab = Tab.LOOK;

    private final UUID shopId;
    private final UUID npcId;
    private final String shopName;
    private final String ownerName;
    private final String initialDialog;

    private int guiLeft;
    private int guiTop;

    // Cached NPC entity reference
    private net.minecraft.entity.LivingEntity cachedNpcEntity;
    private int npcSearchCooldown = 0;

    // Preset skins - names displayed in UI (can be customized later)
    private static final String[] PRESET_SKIN_NAMES = {
            "Preset 1", "Preset 2", "Preset 3", "Preset 4",
            "Preset 5", "Preset 6", "Preset 7", "Preset 8",
            "Preset 9", "Preset 10", "Preset 11", "Preset 12"
    };
    // Texture paths for preset skins
    private static final String[] PRESET_SKIN_TEXTURES = {
            "textures/skins/preset_1.png", "textures/skins/preset_2.png",
            "textures/skins/preset_3.png", "textures/skins/preset_4.png",
            "textures/skins/preset_5.png", "textures/skins/preset_6.png",
            "textures/skins/preset_7.png", "textures/skins/preset_8.png",
            "textures/skins/preset_9.png", "textures/skins/preset_10.png",
            "textures/skins/preset_11.png", "textures/skins/preset_12.png"
    };
    private int currentPresetIndex = 0;

    // Text fields
    private TextFieldWidget playerSkinField;
    private TextFieldWidget npcNameField;
    private TextFieldWidget shopNameField;
    private List<TextFieldWidget> dialogFields = new ArrayList<>();

    // Current values
    private String currentSkinType = "preset"; // "preset", "player", "custom"
    private String currentSkinValue = "";

    public ShopkeeperSettingsScreen(UUID shopId, UUID npcId, String shopName, String ownerName, String dialog) {
        super(Text.translatable("screen.notchcurrency.shopkeeper_settings"));
        this.shopId = shopId;
        this.npcId = npcId;
        this.shopName = shopName;
        this.ownerName = ownerName;
        this.initialDialog = dialog != null ? dialog : "";
    }

    // Store original values to detect actual changes
    private String originalNpcName = "";
    private String originalShopName = "";

    @Override
    protected void init() {
        super.init();

        this.guiLeft = (this.width - GUI_WIDTH) / 2;
        this.guiTop = (this.height - GUI_HEIGHT) / 2;

        // Get the actual NPC name from the entity
        String npcName = getNpcNameFromEntity();
        originalNpcName = npcName;
        originalShopName = shopName;

        initLookTabWidgets();
        initNameTabWidgets(npcName);
        initDialogTabWidgets();

        updateWidgetVisibility();
    }

    /**
     * Get the NPC's current custom name from the entity
     */
    private String getNpcNameFromEntity() {
        if (client != null && client.world != null && npcId != null) {
            for (net.minecraft.entity.Entity entity : client.world.getEntities()) {
                if (entity.getUuid().equals(npcId)) {
                    if (entity.hasCustomName() && entity.getCustomName() != null) {
                        return entity.getCustomName().getString();
                    }
                    break;
                }
            }
        }
        // Fallback to shop name if entity not found
        return shopName;
    }

    private void initLookTabWidgets() {
        // Player skin text field
        playerSkinField = new TextFieldWidget(
                this.textRenderer,
                guiLeft + PLAYER_SKIN_FIELD_X, guiTop + PLAYER_SKIN_FIELD_Y,
                SKIN_FIELD_W, SKIN_FIELD_H,
                Text.literal("Player Name")
        );
        playerSkinField.setMaxLength(16);
        playerSkinField.setDrawsBackground(false);
        addDrawableChild(playerSkinField);
    }

    private void initNameTabWidgets(String npcName) {
        // NPC name field - use actual NPC name, not shop name
        npcNameField = new TextFieldWidget(
                this.textRenderer,
                guiLeft + NPC_NAME_FIELD_X, guiTop + NPC_NAME_FIELD_Y,
                NAME_FIELD_W, NAME_FIELD_H,
                Text.literal("NPC Name")
        );
        npcNameField.setMaxLength(32);
        npcNameField.setDrawsBackground(false);
        npcNameField.setText(npcName);
        npcNameField.setEditableColor(0xFFFFFF); // White text
        // Only mark modified if the text actually changes from original
        npcNameField.setChangedListener(text -> {
            if (!text.equals(originalNpcName)) {
                npcNameModified = true;
            }
        });
        addDrawableChild(npcNameField);

        // Shop name field
        shopNameField = new TextFieldWidget(
                this.textRenderer,
                guiLeft + SHOP_NAME_FIELD_X, guiTop + SHOP_NAME_FIELD_Y,
                NAME_FIELD_W, NAME_FIELD_H,
                Text.literal("Shop Name")
        );
        shopNameField.setMaxLength(32);
        shopNameField.setDrawsBackground(false);
        shopNameField.setText(shopName);
        shopNameField.setEditableColor(0xFFFFFF); // White text
        shopNameField.setChangedListener(text -> {
            if (!text.equals(originalShopName)) {
                shopNameModified = true;
            }
        });
        addDrawableChild(shopNameField);
    }

    private void initDialogTabWidgets() {
        dialogFields.clear();

        // Split initial dialog into lines
        String[] dialogLines = initialDialog.split("\n", DIALOG_LINE_COUNT);

        for (int i = 0; i < DIALOG_LINE_COUNT; i++) {
            TextFieldWidget field = new TextFieldWidget(
                    this.textRenderer,
                    guiLeft + DIALOG_FIELD_X, guiTop + DIALOG_FIELD_Y_START + (i * DIALOG_FIELD_SPACING),
                    DIALOG_FIELD_W, DIALOG_FIELD_H,
                    Text.literal("Dialog " + (i + 1))
            );
            field.setMaxLength(50);
            field.setDrawsBackground(false);
            field.setChangedListener(text -> dialogModified = true);

            // Pre-populate with existing dialog if available
            if (i < dialogLines.length && dialogLines[i] != null) {
                field.setText(dialogLines[i]);
            }

            dialogFields.add(field);
            addDrawableChild(field);
        }
    }

    private void updateWidgetVisibility() {
        // Look tab widgets
        playerSkinField.visible = (currentTab == Tab.LOOK);
        playerSkinField.active = (currentTab == Tab.LOOK);

        // Name tab widgets
        npcNameField.visible = (currentTab == Tab.NAME);
        npcNameField.active = (currentTab == Tab.NAME);
        shopNameField.visible = (currentTab == Tab.NAME);
        shopNameField.active = (currentTab == Tab.NAME);

        // Dialog tab widgets
        for (TextFieldWidget field : dialogFields) {
            field.visible = (currentTab == Tab.DIALOG);
            field.active = (currentTab == Tab.DIALOG);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Handle pending refreshes for skin updates
        if (pendingRefreshTicks > 0) {
            pendingRefreshTicks--;
            if (pendingRefreshTicks % 10 == 0) { // Refresh every 10 ticks (0.5 seconds)
                npcSearchCooldown = 0;
                cachedNpcEntity = null;
            }
        }

        this.renderBackground(context);

        // Draw the appropriate tab texture
        Identifier texture = switch (currentTab) {
            case LOOK -> LOOK_TEXTURE;
            case NAME -> NAME_TEXTURE;
            case DIALOG -> DIALOG_TEXTURE;
        };

        RenderSystem.setShaderTexture(0, texture);
        context.drawTexture(texture, guiLeft, guiTop, 0, 0, GUI_WIDTH, GUI_HEIGHT, TEXTURE_SIZE, TEXTURE_SIZE);

        // Draw tab-specific content
        switch (currentTab) {
            case LOOK -> renderLookTab(context, mouseX, mouseY);
            case NAME -> renderNameTab(context, mouseX, mouseY);
            case DIALOG -> renderDialogTab(context, mouseX, mouseY);
        }

        // Draw widgets with scissor clipping for text fields
        if (currentTab == Tab.NAME) {
            // Debug: Draw the scissor regions as colored rectangles
            boolean DEBUG_SCISSOR = false;
            if (DEBUG_SCISSOR) {
                // Draw red outline for first box scissor region
                context.drawHorizontalLine(guiLeft + 75, guiLeft + 171, guiTop + 87, 0xFFFF0000);
                context.drawHorizontalLine(guiLeft + 75, guiLeft + 171, guiTop + 103, 0xFFFF0000);
                context.drawVerticalLine(guiLeft + 75, guiTop + 87, guiTop + 103, 0xFFFF0000);
                context.drawVerticalLine(guiLeft + 171, guiTop + 87, guiTop + 103, 0xFFFF0000);

                // Draw blue outline for second box scissor region
                context.drawHorizontalLine(guiLeft + 75, guiLeft + 171, guiTop + 124, 0xFF0000FF);
                context.drawHorizontalLine(guiLeft + 75, guiLeft + 171, guiTop + 140, 0xFF0000FF);
                context.drawVerticalLine(guiLeft + 75, guiTop + 124, guiTop + 140, 0xFF0000FF);
                context.drawVerticalLine(guiLeft + 171, guiTop + 124, guiTop + 140, 0xFF0000FF);
            }

            // Enable scissor to clip text fields to their visual dark boxes
            // Dark boxes in texture: x=75 to x=171 (96px wide)

            // NPC Name box (first dark box) - moved down 2px
            context.enableScissor(
                    guiLeft + 75,   // Left edge of dark box
                    guiTop + 87,    // Top of first dark box (down 2px)
                    guiLeft + 171,  // Right edge of dark box
                    guiTop + 103    // Bottom of first dark box (down 2px)
            );
            if (npcNameField != null) {
                npcNameField.render(context, mouseX, mouseY, delta);
            }
            context.disableScissor();

            // Shop Name box (second dark box)
            context.enableScissor(
                    guiLeft + 75,   // Left edge of dark box
                    guiTop + 124,   // Top of second dark box
                    guiLeft + 171,  // Right edge of dark box
                    guiTop + 140    // Bottom of second dark box
            );
            if (shopNameField != null) {
                shopNameField.render(context, mouseX, mouseY, delta);
            }
            context.disableScissor();

            // Render other widgets normally
            for (var child : this.children()) {
                if (child != npcNameField && child != shopNameField && child instanceof net.minecraft.client.gui.Drawable drawable) {
                    drawable.render(context, mouseX, mouseY, delta);
                }
            }
        } else {
            // Draw widgets normally for other tabs
            super.render(context, mouseX, mouseY, delta);
        }

        // Draw skin name in the dark box between arrows on Look tab
        if (currentTab == Tab.LOOK) {
            String skinName = PRESET_SKIN_NAMES[currentPresetIndex];
            String countText = "(" + (currentPresetIndex + 1) + " of " + PRESET_SKIN_NAMES.length + ")";
            int nameWidth = textRenderer.getWidth(skinName);
            int countWidth = textRenderer.getWidth(countText);

            // Dark box is between arrows: approximately x=23-87, y=175-185
            // Center the skin name in the dark box (white text)
            int boxCenterX = guiLeft + 55;  // Center of dark box
            int boxCenterY = guiTop + 178;  // Center Y of dark box (up 2px)

            // Skin name - WHITE text inside the dark box
            context.drawText(textRenderer, skinName, boxCenterX - nameWidth / 2, boxCenterY - 4, 0xFFFFFF, false);

            // Counter - gray text below the box
            context.drawText(textRenderer, countText, boxCenterX - countWidth / 2, guiTop + 190, 0x808080, false);

            // DEBUG: Draw DELETE button outline (red)
            if (DEBUG_DELETE_BUTTON) {
                int dx = guiLeft + DELETE_BTN_X;
                int dy = guiTop + DELETE_BTN_Y;
                context.drawHorizontalLine(dx, dx + DELETE_BTN_W, dy, 0xFFFF0000);
                context.drawHorizontalLine(dx, dx + DELETE_BTN_W, dy + DELETE_BTN_H, 0xFFFF0000);
                context.drawVerticalLine(dx, dy, dy + DELETE_BTN_H, 0xFFFF0000);
                context.drawVerticalLine(dx + DELETE_BTN_W, dy, dy + DELETE_BTN_H, 0xFFFF0000);
            }
        }
    }

    private void renderLookTab(DrawContext context, int mouseX, int mouseY) {
        // Render NPC preview in the preview area
        int previewCenterX = guiLeft + PREVIEW_X + PREVIEW_W / 2;
        int previewCenterY = guiTop + PREVIEW_Y + PREVIEW_H - 20;

        // Try to find the actual NPC entity in the client world
        net.minecraft.entity.LivingEntity npcEntity = findNpcEntity();

        if (npcEntity != null) {
            int entityX = previewCenterX;
            int entityY = previewCenterY;
            int size = 55;

            // Save original yaw
            float originalYaw = npcEntity.getYaw();
            float originalBodyYaw = npcEntity.bodyYaw;

            // Rotate entity 180° to face forward in preview
            npcEntity.setYaw(180);
            npcEntity.bodyYaw = 180;

            // Calculate look direction based on mouse position
            float lookAtX = (float)(previewCenterX) - mouseX;
            float lookAtY = (float)(guiTop + PREVIEW_Y + 40) - mouseY;

            // Render the actual NPC entity
            net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(
                    context,
                    entityX,
                    entityY,
                    size,
                    lookAtX,
                    lookAtY,
                    npcEntity
            );

            // Restore original yaw
            npcEntity.setYaw(originalYaw);
            npcEntity.bodyYaw = originalBodyYaw;
        } else if (client != null && client.player != null) {
            // Fallback to player model if NPC not found
            int entityX = previewCenterX;
            int entityY = previewCenterY;
            int size = 55;

            float lookAtX = (float)(previewCenterX) - mouseX;
            float lookAtY = (float)(guiTop + PREVIEW_Y + 40) - mouseY;

            net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(
                    context,
                    entityX,
                    entityY,
                    size,
                    lookAtX,
                    lookAtY,
                    client.player
            );
        } else {
            // Fallback placeholder text
            String placeholder = "NPC Preview";
            int textWidth = textRenderer.getWidth(placeholder);
            int previewTextY = guiTop + PREVIEW_Y + PREVIEW_H / 2;
            context.drawText(textRenderer, placeholder, previewCenterX - textWidth / 2, previewTextY - 4, 0x808080, false);
        }
    }

    /**
     * Find the NPC entity in the client world by UUID
     * Uses caching to avoid searching every frame, but refreshes periodically
     * to catch skin updates
     */
    private net.minecraft.entity.LivingEntity findNpcEntity() {
        if (client == null || client.world == null || npcId == null) {
            return null;
        }

        // Decrease cooldown
        if (npcSearchCooldown > 0) {
            npcSearchCooldown--;
        }

        // Return cached entity if valid and cooldown active
        if (cachedNpcEntity != null && !cachedNpcEntity.isRemoved() && npcSearchCooldown > 0) {
            return cachedNpcEntity;
        }

        // Search for entity (every 20 ticks / 1 second)
        npcSearchCooldown = 20;

        // Iterate through all entities in the client world to find our NPC
        for (net.minecraft.entity.Entity entity : client.world.getEntities()) {
            if (entity.getUuid().equals(npcId) && entity instanceof net.minecraft.entity.LivingEntity living) {
                cachedNpcEntity = living;
                return living;
            }
        }

        cachedNpcEntity = null;
        return null;
    }

    /**
     * Force refresh the NPC entity (call after skin update)
     */
    private void refreshNpcEntity() {
        // Reset cache immediately
        npcSearchCooldown = 0;
        cachedNpcEntity = null;
        // Set a flag to do additional refreshes over the next few seconds
        // to catch the skin update as it propagates
        pendingRefreshTicks = 60; // Refresh repeatedly for 3 seconds
    }

    // Track pending refreshes for skin updates
    private int pendingRefreshTicks = 0;

    private void renderNameTab(DrawContext context, int mouseX, int mouseY) {
        // Labels are part of the texture, no additional rendering needed
    }

    private void renderDialogTab(DrawContext context, int mouseX, int mouseY) {
        // Labels are part of the texture, no additional rendering needed
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            int mx = (int) mouseX - guiLeft;
            int my = (int) mouseY - guiTop;

            // Check tab clicks
            if (my >= TAB_Y && my <= TAB_Y + TAB_HEIGHT) {
                if (mx >= LOOK_TAB_X && mx <= LOOK_TAB_X + LOOK_TAB_W) {
                    currentTab = Tab.LOOK;
                    updateWidgetVisibility();
                    return true;
                } else if (mx >= NAME_TAB_X && mx <= NAME_TAB_X + NAME_TAB_W) {
                    currentTab = Tab.NAME;
                    updateWidgetVisibility();
                    return true;
                } else if (mx >= DIALOG_TAB_X && mx <= DIALOG_TAB_X + DIALOG_TAB_W) {
                    currentTab = Tab.DIALOG;
                    updateWidgetVisibility();
                    return true;
                }
            }

            // Check bottom buttons
            // DELETE button (only on Look tab)
            if (currentTab == Tab.LOOK) {
                if (mx >= DELETE_BTN_X && mx <= DELETE_BTN_X + DELETE_BTN_W &&
                        my >= DELETE_BTN_Y && my <= DELETE_BTN_Y + DELETE_BTN_H) {
                    confirmDeleteNpc();
                    return true;
                }
            }

            if (mx >= SHOP_BTN_X && mx <= SHOP_BTN_X + SHOP_BTN_W &&
                    my >= SHOP_BTN_Y && my <= SHOP_BTN_Y + SHOP_BTN_H) {
                openShopManagement();
                return true;
            }

            if (mx >= DONE_BTN_X && mx <= DONE_BTN_X + DONE_BTN_W &&
                    my >= DONE_BTN_Y && my <= DONE_BTN_Y + DONE_BTN_H) {
                saveAndClose();
                return true;
            }

            // Look tab specific clicks
            if (currentTab == Tab.LOOK) {
                // Arrow buttons
                if (my >= ARROW_LEFT_Y && my <= ARROW_LEFT_Y + ARROW_SIZE) {
                    if (mx >= ARROW_LEFT_X && mx <= ARROW_LEFT_X + ARROW_SIZE) {
                        // Previous skin
                        currentPresetIndex--;
                        if (currentPresetIndex < 0) currentPresetIndex = PRESET_SKIN_NAMES.length - 1;
                        currentSkinType = "preset";
                        currentSkinValue = PRESET_SKIN_TEXTURES[currentPresetIndex];
                        // Apply skin immediately
                        applyPresetSkin();
                        return true;
                    }
                    if (mx >= ARROW_RIGHT_X && mx <= ARROW_RIGHT_X + ARROW_SIZE) {
                        // Next skin
                        currentPresetIndex++;
                        if (currentPresetIndex >= PRESET_SKIN_NAMES.length) currentPresetIndex = 0;
                        currentSkinType = "preset";
                        currentSkinValue = PRESET_SKIN_TEXTURES[currentPresetIndex];
                        // Apply skin immediately
                        applyPresetSkin();
                        return true;
                    }
                }

                // Player skin checkmark (apply)
                if (mx >= PLAYER_SKIN_CHECK_X && mx <= PLAYER_SKIN_CHECK_X + 16 &&
                        my >= PLAYER_SKIN_CHECK_Y && my <= PLAYER_SKIN_CHECK_Y + 16) {
                    applyPlayerSkin();
                    return true;
                }

                // Custom skin checkmark (apply / open file chooser)
                if (mx >= CUSTOM_SKIN_CHECK_X && mx <= CUSTOM_SKIN_CHECK_X + 16 &&
                        my >= CUSTOM_SKIN_CHECK_Y && my <= CUSTOM_SKIN_CHECK_Y + 16) {
                    openCustomSkinFileChooser();
                    return true;
                }
            }

            // Name tab checkmarks
            if (currentTab == Tab.NAME) {
                if (mx >= NPC_NAME_CHECK_X && mx <= NPC_NAME_CHECK_X + 16 &&
                        my >= NPC_NAME_CHECK_Y && my <= NPC_NAME_CHECK_Y + 16) {
                    applyNpcName();
                    return true;
                }
                if (mx >= SHOP_NAME_CHECK_X && mx <= SHOP_NAME_CHECK_X + 16 &&
                        my >= SHOP_NAME_CHECK_Y && my <= SHOP_NAME_CHECK_Y + 16) {
                    applyShopName();
                    return true;
                }
            }

            // Dialog tab checkmark
            if (currentTab == Tab.DIALOG) {
                if (mx >= DIALOG_CHECK_X && mx <= DIALOG_CHECK_X + 16 &&
                        my >= DIALOG_CHECK_Y && my <= DIALOG_CHECK_Y + 16) {
                    applyDialog();
                    return true;
                }
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void applyPlayerSkin() {
        String playerName = playerSkinField.getText().trim();
        if (!playerName.isEmpty()) {
            currentSkinType = "player";
            currentSkinValue = playerName;
            // Show loading message since this requires API lookups
            if (client != null && client.player != null) {
                client.player.sendMessage(
                        net.minecraft.text.Text.literal("Looking up skin for " + playerName + "...")
                                .formatted(net.minecraft.util.Formatting.GRAY),
                        true // action bar - less intrusive
                );
            }
            sendSkinUpdate("player", playerName);
            skinModified = false; // Already sent, don't resend on close
        }
    }

    private void applyPresetSkin() {
        // Send preset skin texture path to server immediately
        String texturePath = PRESET_SKIN_TEXTURES[currentPresetIndex];
        sendSkinUpdate("preset", texturePath);
        skinModified = false; // Already sent, don't resend on close
    }

    private void openCustomSkinFileChooser() {
        // Run file chooser on a separate thread to avoid blocking
        new Thread(() -> {
            try {
                // Use LWJGL's TinyFileDialogs for native file picker
                org.lwjgl.PointerBuffer filterPatterns = org.lwjgl.BufferUtils.createPointerBuffer(1);
                filterPatterns.put(org.lwjgl.system.MemoryUtil.memASCII("*.png"));
                filterPatterns.flip();

                String result = org.lwjgl.util.tinyfd.TinyFileDialogs.tinyfd_openFileDialog(
                        "Select Skin File",
                        "",
                        filterPatterns,
                        "PNG Image Files (*.png)",
                        false
                );

                if (result != null && !result.isEmpty()) {
                    java.io.File selectedFile = new java.io.File(result);
                    if (selectedFile.exists() && selectedFile.getName().toLowerCase().endsWith(".png")) {
                        // Execute on main thread
                        net.minecraft.client.MinecraftClient.getInstance().execute(() -> {
                            handleCustomSkinFile(selectedFile);
                        });
                    }
                }
            } catch (Exception e) {
                System.err.println("Error opening file chooser: " + e.getMessage());
                e.printStackTrace();
            }
        }, "SkinFileChooser").start();
    }

    private void handleCustomSkinFile(java.io.File skinFile) {
        try {
            // Read the file and send to server
            byte[] fileData = java.nio.file.Files.readAllBytes(skinFile.toPath());
            String base64Data = java.util.Base64.getEncoder().encodeToString(fileData);

            currentSkinType = "custom";
            currentSkinValue = skinFile.getName();

            // Send custom skin data to server
            sendSkinUpdate("custom_file", base64Data);

            if (client != null && client.player != null) {
                client.player.sendMessage(
                        net.minecraft.text.Text.literal("Custom skin selected: " + skinFile.getName())
                                .formatted(net.minecraft.util.Formatting.GREEN),
                        false
                );
            }
        } catch (Exception e) {
            System.err.println("Error loading skin file: " + e.getMessage());
            if (client != null && client.player != null) {
                client.player.sendMessage(
                        net.minecraft.text.Text.literal("Failed to load skin file!")
                                .formatted(net.minecraft.util.Formatting.RED),
                        false
                );
            }
        }
    }

    private void applyNpcName() {
        String name = npcNameField.getText().trim();
        if (!name.isEmpty()) {
            sendNpcNameUpdate(name);
        }
    }

    private void applyShopName() {
        String name = shopNameField.getText().trim();
        if (!name.isEmpty()) {
            sendShopNameUpdate(name);
        }
    }

    private void applyDialog() {
        StringBuilder dialog = new StringBuilder();
        for (TextFieldWidget field : dialogFields) {
            String line = field.getText().trim();
            if (!line.isEmpty()) {
                if (dialog.length() > 0) dialog.append("\n");
                dialog.append(line);
            }
        }
        sendDialogUpdate(dialog.toString());
    }

    private void openShopManagement() {
        // Close this screen and open shop management
        saveSettings();
        this.close();
        // The NpcShopLogic will handle opening the shop screen
        // We need to send a packet to request the shop screen
        sendOpenShopRequest();
    }

    private void saveAndClose() {
        saveSettings();
        this.close();
    }

    // Track what has been modified
    private boolean skinModified = false;
    private boolean npcNameModified = false;
    private boolean shopNameModified = false;
    private boolean dialogModified = false;

    private void saveSettings() {
        // Only send updates for things that were actually changed

        // Skin - only if modified
        if (skinModified) {
            if ("player".equals(currentSkinType) && !currentSkinValue.isEmpty()) {
                sendSkinUpdate("player", currentSkinValue);
            } else if ("preset".equals(currentSkinType)) {
                sendSkinUpdate("preset", PRESET_SKIN_TEXTURES[currentPresetIndex]);
            }
            skinModified = false;
        }

        // NPC Name - only if modified
        if (npcNameModified) {
            String npcName = npcNameField.getText().trim();
            if (!npcName.isEmpty()) {
                sendNpcNameUpdate(npcName);
            }
            npcNameModified = false;
        }

        // Shop Name - only if modified
        if (shopNameModified) {
            String shopName = shopNameField.getText().trim();
            if (!shopName.isEmpty()) {
                sendShopNameUpdate(shopName);
            }
            shopNameModified = false;
        }

        // Dialog - only if modified
        if (dialogModified) {
            applyDialog();
            dialogModified = false;
        }
    }

    // Packet sending methods
    private void sendSkinUpdate(String type, String value) {
        NotchPacketsClient.sendSkinUpdate(npcId, type, value);
        // Refresh NPC entity after a short delay to catch the update
        refreshNpcEntity();
    }

    private void sendNpcNameUpdate(String name) {
        NotchPacketsClient.sendNpcNameUpdate(npcId, name);
    }

    private void sendShopNameUpdate(String name) {
        NotchPacketsClient.sendShopNameUpdate(shopId, name);
    }

    private void sendDialogUpdate(String dialog) {
        NotchPacketsClient.sendDialogUpdate(npcId, dialog);
    }

    private void sendOpenShopRequest() {
        NotchPacketsClient.sendOpenShopRequest(shopId);
    }

    private void confirmDeleteNpc() {
        // Show confirmation in chat and require clicking again within 5 seconds
        if (deleteConfirmationPending && System.currentTimeMillis() - deleteConfirmationTime < 5000) {
            // Confirmed - send delete request
            sendDeleteNpc();
            this.close();
        } else {
            // First click - ask for confirmation
            deleteConfirmationPending = true;
            deleteConfirmationTime = System.currentTimeMillis();
            if (client != null && client.player != null) {
                client.player.sendMessage(
                        Text.literal("Are you sure you want to delete this shopkeeper? ")
                                .formatted(Formatting.YELLOW)
                                .append(Text.literal("Click DELETE again to confirm.")
                                        .formatted(Formatting.RED)),
                        false
                );
            }
        }
    }

    private boolean deleteConfirmationPending = false;
    private long deleteConfirmationTime = 0;

    private void sendDeleteNpc() {
        NotchPacketsClient.sendDeleteNpc(npcId, shopId);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}