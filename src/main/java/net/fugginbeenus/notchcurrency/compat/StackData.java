package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Version-compat facade for reading/writing custom data attached to an {@link ItemStack}.
 *
 * <p>This is one of the mod's four "compat facades" for multi-version support (see the Stonecutter
 * scoping plan). The whole codebase routes item-data access through here so that the single
 * breaking change at Minecraft 1.20.5 — the switch from raw NBT to Data Components — is contained
 * to this one file. On 1.20.1 (this build) every method is a thin passthrough to the stack's NBT,
 * so there is <b>no behavior change</b>; on 1.21+ the same method bodies are swapped to operate on
 * the {@code CUSTOM_DATA} component instead.
 *
 * <p><b>Design rule that makes the migration safe:</b> every accessor is self-contained — it does a
 * full read or a full write and never hands out a mutable compound for callers to poke at. That
 * matters because on 1.21 the custom-data component is copy-on-write: grabbing the compound and
 * mutating it in place (the old {@code stack.getOrCreateNbt().putInt(...)} idiom) would silently
 * drop the change. Keeping the compound private to this class means the 1.21 implementation is a
 * correct drop-in.
 *
 * <p>Default values match vanilla {@link NbtCompound} semantics exactly (0 / 0L / 0.0 / false /
 * "" for a missing key), so routing existing code through this facade is behavior-preserving.
 *
 * <p>Two distinct concerns live here:
 * <ul>
 *   <li><b>Carrier data</b> — typed key/value accessors ({@link #getInt}, {@link #putString}, …)
 *       for items that stash our own {@code nc_*}/named keys on themselves (raffle tickets, the NPC
 *       pickup item, route planner, and the GUI carrier stacks).</li>
 *   <li><b>Whole-stack persistence</b> — {@link #writeStack}/{@link #readStack} for serializing an
 *       entire ItemStack into world-save data (shop listings, trade offers, auctions, bounties…).
 *       On 1.21 these need the registry manager; that is stashed once at server start and used
 *       internally, so call sites stay registry-free.</li>
 * </ul>
 */
public final class StackData {

    private StackData() {}

    // ---- read helpers (internal) ----

    /** The stack's custom-data compound for reading, or {@code null} if none is present. */
    @Nullable
    private static NbtCompound read(ItemStack stack) {
        //? on 1.20.1: the whole-stack tag doubles as our custom-data bag.
        return stack.getNbt();
    }

    /** The stack's custom-data compound for writing (created if absent). */
    private static NbtCompound write(ItemStack stack) {
        return stack.getOrCreateNbt();
    }

    // ---- presence ----

    /** True if the stack carries any custom data at all (was {@code stack.hasNbt()}). */
    public static boolean hasData(ItemStack stack) {
        return stack.hasNbt();
    }

    /** True if the given key is present. */
    public static boolean has(ItemStack stack, String key) {
        NbtCompound nbt = read(stack);
        return nbt != null && nbt.contains(key);
    }

    /** True if the key holds a UUID. */
    public static boolean hasUuid(ItemStack stack, String key) {
        NbtCompound nbt = read(stack);
        return nbt != null && nbt.containsUuid(key);
    }

    /** Remove a key if present. */
    public static void remove(ItemStack stack, String key) {
        NbtCompound nbt = read(stack);
        if (nbt != null) nbt.remove(key);
    }

    // ---- typed getters (vanilla defaults for a missing key) ----

    public static int getInt(ItemStack stack, String key) {
        NbtCompound nbt = read(stack);
        return nbt == null ? 0 : nbt.getInt(key);
    }

    public static long getLong(ItemStack stack, String key) {
        NbtCompound nbt = read(stack);
        return nbt == null ? 0L : nbt.getLong(key);
    }

    public static double getDouble(ItemStack stack, String key) {
        NbtCompound nbt = read(stack);
        return nbt == null ? 0.0 : nbt.getDouble(key);
    }

    public static boolean getBoolean(ItemStack stack, String key) {
        NbtCompound nbt = read(stack);
        return nbt != null && nbt.getBoolean(key);
    }

    public static String getString(ItemStack stack, String key) {
        NbtCompound nbt = read(stack);
        return nbt == null ? "" : nbt.getString(key);
    }

    /** UUID at {@code key}, or {@code null} if absent. */
    @Nullable
    public static UUID getUuid(ItemStack stack, String key) {
        NbtCompound nbt = read(stack);
        return (nbt != null && nbt.containsUuid(key)) ? nbt.getUuid(key) : null;
    }

    /**
     * A copy of the compound stored at {@code key}, or an empty compound if absent. Returns a copy
     * (not a live view) precisely so callers can't mutate-in-place; use {@link #putCompound} to
     * write changes back.
     */
    public static NbtCompound getCompound(ItemStack stack, String key) {
        NbtCompound nbt = read(stack);
        return nbt == null ? new NbtCompound() : nbt.getCompound(key).copy();
    }

    // ---- typed setters (full read-modify-write; safe on copy-on-write component storage) ----

    public static void putInt(ItemStack stack, String key, int value) {
        write(stack).putInt(key, value);
    }

    public static void putLong(ItemStack stack, String key, long value) {
        write(stack).putLong(key, value);
    }

    public static void putDouble(ItemStack stack, String key, double value) {
        write(stack).putDouble(key, value);
    }

    public static void putBoolean(ItemStack stack, String key, boolean value) {
        write(stack).putBoolean(key, value);
    }

    public static void putString(ItemStack stack, String key, String value) {
        write(stack).putString(key, value);
    }

    public static void putUuid(ItemStack stack, String key, UUID value) {
        write(stack).putUuid(key, value);
    }

    public static void putCompound(ItemStack stack, String key, NbtCompound value) {
        write(stack).put(key, value);
    }

    // ---- bulk read (for carrier stacks whose readers do many typed lookups) ----

    /**
     * A read-only <b>copy</b> of the stack's custom data (empty compound if none). Use this for the
     * GUI "carrier" readers that pull a handful of {@code nc_*} keys in one method and rely on typed
     * lookups like {@code t.contains(key, NbtElement.LONG_TYPE)} — replacing {@code stack.getNbt()}
     * with this keeps every downstream vanilla read identical while hiding the 1.21 component switch.
     *
     * <p>Returns a copy on purpose: mutating it must never write back (that's what {@link #editData}
     * /{@link #commitData} are for), so the same code is correct on 1.21's copy-on-write components.
     */
    public static NbtCompound getData(ItemStack stack) {
        NbtCompound nbt = read(stack);
        return nbt == null ? new NbtCompound() : nbt.copy();
    }

    // ---- batch edit (for carrier stacks that set several keys at once) ----

    /**
     * Begin a batch edit: returns the compound to write several keys into, to be handed back to
     * {@link #commitData}. Use this for the GUI "carrier" stacks that stamp a handful of {@code nc_*}
     * keys in one go, instead of calling the typed putters N times.
     *
     * <p>On 1.20.1 this is the stack's live NBT and {@link #commitData} is effectively a no-op; on
     * 1.21 it is a detached copy of the custom-data component that only takes effect once committed.
     * Because of that, treat the returned compound as write-only scratch: fill it, then commit —
     * don't interleave {@link #getInt}-style reads against the same stack before committing.
     */
    public static NbtCompound editData(ItemStack stack) {
        return stack.getOrCreateNbt();
    }

    /** Commit a compound obtained from {@link #editData} back onto the stack. */
    public static void commitData(ItemStack stack, NbtCompound data) {
        stack.setNbt(data);
    }

    /** Strip all custom data from the stack (was {@code stack.setNbt(null)}). */
    public static void clearData(ItemStack stack) {
        stack.setNbt(null);
    }

    // ---- whole-stack persistence (for world-save data classes) ----

    /**
     * Serialize an entire ItemStack into a fresh compound for storage in world data.
     * On 1.20.1 this is {@code stack.writeNbt(new NbtCompound())}; on 1.21 it becomes an
     * {@code ItemStack.CODEC} encode against the stashed registry manager.
     */
    public static NbtCompound writeStack(ItemStack stack) {
        return stack.writeNbt(new NbtCompound());
    }

    /**
     * Reconstruct an ItemStack previously written with {@link #writeStack} (or any vanilla
     * stack-tag). On 1.20.1 this is {@code ItemStack.fromNbt(nbt)}; on 1.21 it becomes an
     * {@code ItemStack.CODEC} decode against the stashed registry manager.
     */
    public static ItemStack readStack(NbtCompound nbt) {
        return ItemStack.fromNbt(nbt);
    }
}
