package net.fugginbeenus.notchcurrency.api;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.server.level.ServerPlayer;

@FunctionalInterface
public interface NpcCustomRole {

    void interact(ServerPlayer player, NotchNpcEntity npc);
}
