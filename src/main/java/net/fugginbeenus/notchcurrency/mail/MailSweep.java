package net.fugginbeenus.notchcurrency.mail;

import net.fugginbeenus.notchcurrency.auction.AuctionState;
import net.fugginbeenus.notchcurrency.trade.TradeOfferState;
import net.minecraft.server.MinecraftServer;

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
