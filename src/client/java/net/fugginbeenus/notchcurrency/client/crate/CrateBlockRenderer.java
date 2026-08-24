package net.fugginbeenus.notchcurrency.client.crate;

import net.fugginbeenus.notchcurrency.block.entity.CrateBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

//? if >=1.21.11 {
/*public class CrateBlockRenderer extends GeoBlockRenderer<CrateBlockEntity, CrateRenderState> {
*///?} else {
public class CrateBlockRenderer extends GeoBlockRenderer<CrateBlockEntity> {
//?}

    public CrateBlockRenderer(
            net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider.Context ctx, String tier) {
        //? if >=26.1 {
        /*super(ctx, new CrateGeoModel(tier));
        *///?} else {
        super(new CrateGeoModel(tier));
        //?}
    }
}
