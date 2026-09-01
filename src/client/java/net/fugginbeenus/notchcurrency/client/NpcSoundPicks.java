package net.fugginbeenus.notchcurrency.client;

import java.util.List;

public final class NpcSoundPicks {

    private NpcSoundPicks() {}

    public record Pick(String name, String id) {}

    public static final List<Pick> ALL = List.of(
            new Pick("No sound", ""),
            new Pick("Villager hmm", "minecraft:entity.villager.ambient"),
            new Pick("Villager yes", "minecraft:entity.villager.yes"),
            new Pick("Villager no", "minecraft:entity.villager.no"),
            new Pick("Villager trade", "minecraft:entity.villager.trade"),
            new Pick("Trader hmm", "minecraft:entity.wandering_trader.ambient"),
            new Pick("Witch cackle", "minecraft:entity.witch.ambient"),
            new Pick("Pillager grunt", "minecraft:entity.pillager.ambient"),
            new Pick("Zombie groan", "minecraft:entity.zombie.ambient"),
            new Pick("Cat purr", "minecraft:entity.cat.purr"),
            new Pick("Wolf whine", "minecraft:entity.wolf.whine"),
            new Pick("Level up", "minecraft:entity.player.levelup"),
            new Pick("Ding", "minecraft:entity.experience_orb.pickup"),
            new Pick("Bell", "minecraft:block.note_block.bell"),
            new Pick("Click", "minecraft:ui.button.click")
    );

    public static String nameFor(String id) {
        String want = id == null ? "" : id.trim();
        for (Pick p : ALL) {
            if (p.id().equals(want)) return p.name();
        }
        return want.isEmpty() ? "No sound" : "Custom";
    }

    public static String next(String id) {
        String want = id == null ? "" : id.trim();
        for (int i = 0; i < ALL.size(); i++) {
            if (ALL.get(i).id().equals(want)) {
                return ALL.get((i + 1) % ALL.size()).id();
            }
        }
        return ALL.get(1).id();
    }
}
