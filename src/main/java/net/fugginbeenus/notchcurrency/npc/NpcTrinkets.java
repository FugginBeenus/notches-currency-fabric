package net.fugginbeenus.notchcurrency.npc;

import dev.emi.trinkets.api.SlotGroup;
import dev.emi.trinkets.api.SlotType;
import dev.emi.trinkets.api.TrinketInventory;
import dev.emi.trinkets.api.TrinketsApi;
import net.fugginbeenus.notchcurrency.entity.NotchNpcEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.inventory.Inventory;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public final class NpcTrinkets {

    public record Spec(String group, String slot, int index) {}

    private NpcTrinkets() {}

    public static List<Spec> slotSpecs(EntityType<?> type, int max) {
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
    }

    @Nullable
    public static Inventory inventoryFor(NotchNpcEntity npc, String group, String slot) {
        return TrinketsApi.getTrinketComponent(npc)
                .map(component -> {
                    Map<String, TrinketInventory> inGroup = component.getInventory().get(group);
                    return inGroup == null ? null : (Inventory) inGroup.get(slot);
                })
                .orElse(null);
    }
}
