package net.fugginbeenus.notchcurrency.client.bounty;

import java.util.Locale;
import java.util.UUID;

public final class Requester {

    private static final String[] NAMES = {
            "Tobias", "Hilda", "Mabel", "Oswin", "Agnes", "Bram", "Cedric", "Edith", "Elric", "Fern", "Gideon",
            "Greta", "Hugo", "Ida", "Jory", "Linus", "Maud", "Nell", "Osric", "Pip", "Rowan", "Silas", "Tilda",
            "Una", "Wendel", "Wren", "Alba", "Corin", "Dunstan", "Esme", "Godric", "Ivy"
    };
    private static final String[][] TRADES = {
            {"farmer", "wheat", "carrot", "potato", "beetroot", "pumpkin", "melon", "sugar_cane", "bread", "egg",
                    "hay", "cocoa", "apple", "seeds", "berries", "honey"},
            {"butcher", "beef", "porkchop", "chicken", "mutton", "rabbit", "cooked"},
            {"fisherman", "cod", "salmon", "fish", "kelp", "ink_sac", "nautilus", "prismarine"},
            {"shepherd", "wool", "string", "carpet", "bed", "shears"},
            {"leatherworker", "leather", "hide", "saddle"},
            {"librarian", "book", "paper", "quill"},
            {"fletcher", "arrow", "stick", "flint", "feather", "bow"},
            {"cleric", "glowstone", "blaze", "nether_wart", "spider_eye", "bone", "rotten_flesh", "ender_pearl",
                    "slime", "phantom"},
            {"cartographer", "map", "compass", "glass"},
            {"toolsmith", "iron", "gold", "copper", "coal", "diamond", "emerald", "redstone", "lapis", "netherite",
                    "ingot", "ore", "raw_"},
            {"mason", "stone", "cobble", "brick", "clay", "terracotta", "granite", "andesite", "diorite",
                    "deepslate", "quartz", "sand"},
    };
    private static final String[] UNDEAD = {"zombie", "skeleton", "husk", "stray", "drowned", "phantom", "wither",
            "zoglin", "bogged"};
    private static final String[] RAIDERS = {"pillager", "vindicator", "evoker", "ravager", "witch", "vex",
            "illusioner"};

    private Requester() {}

    public static String name(UUID id) {
        return NAMES[Math.floorMod(id.hashCode(), NAMES.length)];
    }

    public static String trade(String type, String target, UUID id) {
        String t = target == null ? "" : target.toLowerCase(Locale.ROOT);
        int pick = Math.floorMod(Long.hashCode(id.getLeastSignificantBits()), 3);
        return switch (type) {
            case "KILL" -> any(t, UNDEAD) ? "cleric" : any(t, RAIDERS) ? "captain" : pick == 0 ? "hunter" : "guard";
            case "TALK_TO" -> "librarian";
            case "VISIT" -> "cartographer";
            default -> byItem(t, pick);
        };
    }

    public static String heading(String type) {
        return switch (type) {
            case "KILL" -> "Wanted";
            case "TALK_TO" -> "Message";
            case "VISIT" -> "Scouting";
            case "DELIVER" -> "Delivery";
            default -> "Help wanted";
        };
    }

    private static String byItem(String t, int pick) {
        for (String[] trade : TRADES) {
            for (int k = 1; k < trade.length; k++) {
                if (t.contains(trade[k])) return trade[0];
            }
        }
        return pick == 0 ? "farmer" : pick == 1 ? "mason" : "toolsmith";
    }

    private static boolean any(String t, String[] words) {
        for (String w : words) if (t.contains(w)) return true;
        return false;
    }
}
