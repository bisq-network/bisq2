package bisq.desktop.main.content.mu_sig.trade.pending.trade_details;

class MuSigTradeDetailsRecords {

    record SecurityDepositInfo(double percentValue, String amountText, String percentText, boolean isMatching) {
    }

    record TradeFeeInfo(String amountText, String percentText, boolean showPercent) {
    }
}
