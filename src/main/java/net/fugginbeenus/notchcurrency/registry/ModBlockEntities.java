package net.fugginbeenus.notchcurrency.registry;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.fugginbeenus.notchcurrency.block.entity.CoinFlipBlockEntity;
import net.fugginbeenus.notchcurrency.block.entity.LedgerBoardBlockEntity;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {

    public static final BlockEntityType<net.fugginbeenus.notchcurrency.block.entity.MailboxBlockEntity> MAILBOX =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, NotchCurrency.id("mailbox"),
                    FabricBlockEntityTypeBuilder.create(
                            net.fugginbeenus.notchcurrency.block.entity.MailboxBlockEntity::new,
                            ModBlocks.MAILBOX).build());

    public static final BlockEntityType<LedgerBoardBlockEntity> LEDGER_BOARD =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, NotchCurrency.id("ledger_board"),
                    FabricBlockEntityTypeBuilder.create(LedgerBoardBlockEntity::new, ModBlocks.LEDGER_BOARD).build());

    public static final BlockEntityType<CoinFlipBlockEntity> COIN_FLIP =
            Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, NotchCurrency.id("coin_flip"),
                    FabricBlockEntityTypeBuilder.create(CoinFlipBlockEntity::new, ModBlocks.COIN_FLIP).build());

    private ModBlockEntities() {}

    public static final BlockEntityType<net.fugginbeenus.notchcurrency.block.entity.CrateBlockEntity> COMMON_CRATE =
            crateType("common_crate", ModBlocks.COMMON_CRATE);
    public static final BlockEntityType<net.fugginbeenus.notchcurrency.block.entity.CrateBlockEntity> RARE_CRATE =
            crateType("rare_crate", ModBlocks.RARE_CRATE);
    public static final BlockEntityType<net.fugginbeenus.notchcurrency.block.entity.CrateBlockEntity> EPIC_CRATE =
            crateType("epic_crate", ModBlocks.EPIC_CRATE);

    private static BlockEntityType<net.fugginbeenus.notchcurrency.block.entity.CrateBlockEntity> crateType(
            String id, net.minecraft.world.level.block.Block block) {
        BlockEntityType<net.fugginbeenus.notchcurrency.block.entity.CrateBlockEntity>[] holder =
                new BlockEntityType[1];
        holder[0] = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, NotchCurrency.id(id),
                FabricBlockEntityTypeBuilder.create(
                        (pos, state) -> new net.fugginbeenus.notchcurrency.block.entity.CrateBlockEntity(
                                holder[0], pos, state), block).build());
        return holder[0];
    }

    public static BlockEntityType<net.fugginbeenus.notchcurrency.block.entity.CrateBlockEntity> crateTypeFor(String tier) {
        return switch (tier) {
            case "rare" -> RARE_CRATE;
            case "epic" -> EPIC_CRATE;
            default -> COMMON_CRATE;
        };
    }

    public static void register() {}
}
