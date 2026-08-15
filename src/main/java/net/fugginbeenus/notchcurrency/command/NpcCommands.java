package net.fugginbeenus.notchcurrency.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.fugginbeenus.notchcurrency.api.NotchNpcApi;
import net.fugginbeenus.notchcurrency.economy.adminshop.AdminShop;
import net.fugginbeenus.notchcurrency.economy.adminshop.AdminShopState;
import net.fugginbeenus.notchcurrency.economy.npc.NpcRole;
import net.fugginbeenus.notchcurrency.economy.npc.NpcRoleState;
import net.fugginbeenus.notchcurrency.npc.NpcPresetManager;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class NpcCommands {

    private static final double REACH = 5.0;

    private NpcCommands() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Fetching models for yourself is nobody else's business, so it sits outside /npc, which
        // is operator gated as a whole.
        dispatcher.register(
                Commands.literal("npcmodels")
                        .then(Commands.literal("sync")
                                .executes(ctx -> {
                                    net.minecraft.server.level.ServerPlayer p = ctx.getSource().getPlayer();
                                    if (p == null) return 0;
                                    net.fugginbeenus.notchcurrency.compat.Msg.chat(p,
                                            net.minecraft.network.chat.Component
                                                    .literal("Checking for NPC models...")
                                                    .withStyle(net.minecraft.ChatFormatting.GRAY));
                                    net.fugginbeenus.notchcurrency.npcmodel.NpcModelShare.greet(p);
                                    return 1;
                                })));

        dispatcher.register(Commands.literal("npc")
                .requires(net.fugginbeenus.notchcurrency.compat.Perms::isOperator)
                .then(Commands.literal("setrole")
                        .then(Commands.literal("banker").executes(ctx -> setRole(ctx.getSource(), NpcRole.BANKER, null)))
                        .then(Commands.literal("auctioneer").executes(ctx -> setRole(ctx.getSource(), NpcRole.AUCTIONEER, null)))
                        .then(Commands.literal("mailbox").executes(ctx -> setRole(ctx.getSource(), NpcRole.MAILBOX, null)))
                        .then(Commands.literal("raffle").executes(ctx -> setRole(ctx.getSource(), NpcRole.RAFFLE, null)))
                        .then(Commands.literal("bounty").executes(ctx -> setRole(ctx.getSource(), NpcRole.BOUNTY, null)))
                        .then(Commands.literal("dealer").executes(ctx -> setRole(ctx.getSource(), NpcRole.DEALER, null)))
                        .then(Commands.literal("cosmetics").executes(ctx -> setRole(ctx.getSource(), NpcRole.COSMETICS, null)))
                        .then(Commands.literal("adminshop")
                                .then(Commands.argument("shop", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            AdminShop shop = AdminShopState.get(ctx.getSource().getServer())
                                                    .getByName(StringArgumentType.getString(ctx, "shop"));
                                            if (shop == null) { ctx.getSource().sendFailure(Component.literal("No such admin shop.")); return 0; }
                                            return setRole(ctx.getSource(), NpcRole.ADMIN_SHOP, shop.getId());
                                        }))))
                .then(Commands.literal("clearrole")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            Entity target = p == null ? null : lookedAt(p);
                            if (target == null) { ctx.getSource().sendFailure(Component.literal("Look at an NPC.")); return 0; }
                            boolean had = NotchNpcApi.clearRole(ctx.getSource().getServer(), target.getUUID());
                            ctx.getSource().sendSuccess(() -> Component.literal(had ? "Role cleared." : "That NPC had no role.")
                                    .withStyle(ChatFormatting.YELLOW), false);
                            return had ? 1 : 0;
                        }))
                .then(Commands.literal("info")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            Entity target = p == null ? null : lookedAt(p);
                            if (target == null) { ctx.getSource().sendFailure(Component.literal("Look at an NPC.")); return 0; }
                            NpcRoleState.Assignment a = NotchNpcApi.getRole(ctx.getSource().getServer(), target.getUUID());
                            ctx.getSource().sendSuccess(() -> Component.literal(a == null
                                    ? "That NPC has no economy role."
                                    : "Role: " + a.role() + (a.shopId() != null ? " (linked shop)" : "")).withStyle(ChatFormatting.AQUA), false);
                            return 1;
                        }))
                .then(Commands.literal("debug")
                        .executes(ctx -> {
                            ServerPlayer p = ctx.getSource().getPlayer();
                            Entity target = p == null ? null : lookedAt(p);
                            if (!(target instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc)) {
                                ctx.getSource().sendFailure(Component.literal("Look at a Notch NPC."));
                                return 0;
                            }
                            for (String line : npc.debugSummary(p)) {
                                ctx.getSource().sendSuccess(() -> Component.literal(line).withStyle(ChatFormatting.AQUA), false);
                            }
                            return 1;
                        }))
                .then(Commands.literal("spawn")
                        .executes(ctx -> spawn(ctx.getSource(), null))
                        .then(Commands.argument("preset", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (String name : NpcPresetManager.list()) {
                                        builder.suggest(name);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> spawn(ctx.getSource(), StringArgumentType.getString(ctx, "preset")))))
                .then(Commands.literal("presets")
                        .executes(ctx -> {
                            java.util.List<String> names = NpcPresetManager.list();
                            ctx.getSource().sendSuccess(() -> Component.literal(names.isEmpty()
                                    ? "No presets saved yet (save one from an NPC's Manage tab)."
                                    : "Presets (" + names.size() + "): " + String.join(", ", names))
                                    .withStyle(ChatFormatting.AQUA), false);
                            return names.size();
                        }))
        );
    }

    private static int spawn(CommandSourceStack src, @Nullable String preset) {
        ServerPlayer p = src.getPlayer();
        if (p == null) {
            src.sendFailure(Component.literal("Run as a player."));
            return 0;
        }
        float yaw = p.getYRot() + 180f; // face the admin
        var npc = preset == null
                ? NotchNpcApi.spawnNpc(p.serverLevel(), p.position(), yaw, p)
                : NotchNpcApi.spawnNpcFromPreset(p.serverLevel(), p.position(), yaw, preset, p);
        if (npc == null) {
            src.sendFailure(Component.literal("No preset named '" + preset + "'. Try /npc presets."));
            return 0;
        }
        src.sendSuccess(() -> Component.literal(preset == null
                ? "NPC spawned. Sneak + right-click it to configure."
                : "NPC spawned from preset '" + preset + "'.").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int setRole(CommandSourceStack src, NpcRole role, @Nullable UUID shopId) {
        ServerPlayer p = src.getPlayer();
        if (p == null) { src.sendFailure(Component.literal("Run as a player.")); return 0; }
        Entity target = lookedAt(p);
        if (target == null) { src.sendFailure(Component.literal("Look at an NPC within reach.")); return 0; }
        if (target instanceof ServerPlayer) { src.sendFailure(Component.literal("You can't assign a role to a player.")); return 0; }

        NotchNpcApi.assignRole(src.getServer(), target.getUUID(), role, shopId);
        src.sendSuccess(() -> Component.literal("Bound this NPC as " + role + ".").withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    @Nullable
    static Entity lookedAt(ServerPlayer p) {
        Vec3 start = p.getEyePosition();
        Vec3 dir = p.getViewVector(1.0f);
        Vec3 end = start.add(dir.scale(REACH));
        AABB box = p.getBoundingBox().expandTowards(dir.scale(REACH)).inflate(1.0);
        EntityHitResult hit = ProjectileUtil.getEntityHitResult(p, start, end, box,
                e -> !e.isSpectator() && e.isPickable(), REACH * REACH);
        return hit != null ? hit.getEntity() : null;
    }
}
