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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AmountSelectionTest {
    private final Market market = MarketRepository.getUSDBitcoinMarket();
    private final PriceQuote priceQuote = PriceQuote.fromFiatPrice(100_000, "USD");

    private final List<Consumer<Market>> marketListeners = new ArrayList<>();
    private final List<Consumer<Direction>> directionListeners = new ArrayList<>();
    private final List<Consumer<PriceQuote>> priceQuoteListeners = new ArrayList<>();
    private final ObservableHashMap<PaymentMethod<?>, Account<?, ?>> accountByPaymentMethod = new ObservableHashMap<>();

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
        // The desktop slider controllers clamp incoming amount slider values against the last emitted
        // user-specific limit slider value and feed the clamped value back into the domain. If the amount
        // values were emitted first, they would be clamped against the stale limit of the old range.
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

    private AmountSelection createInitializedAmountSelection() {
        MarketPriceService marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.getMarketPriceQuoteOrThrow(market)).thenReturn(priceQuote);
        when(marketPriceService.findMarketPriceQuote(market)).thenReturn(java.util.Optional.of(priceQuote));

        MarketSelection marketSelection = mock(MarketSelection.class);
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

        PriceSelection priceSelection = mock(PriceSelection.class);
        when(priceSelection.getPriceQuote()).thenReturn(priceQuote);
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
                cookieStore);
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
