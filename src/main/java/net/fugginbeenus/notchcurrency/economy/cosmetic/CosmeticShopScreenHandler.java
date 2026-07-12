package net.fugginbeenus.notchcurrency.economy.cosmetic;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.registry.ModScreenHandlers;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/**
 * Backing handler for the cosmetics shop: a vanilla-style item grid (real read-only slots so the
 * client renders icons) with an optional NPC bust. Offers carry name/price/owned in NBT and refresh
 * each tick so "owned" updates right after a purchase. Purchases go through COSMETIC_BUY.
 */
public class CosmeticShopScreenHandler extends ScreenHandler {

    // Carrier slots live OFF-screen (the row-list screen reads rowStack(i) and draws icons itself).
    public static final int VIS_ROWS = 6, PER_PAGE = VIS_ROWS;
    public static final int P_PAGE = 0, P_TOTAL_PAGES = 1, P_COUNT = 2;
    private static final int PROP_COUNT = 3;

    private final PlayerInventory playerInv;
    @Nullable private final UUID npcId;
    private final SimpleInventory rowInv = new SimpleInventory(PER_PAGE);
    private final PropertyDelegate props = new ArrayPropertyDelegate(PROP_COUNT);
    private int page = 0;

    private static final class ReadOnlySlot extends Slot {
        ReadOnlySlot(Inventory inv, int i, int x, int y) { super(inv, i, x, y); }
        @Override public boolean canInsert(ItemStack s) { return false; }
        @Override public boolean canTakeItems(PlayerEntity p) { return false; }
    }

    /** Client constructor: the opening buf carries the linked NPC uuid (or a no-npc marker). */
    public CosmeticShopScreenHandler(int syncId, PlayerInventory inv, PacketByteBuf buf) {
        this(syncId, inv, buf.readBoolean() ? buf.readUuid() : null);
    }

    public CosmeticShopScreenHandler(int syncId, PlayerInventory inv, @Nullable UUID npcId) {
        super(ModScreenHandlers.COSMETIC_SHOP, syncId);
        this.playerInv = inv;
        this.npcId = npcId;
        this.addProperties(props);
        for (int i = 0; i < PER_PAGE; i++) {
            this.addSlot(new ReadOnlySlot(rowInv, i, -10000, -10000));
        }
        // Player inventory (matches the code-drawn CosmeticShopScreen / ShopBrowseScreen layout).
        final int invX = 43, invY = 158;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, invX + col * 18, invY + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, invX + col * 18, invY + 58));
        }
        refresh();
    }

    /** Open for the player, passing the linked NPC uuid for the preview. */
    public static void open(ServerPlayerEntity sp, @Nullable UUID npcId) {
        sp.openHandledScreen(new ExtendedScreenHandlerFactory() {
            @Override
            public Text getDisplayName() {
                return Text.literal("Cosmetics");
            }

            @Override
            public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity p) {
                return new CosmeticShopScreenHandler(syncId, inv, npcId);
            }

            @Override
            public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
                buf.writeBoolean(npcId != null);
                if (npcId != null) buf.writeUuid(npcId);
            }
        });
    }

    public ItemStack rowStack(int i) { return rowInv.getStack(i); }
    public int prop(int i) { return props.get(i); }
    @Nullable public UUID npcId() { return npcId; }

    private void refresh() {
        if (!(playerInv.player instanceof ServerPlayerEntity sp) || sp.getServer() == null) return;
        CosmeticState state = CosmeticState.get(sp.getServer());
        List<CosmeticOffer> offers = CosmeticRegistry.all();

        int totalPages = Math.max(1, (offers.size() + PER_PAGE - 1) / PER_PAGE);
        if (page >= totalPages) page = totalPages - 1;
        if (page < 0) page = 0;
        props.set(P_PAGE, page);
        props.set(P_TOTAL_PAGES, totalPages);
        props.set(P_COUNT, offers.size());

        int start = page * PER_PAGE;
        for (int i = 0; i < PER_PAGE; i++) {
            int idx = start + i;
            if (idx < offers.size()) {
                CosmeticOffer o = offers.get(idx);
                boolean owned = o.oneTime() && state.owns(sp.getUuid(), o.id());
                rowInv.setStack(i, display(o, owned));
            } else {
                rowInv.setStack(i, ItemStack.EMPTY);
            }
        }
    }

    private static ItemStack display(CosmeticOffer offer, boolean owned) {
        ItemStack carrier = offer.icon().copy();
        if (carrier.isEmpty()) return ItemStack.EMPTY;
        carrier.setCount(1);
        NbtCompound t = StackData.editData(carrier);
        t.putString("nc_cid", offer.id());
        t.putString("nc_name", offer.name());
        t.putLong("nc_price", offer.price());
        t.putBoolean("nc_owned", owned);
        StackData.commitData(carrier, t);
        return carrier;
    }

    @Override
    public void sendContentUpdates() {
        refresh();
        super.sendContentUpdates();
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (!(player instanceof ServerPlayerEntity)) return false;
        if (id == 0) page = Math.max(0, page - 1);
        else if (id == 1) page = page + 1; // clamped in refresh()
        else return false;
        refresh();
        sendContentUpdates();
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
