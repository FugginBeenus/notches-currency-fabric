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

/**
 * The Trinkets side of the NPC equip screen, SOFT integration: this class references Trinkets
 * types, so callers must gate every call behind {@code isModLoaded("trinkets")} and let the JVM
 * load it lazily (same pattern as the Waystones fee handler).
 *
 * <p>Which slots an NPC has comes from Trinkets' data ({@code data/trinkets/entities/}, where the
 * mod opts the NPC in); that data is synced, so {@link #slotSpecs} answers identically on both
 * sides: the screen handler relies on that to build matching slot lists.
 */
public final class NpcTrinkets {

    /** One concrete trinket slot: its group/slot key, the index within that slot's capacity. */
    public record Spec(String group, String slot, int index) {}

    private NpcTrinkets() {}

    /** The NPC type's trinket slots, deterministically ordered, capped to what the screen can show. */
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

    /** The live trinket inventory backing a slot on this NPC, or null if the component is absent. */
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
