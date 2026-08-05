package net.fugginbeenus.notchcurrency.api;

import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.server.network.ServerPlayerEntity;

/**
 * A custom NPC role handler another mod registers via
 * {@link NotchNpcApi#registerCustomRole}. When a player right-clicks a Notch NPC whose role is
 * CUSTOM with this handler's id, {@link #interact} runs server-side: open a screen, run a quest
 * hook, whatever the mod wants. Dialogue, appearance, behavior and all other NPC features keep
 * working as normal around it.
 */
@FunctionalInterface
public interface NpcCustomRole {

    void interact(ServerPlayerEntity player, NotchNpcEntity npc);
}
