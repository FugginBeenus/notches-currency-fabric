package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fugginbeenus.notchcurrency.economy.adminshop.AdminShop;
import net.fugginbeenus.notchcurrency.economy.adminshop.AdminShopEntry;
import net.fugginbeenus.notchcurrency.economy.adminshop.AdminShopManager;
import net.fugginbeenus.notchcurrency.economy.adminshop.AdminShopMenu;
import net.fugginbeenus.notchcurrency.economy.adminshop.AdminShopState;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import java.util.UUID;

public final class AdminShopCommands {

    private AdminShopCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("adminshop")
                .then(Commands.literal("create")
                        .requires(net.fugginbeenus.notchcurrency.compat.Perms::isOperator)
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    AdminShopState state = AdminShopState.get(ctx.getSource().getServer());
                                    String name = StringArgumentType.getString(ctx, "name");
                                    if (state.getByName(name) != null) {
                                        ctx.getSource().sendFailure(Component.literal("A shop named '" + name + "' already exists."));
                                        return 0;
                                    }
                                    AdminShop shop = state.create(name);
                                    ctx.getSource().sendSuccess(() -> Component.literal("Created admin shop '" + shop.getName() + "'.")
                                            .withStyle(ChatFormatting.GREEN), true);
                                    return 1;
                                })))
                .then(Commands.literal("delete")
                        .requires(net.fugginbeenus.notchcurrency.compat.Perms::isOperator)
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    AdminShopState state = AdminShopState.get(ctx.getSource().getServer());
                                    AdminShop shop = state.getByName(StringArgumentType.getString(ctx, "name"));
                                    if (shop == null) { ctx.getSource().sendFailure(Component.literal("No such shop.")); return 0; }
                                    state.remove(shop.getId());
                                    ctx.getSource().sendSuccess(() -> Component.literal("Deleted shop.").withStyle(ChatFormatting.YELLOW), true);
                                    return 1;
                                })))
                .then(Commands.literal("list")
                        .requires(net.fugginbeenus.notchcurrency.compat.Perms::isOperator)
                        .executes(ctx -> {
                            AdminShopState state = AdminShopState.get(ctx.getSource().getServer());
                            if (state.all().isEmpty()) {
                                ctx.getSource().sendSuccess(() -> Component.literal("No admin shops.").withStyle(ChatFormatting.GRAY), false);
                                return 0;
                            }
                            ctx.getSource().sendSuccess(() -> Component.literal("Admin shops:").withStyle(ChatFormatting.GOLD), false);
                            for (AdminShop s : state.all()) {
                                ctx.getSource().sendSuccess(() -> Component.literal(" • " + s.getName() + " (" + s.getEntries().size() + " items)")
                                        .withStyle(ChatFormatting.WHITE), false);
                            }
                            return 1;
                        }))
                .then(Commands.literal("additem")
                        .requires(net.fugginbeenus.notchcurrency.compat.Perms::isOperator)
                        .then(Commands.argument("shop", StringArgumentType.string())
                                .then(Commands.argument("buyPrice", LongArgumentType.longArg(0))
                                        .then(Commands.argument("sellPrice", LongArgumentType.longArg(0))
                                                .executes(ctx -> addItem(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "shop"),
                                                        LongArgumentType.getLong(ctx, "buyPrice"),
                                                        LongArgumentType.getLong(ctx, "sellPrice"), false))
                                                .then(Commands.argument("dynamic", BoolArgumentType.bool())
                                                        .executes(ctx -> addItem(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "shop"),
                                                                LongArgumentType.getLong(ctx, "buyPrice"),
                                                                LongArgumentType.getLong(ctx, "sellPrice"),
                                                                BoolArgumentType.getBool(ctx, "dynamic"))))))))
                .then(Commands.literal("info")
                        .requires(net.fugginbeenus.notchcurrency.compat.Perms::isOperator)
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> info(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(Commands.literal("limit")
                        .requires(net.fugginbeenus.notchcurrency.compat.Perms::isOperator)
                        .then(Commands.argument("shopId", StringArgumentType.word())
                                .then(Commands.argument("entryId", StringArgumentType.word())
                                        .then(Commands.argument("buyLimit", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 9999))
                                                .then(Commands.argument("sellLimit", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0, 9999))
                                                        .then(Commands.argument("resetEvery", StringArgumentType.word())
                                                                .executes(ctx -> setLimit(ctx))))))))
                .then(Commands.literal("removeitem")
                        .requires(net.fugginbeenus.notchcurrency.compat.Perms::isOperator)
                        .then(Commands.argument("shopId", StringArgumentType.word())
                                .then(Commands.argument("entryId", StringArgumentType.word())
                                        .executes(ctx -> {
                                            AdminShopState state = AdminShopState.get(ctx.getSource().getServer());
                                            AdminShop shop = byId(state, ctx, "shopId");
                                            if (shop == null) { ctx.getSource().sendFailure(Component.literal("Shop not found.")); return 0; }
                                            UUID entryId = parseUuid(StringArgumentType.getString(ctx, "entryId"));
                                            if (entryId != null && shop.removeEntry(entryId)) {
                                                AdminShopState.get(ctx.getSource().getServer()).setDirty();
                                                ctx.getSource().sendSuccess(() -> Component.literal("Item removed.").withStyle(ChatFormatting.YELLOW), false);
                                                return 1;
                                            }
                                            ctx.getSource().sendFailure(Component.literal("Item not found."));
                                            return 0;
                                        }))))

                .then(Commands.literal("open")
                        .then(Commands.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    AdminShop shop = AdminShopState.get(ctx.getSource().getServer())
                                            .getByName(StringArgumentType.getString(ctx, "name"));
                                    if (p == null || shop == null) { ctx.getSource().sendFailure(Component.literal("No such shop.")); return 0; }
                                    AdminShopMenu.sendListing(p, shop);
                                    return 1;
                                })))
                .then(Commands.literal("buy")
                        .then(Commands.argument("shopId", StringArgumentType.word())
                                .then(Commands.argument("entryId", StringArgumentType.word())
                                        .then(Commands.argument("qty", IntegerArgumentType.integer(1, 256))
                                                .executes(ctx -> trade(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "shopId"),
                                                        StringArgumentType.getString(ctx, "entryId"),
                                                        IntegerArgumentType.getInteger(ctx, "qty"), true))))))
                .then(Commands.literal("sell")
                        .then(Commands.argument("shopId", StringArgumentType.word())
                                .then(Commands.argument("entryId", StringArgumentType.word())
                                        .then(Commands.argument("qty", IntegerArgumentType.integer(1, 256))
                                                .executes(ctx -> trade(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "shopId"),
                                                        StringArgumentType.getString(ctx, "entryId"),
                                                        IntegerArgumentType.getInteger(ctx, "qty"), false))))))
        );
    }

    private static int addItem(CommandSourceStack src, String shopName, long buy, long sell, boolean dynamic) {
        ServerPlayer p = src.getPlayer();
        if (p == null) { src.sendFailure(Component.literal("Run as a player (uses your held item).")); return 0; }
        AdminShop shop = AdminShopState.get(src.getServer()).getByName(shopName);
        if (shop == null) { src.sendFailure(Component.literal("No such shop: " + shopName)); return 0; }
        ItemStack held = p.getMainHandItem();
        if (held.isEmpty()) { src.sendFailure(Component.literal("Hold the item you want to list.")); return 0; }
        if (buy <= 0 && sell <= 0) { src.sendFailure(Component.literal("Set a buy and/or sell price above 0.")); return 0; }

        AdminShopEntry entry = new AdminShopEntry(held.copy(), buy, sell, dynamic);
        shop.addEntry(entry);
        AdminShopState.get(src.getServer()).setDirty();
        src.sendSuccess(() -> Component.literal("Added ")
                .append(held.getHoverName())
                .append(Component.literal(" (buy " + buy + ", sell " + sell + (dynamic ? ", dynamic" : "") + ").")
                        .withStyle(ChatFormatting.GREEN)), true);
        return 1;
    }

    private static int info(CommandSourceStack src, String name) {
        AdminShop shop = AdminShopState.get(src.getServer()).getByName(name);
        if (shop == null) { src.sendFailure(Component.literal("No such shop.")); return 0; }
        src.sendSuccess(() -> Component.literal("═ " + shop.getName() + " ═").withStyle(ChatFormatting.GOLD), false);
        for (AdminShopEntry e : shop.getEntries()) {
            MutableComponent line = Component.literal(" • ").withStyle(ChatFormatting.DARK_GRAY)
                    .append(e.getItem().getHoverName().copy().withStyle(ChatFormatting.WHITE))
                    .append(Component.literal("  buy " + e.getBaseBuyPrice() + " / sell " + e.getBaseSellPrice()
                            + (e.isDynamic() ? " (dyn)" : "")).withStyle(ChatFormatting.GRAY))
                    .append(Component.literal("  "))
                    .append(Component.literal("[X]").withStyle(ChatFormatting.RED).withStyle(s -> s
                            .withClickEvent(net.fugginbeenus.notchcurrency.compat.Chat.runCommand("/adminshop removeitem " + shop.getId() + " " + e.getId()))
                            .withHoverEvent(net.fugginbeenus.notchcurrency.compat.Chat.showText(Component.literal("Remove this item")))));
            src.sendSuccess(() -> line, false);
        }
        return 1;
    }

    private static int trade(CommandSourceStack src, String shopIdStr, String entryIdStr, int qty, boolean buying) {
        ServerPlayer p = src.getPlayer();
        if (p == null) return 0;
        AdminShopState state = AdminShopState.get(src.getServer());
        UUID shopId = parseUuid(shopIdStr);
        UUID entryId = parseUuid(entryIdStr);
        AdminShop shop = shopId == null ? null : state.get(shopId);
        AdminShopEntry entry = shop == null || entryId == null ? null : shop.getEntry(entryId);
        if (shop == null || entry == null) { src.sendFailure(Component.literal("That item is no longer available.")); return 0; }

        AdminShopManager.Result r = buying
                ? AdminShopManager.buy(p, shop, entry, qty)
                : AdminShopManager.sell(p, shop, entry, qty);
        state.setDirty();

        if (r != AdminShopManager.Result.SUCCESS) {
            String msg = switch (r) {
                case BUY_LIMIT_REACHED -> "You have hit the buying limit for this item. Try again after the next reset.";
                case SELL_LIMIT_REACHED -> "You have hit the selling limit for this item. Try again after the next reset.";
                case NOT_BUYABLE -> "This item isn't for sale here.";
                case NOT_SELLABLE -> "This shop doesn't buy that item.";
                case INSUFFICIENT_FUNDS -> "You don't have enough " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + "!";
                case INSUFFICIENT_ITEMS -> "You don't have enough of that item!";
                case INVALID_QUANTITY -> "Invalid quantity.";
                default -> "Trade failed.";
            };
            net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal(msg).withStyle(ChatFormatting.RED));
            return 0;
        }
        return 1;
    }

    private static AdminShop byId(AdminShopState state, com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx, String arg) {
        UUID id = parseUuid(StringArgumentType.getString(ctx, arg));
        return id == null ? null : state.get(id);
    }

    private static int setLimit(com.mojang.brigadier.context.CommandContext<net.minecraft.commands.CommandSourceStack> ctx) {
        AdminShopState state = AdminShopState.get(ctx.getSource().getServer());
        AdminShop shop = byId(state, ctx, "shopId");
        if (shop == null) { ctx.getSource().sendFailure(Component.literal("Shop not found.")); return 0; }
        UUID entryId = parseUuid(StringArgumentType.getString(ctx, "entryId"));
        AdminShopEntry entry = entryId == null ? null : shop.getEntry(entryId);
        if (entry == null) { ctx.getSource().sendFailure(Component.literal("Item not found.")); return 0; }

        String modeName = StringArgumentType.getString(ctx, "resetEvery").toUpperCase(java.util.Locale.ROOT);
        net.fugginbeenus.notchcurrency.shop.Restock.Mode mode =
                net.fugginbeenus.notchcurrency.shop.Restock.Mode.byName(modeName);
        if (mode == net.fugginbeenus.notchcurrency.shop.Restock.Mode.OFF && !modeName.equals("OFF")) {
            ctx.getSource().sendFailure(Component.literal(
                    "Reset must be one of: OFF, GAME_DAILY, GAME_WEEKLY, REAL_DAILY, REAL_WEEKLY."));
            return 0;
        }

        int buyLimit = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "buyLimit");
        int sellLimit = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "sellLimit");
        entry.setBuyLimit(buyLimit);
        entry.setSellLimit(sellLimit);
        entry.setResetMode(mode);
        state.setDirty();

        ctx.getSource().sendSuccess(() -> Component.literal("Limits set: buy "
                + (buyLimit == 0 ? "unlimited" : String.valueOf(buyLimit)) + ", sell "
                + (sellLimit == 0 ? "unlimited" : String.valueOf(sellLimit)) + ", reset "
                + mode.label().toLowerCase(java.util.Locale.ROOT) + ".").withStyle(ChatFormatting.GREEN), false);
        return 1;
    }

    private static UUID parseUuid(String s) {
        try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }
}
