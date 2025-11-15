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

import net.fugginbeenus.notchcurrency.config.NotchConfig;
import net.fugginbeenus.notchcurrency.config.NotchConfigIO;
import net.fugginbeenus.notchcurrency.crate.BalloonEntity;
import net.fugginbeenus.notchcurrency.crate.CrateDropManager;
import net.fugginbeenus.notchcurrency.crate.DailyCrateManager;
import net.fugginbeenus.notchcurrency.crate.GoldenCacheManager;
import net.fugginbeenus.notchcurrency.loot.BossCurrencyInject;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.fugginbeenus.notchcurrency.registry.ModBlocks;
import net.fugginbeenus.notchcurrency.registry.ModEntities;
import net.fugginbeenus.notchcurrency.registry.ModItems;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.fugginbeenus.notchcurrency.trade.TradeManager;

import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class NotchCurrency implements ModInitializer {

    public static final String MOD_ID = "notchcurrency";
    public static Identifier id(String path) { return new Identifier(MOD_ID, path); }

    @Override
    public void onInitialize() {
        // Registries
        ModBlocks.register();
        ModItems.register();
        ModScreenHandlers.register();
        TradeManager.init();
        ModEntities.register();

        // Managers
        CrateDropManager.init();
        GoldenCacheManager.init();
        DailyCrateManager.init();
        BossCurrencyInject.init();

        // Load config (applies defaults on missing)
        NotchConfig cfg = NotchConfigIO.load();
        DailyCrateManager.applyConfig(cfg);
        GoldenCacheManager.applyConfig(cfg);

        // Commands
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, env) -> {
            registerCommands(dispatcher);

            // ----- /trade -----
            dispatcher.register(
                    CommandManager.literal("trade")
                            .then(CommandManager.argument("player", EntityArgumentType.player())
                                    .executes(ctx -> {
                                        ServerPlayerEntity from = ctx.getSource().getPlayer();
                                        ServerPlayerEntity to   = EntityArgumentType.getPlayer(ctx, "player");
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

            // ----- /balloon spawn [pos] -----
            dispatcher.register(
                    CommandManager.literal("balloon")
                            .requires(src -> src.hasPermissionLevel(2))
                            .then(CommandManager.literal("spawn")
                                    // no args -> at player
                                    .executes(ctx -> {
                                        var src = ctx.getSource();
                                        var world = src.getWorld();
                                        ServerPlayerEntity player = src.getPlayer();
                                        if (player == null) {
                                            src.sendError(Text.literal("Run as a player or use: /balloon spawn <x> <y> <z>"));
                                            return 0;
                                        }
                                        BlockPos base = player.getBlockPos().up(12);
                                        BalloonEntity b = new BalloonEntity(world,
                                                base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
                                        world.spawnEntity(b);
                                        src.sendFeedback(() -> Text.literal(
                                                "Spawned test balloon at " + base.getX() + " " + base.getY() + " " + base.getZ()), false);
                                        return 1;
                                    })
                                    // with pos
                                    .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                                            .executes(ctx -> {
                                                var src = ctx.getSource();
                                                var world = src.getWorld();
                                                BlockPos pos = BlockPosArgumentType.getBlockPos(ctx, "pos");
                                                BalloonEntity b = new BalloonEntity(world,
                                                        pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                                                world.spawnEntity(b);
                                                src.sendFeedback(() -> Text.literal(
                                                        "Spawned test balloon at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()), false);
                                                return 1;
                                            })
                                    )
                            )
            );

            // ----- /cache -----
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
                                                            () -> Text.literal("Spawned Golden Cache at " + placed.toShortString()), false);
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
                                                                var placed = GoldenCacheManager.spawnAt((ServerWorld) world, x, y, z);
                                                                if (placed != null) {
                                                                    src.sendFeedback(
                                                                            () -> Text.literal("Spawned Golden Cache at " + placed.toShortString()), false);
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
                                                        () -> Text.literal("Golden Cache announcements: " + (enabled ? "ON" : "OFF")), false);
                                                return 1;
                                            })
                                    )
                            )
            );

            // ----- /balance + /bal -----
            dispatcher.register(
                    CommandManager.literal("balance")
                            .executes(ctx -> {
                                var p = ctx.getSource().getPlayer();
                                int bal = BalanceStore.get(p);
                                p.sendMessage(Text.literal("Balance: " + bal + " ⛁"), true);
                                NotchPackets.sendBalance(p, bal);
                                return 1;
                            })
            );
            dispatcher.register(
                    CommandManager.literal("bal")
                            .executes(ctx -> {
                                // forward to /balance
                                return ctx.getSource().getServer().getCommandManager()
                                        .executeWithPrefix(ctx.getSource(), "balance");
                            })
            );

            // ----- /pay <player> <amount> -----
            dispatcher.register(
                    CommandManager.literal("pay")
                            .then(CommandManager.argument("target", EntityArgumentType.player())
                                    .then(CommandManager.argument("amount", IntegerArgumentType.integer(1))
                                            .executes(ctx -> {
                                                var from = ctx.getSource().getPlayer();
                                                var to   = EntityArgumentType.getPlayer(ctx, "target");
                                                int amt  = IntegerArgumentType.getInteger(ctx, "amount");

                                                if (from == to) {
                                                    from.sendMessage(Text.literal("You can’t pay yourself.")
                                                            .formatted(net.minecraft.util.Formatting.RED), false);
                                                    return 0;
                                                }

                                                int bal = BalanceStore.get(from);
                                                if (bal < amt) {
                                                    from.sendMessage(Text.literal("Insufficient funds.")
                                                            .formatted(net.minecraft.util.Formatting.RED), false);
                                                    return 0;
                                                }

                                                BalanceStore.subtract(from, amt);
                                                BalanceStore.add(to, amt);

                                                // live HUD sync
                                                NotchPackets.sendBalance(from, BalanceStore.get(from));
                                                NotchPackets.sendBalance(to,   BalanceStore.get(to));

                                                from.sendMessage(Text.literal(
                                                                "Paid " + amt + " ⛁ to " + to.getName().getString())
                                                        .formatted(net.minecraft.util.Formatting.GREEN), false);
                                                to.sendMessage(Text.literal(
                                                                from.getName().getString() + " paid you " + amt + " ⛁")
                                                        .formatted(net.minecraft.util.Formatting.GREEN), false);
                                                return 1;
                                            })
                                    )
                            )
            );
        });

        // HUD balance sync on join
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity sp = handler.player;
            NotchPackets.sendBalance(sp, BalanceStore.get(sp));
        });

        // HUD balance sync on respawn
        ServerPlayerEvents.COPY_FROM.register((oldP, newP, alive) -> {
            ServerPlayerEntity sp = newP;
            NotchPackets.sendBalance(sp, BalanceStore.get(sp));
        });

        // Server handles client's explicit balance request
        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.BALANCE_REQUEST, (server, player, handler, buf, response) -> {
            server.execute(() -> {
                NotchPackets.sendBalance(player, BalanceStore.get(player));
            });
        });
    }

    private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        // /givnotches <amount> (admin only)
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
                                })
                        )
        );

        // /balloon admin settings
        dispatcher.register(
                CommandManager.literal("balloon")
                        .requires(src -> src.hasPermissionLevel(2))
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
                                                                    DailyCrateManager.setArea((ServerWorld) world, new BlockPos(x, y, z), r);
                                                                    src.sendFeedback(() -> Text.literal("Balloon area set to (" + x + "," + y + "," + z + ") r=" + r), false);
                                                                    return 1;
                                                                })
                                                        )
                                                )
                                        )
                                )
                        )
                        .then(CommandManager.literal("setYRange")
                                .then(CommandManager.argument("minY", IntegerArgumentType.integer(5))
                                        .then(CommandManager.argument("maxY", IntegerArgumentType.integer(6))
                                                .executes(ctx -> {
                                                    var src = ctx.getSource();
                                                    var world = src.getWorld();
                                                    int minY = IntegerArgumentType.getInteger(ctx, "minY");
                                                    int maxY = IntegerArgumentType.getInteger(ctx, "maxY");
                                                    DailyCrateManager.setYRange((ServerWorld) world, minY, maxY);
                                                    src.sendFeedback(() -> Text.literal("Balloon Y range set to [" + minY + ".." + maxY + "]"), false);
                                                    return 1;
                                                })
                                        )
                                )
                        )
                        .then(CommandManager.literal("setCount")
                                .then(CommandManager.argument("perDay", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            var src = ctx.getSource();
                                            var world = src.getWorld();
                                            int per = IntegerArgumentType.getInteger(ctx, "perDay");
                                            DailyCrateManager.setCount((ServerWorld) world, per);
                                            src.sendFeedback(() -> Text.literal("Balloons per day set to " + per), false);
                                            return 1;
                                        })
                                )
                        )
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
    }
}
