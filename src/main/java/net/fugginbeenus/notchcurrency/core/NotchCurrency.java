package net.fugginbeenus.notchcurrency.core;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fugginbeenus.notchcurrency.auction.AuctionConfig;
import net.fugginbeenus.notchcurrency.auction.AuctionState;
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
        MutableComponent t = Component.literal("\uE000");

        return t.withStyle(style -> style
                .withColor(net.minecraft.network.chat.TextColor.fromRgb(0xFFFFFF))
                .withHoverEvent(net.fugginbeenus.notchcurrency.compat.Chat.showItem(
                        new ItemStack(ModItems.NOTCH_COIN))));
    }

    public static Component coins(long amount) {
        return Component.literal(Long.toString(amount) + " ").append(coinIcon());
    }

    @Override
    public void onInitialize() {
        net.fugginbeenus.notchcurrency.compat.Net.declareChannels();
        net.fugginbeenus.notchcurrency.compat.Geo.init();
        ModBlocks.register();
        net.fugginbeenus.notchcurrency.registry.ModBlockEntities.register();
        ModItems.register();
        ModScreenHandlers.register();
        ModCreativeTab.register();
        TradeManager.init();
        ModEntities.register();
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(
                net.fugginbeenus.notchcurrency.registry.ModEntities.NOTCH_NPC,
                net.fugginbeenus.notchcurrency.entity.NotchNpcEntity.createAttributes()
        );

        CrateDropManager.init();
        GoldenCacheManager.init();
        DailyCrateManager.init();
        net.fugginbeenus.notchcurrency.economy.crate.CrateManager.init();
        BossCurrencyInject.init();
        WealthTax.init();
        ShopRent.init();
        RaffleManager.init();
        BountyManager.init();
        net.fugginbeenus.notchcurrency.economy.loan.LoanManager.init();
        net.fugginbeenus.notchcurrency.economy.gambling.GamblingManager.init();
        NpcRoleInteractionHandler.register();
        NotchConfig cfg = NotchConfigIO.load();
        GoldenCacheManager.applyConfig(cfg);
        AuctionConfig.apply(cfg);
        WealthTax.applyConfig(cfg);
        ShopRent.applyConfig(cfg);
        net.fugginbeenus.notchcurrency.shop.ShopRules.applyConfig(cfg);
        RaffleManager.applyConfig(cfg);
        BountyManager.applyConfig(cfg);
        net.fugginbeenus.notchcurrency.economy.crate.CrateManager.applyConfig(cfg);
        net.fugginbeenus.notchcurrency.economy.loan.LoanManager.applyConfig(cfg);
        net.fugginbeenus.notchcurrency.economy.gambling.GamblingManager.applyConfig(cfg);
        net.fugginbeenus.notchcurrency.economy.enchanter.EnchanterManager.applyConfig(cfg);
        net.fugginbeenus.notchcurrency.economy.cosmetic.CosmeticManager.applyConfig(cfg);
        net.fugginbeenus.notchcurrency.integration.WaystoneFeeHandler.applyConfig(cfg);
        net.fugginbeenus.notchcurrency.economy.villager.VillagerCoinTrades.applyConfig(cfg);

        if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("waystones")) {
            net.fugginbeenus.notchcurrency.integration.WaystoneFeeHandler.register();
        }

        net.fabricmc.fabric.api.resource.ResourceManagerHelper
                .get(net.minecraft.server.packs.PackType.SERVER_DATA)
                .registerReloadListener(new net.fugginbeenus.notchcurrency.economy.bounty.BountyPoolLoader());
        net.fabricmc.fabric.api.resource.ResourceManagerHelper
                .get(net.minecraft.server.packs.PackType.SERVER_DATA)
                .registerReloadListener(new net.fugginbeenus.notchcurrency.economy.crate.CrateLoader());
        net.fabricmc.fabric.api.resource.ResourceManagerHelper
                .get(net.minecraft.server.packs.PackType.SERVER_DATA)
                .registerReloadListener(new net.fugginbeenus.notchcurrency.economy.cosmetic.CosmeticLoader());

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerLevel overworld = server.overworld();
            if (overworld == null) return;
            AuctionState state = AuctionState.get(overworld);
            state.tick(overworld);
            state.checkLoginReminders(overworld);
            net.fugginbeenus.notchcurrency.shop.ShopState.get(overworld).tickDecay();
            net.fugginbeenus.notchcurrency.npc.dialogue.NpcDialogueManager.tick(server);
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            CurrencyCommands.register(dispatcher);
            TradeCommands.register(dispatcher);
            CrateCommands.register(dispatcher);
            AuctionCommands.register(dispatcher);
            ShopCommands.register(dispatcher);
            EcoCommands.register(dispatcher);
            NpcCommands.register(dispatcher);
            net.fugginbeenus.notchcurrency.command.FactionCommands.register(dispatcher);
            RaffleCommands.register(dispatcher);
            BountyCommands.register(dispatcher);
            CrateKeyCommands.register(dispatcher);
            LoanCommands.register(dispatcher);
            net.fugginbeenus.notchcurrency.command.GamblingCommands.register(dispatcher);
            net.fugginbeenus.notchcurrency.command.HeartCommands.register(dispatcher);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer sp = handler.player;
            NotchPackets.sendBalance(sp, BalanceStore.get(sp));
            ServerLevel world = sp.serverLevel();
            AuctionState state = AuctionState.get(world);
            state.onPlayerJoin(sp);
            RaffleManager.remindOnJoin(sp);
            net.fugginbeenus.notchcurrency.mail.MailSweep.run(server);
            net.fugginbeenus.notchcurrency.mail.MailManager.greet(sp);
            net.fugginbeenus.notchcurrency.currency.CurrencyServerSync.send(sp);
            net.fugginbeenus.notchcurrency.economy.bounty.BountyManager.syncTracker(sp);
            net.fugginbeenus.notchcurrency.crate.DailyCrateManager.sendTo(sp);
            net.fugginbeenus.notchcurrency.npcmodel.NpcModelShare.greet(sp);
            HeartState.applyTo(sp);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                net.fugginbeenus.notchcurrency.npcmodel.NpcModelShare.forget(handler.player));

        ServerPlayerEvents.COPY_FROM.register((oldP, newP, alive) -> {
            ServerPlayer sp = newP;
            NotchPackets.sendBalance(sp, BalanceStore.get(sp));
        });

        ServerPlayerEvents.AFTER_RESPAWN.register((oldP, newP, alive) -> {
            if (!alive) {
                int left = HeartState.onDeath(newP);
                if (left >= 0) {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(newP, net.minecraft.network.chat.Component
                            .literal(left == 0
                                    ? "Your last heart crystal shattered."
                                    : "A heart crystal shattered. " + left
                                            + (left == 1 ? " extra heart left." : " extra hearts left."))
                            .withStyle(net.minecraft.ChatFormatting.RED));
                }
            }
            HeartState.applyTo(newP);
            if (!alive) newP.setHealth(newP.getMaxHealth());
        });

        ServerPacketHandlers.register();
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            net.fugginbeenus.notchcurrency.compat.RegistryAccess.setServer(server.registryAccess());
            net.fugginbeenus.notchcurrency.mail.MailSweep.run(server);
            net.fugginbeenus.notchcurrency.npcmodel.NpcModelServerStore.load(server);
            net.fugginbeenus.notchcurrency.crate.DailyCrateManager.readFromWorld(
                    server, net.fugginbeenus.notchcurrency.config.NotchConfigIO.get());
            net.minecraft.server.level.ServerLevel main = server.overworld();
            if (main != null) {
                net.fugginbeenus.notchcurrency.shop.AdminShopMigration.run(server, main);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(
                server -> net.fugginbeenus.notchcurrency.compat.RegistryAccess.setServer(null));

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

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> EconomyLedger.close());
    }


}