package net.fugginbeenus.notchcurrency.client.particle;

import net.fugginbeenus.notchcurrency.npc.particle.ParticleLayer;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class ParticleCatalog {

    private ParticleCatalog() {}

    public static final String[] GROUPS = {"All", "Notch", "Fire", "Magic", "Nature", "Water", "Sky", "Combat"};

    public record Entry(String id, String name, String group, boolean wanders) {}

    private static final Map<String, String> GROUP_OF = Map.ofEntries(
            Map.entry("flame", "Fire"), Map.entry("soul_fire_flame", "Fire"), Map.entry("small_flame", "Fire"),
            Map.entry("lava", "Fire"), Map.entry("smoke", "Fire"), Map.entry("large_smoke", "Fire"),
            Map.entry("campfire_cosy_smoke", "Fire"), Map.entry("campfire_signal_smoke", "Fire"),
            Map.entry("ash", "Fire"), Map.entry("white_ash", "Fire"), Map.entry("dripping_lava", "Fire"),
            Map.entry("falling_lava", "Fire"), Map.entry("landing_lava", "Fire"),

            Map.entry("enchant", "Magic"), Map.entry("witch", "Magic"), Map.entry("dragon_breath", "Magic"),
            Map.entry("end_rod", "Magic"), Map.entry("portal", "Magic"), Map.entry("reverse_portal", "Magic"),
            Map.entry("effect", "Magic"), Map.entry("instant_effect", "Magic"), Map.entry("entity_effect", "Magic"),
            Map.entry("ambient_entity_effect", "Magic"), Map.entry("soul", "Magic"), Map.entry("sculk_soul", "Magic"),
            Map.entry("glow", "Magic"), Map.entry("electric_spark", "Magic"), Map.entry("scrape", "Magic"),
            Map.entry("wax_on", "Magic"), Map.entry("wax_off", "Magic"), Map.entry("sonic_boom", "Magic"),
            Map.entry("totem_of_undying", "Magic"), Map.entry("dust", "Magic"),
            Map.entry("dust_color_transition", "Magic"), Map.entry("note", "Magic"), Map.entry("nautilus", "Magic"),
            Map.entry("trial_spawner_detection", "Magic"), Map.entry("vault_connection", "Magic"),
            Map.entry("ominous_spawning", "Magic"), Map.entry("infested", "Magic"),

            Map.entry("happy_villager", "Nature"), Map.entry("composter", "Nature"),
            Map.entry("spore_blossom_air", "Nature"), Map.entry("falling_spore_blossom", "Nature"),
            Map.entry("crimson_spore", "Nature"), Map.entry("warped_spore", "Nature"),
            Map.entry("cherry_leaves", "Nature"), Map.entry("pale_oak_leaves", "Nature"),
            Map.entry("mycelium", "Nature"), Map.entry("dripping_honey", "Nature"),
            Map.entry("falling_honey", "Nature"), Map.entry("landing_honey", "Nature"),
            Map.entry("falling_nectar", "Nature"), Map.entry("egg_crack", "Nature"),
            Map.entry("dripping_obsidian_tear", "Nature"), Map.entry("falling_obsidian_tear", "Nature"),
            Map.entry("landing_obsidian_tear", "Nature"), Map.entry("dripping_dripstone_lava", "Nature"),
            Map.entry("falling_dripstone_lava", "Nature"), Map.entry("dripping_dripstone_water", "Nature"),
            Map.entry("falling_dripstone_water", "Nature"), Map.entry("firefly", "Nature"),

            Map.entry("bubble", "Water"), Map.entry("bubble_pop", "Water"), Map.entry("bubble_column_up", "Water"),
            Map.entry("underwater", "Water"), Map.entry("fishing", "Water"), Map.entry("current_down", "Water"),
            Map.entry("dripping_water", "Water"), Map.entry("falling_water", "Water"), Map.entry("splash", "Water"),
            Map.entry("dolphin", "Water"), Map.entry("rain", "Water"), Map.entry("squid_ink", "Water"),
            Map.entry("glow_squid_ink", "Water"),

            Map.entry("snowflake", "Sky"), Map.entry("cloud", "Sky"), Map.entry("poof", "Sky"),
            Map.entry("explosion", "Sky"), Map.entry("explosion_emitter", "Sky"), Map.entry("firework", "Sky"),
            Map.entry("flash", "Sky"), Map.entry("gust", "Sky"), Map.entry("small_gust", "Sky"),
            Map.entry("gust_emitter_large", "Sky"), Map.entry("gust_emitter_small", "Sky"),
            Map.entry("dust_plume", "Sky"), Map.entry("dust_pillar", "Sky"), Map.entry("sneeze", "Sky"),

            Map.entry("crit", "Combat"), Map.entry("enchanted_hit", "Combat"), Map.entry("damage_indicator", "Combat"),
            Map.entry("sweep_attack", "Combat"), Map.entry("angry_villager", "Combat"), Map.entry("heart", "Combat"),
            Map.entry("item_snowball", "Combat"), Map.entry("item_slime", "Combat"), Map.entry("item_cobweb", "Combat"),
            Map.entry("raid_omen", "Combat"), Map.entry("trial_omen", "Combat")
    );

    private static final Map<String, String> NICE = Map.ofEntries(
            Map.entry("notchcurrency:glyph", "Glyphs (galactic letters)"),
            Map.entry("notchcurrency:portal", "Portal swirl"),
            Map.entry("notchcurrency:note", "Music note"),
            Map.entry("notchcurrency:spark", "Sparkle"),
            Map.entry("notchcurrency:spell", "Spell puff"),
            Map.entry("notchcurrency:glow", "Soft glow"),
            Map.entry("notchcurrency:dot", "Plain dot"),
            Map.entry("minecraft:end_rod", "Sparkle (drifts)"),
            Map.entry("minecraft:crit", "Crit sparks"),
            Map.entry("minecraft:poof", "Puff"),
            Map.entry("minecraft:effect", "Potion swirl"),
            Map.entry("minecraft:instant_effect", "Potion swirl (bright)"),
            Map.entry("minecraft:entity_effect", "Potion swirl (colour)"),
            Map.entry("minecraft:ambient_entity_effect", "Potion swirl (faint)"),
            Map.entry("minecraft:enchant", "Glyphs (dive to a table)"),
            Map.entry("minecraft:witch", "Witch spell"),
            Map.entry("minecraft:soul", "Soul wisp"),
            Map.entry("minecraft:sculk_soul", "Sculk soul"),
            Map.entry("minecraft:dust", "Coloured dust"),
            Map.entry("minecraft:dust_color_transition", "Fading dust"),
            Map.entry("minecraft:happy_villager", "Green sparkle"),
            Map.entry("minecraft:angry_villager", "Angry cloud"),
            Map.entry("minecraft:totem_of_undying", "Totem burst"),
            Map.entry("minecraft:campfire_cosy_smoke", "Cosy smoke"),
            Map.entry("minecraft:campfire_signal_smoke", "Tall smoke")
    );

    public static String nameOf(String id) {
        String nice = NICE.get(id);
        if (nice != null) return nice;
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        if (path.startsWith("custom_")) {
            try {
                int slot = Integer.parseInt(path.substring(7));
                String own = net.fugginbeenus.notchcurrency.client.npctexture.NpcTextureLoader.nameOf(slot);
                if (own != null) return own.replace('_', ' ') + " (custom " + slot + ")";
            } catch (NumberFormatException ignored) {
            }
            return "Custom " + path.substring(7) + " (empty)";
        }
        String words = path.replace('_', ' ');
        return Character.toUpperCase(words.charAt(0)) + words.substring(1);
    }

    public static String groupOf(String id) {
        if (id.startsWith("notchcurrency:")) return "Notch";
        String path = id.contains(":") ? id.substring(id.indexOf(':') + 1) : id;
        return GROUP_OF.getOrDefault(path, "Misc");
    }

    public static List<Entry> all() {
        List<Entry> out = new ArrayList<>();
        for (ResourceLocation key : BuiltInRegistries.PARTICLE_TYPE.keySet()) {
            String id = key.toString();
            ParticleType<?> type = BuiltInRegistries.PARTICLE_TYPE.get(key);
            boolean usable = type instanceof SimpleParticleType
                    || id.equals(ParticleLayer.DUST) || id.equals(ParticleLayer.DUST_FADE);
            if (!usable) continue;
            out.add(new Entry(id, nameOf(id), groupOf(id), ParticleLayer.goesItsOwnWay(id)));
        }
        out.sort(Comparator
                .comparing((Entry e) -> e.group().equals("Notch") ? 0 : 1)
                .thenComparing((Entry e) -> e.id().contains(":custom_") ? 1 : 0)
                .thenComparing(Entry::name, String.CASE_INSENSITIVE_ORDER));
        return out;
    }
}
