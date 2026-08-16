package net.fugginbeenus.notchcurrency.compat;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

public final class StackData {

    private StackData() {}

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

    public static CompoundTag getData(ItemStack stack) {
        CompoundTag nbt = read(stack);
        return nbt == null ? new CompoundTag() : nbt.copy();
    }


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

    public static CompoundTag writeStack(ItemStack stack) {
        //? if >=1.21 {
        /*return (CompoundTag) ItemStack.OPTIONAL_CODEC
                .encodeStart(RegistryAccess.get().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE), stack)
                .getOrThrow();
        *///?} else {
        return stack.save(new CompoundTag());
        //?}
    }

    public static CompoundTag writePortableStack(ItemStack stack) {
        CompoundTag out = new CompoundTag();
        net.minecraft.resources.ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        out.putString("Item", id.toString());
        out.putInt("Num", stack.getCount());
        out.put("Native", writeStack(stack));

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

        if (stack.isDamaged()) out.putInt("Dmg", stack.getDamageValue());
        String name = customName(stack);
        if (!name.isEmpty()) out.putString("CustomName", name);
        int dye = dyedColour(stack);
        if (dye != NO_DYE) out.putInt("Dye", dye);
        String[] trim = armourTrim(stack);
        if (trim != null) {
            out.putString("TrimMat", trim[0]);
            out.putString("TrimPat", trim[1]);
        }
        return out;
    }

    @Nullable
    private static String[] armourTrim(ItemStack stack) {
        try {
            //? if >=1.21.11 {
            /*net.minecraft.world.item.equipment.trim.ArmorTrim trim =
                    stack.get(net.minecraft.core.component.DataComponents.TRIM);
            if (trim == null) return null;
            return new String[]{holderId(trim.material()), holderId(trim.pattern())};
            *///?} elif >=1.21 {
            /*net.minecraft.world.item.armortrim.ArmorTrim trim =
                    stack.get(net.minecraft.core.component.DataComponents.TRIM);
            if (trim == null) return null;
            return new String[]{holderId(trim.material()), holderId(trim.pattern())};
            *///?} else {
            java.util.Optional<net.minecraft.world.item.armortrim.ArmorTrim> trim =
                    net.minecraft.world.item.armortrim.ArmorTrim.getTrim(RegistryAccess.get(), stack);
            if (trim.isEmpty()) return null;
            return new String[]{holderId(trim.get().material()), holderId(trim.get().pattern())};
            //?}
        } catch (Exception ignored) {
            return null;
        }
    }

    private static String holderId(net.minecraft.core.Holder<?> holder) {
        return holder.unwrapKey().map(key -> key.location().toString()).orElse("");
    }

    private static final int NO_DYE = Integer.MIN_VALUE;

    private static String customName(ItemStack stack) {
        //? if >=1.21 {
        /*net.minecraft.network.chat.Component name =
                stack.get(net.minecraft.core.component.DataComponents.CUSTOM_NAME);
        return name == null ? "" : name.getString();
        *///?} else {
        return stack.hasCustomHoverName() ? stack.getHoverName().getString() : "";
        //?}
    }

    private static void setCustomName(ItemStack stack, String name) {
        //? if >=1.21 {
        /*stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,
                net.minecraft.network.chat.Component.literal(name));
        *///?} else {
        stack.setHoverName(net.minecraft.network.chat.Component.literal(name));
        //?}
    }

    private static int dyedColour(ItemStack stack) {
        //? if >=1.21 {
        /*net.minecraft.world.item.component.DyedItemColor dyed =
                stack.get(net.minecraft.core.component.DataComponents.DYED_COLOR);
        return dyed == null ? NO_DYE : dyed.rgb();
        *///?} else {
        return stack.getItem() instanceof net.minecraft.world.item.DyeableLeatherItem dyeable
                && dyeable.hasCustomColor(stack)
                ? dyeable.getColor(stack) : NO_DYE;
        //?}
    }

    private static void setDyedColour(ItemStack stack, int rgb) {
        //? if >=1.21.11 {
        /*stack.set(net.minecraft.core.component.DataComponents.DYED_COLOR,
                new net.minecraft.world.item.component.DyedItemColor(rgb));
        *///?} elif >=1.21 {
        /*
        stack.set(net.minecraft.core.component.DataComponents.DYED_COLOR,
                new net.minecraft.world.item.component.DyedItemColor(rgb, true));
        *///?} else {
        if (stack.getItem() instanceof net.minecraft.world.item.DyeableLeatherItem dyeable) {
            dyeable.setColor(stack, rgb);
        }
        //?}
    }

    public static ItemStack readPortableStack(CompoundTag nbt) {
        if (!nbt.contains("Item")) return readStack(nbt);
        net.minecraft.world.item.Item item = BuiltInRegistries.ITEM.get(Reg.parse(nbt.getString("Item")));
        if (item == null || item == net.minecraft.world.item.Items.AIR) return ItemStack.EMPTY;
        int count = Math.max(1, nbt.getInt("Num"));
        ItemStack out = null;
        try {
            ItemStack full = readStack(nbt.getCompound("Native"));
            if (!full.isEmpty() && full.getItem() == item) {
                full.setCount(count);
                out = full;
            }
        } catch (Exception ignored) {
        }
        if (out == null) out = new ItemStack(item, count);
        applyPortableEnchantments(nbt, out);
        applyPortableExtras(nbt, out);
        return out;
    }

    private static void applyPortableExtras(CompoundTag nbt, ItemStack stack) {
        if (nbt.contains("Dmg")) {
            try {
                stack.setDamageValue(nbt.getInt("Dmg"));
            } catch (Exception ignored) {
            }
        }
        if (nbt.contains("CustomName")) {
            try {
                setCustomName(stack, nbt.getString("CustomName"));
            } catch (Exception ignored) {
            }
        }
        if (nbt.contains("Dye")) {
            try {
                setDyedColour(stack, nbt.getInt("Dye"));
            } catch (Exception ignored) {
            }
        }
        if (nbt.contains("TrimMat") && nbt.contains("TrimPat")) {
            try {
                setArmourTrim(stack, nbt.getString("TrimMat"), nbt.getString("TrimPat"));
            } catch (Exception ignored) {
            }
        }
    }

    private static void setArmourTrim(ItemStack stack, String material, String pattern) {
        var mat = holderOf(net.minecraft.core.registries.Registries.TRIM_MATERIAL, material);
        var pat = holderOf(net.minecraft.core.registries.Registries.TRIM_PATTERN, pattern);
        if (mat == null || pat == null) return;
        //? if >=1.21.11 {
        /*stack.set(net.minecraft.core.component.DataComponents.TRIM,
                new net.minecraft.world.item.equipment.trim.ArmorTrim(mat, pat));
        *///?} elif >=1.21 {
        /*stack.set(net.minecraft.core.component.DataComponents.TRIM,
                new net.minecraft.world.item.armortrim.ArmorTrim(mat, pat));
        *///?} else {
        net.minecraft.world.item.armortrim.ArmorTrim.setTrim(RegistryAccess.get(), stack,
                new net.minecraft.world.item.armortrim.ArmorTrim(mat, pat));
        //?}
    }

    @Nullable
    private static <T> net.minecraft.core.Holder<T> holderOf(
            net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<T>> registryKey, String id) {
        net.minecraft.core.Registry<T> registry = RegistryAccess.get().registryOrThrow(registryKey);
        net.minecraft.resources.ResourceLocation location = Reg.parse(id);
        //? if >=1.21.11 {
        /*return registry.get(location).orElse(null);
        *///?} else {
        return registry.getHolder(net.minecraft.resources.ResourceKey.create(registryKey, location))
                .orElse(null);
        //?}
    }

    private static void applyPortableEnchantments(CompoundTag nbt, ItemStack stack) {
        if (!nbt.contains("Ench") || !Ench.get(stack).isEmpty()) return;
        CompoundTag levels = nbt.getCompound("Ench");
        java.util.Map<net.minecraft.world.item.enchantment.Enchantment, Integer> found =
                new java.util.LinkedHashMap<>();
        for (String key : levels.getAllKeys()) {
            net.minecraft.world.item.enchantment.Enchantment ench = Ench.byId(Reg.parse(key));
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
