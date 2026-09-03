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

package bisq.offer.mu_sig.use_case.create_offer.amount;

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
import bisq.common.monetary.TradeAmount;
import bisq.common.observable.Pin;
import bisq.common.observable.map.ObservableHashMap;
import bisq.offer.Direction;
import bisq.offer.mu_sig.use_case.create_offer.direction.DirectionSelection;
import bisq.offer.mu_sig.use_case.create_offer.market.MarketSelection;
import bisq.offer.mu_sig.use_case.create_offer.payment_method.PaymentMethodSelection;
import bisq.offer.mu_sig.use_case.create_offer.price.PriceSelection;
import bisq.offer.mu_sig.use_case.dependencies.CreateOfferDraftCookieStore;
import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AmountSelectionTest {
    private final Market market = MarketRepository.getUSDBitcoinMarket();
    private final PriceQuote priceQuote = PriceQuote.fromFiatPrice(100_000, "USD");

    private final List<Consumer<Market>> marketListeners = new ArrayList<>();
    private final List<Runnable> contextListeners = new ArrayList<>();
    private final List<Consumer<Direction>> directionListeners = new ArrayList<>();
    private final List<Consumer<PriceQuote>> priceQuoteListeners = new ArrayList<>();
    private final ObservableHashMap<PaymentMethod<?>, Account<?, ?>> accountByPaymentMethod = new ObservableHashMap<>();
    private PriceSelection priceSelection;
    private MarketSelection marketSelection;
    private MarketPriceService marketPriceService;

    @Test
    public void userSpecificSliderValueIsRecomputedWhenPaymentMethodLimitChangesTheAmountRange() {
        AmountSelection amountSelection = createInitializedAmountSelection();
        selectPaymentMethod(FiatPaymentRail.ADVANCED_CASH);
        fireInitialState();

        Fiat userSpecificLimit = bisq.offer.mu_sig.use_case.create_offer.amount.limits.UserSpecificAmountLimitsProvider.getUserSpecificLimitInUsd();
        MonetaryRange initialRange = amountSelection.getInputAmountRange();
        double expectedInitialSliderValue = AmountSelection.toSliderValueFromAmount(userSpecificLimit, initialRange);
        assertEquals(expectedInitialSliderValue,
                amountSelection.getUserSpecificTradeAmountLimitAsSliderValue().orElseThrow(),
                1e-9);

        selectPaymentMethod(FiatPaymentRail.WISE);

        MonetaryRange changedRange = amountSelection.getInputAmountRange();
        assertNotEquals(initialRange.getMax().getValue(), changedRange.getMax().getValue(),
                "test setup must actually change the amount range");
        assertTrue(userSpecificLimit.getValue() < changedRange.getMax().getValue(),
                "test setup must keep the user specific limit inside the new range so its clamped value stays equal");
        double expectedSliderValue = AmountSelection.toSliderValueFromAmount(userSpecificLimit, changedRange);
        assertEquals(expectedSliderValue,
                amountSelection.getUserSpecificTradeAmountLimitAsSliderValue().orElseThrow(),
                1e-9);
    }

    @Test
    public void amountSliderValuesAreRecomputedWhenPaymentMethodLimitChangesTheAmountRange() {
        AmountSelection amountSelection = createInitializedAmountSelection();
        selectPaymentMethod(FiatPaymentRail.ADVANCED_CASH);
        fireInitialState();

        MonetaryRange initialRange = amountSelection.getInputAmountRange();
        double initialFixSliderValue = amountSelection.getFixAmountSliderValue();

        selectPaymentMethod(FiatPaymentRail.WISE);

        MonetaryRange changedRange = amountSelection.getInputAmountRange();
        assertNotEquals(initialRange.getMax().getValue(), changedRange.getMax().getValue(),
                "test setup must actually change the amount range");
        double expectedFixSliderValue = AmountSelection.toSliderValueFromAmount(
                amountSelection.toInputAmount(amountSelection.getFixTradeAmount()), changedRange);
        assertNotEquals(initialFixSliderValue, expectedFixSliderValue,
                "test setup must move the expected thumb position when the range changes");
        assertEquals(expectedFixSliderValue, amountSelection.getFixAmountSliderValue(), 1e-9);

        double expectedMinSliderValue = AmountSelection.toSliderValueFromAmount(
                amountSelection.toInputAmount(amountSelection.getMinTradeAmount()), changedRange);
        assertEquals(expectedMinSliderValue, amountSelection.getMinAmountSliderValue(), 1e-9);
        double expectedMaxSliderValue = AmountSelection.toSliderValueFromAmount(
                amountSelection.toInputAmount(amountSelection.getMaxTradeAmount()), changedRange);
        assertEquals(expectedMaxSliderValue, amountSelection.getMaxAmountSliderValue(), 1e-9);
    }

    @Test
    public void userSpecificSliderValueIsEmittedBeforeAmountSliderValuesOnRangeChange() {
        // The desktop slider controllers clamp the displayed thumb against the last emitted user-specific
        // limit slider value (display-only; origin separation prevents writeback). If the amount values
        // were emitted first, the thumb would be clamped against the stale limit of the old range.
        AmountSelection amountSelection = createInitializedAmountSelection();
        selectPaymentMethod(FiatPaymentRail.ADVANCED_CASH);
        fireInitialState();

        List<String> emissions = new ArrayList<>();
        amountSelection.userSpecificTradeAmountLimitAsSliderValueObservable().addObserver(value ->
                emissions.add("userSpecificLimit"));
        amountSelection.fixAmountSliderValueObservable().addObserver(value ->
                emissions.add("fixAmount"));
        amountSelection.minAmountSliderValueObservable().addObserver(value ->
                emissions.add("minAmount"));
        amountSelection.maxAmountSliderValueObservable().addObserver(value ->
                emissions.add("maxAmount"));
        emissions.clear();

        selectPaymentMethod(FiatPaymentRail.WISE);

        int firstLimitEmission = emissions.indexOf("userSpecificLimit");
        assertTrue(firstLimitEmission >= 0, "range change must emit the user specific limit slider value");
        for (String amountEmission : List.of("fixAmount", "minAmount", "maxAmount")) {
            int firstAmountEmission = emissions.indexOf(amountEmission);
            assertTrue(firstAmountEmission >= 0,
                    "range change must emit the " + amountEmission + " slider value");
            assertTrue(firstLimitEmission < firstAmountEmission,
                    "the user specific limit slider value must be emitted before " + amountEmission);
        }
    }


    @Test
    public void inputRangeAndSliderValuesAreRecomputedWhenInputSideChanges() {
        AmountSelection amountSelection = createInitializedAmountSelection();
        selectPaymentMethod(FiatPaymentRail.ADVANCED_CASH);
        fireInitialState();

        // The default input side is the quote (USD) side.
        MonetaryRange quoteSideRange = amountSelection.getInputAmountRange();
        assertEquals("USD", quoteSideRange.getMax().getCode());

        amountSelection.onSetUseBaseCurrencyForAmountInput(true);

        MonetaryRange baseSideRange = amountSelection.getInputAmountRange();
        assertEquals("BTC", baseSideRange.getMax().getCode(),
                "input range must be re-denominated on the base side after the input side switch");
        double expectedFixSliderValue = AmountSelection.toSliderValueFromAmount(
                amountSelection.toInputAmount(amountSelection.getFixTradeAmount()), baseSideRange);
        assertEquals(expectedFixSliderValue, amountSelection.getFixAmountSliderValue(), 1e-9);
    }


    @Test
    public void amountSelectionIsUsableAfterInitializeWithoutFiringDependencyListeners() {
        // The limit sub-providers must compute their current value during initialize(); their
        // market/price/direction listeners do not replay, so without that AmountLimitsProvider
        // never initializes and the amount input handlers silently ignore edits. fireInitialState()
        // is deliberately NOT called here.
        AmountSelection amountSelection = createInitializedAmountSelection();
        selectPaymentMethod(FiatPaymentRail.ADVANCED_CASH);

        assertNotNull(amountSelection.getInputAmountRange(),
                "initialize() plus a selected payment method must establish the amount limits without firing the dependency listeners");
    }


    @Test
    public void contextOnlyRefreshDoesNotRunThePassiveSideConversion() {
        AmountSelection amountSelection = createInitializedAmountSelection();
        selectPaymentMethod(FiatPaymentRail.ADVANCED_CASH);
        fireInitialState();
        var fixAmountBefore = amountSelection.getFixTradeAmount();

        // Make a passive-side conversion detectable: were the conversion to run during the
        // context refresh, it would consume this changed quote and move the passive side.
        when(priceSelection.getPriceQuote()).thenReturn(PriceQuote.fromFiatPrice(120_000, "USD"));

        // A market-context refresh (e.g. the BTC/USD leg moved while the offer quote did not)
        // recomputes the limits but must not convert the passive side of the selected amounts.
        List.copyOf(contextListeners).forEach(Runnable::run);

        assertEquals(fixAmountBefore, amountSelection.getFixTradeAmount());
    }


    @Test
    public void marketSwitchToPricelessMarketClearsAmountsAndReseedsOnQuoteArrival() {
        AmountSelection amountSelection = createInitializedAmountSelection();
        selectPaymentMethod(FiatPaymentRail.ADVANCED_CASH);
        fireInitialState();
        assertNotNull(amountSelection.getFixTradeAmount());

        // The EUR market has no price yet; the USD amounts must not survive the switch, or the
        // late-arriving EUR quote would be applied to USD-denominated values.
        Market eurMarket = new Market("BTC", "EUR", "Bitcoin", "Euro");
        when(marketSelection.getMarket()).thenReturn(eurMarket);
        List.copyOf(marketListeners).forEach(listener -> listener.accept(eurMarket));

        assertNull(amountSelection.getFixTradeAmount(),
                "stale amounts from the previous market must be cleared");
        assertNull(amountSelection.getInputAmountRange(),
                "the derived limit range must be cleared with the amounts");
        assertTrue(amountSelection.getUserSpecificTradeAmountLimitAsSliderValue().isEmpty(),
                "the slider marker must be cleared with the amounts");

        PriceQuote eurQuote = PriceQuote.fromFiatPrice(90_000, "EUR");
        when(marketPriceService.findMarketPriceQuote(eurMarket)).thenReturn(java.util.Optional.of(eurQuote));
        when(marketPriceService.getMarketPriceQuoteOrThrow(eurMarket)).thenReturn(eurQuote);
        when(priceSelection.getPriceQuote()).thenReturn(eurQuote);
        List.copyOf(priceQuoteListeners).forEach(listener -> listener.accept(eurQuote));

        assertNotNull(amountSelection.getFixTradeAmount(), "the default must reseed once the quote arrives");
        assertEquals("EUR", amountSelection.getFixTradeAmount().getQuoteSideAmount().getCode());
    }

    @Test
    public void initialSeedIsRepricedToTheResolvedOfferQuote() {
        marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.getMarketPriceQuoteOrThrow(market)).thenReturn(priceQuote);
        when(marketPriceService.findMarketPriceQuote(market)).thenReturn(java.util.Optional.of(priceQuote));
        // The restored offer quote carries a +10% offset relative to the market price. The
        // initial seed converts with the market price but publishes the base amount, which the
        // offer resolves at the OFFER quote - an unrepriced pair reviews a different amount
        // than the one shown.
        PriceQuote offsetOfferQuote = PriceQuote.fromFiatPrice(110_000, "USD");

        AmountSelection amountSelection = createAmountSelection(offsetOfferQuote);

        TradeAmount seeded = amountSelection.getFixTradeAmount();
        assertNotNull(seeded);
        assertEquals(offsetOfferQuote.toBaseSideMonetary(seeded.getQuoteSideAmount()),
                seeded.getBaseSideAmount(),
                "the initial seed must be consistent with the resolved offer quote");
    }

    @Test
    public void zeroMarketPriceBehavesAsUnpriced() {
        marketPriceService = mock(MarketPriceService.class);
        // A zero market price passes MarketPrice.verify (timestamp-only); the conversion must
        // degrade to the unpriced state instead of failing initialization.
        PriceQuote zeroQuote = PriceQuote.fromFiatPrice(0, "USD");
        when(marketPriceService.findMarketPriceQuote(any())).thenReturn(java.util.Optional.of(zeroQuote));
        when(marketPriceService.getMarketPriceQuoteOrThrow(any())).thenReturn(zeroQuote);

        AmountSelection amountSelection = createAmountSelection(null);

        assertNull(amountSelection.getFixTradeAmount(),
                "a zero market price is the unpriced state, not an initialization failure");
    }

    @Test
    public void lateSeededAmountsAreRepricedToTheResolvedOfferQuote() {
        // Unpriced start: the limits never initialize, so the amount listeners stay
        // unregistered until the first price arrives - the seed then happens during the
        // at-registration replay and no later quote event repriced it.
        AmountSelection amountSelection = createUnpricedAmountSelection();
        selectPaymentMethod(FiatPaymentRail.ADVANCED_CASH);
        List.copyOf(marketListeners).forEach(listener -> listener.accept(market));
        List.copyOf(directionListeners).forEach(listener -> listener.accept(Direction.BUY));
        assertNull(amountSelection.getFixTradeAmount());

        // The first price arrives while the restored offer quote carries a +10% offset. The
        // seeded pair must be consistent with the OFFER quote, not the raw market price - the
        // published offer converts with the offer quote.
        PriceQuote marketQuote = PriceQuote.fromFiatPrice(100_000, "USD");
        PriceQuote offsetOfferQuote = PriceQuote.fromFiatPrice(110_000, "USD");
        org.mockito.Mockito.doReturn(java.util.Optional.of(marketQuote))
                .when(marketPriceService).findMarketPriceQuote(market);
        org.mockito.Mockito.doReturn(marketQuote)
                .when(marketPriceService).getMarketPriceQuoteOrThrow(market);
        when(priceSelection.getPriceQuote()).thenReturn(offsetOfferQuote);
        List.copyOf(priceQuoteListeners).forEach(listener -> listener.accept(offsetOfferQuote));

        TradeAmount seeded = amountSelection.getFixTradeAmount();
        assertNotNull(seeded);
        assertEquals(offsetOfferQuote.toBaseSideMonetary(seeded.getQuoteSideAmount()),
                seeded.getBaseSideAmount(),
                "the seeded base side must be derived from the resolved offer quote");
    }

    @Test
    public void inputSideToggleWhileUnpricedDoesNotReapplyThePreviousMarketsLimits() {
        AmountSelection amountSelection = createInitializedAmountSelection();
        selectPaymentMethod(FiatPaymentRail.ADVANCED_CASH);
        fireInitialState();
        assertNotNull(amountSelection.getInputAmountRange());

        Market eurMarket = new Market("BTC", "EUR", "Bitcoin", "Euro");
        when(marketSelection.getMarket()).thenReturn(eurMarket);
        List.copyOf(marketListeners).forEach(listener -> listener.accept(eurMarket));
        assertNull(amountSelection.getInputAmountRange());

        // The input-side switch re-derives the range from the limit providers; retained
        // previous-market limits must not resurface under the new market.
        amountSelection.onSetUseBaseCurrencyForAmountInput(true);

        assertNull(amountSelection.getInputAmountRange(),
                "the previous market's limits must not be reapplied while the new market is unpriced");
    }

    @Test
    public void switchingBackToThePricedMarketRepopulatesLimitsAndAmounts() {
        AmountSelection amountSelection = createInitializedAmountSelection();
        selectPaymentMethod(FiatPaymentRail.ADVANCED_CASH);
        fireInitialState();

        Market eurMarket = new Market("BTC", "EUR", "Bitcoin", "Euro");
        when(marketSelection.getMarket()).thenReturn(eurMarket);
        List.copyOf(marketListeners).forEach(listener -> listener.accept(eurMarket));
        assertNull(amountSelection.getInputAmountRange());

        // Cleared-then-recomputed values are a real change, so the observers fire again; a
        // retained equal value would have been equality-gated into a permanently blank range.
        when(marketSelection.getMarket()).thenReturn(market);
        List.copyOf(marketListeners).forEach(listener -> listener.accept(market));

        assertNotNull(amountSelection.getInputAmountRange(),
                "returning to the priced market must repopulate the limits");
        assertNotNull(amountSelection.getFixTradeAmount());
        assertEquals("USD", amountSelection.getFixTradeAmount().getQuoteSideAmount().getCode());
    }

    @Test
    public void typingWhileTheLimitsAreClearedIsRefusedSoftly() {
        AmountSelection amountSelection = createInitializedAmountSelection();
        selectPaymentMethod(FiatPaymentRail.ADVANCED_CASH);
        fireInitialState();

        // Switch to a priced EUR market whose BTC/USD leg is unavailable: the offer quote is
        // present, but the limit providers cannot compute, so the ranges are cleared while
        // the initialized latch stays true.
        Market eurMarket = new Market("BTC", "EUR", "Bitcoin", "Euro");
        PriceQuote eurQuote = PriceQuote.fromFiatPrice(90_000, "EUR");
        org.mockito.Mockito.doReturn(java.util.Optional.empty())
                .when(marketPriceService).findMarketPriceQuote(any());
        org.mockito.Mockito.doThrow(new IllegalStateException("no price"))
                .when(marketPriceService).getMarketPriceQuoteOrThrow(any());
        when(marketSelection.getMarket()).thenReturn(eurMarket);
        when(priceSelection.getPriceQuote()).thenReturn(eurQuote);
        List.copyOf(marketListeners).forEach(listener -> listener.accept(eurMarket));
        List.copyOf(priceQuoteListeners).forEach(listener -> listener.accept(eurQuote));
        assertNull(amountSelection.getInputAmountRange());

        // Typing an amount now must be refused softly instead of clamping against a null range.
        amountSelection.onSetFixTradeAmountFromInputAmount(Fiat.fromFaceValue(100, "EUR"));

        assertNull(amountSelection.getFixTradeAmount(),
                "input while the limits are unavailable must not produce an amount");
    }

    @Test
    public void negativeMarketPriceLegBehavesAsUnpriced() {
        marketPriceService = mock(MarketPriceService.class);
        // A negative quote passes MarketPrice.verify (timestamp-only); converting with it
        // would seed a negative trade amount that only fails at spec materialization.
        PriceQuote negativeQuote = PriceQuote.fromFiatPrice(-100_000, "USD");
        when(marketPriceService.findMarketPriceQuote(any())).thenReturn(java.util.Optional.of(negativeQuote));
        when(marketPriceService.getMarketPriceQuoteOrThrow(any())).thenReturn(negativeQuote);

        AmountSelection amountSelection = createAmountSelection(null);

        assertNull(amountSelection.getFixTradeAmount(),
                "a non-positive market price is the unpriced state, not a seedable rate");
    }

    private AmountSelection createUnpricedAmountSelection() {
        marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.findMarketPriceQuote(any())).thenReturn(java.util.Optional.empty());
        when(marketPriceService.getMarketPriceQuoteOrThrow(any())).thenThrow(new IllegalStateException("no market price"));
        return createAmountSelection(null);
    }

    private AmountSelection createInitializedAmountSelection() {
        marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.getMarketPriceQuoteOrThrow(market)).thenReturn(priceQuote);
        when(marketPriceService.findMarketPriceQuote(market)).thenReturn(java.util.Optional.of(priceQuote));
        return createAmountSelection(priceQuote);
    }

    private AmountSelection createAmountSelection(PriceQuote initialPriceQuote) {

        marketSelection = mock(MarketSelection.class);
        when(marketSelection.getMarket()).thenReturn(market);
        when(marketSelection.addMarketListener(any())).thenAnswer(invocation -> {
            marketListeners.add(invocation.getArgument(0));
            return mock(Pin.class);
        });

        DirectionSelection directionSelection = mock(DirectionSelection.class);
        when(directionSelection.getDisplayDirection()).thenReturn(Direction.BUY);
        when(directionSelection.addDisplayDirectionListener(any())).thenAnswer(invocation -> {
            directionListeners.add(invocation.getArgument(0));
            return mock(Pin.class);
        });

        priceSelection = mock(PriceSelection.class);
        when(priceSelection.getPriceQuote()).thenReturn(initialPriceQuote);
        when(priceSelection.addMarketContextListener(any())).thenAnswer(invocation -> {
            contextListeners.add(invocation.getArgument(0));
            return mock(Pin.class);
        });
        when(priceSelection.addPriceQuoteListener(any())).thenAnswer(invocation -> {
            priceQuoteListeners.add(invocation.getArgument(0));
            return mock(Pin.class);
        });

        PaymentMethodSelection paymentMethodSelection = mock(PaymentMethodSelection.class);
        when(paymentMethodSelection.accountByPaymentMethodObservable()).thenReturn(accountByPaymentMethod);
        when(paymentMethodSelection.getAccountByPaymentMethod()).thenAnswer(invocation ->
                ImmutableMap.copyOf(accountByPaymentMethod));

        CreateOfferDraftCookieStore cookieStore = mock(CreateOfferDraftCookieStore.class);

        AmountSelection amountSelection = new AmountSelection(marketPriceService,
                marketSelection,
                directionSelection,
                paymentMethodSelection,
                priceSelection,
                cookieStore,
                new Object());
        amountSelection.initialize();
        return amountSelection;
    }

    private void fireInitialState() {
        List.copyOf(marketListeners).forEach(listener -> listener.accept(market));
        List.copyOf(directionListeners).forEach(listener -> listener.accept(Direction.BUY));
        List.copyOf(priceQuoteListeners).forEach(listener -> listener.accept(priceQuote));
    }

    private void selectPaymentMethod(FiatPaymentRail paymentRail) {
        PaymentMethod<?> paymentMethod = FiatPaymentMethod.fromPaymentRail(paymentRail);
        Account<?, ?> account = mock(Account.class);
        when(account.getPaymentMethod()).thenAnswer(invocation -> paymentMethod);
        accountByPaymentMethod.clear();
        accountByPaymentMethod.put(paymentMethod, account);
    }
}
