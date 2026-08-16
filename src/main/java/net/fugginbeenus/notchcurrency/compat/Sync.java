package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataSerializer;

public final class Sync {

    private Sync() {}

    //? if >=1.21.11 {
    /*
    private static final EntityDataSerializer<CompoundTag> COMPOUND =
            EntityDataSerializer.forValueType(net.minecraft.network.codec.ByteBufCodecs.TRUSTED_COMPOUND_TAG);

    static {
        net.fabricmc.fabric.api.object.builder.v1.entity.FabricTrackedDataRegistry.register(
                Reg.id("compound_tag"), COMPOUND);
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
