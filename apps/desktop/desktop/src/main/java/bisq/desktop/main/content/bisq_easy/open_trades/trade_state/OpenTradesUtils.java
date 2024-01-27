package bisq.desktop.main.content.bisq_easy.open_trades.trade_state;

import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannel;
import bisq.common.encoding.Csv;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.util.FileUtils;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.desktop.common.utils.FileChooserUtil;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.support.mediation.MediationRequestService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.user.profile.UserProfile;
import javafx.scene.Scene;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

public class OpenTradesUtils {

    public static void exportTrade(BisqEasyTrade trade, Scene scene) {
        String tradeId = trade.getId();
        String quoteCurrencyCode = trade.getOffer().getMarket().getQuoteCurrencyCode();
        BisqEasyContract contract = trade.getContract();
        long baseSideAmount = contract.getBaseSideAmount();
        long quoteSideAmount = contract.getQuoteSideAmount();
        String formattedBaseAmount = AmountFormatter.formatAmountWithCode(Coin.asBtcFromValue(baseSideAmount));
        String formattedQuoteAmount = AmountFormatter.formatAmountWithCode(Fiat.from(quoteSideAmount, quoteCurrencyCode));
        String txId = Optional.ofNullable(trade.getTxId().get()).orElse("");
        String btcAddress = Optional.ofNullable(trade.getBtcAddress().get()).orElse("");
        String displayString = contract.getQuoteSidePaymentMethodSpec().getDisplayString();

        List<List<String>> tradeData = List.of(
                List.of(
                        "Trade ID",
                        "BTC amount",
                        quoteCurrencyCode + " amount",
                        "Transaction ID",
                        "Receiver address",
                        "Payment method"
                ),
                List.of(
                        tradeId,
                        formattedBaseAmount,
                        formattedQuoteAmount,
                        txId,
                        btcAddress,
                        displayString
                )
        );

        String csv = Csv.toCsv(tradeData);
        File directory = FileChooserUtil.chooseDirectory(scene, "");
        if (directory != null) {
            try {
                File file = new File(directory, "BisqEasyTrade_" + trade.getShortId() + ".csv");
                FileUtils.writeToFile(csv, file);
            } catch (IOException e) {
                new Popup().error(e).show();
            }
        }
    }

    public static void reportToMediator(BisqEasyOpenTradeChannel channel,
                                        BisqEasyContract contract,
                                        MediationRequestService mediationRequestService) {
        openDispute(channel, contract, mediationRequestService);
    }

    public static void requestMediation(BisqEasyOpenTradeChannel channel,
                                        BisqEasyContract contract,
                                        MediationRequestService mediationRequestService) {
        openDispute(channel, contract, mediationRequestService);
    }

    private static void openDispute(BisqEasyOpenTradeChannel channel, BisqEasyContract contract, MediationRequestService mediationRequestService) {
        Optional<UserProfile> mediator = channel.getMediator();
        if (mediator.isPresent()) {
            new Popup().headline(Res.get("bisqEasy.mediation.request.confirm.headline"))
                    .information(Res.get("bisqEasy.mediation.request.confirm.msg"))
                    .actionButtonText(Res.get("bisqEasy.mediation.request.confirm.openMediation"))
                    .onAction(() -> {
                        //todo (Critical) should be handled with the solution how to treat system messages
                        //String systemMessage = Res.get("bisqEasy.mediation.requester.systemMessage");
                        // chatService.getBisqEasyPrivateTradeChatChannelService().sendSystemMessage(systemMessage, channel);

                        channel.setIsInMediation(true);
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
