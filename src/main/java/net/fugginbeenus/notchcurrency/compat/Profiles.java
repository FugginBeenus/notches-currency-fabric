package net.fugginbeenus.notchcurrency.compat;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;

/**
 * Looking up a player the server has seen before but who is not online now.
 *
 * <p>The leaderboard, the ledger and the offline operator check all need a name or a permission for a
 * bare UUID. Through 1.21.1 that meant the authlib profile cache; 1.21.11 replaced it with a smaller
 * name-to-id cache of its own, and made the operator list speak that type too.
 */
public final class Profiles {

    private Profiles() {}

    /** The stored name for a player id, when the server has ever seen it. */
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

    /** True when the id is on the operator list, whether or not that player is connected. */
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

    /** True when this player opened the world in singleplayer, so the host rather than a guest. */
    public static boolean isSingleplayerOwner(MinecraftServer server, net.minecraft.server.level.ServerPlayer player) {
        //? if >=1.21.11 {
        /*return server.isSingleplayerOwner(new net.minecraft.server.players.NameAndId(player.getGameProfile()));
        *///?} else {
        return server.isSingleplayerOwner(player.getGameProfile());
        //?}
    }
}
