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
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.common.monetary.TradeAmountRange;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AmountLimitsProvidersFailSoftTest {
    private final Market usdMarket = MarketRepository.getUSDBitcoinMarket();

    private final Map<Market, PriceQuote> quotes = new HashMap<>();
    private final List<Runnable> contextListeners = new ArrayList<>();
    private final List<Consumer<PriceQuote>> quoteListeners = new ArrayList<>();
    private final List<Consumer<Direction>> directionListeners = new ArrayList<>();
    private final ObservableHashMap<PaymentMethod<?>, Account<?, ?>> accountByPaymentMethod = new ObservableHashMap<>();

    private MarketPriceService marketPriceService;
    private MarketSelection marketSelection;
    private PriceSelection priceSelection;
    private DirectionSelection directionSelection;
    private PaymentMethodSelection paymentMethodSelection;

    @BeforeEach
    void setUp() {
        marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.findMarketPriceQuote(any())).thenAnswer(invocation ->
                Optional.ofNullable(quotes.get(invocation.getArgument(0, Market.class))));

        marketSelection = mock(MarketSelection.class);
        when(marketSelection.addMarketListener(any())).thenReturn(mock(Pin.class));

        priceSelection = mock(PriceSelection.class);
        when(priceSelection.addPriceQuoteListener(any())).thenAnswer(invocation -> {
            quoteListeners.add(invocation.getArgument(0));
            return mock(Pin.class);
        });
        when(priceSelection.addMarketContextListener(any())).thenAnswer(invocation -> {
            contextListeners.add(invocation.getArgument(0));
            return mock(Pin.class);
        });

        directionSelection = mock(DirectionSelection.class);
        when(directionSelection.addDisplayDirectionListener(any())).thenAnswer(invocation -> {
            directionListeners.add(invocation.getArgument(0));
            return mock(Pin.class);
        });

        paymentMethodSelection = mock(PaymentMethodSelection.class);
        when(paymentMethodSelection.accountByPaymentMethodObservable()).thenReturn(accountByPaymentMethod);
        when(paymentMethodSelection.getAccountByPaymentMethod()).thenAnswer(invocation ->
                ImmutableMap.copyOf(accountByPaymentMethod));
    }

    private void selectUsdMarketAt(double marketPriceValue) {
        PriceQuote quote = PriceQuote.fromFiatPrice(marketPriceValue, "USD");
        quotes.put(usdMarket, quote);
        when(marketSelection.getMarket()).thenReturn(usdMarket);
        when(priceSelection.getPriceQuote()).thenReturn(quote);
    }

    private void fireContext() {
        List.copyOf(contextListeners).forEach(Runnable::run);
    }

    @Test
    void absoluteLimitRetainsOutputWhileBtcUsdIsMissingAndRecovers() {
        selectUsdMarketAt(100_000);
        AbsoluteAmountLimitsProvider provider =
                new AbsoluteAmountLimitsProvider(marketPriceService, marketSelection, priceSelection);
        provider.initialize();
        TradeAmountRange before = provider.getTradeAmountLimits();
        assertEquals(100_000_000L, before.getMax().getQuoteSideAmount().getValue()); // $10,000 at 4 decimals

        // The BTC/USD leg vanishes; a context refresh must retain the previous output, not throw.
        quotes.remove(usdMarket);
        fireContext();
        assertEquals(before, provider.getTradeAmountLimits());

        // Recovery: fresh rates are picked up again on the next refresh.
        selectUsdMarketAt(200_000);
        fireContext();
        assertEquals(100_000_000L, provider.getTradeAmountLimits().getMax().getQuoteSideAmount().getValue());
    }

    @Test
    void userSpecificCapIsClearedWhenNotApplicableEvenWhileARateIsMissing() {
        selectUsdMarketAt(100_000);
        when(directionSelection.getDisplayDirection()).thenReturn(Direction.BUY);
        UserSpecificAmountLimitsProvider provider =
                new UserSpecificAmountLimitsProvider(marketPriceService, marketSelection, directionSelection, priceSelection);
        provider.initialize();
        assertTrue(provider.getTradeAmountLimit().isPresent());

        // The rate vanishes, then the user switches to sell: applicability is decided first, so
        // the stale buy-side cap must be cleared regardless of rate availability.
        quotes.remove(usdMarket);
        when(directionSelection.getDisplayDirection()).thenReturn(Direction.SELL);
        List.copyOf(directionListeners).forEach(listener -> listener.accept(Direction.SELL));
        assertEquals(Optional.empty(), provider.getTradeAmountLimit());
    }

    @Test
    void userSpecificCapRetainsOutputWhileApplicableButRateMissing() {
        selectUsdMarketAt(100_000);
        when(directionSelection.getDisplayDirection()).thenReturn(Direction.BUY);
        UserSpecificAmountLimitsProvider provider =
                new UserSpecificAmountLimitsProvider(marketPriceService, marketSelection, directionSelection, priceSelection);
        provider.initialize();
        Optional<TradeAmount> before = provider.getTradeAmountLimit();
        assertTrue(before.isPresent());

        quotes.remove(usdMarket);
        fireContext();
        assertEquals(before, provider.getTradeAmountLimit());
    }

    @Test
    void absoluteLimitFollowsTheBtcUsdLegWhenTheOfferQuoteIsUnchanged() {
        // Non-fiat market: the $10,000 limit converts through BTC/USD; a BTC/USD move must
        // refresh the limit although the selected XMR/BTC quote did not change.
        PriceQuote xmrQuote = PriceQuote.fromAltCoinPrice(0.001, "XMR");
        Market xmrMarket = xmrQuote.getMarket();
        quotes.put(xmrMarket, xmrQuote);
        quotes.put(usdMarket, PriceQuote.fromFiatPrice(100_000, "USD"));
        when(marketSelection.getMarket()).thenReturn(xmrMarket);
        when(priceSelection.getPriceQuote()).thenReturn(xmrQuote);

        AbsoluteAmountLimitsProvider provider =
                new AbsoluteAmountLimitsProvider(marketPriceService, marketSelection, priceSelection);
        provider.initialize();
        long maxBtcBefore = provider.getTradeAmountLimits().getMax().getQuoteSideAmount().getValue();
        assertEquals(10_000_000L, maxBtcBefore); // $10,000 at 100,000 = 0.1 BTC

        quotes.put(usdMarket, PriceQuote.fromFiatPrice(200_000, "USD"));
        fireContext();
        assertEquals(5_000_000L, provider.getTradeAmountLimits().getMax().getQuoteSideAmount().getValue());
    }

    @Test
    void userSpecificCapAppearsOnBuySwitchUsingCachedRates() {
        selectUsdMarketAt(100_000);
        when(directionSelection.getDisplayDirection()).thenReturn(Direction.SELL);
        UserSpecificAmountLimitsProvider provider =
                new UserSpecificAmountLimitsProvider(marketPriceService, marketSelection, directionSelection, priceSelection);
        provider.initialize();
        assertEquals(Optional.empty(), provider.getTradeAmountLimit());

        // The rate vanishes, then the user becomes a buyer: the cap must appear, computed from the
        // cached rates, instead of silently staying absent.
        quotes.remove(usdMarket);
        when(directionSelection.getDisplayDirection()).thenReturn(Direction.BUY);
        List.copyOf(directionListeners).forEach(listener -> listener.accept(Direction.BUY));
        assertTrue(provider.getTradeAmountLimit().isPresent(),
                "the buyer cap must not be bypassed while a rate is missing");
    }

    @Test
    void absoluteLimitRecomputesForANewQuoteUsingCachedRates() {
        PriceQuote xmrQuote = PriceQuote.fromAltCoinPrice(0.001, "XMR");
        Market xmrMarket = xmrQuote.getMarket();
        quotes.put(xmrMarket, xmrQuote);
        quotes.put(usdMarket, PriceQuote.fromFiatPrice(100_000, "USD"));
        when(marketSelection.getMarket()).thenReturn(xmrMarket);
        when(priceSelection.getPriceQuote()).thenReturn(xmrQuote);
        AbsoluteAmountLimitsProvider provider =
                new AbsoluteAmountLimitsProvider(marketPriceService, marketSelection, priceSelection);
        provider.initialize();
        long baseBefore = provider.getTradeAmountLimits().getMax().getBaseSideAmount().getValue();

        // BTC/USD vanishes, then the offer quote changes: the limit pair must be recomputed for
        // the new quote with the cached rates, not retained against the old quote.
        quotes.remove(usdMarket);
        PriceQuote newQuote = PriceQuote.fromAltCoinPrice(0.0015, "XMR");
        when(priceSelection.getPriceQuote()).thenReturn(newQuote);
        List.copyOf(quoteListeners).forEach(listener -> listener.accept(newQuote));

        long baseAfter = provider.getTradeAmountLimits().getMax().getBaseSideAmount().getValue();
        assertTrue(baseAfter < baseBefore, "a higher XMR price means fewer XMR for the same USD cap");
        assertEquals("XMR", provider.getTradeAmountLimits().getMax().getBaseSideAmount().getCode());
    }
}
