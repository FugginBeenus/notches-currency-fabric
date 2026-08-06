package net.fugginbeenus.notchcurrency.api;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.server.network.ServerPlayerEntity;

@FunctionalInterface
public interface NpcCustomRole {

    void interact(ServerPlayerEntity player, NotchNpcEntity npc);
}
