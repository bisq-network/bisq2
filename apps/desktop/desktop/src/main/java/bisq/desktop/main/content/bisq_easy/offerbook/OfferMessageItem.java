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

package bisq.desktop.main.content.bisq_easy.offerbook;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.FiatPaymentMethod;
import bisq.account.payment_method.PaymentMethod;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.common.data.Pair;
import bisq.common.monetary.Monetary;
import bisq.common.observable.Pin;
import bisq.desktop.common.threading.UIThread;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.PaymentMethodSpecUtil;
import bisq.offer.price.PriceUtil;
import bisq.user.profile.UserProfile;
import bisq.user.reputation.ReputationScore;
import bisq.user.reputation.ReputationService;
import com.google.common.base.Joiner;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class OfferMessageItem {
    @EqualsAndHashCode.Include
    private final BisqEasyOfferbookMessage bisqEasyOfferbookMessage;
    private final BisqEasyOffer bisqEasyOffer;
    private final MarketPriceService marketPriceService;
    private final ReputationService reputationService;
    private final UserProfile userProfile;
    private final String userNickname, minMaxAmountAsString, bitcoinPaymentMethodsAsString, fiatPaymentMethodsAsString;
    private final Pair<Monetary, Monetary> minMaxAmount;
    private final ObjectProperty<ReputationScore> reputationScore = new SimpleObjectProperty<>();
    private final List<FiatPaymentMethod> fiatPaymentMethods;
    private final List<BitcoinPaymentMethod> bitcoinPaymentMethods;
    private long totalScore;
    private double priceSpecAsPercent;
    private Pin marketPriceByCurrencyMapPin, reputationChangedPin;

    OfferMessageItem(BisqEasyOfferbookMessage bisqEasyOfferbookMessage,
                     UserProfile userProfile,
                     ReputationService reputationService,
                     MarketPriceService marketPriceService) {
        this.bisqEasyOfferbookMessage = bisqEasyOfferbookMessage;
        bisqEasyOffer = bisqEasyOfferbookMessage.getBisqEasyOffer().orElseThrow();
        this.userProfile = userProfile;
        this.reputationService = reputationService;
        this.marketPriceService = marketPriceService;
        fiatPaymentMethods = retrieveAndSortFiatPaymentMethods();
        bitcoinPaymentMethods = retrieveAndSortBitcoinPaymentMethods();
        fiatPaymentMethodsAsString = Joiner.on(", ").join(fiatPaymentMethods.stream().map(PaymentMethod::getDisplayString).collect(Collectors.toList()));
        bitcoinPaymentMethodsAsString = Joiner.on(", ").join(bitcoinPaymentMethods.stream().map(PaymentMethod::getDisplayString).collect(Collectors.toList()));
        userNickname = userProfile.getNickName();
        minMaxAmount = retrieveMinMaxAmount();
        minMaxAmountAsString = OfferAmountFormatter.formatQuoteAmount(marketPriceService, bisqEasyOffer, false);

        initialize();
    }

    void dispose() {
        marketPriceByCurrencyMapPin.unbind();
        reputationChangedPin.unbind();
    }

    Monetary getMinAmount() {
        return minMaxAmount.getFirst();
    }

    boolean isBuyOffer() {
        return bisqEasyOffer.getDirection() == Direction.BUY;
    }

    private void initialize() {
        marketPriceByCurrencyMapPin = marketPriceService.getMarketPriceByCurrencyMap().addObserver(() ->
                UIThread.run(this::updatePriceSpecAsPercent));
        updatePriceSpecAsPercent();

        reputationChangedPin = reputationService.getChangedUserProfileScore().addObserver(userProfileId ->
                UIThread.run(this::updateReputationScore));
        updateReputationScore();
    }

    private Pair<Monetary, Monetary> retrieveMinMaxAmount() {
        Monetary minAmount = OfferAmountUtil.findQuoteSideMinOrFixedAmount(marketPriceService, bisqEasyOffer).orElseThrow();
        Monetary maxAmount = OfferAmountUtil.findQuoteSideMaxOrFixedAmount(marketPriceService, bisqEasyOffer).orElseThrow();
        return new Pair<>(minAmount, maxAmount);
    }

    private void updatePriceSpecAsPercent() {
        priceSpecAsPercent = PriceUtil.findPercentFromMarketPrice(marketPriceService, bisqEasyOffer).orElseThrow();
    }

    private void updateReputationScore() {
        reputationScore.set(reputationService.getReputationScore(userProfile));
        totalScore = reputationScore.get().getTotalScore();
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
