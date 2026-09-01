package net.fugginbeenus.notchcurrency.client;

import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.client.ui.NotchTheme;
import net.fugginbeenus.notchcurrency.client.ui.NotchWidgets;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.net.NotchPacketsClient;
import net.fugginbeenus.notchcurrency.shop.ShopManageScreenHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ShopManageScreen extends AbstractContainerScreen<ShopManageScreenHandler> {

    private static final int W = 256, H = 244;
    private static final int ROW_X = 8, ROW_W = 240, ROW_H = 18, ROW_STEP = 19, ROWS_Y = 110;
    private static ItemStack coin;
    private static ItemStack coin() {
        if (coin == null) coin = new ItemStack(net.fugginbeenus.notchcurrency.registry.ModItems.NOTCH_COIN);
        return coin;
    }

    private EditBox nameField;
    private EditBox greetField;

    public ShopManageScreen(ShopManageScreenHandler handler, Inventory inv, Component title) {
        //? if >=26.1 {
        /*super(handler, inv, title, W, H);
        *///?} else {
        super(handler, inv, title);
        this.imageWidth = W;
        this.imageHeight = H;
        //?}
        this.titleLabelX = -1000;
        this.inventoryLabelX = -1000;
    }

    private int rowY(int i) { return this.topPos + ROWS_Y + i * ROW_STEP; }

    private static final char[] TITLE_COLORS =
            {0, '6', 'e', 'a', '2', 'b', '3', '9', 'd', '5', 'c', '4', '7'};

    private char firstColorCode() {
        String s = nameField == null ? "" : nameField.getValue();
        for (int i = 0; i + 1 < s.length(); i++) {
            if (s.charAt(i) == '&' && ChatFormatting.getByCode(s.charAt(i + 1)) != null
                    && net.fugginbeenus.notchcurrency.compat.Colors.isColor(ChatFormatting.getByCode(s.charAt(i + 1)))) {
                return Character.toLowerCase(s.charAt(i + 1));
            }
        }
        return 0;
    }

    private int swatchArgb() {
        ChatFormatting f = firstColorCode() == 0 ? null : ChatFormatting.getByCode(firstColorCode());
        Integer rgb = f == null ? null : net.fugginbeenus.notchcurrency.compat.Colors.rgb(f);
        return rgb == null ? 0xFFFFFFFF : 0xFF000000 | rgb;
    }

    private void cycleTitleColor() {
        char cur = firstColorCode();
        int idx = 0;
        for (int i = 0; i < TITLE_COLORS.length; i++) {
            if (TITLE_COLORS[i] == cur) { idx = i; break; }
        }
        char next = TITLE_COLORS[(idx + 1) % TITLE_COLORS.length];
        String bare = nameField.getValue().replaceFirst("^(?:&[0-9a-fk-orA-FK-OR])+", "");
        nameField.setValue(next == 0 ? bare : "&" + next + bare);
    }

    @Override
    protected void init() {
        super.init();
        String oldName = nameField == null ? menu.shopName() : nameField.getValue();
        nameField = new EditBox(this.font, this.leftPos + 45, this.topPos + 61, 160, 10, Component.literal("Name"));
        nameField.setMaxLength(48);
        nameField.setWidth(144);
        nameField.setBordered(false);
        nameField.setValue(oldName);
        addRenderableWidget(nameField);

        String oldGreet = greetField == null ? menu.greeting() : greetField.getValue();
        greetField = new EditBox(this.font, this.leftPos + 45, this.topPos + 79, 160, 10, Component.literal("Greeting"));
        greetField.setMaxLength(128);
        greetField.setBordered(false);
        greetField.setHint(Component.literal("shown to shoppers").withStyle(ChatFormatting.DARK_GRAY));
        greetField.setValue(oldGreet);
        addRenderableWidget(greetField);
    }

    private record Row(ItemStack icon, UUID listingId, int price, String barterName, int barterCount,
                       ItemStack barterStack, int stock) {}

    private Row row(int i) {
        ItemStack stack = menu.rowStack(i);
        if (stack.isEmpty()) return null;
        CompoundTag t = StackData.getData(stack);
        if (!net.fugginbeenus.notchcurrency.compat.Nbt.hasUuid(t, "nc_lid")) return null;
        ItemStack barter = t.contains("nc_bstack") ? StackData.readStack(t.getCompound("nc_bstack")) : ItemStack.EMPTY;
        return new Row(stack, net.fugginbeenus.notchcurrency.compat.Nbt.getUuid(t, "nc_lid"), t.getInt("nc_price"),
                t.getString("nc_bname"), t.getInt("nc_bcount"), barter, t.getInt("nc_stock"));
    }

    //? if >=26.1 {
    /*@Override
    public void extractContents(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    protected void renderBg(GuiGraphics ctx, float delta, int mouseX, int mouseY) {
    //?}
        final int x = this.leftPos, y = this.topPos;
        NotchWidgets.panel(ctx, x, y, W, H);
        NotchWidgets.title(ctx, this.font, "Shop Manager", x + W / 2, y + 8);

        boolean open = menu.prop(ShopManageScreenHandler.P_OPEN) != 0;
        boolean rentPaused = menu.prop(ShopManageScreenHandler.P_RENT_PAUSED) != 0;
        if (open) {
            NotchWidgets.primaryButton(ctx, this.font, x + 8, y + 20, 60, 14, "Open",
                    over(mouseX, mouseY, x + 8, y + 20, 60, 14));
        } else {
            NotchWidgets.dangerButton(ctx, this.font, x + 8, y + 20, 60, 14, "Closed",
                    over(mouseX, mouseY, x + 8, y + 20, 60, 14));
        }
        boolean isOp = menu.prop(ShopManageScreenHandler.P_IS_OP) != 0;
        boolean adminMode = menu.prop(ShopManageScreenHandler.P_ADMIN_MODE) != 0;
        if (isOp) {
            boolean hovered = over(mouseX, mouseY, x + 186, y + 20, 62, 14);
            if (adminMode) {
                NotchWidgets.goldButton(ctx, this.font, x + 186, y + 20, 62, 14, "Admin", hovered);
            } else {
                NotchWidgets.neutralButton(ctx, this.font, x + 186, y + 20, 62, 14, "Admin", hovered);
            }
        }

        String status = adminMode ? "Never runs out"
                : rentPaused ? "Rent overdue" : open ? "Selling" : "Sales off";
        int statusColor = adminMode ? NotchTheme.TEXT_GOLD
                : rentPaused ? NotchTheme.TEXT_RED : open ? NotchTheme.TEXT_GREEN : NotchTheme.TEXT_MUTED;
        ctx.drawString(this.font, status, x + 74, y + 24, statusColor, false);
        int rentCost = menu.prop(ShopManageScreenHandler.P_RENT_COST);
        if (rentCost > 0 && !adminMode) {
            MutableComponent rent = Component.literal("Rent ").append(NotchCurrency.coins(rentCost));
            ctx.drawString(this.font, rent, x + 248 - this.font.width(rent), y + 24,
                    NotchTheme.TEXT_MUTED, false);
        }

        long pending = menu.pendingBalance();
        int barterItems = menu.prop(ShopManageScreenHandler.P_BARTER_COUNT);
        NotchWidgets.pill(ctx, x + 8, y + 40, 160, 15);
        if (adminMode) {
            ctx.drawString(this.font, "No earnings to collect", x + 14, y + 44, NotchTheme.TEXT_MUTED, false);
            NotchWidgets.neutralButton(ctx, this.font, x + 176, y + 40, 72, 15, "Collect", false);
        } else {
            MutableComponent earnings = Component.literal("Earnings ").append(NotchCurrency.coins(pending));
            if (barterItems > 0) earnings.append(Component.literal(" +" + barterItems + " barter"));
            ctx.drawString(this.font, earnings, x + 14, y + 44, NotchTheme.TEXT_GOLD, false);
            boolean canCollect = pending > 0 || barterItems > 0;
            if (canCollect) {
                NotchWidgets.goldButton(ctx, this.font, x + 176, y + 40, 72, 15, "Collect",
                        over(mouseX, mouseY, x + 176, y + 40, 72, 15));
            } else {
                NotchWidgets.neutralButton(ctx, this.font, x + 176, y + 40, 72, 15, "Collect", false);
            }
        }

        ctx.drawString(this.font, "Name", x + 10, y + 62, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + 42, y + 59, 150, 14, NotchTheme.DEEP);
        NotchWidgets.slot(ctx, x + 195, y + 59, 14, 14);
        ctx.fill(x + 197, y + 61, x + 207, y + 71, swatchArgb());
        NotchWidgets.neutralButton(ctx, this.font, x + 212, y + 59, 36, 14, "Set",
                over(mouseX, mouseY, x + 212, y + 59, 36, 14));
        ctx.drawString(this.font, "Greet", x + 10, y + 80, NotchTheme.TEXT_DARK, false);
        NotchWidgets.inset(ctx, x + 42, y + 77, 166, 14, NotchTheme.DEEP);
        NotchWidgets.neutralButton(ctx, this.font, x + 212, y + 77, 36, 14, "Set",
                over(mouseX, mouseY, x + 212, y + 77, 36, 14));

        NotchWidgets.divider(ctx, x + 8, y + 94, W - 16);

        int count = menu.prop(ShopManageScreenHandler.P_COUNT);
        ctx.drawString(this.font, "LISTINGS (" + count + "/27)", x + 10, y + 99, NotchTheme.TEXT_DARK, false);
        int pageCount = menu.prop(ShopManageScreenHandler.P_TOTAL_PAGES);
        if (pageCount > 1) {
            NotchWidgets.neutralButton(ctx, this.font, x + 196, y + 97, 13, 11, "<",
                    over(mouseX, mouseY, x + 196, y + 97, 13, 11));
            NotchWidgets.centerText(ctx, this.font,
                    (menu.prop(ShopManageScreenHandler.P_PAGE) + 1) + "/" + pageCount,
                    x + 220, y + 99, NotchTheme.TEXT_DARK, false);
            NotchWidgets.neutralButton(ctx, this.font, x + 233, y + 97, 13, 11, ">",
                    over(mouseX, mouseY, x + 233, y + 97, 13, 11));
        }

        boolean any = false;
        for (int i = 0; i < ShopManageScreenHandler.ROWS; i++) {
            Row row = row(i);
            if (row == null) continue;
            any = true;
            int ry = rowY(i);
            NotchWidgets.insetFlipped(ctx, x + ROW_X, ry, ROW_W, ROW_H, NotchTheme.DEEP);
            if (row.price() > 0) {
                ctx.renderItem(coin(), x + ROW_X + 3, ry + 1);
                ctx.renderItemDecorations(this.font, coin(), x + ROW_X + 3, ry + 1,
                        NotchWidgets.compactCount(row.price()));
            }
            if (!row.barterStack().isEmpty()) {
                ctx.renderItem(row.barterStack(), x + ROW_X + 23, ry + 1);
                ctx.renderItemDecorations(this.font, row.barterStack(), x + ROW_X + 23, ry + 1);
            }
            if (row.price() <= 0 && row.barterStack().isEmpty()) {
                ctx.drawString(this.font, "free", x + ROW_X + 6, ry + 5, NotchTheme.TEXT_MUTED, false);
            }
            NotchWidgets.arrowRight(ctx, x + ROW_X + 45, ry + 5, NotchTheme.TEXT_MUTED);
            ctx.renderItem(row.icon(), x + ROW_X + 64, ry + 1);
            ctx.renderItemDecorations(this.font, row.icon(), x + ROW_X + 64, ry + 1);
            boolean unlimited = row.stock() < 0;
            String s = unlimited ? "inf" : "x" + row.stock();
            ctx.drawString(this.font, s, x + ROW_X + 200 - this.font.width(s), ry + 5,
                    unlimited ? NotchTheme.TEXT_GOLD
                            : row.stock() > 0 ? NotchTheme.TEXT_LIGHT : NotchTheme.TEXT_RED, false);
            NotchWidgets.neutralButton(ctx, this.font, x + ROW_X + 204, ry + 1, 32, 15, "Edit",
                    over(mouseX, mouseY, x + ROW_X + 204, ry + 1, 32, 15));
        }
        if (!any) {
            NotchWidgets.centerText(ctx, this.font, "No listings yet - add one below.",
                    x + W / 2, y + ROWS_Y + 40, NotchTheme.TEXT_MUTED, false);
        }

        NotchWidgets.primaryButton(ctx, this.font, x + 8, y + 224, 240, 15, "+ New Listing",
                over(mouseX, mouseY, x + 8, y + 224, 240, 15));
        //? if >=26.1 {
        /*
        super.extractContents(ctx, mouseX, mouseY, delta);
        *///?}
    }

    //? if >=26.1 {
    /*@Override
    public void extractRenderState(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    *///?} else {
    @Override
    public void render(GuiGraphics ctx, int mouseX, int mouseY, float delta) {
    //?}
        //? if <1.21 {
        this.renderBackground(ctx);
        //?}
        //? if >=26.1 {
        /*super.extractRenderState(ctx, mouseX, mouseY, delta);
        *///?} else {
        super.render(ctx, mouseX, mouseY, delta);
        //?}
        if (menu.prop(ShopManageScreenHandler.P_IS_OP) != 0
                && over(mouseX, mouseY, leftPos + 186, topPos + 20, 62, 14)) {
            boolean on = menu.prop(ShopManageScreenHandler.P_ADMIN_MODE) != 0;
            ctx.renderComponentTooltip(this.font, on
                    ? List.of(Component.literal("Admin shop").withStyle(ChatFormatting.GOLD),
                            Component.literal("Stock never runs out.").withStyle(ChatFormatting.GRAY),
                            Component.literal("Buyers pay, but nobody is paid.").withStyle(ChatFormatting.GRAY),
                            Component.literal("Click to make it a normal shop.").withStyle(ChatFormatting.DARK_GRAY))
                    : List.of(Component.literal("Make this an admin shop").withStyle(ChatFormatting.WHITE),
                            Component.literal("Stock never runs out.").withStyle(ChatFormatting.GRAY),
                            Component.literal("Buyers pay, but nobody is paid.").withStyle(ChatFormatting.GRAY),
                            Component.literal("Operators only.").withStyle(ChatFormatting.DARK_GRAY)),
                    mouseX, mouseY);
        }

        if (menu.prop(ShopManageScreenHandler.P_ADMIN_MODE) != 0
                && over(mouseX, mouseY, leftPos + 8, topPos + 40, 240, 15)) {
            ctx.renderComponentTooltip(this.font, List.of(
                    Component.literal("Admin shops do not earn").withStyle(ChatFormatting.GOLD),
                    Component.literal("Coins buyers pay leave the economy.").withStyle(ChatFormatting.GRAY),
                    Component.literal("No rent is charged either.").withStyle(ChatFormatting.GRAY)),
                    mouseX, mouseY);
        }

        if (menu.prop(ShopManageScreenHandler.P_RENT_PAUSED) != 0
                && menu.prop(ShopManageScreenHandler.P_ADMIN_MODE) == 0
                && over(mouseX, mouseY, leftPos + 74, topPos + 24, 100, 10)) {
            ctx.renderComponentTooltip(this.font, List.of(
                    Component.literal("Rent is overdue").withStyle(ChatFormatting.RED),
                    Component.literal("Sales are paused until it is paid.").withStyle(ChatFormatting.GRAY)),
                    mouseX, mouseY);
        }

        if (over(mouseX, mouseY, leftPos + 195, topPos + 59, 14, 14)) {
            ctx.renderComponentTooltip(this.font, List.of(
                    Component.literal("Title color - click to cycle"),
                    Component.literal("Typed &-codes show here too (\"&6Golden Goods\")").withStyle(ChatFormatting.GRAY),
                    Component.literal("Press Set to apply").withStyle(ChatFormatting.GRAY)), mouseX, mouseY);
        }

        for (int i = 0; i < ShopManageScreenHandler.ROWS; i++) {
            Row row = row(i);
            if (row == null) continue;
            int ry = rowY(i);
            if (over(mouseX, mouseY, leftPos + ROW_X, ry, ROW_W - 40, ROW_H)) {
                List<Component> lines = new ArrayList<>();
                lines.add(row.icon().getHoverName());
                lines.add(NotchWidgets.priceText(row.price(), row.barterName(), row.barterCount()));
                boolean endless = row.stock() < 0;
                lines.add(Component.literal(endless ? "Always in stock"
                                : row.stock() > 0 ? "Stock: " + row.stock() : "Out of stock")
                        .withStyle(endless ? ChatFormatting.GOLD
                                : row.stock() > 0 ? ChatFormatting.GRAY : ChatFormatting.RED));
                ctx.renderComponentTooltip(this.font, lines, mouseX, mouseY);
                break;
            }
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
        if (button == 0) {
            int mx = (int) mouseX, my = (int) mouseY;
            if (menu.prop(ShopManageScreenHandler.P_ADMIN_MODE) == 0
                    && (menu.pendingBalance() > 0 || menu.prop(ShopManageScreenHandler.P_BARTER_COUNT) > 0)
                    && over(mx, my, leftPos + 176, topPos + 40, 72, 15)) {
                NotchWidgets.click();
                NotchPacketsClient.sendShopWithdraw(menu.shopId());
                return true;
            }
            if (over(mx, my, leftPos + 8, topPos + 20, 60, 14)) {
                NotchWidgets.click();
                NotchPacketsClient.sendShopManageAction(ShopManageScreenHandler.ACTION_TOGGLE_OPEN, "", null);
                return true;
            }
            if (menu.prop(ShopManageScreenHandler.P_IS_OP) != 0
                    && over(mx, my, leftPos + 186, topPos + 20, 62, 14)) {
                NotchWidgets.click();
                NotchPacketsClient.sendShopManageAction(ShopManageScreenHandler.ACTION_TOGGLE_ADMIN, "", null);
                return true;
            }
            if (over(mx, my, leftPos + 195, topPos + 59, 14, 14)) {
                NotchWidgets.tick();
                cycleTitleColor();
                return true;
            }
            if (over(mx, my, leftPos + 212, topPos + 59, 36, 14)) {
                NotchWidgets.click();
                NotchPacketsClient.sendShopManageAction(ShopManageScreenHandler.ACTION_RENAME,
                        nameField.getValue().trim(), null);
                return true;
            }
            if (over(mx, my, leftPos + 212, topPos + 77, 36, 14)) {
                NotchWidgets.click();
                NotchPacketsClient.sendShopManageAction(ShopManageScreenHandler.ACTION_GREETING,
                        greetField.getValue().trim(), null);
                return true;
            }
            int pageCount = menu.prop(ShopManageScreenHandler.P_TOTAL_PAGES);
            if (pageCount > 1) {
                if (over(mx, my, leftPos + 196, topPos + 97, 13, 11)) { NotchWidgets.tick(); clickButton(0); return true; }
                if (over(mx, my, leftPos + 233, topPos + 97, 13, 11)) { NotchWidgets.tick(); clickButton(1); return true; }
            }
            for (int i = 0; i < ShopManageScreenHandler.ROWS; i++) {
                Row row = row(i);
                if (row != null && over(mx, my, leftPos + ROW_X + 204, rowY(i) + 1, 32, 15)) {
                    NotchWidgets.click();
                    NotchPacketsClient.sendShopManageAction(ShopManageScreenHandler.ACTION_EDIT_LISTING,
                            "", row.listingId());
                    return true;
                }
            }
            if (over(mx, my, leftPos + 8, topPos + 224, 240, 15)) {
                NotchWidgets.click();
                NotchPacketsClient.sendShopManageAction(ShopManageScreenHandler.ACTION_NEW_LISTING, "", null);
                return true;
            }
        }
        //? if >=1.21.11 {
        /*return super.mouseClicked(event, doubleClick);
        *///?} else {
        return super.mouseClicked(mouseX, mouseY, button);
        //?}
    }

    private void clickButton(int id) {
        if (this.minecraft != null && this.minecraft.gameMode != null) {
            this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        }
    }

    private boolean over(int mx, int my, int bx, int by, int bw, int bh) {
        return mx >= bx && mx < bx + bw && my >= by && my < by + bh;
    }

    //? if >=1.21.11 {
    /*@Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int keyCode = event.key(), scanCode = event.scancode(), modifiers = event.modifiers();
    *///?} else {
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    //?}
        if (NotchWidgets.typingInField(keyCode, scanCode, modifiers, nameField, greetField)) return true;
        //? if >=1.21.11 {
        /*return super.keyPressed(event);
        *///?} else {
        return super.keyPressed(keyCode, scanCode, modifiers);
        //?}
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
}
