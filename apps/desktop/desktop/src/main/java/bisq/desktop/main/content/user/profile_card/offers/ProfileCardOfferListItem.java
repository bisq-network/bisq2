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

package bisq.desktop.main.content.user.profile_card.offers;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.PaymentMethod;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.monetary.Monetary;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.PaymentMethodSpecUtil;
import bisq.offer.price.OfferPriceFormatter;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.PriceSpecFormatter;
import bisq.presentation.formatters.DateFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.TimeFormatter;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import com.google.common.base.Joiner;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ProfileCardOfferListItem {
    @EqualsAndHashCode.Include
    private final BisqEasyOfferbookMessage bisqEasyOfferbookMessage;

    private final BisqEasyOffer bisqEasyOffer;
    private final MarketPriceService marketPriceService;
    private final ReputationService reputationService;
    private final UserProfile senderUserProfile;
    private final String userNickname, formattedRangeQuoteAmount, bitcoinPaymentMethodsAsString,
            fiatPaymentMethodsAsString, authorUserProfileId, marketCurrencyCode, offerType,
            formattedOfferAge, offerAgeTooltipText;
    private final ReputationScore reputationScore;
    private final List<FiatPaymentMethod> fiatPaymentMethods;
    private final List<BitcoinPaymentMethod> bitcoinPaymentMethods;
    private final boolean isFixPrice;
    private final Monetary quoteSideMinAmount;
    private final long totalScore;
    private final Pin marketPriceByCurrencyMapPin;
    private final long offerAgeInDays;
    private double priceSpecAsPercent;
    private String formattedPercentagePrice, priceTooltipText;

    public ProfileCardOfferListItem(BisqEasyOfferbookMessage bisqEasyOfferbookMessage,
                                    UserProfile senderUserProfile,
                                    ReputationService reputationService,
                                    MarketPriceService marketPriceService) {
        this.bisqEasyOfferbookMessage = bisqEasyOfferbookMessage;

        bisqEasyOffer = bisqEasyOfferbookMessage.getBisqEasyOffer().orElseThrow();
        this.senderUserProfile = senderUserProfile;
        this.reputationService = reputationService;
        this.marketPriceService = marketPriceService;
        fiatPaymentMethods = retrieveAndSortFiatPaymentMethods();
        bitcoinPaymentMethods = retrieveAndSortBitcoinPaymentMethods();
        fiatPaymentMethodsAsString = Joiner.on(", ").join(fiatPaymentMethods.stream()
                .map(PaymentMethod::getDisplayString)
                .collect(Collectors.toList()));
        bitcoinPaymentMethodsAsString = Joiner.on(", ").join(bitcoinPaymentMethods.stream()
                .map(PaymentMethod::getDisplayString)
                .collect(Collectors.toList()));
        userNickname = senderUserProfile.getNickName();
        quoteSideMinAmount = OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, bisqEasyOffer).orElseThrow();
        formattedRangeQuoteAmount = OfferAmountFormatter.formatQuoteAmount(marketPriceService, bisqEasyOffer, true);
        isFixPrice = bisqEasyOffer.getPriceSpec() instanceof FixPriceSpec;
        authorUserProfileId = bisqEasyOfferbookMessage.getAuthorUserProfileId();
        marketCurrencyCode = bisqEasyOffer.getMarket().getQuoteCurrencyCode();
        offerType = bisqEasyOffer.getDirection().isBuy()
                ? Res.get("bisqEasy.offerbook.offerList.table.columns.offerType.buy")
                : Res.get("bisqEasy.offerbook.offerList.table.columns.offerType.sell");

        reputationScore = reputationService.getReputationScore(senderUserProfile);
        totalScore = reputationScore.getTotalScore();
        offerAgeInDays = TimeFormatter.getAgeInDays(bisqEasyOffer.getDate());
        formattedOfferAge = TimeFormatter.formatAgeInDays(bisqEasyOffer.getDate());
        offerAgeTooltipText = Res.get("user.profileCard.offers.table.columns.offerAge.tooltip",
                DateFormatter.formatDateTime(bisqEasyOffer.getDate()));

        marketPriceByCurrencyMapPin = marketPriceService.getMarketPriceByCurrencyMap().addObserver(() ->
                UIThread.run(this::updatePriceSpecAsPercent));
        updatePriceSpecAsPercent();
    }

    void dispose() {
        marketPriceByCurrencyMapPin.unbind();
    }

    boolean isBuyOffer() {
        return bisqEasyOffer.getDirection() == Direction.BUY;
    }

    private void updatePriceSpecAsPercent() {
        priceSpecAsPercent = PriceUtil.findPercentFromMarketPrice(marketPriceService, bisqEasyOffer).orElseThrow();
        formattedPercentagePrice = PercentageFormatter.formatToPercentWithSignAndSymbol(priceSpecAsPercent);
        String offerPrice = OfferPriceFormatter.formatQuote(marketPriceService, bisqEasyOffer);
        priceTooltipText = PriceSpecFormatter.getFormattedPriceSpecWithOfferPrice(bisqEasyOffer.getPriceSpec(), offerPrice);
    }

    private List<FiatPaymentMethod> retrieveAndSortFiatPaymentMethods() {
        List<FiatPaymentMethod> paymentMethods =
                PaymentMethodSpecUtil.getPaymentMethods(bisqEasyOffer.getQuoteSidePaymentMethodSpecs());
        paymentMethods.sort(Comparator.comparing(FiatPaymentMethod::isCustomPaymentMethod)
                .thenComparing(FiatPaymentMethod::getDisplayString));
        return paymentMethods;
    }

    private List<BitcoinPaymentMethod> retrieveAndSortBitcoinPaymentMethods() {
        List<BitcoinPaymentMethod> paymentMethods =
                PaymentMethodSpecUtil.getPaymentMethods(bisqEasyOffer.getBaseSidePaymentMethodSpecs());
        paymentMethods.sort(Comparator.comparing(BitcoinPaymentMethod::getDisplayString).reversed());
        return paymentMethods;
    }
}
