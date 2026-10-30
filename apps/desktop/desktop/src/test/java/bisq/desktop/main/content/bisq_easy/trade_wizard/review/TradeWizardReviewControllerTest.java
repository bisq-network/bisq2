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

package bisq.desktop.main.content.bisq_easy.trade_wizard.review;

import bisq.account.payment_method.BitcoinPaymentMethod;
import bisq.account.payment_method.BitcoinPaymentMethodSpec;
import bisq.account.payment_method.BitcoinPaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethodSpec;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.Observable;
import bisq.common.observable.map.ObservableHashMap;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.Transitions;
import bisq.desktop.testutil.JavaFxToolkitExtension;
import bisq.offer.Direction;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.BaseSideRangeAmountSpec;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import bisq.offer.amount.spec.QuoteSideRangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.settings.SettingsService;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.profile.UserProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static bisq.trade.bisq_easy.validation.BisqEasyOfferAmountValidator.validateOfferAmount;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * The review step of the trade wizard when the user takes an offer from the offer list (bisq2#5170). The offer list
 * compares the user's amount with the range of an offer at the precision of cents. At a price of 60,003 USD, a
 * conversion of 100 USD to Bitcoin and back gives 166,658 satoshi and 99.9997 USD.
 */
@ExtendWith(JavaFxToolkitExtension.class)
@ResourceLock(value = "Transitions.settingsService", mode = ResourceAccessMode.READ_WRITE)
class TradeWizardReviewControllerTest {
    private static final Market MARKET = MarketRepository.getUSDBitcoinMarket();
    private static final PriceQuote PRICE_QUOTE = PriceQuote.fromFiatPrice(60_003, "USD");
    private static final BitcoinPaymentMethod MAIN_CHAIN = BitcoinPaymentMethod.fromPaymentRail(BitcoinPaymentRail.MAIN_CHAIN);
    private static final FiatPaymentMethod ZELLE = FiatPaymentMethod.fromPaymentRail(FiatPaymentRail.ZELLE);

    private ServiceProvider serviceProvider;
    private MarketPrice marketPrice;
    private MarketPriceService marketPriceService;

    @BeforeEach
    void setUp() {
        marketPrice = mock(MarketPrice.class);
        ObservableHashMap<Market, MarketPrice> marketPriceByCurrencyMap = new ObservableHashMap<>();
        marketPriceByCurrencyMap.put(MARKET, marketPrice);
        marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.getMarketPriceByCurrencyMap()).thenReturn(marketPriceByCurrencyMap);
        when(marketPriceService.findMarketPrice(MARKET)).thenReturn(Optional.of(marketPrice));
        useMarketPrice(PRICE_QUOTE);

        BisqEasyTradeService bisqEasyTradeService = mock(BisqEasyTradeService.class);
        when(bisqEasyTradeService.takerCreatesProtocol(any(), any(), any(), any(), any(), any(), any(), anyLong()))
                .thenAnswer(invocation -> {
                    throw new TakenAmounts(invocation.<Monetary>getArgument(2).getValue(),
                            invocation.<Monetary>getArgument(3).getValue());
                });

        serviceProvider = mock(ServiceProvider.class, RETURNS_DEEP_STUBS);
        when(serviceProvider.getBondedRolesService().getMarketPriceService()).thenReturn(marketPriceService);
        when(serviceProvider.getTradeService().getBisqEasyTradeService()).thenReturn(bisqEasyTradeService);
        when(serviceProvider.getSupportService().getBisqEasyMediationRequestService().selectMediator(any(), any(), any()))
                .thenReturn(Optional.of(mock(UserProfile.class)));

        // The view of the review step has a waiting animation, which reads whether animations are used
        SettingsService settingsService = mock(SettingsService.class, RETURNS_DEEP_STUBS);
        when(settingsService.getUseAnimations()).thenReturn(new Observable<>(false));
        Transitions.setSettingsService(settingsService);
    }

    @AfterEach
    void tearDown() {
        Transitions.setSettingsService(null);
    }

    // 99.9997 USD is 100.00 USD in cents, so the offer list shows the offer
    @Test
    void anAmountWhichMissesTheMinimumOnlyBelowACentIsTakenAtTheMinimum() {
        BisqEasyOffer offer = rangeSellOffer(new QuoteSideRangeAmountSpec(1_000_000, 5_000_000));

        TakenAmounts takenAmounts = takeOffer(offer, 999_997);

        assertDoesNotThrow(() -> validateOfferAmount(offer, takenAmounts.baseSideAmount, takenAmounts.quoteSideAmount));
        assertThat(takenAmounts.quoteSideAmount).isEqualTo(1_000_000);
    }

    @Test
    void anAmountWhichMissesTheMaximumOnlyBelowACentIsTakenAtTheMaximum() {
        BisqEasyOffer offer = rangeSellOffer(new QuoteSideRangeAmountSpec(1_000_000, 5_000_000));

        TakenAmounts takenAmounts = takeOffer(offer, 5_000_004);

        assertDoesNotThrow(() -> validateOfferAmount(offer, takenAmounts.baseSideAmount, takenAmounts.quoteSideAmount));
        assertThat(takenAmounts.quoteSideAmount).isEqualTo(5_000_000);
    }

    // The minimum of 166,659 satoshi is 100.0003 USD, and 100 USD are 166,658 satoshi
    @Test
    void anAmountWhichMissesTheBitcoinMinimumOnlyBelowACentIsTakenAtTheMinimum() {
        BisqEasyOffer offer = rangeSellOffer(new BaseSideRangeAmountSpec(166_659, 833_291));

        TakenAmounts takenAmounts = takeOffer(offer, 1_000_000);

        assertDoesNotThrow(() -> validateOfferAmount(offer, takenAmounts.baseSideAmount, takenAmounts.quoteSideAmount));
        assertThat(takenAmounts.baseSideAmount).isEqualTo(166_659);
    }

    // At 4,000 USD, the minimum of 2,500,001 satoshi is 100.0000 USD, and 100 USD are 2,500,000 satoshi
    @Test
    void anAmountWhichMissesTheBitcoinMinimumOnlyBelowACentIsTakenAtTheMinimumAtALowPrice() {
        useMarketPrice(PriceQuote.fromFiatPrice(4_000, "USD"));
        BisqEasyOffer offer = rangeSellOffer(new BaseSideRangeAmountSpec(2_500_001, 12_500_001));

        TakenAmounts takenAmounts = takeOffer(offer, 999_997);

        assertDoesNotThrow(() -> validateOfferAmount(offer, takenAmounts.baseSideAmount, takenAmounts.quoteSideAmount));
        assertThat(takenAmounts.baseSideAmount).isEqualTo(2_500_001);
    }

    // At 4,000 USD, the maximum of 12,500,001 satoshi is 500.0000 USD, and 500 USD are 12,500,000 satoshi
    @Test
    void anAmountWhichMissesTheBitcoinMaximumOnlyBelowACentIsTakenAtTheMaximumAtALowPrice() {
        useMarketPrice(PriceQuote.fromFiatPrice(4_000, "USD"));
        BisqEasyOffer offer = rangeSellOffer(new BaseSideRangeAmountSpec(2_500_001, 12_500_001));

        TakenAmounts takenAmounts = takeOffer(offer, 5_000_004);

        assertDoesNotThrow(() -> validateOfferAmount(offer, takenAmounts.baseSideAmount, takenAmounts.quoteSideAmount));
        assertThat(takenAmounts.baseSideAmount).isEqualTo(12_500_001);
    }

    // 99.9949 USD is 99.99 USD in cents, so the offer list does not show the offer
    @Test
    void anAmountWhichMissesTheMinimumByACentIsNotChanged() {
        BisqEasyOffer offer = rangeSellOffer(new QuoteSideRangeAmountSpec(1_000_000, 5_000_000));

        TakenAmounts takenAmounts = takeOffer(offer, 999_949);

        assertThat(takenAmounts.quoteSideAmount).isEqualTo(999_949);
    }

    private void useMarketPrice(PriceQuote priceQuote) {
        when(marketPrice.getPriceQuote()).thenReturn(priceQuote);
        when(marketPriceService.findMarketPriceQuote(MARKET)).thenReturn(Optional.of(priceQuote));
    }

    private TakenAmounts takeOffer(BisqEasyOffer offer, long quoteSideAmount) {
        AtomicReference<TakenAmounts> takenAmounts = new AtomicReference<>();
        JavaFxToolkitExtension.runOnFxThread(() -> {
            TradeWizardReviewController controller = new TradeWizardReviewController(serviceProvider,
                    mainButtonsVisible -> {
                    },
                    navigationTarget -> {
                    });
            controller.setDataForTakeOffer(offer,
                    new QuoteSideFixedAmountSpec(quoteSideAmount),
                    List.of(MAIN_CHAIN),
                    List.of(ZELLE));
            try {
                controller.takeOffer();
            } catch (TakenAmounts e) {
                takenAmounts.set(e);
            }
        });
        assertThat(takenAmounts.get()).isNotNull();
        return takenAmounts.get();
    }

    private static BisqEasyOffer rangeSellOffer(AmountSpec amountSpec) {
        BisqEasyOffer offer = mock(BisqEasyOffer.class);
        when(offer.getMarket()).thenReturn(MARKET);
        when(offer.getDirection()).thenReturn(Direction.SELL);
        when(offer.getTakersDirection()).thenReturn(Direction.BUY);
        when(offer.getAmountSpec()).thenReturn(amountSpec);
        when(offer.getPriceSpec()).thenReturn(new MarketPriceSpec());
        when(offer.getBaseSidePaymentMethodSpecs()).thenReturn(List.of(new BitcoinPaymentMethodSpec(MAIN_CHAIN)));
        when(offer.getQuoteSidePaymentMethodSpecs()).thenReturn(List.of(new FiatPaymentMethodSpec(ZELLE)));
        return offer;
    }

    // Stops the take offer at the trade service, with the amounts which the trade service got
    private static final class TakenAmounts extends RuntimeException {
        private final long baseSideAmount;
        private final long quoteSideAmount;

        private TakenAmounts(long baseSideAmount, long quoteSideAmount) {
            this.baseSideAmount = baseSideAmount;
            this.quoteSideAmount = quoteSideAmount;
        }
    }
}
