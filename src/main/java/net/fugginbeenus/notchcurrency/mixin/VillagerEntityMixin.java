package net.fugginbeenus.notchcurrency.mixin;

import net.fugginbeenus.notchcurrency.economy.villager.VillagerCoinTrades;
import net.minecraft.entity.passive.VillagerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(VillagerEntity.class)
public abstract class VillagerEntityMixin {

    @Inject(method = "fillRecipes", at = @At("TAIL"))
    private void notchcurrency$convertCoinTrades(CallbackInfo ci) {
        VillagerCoinTrades.convert((VillagerEntity) (Object) this);
    }
}
