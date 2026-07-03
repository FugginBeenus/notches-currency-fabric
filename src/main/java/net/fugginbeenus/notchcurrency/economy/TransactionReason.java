package net.fugginbeenus.notchcurrency.economy;

/**
 * Why a balance changed. Recorded with every ledger entry so admins can tell faucets
 * (money created) from sinks (money destroyed) and trace dupes.
 *
 * {@code adminRelevant} marks reasons that should always mirror to the Discord webhook
 * regardless of amount (e.g. operator adjustments).
 */
public enum TransactionReason {
    UNSPECIFIED(false),
    PAY(false),
    TRADE(false),
    ATM_DEPOSIT(false),
    ATM_WITHDRAW(false),
    SHOP_SALE(false),
    SHOP_PAYOUT(false),
    AUCTION(false),
    AUCTION_BID(false),
    AUCTION_REFUND(false),
    BOUNTY(false),
    RAFFLE(false),
    /** Money destroyed by a sink (tax, listing fee, rent, gambling loss, ...). */
    SINK(false),
    /** Money created by a faucet (boss drop, crate, daily reward, ...). */
    FAUCET(false),
    /** Operator adjustment via /eco. Always webhook-relevant. */
    ADMIN(true);

    private final boolean adminRelevant;

    TransactionReason(boolean adminRelevant) {
        this.adminRelevant = adminRelevant;
    }

    public boolean isAdminRelevant() {
        return adminRelevant;
    }
}
