package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.nbt.CompoundTag;

import java.util.UUID;

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
    /*
    public static int[] intArray(CompoundTag tag, String key) {
        return tag.getIntArray(key).orElse(new int[0]);
    }

    public static int[] intArray(net.minecraft.nbt.ListTag list, int index) {
        return list.getIntArray(index).orElse(new int[0]);
    }

    private static final com.mojang.serialization.Codec<net.minecraft.nbt.Tag> TAG_CODEC =
            com.mojang.serialization.Codec.PASSTHROUGH.xmap(
                    dynamic -> dynamic.convert(net.minecraft.nbt.NbtOps.INSTANCE).getValue(),
                    tag -> new com.mojang.serialization.Dynamic<>(net.minecraft.nbt.NbtOps.INSTANCE, tag));

    public static void copyInto(CompoundTag source, net.minecraft.world.level.storage.ValueOutput out) {
        for (String key : source.keySet()) {
            out.store(key, TAG_CODEC, source.get(key));
        }
    }

    public static CompoundTag readAll(net.minecraft.world.level.storage.ValueInput in) {
        CompoundTag tag = new CompoundTag();
        for (String key : in.keys()) {
            in.read(key, TAG_CODEC).ifPresent(value -> tag.put(key, value));
        }
        return tag;
    }
    *///?}
}
