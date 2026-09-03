package net.fugginbeenus.notchcurrency.npc.anim;

import net.fugginbeenus.notchcurrency.compat.Net;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class AnimationManager {

    private AnimationManager() {}

    public static void register() {
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> syncTo(handler.getPlayer(), server));
    }

    public static void syncTo(ServerPlayer player, MinecraftServer server) {
        if (player == null || server == null) return;
        NpcAnimationState state = NpcAnimationState.get(server);
        CompoundTag payload = new CompoundTag();
        ListTag list = new ListTag();
        for (NpcAnimation a : state.all()) list.add(a.toNbt());
        payload.put("Animations", list);
        var buf = Net.buf();
        buf.writeNbt(payload);
        Net.sendToClient(player, NotchPackets.ANIM_LIST, buf);
    }

    public static void syncAll(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) syncTo(p, server);
    }
}
