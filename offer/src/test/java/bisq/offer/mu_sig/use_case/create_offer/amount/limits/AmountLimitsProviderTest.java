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

package bisq.offer.mu_sig.use_case.create_offer.amount.limits;

import bisq.account.accounts.Account;
import bisq.account.payment_method.PaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentMethod;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.MonetaryRange;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.Pin;
import bisq.common.observable.map.ObservableHashMap;
import bisq.offer.Direction;
import bisq.offer.mu_sig.use_case.create_offer.direction.DirectionSelection;
import bisq.offer.mu_sig.use_case.create_offer.market.MarketSelection;
import bisq.offer.mu_sig.use_case.create_offer.payment_method.PaymentMethodSelection;
import bisq.offer.mu_sig.use_case.create_offer.price.PriceSelection;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AmountLimitsProviderTest {
    private final Market usdMarket = MarketRepository.getUSDBitcoinMarket();
    private final ObservableHashMap<PaymentMethod<?>, Account<?, ?>> accountByPaymentMethod = new ObservableHashMap<>();

    private MarketPriceService marketPriceService;
    private MarketSelection marketSelection;
    private PriceSelection priceSelection;
    private DirectionSelection directionSelection;
    private PaymentMethodSelection paymentMethodSelection;

    @BeforeEach
    void setUp() {
        PriceQuote quote = PriceQuote.fromFiatPrice(100_000, "USD");
        marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.findMarketPriceQuote(usdMarket)).thenReturn(Optional.of(quote));

        marketSelection = mock(MarketSelection.class);
        when(marketSelection.addMarketListener(any())).thenReturn(mock(Pin.class));
        when(marketSelection.getMarket()).thenReturn(usdMarket);

        priceSelection = mock(PriceSelection.class);
        when(priceSelection.addPriceQuoteListener(any())).thenReturn(mock(Pin.class));
        when(priceSelection.addMarketContextListener(any())).thenReturn(mock(Pin.class));
        when(priceSelection.getPriceQuote()).thenReturn(quote);

        directionSelection = mock(DirectionSelection.class);
        when(directionSelection.addDisplayDirectionListener(any())).thenReturn(mock(Pin.class));
        when(directionSelection.getDisplayDirection()).thenReturn(Direction.SELL);

        paymentMethodSelection = mock(PaymentMethodSelection.class);
        when(paymentMethodSelection.accountByPaymentMethodObservable()).thenReturn(accountByPaymentMethod);
        when(paymentMethodSelection.getAccountByPaymentMethod()).thenAnswer(invocation ->
                ImmutableMap.copyOf(accountByPaymentMethod));
    }

    @Test
    void potentialLimitsInUsdFollowTheSelectedPaymentMethodCap() {
        selectPaymentMethod(FiatPaymentRail.WISE);
        AmountLimitsProvider provider = createProvider();

        // WISE is a MODERATE chargeback-risk rail: 50% of the 10k USD absolute maximum.
        MonetaryRange limitsInUsd = provider.getPotentialTradeAmountLimitsInUsd();
        assertEquals(Fiat.fromFaceValue(10, "USD"), limitsInUsd.getMin());
        assertEquals(Fiat.fromFaceValue(5000, "USD"), limitsInUsd.getMax());
        assertEquals(limitsInUsd.getMax().getValue(),
                provider.getPotentialTradeAmountLimits().getMax().getQuoteSideAmount().getValue());

        selectPaymentMethod(FiatPaymentRail.ADVANCED_CASH);
        assertEquals(Fiat.fromFaceValue(10_000, "USD"), provider.getPotentialTradeAmountLimitsInUsd().getMax());
    }

    @Test
    void potentialLimitsInUsdAreClearedWithTheOtherLimits() {
        selectPaymentMethod(FiatPaymentRail.WISE);
        AmountLimitsProvider provider = createProvider();

        // A market change clears the providers' outputs; the USD bounds must not survive it either.
        Market eurMarket = new Market("BTC", "EUR", "Bitcoin", "Euro");
        when(marketSelection.getMarket()).thenReturn(eurMarket);
        when(marketPriceService.findMarketPriceQuote(eurMarket)).thenReturn(Optional.empty());
        accountByPaymentMethod.clear();

        assertNull(provider.getPotentialTradeAmountLimits());
        assertNull(provider.getPotentialTradeAmountLimitsInUsd());
    }

    private AmountLimitsProvider createProvider() {
        AmountLimitsProvider provider = new AmountLimitsProvider(marketPriceService, marketSelection,
                directionSelection, paymentMethodSelection, priceSelection);
        provider.initialize();
        return provider;
    }

    private void selectPaymentMethod(FiatPaymentRail paymentRail) {
        PaymentMethod<?> paymentMethod = FiatPaymentMethod.fromPaymentRail(paymentRail);
        Account<?, ?> account = mock(Account.class);
        when(account.getPaymentMethod()).thenAnswer(invocation -> paymentMethod);
        accountByPaymentMethod.clear();
        accountByPaymentMethod.put(paymentMethod, account);
    }
}
