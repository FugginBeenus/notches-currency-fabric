package net.fugginbeenus.notchcurrency.npc;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

public final class NpcText {

    private NpcText() {}

    public static String npcName(NotchNpcEntity npc) {
        return (npc.hasCustomName() && npc.getCustomName() != null)
                ? npc.getCustomName().getString() : "NPC";
    }

    public static void say(ServerPlayerEntity sp, NotchNpcEntity npc, String line) {
        if (sp == null || line == null || line.isBlank()) return;
        npc.playVoice(); // every line it speaks gets its voice, not just the first hello
        sendLine(sp, npc, line);
    }

    public static void sendLine(ServerPlayerEntity sp, NotchNpcEntity npc, String line) {
        if (sp == null || line == null || line.isBlank()) return;
        String name = npcName(npc);
        sp.sendMessage(net.minecraft.text.Text.literal("<" + name + "> " + substitute(line, sp, name))
                .formatted(net.minecraft.util.Formatting.WHITE), false);
    }

    public static String substitute(String text, @Nullable ServerPlayerEntity sp, String npcName) {
        if (text == null) return "";
        String out = text
                .replace("%player%", sp == null ? "someone" : sp.getName().getString())
                .replace("%npc%", npcName);
        if (out.contains("%balance%")) {
            out = out.replace("%balance%", sp == null ? "0" : Long.toString(CurrencyApi.getBalance(sp)));
        }
        return colorize(out);
    }

    public static String colorize(String text) {
        return text == null ? "" : text.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
    }
}
