package net.fugginbeenus.notchcurrency.mixin;

import net.fugginbeenus.notchcurrency.economy.villager.VillagerCoinTrades;
import net.minecraft.world.entity.npc.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerEntityMixin {

    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void notchcurrency$convertCoinTrades(CallbackInfo ci) {
        VillagerCoinTrades.convert((Villager) (Object) this);
    }
}
