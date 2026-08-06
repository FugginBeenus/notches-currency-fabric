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
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.Collection;
import java.util.UUID;

import static net.fugginbeenus.notchcurrency.core.NotchCurrency.coinIcon;
import static net.fugginbeenus.notchcurrency.core.NotchCurrency.coins;

public final class ShopCommands {

    private ShopCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // ===== /shop commands =====
        dispatcher.register(
                CommandManager.literal("shop")
                        // /shop create <name> - Create a new shop
                        .then(CommandManager.literal("create")
                                .then(CommandManager.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            String shopName = StringArgumentType.getString(ctx, "name");
                                            var shop = net.fugginbeenus.notchcurrency.shop.PlayerShopManager.createShop(p, shopName);
                                            return shop != null ? 1 : 0;
                                        })
                                )
                        )

                        // /shop list - List your shops
                        .then(CommandManager.literal("list")
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(p.getServerWorld());
                                    var shops = state.getShopsByOwner(p.getUuid());

                                    if (shops.isEmpty()) {
                                        p.sendMessage(Text.literal("You don't have any shops. Use /shop create <name> to create one.")
                                                .formatted(Formatting.YELLOW), false);
                                        return 0;
                                    }

                                    p.sendMessage(Text.literal("Your Shops:").formatted(Formatting.GOLD), false);
                                    for (var shop : shops) {
                                        String status = shop.isOpen() ? "§aOPEN" : "§cCLOSED";
                                        int listings = shop.getListings().size();
                                        p.sendMessage(Text.literal(" - " + shop.getShopName() + " [" + status + "§r] (" + listings + " items)")
                                                .append(Text.literal(" [" + shop.getShopId().toString().substring(0, 8) + "...]")
                                                        .formatted(Formatting.GRAY)), false);
                                    }
                                    return 1;
                                })
                        )

                        // /shop open <id> - Open your shop for editing
                        .then(CommandManager.literal("open")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            String raw = StringArgumentType.getString(ctx, "id");

                                            UUID shopId = findShopByIdOrName(p, raw);
                                            if (shopId == null) return 0;

                                            net.fugginbeenus.notchcurrency.shop.NpcShopLogic.openShop(p, shopId);
                                            return 1;
                                        })
                                )
                        )

                        // /shop browse - Browse all open shops
                        .then(CommandManager.literal("browse")
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(p.getServerWorld());
                                    var shops = state.getAllOpenShops();

                                    if (shops.isEmpty()) {
                                        p.sendMessage(Text.literal("No shops are currently open.")
                                                .formatted(Formatting.YELLOW), false);
                                        return 0;
                                    }

                                    p.sendMessage(Text.literal("Open Shops:").formatted(Formatting.GOLD), false);
                                    for (var shop : shops) {
                                        int listings = shop.getInStockListings().size();
                                        p.sendMessage(Text.literal(" - " + shop.getShopName() + " by " + shop.getOwnerName() + " (" + listings + " items)")
                                                .append(Text.literal(" [" + shop.getShopId().toString().substring(0, 8) + "...]")
                                                        .formatted(Formatting.GRAY)), false);
                                    }
                                    p.sendMessage(Text.literal("Use /shop visit <id> to browse a shop.").formatted(Formatting.YELLOW), false);
                                    return 1;
                                })
                        )

                        // /shop visit <id> - Visit/browse another player's shop
                        .then(CommandManager.literal("visit")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            String raw = StringArgumentType.getString(ctx, "id");

                                            UUID shopId = findShopByIdOrName(p, raw);
                                            if (shopId == null) return 0;

                                            var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(p.getServerWorld());
                                            var shop = state.getShop(shopId);

                                            if (shop == null) {
                                                p.sendMessage(Text.literal("Shop not found!").formatted(Formatting.RED), false);
                                                return 0;
                                            }

                                            if (!shop.isOpen() && !shop.getOwnerId().equals(p.getUuid())) {
                                                p.sendMessage(Text.literal("This shop is currently closed.").formatted(Formatting.RED), false);
                                                return 0;
                                            }

                                            net.fugginbeenus.notchcurrency.shop.NpcShopLogic.openShop(p, shopId);
                                            return 1;
                                        })
                                )
                        )

                        // /shop delete <id> - Delete your shop
                        .then(CommandManager.literal("delete")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            String raw = StringArgumentType.getString(ctx, "id");

                                            UUID shopId = findShopByIdOrName(p, raw);
                                            if (shopId == null) return 0;

                                            boolean success = net.fugginbeenus.notchcurrency.shop.PlayerShopManager.deleteShop(p, shopId);
                                            return success ? 1 : 0;
                                        })
                                )
                        )

                        // /shop linknpc <shopId> - Link current target NPC to shop (admin)
                        .then(CommandManager.literal("linknpc")
                                .requires(src -> src.hasPermissionLevel(2))
                                .then(CommandManager.argument("shopId", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            String raw = StringArgumentType.getString(ctx, "shopId");

                                            var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(p.getServerWorld());
                                            UUID shopId = findShopByIdOrNameAdmin(p, raw, state);
                                            if (shopId == null) return 0;

                                            // Get the entity the player is looking at
                                            var hit = p.raycast(5.0, 0.0f, false);
                                            if (hit.getType() == net.minecraft.util.hit.HitResult.Type.ENTITY) {
                                                var entityHit = (net.minecraft.util.hit.EntityHitResult) hit;
                                                UUID npcId = entityHit.getEntity().getUuid();

                                                state.linkNpcToShop(npcId, shopId);
                                                state.markDirtyAndSave();

                                                var shop = state.getShop(shopId);
                                                p.sendMessage(Text.literal("✓ Linked NPC to shop: " + (shop != null ? shop.getShopName() : shopId)).formatted(Formatting.GREEN), false);
                                                return 1;
                                            } else {
                                                p.sendMessage(Text.literal("Look at an NPC to link it!").formatted(Formatting.RED), false);
                                                return 0;
                                            }
                                        })
                                )
                        )

                        // /shop toggle <id> - Toggle shop open/closed
                        .then(CommandManager.literal("toggle")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            String raw = StringArgumentType.getString(ctx, "id");

                                            UUID shopId = findShopByIdOrName(p, raw);
                                            if (shopId == null) return 0;

                                            var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(p.getServerWorld());
                                            var shop = state.getShop(shopId);

                                            if (shop == null || !shop.getOwnerId().equals(p.getUuid())) {
                                                p.sendMessage(Text.literal("You don't own this shop!").formatted(Formatting.RED), false);
                                                return 0;
                                            }

                                            shop.setOpen(!shop.isOpen());
                                            state.markDirtyAndSave();

                                            String status = shop.isOpen() ? "§aOPEN" : "§cCLOSED";
                                            p.sendMessage(Text.literal("Shop is now: " + status), false);
                                            return 1;
                                        })
                                )
                        )


                        // ===== ADMIN COMMANDS =====

                        // /shop admin list - List ALL shops (admin only)
                        .then(CommandManager.literal("admin")
                                .requires(src -> src.hasPermissionLevel(2))

                                .then(CommandManager.literal("list")
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(p.getServerWorld());
                                            var shops = state.getAllShops();

                                            if (shops.isEmpty()) {
                                                p.sendMessage(Text.literal("No shops exist on this server.").formatted(Formatting.YELLOW), false);
                                                return 0;
                                            }

                                            p.sendMessage(Text.literal("=== All Shops (" + shops.size() + ") ===").formatted(Formatting.GOLD), false);
                                            for (var shop : shops) {
                                                String status = shop.isOpen() ? "§aOPEN" : "§cCLOSED";
                                                String npcStatus = shop.getLinkedNpcId() != null ? "§aLinked" : "§7No NPC";
                                                // Line 1: Shop name and status
                                                p.sendMessage(Text.literal(" • " + shop.getShopName())
                                                        .formatted(Formatting.WHITE)
                                                        .append(Text.literal(" [" + status + "§r] "))
                                                        .append(Text.literal("[" + npcStatus + "§r]")), false);
                                                // Line 2: Owner and ID (clickable to copy)
                                                String fullId = shop.getShopId().toString();
                                                p.sendMessage(Text.literal("   Owner: ").formatted(Formatting.GRAY)
                                                        .append(Text.literal(shop.getOwnerName()).formatted(Formatting.AQUA))
                                                        .append(Text.literal(" | ID: ").formatted(Formatting.GRAY))
                                                        .append(Text.literal(fullId).formatted(Formatting.DARK_GRAY)
                                                                .styled(style -> style
                                                                        .withClickEvent(new net.minecraft.text.ClickEvent(
                                                                                net.minecraft.text.ClickEvent.Action.COPY_TO_CLIPBOARD, fullId))
                                                                        .withHoverEvent(new net.minecraft.text.HoverEvent(
                                                                                net.minecraft.text.HoverEvent.Action.SHOW_TEXT,
                                                                                Text.literal("Click to copy ID"))))), false);
                                            }
                                            return 1;
                                        })
                                )

                                // /shop admin delete <id> - Force delete any shop (returns items to owner)
                                .then(CommandManager.literal("delete")
                                        .then(CommandManager.argument("id", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                                    String raw = StringArgumentType.getString(ctx, "id");

                                                    var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(p.getServerWorld());
                                                    UUID shopId = findShopByIdOrNameAdmin(p, raw, state);
                                                    if (shopId == null) return 0;

                                                    var shop = state.getShop(shopId);
                                                    if (shop == null) {
                                                        p.sendMessage(Text.literal("Shop not found!").formatted(Formatting.RED), false);
                                                        return 0;
                                                    }

                                                    // Return items to owner if online
                                                    ServerPlayerEntity owner = p.getServer().getPlayerManager().getPlayer(shop.getOwnerId());
                                                    net.fugginbeenus.notchcurrency.shop.PlayerShopManager.returnAllShopContents(p.getServer(), shop, owner);

                                                    // Remove NPC if linked
                                                    if (shop.getLinkedNpcId() != null) {
                                                        var npc = p.getServerWorld().getEntity(shop.getLinkedNpcId());
                                                        if (npc != null) npc.discard();
                                                    }

                                                    // Remove shop
                                                    state.removeShop(shopId);
                                                    state.markDirtyAndSave();

                                                    p.sendMessage(Text.literal("✓ Deleted shop: " + shop.getShopName() + " (owned by " + shop.getOwnerName() + ")")
                                                            .formatted(Formatting.GREEN), false);
                                                    return 1;
                                                })
                                        )
                                )

                                // /shop admin info <id> - Get detailed info about a shop
                                .then(CommandManager.literal("info")
                                        .then(CommandManager.argument("id", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                                    String raw = StringArgumentType.getString(ctx, "id");

                                                    var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(p.getServerWorld());
                                                    UUID shopId = findShopByIdOrNameAdmin(p, raw, state);
                                                    if (shopId == null) return 0;

                                                    var shop = state.getShop(shopId);
                                                    if (shop == null) {
                                                        p.sendMessage(Text.literal("Shop not found!").formatted(Formatting.RED), false);
                                                        return 0;
                                                    }

                                                    p.sendMessage(Text.literal("=== Shop Info ===").formatted(Formatting.GOLD), false);
                                                    p.sendMessage(Text.literal("Name: " + shop.getShopName()), false);
                                                    p.sendMessage(Text.literal("Owner: " + shop.getOwnerName() + " (" + shop.getOwnerId() + ")"), false);
                                                    p.sendMessage(Text.literal("Shop ID: " + shop.getShopId()), false);
                                                    p.sendMessage(Text.literal("Status: " + (shop.isOpen() ? "§aOPEN" : "§cCLOSED")), false);
                                                    p.sendMessage(Text.literal("NPC: " + (shop.getLinkedNpcId() != null ? shop.getLinkedNpcId().toString() : "None")), false);
                                                    p.sendMessage(Text.literal("Listings: " + shop.getListings().size()), false);
                                                    p.sendMessage(Text.literal("Pending Balance: " + shop.getPendingBalance() + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word()), false);
                                                    p.sendMessage(Text.literal("Total Revenue: " + shop.getTotalRevenue() + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word()), false);
                                                    p.sendMessage(Text.literal("Total Transactions: " + shop.getTotalTransactions()), false);
                                                    return 1;
                                                })
                                        )
                                )

                                // /shop admin transfer <id> <player> - Transfer shop ownership
                                .then(CommandManager.literal("transfer")
                                        .then(CommandManager.argument("id", StringArgumentType.word())
                                                .then(CommandManager.argument("newOwner", EntityArgumentType.player())
                                                        .executes(ctx -> {
                                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                                            String raw = StringArgumentType.getString(ctx, "id");
                                                            ServerPlayerEntity newOwner = EntityArgumentType.getPlayer(ctx, "newOwner");

                                                            var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(p.getServerWorld());
                                                            UUID shopId = findShopByIdOrNameAdmin(p, raw, state);
                                                            if (shopId == null) return 0;

                                                            boolean success = net.fugginbeenus.notchcurrency.shop.PlayerShopManager.transferOwnership(
                                                                    p.getServer(), shopId, newOwner.getUuid(), newOwner.getName().getString());

                                                            if (success) {
                                                                p.sendMessage(Text.literal("✓ Transferred shop to " + newOwner.getName().getString())
                                                                        .formatted(Formatting.GREEN), false);
                                                                newOwner.sendMessage(Text.literal("You are now the owner of a shop!")
                                                                        .formatted(Formatting.GREEN), false);
                                                            } else {
                                                                p.sendMessage(Text.literal("Failed to transfer shop!").formatted(Formatting.RED), false);
                                                            }
                                                            return success ? 1 : 0;
                                                        })
                                                )
                                        )
                                )

                                // /shop admin cleanup - Clean up orphaned NPC links (use with caution)
                                .then(CommandManager.literal("cleanup")
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(p.getServerWorld());

                                            p.sendMessage(Text.literal("⚠ Running orphan cleanup...").formatted(Formatting.YELLOW), false);
                                            p.sendMessage(Text.literal("Note: Only NPCs in loaded chunks will be detected!").formatted(Formatting.GRAY), false);

                                            int cleaned = state.cleanupOrphans(p.getServerWorld());

                                            if (cleaned > 0) {
                                                p.sendMessage(Text.literal("✓ Cleaned up " + cleaned + " orphaned NPC links.").formatted(Formatting.GREEN), false);
                                            } else {
                                                p.sendMessage(Text.literal("No orphaned links found.").formatted(Formatting.GREEN), false);
                                            }
                                            return 1;
                                        })
                                )
                        )
        );
    }

    private static UUID findShopByIdOrName(ServerPlayerEntity player, String input) {
        var state = net.fugginbeenus.notchcurrency.shop.ShopState.get(player.getServerWorld());

        // Try as UUID first
        try {
            UUID id = UUID.fromString(input);
            if (state.getShop(id) != null) {
                return id;
            }
        } catch (IllegalArgumentException ignored) {}

        // Try partial UUID match
        for (var shop : state.getAllShops()) {
            if (shop.getShopId().toString().startsWith(input)) {
                return shop.getShopId();
            }
        }

        // Try name match (owned shops first)
        var ownedShops = state.getShopsByOwner(player.getUuid());
        for (var shop : ownedShops) {
            if (shop.getShopName().equalsIgnoreCase(input)) {
                return shop.getShopId();
            }
        }

        // Try name match (all shops)
        for (var shop : state.getAllShops()) {
            if (shop.getShopName().equalsIgnoreCase(input)) {
                return shop.getShopId();
            }
        }

        player.sendMessage(Text.literal("Shop not found: " + input).formatted(Formatting.RED), false);
        return null;
    }

    private static UUID findShopByIdOrNameAdmin(ServerPlayerEntity player, String input, net.fugginbeenus.notchcurrency.shop.ShopState state) {
        // Try as UUID first
        try {
            UUID id = UUID.fromString(input);
            if (state.getShop(id) != null) {
                return id;
            }
        } catch (IllegalArgumentException ignored) {}

        // Try partial UUID match
        for (var shop : state.getAllShops()) {
            if (shop.getShopId().toString().startsWith(input)) {
                return shop.getShopId();
            }
        }

        // Try name match (all shops)
        for (var shop : state.getAllShops()) {
            if (shop.getShopName().toLowerCase().contains(input.toLowerCase())) {
                return shop.getShopId();
            }
        }

        player.sendMessage(Text.literal("Shop not found: " + input).formatted(Formatting.RED), false);
        return null;
    }
}
