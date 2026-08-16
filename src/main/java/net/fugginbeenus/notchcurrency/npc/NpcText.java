package net.fugginbeenus.notchcurrency.npc;

import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

public final class NpcText {

    private NpcText() {}

    public static String npcName(NotchNpcEntity npc) {
        return (npc.hasCustomName() && npc.getCustomName() != null)
                ? npc.getCustomName().getString() : "NPC";
    }

    public static void say(ServerPlayer sp, NotchNpcEntity npc, String line) {
        if (sp == null || line == null || line.isBlank()) return;
        npc.playVoice();
        sendLine(sp, npc, line);
    }

    public static void sendLine(ServerPlayer sp, NotchNpcEntity npc, String line) {
        if (sp == null || line == null || line.isBlank()) return;
        String name = npcName(npc);
        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, net.minecraft.network.chat.Component.literal("<" + name + "> " + substitute(line, sp, name))
                .withStyle(net.minecraft.ChatFormatting.WHITE));
    }

    public static String substitute(String text, @Nullable ServerPlayer sp, String npcName) {
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
