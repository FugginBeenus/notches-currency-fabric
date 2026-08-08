package net.fugginbeenus.notchcurrency.compat;

import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;

public final class StateData {

    private StateData() {}

    public static <T extends SavedData & NbtState> T getOrCreate(
            DimensionDataStorage manager, Supplier<T> constructor, Function<CompoundTag, T> reader, String key) {
        // From 1.21.11 SavedData is described by a codec rather than a save/load pair, so the tag work
        // the state classes already do is wrapped as one: read with their loader, write with writeNbt.
        // The stored bytes come out identical to the older versions', since it is the same tag.
        //
        // The data-fix type is not optional the way the old factory's was, and it is dereferenced
        // without a null check on load. Command storage is the vanilla saved-data type meant for
        // free-form NBT, so it is the one least likely to try reshaping keys it has never seen.
        //
        // 26.1 changed one thing further: the type names itself with an Identifier, not a bare string.
        //? if >=26.1 {
        /*return manager.computeIfAbsent(new net.minecraft.world.level.saveddata.SavedDataType<>(
                Reg.id(key), constructor,
                CompoundTag.CODEC.xmap(reader, state -> state.writeNbt(new CompoundTag())),
                net.minecraft.util.datafix.DataFixTypes.SAVED_DATA_COMMAND_STORAGE));
        *///?} elif >=1.21.11 {
        /*return manager.computeIfAbsent(new net.minecraft.world.level.saveddata.SavedDataType<>(
                key, constructor,
                CompoundTag.CODEC.xmap(reader, state -> state.writeNbt(new CompoundTag())),
                net.minecraft.util.datafix.DataFixTypes.SAVED_DATA_COMMAND_STORAGE));
        *///?} elif >=1.21 {
        /*return manager.computeIfAbsent(
                new SavedData.Factory<>(constructor, (nbt, registries) -> reader.apply(nbt), null), key);
        *///?} else {
        return manager.computeIfAbsent(reader, constructor, key);
        //?}
    }
}
