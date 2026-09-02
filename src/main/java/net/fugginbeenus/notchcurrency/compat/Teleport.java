package net.fugginbeenus.notchcurrency.compat;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class Teleport {

    private Teleport() {}

    public static void move(ServerPlayer sp, ServerLevel target, double x, double y, double z) {
        //? if >=1.21.11 {
        /*sp.teleport(new net.minecraft.world.level.portal.TeleportTransition(
                target, new net.minecraft.world.phys.Vec3(x, y, z),
                net.minecraft.world.phys.Vec3.ZERO, sp.getYRot(), sp.getXRot(),
                net.minecraft.world.level.portal.TeleportTransition.DO_NOTHING));
        *///?} else {
        sp.teleportTo(target, x, y, z, sp.getYRot(), sp.getXRot());
        //?}
    }
}
