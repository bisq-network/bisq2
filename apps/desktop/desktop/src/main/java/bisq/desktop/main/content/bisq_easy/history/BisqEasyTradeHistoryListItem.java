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

package bisq.desktop.main.content.bisq_easy.history;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.common.market.Market;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.desktop.components.table.DateTableItem;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.offer.price.spec.PriceSpecFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeFormatter;
import bisq.trade.bisq_easy.protocol.BisqEasyClosedTrade;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BisqEasyTradeHistoryListItem implements DateTableItem {
    @EqualsAndHashCode.Include
    private final BisqEasyTrade trade;
    private final String myUserName, directionalTitle, peersUserName, tradeId, shortTradeId, dateString, timeString, dateTimeString,
            tradeCompletedDateString, baseAmountString, quoteAmountString, quoteAmountWithCodeString, priceString, priceWithCodeString,
            priceTooltip, pricePercentage, priceModality, myRole, paymentMethodAsString, offerType, receiverAddressOrInvoice, txIdOrPreimage;
    private final long date, price, baseAmount, quoteAmount;
    private final boolean hasFixPrice;
    private final Market market;
    private final UserProfile myUserProfile, peersUserProfile;
    private final ReputationScore peersReputationScore;
    private final FiatPaymentMethod paymentMethod;
    private final BitcoinPaymentMethod settlementMethod;

    public BisqEasyTradeHistoryListItem(BisqEasyClosedTrade closedTrade,
                                        ReputationService reputationService) {
        this.trade = closedTrade.trade();
        this.tradeId = trade.getId();
        shortTradeId = trade.getShortId();
        market = trade.getOffer().getMarket();

        BisqEasyContract contract = trade.getContract();

        date = trade.getTradeCompletedDate().orElse(contract.getTakeOfferDate());
        dateString = DateFormatter.formatDate(date);
        timeString = DateFormatter.formatTime(date);
        dateTimeString = String.format("%s %s", dateString, timeString);

        // btc confirmed
        tradeCompletedDateString = trade.getTradeCompletedDate().map(DateFormatter::formatDate).orElse("N/A");

        myUserProfile = closedTrade.myUserProfile();
        myUserName = myUserProfile.getUserName();

        directionalTitle = BisqEasyTradeFormatter.getDirectionalTitle(trade);
        offerType = trade.getOffer().getDirection().isBuy()
                ? Res.get("bisqEasy.openTrades.csv.offerType.buy")
                : Res.get("bisqEasy.openTrades.csv.offerType.sell");

        receiverAddressOrInvoice = trade.getBitcoinPaymentData().orElseGet(() -> Res.get("data.na"));
        txIdOrPreimage = trade.getPaymentProof().orElseGet(() -> Res.get("data.na"));

        peersUserProfile = closedTrade.peerUserProfile();
        peersUserName = peersUserProfile.getUserName();
        peersReputationScore = reputationService.getReputationScore(peersUserProfile);

        baseAmount = contract.getBaseSideAmount();
        baseAmountString = BisqEasyTradeFormatter.formatBaseSideAmount(trade);

        quoteAmount = contract.getQuoteSideAmount();
        quoteAmountString = BisqEasyTradeFormatter.formatQuoteSideAmount(trade);
        quoteAmountWithCodeString = BisqEasyTradeFormatter.formatQuoteSideAmountWithCode(trade);

        price = trade.getPriceQuote().getValue();
        BisqEasyOffer offer = contract.getOffer();
        PriceSpec priceSpec = offer.getPriceSpec();
        hasFixPrice = priceSpec instanceof FixPriceSpec;
        priceString = PriceFormatter.format(trade.getPriceQuote());
        priceWithCodeString = PriceFormatter.formatWithCode(trade.getPriceQuote());
        priceTooltip = PriceSpecFormatter.getFormattedPriceSpecWithoutPrice(priceSpec);
        pricePercentage = PriceSpecFormatter.getFormattedPriceAsPercentage(trade.getPriceQuote(), priceSpec, contract.getMarketPrice(), market);
        priceModality = priceSpec.getDisplayName();

        paymentMethod = contract.getQuoteSidePaymentMethodSpec().getPaymentMethod();
        settlementMethod = contract.getBaseSidePaymentMethodSpec().getPaymentMethod();
        paymentMethodAsString = String.format("%s / %s", paymentMethod.getDisplayString(), settlementMethod.getDisplayString());

        myRole = BisqEasyTradeFormatter.getMakerTakerRole(trade);
    }
}
