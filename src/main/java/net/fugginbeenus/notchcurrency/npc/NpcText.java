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
     * player shouldn't print half-substituted noise in that case: the player placeholders just resolve
     * to something neutral.
     */
    /**
     * One NPC line, to one player, formatted the way dialogue's chat mode does it.
     *
     * <p>Private on purpose: a busy street of NPCs must not fill everybody's chat. Lives here so the
     * reaction actions and the closed-for-business reply speak with one voice instead of drifting
     * apart the first time either is touched.
     */
    public static void say(ServerPlayerEntity sp, NotchNpcEntity npc, String line) {
        if (sp == null || line == null || line.isBlank()) return;
        npc.playVoice(); // every line it speaks gets its voice, not just the first hello
        sendLine(sp, npc, line);
    }

    /**
     * The text half of {@link #say}, without the voice.
     *
     * <p>Exists because a line spoken to a crowd is still one NPC speaking once. The sound is already
     * heard by everyone in range, so voicing it per reader would stack a dozen copies of the same
     * grunt on top of each other.
     */
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

    /** Classic '&' colour/format codes (&6 gold, &l bold, &r reset) rendered as § formatting. The one
     *  copy, so a name, a sign, a shop title and a spoken line all understand the same codes. */
    public static String colorize(String text) {
        return text == null ? "" : text.replaceAll("&([0-9a-fk-orA-FK-OR])", "§$1");
    }
}
