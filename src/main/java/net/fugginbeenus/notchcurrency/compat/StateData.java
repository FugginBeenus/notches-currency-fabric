package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;

import java.util.function.Function;
import java.util.function.Supplier;

public final class StateData {

    private StateData() {}

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
