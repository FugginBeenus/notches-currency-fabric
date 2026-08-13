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
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import java.util.Collection;
import java.util.UUID;

import static net.fugginbeenus.notchcurrency.core.NotchCurrency.coinIcon;
import static net.fugginbeenus.notchcurrency.core.NotchCurrency.coins;

public final class CrateCommands {

    private CrateCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // ===== /balloon (spawn + admin settings) =====
        dispatcher.register(
                Commands.literal("balloon")
                        .requires(net.fugginbeenus.notchcurrency.compat.Perms::isOperator)

                        // /balloon spawn [pos]
                        .then(Commands.literal("spawn")
                                // no args -> at player
                                .executes(ctx -> {
                                    var src = ctx.getSource();
                                    var world = src.getLevel();
                                    ServerPlayer player = src.getPlayer();
                                    if (player == null) {
                                        src.sendFailure(Component.literal(
                                                "Run as a player or use: /balloon spawn <x> <y> <z>"));
                                        return 0;
                                    }
                                    BlockPos base = player.blockPosition().above(12);
                                    BalloonEntity b = new BalloonEntity(world,
                                            base.getX() + 0.5, base.getY(), base.getZ() + 0.5);
                                    world.addFreshEntity(b);
                                    src.sendSuccess(() -> Component.literal(
                                            "Spawned test balloon at " + base.getX() + " " + base.getY() + " " + base.getZ()
                                    ), false);
                                    return 1;
                                })
                                // with explicit block pos
                                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                        .executes(ctx -> {
                                            var src = ctx.getSource();
                                            var world = src.getLevel();
                                            BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
                                            BalloonEntity b = new BalloonEntity(world,
                                                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                                            world.addFreshEntity(b);
                                            src.sendSuccess(() -> Component.literal(
                                                    "Spawned test balloon at " + pos.getX() + " " + pos.getY() + " " + pos.getZ()
                                            ), false);
                                            return 1;
                                        })
                                )
                        )

                        // /balloon setArea <x> <y> <z> <radius>
                        .then(Commands.literal("setArea")
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                                        .then(Commands.argument("radius", IntegerArgumentType.integer(1))
                                                                .executes(ctx -> {
                                                                    var src = ctx.getSource();
                                                                    var world = src.getLevel();
                                                                    int x = IntegerArgumentType.getInteger(ctx, "x");
                                                                    int y = IntegerArgumentType.getInteger(ctx, "y");
                                                                    int z = IntegerArgumentType.getInteger(ctx, "z");
                                                                    int r = IntegerArgumentType.getInteger(ctx, "radius");
                                                                    DailyCrateManager.setArea((ServerLevel) world,
                                                                            new BlockPos(x, y, z), r);
                                                                    src.sendSuccess(() -> Component.literal(
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
                        .then(Commands.literal("setYRange")
                                .then(Commands.argument("minY", IntegerArgumentType.integer(5))
                                        .then(Commands.argument("maxY", IntegerArgumentType.integer(6))
                                                .executes(ctx -> {
                                                    var src = ctx.getSource();
                                                    var world = src.getLevel();
                                                    int minY = IntegerArgumentType.getInteger(ctx, "minY");
                                                    int maxY = IntegerArgumentType.getInteger(ctx, "maxY");
                                                    DailyCrateManager.setYRange((ServerLevel) world, minY, maxY);
                                                    src.sendSuccess(() -> Component.literal(
                                                            "Balloon Y range set to [" + minY + ".." + maxY + "]"
                                                    ), false);
                                                    return 1;
                                                })
                                        )
                                )
                        )

                        // /balloon setCount <perDay>
                        .then(Commands.literal("setCount")
                                .then(Commands.argument("perDay", IntegerArgumentType.integer(0))
                                        .executes(ctx -> {
                                            var src = ctx.getSource();
                                            var world = src.getLevel();
                                            int per = IntegerArgumentType.getInteger(ctx, "perDay");
                                            DailyCrateManager.setCount((ServerLevel) world, per);
                                            src.sendSuccess(() -> Component.literal(
                                                    "Balloons per day set to " + per
                                            ), false);
                                            return 1;
                                        })
                                )
                        )

                        // /balloon perplayer on|off  - one each, alongside the wave
                        .then(Commands.literal("perplayer")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            boolean on = BoolArgumentType.getBool(ctx, "enabled");
                                            DailyCrateManager.setPerPlayer(ctx.getSource().getLevel(), on);
                                            ctx.getSource().sendSuccess(() -> Component.literal(on
                                                    ? "Each player online now gets a balloon when a wave fires."
                                                    : "Waves spawn over the area only."), true);
                                            return 1;
                                        })))
                        // /balloon playerarea on|off  - only inside the balloon area, or anywhere
                        .then(Commands.literal("playerarea")
                                .then(Commands.argument("areaOnly", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            boolean areaOnly = BoolArgumentType.getBool(ctx, "areaOnly");
                                            DailyCrateManager.setPlayerInAreaOnly(ctx.getSource().getLevel(), areaOnly);
                                            ctx.getSource().sendSuccess(() -> Component.literal(areaOnly
                                                    ? "Only players inside the balloon area get one."
                                                    : "Players anywhere get one."), true);
                                            return 1;
                                        })))
                        // /balloon playerheight <blocksUp> <spread>
                        .then(Commands.literal("playerheight")
                                .then(Commands.argument("up", IntegerArgumentType.integer(5, 200))
                                        .then(Commands.argument("spread", IntegerArgumentType.integer(1, 64))
                                                .executes(ctx -> {
                                                    int up = IntegerArgumentType.getInteger(ctx, "up");
                                                    int spread = IntegerArgumentType.getInteger(ctx, "spread");
                                                    DailyCrateManager.setPlayerHeight(ctx.getSource().getLevel(), up, spread);
                                                    ctx.getSource().sendSuccess(() -> Component.literal(
                                                            "A player's balloon appears " + up + " up and within "
                                                                    + spread + " blocks."), true);
                                                    return 1;
                                                }))))
                        // /balloon announce on|off
                        .then(Commands.literal("announce")
                                .then(Commands.literal("on").executes(ctx -> {
                                    var src = ctx.getSource();
                                    DailyCrateManager.setAnnouncements((ServerLevel) src.getLevel(), true);
                                    src.sendSuccess(() -> Component.literal("Balloon announcements: ON"), false);
                                    return 1;
                                }))
                                .then(Commands.literal("off").executes(ctx -> {
                                    var src = ctx.getSource();
                                    DailyCrateManager.setAnnouncements((ServerLevel) src.getLevel(), false);
                                    src.sendSuccess(() -> Component.literal("Balloon announcements: OFF"), false);
                                    return 1;
                                }))
                        )
        );

        // ===== /cache =====
        dispatcher.register(
                Commands.literal("cache")
                        .requires(net.fugginbeenus.notchcurrency.compat.Perms::isOperator)
                        // /cache spawn [radius]
                        .then(Commands.literal("spawn")
                                .then(Commands.argument("radius", IntegerArgumentType.integer(8, 256))
                                        .executes(ctx -> {
                                            ServerPlayer p = ctx.getSource().getPlayer();
                                            int r = IntegerArgumentType.getInteger(ctx, "radius");
                                            var world = p.serverLevel();
                                            var placed = GoldenCacheManager.spawnNear(world, p.blockPosition(), r);
                                            if (placed != null) {
                                                ctx.getSource().sendSuccess(
                                                        () -> Component.literal("Spawned Golden Cache at " + placed.toShortString()),
                                                        false);
                                                return 1;
                                            } else {
                                                ctx.getSource().sendFailure(Component.literal("Failed to place cache."));
                                                return 0;
                                            }
                                        })
                                )
                                .executes(ctx -> {
                                    // default radius 64
                                    ServerPlayer p = ctx.getSource().getPlayer();
                                    var world = p.serverLevel();
                                    var placed = GoldenCacheManager.spawnNear(world, p.blockPosition(), 64);
                                    if (placed != null) {
                                        ctx.getSource().sendSuccess(
                                                () -> Component.literal("Spawned Golden Cache at " + placed.toShortString()), false);
                                        return 1;
                                    } else {
                                        ctx.getSource().sendFailure(Component.literal("Failed to place cache."));
                                        return 0;
                                    }
                                })
                        )
                        // /cache spawn_at <x> <y> <z>
                        .then(Commands.literal("spawn_at")
                                .then(Commands.argument("x", IntegerArgumentType.integer())
                                        .then(Commands.argument("y", IntegerArgumentType.integer())
                                                .then(Commands.argument("z", IntegerArgumentType.integer())
                                                        .executes(ctx -> {
                                                            var src = ctx.getSource();
                                                            var world = src.getLevel();
                                                            int x = IntegerArgumentType.getInteger(ctx, "x");
                                                            int y = IntegerArgumentType.getInteger(ctx, "y");
                                                            int z = IntegerArgumentType.getInteger(ctx, "z");
                                                            var placed = GoldenCacheManager.spawnAt(
                                                                    (ServerLevel) world, x, y, z);
                                                            if (placed != null) {
                                                                src.sendSuccess(
                                                                        () -> Component.literal(
                                                                                "Spawned Golden Cache at " + placed.toShortString()
                                                                        ), false);
                                                                return 1;
                                                            } else {
                                                                src.sendFailure(Component.literal("Failed to place cache."));
                                                                return 0;
                                                            }
                                                        })
                                                )
                                        )
                                )
                        )
                        // /cache announce <true|false>
                        .then(Commands.literal("announce")
                                .then(Commands.argument("enabled", BoolArgumentType.bool())
                                        .executes(ctx -> {
                                            boolean enabled = BoolArgumentType.getBool(ctx, "enabled");
                                            GoldenCacheManager.setAnnouncements(enabled);
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("Golden Cache announcements: " +
                                                            (enabled ? "ON" : "OFF")), false);
                                            return 1;
                                        })
                                )
                        )
        );

    }
}
