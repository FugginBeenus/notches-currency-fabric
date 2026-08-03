package net.fugginbeenus.notchcurrency.npc;

import net.fugginbeenus.notchcurrency.api.CurrencyApi;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Text an NPC says: placeholder substitution and the name to say it under. Shared by dialogue and by
 * the trigger actions, so a line reads the same however it was set off.
 */
public final class NpcText {

    private NpcText() {}

    /** The NPC's display name, or a plain fallback so a nameless NPC still reads sensibly. */
    public static String npcName(NotchNpcEntity npc) {
        return (npc.hasCustomName() && npc.getCustomName() != null)
                ? npc.getCustomName().getString() : "NPC";
    }

    /**
     * Fill in the placeholders and turn {@code &} codes into real formatting. The player is optional:
     * a trigger can fire with nobody attached (an NPC burning to death, say), and a line written for a
     * player shouldn't print half-substituted noise in that case — the player placeholders just resolve
     * to something neutral.
     */
    public static String substitute(String text, @Nullable ServerPlayerEntity sp, String npcName) {
        if (text == null) return "";
        String out = text
                .replace("%player%", sp == null ? "someone" : sp.getName().getString())
                .replace("%npc%", npcName);
        if (out.contains("%balance%")) {
            out = out.replace("%balance%", sp == null ? "0" : Long.toString(CurrencyApi.getBalance(sp)));
        }
        // Classic '&' color/format codes (&6 gold, &l bold, &r reset, ...) render as § formatting.
        return out.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
    }
}
