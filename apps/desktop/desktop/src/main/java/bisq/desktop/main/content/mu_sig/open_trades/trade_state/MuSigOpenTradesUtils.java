package bisq.desktop.main.content.mu_sig.open_trades.trade_state;

import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.common.encoding.Csv;
import bisq.common.file.FileUtils;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.contract.mu_sig.MuSigContract;
import bisq.desktop.common.utils.FileChooserUtil;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.support.mediation.MediationRequestService;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.user.profile.UserProfile;
import javafx.scene.Scene;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
public class MuSigOpenTradesUtils {

    public static void exportTrade(MuSigTrade trade, Scene scene) {
        try {
            String tradeId = trade.getId();
            String quoteCurrencyCode = trade.getOffer().getMarket().getQuoteCurrencyCode();
            MuSigContract contract = trade.getContract();
            long baseSideAmount = contract.getBaseSideAmount();
            long quoteSideAmount = contract.getQuoteSideAmount();
            String formattedBaseAmount = AmountFormatter.formatBaseAmountWithCode(Coin.asBtcFromValue(baseSideAmount));
            String formattedQuoteAmount = AmountFormatter.formatQuoteAmountWithCode(Fiat.from(quoteSideAmount, quoteCurrencyCode));
            String paymentProof = Optional.ofNullable(trade.getDepositTxId()).orElseGet(() -> Res.get("data.na"));
            String bitcoinMethod = contract.getBaseSidePaymentMethodSpec().getDisplayString();
            String fiatMethod = contract.getQuoteSidePaymentMethodSpec().getDisplayString();
            String paymentMethod = bitcoinMethod + " / " + fiatMethod;
            List<String> headers = List.of(
                    Res.get("bisqEasy.openTrades.table.tradeId"),
                    Res.get("bisqEasy.openTrades.table.baseAmount"),
                    Res.get("bisqEasy.openTrades.csv.quoteAmount", quoteCurrencyCode),
                    Res.get("bisqEasy.openTrades.csv.txIdOrPreimage"),
                    Res.get("bisqEasy.openTrades.csv.paymentMethod")
            );
            List<List<String>> tradeData = List.of(
                    List.of(
                            tradeId,
                            formattedBaseAmount,
                            formattedQuoteAmount,
                            paymentProof,
                            paymentMethod
                    )
            );
            String csv = Csv.toCsv(headers, tradeData);
            String initialFileName = "BisqEasy-trade-" + trade.getShortId() + ".csv";
            FileChooserUtil.saveFile(scene, initialFileName)
                    .ifPresent(file -> {
                        try {
                            FileUtils.writeToFile(csv, file);
                        } catch (IOException e) {
                            new Popup().error(e).show();
                        }
                    });
        } catch (Exception e) {
            log.error("Error exporting trade {}", trade, e);
        }
    }

    public static void requestMediation(MuSigOpenTradeChannel channel,
                                        MuSigContract contract,
                                        MediationRequestService mediationRequestService,
                                        MuSigOpenTradeChannelService channelService) {
        Optional<UserProfile> mediator = channel.getMediator();
        if (mediator.isPresent()) {
            new Popup().headline(Res.get("bisqEasy.mediation.request.confirm.headline"))
                    .information(Res.get("bisqEasy.mediation.request.confirm.msg"))
                    .actionButtonText(Res.get("bisqEasy.mediation.request.confirm.openMediation"))
                    .onAction(() -> {
                        String encoded = Res.encode("bisqEasy.mediation.requester.tradeLogMessage", channel.getMyUserIdentity().getUserName());
                        channelService.sendTradeLogMessage(encoded, channel);
                        channel.setIsInMediation(true);
                        channelService.persist();
                        mediationRequestService.requestMediation(channel, contract);
                        new Popup().headline(Res.get("bisqEasy.mediation.request.feedback.headline"))
                                .feedback(Res.get("bisqEasy.mediation.request.feedback.msg"))
                                .show();
                    })
                    .closeButtonText(Res.get("action.cancel"))
                    .show();
        } else {
            new Popup().warning(Res.get("bisqEasy.mediation.request.feedback.noMediatorAvailable")).show();
        }
    }
}
