package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

/**
 * Reading and writing a UUID in NBT, on every version.
 *
 * <p>1.21.11 removed {@code CompoundTag.getUUID} and {@code putUUID} and left nothing in their place.
 * That matters more than a missing convenience method usually would, because these bytes travel: an
 * NPC's owner, a shop id and a faction id all go into share codes, presets and the pick-up item, and
 * those are meant to move between servers running different versions of the game.
 *
 * <p>So the format is pinned rather than reinvented. Vanilla always stored a UUID as an array of four
 * ints, and that is what both branches below write, which keeps a code made on 1.20.1 readable on
 * 1.21.11 and the other way around. Anything else here would quietly break that promise, and it would
 * break it in a way that only shows up when somebody pastes a code between two servers.
 */
public final class Nbt {

    private Nbt() {}

    public static void putUuid(CompoundTag tag, String key, UUID value) {
        //? if >=1.21.11 {
        /*tag.putIntArray(key, net.minecraft.core.UUIDUtil.uuidToIntArray(value));
        *///?} else {
        tag.putUUID(key, value);
        //?}
    }

    public static UUID getUuid(CompoundTag tag, String key) {
        //? if >=1.21.11 {
        /*return net.minecraft.core.UUIDUtil.uuidFromIntArray(tag.getIntArray(key).orElse(new int[4]));
        *///?} else {
        return tag.getUUID(key);
        //?}
    }

    /** True when the key holds a UUID this can read back. */
    public static boolean hasUuid(CompoundTag tag, String key) {
        //? if >=1.21.11 {
        /*return tag.getIntArray(key).map(a -> a.length == 4).orElse(false);
        *///?} else {
        return tag.hasUUID(key);
        //?}
    }
}
