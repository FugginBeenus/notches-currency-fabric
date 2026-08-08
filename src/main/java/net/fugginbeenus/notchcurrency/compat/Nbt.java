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

    //? if <1.21.11 {
    public static int[] intArray(CompoundTag tag, String key) {
        return tag.getIntArray(key);
    }

    public static int[] intArray(net.minecraft.nbt.ListTag list, int index) {
        return list.getIntArray(index);
    }
    //?}

    //? if >=1.21.11 {
    /*// Int arrays went Optional with the rest of the getters. Not a blanket rewrite like the others,
    // because the UUID helpers above already unwrap the Optional themselves and would end up
    // unwrapping it twice.
    public static int[] intArray(CompoundTag tag, String key) {
        return tag.getIntArray(key).orElse(new int[0]);
    }

    public static int[] intArray(net.minecraft.nbt.ListTag list, int index) {
        return list.getIntArray(index).orElse(new int[0]);
    }

    // Entities stopped being handed a CompoundTag to fill in and now get a ValueOutput view. The
    // NPC's own read and write still work in tags, because presets, share codes and the pick-up item
    // all reuse the same two methods, so the pair below carries a whole tag across that boundary.
    //
    // Key by key, at the top level, rather than nesting the lot under one name: an NPC saved by an
    // older version has its keys sitting directly on the entity, and burying them would read back as
    // a factory-fresh NPC on a world that had simply been upgraded.
    //
    // There is no codec for "any tag" in vanilla, so this makes one: PASSTHROUGH keeps the value as
    // a Dynamic, and converting it to NBT ops on the way out gets the original tag back untouched.
    private static final com.mojang.serialization.Codec<net.minecraft.nbt.Tag> TAG_CODEC =
            com.mojang.serialization.Codec.PASSTHROUGH.xmap(
                    dynamic -> dynamic.convert(net.minecraft.nbt.NbtOps.INSTANCE).getValue(),
                    tag -> new com.mojang.serialization.Dynamic<>(net.minecraft.nbt.NbtOps.INSTANCE, tag));

    public static void copyInto(CompoundTag source, net.minecraft.world.level.storage.ValueOutput out) {
        for (String key : source.keySet()) {
            out.store(key, TAG_CODEC, source.get(key));
        }
    }

    // Everything the view holds, as a tag. Vanilla's own entity keys ride along; the NPC ignores them.
    public static CompoundTag readAll(net.minecraft.world.level.storage.ValueInput in) {
        CompoundTag tag = new CompoundTag();
        for (String key : in.keys()) {
            in.read(key, TAG_CODEC).ifPresent(value -> tag.put(key, value));
        }
        return tag;
    }
    *///?}
}
