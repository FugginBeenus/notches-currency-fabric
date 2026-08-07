package net.fugginbeenus.notchcurrency.client.npc;

import net.fugginbeenus.notchcurrency.client.NotchHud;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.client.Minecraft;

public final class NpcBillboard {

    public static final double LINE_HEIGHT = 0.29;
    public static final double BASE_GAP = 0.30;

    private static final String[] NONE = new String[0];

    private NpcBillboard() {}

    public static String[] lines(NotchNpcEntity npc) {
        String raw = npc.getBillboard();
        if (raw == null || raw.isBlank() || npc.isInvisible()) return NONE;
        String[] typed = raw.split("\n", -1);
        String[] out = new String[typed.length];
        for (int i = 0; i < typed.length; i++) {
            out[i] = substitute(typed[typed.length - 1 - i], npc);
        }
        return out;
    }

    private static String substitute(String line, NotchNpcEntity npc) {
        if (line == null || line.isBlank()) return "";
        Minecraft mc = Minecraft.getInstance();
        String viewer = mc.player == null ? "someone" : mc.player.getName().getString();
        String npcName = (npc.hasCustomName() && npc.getCustomName() != null)
                ? npc.getCustomName().getString() : "NPC";
        String out = line.replace("%player%", viewer).replace("%npc%", npcName);
        if (out.contains("%balance%")) {
            out = out.replace("%balance%", Long.toString(NotchHud.getBalance()));
        }
        return out.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
    }
}
