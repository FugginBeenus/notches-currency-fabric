package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fugginbeenus.notchcurrency.auction.AuctionCategories;
import net.fugginbeenus.notchcurrency.auction.AuctionConfig;
import net.fugginbeenus.notchcurrency.auction.AuctionHouseScreenHandler;
import net.fugginbeenus.notchcurrency.auction.AuctionListing;
import net.fugginbeenus.notchcurrency.auction.AuctionState;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.core.CoinEconomy;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.crate.BalloonEntity;
import net.fugginbeenus.notchcurrency.crate.DailyCrateManager;
import net.fugginbeenus.notchcurrency.crate.GoldenCacheManager;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.fugginbeenus.notchcurrency.trade.TradeManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.Collection;
import java.util.UUID;

import static net.fugginbeenus.notchcurrency.core.NotchCurrency.coinIcon;
import static net.fugginbeenus.notchcurrency.core.NotchCurrency.coins;

public final class ShopCommands {

    private ShopCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("shop")
                        .then(Commands.literal("create")
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayer();
                                            String shopName = StringArgumentType.getString(ctx, "name");
                                            var shop = net.fugginbeenus.notchcurrency.shop.PlayerShopManager.createShop(p, shopName);
                                            return shop != null ? 1 : 0;
                                        })
                                )
                        )

                        .then(Commands.literal("list")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(p.serverLevel());
                                    var shops = state.getShopsByOwner(p.getUUID());

                                    if (shops.isEmpty()) {
                                        net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("You don't have any shops. Use /shop create <name> to create one.")
                                                .withStyle(ChatFormatting.YELLOW));
                                        return 0;
                                    }

                                    net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Your Shops:").withStyle(ChatFormatting.GOLD));
                                    for (var shop : shops) {
                                        Component status = shop.isOpen()
                                                ? Component.literal("OPEN").withStyle(net.minecraft.ChatFormatting.GREEN)
                                                : Component.literal("CLOSED").withStyle(net.minecraft.ChatFormatting.RED);
                                        int listings = shop.getListings().size();
                                        net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal(" - " + shop.getShopName() + " [").append(status)
                                                .append(Component.literal("] (" + listings + " items)"))
                                                .append(Component.literal(" [" + shop.getShopId().toString().substring(0, 8) + "...]")
                                                        .withStyle(ChatFormatting.GRAY)));
                                    }
                                    return 1;
                                })
                        )

                        .then(Commands.literal("open")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayer();
                                            String raw = StringArgumentType.getString(ctx, "id");

                                            UUID shopId = findShopByIdOrName(p, raw);
                                            if (shopId == null) return 0;

                                            net.fugginbeenus.notchcurrency.shop.NpcShopLogic.openShop(p, shopId);
                                            return 1;
                                        })
                                )
                        )

                        .then(Commands.literal("relink")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayer();
                                            if (p == null) { ctx.getSource().sendFailure(Component.literal("Run as a player.")); return 0; }
                                            return relink(p, StringArgumentType.getString(ctx, "id"));
                                        })
                                )
                        )

                        .then(Commands.literal("browse")
                                .executes(ctx -> {
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(p.serverLevel());
                                    var shops = state.getAllOpenShops();

                                    if (shops.isEmpty()) {
                                        net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("No shops are currently open.")
                                                .withStyle(ChatFormatting.YELLOW));
                                        return 0;
                                    }

                                    net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Open Shops:").withStyle(ChatFormatting.GOLD));
                                    for (var shop : shops) {
                                        int listings = shop.getInStockListings().size();
                                        net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal(" - " + shop.getShopName() + " by " + shop.getOwnerName() + " (" + listings + " items)")
                                                .append(Component.literal(" [" + shop.getShopId().toString().substring(0, 8) + "...]")
                                                        .withStyle(ChatFormatting.GRAY)));
                                    }
                                    net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Use /shop visit <id> to browse a shop.").withStyle(ChatFormatting.YELLOW));
                                    return 1;
                                })
                        )

                        .then(Commands.literal("visit")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayer();
                                            String raw = StringArgumentType.getString(ctx, "id");

                                            UUID shopId = findShopByIdOrName(p, raw);
                                            if (shopId == null) return 0;

                                            var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(p.serverLevel());
                                            var shop = state.getShop(shopId);

                                            if (shop == null) {
                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Shop not found!").withStyle(ChatFormatting.RED));
                                                return 0;
                                            }

                                            if (!shop.isOpen() && !shop.getOwnerId().equals(p.getUUID())) {
                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("This shop is currently closed.").withStyle(ChatFormatting.RED));
                                                return 0;
                                            }

                                            net.fugginbeenus.notchcurrency.shop.NpcShopLogic.openShop(p, shopId);
                                            return 1;
                                        })
                                )
                        )

                        .then(Commands.literal("delete")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayer();
                                            String raw = StringArgumentType.getString(ctx, "id");

                                            UUID shopId = findShopByIdOrName(p, raw);
                                            if (shopId == null) return 0;

                                            boolean success = net.fugginbeenus.notchcurrency.shop.PlayerShopManager.deleteShop(p, shopId);
                                            return success ? 1 : 0;
                                        })
                                )
                        )

                        .then(Commands.literal("linknpc")
                                .requires(net.fugginbeenus.notchcurrency.compat.Perms::isOperator)
                                .then(Commands.argument("shopId", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayer();
                                            String raw = StringArgumentType.getString(ctx, "shopId");

                                            var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(p.serverLevel());
                                            UUID shopId = findShopByIdOrNameAdmin(p, raw, state);
                                            if (shopId == null) return 0;
                                            var hit = p.pick(5.0, 0.0f, false);
                                            if (hit.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY) {
                                                var entityHit = (net.minecraft.world.phys.EntityHitResult) hit;
                                                UUID npcId = entityHit.getEntity().getUUID();

                                                state.linkNpcToShop(npcId, shopId);
                                                state.markDirtyAndSave();

                                                var shop = state.getShop(shopId);
                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("✓ Linked NPC to shop: " + (shop != null ? shop.getShopName() : shopId)).withStyle(ChatFormatting.GREEN));
                                                return 1;
                                            } else {
                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Look at an NPC to link it!").withStyle(ChatFormatting.RED));
                                                return 0;
                                            }
                                        })
                                )
                        )

                        .then(Commands.literal("toggle")
                                .then(Commands.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayer();
                                            String raw = StringArgumentType.getString(ctx, "id");

                                            UUID shopId = findShopByIdOrName(p, raw);
                                            if (shopId == null) return 0;

                                            var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(p.serverLevel());
                                            var shop = state.getShop(shopId);

                                            if (shop == null || !shop.getOwnerId().equals(p.getUUID())) {
                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("You don't own this shop!").withStyle(ChatFormatting.RED));
                                                return 0;
                                            }

                                            shop.setOpen(!shop.isOpen());
                                            state.markDirtyAndSave();

                                            Component status = shop.isOpen()
                                                ? Component.literal("OPEN").withStyle(net.minecraft.ChatFormatting.GREEN)
                                                : Component.literal("CLOSED").withStyle(net.minecraft.ChatFormatting.RED);
                                            net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Shop is now: " + status));
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("admin")
                                .requires(net.fugginbeenus.notchcurrency.compat.Perms::isOperator)

                                .then(Commands.literal("list")
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayer();
                                            var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(p.serverLevel());
                                            var shops = state.getAllShops();

                                            if (shops.isEmpty()) {
                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("No shops exist on this server.").withStyle(ChatFormatting.YELLOW));
                                                return 0;
                                            }

                                            net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("=== All Shops (" + shops.size() + ") ===").withStyle(ChatFormatting.GOLD));
                                            for (var shop : shops) {
                                                Component status = shop.isOpen()
                                                ? Component.literal("OPEN").withStyle(net.minecraft.ChatFormatting.GREEN)
                                                : Component.literal("CLOSED").withStyle(net.minecraft.ChatFormatting.RED);
                                                Component npcStatus = shop.getLinkedNpcId() != null
                                                ? Component.literal("Linked").withStyle(net.minecraft.ChatFormatting.GREEN)
                                                : Component.literal("No NPC").withStyle(net.minecraft.ChatFormatting.GRAY);
                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal(" • " + shop.getShopName())
                                                        .withStyle(ChatFormatting.WHITE)
                                                        .append(Component.literal(" [")).append(status).append(Component.literal("] "))
                                                        .append(Component.literal("[")).append(npcStatus).append(Component.literal("]")));
                                                String fullId = shop.getShopId().toString();
                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("   Owner: ").withStyle(ChatFormatting.GRAY)
                                                        .append(Component.literal(shop.getOwnerName()).withStyle(ChatFormatting.AQUA))
                                                        .append(Component.literal(" | ID: ").withStyle(ChatFormatting.GRAY))
                                                        .append(Component.literal(fullId).withStyle(ChatFormatting.DARK_GRAY)
                                                                .withStyle(style -> style
                                                                        .withClickEvent(net.fugginbeenus.notchcurrency.compat.Chat.copyToClipboard(fullId))
                                                                        .withHoverEvent(net.fugginbeenus.notchcurrency.compat.Chat.showText(Component.literal("Click to copy ID"))))));
                                            }
                                            return 1;
                                        })
                                )

                                .then(Commands.literal("delete")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    ServerPlayer p = ctx.getSource().getPlayer();
                                                    String raw = StringArgumentType.getString(ctx, "id");

                                                    var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(p.serverLevel());
                                                    UUID shopId = findShopByIdOrNameAdmin(p, raw, state);
                                                    if (shopId == null) return 0;

                                                    var shop = state.getShop(shopId);
                                                    if (shop == null) {
                                                        net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Shop not found!").withStyle(ChatFormatting.RED));
                                                        return 0;
                                                    }

                                                    ServerPlayer owner = p.level().getServer().getPlayerList().getPlayer(shop.getOwnerId());
                                                    net.fugginbeenus.notchcurrency.shop.PlayerShopManager.returnAllShopContents(p.level().getServer(), shop, owner);

                                                    if (shop.getLinkedNpcId() != null) {
                                                        var npc = p.serverLevel().getEntity(shop.getLinkedNpcId());
                                                        if (npc != null) npc.discard();
                                                    }

                                                    state.removeShop(shopId);
                                                    state.markDirtyAndSave();

                                                    net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("✓ Deleted shop: " + shop.getShopName() + " (owned by " + shop.getOwnerName() + ")")
                                                            .withStyle(ChatFormatting.GREEN));
                                                    return 1;
                                                })
                                        )
                                )

                                .then(Commands.literal("info")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    ServerPlayer p = ctx.getSource().getPlayer();
                                                    String raw = StringArgumentType.getString(ctx, "id");

                                                    var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(p.serverLevel());
                                                    UUID shopId = findShopByIdOrNameAdmin(p, raw, state);
                                                    if (shopId == null) return 0;

                                                    var shop = state.getShop(shopId);
                                                    if (shop == null) {
                                                        net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Shop not found!").withStyle(ChatFormatting.RED));
                                                        return 0;
                                                    }

                                                    net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("=== Shop Info ===").withStyle(ChatFormatting.GOLD));
                                                    net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Name: " + shop.getShopName()));
                                                    net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Owner: " + shop.getOwnerName() + " (" + shop.getOwnerId() + ")"));
                                                    net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Shop ID: " + shop.getShopId()));
                                                    net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Status: ").append(shop.isOpen()
                                                            ? Component.literal("OPEN").withStyle(net.minecraft.ChatFormatting.GREEN)
                                                            : Component.literal("CLOSED").withStyle(net.minecraft.ChatFormatting.RED)));
                                                    net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("NPC: " + (shop.getLinkedNpcId() != null ? shop.getLinkedNpcId().toString() : "None")));
                                                    net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Listings: " + shop.getListings().size()));
                                                    net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Pending Balance: " + shop.getPendingBalance() + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word()));
                                                    net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Total Revenue: " + shop.getTotalRevenue() + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word()));
                                                    net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Total Transactions: " + shop.getTotalTransactions()));
                                                    return 1;
                                                })
                                        )
                                )

                                .then(Commands.literal("transfer")
                                        .then(Commands.argument("id", StringArgumentType.word())
                                                .then(Commands.argument("newOwner", EntityArgument.player())
                                                        .executes(ctx -> {
                                                            ServerPlayer p = ctx.getSource().getPlayer();
                                                            String raw = StringArgumentType.getString(ctx, "id");
                                                            ServerPlayer newOwner = EntityArgument.getPlayer(ctx, "newOwner");

                                                            var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(p.serverLevel());
                                                            UUID shopId = findShopByIdOrNameAdmin(p, raw, state);
                                                            if (shopId == null) return 0;

                                                            boolean success = net.fugginbeenus.notchcurrency.shop.PlayerShopManager.transferOwnership(
                                                                    p.level().getServer(), shopId, newOwner.getUUID(), newOwner.getName().getString());

                                                            if (success) {
                                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("✓ Transferred shop to " + newOwner.getName().getString())
                                                                        .withStyle(ChatFormatting.GREEN));
                                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(newOwner, Component.literal("You are now the owner of a shop!")
                                                                        .withStyle(ChatFormatting.GREEN));
                                                            } else {
                                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Failed to transfer shop!").withStyle(ChatFormatting.RED));
                                                            }
                                                            return success ? 1 : 0;
                                                        })
                                                )
                                        )
                                )

                                .then(Commands.literal("cleanup")
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayer();
                                            var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(p.serverLevel());
                                            var sweep = state.cleanupOrphans(ctx.getSource().getServer(), false);

                                            if (sweep.missing() == 0) {
                                                net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Every shop's NPC was found. Nothing to do.").withStyle(ChatFormatting.GREEN));
                                                return 1;
                                            }
                                            net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal(sweep.missing() + " shop(s) have an NPC I could not find.").withStyle(ChatFormatting.YELLOW));
                                            net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("That includes NPCs standing in chunks nobody has loaded, which are fine. Load the area and run this again before you act on it.").withStyle(ChatFormatting.GRAY));
                                            net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("If you are sure: /shop admin cleanup confirm. This cannot be undone in game.").withStyle(ChatFormatting.GRAY));
                                            return 1;
                                        })
                                        .then(Commands.literal("confirm")
                                                .executes(ctx -> {
                                                    ServerPlayer p = ctx.getSource().getPlayer();
                                                    var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(p.serverLevel());
                                                    var sweep = state.cleanupOrphans(ctx.getSource().getServer(), true);
                                                    net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Unlinked " + sweep.unlinked() + " shop(s) from a missing NPC. The shops and everything in them are untouched.").withStyle(ChatFormatting.GREEN));
                                                    return 1;
                                                })
                                        )
                                )
                        )
        );
    }

    private static int relink(ServerPlayer p, String input) {
        var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(p.serverLevel());
        UUID shopId = findShopByIdOrName(p, input);
        if (shopId == null) return 0;

        var shop = state.getShop(shopId);
        if (shop == null || !p.getUUID().equals(shop.getOwnerId())) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("That isn't your shop.").withStyle(ChatFormatting.RED));
            return 0;
        }

        net.minecraft.world.entity.Entity target = NpcCommands.lookedAt(p);
        if (!(target instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc)) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("Look at the NPC you want running this shop, then run it again.").withStyle(ChatFormatting.RED));
            return 0;
        }
        if (!npc.canEdit(p)) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("That isn't your NPC.").withStyle(ChatFormatting.RED));
            return 0;
        }

        var already = state.getShopByNpc(npc.getUUID());
        if (already != null && !already.getShopId().equals(shopId)) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("That NPC already runs \"" + already.getShopName() + "\". Use a different NPC.").withStyle(ChatFormatting.RED));
            return 0;
        }

        var role = npc.getRole();
        if (role != net.fugginbeenus.notchcurrency.economy.npc.NpcRole.NONE
                && role != net.fugginbeenus.notchcurrency.economy.npc.NpcRole.SHOP) {
            net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("That NPC is a "
                    + role.name().toLowerCase().replace('_', ' ')
                    + ". Use one with no job, or change its role first.").withStyle(ChatFormatting.RED));
            return 0;
        }
        npc.setRole(net.fugginbeenus.notchcurrency.economy.npc.NpcRole.SHOP);
        state.linkNpcToShop(npc.getUUID(), shopId);
        state.markDirtyAndSave();
        net.fugginbeenus.notchcurrency.compat.Msg.chat(p, Component.literal("\"" + shop.getShopName() + "\" is now run by this NPC. Everything in it is exactly where you left it.").withStyle(ChatFormatting.GREEN));
        return 1;
    }

    private static UUID findShopByIdOrName(ServerPlayer player, String input) {
        var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(player.serverLevel());

        try {
            UUID id = UUID.fromString(input);
            if (state.getShop(id) != null) {
                return id;
            }
        } catch (IllegalArgumentException ignored) {}

        for (var shop : state.getAllShops()) {
            if (shop.getShopId().toString().startsWith(input)) {
                return shop.getShopId();
            }
        }

        var ownedShops = state.getShopsByOwner(player.getUUID());
        for (var shop : ownedShops) {
            if (shop.getShopName().equalsIgnoreCase(input)) {
                return shop.getShopId();
            }
        }

        for (var shop : state.getAllShops()) {
            if (shop.getShopName().equalsIgnoreCase(input)) {
                return shop.getShopId();
            }
        }

        net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("Shop not found: " + input).withStyle(ChatFormatting.RED));
        return null;
    }

    private static UUID findShopByIdOrNameAdmin(ServerPlayer player, String input, net.fugginbeenus.notchcurrency.shop.ShopState state) {

        try {
            UUID id = UUID.fromString(input);
            if (state.getShop(id) != null) {
                return id;
            }
        } catch (IllegalArgumentException ignored) {}

        for (var shop : state.getAllShops()) {
            if (shop.getShopId().toString().startsWith(input)) {
                return shop.getShopId();
            }
        }

        for (var shop : state.getAllShops()) {
            if (shop.getShopName().toLowerCase().contains(input.toLowerCase())) {
                return shop.getShopId();
            }
        }

        net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("Shop not found: " + input).withStyle(ChatFormatting.RED));
        return null;
    }
}
