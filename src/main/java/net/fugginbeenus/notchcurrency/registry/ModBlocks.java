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

    public static final Block ATM = Registry.register(
            BuiltInRegistries.BLOCK,
            NotchCurrency.id("atm"),
            new ATMBlock(net.fugginbeenus.notchcurrency.compat.Reg.blockProps("atm")
                    .strength(2.0f)
                    .noOcclusion())
    );

    public static final Item ATM_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            NotchCurrency.id("atm"),
            new BlockItem(ATM, net.fugginbeenus.notchcurrency.compat.Reg.blockItemProps("atm"))
    );

    public static final Block GOLDEN_CACHE = Registry.register(
            BuiltInRegistries.BLOCK,
            NotchCurrency.id("golden_cache"),
            new GoldenCacheBlock(
                    net.fugginbeenus.notchcurrency.compat.Reg.blockProps("golden_cache")
                            .strength(2.0f)
                            .noOcclusion()
                            .lightLevel(state -> 12)   // <--- glowy boi
            )
    );

    public static final Item GOLDEN_CACHE_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            NotchCurrency.id("golden_cache"),
            new BlockItem(GOLDEN_CACHE, net.fugginbeenus.notchcurrency.compat.Reg.blockItemProps("golden_cache"))
    );

    public static final Block MAILBOX = Registry.register(
            BuiltInRegistries.BLOCK,
            NotchCurrency.id("mailbox"),
            new net.fugginbeenus.notchcurrency.block.MailboxBlock(
                    net.fugginbeenus.notchcurrency.compat.Reg.blockProps("mailbox")
                            .strength(1.5f)
                            .noOcclusion())
    );

    public static final Item MAILBOX_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            NotchCurrency.id("mailbox"),
            new BlockItem(MAILBOX, net.fugginbeenus.notchcurrency.compat.Reg.blockItemProps("mailbox"))
    );

    public static final Block LEDGER_BOARD = Registry.register(
            BuiltInRegistries.BLOCK,
            NotchCurrency.id("ledger_board"),
            new LedgerBoardBlock(net.fugginbeenus.notchcurrency.compat.Reg.blockProps("ledger_board")
                    .strength(2.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
    );

    public static final Item LEDGER_BOARD_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            NotchCurrency.id("ledger_board"),
            new BlockItem(LEDGER_BOARD, net.fugginbeenus.notchcurrency.compat.Reg.blockItemProps("ledger_board"))
    );

    public static final Block BOUNTY_BOARD = Registry.register(
            BuiltInRegistries.BLOCK,
            NotchCurrency.id("bounty_board"),
            new BountyBoardBlock(net.fugginbeenus.notchcurrency.compat.Reg.blockProps("bounty_board")
                    .strength(2.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
    );

    public static final Item BOUNTY_BOARD_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            NotchCurrency.id("bounty_board"),
            new BlockItem(BOUNTY_BOARD, net.fugginbeenus.notchcurrency.compat.Reg.blockItemProps("bounty_board"))
    );

    public static final Block SLOT_MACHINE = Registry.register(
            BuiltInRegistries.BLOCK,
            NotchCurrency.id("slot_machine"),
            new SlotMachineBlock(net.fugginbeenus.notchcurrency.compat.Reg.blockProps("slot_machine")
                    .strength(2.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
    );

    public static final Item SLOT_MACHINE_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            NotchCurrency.id("slot_machine"),
            new BlockItem(SLOT_MACHINE, net.fugginbeenus.notchcurrency.compat.Reg.blockItemProps("slot_machine"))
    );

    public static final Block COIN_FLIP = Registry.register(
            BuiltInRegistries.BLOCK,
            NotchCurrency.id("coin_flip"),
            new CoinFlipBlock(net.fugginbeenus.notchcurrency.compat.Reg.blockProps("coin_flip")
                    .strength(2.0f)
                    .requiresCorrectToolForDrops()
                    .noOcclusion())
    );

    public static final Item COIN_FLIP_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            NotchCurrency.id("coin_flip"),
            new BlockItem(COIN_FLIP, net.fugginbeenus.notchcurrency.compat.Reg.blockItemProps("coin_flip"))
    );

    public static final Block COMMON_CRATE = registerCrate("common_crate", "common");
    public static final Block RARE_CRATE = registerCrate("rare_crate", "rare");
    public static final Block EPIC_CRATE = registerCrate("epic_crate", "epic");

    private static Block registerCrate(String blockId, String crateType) {
        Block block = Registry.register(BuiltInRegistries.BLOCK, NotchCurrency.id(blockId),
                new CrateBlock(net.fugginbeenus.notchcurrency.compat.Reg.blockProps(blockId).strength(2.0f).requiresCorrectToolForDrops().noOcclusion(), crateType));
        Registry.register(BuiltInRegistries.ITEM, NotchCurrency.id(blockId), new BlockItem(block, net.fugginbeenus.notchcurrency.compat.Reg.blockItemProps(blockId)));
        return block;
    }

    public static Item crateItem(String blockId) {
        return BuiltInRegistries.ITEM.get(NotchCurrency.id(blockId));
    }

    private ModBlocks() {}

    public static void register() {
    }
}
