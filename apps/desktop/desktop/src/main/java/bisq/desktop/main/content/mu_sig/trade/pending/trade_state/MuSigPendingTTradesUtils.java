package bisq.desktop.main.content.mu_sig.trade.pending.trade_state;

import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannel;
import bisq.chat.mu_sig.open_trades.MuSigOpenTradeChannelService;
import bisq.common.encoding.Csv;
import bisq.common.file.FileMutatorUtils;
import bisq.common.monetary.Monetary;
import bisq.contract.mu_sig.MuSigContract;
import bisq.desktop.common.utils.FileChooserUtil;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.presentation.formatters.AmountFormatter;
import bisq.support.mediation.mu_sig.MuSigMediationRequestService;
import bisq.trade.mu_sig.MuSigTrade;
import bisq.trade.mu_sig.MuSigTradeService;
import bisq.trade.mu_sig.MuSigTradeUtils;
import bisq.user.profile.UserProfile;
import javafx.scene.Scene;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
public class MuSigPendingTTradesUtils {

    public static void exportTrade(MuSigTrade trade, Scene scene) {
        try {
            String tradeId = trade.getId();
            MuSigContract contract = trade.getContract();
            Monetary baseSideMonetary = MuSigTradeUtils.getBaseSideMonetary(contract);
            Monetary quoteSideMonetary = MuSigTradeUtils.getQuoteSideMonetary(contract);
            String formattedBaseAmount = AmountFormatter.formatBaseAmountWithCode(baseSideMonetary);
            String formattedQuoteAmount = AmountFormatter.formatQuoteAmountWithCode(quoteSideMonetary);
            String paymentProof = Optional.ofNullable(trade.getDepositTxId()).orElseGet(() -> Res.get("data.na"));
            String baseSideMethod = contract.getBaseSidePaymentMethodSpec().getDisplayString();
            String quoteSideMethod = contract.getQuoteSidePaymentMethodSpec().getDisplayString();
            String paymentMethod = baseSideMethod + " / " + quoteSideMethod;
            List<String> headers = List.of(
                    Res.get("muSig.trade.pending.table.tradeId"),
                    Res.get("muSig.trade.pending.csv.amount", baseSideMonetary.getCode()),
                    Res.get("muSig.trade.pending.csv.amount", quoteSideMonetary.getCode()),
                    Res.get("muSig.trade.pending.csv.txIdOrPreimage"),
                    Res.get("muSig.trade.pending.csv.paymentMethod")
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
            String initialFileName = "MuSig-trade-" + trade.getShortId() + ".csv";
            FileChooserUtil.saveFile(scene, initialFileName)
                    .ifPresent(file -> {
                        try {
                            FileMutatorUtils.writeToPath(csv, file);
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
                                        MuSigMediationRequestService muSigMediationRequestService,
                                        MuSigOpenTradeChannelService channelService,
                                        MuSigTradeService tradeService) {
        Optional<UserProfile> mediator = channel.getMediator();
        if (mediator.isPresent()) {
            new Popup().headline(Res.get("muSig.mediation.request.confirm.headline"))
                    .information(Res.get("muSig.mediation.request.confirm.msg"))
                    .actionButtonText(Res.get("muSig.mediation.request.confirm.openMediation"))
                    .onAction(() -> {
                        tradeService.maybeApplyDisputeStateFromMediationRequest(channel.getTradeId());
                        String encoded = Res.encode("muSig.mediation.requester.tradeLogMessage", channel.getMyUserIdentity().getUserName());
                        channelService.sendTradeLogMessage(encoded, channel);
                        channel.setIsInMediation(true);
                        channelService.persist();
                        muSigMediationRequestService.requestMediation(channel, contract);
                        new Popup().headline(Res.get("muSig.mediation.request.feedback.headline"))
                                .feedback(Res.get("muSig.mediation.request.feedback.msg"))
                                .show();
                    })
                    .closeButtonText(Res.get("action.cancel"))
                    .show();
        } else {
            new Popup().warning(Res.get("muSig.mediation.request.feedback.noMediatorAvailable")).show();
        }
    }
}
