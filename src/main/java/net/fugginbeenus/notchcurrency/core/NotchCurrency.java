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
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class NotchCurrency implements ModInitializer {

    public static final String MOD_ID = "notchcurrency";

    public static Identifier id(String path) {
        return net.fugginbeenus.notchcurrency.compat.Reg.id(path);
    }

    /** Gold coin glyph with a hover that shows the Notch Coin item. */
    public static Text coinIcon() {
        MutableText t = Text.literal("\uE000");  // Character mapped in minecraft:default font
        HoverEvent.ItemStackContent content =
                new HoverEvent.ItemStackContent(new ItemStack(ModItems.NOTCH_COIN));

        // Force white so the coin glyph renders at full brightness (untinted) no matter what colour
        // the surrounding price text is drawn in — otherwise dark price text darkens the coin.
        return t.styled(style -> style
                .withColor(net.minecraft.text.TextColor.fromRgb(0xFFFFFF))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_ITEM, content)));
    }

    /** Convenience: "123 <coinIcon>" */
    public static Text coins(long amount) {
        return Text.literal(Long.toString(amount) + " ").append(coinIcon());
    }

    @Override
    public void onInitialize() {
        // GeckoLib (animation framework for the Notch NPC entity) — must init before entities.
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

        // (The legacy ShopkeeperEntity system was retired — the Notch NPC SHOP role replaced it.)

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

        // Waystone fee — soft integration; only hook the event when the Waystones mod is present.
        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("waystones")) {
            net.fugginbeenus.notchcurrency.integration.WaystoneFeeHandler.register();
        }

        // Bounty pools & crate definitions load from datapacks (mod ships defaults).
        net.fabricmc.fabric.api.resource.ResourceManagerHelper
                .get(net.minecraft.resource.ResourceType.SERVER_DATA)
                .registerReloadListener(new net.fugginbeenus.notchcurrency.economy.bounty.BountyPoolLoader());
        net.fabricmc.fabric.api.resource.ResourceManagerHelper
                .get(net.minecraft.resource.ResourceType.SERVER_DATA)
                .registerReloadListener(new net.fugginbeenus.notchcurrency.economy.crate.CrateLoader());
        net.fabricmc.fabric.api.resource.ResourceManagerHelper
                .get(net.minecraft.resource.ResourceType.SERVER_DATA)
                .registerReloadListener(new net.fugginbeenus.notchcurrency.economy.cosmetic.CosmeticLoader());

        // Auction expiration / cleanup & payouts + login reminders.
        // Auctions are global (overworld-stored), so tick the single state once per
        // server tick rather than once per dimension.
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld overworld = server.getOverworld();
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
            RaffleCommands.register(dispatcher);
            BountyCommands.register(dispatcher);
            CrateKeyCommands.register(dispatcher);
            LoanCommands.register(dispatcher);
            net.fugginbeenus.notchcurrency.command.GamblingCommands.register(dispatcher);
        });

        // HUD balance sync on join + schedule auction mailbox reminder
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity sp = handler.player;
            NotchPackets.sendBalance(sp, BalanceStore.get(sp));

            ServerWorld world = sp.getServerWorld();
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
            ServerPlayerEntity sp = newP;
            NotchPackets.sendBalance(sp, BalanceStore.get(sp));
        });

        // Packet channels must be declared before any receiver is registered or anything is sent.
        net.fugginbeenus.notchcurrency.compat.Net.declareChannels();

        // Server-bound packet receivers (balance request, bids, ATM withdraw, shop ops)
        ServerPacketHandlers.register();

        // StackData needs a registry lookup to (de)serialize whole stacks on 1.21+.
        ServerLifecycleEvents.SERVER_STARTED.register(
                server -> net.fugginbeenus.notchcurrency.compat.RegistryAccess.set(server.getRegistryManager()));

        // Flush & close the economy audit log when the server stops.
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> EconomyLedger.close());
    }


}