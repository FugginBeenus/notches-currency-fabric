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
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.UUID;

/**
 * {@code /adminshop} — admins create/configure server shops; everyone can buy/sell.
 * Buy/sell take raw UUID args but players never type them: they come from the clickable
 * chat menu ({@link AdminShopMenu}).
 */
public final class AdminShopCommands {

    private AdminShopCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("adminshop")
                // ---- admin setup ----
                .then(CommandManager.literal("create")
                        .requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    AdminShopState state = AdminShopState.get(ctx.getSource().getServer());
                                    String name = StringArgumentType.getString(ctx, "name");
                                    if (state.getByName(name) != null) {
                                        ctx.getSource().sendError(Text.literal("A shop named '" + name + "' already exists."));
                                        return 0;
                                    }
                                    AdminShop shop = state.create(name);
                                    ctx.getSource().sendFeedback(() -> Text.literal("Created admin shop '" + shop.getName() + "'.")
                                            .formatted(Formatting.GREEN), true);
                                    return 1;
                                })))
                .then(CommandManager.literal("delete")
                        .requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    AdminShopState state = AdminShopState.get(ctx.getSource().getServer());
                                    AdminShop shop = state.getByName(StringArgumentType.getString(ctx, "name"));
                                    if (shop == null) { ctx.getSource().sendError(Text.literal("No such shop.")); return 0; }
                                    state.remove(shop.getId());
                                    ctx.getSource().sendFeedback(() -> Text.literal("Deleted shop.").formatted(Formatting.YELLOW), true);
                                    return 1;
                                })))
                .then(CommandManager.literal("list")
                        .requires(s -> s.hasPermissionLevel(2))
                        .executes(ctx -> {
                            AdminShopState state = AdminShopState.get(ctx.getSource().getServer());
                            if (state.all().isEmpty()) {
                                ctx.getSource().sendFeedback(() -> Text.literal("No admin shops.").formatted(Formatting.GRAY), false);
                                return 0;
                            }
                            ctx.getSource().sendFeedback(() -> Text.literal("Admin shops:").formatted(Formatting.GOLD), false);
                            for (AdminShop s : state.all()) {
                                ctx.getSource().sendFeedback(() -> Text.literal(" • " + s.getName() + " (" + s.getEntries().size() + " items)")
                                        .formatted(Formatting.WHITE), false);
                            }
                            return 1;
                        }))
                .then(CommandManager.literal("additem")
                        .requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.argument("shop", StringArgumentType.string())
                                .then(CommandManager.argument("buyPrice", LongArgumentType.longArg(0))
                                        .then(CommandManager.argument("sellPrice", LongArgumentType.longArg(0))
                                                .executes(ctx -> addItem(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "shop"),
                                                        LongArgumentType.getLong(ctx, "buyPrice"),
                                                        LongArgumentType.getLong(ctx, "sellPrice"), false))
                                                .then(CommandManager.argument("dynamic", BoolArgumentType.bool())
                                                        .executes(ctx -> addItem(ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "shop"),
                                                                LongArgumentType.getLong(ctx, "buyPrice"),
                                                                LongArgumentType.getLong(ctx, "sellPrice"),
                                                                BoolArgumentType.getBool(ctx, "dynamic"))))))))
                .then(CommandManager.literal("info")
                        .requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> info(ctx.getSource(), StringArgumentType.getString(ctx, "name")))))
                .then(CommandManager.literal("removeitem")
                        .requires(s -> s.hasPermissionLevel(2))
                        .then(CommandManager.argument("shopId", StringArgumentType.word())
                                .then(CommandManager.argument("entryId", StringArgumentType.word())
                                        .executes(ctx -> {
                                            AdminShopState state = AdminShopState.get(ctx.getSource().getServer());
                                            AdminShop shop = byId(state, ctx, "shopId");
                                            if (shop == null) { ctx.getSource().sendError(Text.literal("Shop not found.")); return 0; }
                                            UUID entryId = parseUuid(StringArgumentType.getString(ctx, "entryId"));
                                            if (entryId != null && shop.removeEntry(entryId)) {
                                                AdminShopState.get(ctx.getSource().getServer()).markDirty();
                                                ctx.getSource().sendFeedback(() -> Text.literal("Item removed.").formatted(Formatting.YELLOW), false);
                                                return 1;
                                            }
                                            ctx.getSource().sendError(Text.literal("Item not found."));
                                            return 0;
                                        }))))

                // ---- player-facing ----
                .then(CommandManager.literal("open")
                        .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    AdminShop shop = AdminShopState.get(ctx.getSource().getServer())
                                            .getByName(StringArgumentType.getString(ctx, "name"));
                                    if (p == null || shop == null) { ctx.getSource().sendError(Text.literal("No such shop.")); return 0; }
                                    AdminShopMenu.sendListing(p, shop);
                                    return 1;
                                })))
                .then(CommandManager.literal("buy")
                        .then(CommandManager.argument("shopId", StringArgumentType.word())
                                .then(CommandManager.argument("entryId", StringArgumentType.word())
                                        .then(CommandManager.argument("qty", IntegerArgumentType.integer(1, 256))
                                                .executes(ctx -> trade(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "shopId"),
                                                        StringArgumentType.getString(ctx, "entryId"),
                                                        IntegerArgumentType.getInteger(ctx, "qty"), true))))))
                .then(CommandManager.literal("sell")
                        .then(CommandManager.argument("shopId", StringArgumentType.word())
                                .then(CommandManager.argument("entryId", StringArgumentType.word())
                                        .then(CommandManager.argument("qty", IntegerArgumentType.integer(1, 256))
                                                .executes(ctx -> trade(ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "shopId"),
                                                        StringArgumentType.getString(ctx, "entryId"),
                                                        IntegerArgumentType.getInteger(ctx, "qty"), false))))))
        );
    }

    private static int addItem(ServerCommandSource src, String shopName, long buy, long sell, boolean dynamic) {
        ServerPlayerEntity p = src.getPlayer();
        if (p == null) { src.sendError(Text.literal("Run as a player (uses your held item).")); return 0; }
        AdminShop shop = AdminShopState.get(src.getServer()).getByName(shopName);
        if (shop == null) { src.sendError(Text.literal("No such shop: " + shopName)); return 0; }
        ItemStack held = p.getMainHandStack();
        if (held.isEmpty()) { src.sendError(Text.literal("Hold the item you want to list.")); return 0; }
        if (buy <= 0 && sell <= 0) { src.sendError(Text.literal("Set a buy and/or sell price above 0.")); return 0; }

        AdminShopEntry entry = new AdminShopEntry(held.copy(), buy, sell, dynamic);
        shop.addEntry(entry);
        AdminShopState.get(src.getServer()).markDirty();
        src.sendFeedback(() -> Text.literal("Added ")
                .append(held.getName())
                .append(Text.literal(" (buy " + buy + ", sell " + sell + (dynamic ? ", dynamic" : "") + ").")
                        .formatted(Formatting.GREEN)), true);
        return 1;
    }

    private static int info(ServerCommandSource src, String name) {
        AdminShop shop = AdminShopState.get(src.getServer()).getByName(name);
        if (shop == null) { src.sendError(Text.literal("No such shop.")); return 0; }
        src.sendFeedback(() -> Text.literal("═ " + shop.getName() + " ═").formatted(Formatting.GOLD), false);
        for (AdminShopEntry e : shop.getEntries()) {
            MutableText line = Text.literal(" • ").formatted(Formatting.DARK_GRAY)
                    .append(e.getItem().getName().copy().formatted(Formatting.WHITE))
                    .append(Text.literal("  buy " + e.getBaseBuyPrice() + " / sell " + e.getBaseSellPrice()
                            + (e.isDynamic() ? " (dyn)" : "")).formatted(Formatting.GRAY))
                    .append(Text.literal("  "))
                    .append(Text.literal("[X]").formatted(Formatting.RED).styled(s -> s
                            .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                                    "/adminshop removeitem " + shop.getId() + " " + e.getId()))
                            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("Remove this item")))));
            src.sendFeedback(() -> line, false);
        }
        return 1;
    }

    private static int trade(ServerCommandSource src, String shopIdStr, String entryIdStr, int qty, boolean buying) {
        ServerPlayerEntity p = src.getPlayer();
        if (p == null) return 0;
        AdminShopState state = AdminShopState.get(src.getServer());
        UUID shopId = parseUuid(shopIdStr);
        UUID entryId = parseUuid(entryIdStr);
        AdminShop shop = shopId == null ? null : state.get(shopId);
        AdminShopEntry entry = shop == null || entryId == null ? null : shop.getEntry(entryId);
        if (shop == null || entry == null) { src.sendError(Text.literal("That item is no longer available.")); return 0; }

        AdminShopManager.Result r = buying
                ? AdminShopManager.buy(p, shop, entry, qty)
                : AdminShopManager.sell(p, shop, entry, qty);
        state.markDirty();

        if (r != AdminShopManager.Result.SUCCESS) {
            String msg = switch (r) {
                case NOT_BUYABLE -> "This item isn't for sale here.";
                case NOT_SELLABLE -> "This shop doesn't buy that item.";
                case INSUFFICIENT_FUNDS -> "You don't have enough coins!";
                case INSUFFICIENT_ITEMS -> "You don't have enough of that item!";
                case INVALID_QUANTITY -> "Invalid quantity.";
                default -> "Trade failed.";
            };
            p.sendMessage(Text.literal(msg).formatted(Formatting.RED), false);
            return 0;
        }
        return 1;
    }

    private static AdminShop byId(AdminShopState state, com.mojang.brigadier.context.CommandContext<ServerCommandSource> ctx, String arg) {
        UUID id = parseUuid(StringArgumentType.getString(ctx, arg));
        return id == null ? null : state.get(id);
    }

    private static UUID parseUuid(String s) {
        try { return UUID.fromString(s); } catch (IllegalArgumentException e) { return null; }
    }
}
