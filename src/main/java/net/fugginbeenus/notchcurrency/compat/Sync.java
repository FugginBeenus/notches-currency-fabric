package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataSerializer;

/**
 * The tracked-data serializer for the NPC's custom pose.
 *
 * <p>A pose is a tag of per-bone rotations, and it has to reach the client to be drawn, so it rides
 * along as tracked entity data. Vanilla offered a CompoundTag serializer for exactly this until
 * 1.21.11 dropped it, so on that version the mod registers its own. Same wire shape, same tag.
 */
public final class Sync {

    private Sync() {}

    //? if >=1.21.11 {
    /*// Registered on first touch, which is when the entity class defines its accessor, and that
    // happens before any entity exists. Trusted, because this tag is written by the server.
    private static final EntityDataSerializer<CompoundTag> COMPOUND =
            EntityDataSerializer.forValueType(net.minecraft.network.codec.ByteBufCodecs.TRUSTED_COMPOUND_TAG);

    static {
        net.minecraft.network.syncher.EntityDataSerializers.registerSerializer(COMPOUND);
    }
    *///?}

    public static EntityDataSerializer<CompoundTag> compound() {
        //? if >=1.21.11 {
        /*return COMPOUND;
        *///?} else {
        return net.minecraft.network.syncher.EntityDataSerializers.COMPOUND_TAG;
        //?}
    }
}
