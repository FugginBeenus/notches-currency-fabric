package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Version-compat facade for loading world-save state ({@link PersistentState}).
 *
 * <p>Companion to {@link StackData}: that one covers data on an item, this one covers data on the
 * world. 1.20.1 loads a state with {@code getOrCreate(reader, constructor, key)}; from 1.20.5 the
 * three arguments collapse into a {@code PersistentState.Type} and the reader gains a registry
 * lookup. The mod's twelve state classes don't need registries to deserialize, so the lookup is
 * accepted and dropped here rather than threaded through all of them.
 *
 * <p>Note the sibling change this facade can't hide: {@code PersistentState.writeNbt} itself gained a
 * registry-lookup parameter, and an override signature has to live on the subclass, so each state
 * class carries a small version-gated signature of its own.
 */
public final class StateData {

    private StateData() {}

    /** Load (or create) a world-save state under {@code key}. */
    public static <T extends PersistentState> T getOrCreate(
            PersistentStateManager manager, Supplier<T> constructor, Function<NbtCompound, T> reader, String key) {
        //? if >=1.21 {
        /*return manager.getOrCreate(
                new PersistentState.Type<>(constructor, (nbt, registries) -> reader.apply(nbt), null), key);
        *///?} else {
        return manager.getOrCreate(reader, constructor, key);
        //?}
    }
}
