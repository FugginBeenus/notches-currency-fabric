package net.fugginbeenus.notchcurrency.compat;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class StackData {

    private StackData() {}

    // ---- read helpers (internal) ----

    @Nullable
    private static CompoundTag read(ItemStack stack) {
        //? if >=1.21 {
        /*net.minecraft.world.item.component.CustomData held =
                stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return held == null ? null : held.copyTag();
        *///?} else {
        return stack.getTag();
        //?}
    }

    private static void mutate(ItemStack stack, java.util.function.Consumer<CompoundTag> action) {
        //? if >=1.21 {
        /*CompoundTag data = stack.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        action.accept(data);
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(data));
        *///?} else {
        action.accept(stack.getOrCreateTag());
        //?}
    }

    // ---- presence ----

    public static boolean hasData(ItemStack stack) {
        //? if >=1.21 {
        /*return stack.has(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        *///?} else {
        return stack.hasTag();
        //?}
    }

    public static boolean has(ItemStack stack, String key) {
        CompoundTag nbt = read(stack);
        return nbt != null && nbt.contains(key);
    }

    public static boolean hasUuid(ItemStack stack, String key) {
        CompoundTag nbt = read(stack);
        return nbt != null && nbt.hasUUID(key);
    }

    public static void remove(ItemStack stack, String key) {
        if (!hasData(stack)) return;
        mutate(stack, data -> data.remove(key));
    }

    // ---- typed getters (vanilla defaults for a missing key) ----

    public static int getInt(ItemStack stack, String key) {
        CompoundTag nbt = read(stack);
        return nbt == null ? 0 : nbt.getInt(key);
    }

    public static long getLong(ItemStack stack, String key) {
        CompoundTag nbt = read(stack);
        return nbt == null ? 0L : nbt.getLong(key);
    }

    public static double getDouble(ItemStack stack, String key) {
        CompoundTag nbt = read(stack);
        return nbt == null ? 0.0 : nbt.getDouble(key);
    }

    public static boolean getBoolean(ItemStack stack, String key) {
        CompoundTag nbt = read(stack);
        return nbt != null && nbt.getBoolean(key);
    }

    public static String getString(ItemStack stack, String key) {
        CompoundTag nbt = read(stack);
        return nbt == null ? "" : nbt.getString(key);
    }

    @Nullable
    public static UUID getUuid(ItemStack stack, String key) {
        CompoundTag nbt = read(stack);
        return (nbt != null && nbt.hasUUID(key)) ? nbt.getUUID(key) : null;
    }

    public static CompoundTag getCompound(ItemStack stack, String key) {
        CompoundTag nbt = read(stack);
        return nbt == null ? new CompoundTag() : nbt.getCompound(key).copy();
    }

    // ---- typed setters (full read-modify-write; safe on copy-on-write component storage) ----

    public static void putInt(ItemStack stack, String key, int value) {
        mutate(stack, data -> data.putInt(key, value));
    }

    public static void putLong(ItemStack stack, String key, long value) {
        mutate(stack, data -> data.putLong(key, value));
    }

    public static void putDouble(ItemStack stack, String key, double value) {
        mutate(stack, data -> data.putDouble(key, value));
    }

    public static void putBoolean(ItemStack stack, String key, boolean value) {
        mutate(stack, data -> data.putBoolean(key, value));
    }

    public static void putString(ItemStack stack, String key, String value) {
        mutate(stack, data -> data.putString(key, value));
    }

    public static void putUuid(ItemStack stack, String key, UUID value) {
        mutate(stack, data -> net.fugginbeenus.notchcurrency.compat.Nbt.putUuid(data, key, value));
    }

    public static void putCompound(ItemStack stack, String key, CompoundTag value) {
        mutate(stack, data -> data.put(key, value));
    }

    public static boolean canCombine(ItemStack a, ItemStack b) {
        //? if >=1.21 {
        /*return ItemStack.isSameItemSameComponents(a, b);
        *///?} else {
        return ItemStack.isSameItemSameTags(a, b);
        //?}
    }

    // ---- bulk read (for carrier stacks whose readers do many typed lookups) ----

    public static CompoundTag getData(ItemStack stack) {
        CompoundTag nbt = read(stack);
        return nbt == null ? new CompoundTag() : nbt.copy();
    }

    // ---- batch edit (for carrier stacks that set several keys at once) ----

    public static CompoundTag editData(ItemStack stack) {
        //? if >=1.21 {
        /*return stack.getOrDefault(
                net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag();
        *///?} else {
        return stack.getOrCreateTag();
        //?}
    }

    public static void commitData(ItemStack stack, CompoundTag data) {
        //? if >=1.21 {
        /*stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.of(data));
        *///?} else {
        stack.setTag(data);
        //?}
    }

    public static void clearData(ItemStack stack) {
        //? if >=1.21 {
        /*stack.remove(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        *///?} else {
        stack.setTag(null);
        //?}
    }

    // ---- whole-stack persistence (for world-save data classes) ----

    public static CompoundTag writeStack(ItemStack stack) {
        //? if >=1.21 {
        /*return (CompoundTag) ItemStack.OPTIONAL_CODEC
                .encodeStart(RegistryAccess.get().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), stack)
                .getOrThrow();
        *///?} else {
        return stack.save(new CompoundTag());
        //?}
    }

    public static ItemStack readStack(CompoundTag nbt) {
        //? if >=1.21 {
        /*return ItemStack.OPTIONAL_CODEC
                .parse(RegistryAccess.get().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), nbt)
                .result()
                .orElse(ItemStack.EMPTY);
        *///?} else {
        return ItemStack.of(nbt);
        //?}
    }
}
