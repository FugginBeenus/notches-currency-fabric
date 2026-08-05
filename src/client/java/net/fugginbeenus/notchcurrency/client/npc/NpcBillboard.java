package net.fugginbeenus.notchcurrency.client.npc;

import net.fugginbeenus.notchcurrency.client.NotchHud;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.client.MinecraftClient;

/**
 * The floating sign over an NPC: a shop's price board, a title, a welcome.
 *
 * <p>This works out WHAT to draw; the renderers draw it, each through vanilla's own name-label
 * routine. Going through that routine is the point: the sign then billboards to face you, picks up
 * the see-through background, and fades out at range exactly like a nameplate.
 *
 * <p>Placeholders are filled in here, on the client, rather than once on the server: {@code %player%}
 * and {@code %balance%} mean something different to everyone reading the sign.
 */
public final class NpcBillboard {

    /** Roughly one label's height in blocks, at the scale vanilla draws names. */
    public static final double LINE_HEIGHT = 0.29;
    /** Clear of the nameplate, so the bottom line doesn't sit on top of it. */
    public static final double BASE_GAP = 0.30;

    private static final String[] NONE = new String[0];

    private NpcBillboard() {}

    /**
     * The sign's lines, bottom line first. That's the order they're drawn in, working upward, which
     * leaves the text reading top-down the way it was typed. Empty when there's no sign to draw.
     */
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

    /** Same placeholders as dialogue, resolved against whoever is looking. */
    private static String substitute(String line, NotchNpcEntity npc) {
        if (line == null || line.isBlank()) return "";
        MinecraftClient mc = MinecraftClient.getInstance();
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
