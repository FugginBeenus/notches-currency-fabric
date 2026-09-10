package net.fugginbeenus.notchcurrency.npc.particle;

import net.fugginbeenus.notchcurrency.compat.Net;
import net.fugginbeenus.notchcurrency.net.NotchPackets;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ParticleEffectManager {

    private ParticleEffectManager() {}

    public static void register() {
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register(
                (handler, sender, server) -> syncTo(handler.getPlayer(), server));
    }

    public static void syncTo(ServerPlayer player, MinecraftServer server) {
        if (player == null || server == null) return;
        NpcParticleState state = NpcParticleState.get(server);
        CompoundTag payload = new CompoundTag();
        ListTag list = new ListTag();
        for (ParticleEffect e : state.all()) list.add(e.toNbt());
        payload.put("Effects", list);
        var buf = Net.buf();
        buf.writeNbt(payload);
        Net.sendToClient(player, NotchPackets.PARTICLE_LIST, buf);
    }

    public static void syncAll(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) syncTo(p, server);
    }
}
