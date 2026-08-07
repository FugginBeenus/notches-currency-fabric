package net.fugginbeenus.notchcurrency.compat;

import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public final class StateData {

    private StateData() {}

    public static <T extends SavedData> T getOrCreate(
            DimensionDataStorage manager, Supplier<T> constructor, Function<CompoundTag, T> reader, String key) {
        //? if >=1.21 {
        /*return manager.getOrCreate(
                new SavedData.Type<>(constructor, (nbt, registries) -> reader.apply(nbt), null), key);
        *///?} else {
        return manager.computeIfAbsent(reader, constructor, key);
        //?}
    }
}
