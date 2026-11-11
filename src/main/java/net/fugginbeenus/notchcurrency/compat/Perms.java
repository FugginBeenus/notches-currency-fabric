package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.function.Predicate;

public final class Perms {

    public static final String ECO = "eco", CRATE = "crate", RAFFLE = "raffle", BOUNTY = "bounty", QUEST = "quest",
            SHOP = "shop", FACTION = "faction", NPC = "npc", NPC_COMMANDS = "npc.commands", HEARTS = "hearts",
            COSMETICS = "cosmetics";

    private Perms() {}

    public static boolean isOperator(Player player) {
        //? if >=1.21.11 {
        /*return player instanceof net.minecraft.server.level.ServerPlayer sp
                && sp.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        *///?} else {
        return player.hasPermissions(2);
        //?}
    }

    public static boolean isOperator(net.minecraft.commands.CommandSourceStack source) {
        //? if >=1.21.11 {
        /*return source.permissions()
                .hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        *///?} else {
        return source.hasPermission(2);
        //?}
    }

    public static boolean isAdmin(Player player, String area) {
        return has(player, "notchcurrency.admin." + area, isOperator(player));
    }

    public static boolean isAdmin(CommandSourceStack source, String area) {
        return has(source, "notchcurrency.admin." + area, isOperator(source));
    }

    public static Predicate<CommandSourceStack> admin(String area) {
        return source -> isAdmin(source, area);
    }

    public static Predicate<CommandSourceStack> command(String name) {
        return source -> has(source, "notchcurrency.command." + name, true);
    }

    public static boolean has(Player player, String node, boolean fallback) {
        if (Bridge.ENTITY == null) return fallback;
        try {
            return (boolean) Bridge.ENTITY.invoke(player, node, fallback);
        } catch (Throwable e) {
            return fallback;
        }
    }

    public static boolean has(CommandSourceStack source, String node, boolean fallback) {
        if (Bridge.SOURCE == null) return fallback;
        try {
            return (boolean) Bridge.SOURCE.invoke(source, node, fallback);
        } catch (Throwable e) {
            return fallback;
        }
    }

    private static final class Bridge {
        static final MethodHandle SOURCE, ENTITY;

        static {
            MethodHandle source = null, entity = null;
            try {
                Class<?> api = Class.forName("me.lucko.fabric.api.permissions.v0.Permissions");
                for (Method m : api.getDeclaredMethods()) {
                    Class<?>[] p = m.getParameterTypes();
                    if (!m.getName().equals("check") || !Modifier.isStatic(m.getModifiers())
                            || !Modifier.isPublic(m.getModifiers()) || m.getReturnType() != boolean.class
                            || p.length != 3 || p[1] != String.class || p[2] != boolean.class) continue;
                    if (p[0].isAssignableFrom(CommandSourceStack.class)) {
                        source = MethodHandles.publicLookup().unreflect(m);
                    } else if (p[0].isAssignableFrom(ServerPlayer.class)) {
                        entity = MethodHandles.publicLookup().unreflect(m);
                    }
                }
            } catch (Throwable ignored) {
            }
            SOURCE = source;
            ENTITY = entity;
        }
    }
}
