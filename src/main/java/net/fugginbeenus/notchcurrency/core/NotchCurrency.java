package net.fugginbeenus.notchcurrency.core;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fugginbeenus.notchcurrency.auction.AuctionConfig;
import net.fugginbeenus.notchcurrency.auction.AuctionState;
import net.fugginbeenus.notchcurrency.command.AdminShopCommands;
import net.fugginbeenus.notchcurrency.command.AuctionCommands;
import net.fugginbeenus.notchcurrency.command.CrateCommands;
import net.fugginbeenus.notchcurrency.command.CurrencyCommands;
import net.fugginbeenus.notchcurrency.command.EcoCommands;
import net.fugginbeenus.notchcurrency.command.BountyCommands;
import net.fugginbeenus.notchcurrency.command.CrateKeyCommands;
import net.fugginbeenus.notchcurrency.command.LoanCommands;
import net.fugginbeenus.notchcurrency.command.NpcCommands;
import net.fugginbeenus.notchcurrency.command.RaffleCommands;
import net.fugginbeenus.notchcurrency.command.ShopCommands;
import net.fugginbeenus.notchcurrency.command.TradeCommands;
import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.economy.EconomyLedger;
import net.fugginbeenus.notchcurrency.economy.ShopRent;
import net.fugginbeenus.notchcurrency.economy.WealthTax;
import net.fugginbeenus.notchcurrency.economy.adminshop.AdminShopState;
import net.fugginbeenus.notchcurrency.economy.bounty.BountyManager;
import net.fugginbeenus.notchcurrency.economy.raffle.RaffleManager;
import net.fugginbeenus.notchcurrency.economy.npc.NpcRoleInteractionHandler;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.fugginbeenus.notchcurrency.crate.CrateDropManager;
import net.fugginbeenus.notchcurrency.crate.DailyCrateManager;
import net.fugginbeenus.notchcurrency.crate.GoldenCacheManager;
import net.fugginbeenus.notchcurrency.loot.BossCurrencyInject;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.net.ServerPacketHandlers;
import net.fugginbeenus.notchcurrency.registry.ModBlocks;
import net.fugginbeenus.notchcurrency.registry.ModCreativeTab;
import net.fugginbeenus.notchcurrency.registry.ModEntities;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.fugginbeenus.notchcurrency.trade.TradeManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public class NotchCurrency implements ModInitializer {

    public static final String MOD_ID = "notchcurrency";

    public static ResourceLocation id(String path) {
        return net.fugginbeenus.notchcurrency.compat.Reg.id(path);
    }

    public static Component coinIcon() {
        MutableComponent t = Component.literal("\uE000");  // Character mapped in minecraft:default font
        HoverEvent.ItemStackInfo content =
                new HoverEvent.ItemStackInfo(new ItemStack(ModItems.NOTCH_COIN));

        // Force white so the coin glyph renders at full brightness (untinted) no matter what colour
        // the surrounding price text is drawn in, otherwise dark price text darkens the coin.
        return t.withStyle(style -> style
                .withColor(net.minecraft.network.chat.TextColor.fromRgb(0xFFFFFF))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, content)));
    }

    public static Component coins(long amount) {
        return Component.literal(Long.toString(amount) + " ").append(coinIcon());
    }

    @Override
    public void onInitialize() {
        // Packet channels must be declared before ANYTHING registers a receiver or sends.
        // TradeManager.init() below registers receivers, so this has to be the first thing that runs.
        net.fugginbeenus.notchcurrency.compat.Net.declareChannels();

        // GeckoLib (animation framework for the Notch NPC entity): must init before entities.
        // 4.8 (the 1.21 build) initializes itself; the manual call only exists on 4.4.
        //? if <1.21
        software.bernie.geckolib.GeckoLib.initialize();

        // Registries
        ModBlocks.register();
        net.fugginbeenus.notchcurrency.registry.ModBlockEntities.register();
        ModItems.register();
        ModScreenHandlers.register();
        ModCreativeTab.register();
        TradeManager.init();
        ModEntities.register();

        // Register entity attributes
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                net.fugginbeenus.notchcurrency.registry.ModEntities.NOTCH_NPC,
                net.fugginbeenus.notchcurrency.entity.NotchNpcEntity.createAttributes()
        );

        // Managers
        CrateDropManager.init();
        GoldenCacheManager.init();
        DailyCrateManager.init();
        BossCurrencyInject.init();
        WealthTax.init();
        ShopRent.init();
        RaffleManager.init();
        BountyManager.init();
        net.fugginbeenus.notchcurrency.economy.loan.LoanManager.init();
        net.fugginbeenus.notchcurrency.economy.gambling.GamblingManager.init();

        // (The legacy ShopkeeperEntity system was retired: the Notch NPC SHOP role replaced it.)

        // Economy NPC roles (admin shop / banker / auctioneer / mailbox on any NPC)
        NpcRoleInteractionHandler.register();

        // Load config (applies defaults on missing)
        NotchConfig cfg = NotchConfigIO.load();
        DailyCrateManager.applyConfig(cfg);
        GoldenCacheManager.applyConfig(cfg);
        AuctionConfig.apply(cfg);
        WealthTax.applyConfig(cfg);
        ShopRent.applyConfig(cfg);
        RaffleManager.applyConfig(cfg);
        BountyManager.applyConfig(cfg);
        net.fugginbeenus.notchcurrency.economy.crate.CrateManager.applyConfig(cfg);
        net.fugginbeenus.notchcurrency.economy.loan.LoanManager.applyConfig(cfg);
        net.fugginbeenus.notchcurrency.economy.gambling.GamblingManager.applyConfig(cfg);
        net.fugginbeenus.notchcurrency.economy.enchanter.EnchanterManager.applyConfig(cfg);
        net.fugginbeenus.notchcurrency.economy.cosmetic.CosmeticManager.applyConfig(cfg);
        net.fugginbeenus.notchcurrency.integration.WaystoneFeeHandler.applyConfig(cfg);
        net.fugginbeenus.notchcurrency.economy.villager.VillagerCoinTrades.applyConfig(cfg);

        // Waystone fee, soft integration; only hook the event when the Waystones mod is present.
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("waystones")) {
            net.fugginbeenus.notchcurrency.integration.WaystoneFeeHandler.register();
        }

        // Bounty pools & crate definitions load from datapacks (mod ships defaults).
        net.fabricmc.fabric.api.resource.ResourceManagerHelper
                .get(net.minecraft.server.packs.PackType.SERVER_DATA)
                .registerReloadListener(new net.fugginbeenus.notchcurrency.economy.bounty.BountyPoolLoader());
        net.fabricmc.fabric.api.resource.ResourceManagerHelper
                .get(net.minecraft.server.packs.PackType.SERVER_DATA)
                .registerReloadListener(new net.fugginbeenus.notchcurrency.economy.crate.CrateLoader());
        net.fabricmc.fabric.api.resource.ResourceManagerHelper
                .get(net.minecraft.server.packs.PackType.SERVER_DATA)
                .registerReloadListener(new net.fugginbeenus.notchcurrency.economy.cosmetic.CosmeticLoader());

        // Auction expiration / cleanup & payouts + login reminders.
        // Auctions are global (overworld-stored), so tick the single state once per
        // server tick rather than once per dimension.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerLevel overworld = server.overworld();
            if (overworld == null) return;
            AuctionState state = AuctionState.get(overworld);
            state.tick(overworld);
            state.checkLoginReminders(overworld);

            // Recover admin-shop dynamic prices toward baseline.
            AdminShopState.get(server).tickDecay();

            // NPC dialogue hand-offs: delayed greeting→GUI opens + goodbye lines on screen close.
            net.fugginbeenus.notchcurrency.npc.dialogue.NpcDialogueManager.tick(server);
        });

        // NOTE: Orphan cleanup is NOT run automatically on startup because entities
        // may not be loaded yet (chunks not loaded). Use /shop admin cleanup instead.

        // Commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            CurrencyCommands.register(dispatcher);
            TradeCommands.register(dispatcher);
            CrateCommands.register(dispatcher);
            AuctionCommands.register(dispatcher);
            ShopCommands.register(dispatcher);
            EcoCommands.register(dispatcher);
            AdminShopCommands.register(dispatcher);
            NpcCommands.register(dispatcher);
            net.fugginbeenus.notchcurrency.command.FactionCommands.register(dispatcher);
            RaffleCommands.register(dispatcher);
            BountyCommands.register(dispatcher);
            CrateKeyCommands.register(dispatcher);
            LoanCommands.register(dispatcher);
            net.fugginbeenus.notchcurrency.command.GamblingCommands.register(dispatcher);
        });

        // HUD balance sync on join + schedule auction mailbox reminder
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer sp = handler.player;
            NotchPackets.sendBalance(sp, BalanceStore.get(sp));

            ServerLevel world = sp.serverLevel();
            AuctionState state = AuctionState.get(world);
            state.onPlayerJoin(sp);

            // Resolve raffle ticket statuses & remind the player of any unclaimed prize.
            RaffleManager.remindOnJoin(sp);

            // Hand over any items owed from trade offers resolved while they were offline.
            net.fugginbeenus.notchcurrency.trade.TradeOfferManager.deliverMail(sp);

            // Push the server's custom coin skin (art + name) so every player sees it.
            net.fugginbeenus.notchcurrency.currency.CurrencyServerSync.send(sp);

            // Seed the on-screen bounty tracker with their taken bounties.
            net.fugginbeenus.notchcurrency.economy.bounty.BountyManager.syncTracker(sp);
        });

        // HUD balance sync on respawn
        ServerPlayerEvents.COPY_FROM.register((oldP, newP, alive) -> {
            ServerPlayer sp = newP;
            NotchPackets.sendBalance(sp, BalanceStore.get(sp));
        });

        // Server-bound packet receivers (balance request, bids, ATM withdraw, shop ops)
        ServerPacketHandlers.register();

        // StackData/Ench need a registry lookup to (de)serialize on 1.21+.
        ServerLifecycleEvents.SERVER_STARTED.register(
                server -> net.fugginbeenus.notchcurrency.compat.RegistryAccess.setServer(server.registryAccess()));
        ServerLifecycleEvents.SERVER_STOPPED.register(
                server -> net.fugginbeenus.notchcurrency.compat.RegistryAccess.setServer(null));

        // A block gets first refusal on a right-click, before the held item is consulted. That is
        // fine everywhere except when marking a schedule spot: clicking a bed would try to put the
        // player to sleep and the tool would never see the click at all. Intercepting here is the
        // only way the tool gets told about the one block it most needs to be pointed at.
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player, world, hand, hit) -> {
            if (world.isClientSide() || !(player instanceof net.minecraft.server.level.ServerPlayer sp)) {
                return net.minecraft.world.InteractionResult.PASS;
            }
            net.minecraft.world.item.ItemStack held = player.getItemInHand(hand);
            if (!(held.getItem() instanceof net.fugginbeenus.notchcurrency.item.RoutePlannerItem)
                    || !net.fugginbeenus.notchcurrency.compat.StackData.has(
                            held, net.fugginbeenus.notchcurrency.item.RoutePlannerItem.ENTRY_KEY)) {
                return net.minecraft.world.InteractionResult.PASS;
            }
            net.fugginbeenus.notchcurrency.item.RoutePlannerItem.markScheduleSpot(sp, held, hit.getBlockPos(), hit.getDirection());
            return net.minecraft.world.InteractionResult.SUCCESS;
        });

        // Flush & close the economy audit log when the server stops.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> EconomyLedger.close());
    }


}