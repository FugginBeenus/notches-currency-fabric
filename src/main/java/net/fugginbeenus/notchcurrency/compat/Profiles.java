package net.fugginbeenus.notchcurrency.compat;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

public final class Profiles {

    private Profiles() {}

    public static Optional<String> nameOf(MinecraftServer server, UUID id) {
        //? if >=1.21.11 {
        /*return server.services().nameToIdCache().get(id)
                .map(net.minecraft.server.players.NameAndId::name);
        *///?} else {
        var cache = server.getProfileCache();
        if (cache == null) return Optional.empty();
        return cache.get(id).map(com.mojang.authlib.GameProfile::getName);
        //?}
    }

    public static boolean isOp(MinecraftServer server, UUID id) {
        //? if >=1.21.11 {
        /*return server.services().nameToIdCache().get(id)
                .map(server.getPlayerList()::isOp).orElse(false);
        *///?} else {
        var cache = server.getProfileCache();
        if (cache == null) return false;
        return cache.get(id).map(server.getPlayerList()::isOp).orElse(false);
        //?}
    }

    public static boolean isSingleplayerOwner(MinecraftServer server, net.minecraft.server.level.ServerPlayer player) {
        //? if >=1.21.11 {
        /*return server.isSingleplayerOwner(new net.minecraft.server.players.NameAndId(player.getGameProfile()));
        *///?} else {
        return server.isSingleplayerOwner(player.getGameProfile());
        //?}
    }
}
