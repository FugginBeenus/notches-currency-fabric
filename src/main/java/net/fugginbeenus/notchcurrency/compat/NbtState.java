package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.nbt.CompoundTag;

/**
 * The NBT half of a saved state, named the same on every version.
 *
 * <p>Vanilla's own name for this kept moving. Through 1.21.1 it was {@code SavedData.save}, which took
 * a registry lookup from 1.21 on; in 1.21.11 {@code SavedData} went codec-based and stopped declaring
 * a save method at all. Rather than have thirteen state classes each branch their signature three
 * ways, they all implement this instead, and only the thin bridge below them changes per version.
 */
public interface NbtState {

    /** Writes this state into the tag and returns it. */
    CompoundTag writeNbt(CompoundTag nbt);
}
