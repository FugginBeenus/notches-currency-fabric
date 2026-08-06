package net.fugginbeenus.notchcurrency.economy;

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
    SINK(false),
    FAUCET(false),
    ADMIN(true);

    private final boolean adminRelevant;

    TransactionReason(boolean adminRelevant) {
        this.adminRelevant = adminRelevant;
    }

    public boolean isAdminRelevant() {
        return adminRelevant;
    }
}
