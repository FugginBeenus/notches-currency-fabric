package net.fugginbeenus.notchcurrency.registry;

import net.fugginbeenus.notchcurrency.atm.ATMBlock;
import net.fugginbeenus.notchcurrency.block.BountyBoardBlock;
import net.fugginbeenus.notchcurrency.block.CoinFlipBlock;
import net.fugginbeenus.notchcurrency.block.CrateBlock;
import net.fugginbeenus.notchcurrency.block.GoldenCacheBlock;
import net.fugginbeenus.notchcurrency.block.LedgerBoardBlock;
import net.fugginbeenus.notchcurrency.block.SlotMachineBlock;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class ModBlocks {

    // ATM
    public static final Block ATM = Registry.register(
            BuiltInRegistries.BLOCK,
            NotchCurrency.id("atm"),
            new ATMBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .noOcclusion())
    );

    public static final Item ATM_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            NotchCurrency.id("atm"),
            new BlockItem(ATM, new Item.Properties())
    );

    // Golden Cache (crate)
    public static final Block GOLDEN_CACHE = Registry.register(
            BuiltInRegistries.BLOCK,
            NotchCurrency.id("golden_cache"),
            new GoldenCacheBlock(
                    BlockBehaviour.Properties.of()
                            .strength(2.0f)
                            .noOcclusion()
                            .lightLevel(state -> 12)   // <--- glowy boi
            )
    );

    public static final Item GOLDEN_CACHE_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            NotchCurrency.id("golden_cache"),
            new BlockItem(GOLDEN_CACHE, new Item.Properties())
    );

    // Ledger Board: shows the balance leaderboard on use. Placeholder model/texture for now.
    public static final Block LEDGER_BOARD = Registry.register(
            BuiltInRegistries.BLOCK,
            NotchCurrency.id("ledger_board"),
            new LedgerBoardBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
    );

    public static final Item LEDGER_BOARD_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            NotchCurrency.id("ledger_board"),
            new BlockItem(LEDGER_BOARD, new Item.Properties())
    );

    // Bounty Board: shows the auto-generated bounties on use. Placeholder model/texture for now.
    public static final Block BOUNTY_BOARD = Registry.register(
            BuiltInRegistries.BLOCK,
            NotchCurrency.id("bounty_board"),
            new BountyBoardBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
    );

    public static final Item BOUNTY_BOARD_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            NotchCurrency.id("bounty_board"),
            new BlockItem(BOUNTY_BOARD, new Item.Properties())
    );

    // Slot Machine: right-click to play the slots. Non-full model, so nonOpaque keeps
    // neighbouring blocks from culling their touching faces (the see-through-world bug).
    public static final Block SLOT_MACHINE = Registry.register(
            BuiltInRegistries.BLOCK,
            NotchCurrency.id("slot_machine"),
            new SlotMachineBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
    );

    public static final Item SLOT_MACHINE_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            NotchCurrency.id("slot_machine"),
            new BlockItem(SLOT_MACHINE, new Item.Properties())
    );

    // Coin Flip block: right-click to bet, it "flips" then reveals. Placeholder model/texture.
    public static final Block COIN_FLIP = Registry.register(
            BuiltInRegistries.BLOCK,
            NotchCurrency.id("coin_flip"),
            new CoinFlipBlock(BlockBehaviour.Properties.of()
                    .strength(2.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
    );

    public static final Item COIN_FLIP_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            NotchCurrency.id("coin_flip"),
            new BlockItem(COIN_FLIP, new Item.Properties())
    );

    // Crates (opened with keys). Each block is bound to a datapack crate type id.
    public static final Block COMMON_CRATE = registerCrate("common_crate", "common");
    public static final Block RARE_CRATE = registerCrate("rare_crate", "rare");
    public static final Block EPIC_CRATE = registerCrate("epic_crate", "epic");

    private static Block registerCrate(String blockId, String crateType) {
        Block block = Registry.register(BuiltInRegistries.BLOCK, NotchCurrency.id(blockId),
                new CrateBlock(BlockBehaviour.Properties.of().strength(2.0f).requiresCorrectToolForDrops().noOcclusion(), crateType));
        Registry.register(BuiltInRegistries.ITEM, NotchCurrency.id(blockId), new BlockItem(block, new Item.Properties()));
        return block;
    }

    public static Item crateItem(String blockId) {
        return BuiltInRegistries.ITEM.get(NotchCurrency.id(blockId));
    }

    private ModBlocks() {}

    public static void register() {
        // NO-OP; static initializers above already did all the registering.
    }
}
