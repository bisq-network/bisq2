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
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.data.Pair;
import bisq.common.market.Market;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.i18n.Res;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.offer.price.spec.PriceSpecFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeFormatter;
import bisq.trade.bisq_easy.BisqEasyTradeUtils;
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
public class BisqEasyTradeHistoryListItem {
    @EqualsAndHashCode.Include
    private final BisqEasyTrade trade;
    private final String tradeId, shortTradeId, dateString, tradeCompletedDateString, baseAmountAsString,
            baseAmountWithSymbol, quoteAmountAsString, quoteAmountWithSymbol, priceTooltip, myRole, paymentAsString;
    private final long date, price, baseAmount, quoteAmount;
    private final boolean hasFixPrice;
    private final Market market;
    private final UserProfile myUserProfile, peerProfile;
    private final ReputationScore myReputationScore, peerReputationScore;
    private final Pair<String, String> pricePair;
    private final FiatPaymentMethod paymentMethod;
    private final BitcoinPaymentMethod settlementMethod;

    public BisqEasyTradeHistoryListItem(BisqEasyClosedTrade closedTrade,
                                        ReputationService reputationService,
                                        MarketPriceService marketPriceService) {
        this.trade = closedTrade.trade();
        this.tradeId = trade.getId();
        shortTradeId = trade.getShortId();
        market = trade.getOffer().getMarket();

        BisqEasyContract contract = trade.getContract();
        date = contract.getTakeOfferDate();
        dateString = DateFormatter.formatDateTime(date);
        // btc confirmed
        tradeCompletedDateString = trade.getTradeCompletedDate().map(DateFormatter::formatDate).orElse("N/A");

        myUserProfile = closedTrade.myUserProfile();
        myReputationScore = reputationService.getReputationScore(myUserProfile);
        peerProfile = closedTrade.peerUserProfile();
        peerReputationScore = reputationService.getReputationScore(peerProfile);

        baseAmount = contract.getBaseSideAmount();
        baseAmountAsString = BisqEasyTradeFormatter.formatBaseSideAmount(trade);
        baseAmountWithSymbol = String.format("%s %s", baseAmountAsString, market.getBaseCurrencyCode());
        quoteAmount = contract.getQuoteSideAmount();
        quoteAmountAsString = BisqEasyTradeFormatter.formatQuoteSideAmount(trade);
        quoteAmountWithSymbol = String.format("%s %s", quoteAmountAsString, market.getQuoteCurrencyCode());

        price = BisqEasyTradeUtils.getPriceQuote(trade).getValue();
        BisqEasyOffer offer = contract.getOffer();
        PriceSpec priceSpec = offer.getPriceSpec();
        hasFixPrice = priceSpec instanceof FixPriceSpec;
        pricePair = PriceSpecFormatter.getFormattedPricePair(priceSpec, marketPriceService, offer.getMarket());
        String priceAsString = BisqEasyTradeFormatter.formatQuoteSideAmount(contract);
        priceTooltip = PriceSpecFormatter.getFormattedPriceSpecWithOfferPrice(priceSpec, priceAsString);

        paymentMethod = contract.getQuoteSidePaymentMethodSpec().getPaymentMethod();
        settlementMethod = contract.getBaseSidePaymentMethodSpec().getPaymentMethod();
        paymentAsString = String.format("%s / %s", paymentMethod, settlementMethod);

        String direction = trade.getDisplayDirection().isBuy()
                ? Res.get("bisqEasy.history.table.myRole.buyer")
                : Res.get("bisqEasy.history.table.myRole.seller");
        String role = BisqEasyTradeFormatter.getMakerTakerRole(trade);
        myRole = String.format("%s / %s", direction, role);
    }
}
