package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fugginbeenus.notchcurrency.api.NotchNpcApi;
import net.fugginbeenus.notchcurrency.economy.adminshop.AdminShop;
import net.fugginbeenus.notchcurrency.economy.adminshop.AdminShopState;
import net.fugginbeenus.notchcurrency.economy.npc.NpcRole;
import net.fugginbeenus.notchcurrency.economy.npc.NpcRoleState;
import net.fugginbeenus.notchcurrency.npc.NpcPresetManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class NpcCommands {

    private static final double REACH = 5.0;

    private NpcCommands() {}

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("npc")
                .requires(s -> s.hasPermissionLevel(2))
                .then(CommandManager.literal("setrole")
                        .then(CommandManager.literal("banker").executes(ctx -> setRole(ctx.getSource(), NpcRole.BANKER, null)))
                        .then(CommandManager.literal("auctioneer").executes(ctx -> setRole(ctx.getSource(), NpcRole.AUCTIONEER, null)))
                        .then(CommandManager.literal("mailbox").executes(ctx -> setRole(ctx.getSource(), NpcRole.MAILBOX, null)))
                        .then(CommandManager.literal("raffle").executes(ctx -> setRole(ctx.getSource(), NpcRole.RAFFLE, null)))
                        .then(CommandManager.literal("bounty").executes(ctx -> setRole(ctx.getSource(), NpcRole.BOUNTY, null)))
                        .then(CommandManager.literal("dealer").executes(ctx -> setRole(ctx.getSource(), NpcRole.DEALER, null)))
                        .then(CommandManager.literal("cosmetics").executes(ctx -> setRole(ctx.getSource(), NpcRole.COSMETICS, null)))
                        .then(CommandManager.literal("adminshop")
                                .then(CommandManager.argument("shop", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            AdminShop shop = AdminShopState.get(ctx.getSource().getServer())
                                                    .getByName(StringArgumentType.getString(ctx, "shop"));
                                            if (shop == null) { ctx.getSource().sendError(Text.literal("No such admin shop.")); return 0; }
                                            return setRole(ctx.getSource(), NpcRole.ADMIN_SHOP, shop.getId());
                                        }))))
                .then(CommandManager.literal("clearrole")
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            Entity target = p == null ? null : lookedAt(p);
                            if (target == null) { ctx.getSource().sendError(Text.literal("Look at an NPC.")); return 0; }
                            boolean had = NotchNpcApi.clearRole(ctx.getSource().getServer(), target.getUuid());
                            ctx.getSource().sendFeedback(() -> Text.literal(had ? "Role cleared." : "That NPC had no role.")
                                    .formatted(Formatting.YELLOW), false);
                            return had ? 1 : 0;
                        }))
                .then(CommandManager.literal("info")
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            Entity target = p == null ? null : lookedAt(p);
                            if (target == null) { ctx.getSource().sendError(Text.literal("Look at an NPC.")); return 0; }
                            NpcRoleState.Assignment a = NotchNpcApi.getRole(ctx.getSource().getServer(), target.getUuid());
                            ctx.getSource().sendFeedback(() -> Text.literal(a == null
                                    ? "That NPC has no economy role."
                                    : "Role: " + a.role() + (a.shopId() != null ? " (linked shop)" : "")).formatted(Formatting.AQUA), false);
                            return 1;
                        }))
                .then(CommandManager.literal("debug")
                        .executes(ctx -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            Entity target = p == null ? null : lookedAt(p);
                            if (!(target instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc)) {
                                ctx.getSource().sendError(Text.literal("Look at a Notch NPC."));
                                return 0;
                            }
                            for (String line : npc.debugSummary(p)) {
                                ctx.getSource().sendFeedback(() -> Text.literal(line).formatted(Formatting.AQUA), false);
                            }
                            return 1;
                        }))
                .then(CommandManager.literal("spawn")
                        .executes(ctx -> spawn(ctx.getSource(), null))
                        .then(CommandManager.argument("preset", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (String name : NpcPresetManager.list()) {
                                        builder.suggest(name);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> spawn(ctx.getSource(), StringArgumentType.getString(ctx, "preset")))))
                .then(CommandManager.literal("presets")
                        .executes(ctx -> {
                            java.util.List<String> names = NpcPresetManager.list();
                            ctx.getSource().sendFeedback(() -> Text.literal(names.isEmpty()
                                    ? "No presets saved yet (save one from an NPC's Manage tab)."
                                    : "Presets (" + names.size() + "): " + String.join(", ", names))
                                    .formatted(Formatting.AQUA), false);
                            return names.size();
                        }))
        );
    }

    private static int spawn(ServerCommandSource src, @Nullable String preset) {
        ServerPlayerEntity p = src.getPlayer();
        if (p == null) {
            src.sendError(Text.literal("Run as a player."));
            return 0;
        }
        float yaw = p.getYaw() + 180f; // face the admin
        var npc = preset == null
                ? NotchNpcApi.spawnNpc(p.getServerWorld(), p.getPos(), yaw, p)
                : NotchNpcApi.spawnNpcFromPreset(p.getServerWorld(), p.getPos(), yaw, preset, p);
        if (npc == null) {
            src.sendError(Text.literal("No preset named '" + preset + "'. Try /npc presets."));
            return 0;
        }
        src.sendFeedback(() -> Text.literal(preset == null
                ? "NPC spawned. Sneak + right-click it to configure."
                : "NPC spawned from preset '" + preset + "'.").formatted(Formatting.GREEN), true);
        return 1;
    }

    private static int setRole(ServerCommandSource src, NpcRole role, @Nullable UUID shopId) {
        ServerPlayerEntity p = src.getPlayer();
        if (p == null) { src.sendError(Text.literal("Run as a player.")); return 0; }
        Entity target = lookedAt(p);
        if (target == null) { src.sendError(Text.literal("Look at an NPC within reach.")); return 0; }
        if (target instanceof ServerPlayerEntity) { src.sendError(Text.literal("You can't assign a role to a player.")); return 0; }

        NotchNpcApi.assignRole(src.getServer(), target.getUuid(), role, shopId);
        src.sendFeedback(() -> Text.literal("Bound this NPC as " + role + ".").formatted(Formatting.GREEN), true);
        return 1;
    }

    @Nullable
    private static Entity lookedAt(ServerPlayerEntity p) {
        Vec3d start = p.getEyePos();
        Vec3d dir = p.getRotationVec(1.0f);
        Vec3d end = start.add(dir.multiply(REACH));
        Box box = p.getBoundingBox().stretch(dir.multiply(REACH)).expand(1.0);
        EntityHitResult hit = ProjectileUtil.raycast(p, start, end, box,
                e -> !e.isSpectator() && e.canHit(), REACH * REACH);
        return hit != null ? hit.getEntity() : null;
    }
}
