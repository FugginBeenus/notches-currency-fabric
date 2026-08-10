package net.fugginbeenus.notchcurrency.compat;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
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
        return nbt != null && Nbt.hasUuid(nbt, key);
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
        return (nbt != null && Nbt.hasUuid(nbt, key)) ? Nbt.getUuid(nbt, key) : null;
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

    /**
     * An item written so that a different Minecraft version can still read it, for share codes and
     * presets.
     *
     * <p>Item stacks moved from tags to components at 1.21, and the two shapes do not read each
     * other. Worse, they half read each other: an older stack handed to the newer codec keeps its id
     * and silently loses its count, while a newer stack handed to the older reader comes back empty.
     * Both go quiet about it. So the item and the count are written plainly alongside the native
     * form, and the native form is only trusted when it agrees about which item this is.
     */
    public static CompoundTag writePortableStack(ItemStack stack) {
        CompoundTag out = new CompoundTag();
        net.minecraft.resources.ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        out.putString("Item", id.toString());
        out.putInt("Num", stack.getCount());
        out.put("Native", writeStack(stack));

        // Enchantments by name, because the component that holds them has its own shape per era and
        // does not survive a crossing either: 1.20.1, 1.21.1 and 1.21.11 upwards all disagree. Names
        // and levels are the part worth keeping, and they have not changed.
        java.util.Map<net.minecraft.world.item.enchantment.Enchantment, Integer> enchantments =
                Ench.get(stack);
        if (!enchantments.isEmpty()) {
            CompoundTag levels = new CompoundTag();
            for (var entry : enchantments.entrySet()) {
                net.minecraft.resources.ResourceLocation key = Ench.idOf(entry.getKey());
                if (key != null) levels.putInt(key.toString(), entry.getValue());
            }
            out.put("Ench", levels);
        }
        return out;
    }

    /** Reverses {@link #writePortableStack}, and still reads a bare stack from before that existed. */
    public static ItemStack readPortableStack(CompoundTag nbt) {
        if (!nbt.contains("Item")) return readStack(nbt);

        // One line: the lookup is rewritten by pattern on the newer versions, and the pattern wants
        // the registry and the call together.
        net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(Reg.parse(nbt.getString("Item")));
        if (item == null || item == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
        int count = Math.max(1, nbt.getInt("Num"));

        // Enchantments and the rest ride in the native block, which is worth having whenever this
        // version can actually read it. Anything it says about the count is not: that is the field
        // the two shapes disagree on.
        ItemStack out = null;
        try {
            ItemStack full = readStack(nbt.getCompound("Native"));
            if (!full.isEmpty() && full.getItem() == item) {
                full.setCount(count);
                out = full;
            }
        } catch (Exception ignored) {
            // Written by the other side of the 1.21 line. The plain fields below carry it instead.
        }
        if (out == null) out = new ItemStack(item, count);
        applyPortableEnchantments(nbt, out);
        return out;
    }

    /** Puts back the enchantments recorded by name, for a stack whose native block did not carry them. */
    private static void applyPortableEnchantments(CompoundTag nbt, ItemStack stack) {
        if (!nbt.contains("Ench") || !Ench.get(stack).isEmpty()) return;
        CompoundTag levels = nbt.getCompound("Ench");
        java.util.Map<net.minecraft.world.item.enchantment.Enchantment, Integer> found =
                new java.util.LinkedHashMap<>();
        for (String key : levels.getAllKeys()) {
            net.minecraft.world.item.enchantment.Enchantment ench = Ench.byId(Reg.parse(key));
            // An enchantment the reading version does not have is simply skipped.
            if (ench != null) found.put(ench, levels.getInt(key));
        }
        if (!found.isEmpty()) Ench.set(found, stack);
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
