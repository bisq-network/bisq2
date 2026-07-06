/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.main.content.bisq_easy;

import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannel;
import bisq.chat.bisq_easy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.common.encoding.Csv;
import bisq.common.file.FileMutatorUtils;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.desktop.common.utils.FileChooserUtil;
import bisq.desktop.components.overlay.Popup;
import bisq.i18n.Res;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.support.mediation.bisq_easy.BisqEasyMediationRequestService;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.user.profile.UserProfile;
import javafx.scene.Scene;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Slf4j
public class TradesUtils {

    public static void exportTrade(BisqEasyTrade trade, Scene scene) {
        try {
            BisqEasyContract contract = trade.getContract();
            String tradeId = trade.getId();
            String role = trade.isMaker() ?
                    Res.get("bisqEasy.openTrades.table.makerTakerRole.maker") :
                    Res.get("bisqEasy.openTrades.table.makerTakerRole.taker");
            String offerType = trade.getOffer().getDirection().isBuy() ?
                    Res.get("bisqEasy.openTrades.csv.offerType.buy") :
                    Res.get("bisqEasy.openTrades.csv.offerType.sell");
            String quoteCurrencyCode = trade.getOffer().getMarket().getQuoteCurrencyCode();
            long baseSideAmount = contract.getBaseSideAmount();
            long quoteSideAmount = contract.getQuoteSideAmount();
            String formattedBaseAmount = AmountFormatter.formatBaseAmount(Coin.asBtcFromValue(baseSideAmount));
            String formattedQuoteAmount = AmountFormatter.formatQuoteAmount(Fiat.from(quoteSideAmount, quoteCurrencyCode));
            String priceCodes = trade.getOffer().getMarket().getMarketCodes();
            PriceSpec priceSpec = trade.getOffer().getPriceSpec();
            String priceSpecString = priceSpec.getDisplayName();
            String price = PriceFormatter.format(contract.getPriceQuote());
            String pricePercentage = priceSpec instanceof FixPriceSpec
                    ? ""
                    : PercentageFormatter.formatToPercentWithSignAndSymbol(
                            priceSpec instanceof FloatPriceSpec floatPriceSpec
                                    ? floatPriceSpec.getPercentage()
                                    : 0
                    );
            String paymentProof = trade.getPaymentProof().orElseGet(() -> Res.get("data.na"));
            String bitcoinPaymentData = trade.getBitcoinPaymentData().orElseGet(() -> Res.get("data.na"));
            String bitcoinMethod = contract.getBaseSidePaymentMethodSpec().getDisplayString();
            String fiatMethod = contract.getQuoteSidePaymentMethodSpec().getDisplayString();
            String paymentMethod = String.format("%s / %s", fiatMethod, bitcoinMethod);
            Long date = trade.getTradeCompletedDate().orElse(contract.getTakeOfferDate());
            String formattedDate = DateFormatter.formatDateTime(date);
            List<String> headers = List.of(
                    Res.get("bisqEasy.openTrades.table.tradeId").toUpperCase(),
                    Res.get("bisqEasy.openTrades.csv.date").toUpperCase(),
                    Res.get("bisqEasy.openTrades.table.makerTakerRole").toUpperCase(),
                    Res.get("bisqEasy.openTrades.csv.offerType").toUpperCase(),
                    Res.get("bisqEasy.history.table.csv.quoteAmount").toUpperCase(),
                    Res.get("bisqEasy.history.table.csv.quoteAmountCurrencyCode").toUpperCase(),
                    Res.get("bisqEasy.openTrades.table.baseAmount").toUpperCase(),
                    Res.get("bisqEasy.history.table.csv.tradePrice").toUpperCase(),
                    Res.get("bisqEasy.openTrades.csv.market").toUpperCase(),
                    Res.get("bisqEasy.history.table.csv.pricePercentage").toUpperCase(),
                    Res.get("bisqEasy.history.table.csv.priceModality").toUpperCase(),
                    Res.get("bisqEasy.openTrades.csv.paymentMethod").toUpperCase(),
                    Res.get("bisqEasy.openTrades.csv.receiverAddressOrInvoice").toUpperCase(),
                    Res.get("bisqEasy.openTrades.csv.txIdOrPreimage").toUpperCase()
                    );
            List<List<String>> tradeData = List.of(
                    List.of(
                            tradeId,
                            formattedDate,
                            role,
                            offerType,
                            formattedQuoteAmount,
                            quoteCurrencyCode,
                            formattedBaseAmount,
                            price,
                            priceCodes,
                            pricePercentage,
                            priceSpecString,
                            paymentMethod,
                            bitcoinPaymentData,
                            paymentProof
                    )
            );
            String csv = Csv.toCsv(headers, tradeData);
            String initialFileName = "BisqEasy-trade-" + trade.getShortId() + ".csv";
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

    public static void requestMediation(BisqEasyOpenTradeChannel channel,
                                         BisqEasyContract contract,
                                         BisqEasyMediationRequestService bisqEasyMediationRequestService,
                                         BisqEasyOpenTradeChannelService channelService) {
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
                        bisqEasyMediationRequestService.requestMediation(channel, contract);
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
