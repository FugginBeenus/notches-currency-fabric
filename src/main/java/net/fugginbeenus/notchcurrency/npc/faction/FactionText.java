package net.fugginbeenus.notchcurrency.npc.faction;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

public final class FactionText {

    private FactionText() {}

    public static MutableComponent name(@Nullable MinecraftServer server, Faction faction) {
        Faction parent = server == null || !faction.isHouse() ? null : FactionState.get(server).get(faction.parent());
        if (parent == null) return Component.literal(faction.displayName()).withStyle(faction.color());
        return Component.literal(parent.displayName()).withStyle(faction.color())
                .append(Component.literal(" > ").withStyle(ChatFormatting.GRAY))
                .append(Component.literal(faction.displayName()).withStyle(faction.color()));
    }

    public static String plain(@Nullable MinecraftServer server, Faction faction) {
        Faction parent = server == null || !faction.isHouse() ? null : FactionState.get(server).get(faction.parent());
        return parent == null ? faction.displayName() : parent.displayName() + " > " + faction.displayName();
    }
}
