package net.fugginbeenus.notchcurrency.registry;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fugginbeenus.notchcurrency.block.entity.CoinFlipBlockEntity;
import net.fugginbeenus.notchcurrency.block.entity.LedgerBoardBlockEntity;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** Block entities: the Ledger Board (live leaderboard) and the Coin Flip table (animated coin). */
public final class ModBlockEntities {

    public static final BlockEntityType<LedgerBoardBlockEntity> LEDGER_BOARD =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, NotchCurrency.id("ledger_board"),
                    FabricBlockEntityTypeBuilder.create(LedgerBoardBlockEntity::new, ModBlocks.LEDGER_BOARD).build());

    public static final BlockEntityType<CoinFlipBlockEntity> COIN_FLIP =
            Registry.register(Registries.BLOCK_ENTITY_TYPE, NotchCurrency.id("coin_flip"),
                    FabricBlockEntityTypeBuilder.create(CoinFlipBlockEntity::new, ModBlocks.COIN_FLIP).build());

    private ModBlockEntities() {}

    /** Force class-load so the static registration runs. */
    public static void register() {}
}
