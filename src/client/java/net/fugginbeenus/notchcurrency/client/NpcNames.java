package net.fugginbeenus.notchcurrency.client;

import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.List;

public final class NpcNames {

    private NpcNames() {}

    public static List<String> nearby(boolean withBlank) {
        List<String> names = new ArrayList<>();
        if (withBlank) names.add("");
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return names;
        for (net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc : mc.level.getEntitiesOfClass(
                net.fugginbeenus.notchcurrency.entity.NotchNpcEntity.class,
                mc.player.getBoundingBox().inflate(48.0))) {
            String n = net.fugginbeenus.notchcurrency.npc.NpcText.npcName(npc);
            if (!n.isBlank() && !names.contains(n)) names.add(n);
        }
        return names;
    }

    public static String next(String current, boolean withBlank) {
        List<String> names = nearby(withBlank);
        if (names.isEmpty()) return current == null ? "" : current;
        int at = names.indexOf(current == null ? "" : current.trim());
        return names.get((at < 0 ? 0 : at + 1) % names.size());
    }
}
