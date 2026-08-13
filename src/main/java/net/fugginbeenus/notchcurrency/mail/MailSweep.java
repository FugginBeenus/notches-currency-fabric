package net.fugginbeenus.notchcurrency.mail;

import net.fugginbeenus.notchcurrency.auction.AuctionState;
import net.fugginbeenus.notchcurrency.trade.TradeOfferState;
import net.minecraft.server.MinecraftServer;

/**
 * Empties the older piles into the one inbox.
 *
 * <p>Auction payouts and the item half of an offline trade offer each used to wait in their own
 * store, claimed by their own command. Both now post to the mail, but a world that has been played
 * already has obligations sitting in the old places, and those still have to reach someone.
 *
 * <p>Running this repeatedly is safe and cheap: it clears what it moves, and does nothing at all
 * once both piles are empty, which is the normal case on any world that has been swept once.
 */
public final class MailSweep {

    private MailSweep() {}

    public static int run(MinecraftServer server) {
        if (server == null) return 0;
        int posted = 0;
        posted += AuctionState.get(server.overworld()).drainIntoMail(server);
        posted += TradeOfferState.get(server).drainIntoMail(server);
        return posted;
    }
}
