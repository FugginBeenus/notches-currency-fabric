package net.fugginbeenus.notchcurrency.net;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.core.CoinEconomy;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.fugginbeenus.notchcurrency.auction.AuctionState;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.UUID;

import static net.fugginbeenus.notchcurrency.core.NotchCurrency.coinIcon;
import static net.fugginbeenus.notchcurrency.core.NotchCurrency.coins;

/**
 * Registration of all server-bound packet receivers (client -> server):
 * balance requests, auction bids, ATM withdrawals, and player-shop operations.
 *
 * Extracted from the mod initializer to keep {@link NotchCurrency} small.
 */
public final class ServerPacketHandlers {

    private ServerPacketHandlers() {}

    /** Call once from NotchCurrency.onInitialize(). */
    public static void register() {
        // Server handles client's explicit balance request
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.BALANCE_REQUEST,
                (server, player, handler, buf, response) ->
                        server.execute(() ->
                                NotchPackets.sendBalance(player, BalanceStore.get(player)))
        );

        // Server handles client's bid request (from right-click GUI)
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.BID_REQUEST,
                (server, player, handler, buf, response) -> {
                    UUID listingId = buf.readUuid();
                    long bidAmount = buf.readVarLong();

                    server.execute(() -> {
                        // player is already a ServerPlayerEntity here
                        ServerPlayerEntity sp = player;
                        ServerWorld world = sp.getServerWorld();
                        AuctionState state = AuctionState.get(world);
                        state.placeBid(world, sp, listingId, bidAmount);
                    });
                }
        );

        // Server handles cancel-listing requests from the AH "My Listings" popup
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.AUCTION_CANCEL,
                (server, player, handler, buf, response) -> {
                    UUID listingId = buf.readUuid();

                    server.execute(() -> {
                        ServerPlayerEntity sp = player;
                        ServerWorld world = sp.getServerWorld();
                        AuctionState state = AuctionState.get(world);
                        net.fugginbeenus.notchcurrency.auction.AuctionListing l = state.getListing(listingId);

                        if (l == null) {
                            sp.sendMessage(Text.literal("That listing is no longer available.").formatted(Formatting.RED), false);
                            return;
                        }
                        if (!sp.getUuid().equals(l.sellerUuid)) {
                            sp.sendMessage(Text.literal("Only the seller can cancel this listing.").formatted(Formatting.RED), false);
                            return;
                        }
                        // Refund any standing bid before removing — bids escrow coins.
                        long refunded = state.refundHighestBid(world, l);
                        state.removeListing(listingId);
                        if (refunded > 0) {
                            sp.sendMessage(Text.literal("The high bidder was refunded " + refunded + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".")
                                    .formatted(Formatting.GRAY), false);
                        }

                        ItemStack toReturn = l.stack.copy();
                        if (toReturn.hasNbt()) {
                            net.minecraft.nbt.NbtCompound tag = toReturn.getNbt();
                            tag.remove("nc_price");
                            tag.remove("nc_seller");
                            tag.remove("nc_created");
                            tag.remove("nc_expires");
                            tag.remove("nc_highest_bid");
                            tag.remove("nc_highest_bidder");
                            tag.remove("nc_listing_id");
                            if (tag.isEmpty()) toReturn.setNbt(null);
                        }
                        if (!sp.getInventory().insertStack(toReturn) && !toReturn.isEmpty()) {
                            sp.dropItem(toReturn, false);
                        }

                        sp.sendMessage(Text.literal("Cancelled listing for ").formatted(Formatting.GREEN)
                                .append(l.stack.getName().copy().formatted(Formatting.YELLOW))
                                .append(Text.literal(" — item returned.").formatted(Formatting.GREEN)), false);

                        // Refresh the open Auction House so the popup updates live.
                        if (sp.currentScreenHandler instanceof net.fugginbeenus.notchcurrency.auction.AuctionHouseScreenHandler ah) {
                            ah.reload();
                            ah.sendContentUpdates();
                        }
                    });
                }
        );

        // Server handles create-listing from the "List an Item" screen
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.AUCTION_LIST,
                (server, player, handler, buf, response) -> {
                    long price = buf.readVarLong();
                    int days = buf.readVarInt();
                    server.execute(() -> {
                        if (player.currentScreenHandler
                                instanceof net.fugginbeenus.notchcurrency.auction.AuctionListingScreenHandler listing) {
                            listing.listFromInput(player, price, days);
                        }
                    });
                }
        );

        // Server handles raffle admin GUI "Save & Apply" (op-only)
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.RAFFLE_ADMIN_SAVE,
                (server, player, handler, buf, response) -> {
                    long price = buf.readVarLong();
                    int cut = buf.readVarInt();
                    int intervalDays = buf.readVarInt();
                    boolean enabled = buf.readBoolean();
                    long coinsPool = buf.readVarLong();
                    server.execute(() -> {
                        if (!player.hasPermissionLevel(2)) return;
                        boolean wasEnabled = net.fugginbeenus.notchcurrency.economy.raffle.RaffleManager.isEnabled();
                        net.fugginbeenus.notchcurrency.config.NotchConfig cfg =
                                net.fugginbeenus.notchcurrency.config.NotchConfigIO.get();
                        cfg.raffle.ticketPrice = Math.max(1L, price);
                        cfg.raffle.houseCutPercent = Math.max(0, Math.min(100, cut));
                        cfg.raffle.drawIntervalMinutes = Math.max(0, intervalDays) * 1440; // days → minutes
                        cfg.raffle.enabled = enabled;
                        net.fugginbeenus.notchcurrency.config.NotchConfigIO.save(cfg);
                        net.fugginbeenus.notchcurrency.economy.raffle.RaffleManager.applyConfig(cfg);

                        net.fugginbeenus.notchcurrency.economy.raffle.RaffleState state =
                                net.fugginbeenus.notchcurrency.economy.raffle.RaffleState.get(server);

                        if (wasEnabled && !enabled) {
                            // Turning it off cancels the raffle: wipe entries/pot/prize, void tickets,
                            // and hand the escrowed prize back to the admin.
                            net.fugginbeenus.notchcurrency.economy.raffle.RaffleManager.resetAndReturn(player);
                            player.sendMessage(Text.literal("Raffle cancelled — entries & pot cleared, prize returned, tickets voided.")
                                    .formatted(Formatting.YELLOW), false);
                            return;
                        }

                        // Starting a raffle with leftover ticket state → clear the old pot/entries
                        // (keeps the prize you're configuring) so it doesn't begin with a stale pot.
                        if (!wasEnabled && enabled && (state.getPot() > 0 || state.getTotalTickets() > 0)) {
                            state.clearEntries();
                        }

                        state.setCoinsPool(coinsPool);
                        if (player.currentScreenHandler
                                instanceof net.fugginbeenus.notchcurrency.economy.raffle.RaffleAdminScreenHandler h) {
                            h.applyPrizeFromInput(player);
                        }
                        net.fugginbeenus.notchcurrency.economy.raffle.RaffleManager.refreshAllOnline(server);
                        player.sendMessage(Text.literal("Raffle settings saved & applied.").formatted(Formatting.GREEN), false);
                    });
                }
        );

        // Server handles bounty board actions (take / claim / turn in) from the GUI
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.BOUNTY_ACTION,
                (server, player, handler, buf, response) -> {
                    UUID bountyId = buf.readUuid();
                    int action = buf.readVarInt();
                    server.execute(() -> {
                        switch (action) {
                            case 0 -> net.fugginbeenus.notchcurrency.economy.bounty.BountyManager.take(player, bountyId);
                            case 1 -> net.fugginbeenus.notchcurrency.economy.bounty.BountyManager.claim(player, bountyId);
                            case 2 -> net.fugginbeenus.notchcurrency.economy.bounty.BountyManager.turnIn(player, bountyId);
                            default -> { }
                        }
                    });
                }
        );

        // Server handles bounty admin GUI "Save & Apply" (op-only)
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.BOUNTY_ADMIN_SAVE,
                (server, player, handler, buf, response) -> {
                    boolean enabled = buf.readBoolean();
                    int activeCount = buf.readVarInt();
                    int takeLimit = buf.readVarInt();
                    int durationMinutes = buf.readVarInt();
                    server.execute(() -> {
                        if (!player.hasPermissionLevel(2)) return;
                        net.fugginbeenus.notchcurrency.config.NotchConfig cfg =
                                net.fugginbeenus.notchcurrency.config.NotchConfigIO.get();
                        cfg.bounty.enabled = enabled;
                        cfg.bounty.activeCount = Math.max(0, Math.min(20, activeCount));
                        cfg.bounty.takeLimit = Math.max(1, Math.min(5, takeLimit));
                        cfg.bounty.durationMinutes = Math.max(1, durationMinutes);
                        net.fugginbeenus.notchcurrency.config.NotchConfigIO.save(cfg);
                        net.fugginbeenus.notchcurrency.economy.bounty.BountyManager.applyConfig(cfg);
                        if (player.currentScreenHandler
                                instanceof net.fugginbeenus.notchcurrency.economy.bounty.BountyAdminScreenHandler h) {
                            h.persistDecrees(player);
                        }
                        player.sendMessage(Text.literal("Bounty settings saved & applied.").formatted(Formatting.GREEN), false);
                    });
                }
        );

        // Server handles loan GUI borrow/repay
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.LOAN_ACTION,
                (server, player, handler, buf, response) -> {
                    int action = buf.readVarInt();
                    long amount = buf.readVarLong();
                    server.execute(() -> {
                        if (action == 0) net.fugginbeenus.notchcurrency.economy.loan.LoanManager.borrow(player, amount);
                        else net.fugginbeenus.notchcurrency.economy.loan.LoanManager.repay(player, amount);
                    });
                }
        );

        // Server handles slot-machine spins (bet typed in the GUI)
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.SLOTS_SPIN,
                (server, player, handler, buf, response) -> {
                    long bet = buf.readVarLong();
                    server.execute(() -> {
                        if (player.currentScreenHandler
                                instanceof net.fugginbeenus.notchcurrency.economy.gambling.SlotMachineScreenHandler h) {
                            h.spin(bet);
                        }
                    });
                }
        );

        // Server handles coin-flip bets (side + bet from the GUI; block does the reveal)
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.COINFLIP_FLIP,
                (server, player, handler, buf, response) -> {
                    boolean guessHeads = buf.readBoolean();
                    long bet = buf.readVarLong();
                    server.execute(() ->
                            net.fugginbeenus.notchcurrency.economy.gambling.CoinFlipManager.flipFromScreen(player, guessHeads, bet));
                }
        );

        // ---- Notch NPC editor (owner/op re-checked inside NotchNpcManager) ----
        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_SET_ROLE, (server, player, handler, buf, rs) -> {
            UUID id = buf.readUuid();
            int ord = buf.readVarInt();
            server.execute(() -> {
                net.minecraft.entity.Entity e = player.getServerWorld().getEntity(id);
                if (!(e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc)) return;
                net.fugginbeenus.notchcurrency.economy.npc.NpcRole[] all =
                        net.fugginbeenus.notchcurrency.economy.npc.NpcRole.values();
                net.fugginbeenus.notchcurrency.economy.npc.NpcRole role =
                        (ord >= 0 && ord < all.length) ? all[ord] : net.fugginbeenus.notchcurrency.economy.npc.NpcRole.NONE;
                net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setRole(player, npc, role);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_SET_NAME, (server, player, handler, buf, rs) -> {
            UUID id = buf.readUuid();
            String name = buf.readString();
            server.execute(() -> {
                net.minecraft.entity.Entity e = player.getServerWorld().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setName(player, npc, name);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_PICKUP, (server, player, handler, buf, rs) -> {
            UUID id = buf.readUuid();
            server.execute(() -> {
                net.minecraft.entity.Entity e = player.getServerWorld().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.pickUp(player, npc);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_DELETE, (server, player, handler, buf, rs) -> {
            UUID id = buf.readUuid();
            server.execute(() -> {
                net.minecraft.entity.Entity e = player.getServerWorld().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.delete(player, npc);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_DIALOGUE_CHOICE, (server, player, handler, buf, rs) -> {
            UUID id = buf.readUuid();
            String nodeId = buf.readString();
            int choice = buf.readVarInt();
            server.execute(() ->
                    net.fugginbeenus.notchcurrency.npc.dialogue.NpcDialogueManager.choose(player, id, nodeId, choice));
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_DIALOGUE_TEMPLATE, (server, player, handler, buf, rs) -> {
            UUID id = buf.readUuid();
            server.execute(() -> {
                net.minecraft.entity.Entity e = player.getServerWorld().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.createDialogueTemplate(player, npc);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_DIALOGUE_CLEAR, (server, player, handler, buf, rs) -> {
            UUID id = buf.readUuid();
            server.execute(() -> {
                net.minecraft.entity.Entity e = player.getServerWorld().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.clearDialogue(player, npc);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_DIALOGUE_MODE, (server, player, handler, buf, rs) -> {
            UUID id = buf.readUuid();
            int mode = buf.readVarInt();
            server.execute(() -> {
                net.minecraft.entity.Entity e = player.getServerWorld().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setDialogueMode(player, npc, mode);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_STUDIO_OPEN, (server, player, handler, buf, rs) -> {
            UUID id = buf.readUuid();
            server.execute(() -> {
                net.minecraft.entity.Entity e = player.getServerWorld().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.openStudio(player, npc);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_STUDIO_SAVE, (server, player, handler, buf, rs) -> {
            UUID id = buf.readUuid();
            net.minecraft.nbt.NbtCompound tree = buf.readNbt();
            server.execute(() -> {
                net.minecraft.entity.Entity e = player.getServerWorld().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.saveDialogue(player, npc, tree);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_SET_BEHAVIOR, (server, player, handler, buf, rs) -> {
            UUID id = buf.readUuid();
            int mode = buf.readVarInt();
            int radius = buf.readVarInt();
            String followName = buf.readString(16);
            int movesBits = buf.readVarInt();
            server.execute(() -> {
                net.minecraft.entity.Entity e = player.getServerWorld().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setBehavior(player, npc, mode, radius, followName, movesBits);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_POSE_PART, (server, player, handler, buf, rs) -> {
            UUID id = buf.readUuid();
            int part = buf.readVarInt();
            int x = buf.readVarInt();
            int y = buf.readVarInt();
            int z = buf.readVarInt();
            server.execute(() -> {
                net.minecraft.entity.Entity e = player.getServerWorld().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setPosePart(player, npc, part, x, y, z);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_SET_ANIM, (server, player, handler, buf, rs) -> {
            UUID id = buf.readUuid();
            int anim = buf.readVarInt();
            server.execute(() -> {
                net.minecraft.entity.Entity e = player.getServerWorld().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setPoseAnim(player, npc, anim);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_TRANSFORM, (server, player, handler, buf, rs) -> {
            UUID id = buf.readUuid();
            double dx = buf.readDouble();
            double dy = buf.readDouble();
            double dz = buf.readDouble();
            float yaw = buf.readFloat();
            boolean applyYaw = buf.readBoolean();
            server.execute(() -> {
                net.minecraft.entity.Entity e = player.getServerWorld().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.transform(player, npc, dx, dy, dz, yaw, applyYaw);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_EDITOR_REOPEN, (server, player, handler, buf, rs) -> {
            UUID id = buf.readUuid();
            server.execute(() -> {
                net.minecraft.entity.Entity e = player.getServerWorld().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.openEditor(player, npc);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_SET_POSE, (server, player, handler, buf, rs) -> {
            UUID id = buf.readUuid();
            int pose = buf.readVarInt();
            server.execute(() -> {
                net.minecraft.entity.Entity e = player.getServerWorld().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setPose(player, npc, pose);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_PATROL, (server, player, handler, buf, rs) -> {
            UUID id = buf.readUuid();
            int action = buf.readVarInt();
            int value = buf.readVarInt();
            server.execute(() -> {
                net.minecraft.entity.Entity e = player.getServerWorld().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.patrolAction(player, npc, action, value);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_EQUIP, (server, player, handler, buf, rs) -> {
            UUID id = buf.readUuid();
            server.execute(() -> {
                net.minecraft.entity.Entity e = player.getServerWorld().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.openEquipScreen(player, npc);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_SET_STATS, (server, player, handler, buf, rs) -> {
            UUID id = buf.readUuid();
            int bits = buf.readVarInt();
            server.execute(() -> {
                net.minecraft.entity.Entity e = player.getServerWorld().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setStats(player, npc, bits);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_SET_ATTRS, (server, player, handler, buf, rs) -> {
            UUID id = buf.readUuid();
            int maxHealth = buf.readVarInt();
            int speedPct = buf.readVarInt();
            int regen = buf.readVarInt();
            server.execute(() -> {
                net.minecraft.entity.Entity e = player.getServerWorld().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setAttrs(player, npc, maxHealth, speedPct, regen);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.SHOP_MANAGE_ACTION, (server, player, handler, buf, rs) -> {
            int action = buf.readVarInt();
            String text = buf.readString(160);
            UUID listingId = buf.readBoolean() ? buf.readUuid() : null;
            server.execute(() -> {
                if (player.currentScreenHandler instanceof net.fugginbeenus.notchcurrency.shop.ShopManageScreenHandler h) {
                    h.handleAction(player, action, text, listingId);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.SHOP_EDIT_ACTION, (server, player, handler, buf, rs) -> {
            int action = buf.readVarInt();
            int price = buf.readVarInt();
            server.execute(() -> {
                if (player.currentScreenHandler instanceof net.fugginbeenus.notchcurrency.shop.ShopListingEditScreenHandler h) {
                    h.handleAction(player, action, price);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.TRADE_OFFER_CREATE, (server, player, handler, buf, rs) -> {
            long price = buf.readVarLong();
            long giveCoins = buf.readVarLong();
            String target = buf.readString(16);
            server.execute(() -> {
                if (player.currentScreenHandler instanceof net.fugginbeenus.notchcurrency.trade.TradeOfferCreateScreenHandler h) {
                    h.submit(player, price, giveCoins, target);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.TRADE_OFFER_ACTION, (server, player, handler, buf, rs) -> {
            java.util.UUID offerId = buf.readUuid();
            int action = buf.readVarInt();
            server.execute(() -> {
                if (action == 0) net.fugginbeenus.notchcurrency.trade.TradeOfferManager.accept(player, offerId);
                else if (action == 1) net.fugginbeenus.notchcurrency.trade.TradeOfferManager.cancel(player, offerId);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.COSMETIC_BUY, (server, player, handler, buf, rs) -> {
            String offerId = buf.readString(128);
            server.execute(() -> {
                if (player.currentScreenHandler instanceof net.fugginbeenus.notchcurrency.economy.cosmetic.CosmeticShopScreenHandler) {
                    net.fugginbeenus.notchcurrency.economy.cosmetic.CosmeticManager.buy(player, offerId);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.ENCHANTER_ACTION, (server, player, handler, buf, rs) -> {
            int action = buf.readVarInt();
            String enchId = buf.readString(128);
            server.execute(() -> {
                if (player.currentScreenHandler instanceof net.fugginbeenus.notchcurrency.economy.enchanter.EnchanterScreenHandler h) {
                    h.handleAction(player, action, enchId);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_PRESET, (server, player, handler, buf, rs) -> {
            UUID id = buf.readUuid();
            int action = buf.readVarInt();
            String name = buf.readString(64);
            server.execute(() -> {
                net.minecraft.entity.Entity e = player.getServerWorld().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NpcPresetManager.action(player, npc, action, name);
                }
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(NotchPackets.NPC_SET_APPEARANCE, (server, player, handler, buf, rs) -> {
            UUID id = buf.readUuid();
            String model = buf.readString();
            String skinType = buf.readString();
            String skinValue = buf.readString();
            boolean slim = buf.readBoolean();
            float scale = buf.readFloat();
            server.execute(() -> {
                net.minecraft.entity.Entity e = player.getServerWorld().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setAppearance(player, npc, model, skinType, skinValue, slim, scale);
                }
            });
        });

        // Server handles ATM withdraw requests (client -> server)
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.ATM_WITHDRAW,
                (server, player, handler, buf, response) -> {
                    int requested = buf.readVarInt();

                    server.execute(() -> {
                        if (!(player instanceof ServerPlayerEntity)) return;
                        ServerPlayerEntity sp = (ServerPlayerEntity) player;
                        if (requested <= 0) return;

                        long currentBal = BalanceStore.get(sp);
                        // requested is a count of physical coin items, so it fits in int.
                        int toWithdraw = (int) Math.min(currentBal, requested);
                        if (toWithdraw <= 0) {
                            sp.sendMessage(
                                    Text.literal("You don't have that many " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " in your account.")
                                            .formatted(Formatting.RED),
                                    false
                            );
                            return;
                        }

                        // Subtract from virtual balance and push the new balance to the
                        // client (HUD + ATM screen read it from there).
                        BalanceStore.subtract(sp, toWithdraw, net.fugginbeenus.notchcurrency.economy.TransactionReason.ATM_WITHDRAW, "ATM withdraw");
                        NotchPackets.sendBalance(sp, BalanceStore.get(sp));

                        // Give physical coins (prefer physical stacks)
                        CoinEconomy.give(sp, toWithdraw, false);

                        sp.sendMessage(
                                Text.literal("Withdrew " + toWithdraw + " ")
                                        .append(NotchCurrency.coinIcon())
                                        .formatted(Formatting.GREEN),
                                false
                        );
                    });
                }
        );
        // Handle shop purchase requests
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.SHOP_PURCHASE,
                (server, player, handler, buf, response) -> {
                    UUID shopId = buf.readUuid();
                    UUID listingId = buf.readUuid();
                    int quantity = buf.readVarInt();
                    // Note: useCoins boolean is still read for backwards compatibility but ignored
                    buf.readBoolean();

                    server.execute(() -> {
                        ServerPlayerEntity sp = player;
                        // Use unified purchase method that handles both coin AND barter
                        net.fugginbeenus.notchcurrency.shop.PlayerShopManager.PurchaseResult result =
                                net.fugginbeenus.notchcurrency.shop.PlayerShopManager.purchase(sp, shopId, listingId, quantity);

                        if (result != net.fugginbeenus.notchcurrency.shop.PlayerShopManager.PurchaseResult.SUCCESS) {
                            String errorMsg = switch (result) {
                                case SHOP_NOT_FOUND -> "Shop not found!";
                                case SHOP_CLOSED -> "This shop is currently closed.";
                                case OWN_SHOP -> "You can't buy from your own shop!";
                                case LISTING_NOT_FOUND -> "Item no longer available.";
                                case COINS_NOT_ACCEPTED -> "This item doesn't accept coin payment.";
                                case BARTER_NOT_ACCEPTED -> "This item doesn't accept barter.";
                                case INVALID_QUANTITY -> "Invalid quantity.";
                                case INSUFFICIENT_STOCK -> "Not enough stock available.";
                                case INSUFFICIENT_FUNDS -> "You don't have enough " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + "!";
                                case INSUFFICIENT_ITEMS -> "You don't have the required items!";
                                default -> "Purchase failed.";
                            };
                            sp.sendMessage(Text.literal(errorMsg).formatted(Formatting.RED), false);
                        }
                    });
                }
        );

        // Handle shop balance withdrawal
        ServerPlayNetworking.registerGlobalReceiver(
                NotchPackets.SHOP_WITHDRAW,
                (server, player, handler, buf, response) -> {
                    UUID shopId = buf.readUuid();

                    server.execute(() -> {
                        ServerPlayerEntity sp = player;
                        net.fugginbeenus.notchcurrency.shop.ShopState state =
                                net.fugginbeenus.notchcurrency.shop.ShopState.get(sp.getServerWorld());
                        net.fugginbeenus.notchcurrency.shop.PlayerShop shop = state.getShop(shopId);

                        if (shop == null) {
                            sp.sendMessage(Text.literal("Shop not found!").formatted(Formatting.RED), false);
                            return;
                        }

                        if (!shop.getOwnerId().equals(sp.getUuid())) {
                            sp.sendMessage(Text.literal("You don't own this shop!").formatted(Formatting.RED), false);
                            return;
                        }

                        long amount = shop.withdrawBalance();
                        java.util.List<ItemStack> barterItems = shop.collectPendingBarterItems();

                        boolean hadCoins = amount > 0;
                        boolean hadItems = !barterItems.isEmpty();

                        if (hadCoins) {
                            net.fugginbeenus.notchcurrency.api.CurrencyApi.deposit(sp, amount,
                                    net.fugginbeenus.notchcurrency.economy.TransactionReason.SHOP_PAYOUT, "shop withdrawal");
                        }

                        // Give barter items
                        for (ItemStack item : barterItems) {
                            if (!item.isEmpty()) {
                                int remaining = item.getCount();
                                while (remaining > 0) {
                                    int giveCount = Math.min(remaining, item.getMaxCount());
                                    ItemStack toGive = item.copy();
                                    toGive.setCount(giveCount);
                                    if (!sp.getInventory().insertStack(toGive)) {
                                        sp.dropItem(toGive, false);
                                    }
                                    remaining -= giveCount;
                                }
                            }
                        }

                        if (hadCoins || hadItems) {
                            MutableText message = Text.literal("Withdrew ");
                            if (hadCoins) {
                                message.append(coins(amount));
                            }
                            if (hadCoins && hadItems) {
                                message.append(Text.literal(" and "));
                            }
                            if (hadItems) {
                                int totalItems = barterItems.stream().mapToInt(ItemStack::getCount).sum();
                                message.append(Text.literal(totalItems + " barter items").formatted(Formatting.AQUA));
                            }
                            message.append(Text.literal(" from your shop!").formatted(Formatting.GREEN));
                            sp.sendMessage(message, false);
                            state.markDirtyAndSave();
                        } else {
                            sp.sendMessage(Text.literal("No balance to withdraw.").formatted(Formatting.YELLOW), false);
                        }
                    });
                }
        );
        // NOTE: SHOP_REMOVE_LISTING is registered once above (it routes through
        // PlayerShopManager.removeListing, which returns leftover stock to the owner).
        // A second, duplicate registration used to live here; Fabric silently ignores
        // duplicate receivers, so it was dead code and has been removed.
    }

}
