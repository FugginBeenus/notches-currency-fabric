package net.fugginbeenus.notchcurrency.core;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fugginbeenus.notchcurrency.auction.AuctionCategories;
import net.fugginbeenus.notchcurrency.auction.AuctionHouseScreenHandler;
import net.fugginbeenus.notchcurrency.auction.AuctionListing;
import net.fugginbeenus.notchcurrency.auction.AuctionState;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.fugginbeenus.notchcurrency.crate.BalloonEntity;
import net.fugginbeenus.notchcurrency.crate.CrateDropManager;
import net.fugginbeenus.notchcurrency.crate.DailyCrateManager;
import net.fugginbeenus.notchcurrency.crate.GoldenCacheManager;
import net.fugginbeenus.notchcurrency.loot.BossCurrencyInject;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.registry.ModBlocks;
import net.fugginbeenus.notchcurrency.registry.ModCreativeTab;
import net.fugginbeenus.notchcurrency.registry.ModEntities;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.fugginbeenus.notchcurrency.trade.TradeManager;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fugginbeenus.notchcurrency.auction.AuctionConfig;
import net.fugginbeenus.notchcurrency.auction.AuctionState;

import java.util.UUID;

public class NotchCurrency implements ModInitializer {

    public static final String MOD_ID = "notchcurrency";

    public static Identifier id(String path) {
        return new Identifier(MOD_ID, path);
    }

    /** Gold coin glyph with a hover that shows the Notch Coin item. */
    public static Text coinIcon() {
        MutableText t = Text.literal("⛁");  // the symbol itself
        HoverEvent.ItemStackContent content =
                new HoverEvent.ItemStackContent(new ItemStack(ModItems.NOTCH_COIN));

        return t.styled(style -> style
                .withColor(Formatting.GOLD)
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, content)));
    }

    /** Convenience: "123 <coinIcon>" */
    public static Text coins(long amount) {
        return Text.literal(Long.toString(amount) + " ").append(coinIcon());
    }

    @Override
    public void onInitialize() {
        // Registries
        ModBlocks.register();
        ModItems.register();
        ModScreenHandlers.register();
        ModCreativeTab.register();
        TradeManager.init();
        ModEntities.register();

        // Register entity attributes
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                net.fugginbeenus.notchcurrency.registry.ModEntities.SHOPKEEPER,
                net.fugginbeenus.notchcurrency.entity.ShopkeeperEntity.createShopkeeperAttributes()
        );

        // Managers
        CrateDropManager.init();
        GoldenCacheManager.init();
        DailyCrateManager.init();
        BossCurrencyInject.init();

        // Shopkeeper system
        net.fugginbeenus.notchcurrency.shop.ShopkeeperInteractionHandler.register();
        net.fugginbeenus.notchcurrency.shop.ShopkeeperDeathHandler.register();
        net.fugginbeenus.notchcurrency.shop.ShopkeeperPacketHandler.register();
        net.fugginbeenus.notchcurrency.shop.ShopkeeperAIHandler.register();

        // Load config (applies defaults on missing)
        NotchConfig cfg = NotchConfigIO.load();
        DailyCrateManager.applyConfig(cfg);
        GoldenCacheManager.applyConfig(cfg);
        AuctionConfig.apply(cfg);

        // Auction expiration / cleanup & payouts each world tick + login reminders
        ServerTickEvents.END_WORLD_TICK.register((ServerWorld world) -> {
            AuctionState state = AuctionState.get(world);
            state.tick(world);
            state.checkLoginReminders(world);
        });

        // NOTE: Orphan cleanup is NOT run automatically on startup because entities
        // may not be loaded yet (chunks not loaded). Use /shop admin cleanup instead.

        // Commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            registerCommands(dispatcher);
        });

        // HUD balance sync on join + schedule auction mailbox reminder
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity sp = handler.player;
            NotchPackets.sendBalance(sp, BalanceStore.get(sp));

            ServerWorld world = sp.getServerWorld();
            AuctionState state = AuctionState.get(world);
            state.onPlayerJoin(sp);
        });

        // HUD balance sync on respawn
        ServerPlayerEvents.COPY_FROM.register((oldP, newP, alive) -> {
            ServerPlayerEntity sp = newP;
            NotchPackets.sendBalance(sp, BalanceStore.get(sp));
        });

        // Server handles client's explicit balance request
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.BALANCE_REQUEST,
                (server, player, handler, buf, response) ->
                        server.execute(() ->
                                NotchPackets.sendBalance(player, BalanceStore.get(player)))
        );

        // Server handles client's bid request (from right-click GUI)
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.BID_REQUEST,
                (server, player, handler, buf, response) -> {
                    UUID listingId = buf.readUuid();
                    long bidAmount = buf.readVarLong();

                    server.execute(() -> {
                        // player is already a ServerPlayerEntity here
                        ServerPlayerEntity sp = player;
                        ServerWorld world = sp.getServerWorld();
                        AuctionState state = AuctionState.get(world);
                        state.placeBid(world, sp, listingId, bidAmount);
                    });
                }
        );

        // Server handles ATM withdraw requests (client -> server)
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.ATM_WITHDRAW,
                (server, player, handler, buf, response) -> {
                    int requested = buf.readVarInt();

                    server.execute(() -> {
                        if (!(player instanceof ServerPlayerEntity)) return;
                        ServerPlayerEntity sp = (ServerPlayerEntity) player;
                        if (requested <= 0) return;

                        int currentBal = BalanceStore.get(sp);
                        int toWithdraw = Math.min(currentBal, requested);
                        if (toWithdraw <= 0) {
                            sp.sendMessage(
                                    Text.literal("You don't have that many Notch coins in your account.")
                                            .formatted(Formatting.RED),
                                    false
                            );
                            return;
                        }

                        // Subtract from virtual balance
                        BalanceStore.subtract(sp, toWithdraw);
                        int newBal = BalanceStore.get(sp);
                        NotchPackets.sendBalance(sp, newBal);

                        // If they currently have an ATM open, update its synced balance property
                        if (sp.currentScreenHandler instanceof net.fugginbeenus.notchcurrency.ui.ATMTestScreenHandler atmHandler) {
                            atmHandler.setSyncedBalance(newBal);
                        }

                        // Give physical coins (prefer physical stacks)
                        CoinEconomy.give(sp, toWithdraw, false);

                        sp.sendMessage(
                                Text.literal("Withdrew " + toWithdraw + " ")
                                        .append(NotchCurrency.coinIcon())
                                        .formatted(Formatting.GREEN),
                                false
                        );
                    });
                }
        );

        // Shop packet handlers
        registerShopPacketHandlers();
    }

    private void registerShopPacketHandlers() {
        // Handle shop purchase requests
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.SHOP_PURCHASE,
                (server, player, handler, buf, response) -> {
                    UUID shopId = buf.readUuid();
                    UUID listingId = buf.readUuid();
                    int quantity = buf.readVarInt();
                    // Note: useCoins boolean is still read for backwards compatibility but ignored
                    buf.readBoolean();

                    server.execute(() -> {
                        ServerPlayerEntity sp = player;
                        // Use unified purchase method that handles both coin AND barter
                        net.fugginbeenus.notchcurrency.shop.PlayerShopManager.PurchaseResult result =
                                net.fugginbeenus.notchcurrency.shop.PlayerShopManager.purchase(sp, shopId, listingId, quantity);

                        if (result != net.fugginbeenus.notchcurrency.shop.PlayerShopManager.PurchaseResult.SUCCESS) {
                            String errorMsg = switch (result) {
                                case SHOP_NOT_FOUND -> "Shop not found!";
                                case SHOP_CLOSED -> "This shop is currently closed.";
                                case OWN_SHOP -> "You can't buy from your own shop!";
                                case LISTING_NOT_FOUND -> "Item no longer available.";
                                case COINS_NOT_ACCEPTED -> "This item doesn't accept coin payment.";
                                case BARTER_NOT_ACCEPTED -> "This item doesn't accept barter.";
                                case INVALID_QUANTITY -> "Invalid quantity.";
                                case INSUFFICIENT_STOCK -> "Not enough stock available.";
                                case INSUFFICIENT_FUNDS -> "You don't have enough coins!";
                                case INSUFFICIENT_ITEMS -> "You don't have the required items!";
                                default -> "Purchase failed.";
                            };
                            sp.sendMessage(Text.literal(errorMsg).formatted(Formatting.RED), false);
                        }
                    });
                }
        );

        // Handle add listing requests
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.SHOP_ADD_LISTING,
                (server, player, handler, buf, response) -> {
                    UUID shopId = buf.readUuid();
                    ItemStack item = buf.readItemStack();
                    int price = buf.readVarInt();

                    server.execute(() -> {
                        ServerPlayerEntity sp = player;

                        if (item.isEmpty()) {
                            sp.sendMessage(Text.literal("§cNo item to list!"), false);
                            return;
                        }

                        int needed = item.getCount();
                        int found = 0;
                        ItemStack cursorStack = ItemStack.EMPTY;

                        // Check cursor stack first (player clicked with held item)
                        if (sp.currentScreenHandler != null) {
                            cursorStack = sp.currentScreenHandler.getCursorStack();
                            if (!cursorStack.isEmpty() && ItemStack.canCombine(cursorStack, item)) {
                                found += cursorStack.getCount();
                            }
                        }

                        // Then check inventory
                        PlayerInventory inv = sp.getInventory();
                        for (int i = 0; i < inv.size() && found < needed; i++) {
                            ItemStack stack = inv.getStack(i);
                            if (ItemStack.canCombine(stack, item)) {
                                found += stack.getCount();
                            }
                        }

                        if (found < needed) {
                            sp.sendMessage(Text.literal("§cNot enough items! Need " + needed + ", have " + found).formatted(Formatting.RED), false);
                            return;
                        }

                        // Remove items - cursor first, then inventory
                        int remaining = needed;

                        // Take from cursor
                        if (!cursorStack.isEmpty() && ItemStack.canCombine(cursorStack, item) && remaining > 0) {
                            int take = Math.min(remaining, cursorStack.getCount());
                            cursorStack.decrement(take);
                            if (cursorStack.isEmpty()) {
                                sp.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
                            }
                            remaining -= take;
                        }

                        // Take from inventory
                        for (int i = 0; i < inv.size() && remaining > 0; i++) {
                            ItemStack stack = inv.getStack(i);
                            if (ItemStack.canCombine(stack, item)) {
                                int take = Math.min(remaining, stack.getCount());
                                stack.decrement(take);
                                if (stack.isEmpty()) {
                                    inv.setStack(i, ItemStack.EMPTY);
                                }
                                remaining -= take;
                            }
                        }

                        // Sync inventory
                        sp.currentScreenHandler.sendContentUpdates();

                        // Add the listing
                        boolean success = net.fugginbeenus.notchcurrency.shop.PlayerShopManager.addListing(sp, shopId, item, price);
                        if (success) {
                            sp.sendMessage(Text.literal("§aListing added!"), false);
                            // Refresh display
                            if (sp.currentScreenHandler instanceof net.fugginbeenus.notchcurrency.shop.PlayerShopScreenHandler shopHandler) {
                                shopHandler.refreshDisplay();
                                shopHandler.sendContentUpdates();
                            }
                        } else {
                            // Give items back
                            ItemStack refund = item.copy();
                            if (!inv.insertStack(refund)) {
                                sp.dropItem(refund, false);
                            }
                            sp.currentScreenHandler.sendContentUpdates();
                        }
                    });
                }
        );

        // Handle remove listing requests
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.SHOP_REMOVE_LISTING,
                (server, player, handler, buf, response) -> {
                    UUID shopId = buf.readUuid();
                    UUID listingId = buf.readUuid();

                    server.execute(() -> {
                        ServerPlayerEntity sp = player;
                        boolean success = net.fugginbeenus.notchcurrency.shop.PlayerShopManager.removeListing(sp, shopId, listingId);

                        if (success && sp.currentScreenHandler instanceof net.fugginbeenus.notchcurrency.shop.PlayerShopScreenHandler shopHandler) {
                            shopHandler.refreshDisplay();
                            shopHandler.sendContentUpdates();
                        }
                    });
                }
        );

        // Handle price update requests
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.SHOP_UPDATE_PRICE,
                (server, player, handler, buf, response) -> {
                    UUID shopId = buf.readUuid();
                    UUID listingId = buf.readUuid();
                    int newPrice = buf.readVarInt();

                    server.execute(() -> {
                        ServerPlayerEntity sp = player;
                        net.fugginbeenus.notchcurrency.shop.PlayerShopManager.updatePrice(sp, shopId, listingId, newPrice);
                    });
                }
        );

        // Handle set barter item on existing listing
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.SHOP_SET_BARTER,
                (server, player, handler, buf, response) -> {
                    UUID shopId = buf.readUuid();
                    UUID listingId = buf.readUuid();
                    ItemStack barterItem = buf.readItemStack();
                    int barterCount = buf.readVarInt();

                    server.execute(() -> {
                        ServerPlayerEntity sp = player;
                        net.fugginbeenus.notchcurrency.shop.ShopState state =
                                net.fugginbeenus.notchcurrency.shop.ShopState.get(sp.getServerWorld());
                        net.fugginbeenus.notchcurrency.shop.PlayerShop shop = state.getShop(shopId);

                        if (shop == null || !shop.getOwnerId().equals(sp.getUuid())) {
                            sp.sendMessage(Text.literal("§cYou don't own this shop!"), false);
                            return;
                        }

                        // Find the listing
                        net.fugginbeenus.notchcurrency.shop.ShopListing listing = null;
                        for (net.fugginbeenus.notchcurrency.shop.ShopListing l : shop.getListings()) {
                            if (l.getId().equals(listingId)) {
                                listing = l;
                                break;
                            }
                        }

                        if (listing == null) {
                            sp.sendMessage(Text.literal("§cListing not found!"), false);
                            return;
                        }

                        // Set the barter price (no items consumed - just setting what's required)
                        if (barterItem.isEmpty() || barterCount <= 0) {
                            listing.setBarterPrice(ItemStack.EMPTY, 0);
                            sp.sendMessage(Text.literal("§eBarter requirement removed."), false);
                        } else {
                            listing.setBarterPrice(barterItem.copy(), barterCount);
                            sp.sendMessage(Text.literal("§aBarter set: " + barterCount + "x " + barterItem.getName().getString()), false);
                        }

                        state.markDirtyAndSave();

                        // Refresh display
                        if (sp.currentScreenHandler instanceof net.fugginbeenus.notchcurrency.shop.PlayerShopScreenHandler shopHandler) {
                            shopHandler.refreshDisplay();
                            shopHandler.sendContentUpdates();
                        }
                    });
                }
        );

        // Handle add stock to existing listing
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.SHOP_ADD_STOCK,
                (server, player, handler, buf, response) -> {
                    UUID shopId = buf.readUuid();
                    UUID listingId = buf.readUuid();
                    ItemStack items = buf.readItemStack();

                    server.execute(() -> {
                        ServerPlayerEntity sp = player;

                        if (items.isEmpty()) {
                            return;
                        }

                        int needed = items.getCount();
                        int found = 0;
                        ItemStack cursorStack = ItemStack.EMPTY;

                        // Check cursor stack first (player clicked with held item)
                        if (sp.currentScreenHandler != null) {
                            cursorStack = sp.currentScreenHandler.getCursorStack();
                            if (!cursorStack.isEmpty() && ItemStack.canCombine(cursorStack, items)) {
                                found += cursorStack.getCount();
                            }
                        }

                        // Then check inventory
                        PlayerInventory inv = sp.getInventory();
                        for (int i = 0; i < inv.size() && found < needed; i++) {
                            ItemStack stack = inv.getStack(i);
                            if (ItemStack.canCombine(stack, items)) {
                                found += stack.getCount();
                            }
                        }

                        if (found < needed) {
                            sp.sendMessage(Text.literal("§cNot enough items! Need " + needed + ", have " + found).formatted(Formatting.RED), false);
                            return;
                        }

                        // Remove items - cursor first, then inventory
                        int remaining = needed;

                        // Take from cursor
                        if (!cursorStack.isEmpty() && ItemStack.canCombine(cursorStack, items) && remaining > 0) {
                            int take = Math.min(remaining, cursorStack.getCount());
                            cursorStack.decrement(take);
                            if (cursorStack.isEmpty()) {
                                sp.currentScreenHandler.setCursorStack(ItemStack.EMPTY);
                            }
                            remaining -= take;
                        }

                        // Take from inventory
                        for (int i = 0; i < inv.size() && remaining > 0; i++) {
                            ItemStack stack = inv.getStack(i);
                            if (ItemStack.canCombine(stack, items)) {
                                int take = Math.min(remaining, stack.getCount());
                                stack.decrement(take);
                                if (stack.isEmpty()) {
                                    inv.setStack(i, ItemStack.EMPTY);
                                }
                                remaining -= take;
                            }
                        }

                        // Sync inventory
                        sp.currentScreenHandler.sendContentUpdates();

                        // Add stock to listing
                        boolean success = net.fugginbeenus.notchcurrency.shop.PlayerShopManager.addStock(
                                sp, shopId, listingId, items);

                        if (success) {
                            // Refresh display
                            if (sp.currentScreenHandler instanceof net.fugginbeenus.notchcurrency.shop.PlayerShopScreenHandler shopHandler) {
                                shopHandler.refreshDisplay();
                                shopHandler.sendContentUpdates();
                            }
                        } else {
                            // Give items back
                            ItemStack refund = items.copy();
                            if (!inv.insertStack(refund)) {
                                sp.dropItem(refund, false);
                            }
                            sp.currentScreenHandler.sendContentUpdates();
                        }
                    });
                }
        );

        // Handle save listings - validates changes and syncs with server shop data
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.SHOP_SAVE_LISTINGS,
                (server, player, handler, buf, response) -> {
                    UUID shopId = buf.readUuid();
                    int listingCount = buf.readVarInt();

                    // Read all data first
                    java.util.List<ListingData> listingsData = new java.util.ArrayList<>();
                    for (int i = 0; i < listingCount; i++) {
                        int row = buf.readVarInt();
                        int coinPrice = buf.readVarInt();
                        ItemStack barterItem = buf.readItemStack();
                        int barterCount = buf.readVarInt();
                        ItemStack saleItem = buf.readItemStack();
                        int stockCount = buf.readVarInt();
                        listingsData.add(new ListingData(row, coinPrice, barterItem, barterCount, saleItem, stockCount));
                    }

                    server.execute(() -> {
                        ServerPlayerEntity sp = player;
                        net.fugginbeenus.notchcurrency.shop.ShopState state =
                                net.fugginbeenus.notchcurrency.shop.ShopState.get(sp.getServerWorld());
                        net.fugginbeenus.notchcurrency.shop.PlayerShop shop = state.getShop(shopId);

                        if (shop == null || !shop.getOwnerId().equals(sp.getUuid())) {
                            return;
                        }

                        // Get current screen handler to validate slot contents
                        if (!(sp.currentScreenHandler instanceof net.fugginbeenus.notchcurrency.shop.PlayerShopScreenHandler shopHandler)) {
                            return;
                        }

                        java.util.List<net.fugginbeenus.notchcurrency.shop.ShopListing> existingListings = shop.getListings();

                        for (ListingData data : listingsData) {
                            // Validate the items actually exist in the slots
                            int barterSlotIndex = data.row * 3;
                            int saleSlotIndex = data.row * 3 + 1;
                            int stockSlotIndex = data.row * 3 + 2;

                            if (saleSlotIndex >= shopHandler.slots.size()) continue;

                            ItemStack actualBarterSlot = shopHandler.slots.get(barterSlotIndex).getStack();
                            ItemStack actualSaleSlot = shopHandler.slots.get(saleSlotIndex).getStack();
                            ItemStack actualStockSlot = shopHandler.slots.get(stockSlotIndex).getStack();

                            // Stock is ONLY from the stock slot - sale slot just shows item type
                            int actualStock = actualStockSlot.isEmpty() ? 0 : actualStockSlot.getCount();

                            if (data.row < existingListings.size()) {
                                // Update existing listing
                                net.fugginbeenus.notchcurrency.shop.ShopListing listing = existingListings.get(data.row);

                                // Price can be updated freely
                                listing.setCoinPrice(data.coinPrice);

                                // Update sale item quantity (how many items per purchase)
                                if (!actualSaleSlot.isEmpty()) {
                                    ItemStack newSaleItem = listing.getItemForSale().copy();
                                    newSaleItem.setCount(actualSaleSlot.getCount());
                                    listing.setItemForSale(newSaleItem);
                                }

                                // Barter can be updated freely (it's just a requirement, not real items)
                                if (!actualBarterSlot.isEmpty()) {
                                    listing.setBarterPrice(actualBarterSlot.copy(), actualBarterSlot.getCount());
                                } else {
                                    listing.setBarterPrice(ItemStack.EMPTY, 0);
                                }

                                // Stock must match actual slot contents
                                listing.setStock(actualStock);

                            } else if (!actualSaleSlot.isEmpty()) {
                                // New listing - validate items came from player inventory
                                // Since they put items in the slot, they had them
                                ItemStack saleTemplate = actualSaleSlot.copy();
                                // Keep the count - this is how many items per purchase

                                net.fugginbeenus.notchcurrency.shop.ShopListing newListing =
                                        new net.fugginbeenus.notchcurrency.shop.ShopListing(
                                                saleTemplate, actualStock, data.coinPrice);

                                if (!actualBarterSlot.isEmpty()) {
                                    newListing.setBarterPrice(actualBarterSlot.copy(), actualBarterSlot.getCount());
                                }
                                shop.addListing(newListing);
                            }
                        }

                        // Check for removed listings (listings that no longer have items in slots)
                        for (int row = 0; row < Math.min(existingListings.size(), 6); row++) {
                            int saleSlotIndex = row * 3 + 1;
                            if (saleSlotIndex < shopHandler.slots.size()) {
                                ItemStack saleSlot = shopHandler.slots.get(saleSlotIndex).getStack();
                                if (saleSlot.isEmpty()) {
                                    // Listing was cleared - return items handled by slot removal
                                    // Mark for removal (can't remove during iteration)
                                }
                            }
                        }

                        state.markDirtyAndSave();
                    });
                }
        );

        // Handle shop balance withdrawal
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.SHOP_WITHDRAW,
                (server, player, handler, buf, response) -> {
                    UUID shopId = buf.readUuid();

                    server.execute(() -> {
                        ServerPlayerEntity sp = player;
                        net.fugginbeenus.notchcurrency.shop.ShopState state =
                                net.fugginbeenus.notchcurrency.shop.ShopState.get(sp.getServerWorld());
                        net.fugginbeenus.notchcurrency.shop.PlayerShop shop = state.getShop(shopId);

                        if (shop == null) {
                            sp.sendMessage(Text.literal("Shop not found!").formatted(Formatting.RED), false);
                            return;
                        }

                        if (!shop.getOwnerId().equals(sp.getUuid())) {
                            sp.sendMessage(Text.literal("You don't own this shop!").formatted(Formatting.RED), false);
                            return;
                        }

                        long amount = shop.withdrawBalance();
                        java.util.List<ItemStack> barterItems = shop.collectPendingBarterItems();

                        boolean hadCoins = amount > 0;
                        boolean hadItems = !barterItems.isEmpty();

                        if (hadCoins) {
                            net.fugginbeenus.notchcurrency.api.CurrencyApi.deposit(sp, (int) amount);
                        }

                        // Give barter items
                        for (ItemStack item : barterItems) {
                            if (!item.isEmpty()) {
                                int remaining = item.getCount();
                                while (remaining > 0) {
                                    int giveCount = Math.min(remaining, item.getMaxCount());
                                    ItemStack toGive = item.copy();
                                    toGive.setCount(giveCount);
                                    if (!sp.getInventory().insertStack(toGive)) {
                                        sp.dropItem(toGive, false);
                                    }
                                    remaining -= giveCount;
                                }
                            }
                        }

                        if (hadCoins || hadItems) {
                            MutableText message = Text.literal("Withdrew ");
                            if (hadCoins) {
                                message.append(coins((int) amount));
                            }
                            if (hadCoins && hadItems) {
                                message.append(Text.literal(" and "));
                            }
                            if (hadItems) {
                                int totalItems = barterItems.stream().mapToInt(ItemStack::getCount).sum();
                                message.append(Text.literal(totalItems + " barter items").formatted(Formatting.AQUA));
                            }
                            message.append(Text.literal(" from your shop!").formatted(Formatting.GREEN));
                            sp.sendMessage(message, false);
                            state.markDirtyAndSave();
                        } else {
                            sp.sendMessage(Text.literal("No balance to withdraw.").formatted(Formatting.YELLOW), false);
                        }
                    });
                }
        );

        // Handle remove listing
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.SHOP_REMOVE_LISTING,
                (server, player, handler, buf, response) -> {
                    UUID shopId = buf.readUuid();
                    UUID listingId = buf.readUuid();

                    server.execute(() -> {
                        ServerPlayerEntity sp = player;
                        net.fugginbeenus.notchcurrency.shop.ShopState state =
                                net.fugginbeenus.notchcurrency.shop.ShopState.get(sp.getServerWorld());
                        net.fugginbeenus.notchcurrency.shop.PlayerShop shop = state.getShop(shopId);

                        if (shop == null || !shop.getOwnerId().equals(sp.getUuid())) {
                            return;
                        }

                        // Get the listing to return remaining stock to player
                        net.fugginbeenus.notchcurrency.shop.ShopListing listing = shop.getListing(listingId);
                        if (listing != null && listing.getStockQuantity() > 0) {
                            // Return stock to player
                            ItemStack returnItems = listing.getItemForSale().copy();
                            returnItems.setCount(listing.getStockQuantity());

                            // Give items back (split into stacks if needed)
                            int remaining = returnItems.getCount();
                            while (remaining > 0) {
                                int giveCount = Math.min(remaining, returnItems.getMaxCount());
                                ItemStack toGive = returnItems.copy();
                                toGive.setCount(giveCount);
                                if (!sp.getInventory().insertStack(toGive)) {
                                    sp.dropItem(toGive, false);
                                }
                                remaining -= giveCount;
                            }
                        }

                        shop.removeListing(listingId);
                        state.markDirtyAndSave();
                    });
                }
        );
    }

    // Helper class for listing data
    private record ListingData(int row, int coinPrice, ItemStack barterItem, int barterCount,
                               ItemStack saleItem, int stockCount) {}

    private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        // ===== /givnotches <amount> (admin only) =====
        dispatcher.register(
                CommandManager.literal("givnotches")
                        .requires(src -> src.hasPermissionLevel(2))
                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                .executes(ctx -> {
                                    ServerPlayerEntity player = ctx.getSource().getPlayer();
                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");
                                    player.giveItemStack(new ItemStack(ModItems.NOTCH_COIN, amount));
                                    player.sendMessage(Text.literal("Given " + amount + " Notch Coins!"), false);
                                    return 1;
                                }))
        );

        // ===== /trade =====
        dispatcher.register(
                CommandManager.literal("trade")
                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                .executes(ctx -> {
                                    ServerPlayerEntity from = ctx.getSource().getPlayer();
                                    ServerPlayerEntity to = EntityArgumentType.getPlayer(ctx, "player");
                                    TradeManager.invite(from, to);
                                    return 1;
                                })
                        )
                        .then(CommandManager.literal("accept")
                                .then(CommandManager.argument("inviter", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            String inviter = StringArgumentType.getString(ctx, "inviter");
                                            TradeManager.accept(p, inviter);
                                            return 1;
                                        })
                                )
                        )
                        .then(CommandManager.literal("decline")
                                .then(CommandManager.argument("inviter", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            String inviter = StringArgumentType.getString(ctx, "inviter");
                                            TradeManager.decline(p, inviter);
                                            return 1;
                                        })
                                )
                        )
        );

        // ===== /balloon (spawn + admin settings) =====
        dispatcher.register(
                CommandManager.literal("balloon")
                        .requires(src -> src.hasPermissionLevel(2))

                        // /balloon spawn [pos]
                        .then(CommandManager.literal("spawn")
                                // no args -> at player
                                .executes(ctx -> {
                                    var src = ctx.getSource();
                                    var world = src.getWorld();
                                    ServerPlayerEntity player = src.getPlayer();
                                    if (player == null) {
                                        src.sendError(Text.literal(
                                                "Run as a player or use: /balloon spawn <x> <y> <z>"));
                                        return 0;
                                    }
                                    BlockPos base = player.getBlockPos().up(12);
                                    BalloonEntity b = new BalloonEntity(world,
                                            base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
                                    world.spawnEntity(b);
                                    src.sendFeedback(() -> Text.literal(
                                            "Spawned test balloon at " + base.getX() + " " + base.getY() + " " + base.getZ()
                                    ), false);
                                    return 1;
                                })
                                // with explicit block pos
                                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                        .executes(ctx -> {
                                            var src = ctx.getSource();
                                            var world = src.getWorld();
                                            BlockPos pos = BlockPosArgumentType.getBlockPos(ctx, "pos");
                                            BalloonEntity b = new BalloonEntity(world,
                                                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                                            world.spawnEntity(b);
                                            src.sendFeedback(() -> Text.literal(
                                                    "Spawned test balloon at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                                            ), false);
                                            return 1;
                                        })
                                )
                        )

                        // /balloon setArea <x> <y> <z> <radius>
                        .then(CommandManager.literal("setArea")
                                .then(CommandManager.argument("x", IntegerArgumentType.integer())
                                        .then(CommandManager.argument("y", IntegerArgumentType.integer())
                                                .then(CommandManager.argument("z", IntegerArgumentType.integer())
                                                        .then(CommandManager.argument("radius", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> {
                                                                    var src = ctx.getSource();
                                                                    var world = src.getWorld();
                                                                    int x = IntegerArgumentType.getInteger(ctx, "x");
                                                                    int y = IntegerArgumentType.getInteger(ctx, "y");
                                                                    int z = IntegerArgumentType.getInteger(ctx, "z");
                                                                    int r = IntegerArgumentType.getInteger(ctx, "radius");
                                                                    DailyCrateManager.setArea((ServerWorld) world,
                                                                            new BlockPos(x, y, z), r);
                                                                    src.sendFeedback(() -> Text.literal(
                                                                            "Balloon area set to (" + x + "," + y + "," + z + ") r=" + r
                                                                    ), false);
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                        )
                                )
                        )

                        // /balloon setYRange <minY> <maxY>
                        .then(CommandManager.literal("setYRange")
                                .then(CommandManager.argument("minY", IntegerArgumentType.integer(5))
                                        .then(CommandManager.argument("maxY", IntegerArgumentType.integer(6))
                                                .executes(ctx -> {
                                                    var src = ctx.getSource();
                                                    var world = src.getWorld();
                                                    int minY = IntegerArgumentType.getInteger(ctx, "minY");
                                                    int maxY = IntegerArgumentType.getInteger(ctx, "maxY");
                                                    DailyCrateManager.setYRange((ServerWorld) world, minY, maxY);
                                                    src.sendFeedback(() -> Text.literal(
                                                            "Balloon Y range set to [" + minY + ".." + maxY + "]"
                                                    ), false);
                                                    return 1;
                                                })
                                        )
                                )
                        )

                        // /balloon setCount <perDay>
                        .then(CommandManager.literal("setCount")
                                .then(CommandManager.argument("perDay", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            var src = ctx.getSource();
                                            var world = src.getWorld();
                                            int per = IntegerArgumentType.getInteger(ctx, "perDay");
                                            DailyCrateManager.setCount((ServerWorld) world, per);
                                            src.sendFeedback(() -> Text.literal(
                                                    "Balloons per day set to " + per
                                            ), false);
                                            return 1;
                                        })
                                )
                        )

                        // /balloon announce on|off
                        .then(CommandManager.literal("announce")
                                .then(CommandManager.literal("on").executes(ctx -> {
                                    var src = ctx.getSource();
                                    DailyCrateManager.setAnnouncements((ServerWorld) src.getWorld(), true);
                                    src.sendFeedback(() -> Text.literal("Balloon announcements: ON"), false);
                                    return 1;
                                }))
                                .then(CommandManager.literal("off").executes(ctx -> {
                                    var src = ctx.getSource();
                                    DailyCrateManager.setAnnouncements((ServerWorld) src.getWorld(), false);
                                    src.sendFeedback(() -> Text.literal("Balloon announcements: OFF"), false);
                                    return 1;
                                }))
                        )
        );

        // ===== /cache =====
        dispatcher.register(
                CommandManager.literal("cache")
                        .requires(src -> src.hasPermissionLevel(2))
                        // /cache spawn [radius]
                        .then(CommandManager.literal("spawn")
                                .then(CommandManager.argument("radius", IntegerArgumentType.integer(8, 256))
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            int r = IntegerArgumentType.getInteger(ctx, "radius");
                                            var world = p.getServerWorld();
                                            var placed = GoldenCacheManager.spawnNear(world, p.getBlockPos(), r);
                                            if (placed != null) {
                                                ctx.getSource().sendFeedback(
                                                        () -> Text.literal("Spawned Golden Cache at " + placed.toShortString()),
                                                        false);
                                                return 1;
                                            } else {
                                                ctx.getSource().sendError(Text.literal("Failed to place cache."));
                                                return 0;
                                            }
                                        })
                                )
                                .executes(ctx -> {
                                    // default radius 64
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    var world = p.getServerWorld();
                                    var placed = GoldenCacheManager.spawnNear(world, p.getBlockPos(), 64);
                                    if (placed != null) {
                                        ctx.getSource().sendFeedback(
                                                () -> Text.literal("Spawned Golden Cache at " + placed.toShortString()), false);
                                        return 1;
                                    } else {
                                        ctx.getSource().sendError(Text.literal("Failed to place cache."));
                                        return 0;
                                    }
                                })
                        )
                        // /cache spawn_at <x> <y> <z>
                        .then(CommandManager.literal("spawn_at")
                                .then(CommandManager.argument("x", IntegerArgumentType.integer())
                                        .then(CommandManager.argument("y", IntegerArgumentType.integer())
                                                .then(CommandManager.argument("z", IntegerArgumentType.integer())
                                                        .executes(ctx -> {
                                                            var src = ctx.getSource();
                                                            var world = src.getWorld();
                                                            int x = IntegerArgumentType.getInteger(ctx, "x");
                                                            int y = IntegerArgumentType.getInteger(ctx, "y");
                                                            int z = IntegerArgumentType.getInteger(ctx, "z");
                                                            var placed = GoldenCacheManager.spawnAt(
                                                                    (ServerWorld) world, x, y, z);
                                                            if (placed != null) {
                                                                src.sendFeedback(
                                                                        () -> Text.literal(
                                                                                "Spawned Golden Cache at " + placed.toShortString()
                                                                        ), false);
                                                                return 1;
                                                            } else {
                                                                src.sendError(Text.literal("Failed to place cache."));
                                                                return 0;
                                                            }
                                                        })
                                                )
                                        )
                                )
                        )
                        // /cache announce <true|false>
                        .then(CommandManager.literal("announce")
                                .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                                            GoldenCacheManager.setAnnouncements(enabled);
                                            ctx.getSource().sendFeedback(
                                                    () -> Text.literal("Golden Cache announcements: " +
                                                            (enabled ? "ON" : "OFF")), false);
                                            return 1;
                                        })
                                )
                        )
        );

        // ===== /balance + /bal =====
        dispatcher.register(
                CommandManager.literal("balance")
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            int bal = BalanceStore.get(p);
                            p.sendMessage(
                                    Text.literal("Balance: " + bal + " ")
                                            .append(NotchCurrency.coinIcon()),
                                    true
                            );
                            NotchPackets.sendBalance(p, bal);
                            return 1;
                        })
        );
        dispatcher.register(
                CommandManager.literal("bal")
                        .executes(ctx -> ctx.getSource().getServer().getCommandManager()
                                .executeWithPrefix(ctx.getSource(), "balance"))
        );

        // ===== /pay <player> <amount> =====
        dispatcher.register(
                CommandManager.literal("pay")
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                        .executes(ctx -> {
                                            ServerPlayerEntity from = ctx.getSource().getPlayer();
                                            ServerPlayerEntity to = EntityArgumentType.getPlayer(ctx, "target");
                                            int amt = IntegerArgumentType.getInteger(ctx, "amount");

                                            if (from == to) {
                                                from.sendMessage(
                                                        Text.literal("You can’t pay yourself.")
                                                                .formatted(Formatting.RED),
                                                        false);
                                                return 0;
                                            }

                                            int bal = BalanceStore.get(from);
                                            if (bal < amt) {
                                                from.sendMessage(
                                                        Text.literal("Insufficient funds.")
                                                                .formatted(Formatting.RED),
                                                        false);
                                                return 0;
                                            }

                                            BalanceStore.subtract(from, amt);
                                            BalanceStore.add(to, amt);

                                            NotchPackets.sendBalance(from, BalanceStore.get(from));
                                            NotchPackets.sendBalance(to, BalanceStore.get(to));

                                            from.sendMessage(
                                                    Text.literal("Paid " + amt + " ")
                                                            .append(NotchCurrency.coinIcon())
                                                            .append(Text.literal(" to " + to.getName().getString()))
                                                            .formatted(Formatting.GREEN),
                                                    false
                                            );

                                            to.sendMessage(
                                                    Text.literal(from.getName().getString() + " paid you " + amt + " ")
                                                            .append(NotchCurrency.coinIcon())
                                                            .formatted(Formatting.GREEN),
                                                    false
                                            );
                                            return 1;
                                        })
                                )
                        )
        );

        // ===== /ah (Auction House) =====
        dispatcher.register(
                CommandManager.literal("ah")

                        // /ah -> open GUI
                        .executes(ctx -> {
                            ServerPlayerEntity player = ctx.getSource().getPlayer();
                            player.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                                    (syncId, inv, p) -> new AuctionHouseScreenHandler(syncId, inv),
                                    Text.literal("Auction House")
                            ));
                            return 1;
                        })

                        // /ah browse  -> simple text listing
                        .then(CommandManager.literal("browse")
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    var world = ctx.getSource().getWorld();
                                    AuctionState state = AuctionState.get((ServerWorld) world);

                                    p.sendMessage(Text.literal("=== Auction Listings ==="), false);
                                    if (state.getListings().isEmpty()) {
                                        p.sendMessage(Text.literal("No active listings."), false);
                                        return 1;
                                    }

                                    for (AuctionListing l : state.getListings()) {
                                        Text line = Text.literal(l.id.toString())
                                                .append(Text.literal(" | " + l.stack.getCount() + "× " + l.stack.getName().getString() + " | " + l.price + " "))
                                                .append(NotchCurrency.coinIcon())
                                                .append(Text.literal(" | Seller: " + l.sellerName));

                                        p.sendMessage(line, false);
                                    }
                                    p.sendMessage(Text.literal("Use /ah buy <id> to purchase or /ah bid <id> <amount> to bid."), false);

                                    return 1;
                                })
                        )

                        // /ah list <price> [days]
                        .then(CommandManager.literal("list")
                                .then(CommandManager.argument("price", IntegerArgumentType.integer(1))
                                        // /ah list <price>  -> regular, NO time limit (instant buy)
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            ServerWorld world = (ServerWorld) ctx.getSource().getWorld();
                                            AuctionState state = AuctionState.get(world);

                                            int price = IntegerArgumentType.getInteger(ctx, "price");
                                            // days = 0 => no timer
                                            return handleListCommand(p, world, state, price, 0, false);
                                        })
                                        // /ah list <price> <days> -> TIMED AUCTION
                                        .then(CommandManager.argument("days", IntegerArgumentType.integer(1, 7))
                                                .executes(ctx -> {
                                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                                    ServerWorld world = (ServerWorld) ctx.getSource().getWorld();
                                                    AuctionState state = AuctionState.get(world);

                                                    int price = IntegerArgumentType.getInteger(ctx, "price");
                                                    int days  = IntegerArgumentType.getInteger(ctx, "days");
                                                    return handleListCommand(p, world, state, price, days, true);
                                                })
                                        )
                                )
                        )

                        // /ah buy <id>
                        .then(CommandManager.literal("buy")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            var world = ctx.getSource().getWorld();
                                            AuctionState state = AuctionState.get((ServerWorld) world);

                                            String raw = StringArgumentType.getString(ctx, "id");
                                            UUID id;
                                            try {
                                                id = UUID.fromString(raw);
                                            } catch (IllegalArgumentException e) {
                                                p.sendMessage(Text.literal(
                                                        "Invalid listing id (must be a UUID)."
                                                ), false);
                                                return 0;
                                            }

                                            state.buyListing(p, id);
                                            return 1;
                                        })
                                )
                        )

                        // /ah claim <id> – winner-only, pulls from pending winnings
                        .then(CommandManager.literal("claim")
                                // /ah claim
                                .executes(ctx -> {
                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                    ServerWorld world = (ServerWorld) ctx.getSource().getWorld();
                                    AuctionState state = AuctionState.get(world);
                                    state.claimAll(world, p);
                                    return 1;
                                })
                                // /ah claim <id>
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            ServerWorld world = (ServerWorld) ctx.getSource().getWorld();
                                            AuctionState state = AuctionState.get(world);

                                            String raw = StringArgumentType.getString(ctx, "id");
                                            UUID id;
                                            try {
                                                id = UUID.fromString(raw);
                                            } catch (IllegalArgumentException e) {
                                                p.sendMessage(
                                                        Text.literal("Invalid listing id (must be a UUID).")
                                                                .formatted(Formatting.RED),
                                                        false
                                                );
                                                return 0;
                                            }

                                            AuctionState.PendingWinnings pw = state.getPending(id);
                                            if (pw == null) {
                                                p.sendMessage(
                                                        Text.literal("No pending winnings for that id.")
                                                                .formatted(Formatting.RED),
                                                        false
                                                );
                                                return 0;
                                            }

                                            boolean claimedSomething = false;

                                            // Claim coins if you are the seller
                                            if (p.getUuid().equals(pw.sellerUuid) && pw.finalPrice > 0L) {
                                                int amt = (int) Math.min(Integer.MAX_VALUE, pw.finalPrice);
                                                BalanceStore.add(p, amt);
                                                NotchPackets.sendBalance(p, BalanceStore.get(p));

                                                p.sendMessage(
                                                        Text.literal("Claimed ")
                                                                .append(Text.literal(String.valueOf(amt) + " ").formatted(Formatting.GOLD))
                                                                .append(NotchCurrency.coinIcon())
                                                                .append(Text.literal(" from auction winnings.").formatted(Formatting.GREEN)),
                                                        false
                                                );
                                                pw.finalPrice = 0L;
                                                claimedSomething = true;
                                            }

                                            // Claim item if you are the winner (or owner of returned item)
                                            if (p.getUuid().equals(pw.winnerUuid) && !pw.stack.isEmpty()) {
                                                ItemStack toGive = pw.stack.copy();
                                                boolean inserted = p.getInventory().insertStack(toGive);
                                                if (!inserted && !toGive.isEmpty()) {
                                                    // Explicit claim: worst case drop near them
                                                    p.dropItem(toGive, false);
                                                }

                                                p.sendMessage(
                                                        Text.literal("Claimed item: ")
                                                                .append(pw.stack.getName().copy())
                                                                .formatted(Formatting.GREEN),
                                                        false
                                                );
                                                pw.stack = ItemStack.EMPTY;
                                                claimedSomething = true;
                                            }

                                            // If you don't match either role
                                            if (!claimedSomething) {
                                                p.sendMessage(
                                                        Text.literal("You have nothing to claim for that listing.")
                                                                .formatted(Formatting.RED),
                                                        false
                                                );
                                                return 0;
                                            }

                                            if (pw.isFullyClaimed()) {
                                                state.removePending(id);
                                            } else {
                                                state.addPending(pw); // markDirty is inside
                                            }

                                            return 1;
                                        })
                                )
                        )

                        // /ah bid <id> <amount>
                        .then(CommandManager.literal("bid")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                                .executes(ctx -> {
                                                    ServerPlayerEntity p = ctx.getSource().getPlayer();
                                                    ServerWorld world = (ServerWorld) ctx.getSource().getWorld();
                                                    AuctionState state = AuctionState.get(world);

                                                    String raw = StringArgumentType.getString(ctx, "id");
                                                    int amount = IntegerArgumentType.getInteger(ctx, "amount");

                                                    UUID id;
                                                    try {
                                                        id = UUID.fromString(raw);
                                                    } catch (IllegalArgumentException e) {
                                                        p.sendMessage(Text.literal(
                                                                "Invalid listing id (must be a UUID)."
                                                        ), false);
                                                        return 0;
                                                    }

                                                    state.placeBid(world, p, id, amount);
                                                    return 1;
                                                })
                                        )
                                )
                        )

                        // /ah cancel <id> (seller only; returns item)
                        .then(CommandManager.literal("cancel")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            var world = ctx.getSource().getWorld();
                                            AuctionState state = AuctionState.get((ServerWorld) world);

                                            String raw = StringArgumentType.getString(ctx, "id");
                                            UUID id;
                                            try {
                                                id = UUID.fromString(raw);
                                            } catch (IllegalArgumentException e) {
                                                p.sendMessage(Text.literal(
                                                        "Invalid listing id (must be a UUID)."
                                                ), false);
                                                return 0;
                                            }

                                            AuctionListing l = state.getListing(id);
                                            if (l == null) {
                                                p.sendMessage(Text.literal(
                                                        "No listing with that id."
                                                ), false);
                                                return 0;
                                            }
                                            if (!p.getUuid().equals(l.sellerUuid)) {
                                                p.sendMessage(Text.literal(
                                                        "Only the seller can cancel this listing."
                                                ), false);
                                                return 0;
                                            }

                                            state.removeListing(id);

                                            ItemStack toReturn = l.stack.copy();
                                            boolean inserted = p.getInventory().insertStack(toReturn);
                                            if (!inserted && !toReturn.isEmpty()) {
                                                p.dropItem(toReturn, false);
                                            }

                                            p.sendMessage(Text.literal(
                                                    "Cancelled listing " + id + "."
                                            ), false);
                                            return 1;
                                        })
                                )
                        )
        );

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

                        // /shop claim <id> - Claim items from a destroyed shopkeeper
                        .then(CommandManager.literal("claim")
                                .then(CommandManager.argument("id", StringArgumentType.word())
                                        .executes(ctx -> {
                                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                                            String raw = StringArgumentType.getString(ctx, "id");

                                            UUID shopId;
                                            try {
                                                shopId = UUID.fromString(raw);
                                            } catch (IllegalArgumentException e) {
                                                p.sendMessage(Text.literal("Invalid shop ID!").formatted(Formatting.RED), false);
                                                return 0;
                                            }

                                            boolean success = net.fugginbeenus.notchcurrency.shop.ShopkeeperDeathHandler.claimShopItems(p, shopId);
                                            return success ? 1 : 0;
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
                                                    p.sendMessage(Text.literal("Pending Balance: " + shop.getPendingBalance() + " coins"), false);
                                                    p.sendMessage(Text.literal("Total Revenue: " + shop.getTotalRevenue() + " coins"), false);
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

    /**
     * Helper to find a shop by UUID or partial match on shop name for the given player.
     */
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

    /**
     * Helper to find a shop by UUID or partial match on shop name (admin version - searches all shops).
     */
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

    /**
     * Shared helper for /ah list <price> [days].
     *
     * @param price  starting / fixed price
     * @param days   length of auction in days (ignored for untimed)
     * @param timed  true = timed auction, false = no time limit (instant buy)
     */
    private int handleListCommand(ServerPlayerEntity p,
                                  ServerWorld world,
                                  AuctionState state,
                                  int price,
                                  int days,
                                  boolean timed) {

        ItemStack hand = p.getMainHandStack();

        if (hand.isEmpty()) {
            p.sendMessage(Text.literal(
                    "Hold the item you want to list in your main hand."
            ), false);
            return 0;
        }

        // --- Auction listing fee (optional, from config) ---
        int fee = AuctionConfig.LISTING_FEE_FLAT;
        if (fee > 0) {
            int bal = BalanceStore.get(p);
            if (bal < fee) {
                p.sendMessage(
                        Text.literal("You need ")
                                .append(Text.literal(String.valueOf(fee) + " ").formatted(Formatting.GOLD))
                                .append(NotchCurrency.coinIcon())
                                .append(Text.literal(" to pay the auction listing fee.")
                                        .formatted(Formatting.RED)),
                        false
                );
                return 0;
            }

            BalanceStore.subtract(p, fee);
            NotchPackets.sendBalance(p, BalanceStore.get(p));

            p.sendMessage(
                    Text.literal("Paid ")
                            .append(Text.literal(String.valueOf(fee) + " ").formatted(Formatting.GOLD))
                            .append(NotchCurrency.coinIcon())
                            .append(Text.literal(" as auction listing fee.")
                                    .formatted(Formatting.GRAY)),
                    false
            );
        }
        // --- end listing fee ---

        // Copy stack, remove from player
        ItemStack listingStack = hand.copy();
        hand.decrement(listingStack.getCount());

        long durationTicks = 0L;
        int clampedDays = 0;

        if (timed) {
            clampedDays = Math.max(1, Math.min(7, days));

            // real-time days: 24h * 60m * 60s * 20 ticks/s
            durationTicks = clampedDays * 24L * 60L * 60L * 20L;
        }

        String category = AuctionCategories.classify(listingStack);

        AuctionListing listing = state.addListing(
                world, p, listingStack, price, category, durationTicks);

        // Common part of the message
        MutableText listedMsg = Text.literal("Listed ")
                .append(Text.literal("x" + listingStack.getCount() + " ")
                        .formatted(Formatting.GREEN))
                .append(listingStack.getName().copy().formatted(Formatting.GREEN))
                .append(Text.literal(" for " + price + " ")
                        .formatted(Formatting.GOLD))
                .append(NotchCurrency.coinIcon());

        if (timed) {
            listedMsg.append(
                    Text.literal(" (" + clampedDays + " day" + (clampedDays > 1 ? "s" : "") + " auction)")
                            .formatted(Formatting.GRAY)
            );
        } else {
            listedMsg.append(
                    Text.literal(" (no time limit)")
                            .formatted(Formatting.GRAY)
            );
        }

        p.sendMessage(listedMsg, false);

        return 1;
    }

}