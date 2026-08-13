package net.fugginbeenus.notchcurrency.net;

import net.fugginbeenus.notchcurrency.compat.Net;
import net.fugginbeenus.notchcurrency.compat.StackData;
import net.fugginbeenus.notchcurrency.core.BalanceStore;
import net.fugginbeenus.notchcurrency.core.CoinEconomy;
import net.fugginbeenus.notchcurrency.core.NotchCurrency;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.fugginbeenus.notchcurrency.auction.AuctionState;
import java.util.UUID;

import static net.fugginbeenus.notchcurrency.core.NotchCurrency.coinIcon;
import static net.fugginbeenus.notchcurrency.core.NotchCurrency.coins;

public final class ServerPacketHandlers {

    private ServerPacketHandlers() {}

    public static void register() {
        // Server handles client's explicit balance request
        Net.registerServerReceiver(
                NotchPackets.BALANCE_REQUEST,
                (server, player, buf) ->
                        server.execute(() ->
                                NotchPackets.sendBalance(player, BalanceStore.get(player)))
        );

        // Server handles client's bid request (from right-click GUI)
        Net.registerServerReceiver(
                NotchPackets.BID_REQUEST,
                (server, player, buf) -> {
                    UUID listingId = buf.readUUID();
                    long bidAmount = buf.readVarLong();

                    server.execute(() -> {
                        // player is already a ServerPlayer here
                        ServerPlayer sp = player;
                        ServerLevel world = sp.serverLevel();
                        AuctionState state = AuctionState.get(world);
                        state.placeBid(world, sp, listingId, bidAmount);
                    });
                }
        );

        // Server handles cancel-listing requests from the AH "My Listings" popup
        Net.registerServerReceiver(
                NotchPackets.AUCTION_CANCEL,
                (server, player, buf) -> {
                    UUID listingId = buf.readUUID();

                    server.execute(() -> {
                        ServerPlayer sp = player;
                        ServerLevel world = sp.serverLevel();
                        AuctionState state = AuctionState.get(world);
                        net.fugginbeenus.notchcurrency.auction.AuctionListing l = state.getListing(listingId);

                        if (l == null) {
                            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("That listing is no longer available.").withStyle(ChatFormatting.RED));
                            return;
                        }
                        if (!sp.getUUID().equals(l.sellerUuid)) {
                            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Only the seller can cancel this listing.").withStyle(ChatFormatting.RED));
                            return;
                        }
                        // Refund any standing bid before removing: bids escrow coins.
                        long refunded = state.refundHighestBid(world, l);
                        state.removeListing(listingId);
                        if (refunded > 0) {
                            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("The high bidder was refunded " + refunded + " " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + ".")
                                    .withStyle(ChatFormatting.GRAY));
                        }

                        ItemStack toReturn = l.stack.copy();
                        if (StackData.hasData(toReturn)) {
                            net.minecraft.nbt.CompoundTag tag = StackData.editData(toReturn);
                            tag.remove("nc_price");
                            tag.remove("nc_seller");
                            tag.remove("nc_created");
                            tag.remove("nc_expires");
                            tag.remove("nc_highest_bid");
                            tag.remove("nc_highest_bidder");
                            tag.remove("nc_listing_id");
                            if (tag.isEmpty()) StackData.clearData(toReturn);
                            else StackData.commitData(toReturn, tag);
                        }
                        if (!sp.getInventory().add(toReturn) && !toReturn.isEmpty()) {
                            sp.drop(toReturn, false);
                        }

                        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Cancelled listing for ").withStyle(ChatFormatting.GREEN)
                                .append(l.stack.getHoverName().copy().withStyle(ChatFormatting.YELLOW))
                                .append(Component.literal(" - item returned.").withStyle(ChatFormatting.GREEN)));

                        // Refresh the open Auction House so the popup updates live.
                        if (sp.containerMenu instanceof net.fugginbeenus.notchcurrency.auction.AuctionHouseScreenHandler ah) {
                            ah.reload();
                            ah.broadcastChanges();
                        }
                    });
                }
        );

        // Server handles create-listing from the "List an Item" screen
        Net.registerServerReceiver(
                NotchPackets.AUCTION_LIST,
                (server, player, buf) -> {
                    long price = buf.readVarLong();
                    int days = buf.readVarInt();
                    server.execute(() -> {
                        if (player.containerMenu
                                instanceof net.fugginbeenus.notchcurrency.auction.AuctionListingScreenHandler listing) {
                            listing.listFromInput(player, price, days);
                        }
                    });
                }
        );

        // Server handles raffle admin GUI "Save & Apply" (op-only)
        Net.registerServerReceiver(
                NotchPackets.RAFFLE_ADMIN_SAVE,
                (server, player, buf) -> {
                    long price = buf.readVarLong();
                    int cut = buf.readVarInt();
                    int intervalDays = buf.readVarInt();
                    boolean enabled = buf.readBoolean();
                    long coinsPool = buf.readVarLong();
                    server.execute(() -> {
                        if (!net.fugginbeenus.notchcurrency.compat.Perms.isOperator(player)) return;
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
                            net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("Raffle cancelled - entries & pot cleared, prize returned, tickets voided.")
                                    .withStyle(ChatFormatting.YELLOW));
                            return;
                        }

                        // Starting a raffle with leftover ticket state → clear the old pot/entries
                        // (keeps the prize you're configuring) so it doesn't begin with a stale pot.
                        if (!wasEnabled && enabled && (state.getPot() > 0 || state.getTotalTickets() > 0)) {
                            state.clearEntries();
                        }

                        state.setCoinsPool(coinsPool);
                        if (player.containerMenu
                                instanceof net.fugginbeenus.notchcurrency.economy.raffle.RaffleAdminScreenHandler h) {
                            h.applyPrizeFromInput(player);
                        }
                        net.fugginbeenus.notchcurrency.economy.raffle.RaffleManager.refreshAllOnline(server);
                        net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("Raffle settings saved & applied.").withStyle(ChatFormatting.GREEN));
                    });
                }
        );

        // Server handles bounty board actions (take / claim / turn in) from the GUI
        Net.registerServerReceiver(
                NotchPackets.BOUNTY_ACTION,
                (server, player, buf) -> {
                    UUID bountyId = buf.readUUID();
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
        Net.registerServerReceiver(
                NotchPackets.BOUNTY_ADMIN_SAVE,
                (server, player, buf) -> {
                    boolean enabled = buf.readBoolean();
                    int activeCount = buf.readVarInt();
                    int takeLimit = buf.readVarInt();
                    int durationMinutes = buf.readVarInt();
                    server.execute(() -> {
                        if (!net.fugginbeenus.notchcurrency.compat.Perms.isOperator(player)) return;
                        net.fugginbeenus.notchcurrency.config.NotchConfig cfg =
                                net.fugginbeenus.notchcurrency.config.NotchConfigIO.get();
                        cfg.bounty.enabled = enabled;
                        cfg.bounty.activeCount = Math.max(0, Math.min(20, activeCount));
                        cfg.bounty.takeLimit = Math.max(1, Math.min(5, takeLimit));
                        cfg.bounty.durationMinutes = Math.max(1, durationMinutes);
                        net.fugginbeenus.notchcurrency.config.NotchConfigIO.save(cfg);
                        net.fugginbeenus.notchcurrency.economy.bounty.BountyManager.applyConfig(cfg);
                        if (player.containerMenu
                                instanceof net.fugginbeenus.notchcurrency.economy.bounty.BountyAdminScreenHandler h) {
                            h.persistDecrees(player);
                        }
                        net.fugginbeenus.notchcurrency.compat.Msg.chat(player, Component.literal("Bounty settings saved & applied.").withStyle(ChatFormatting.GREEN));
                    });
                }
        );

        // Server handles loan GUI borrow/repay
        Net.registerServerReceiver(
                NotchPackets.LOAN_ACTION,
                (server, player, buf) -> {
                    int action = buf.readVarInt();
                    long amount = buf.readVarLong();
                    server.execute(() -> {
                        if (action == 0) net.fugginbeenus.notchcurrency.economy.loan.LoanManager.borrow(player, amount);
                        else net.fugginbeenus.notchcurrency.economy.loan.LoanManager.repay(player, amount);
                    });
                }
        );

        // Server handles slot-machine spins (bet typed in the GUI)
        Net.registerServerReceiver(
                NotchPackets.SLOTS_SPIN,
                (server, player, buf) -> {
                    long bet = buf.readVarLong();
                    server.execute(() -> {
                        if (player.containerMenu
                                instanceof net.fugginbeenus.notchcurrency.economy.gambling.SlotMachineScreenHandler h) {
                            h.spin(bet);
                        }
                    });
                }
        );

        // Server handles coin-flip bets (side + bet from the GUI; block does the reveal)
        Net.registerServerReceiver(
                NotchPackets.COINFLIP_FLIP,
                (server, player, buf) -> {
                    boolean guessHeads = buf.readBoolean();
                    long bet = buf.readVarLong();
                    server.execute(() ->
                            net.fugginbeenus.notchcurrency.economy.gambling.CoinFlipManager.flipFromScreen(player, guessHeads, bet));
                }
        );

        // ---- Notch NPC editor (owner/op re-checked inside NotchNpcManager) ----
        Net.registerServerReceiver(NotchPackets.NPC_SET_ROLE, (server, player, buf) -> {
            UUID id = buf.readUUID();
            int ord = buf.readVarInt();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (!(e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc)) return;
                net.fugginbeenus.notchcurrency.economy.npc.NpcRole[] all =
                        net.fugginbeenus.notchcurrency.economy.npc.NpcRole.values();
                net.fugginbeenus.notchcurrency.economy.npc.NpcRole role =
                        (ord >= 0 && ord < all.length) ? all[ord] : net.fugginbeenus.notchcurrency.economy.npc.NpcRole.NONE;
                net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setRole(player, npc, role);
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_SET_NAME, (server, player, buf) -> {
            UUID id = buf.readUUID();
            String name = buf.readUtf(64); // editor field caps at 48; wire cap is belt-and-suspenders
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setName(player, npc, name);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_SET_FAREWELL, (server, player, buf) -> {
            UUID id = buf.readUUID();
            String text = buf.readUtf(160);
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setFarewell(player, npc, text);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_PICKUP, (server, player, buf) -> {
            UUID id = buf.readUUID();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.pickUp(player, npc);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_DELETE, (server, player, buf) -> {
            UUID id = buf.readUUID();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.delete(player, npc);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_DIALOGUE_CHOICE, (server, player, buf) -> {
            UUID id = buf.readUUID();
            String nodeId = buf.readUtf(64); // page ids are <=24 chars of [a-z0-9_]
            int choice = buf.readVarInt();
            server.execute(() ->
                    net.fugginbeenus.notchcurrency.npc.dialogue.NpcDialogueManager.choose(player, id, nodeId, choice));
        });

        Net.registerServerReceiver(NotchPackets.NPC_DIALOGUE_TEMPLATE, (server, player, buf) -> {
            UUID id = buf.readUUID();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.createDialogueTemplate(player, npc);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_DIALOGUE_CLEAR, (server, player, buf) -> {
            UUID id = buf.readUUID();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.clearDialogue(player, npc);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_DIALOGUE_MODE, (server, player, buf) -> {
            UUID id = buf.readUUID();
            int mode = buf.readVarInt();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setDialogueMode(player, npc, mode);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_STUDIO_OPEN, (server, player, buf) -> {
            UUID id = buf.readUUID();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.openStudio(player, npc);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_STUDIO_SAVE, (server, player, buf) -> {
            UUID id = buf.readUUID();
            net.minecraft.nbt.CompoundTag tree = buf.readNbt();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.saveDialogue(player, npc, tree);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_BILLBOARD, (server, player, buf) -> {
            UUID id = buf.readUUID();
            String text = buf.readUtf(400);
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setBillboard(player, npc, text);
                }
            });
        });

        // Switching tabs. The aim comes back with it so the Outbox does not forget who the parcel
        // was for, and it is only ever used to preselect a name the sender still has to send to.
        Net.registerServerReceiver(NotchPackets.MAIL_TAB, (server, player, buf) -> {
            int tab = buf.readVarInt();
            UUID aim = buf.readBoolean() ? buf.readUUID() : null;
            server.execute(() -> {
                if (tab == 0) {
                    net.fugginbeenus.notchcurrency.mail.MailManager.openInbox(player);
                } else {
                    net.fugginbeenus.notchcurrency.mail.MailManager.openPost(player, aim);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.MAIL_POST_OPEN, (server, player, buf) ->
                server.execute(() ->
                        net.fugginbeenus.notchcurrency.mail.MailManager.openPost(player, null)));

        // Posting a parcel. The items come from the open screen, never from the packet, so a
        // crafted packet cannot conjure anything the sender did not actually put in.
        Net.registerServerReceiver(NotchPackets.MAIL_SEND, (server, player, buf) -> {
            UUID recipient = buf.readUUID();
            String note = buf.readUtf(128);
            long coins = buf.readVarLong();
            server.execute(() -> {
                if (player.containerMenu instanceof
                        net.fugginbeenus.notchcurrency.mail.MailPostScreenHandler parcel) {
                    net.fugginbeenus.notchcurrency.mail.MailManager.send(
                            player, recipient, note, coins, parcel);
                }
            });
        });

        // Starting a live trade from the mailbox. It goes through the same invite as /trade, so
        // the other player still has to agree before either inventory is on the table.
        Net.registerServerReceiver(NotchPackets.MAIL_TRADE, (server, player, buf) -> {
            UUID target = buf.readUUID();
            server.execute(() -> {
                net.minecraft.server.level.ServerPlayer other = server.getPlayerList().getPlayer(target);
                if (other == null) {
                    net.fugginbeenus.notchcurrency.compat.Msg.chat(player,
                            net.minecraft.network.chat.Component.literal("They are not online to trade.")
                                    .withStyle(net.minecraft.ChatFormatting.RED));
                    return;
                }
                player.closeContainer();
                net.fugginbeenus.notchcurrency.trade.TradeManager.invite(player, other);
            });
        });

        // Take all: hands over the parcels on screen, as many as the player can hold.
        Net.registerServerReceiver(NotchPackets.MAIL_TAKE, (server, player, buf) -> {
            buf.readUUID(); // kept so older clients still parse; the inbox has one button
            server.execute(() -> {
                if (player.containerMenu instanceof
                        net.fugginbeenus.notchcurrency.mail.MailInboxMenu inbox) {
                    inbox.takeAll(player);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_FACTION_PICK, (server, player, buf) -> {
            UUID id = buf.readUUID();
            int action = buf.readVarInt();
            String factionId = buf.readUtf(32);
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    if (action == net.fugginbeenus.notchcurrency.npc.faction.RecruiterManager.PICK_LIST) {
                        net.fugginbeenus.notchcurrency.npc.faction.RecruiterManager.sendList(player, npc);
                    } else {
                        net.fugginbeenus.notchcurrency.npc.faction.RecruiterManager.pick(player, npc, action, factionId);
                    }
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_RECRUITER_ACTION, (server, player, buf) -> {
            UUID id = buf.readUUID();
            int action = buf.readVarInt();
            String name = buf.readUtf(32);
            String color = buf.readUtf(24);
            int fee = buf.readVarInt();
            boolean open = buf.readBoolean();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc
                        && player.distanceToSqr(npc) <= 64.0) { // same 8-block reach as dialogue
                    net.fugginbeenus.notchcurrency.npc.faction.RecruiterManager.act(player, npc, action,
                            name, color, fee, open);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_ACTIONS_OPEN, (server, player, buf) -> {
            UUID id = buf.readUUID();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.openActions(player, npc);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_ACTIONS_SAVE, (server, player, buf) -> {
            UUID id = buf.readUUID();
            net.minecraft.nbt.CompoundTag actions = buf.readNbt();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.saveActions(player, npc, actions);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_SET_BEHAVIOR, (server, player, buf) -> {
            UUID id = buf.readUUID();
            int mode = buf.readVarInt();
            int radius = buf.readVarInt();
            String followName = buf.readUtf(16);
            int movesBits = buf.readVarInt();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setBehavior(player, npc, mode, radius, followName, movesBits);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_POSE_PART, (server, player, buf) -> {
            UUID id = buf.readUUID();
            int part = buf.readVarInt();
            int x = buf.readVarInt();
            int y = buf.readVarInt();
            int z = buf.readVarInt();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setPosePart(player, npc, part, x, y, z);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_SET_ANIM, (server, player, buf) -> {
            UUID id = buf.readUUID();
            int anim = buf.readVarInt();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setPoseAnim(player, npc, anim);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_SET_CLIP, (server, player, buf) -> {
            UUID id = buf.readUUID();
            String clip = buf.readUtf(128);
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setCustomClip(player, npc, clip);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_TRANSFORM, (server, player, buf) -> {
            UUID id = buf.readUUID();
            double dx = buf.readDouble();
            double dy = buf.readDouble();
            double dz = buf.readDouble();
            float yaw = buf.readFloat();
            boolean applyYaw = buf.readBoolean();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.transform(player, npc, dx, dy, dz, yaw, applyYaw);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_EDITOR_REOPEN, (server, player, buf) -> {
            UUID id = buf.readUUID();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.openEditor(player, npc);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_SET_POSE, (server, player, buf) -> {
            UUID id = buf.readUUID();
            int pose = buf.readVarInt();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setPose(player, npc, pose);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_PATROL, (server, player, buf) -> {
            UUID id = buf.readUUID();
            int action = buf.readVarInt();
            int value = buf.readVarInt();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.patrolAction(player, npc, action, value);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_EQUIP, (server, player, buf) -> {
            UUID id = buf.readUUID();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.openEquipScreen(player, npc);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_SET_STATS, (server, player, buf) -> {
            UUID id = buf.readUUID();
            int bits = buf.readVarInt();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setStats(player, npc, bits);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_SET_ATTRS, (server, player, buf) -> {
            UUID id = buf.readUUID();
            int maxHealth = buf.readVarInt();
            int speedPct = buf.readVarInt();
            int regen = buf.readVarInt();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setAttrs(player, npc, maxHealth, speedPct, regen);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.SHOP_MANAGE_ACTION, (server, player, buf) -> {
            int action = buf.readVarInt();
            String text = buf.readUtf(160);
            UUID listingId = buf.readBoolean() ? buf.readUUID() : null;
            server.execute(() -> {
                if (player.containerMenu instanceof net.fugginbeenus.notchcurrency.shop.ShopManageScreenHandler h) {
                    h.handleAction(player, action, text, listingId);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.SHOP_EDIT_ACTION, (server, player, buf) -> {
            int action = buf.readVarInt();
            int price = buf.readVarInt();
            server.execute(() -> {
                if (player.containerMenu instanceof net.fugginbeenus.notchcurrency.shop.ShopListingEditScreenHandler h) {
                    h.handleAction(player, action, price);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.TRADE_OFFER_CREATE, (server, player, buf) -> {
            long price = buf.readVarLong();
            long giveCoins = buf.readVarLong();
            String target = buf.readUtf(16);
            server.execute(() -> {
                if (player.containerMenu instanceof net.fugginbeenus.notchcurrency.trade.TradeOfferCreateScreenHandler h) {
                    h.submit(player, price, giveCoins, target);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.TRADE_OFFER_ACTION, (server, player, buf) -> {
            java.util.UUID offerId = buf.readUUID();
            int action = buf.readVarInt();
            server.execute(() -> {
                if (action == 0) net.fugginbeenus.notchcurrency.trade.TradeOfferManager.accept(player, offerId);
                else if (action == 1) net.fugginbeenus.notchcurrency.trade.TradeOfferManager.cancel(player, offerId);
            });
        });

        Net.registerServerReceiver(NotchPackets.COSMETIC_BUY, (server, player, buf) -> {
            String offerId = buf.readUtf(128);
            server.execute(() -> {
                if (player.containerMenu instanceof net.fugginbeenus.notchcurrency.economy.cosmetic.CosmeticShopScreenHandler) {
                    net.fugginbeenus.notchcurrency.economy.cosmetic.CosmeticManager.buy(player, offerId);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.ENCHANTER_ACTION, (server, player, buf) -> {
            int action = buf.readVarInt();
            String enchId = buf.readUtf(128);
            server.execute(() -> {
                if (player.containerMenu instanceof net.fugginbeenus.notchcurrency.economy.enchanter.EnchanterScreenHandler h) {
                    h.handleAction(player, action, enchId);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_PRESET, (server, player, buf) -> {
            UUID id = buf.readUUID();
            int action = buf.readVarInt();
            String name = buf.readUtf(64);
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NpcPresetManager.action(player, npc, action, name);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_SCHEDULE_OPEN, (server, player, buf) -> {
            UUID id = buf.readUUID();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.openSchedule(player, npc);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_SCHEDULE_SAVE, (server, player, buf) -> {
            UUID id = buf.readUUID();
            net.minecraft.nbt.CompoundTag nbt = buf.readNbt();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.saveSchedule(player, npc, nbt);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_SET_FLAVOR, (server, player, buf) -> {
            UUID id = buf.readUUID();
            String subtitle = buf.readUtf(64);
            String voice = buf.readUtf(128);
            int pitch = buf.readVarInt();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setFlavor(player, npc, subtitle, voice, pitch);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_SCHEDULE_TOOL, (server, player, buf) -> {
            UUID id = buf.readUUID();
            int entryIndex = buf.readVarInt();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.giveScheduleTool(player, npc, entryIndex);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_SHARE, (server, player, buf) -> {
            UUID id = buf.readUUID();
            int action = buf.readVarInt();
            // Read the payload on the network thread, but cap it here: this is the one packet whose
            // contents come from another player's clipboard.
            String payload = buf.readUtf(net.fugginbeenus.notchcurrency.npc.NpcShareCodec.MAX_WIRE_CHARS);
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NpcShareManager.action(player, npc, action, payload);
                }
            });
        });

        Net.registerServerReceiver(NotchPackets.NPC_SET_APPEARANCE, (server, player, buf) -> {
            UUID id = buf.readUUID();
            String model = buf.readUtf(64);
            String skinType = buf.readUtf(16);
            String skinValue = buf.readUtf(256); // player name or skin URL (client field caps at 256)
            boolean slim = buf.readBoolean();
            float scaleX = buf.readFloat();
            float scaleY = buf.readFloat();
            float scaleZ = buf.readFloat();
            float nameOffset = buf.readFloat();
            server.execute(() -> {
                net.minecraft.world.entity.Entity e = player.serverLevel().getEntity(id);
                if (e instanceof net.fugginbeenus.notchcurrency.entity.NotchNpcEntity npc) {
                    net.fugginbeenus.notchcurrency.npc.NotchNpcManager.setAppearance(player, npc, model,
                            skinType, skinValue, slim, scaleX, scaleY, scaleZ, nameOffset);
                }
            });
        });

        // Server handles ATM withdraw requests (client -> server)
        Net.registerServerReceiver(
                NotchPackets.ATM_WITHDRAW,
                (server, player, buf) -> {
                    int requested = buf.readVarInt();

                    server.execute(() -> {
                        if (!(player instanceof ServerPlayer)) return;
                        ServerPlayer sp = (ServerPlayer) player;
                        if (requested <= 0) return;

                        long currentBal = BalanceStore.get(sp);
                        // requested is a count of physical coin items, so it fits in int.
                        int toWithdraw = (int) Math.min(currentBal, requested);
                        if (toWithdraw <= 0) {
                            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("You don't have that many " + net.fugginbeenus.notchcurrency.core.CurrencyText.word() + " in your account.")
                                            .withStyle(ChatFormatting.RED));
                            return;
                        }

                        // Subtract from virtual balance and push the new balance to the
                        // client (HUD + ATM screen read it from there).
                        BalanceStore.subtract(sp, toWithdraw, net.fugginbeenus.notchcurrency.economy.TransactionReason.ATM_WITHDRAW, "ATM withdraw");
                        NotchPackets.sendBalance(sp, BalanceStore.get(sp));

                        // Give physical coins (prefer physical stacks)
                        CoinEconomy.give(sp, toWithdraw, false);

                        net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Withdrew " + toWithdraw + " ")
                                        .append(NotchCurrency.coinIcon())
                                        .withStyle(ChatFormatting.GREEN));
                    });
                }
        );
        // Handle shop purchase requests
        Net.registerServerReceiver(
                NotchPackets.SHOP_PURCHASE,
                (server, player, buf) -> {
                    UUID shopId = buf.readUUID();
                    UUID listingId = buf.readUUID();
                    int quantity = buf.readVarInt();
                    // Note: useCoins boolean is still read for backwards compatibility but ignored
                    buf.readBoolean();

                    server.execute(() -> {
                        ServerPlayer sp = player;
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
                            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal(errorMsg).withStyle(ChatFormatting.RED));
                        }
                    });
                }
        );

        // Handle shop balance withdrawal
        Net.registerServerReceiver(
                NotchPackets.SHOP_WITHDRAW,
                (server, player, buf) -> {
                    UUID shopId = buf.readUUID();

                    server.execute(() -> {
                        ServerPlayer sp = player;
                        net.fugginbeenus.notchcurrency.shop.ShopState state =
                                net.fugginbeenus.notchcurrency.shop.ShopState.get(sp.serverLevel());
                        net.fugginbeenus.notchcurrency.shop.PlayerShop shop = state.getShop(shopId);

                        if (shop == null) {
                            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("Shop not found!").withStyle(ChatFormatting.RED));
                            return;
                        }

                        if (!shop.getOwnerId().equals(sp.getUUID())) {
                            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("You don't own this shop!").withStyle(ChatFormatting.RED));
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
                                    int giveCount = Math.min(remaining, item.getMaxStackSize());
                                    ItemStack toGive = item.copy();
                                    toGive.setCount(giveCount);
                                    if (!sp.getInventory().add(toGive)) {
                                        sp.drop(toGive, false);
                                    }
                                    remaining -= giveCount;
                                }
                            }
                        }

                        if (hadCoins || hadItems) {
                            MutableComponent message = Component.literal("Withdrew ");
                            if (hadCoins) {
                                message.append(coins(amount));
                            }
                            if (hadCoins && hadItems) {
                                message.append(Component.literal(" and "));
                            }
                            if (hadItems) {
                                int totalItems = barterItems.stream().mapToInt(ItemStack::getCount).sum();
                                message.append(Component.literal(totalItems + " barter items").withStyle(ChatFormatting.AQUA));
                            }
                            message.append(Component.literal(" from your shop!").withStyle(ChatFormatting.GREEN));
                            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, message);
                            state.markDirtyAndSave();
                        } else {
                            net.fugginbeenus.notchcurrency.compat.Msg.chat(sp, Component.literal("No balance to withdraw.").withStyle(ChatFormatting.YELLOW));
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
