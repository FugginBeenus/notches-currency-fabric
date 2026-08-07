package net.fugginbeenus.notchcurrency.npc;

//? if <1.21.11 {
import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
//?}
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
//? if <1.21.11 {
import java.util.Map;
import java.util.TreeMap;
//?}

/**
 * Accessory slots on an NPC, where an accessory mod exists to provide them.
 *
 * <p>Trinkets stopped at 1.21.1 and its successor Accessories at 1.21.8, so from 1.21.11 there is
 * nothing to integrate with. That is not a choice made here: no such mod is published for those
 * versions, so there is no API to compile against, let alone slots to show.
 *
 * <p>The methods still exist there and simply answer "no slots", which is exactly what they already
 * answered on any world where the player had not installed Trinkets. The equipment screen needs no
 * knowledge of the difference, and neither does anything else.
 */
public final class NpcTrinkets {

    public record Spec(String group, String slot, int index) {}

    private NpcTrinkets() {}

    public static List<Spec> slotSpecs(EntityType<?> type, int max) {
        //? if >=1.21.11 {
        /*return List.of();
        *///?} else {
        List<Spec> specs = new ArrayList<>();
        Map<String, SlotGroup> groups = new TreeMap<>(TrinketsApi.getEntitySlots(type));
        for (Map.Entry<String, SlotGroup> group : groups.entrySet()) {
            Map<String, SlotType> slots = new TreeMap<>(group.getValue().getSlots());
            for (Map.Entry<String, SlotType> slot : slots.entrySet()) {
                for (int i = 0; i < slot.getValue().getAmount(); i++) {
                    if (specs.size() >= max) return specs;
                    specs.add(new Spec(group.getKey(), slot.getKey(), i));
                }
            }
        }
        return specs;
        //?}
    }

    @Nullable
    public static Container inventoryFor(NotchNpcEntity npc, String group, String slot) {
        //? if >=1.21.11 {
        /*return null;
        *///?} else {
        return TrinketsApi.getTrinketComponent(npc)
                .map(component -> {
                    Map<String, TrinketInventory> inGroup = component.getInventory().get(group);
                    return inGroup == null ? null : (Container) inGroup.get(slot);
                })
                .orElse(null);
        //?}
    }
}
