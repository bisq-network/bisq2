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

package bisq.offer.mu_sig.use_case.create_offer.price;

import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.PriceQuote;
import bisq.common.observable.map.ObservableHashMap;
import bisq.offer.mu_sig.use_case.create_offer.market.MarketSelection;
import bisq.offer.mu_sig.use_case.dependencies.CreateOfferDraftCookieStore;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class PriceSelectionTest {
    private final Market usdMarket = MarketRepository.getUSDBitcoinMarket();

    private final ObservableHashMap<Market, MarketPrice> marketPriceMap = new ObservableHashMap<>();
    private final Map<Market, PriceQuote> quotes = new HashMap<>();
    private final List<PriceQuote> quoteEmissions = new ArrayList<>();
    private MarketPriceService marketPriceService;
    private CreateOfferDraftCookieStore cookieStore;
    private MarketSelection marketSelection;
    private PriceSelection priceSelection;
    private final Object draftLock = new Object();

    @BeforeEach
    void setUp() {
        marketPriceService = mock(MarketPriceService.class);
        when(marketPriceService.getMarketPriceByCurrencyMap()).thenReturn(marketPriceMap);
        when(marketPriceService.findMarketPriceQuote(any())).thenAnswer(invocation ->
                Optional.ofNullable(quotes.get(invocation.getArgument(0, Market.class))));
        cookieStore = mock(CreateOfferDraftCookieStore.class);
        marketSelection = new MarketSelection(draftLock);
        priceSelection = new PriceSelection(marketPriceService, marketSelection, cookieStore, draftLock);
    }

    private void initializeAt(Market market, double marketPriceValue) {
        setMarketPriceSilently(market, marketPriceValue);
        marketSelection.onSetMarket(market);
        priceSelection.initialize();
        priceSelection.addPriceQuoteListener(quoteEmissions::add);
        quoteEmissions.clear();
    }

    private void setMarketPriceSilently(Market market, double value) {
        quotes.put(market, PriceQuote.fromFiatPrice(value, market.getQuoteCurrencyCode()));
    }

    private void tick(Market market, double value) {
        setMarketPriceSilently(market, value);
        marketPriceMap.put(market, mock(MarketPrice.class));
    }

    private PriceQuote marketQuote(Market market) {
        return quotes.get(market);
    }

    @Test
    void floatingQuoteTracksUpwardMarketTickWithFreshLimits() {
        initializeAt(usdMarket, 100_000);
        assertEquals(marketQuote(usdMarket), priceSelection.getPriceQuote());

        tick(usdMarket, 200_000);

        // A stale range from the previous price would clamp a 0% quote of 200,000 to the old
        // +50% maximum of 150,000.
        assertEquals(marketQuote(usdMarket), priceSelection.getPriceQuote());
        assertEquals(0d, priceSelection.getPricePercentage());
    }

    @Test
    void floatingQuoteTracksDownwardMarketTickWithFreshLimits() {
        initializeAt(usdMarket, 200_000);

        tick(usdMarket, 100_000);

        assertEquals(marketQuote(usdMarket), priceSelection.getPriceQuote());
    }

    @Test
    void floatingEndpointsTrackTicksExactlyAndKeepThePercentage() {
        initializeAt(usdMarket, 100_000);
        for (double endpoint : new double[]{-0.1, 0.5}) {
            priceSelection.onSetPricePercentage(endpoint);
            assertEquals(endpoint, priceSelection.getPricePercentage());

            tick(usdMarket, 200_000);
            assertEquals(endpoint, priceSelection.getPricePercentage());
            assertEquals(PriceUtil.fromMarketPriceMarkup(marketQuote(usdMarket), endpoint),
                    priceSelection.getPriceQuote());

            tick(usdMarket, 100_000);
            assertEquals(endpoint, priceSelection.getPricePercentage());
            assertEquals(PriceUtil.fromMarketPriceMarkup(marketQuote(usdMarket), endpoint),
                    priceSelection.getPriceQuote());
        }
    }

    @Test
    void floatingPercentageSurvivesLowResolutionRounding() {
        // Sub-unit quote regime: a DOGE-like market at satoshi-level prices, where the inverse
        // quote-to-percentage derivation does not round-trip exactly.
        PriceQuote altQuote = PriceQuote.fromAltCoinPrice(0.00000192, "DOGE");
        Market altMarket = altQuote.getMarket();
        quotes.put(altMarket, altQuote);
        marketSelection.onSetMarket(altMarket);
        priceSelection.initialize();

        priceSelection.onSetPricePercentage(0.0469);
        assertEquals(0.0469, priceSelection.getPricePercentage());

        quotes.put(altMarket, PriceQuote.fromAltCoinPrice(0.00000193, "DOGE"));
        marketPriceMap.put(altMarket, mock(MarketPrice.class));
        assertEquals(0.0469, priceSelection.getPricePercentage());

        quotes.put(altMarket, PriceQuote.fromAltCoinPrice(0.00000192, "DOGE"));
        marketPriceMap.put(altMarket, mock(MarketPrice.class));
        assertEquals(0.0469, priceSelection.getPricePercentage());
    }

    @Test
    void typedPercentageBelowMinimumClampsAndSticks() {
        initializeAt(usdMarket, 100_000);

        priceSelection.onSetPricePercentage(-0.5);

        assertEquals(-0.1, priceSelection.getPricePercentage());
        assertEquals(PriceUtil.fromMarketPriceMarkup(marketQuote(usdMarket), -0.1), priceSelection.getPriceQuote());
        verify(cookieStore).persistPricePercentage(usdMarket, -0.1);

        // A same-price map mutation must not move or reject the clamped value.
        marketPriceMap.put(usdMarket, mock(MarketPrice.class));
        assertEquals(-0.1, priceSelection.getPricePercentage());
    }

    @Test
    void typedPercentageAboveMaximumClampsAndSticks() {
        initializeAt(usdMarket, 100_000);

        priceSelection.onSetPricePercentage(9.99);

        assertEquals(0.5, priceSelection.getPricePercentage());
        assertEquals(PriceUtil.fromMarketPriceMarkup(marketQuote(usdMarket), 0.5), priceSelection.getPriceQuote());
    }

    @Test
    void fixedQuoteSurvivesUpwardAndDownwardTicksWhileDeviationDisplayFollows() {
        initializeAt(usdMarket, 50_000);
        priceSelection.onSetUseFixPrice(true);
        priceSelection.onSetFixedPriceQuote(PriceQuote.fromFiatPrice(50_125, "USD"));
        PriceQuote fixedQuote = priceSelection.getPriceQuote();
        quoteEmissions.clear();

        tick(usdMarket, 100_000);
        assertEquals(fixedQuote, priceSelection.getPriceQuote());
        assertTrue(quoteEmissions.isEmpty(), "a market tick must not publish a quote change in fixed mode");
        assertEquals(PriceUtil.getPercentageToMarketPrice(marketQuote(usdMarket), fixedQuote),
                priceSelection.getPricePercentage(), "the displayed deviation follows the market");

        tick(usdMarket, 25_000);
        assertEquals(fixedQuote, priceSelection.getPriceQuote());
        assertTrue(quoteEmissions.isEmpty());
        assertEquals(Optional.of(marketQuote(usdMarket)), priceSelection.getObservedMarketPriceQuote(),
                "the snapshot source must pair with the last processed transition");
    }

    @Test
    void reactivationReplayIsReadOnly() {
        initializeAt(usdMarket, 100_000);
        priceSelection.onSetUseFixPrice(true);
        priceSelection.onSetFixedPriceQuote(PriceQuote.fromFiatPrice(101_000, "USD"));
        PriceQuote fixedQuote = priceSelection.getPriceQuote();
        double deviation = priceSelection.getPricePercentage();
        tick(usdMarket, 200_000);
        double deviationAfterTick = priceSelection.getPricePercentage();

        // Step re-entry replays all observers with current values; nothing may change.
        List<PriceQuote> replayedQuotes = new ArrayList<>();
        priceSelection.priceQuoteObservable().addObserver(replayedQuotes::add);
        priceSelection.pricePercentageObservable().addObserver(value -> {
        });
        priceSelection.useFixPriceObservable().addObserver(value -> {
        });
        marketPriceMap.addObserver(() -> {
        });

        assertEquals(List.of(fixedQuote), replayedQuotes);
        assertEquals(fixedQuote, priceSelection.getPriceQuote());
        assertEquals(deviationAfterTick, priceSelection.getPricePercentage());
        assertTrue(priceSelection.getUseFixPrice());
    }

    @Test
    void marketSelectionUsesItsSilentTransition() {
        initializeAt(usdMarket, 100_000);
        PriceQuote eurQuote = PriceQuote.fromFiatPrice(90_000, "EUR");
        Market eurMarket = eurQuote.getMarket();
        quotes.put(eurMarket, eurQuote);
        when(cookieStore.getPricePercentage(eurMarket)).thenReturn(0.05);

        marketSelection.onSetMarket(eurMarket);

        assertTrue(quoteEmissions.isEmpty(), "a market switch must not notify quote listeners");
        assertEquals(0.05, priceSelection.getPricePercentage());
        assertEquals(PriceUtil.fromMarketPriceMarkup(eurQuote, 0.05), priceSelection.getPriceQuote());
    }

    @Test
    void quoteListenersReceiveOnlyChangedFinalClampedQuote() {
        initializeAt(usdMarket, 100_000);

        priceSelection.onSetPricePercentage(-0.5);
        assertEquals(List.of(PriceUtil.fromMarketPriceMarkup(marketQuote(usdMarket), -0.1)), quoteEmissions);

        quoteEmissions.clear();
        // Clamping onto the already-stored bound must not emit.
        priceSelection.onSetPricePercentage(-0.7);
        assertTrue(quoteEmissions.isEmpty());
        assertEquals(-0.1, priceSelection.getPricePercentage());
    }

    @Test
    void missingMarketPriceKeepsLastStateAndLaterRecovers() {
        initializeAt(usdMarket, 100_000);
        PriceQuote before = priceSelection.getPriceQuote();

        quotes.remove(usdMarket);
        marketPriceMap.put(usdMarket, mock(MarketPrice.class));
        assertEquals(before, priceSelection.getPriceQuote());

        priceSelection.onSetPricePercentage(0.2);
        assertEquals(before, priceSelection.getPriceQuote(), "input without a market price is ignored");

        tick(usdMarket, 120_000);
        assertEquals(marketQuote(usdMarket), priceSelection.getPriceQuote(), "a later valid update reconciles");
    }

    @Test
    void marketTicksDoNotPersist() {
        initializeAt(usdMarket, 100_000);
        priceSelection.onSetPricePercentage(0.1);
        verify(cookieStore).persistPricePercentage(usdMarket, 0.1);

        tick(usdMarket, 200_000);
        tick(usdMarket, 50_000);

        verify(cookieStore, times(1)).persistPricePercentage(any(), anyDouble());
    }

    @Test
    void fixedQuoteEditPersistsTheBoundedEquivalentPercentage() {
        initializeAt(usdMarket, 100_000);
        priceSelection.onSetUseFixPrice(true);

        priceSelection.onSetFixedPriceQuote(PriceQuote.fromFiatPrice(120_000, "USD"));

        double deviation = PriceUtil.getPercentageToMarketPrice(marketQuote(usdMarket), priceSelection.getPriceQuote());
        verify(cookieStore).persistPricePercentage(usdMarket, deviation);
    }

    @Test
    void fixedQuoteInputIsClampedAgainstTheCurrentRange() {
        initializeAt(usdMarket, 100_000);
        priceSelection.onSetUseFixPrice(true);

        priceSelection.onSetFixedPriceQuote(PriceQuote.fromFiatPrice(400_000, "USD"));

        assertEquals(PriceUtil.fromMarketPriceMarkup(marketQuote(usdMarket), 0.5), priceSelection.getPriceQuote());
    }

    @Test
    void recommittingTheUnchangedFixedQuoteAfterAMarketMoveDoesNotReclampIt() {
        initializeAt(usdMarket, 100_000);
        priceSelection.onSetUseFixPrice(true);
        PriceQuote fixedQuote = PriceQuote.fromFiatPrice(100_000, "USD");
        priceSelection.onSetFixedPriceQuote(fixedQuote);

        tick(usdMarket, 200_000);

        // A focus-loss recommit of the unchanged value is not an edit; clamping it against the
        // moved range (minimum 180,000) would rewrite the fixed offer on a no-op blur.
        priceSelection.onSetFixedPriceQuote(fixedQuote);

        assertEquals(fixedQuote, priceSelection.getPriceQuote());
    }

    @Test
    void restoredFixedQuoteDeviationIsRecomputedFromTheQuantizedQuote() {
        when(cookieStore.getUseFixPrice(usdMarket)).thenReturn(true);
        when(cookieStore.getPricePercentage(usdMarket)).thenReturn(0.005);
        // At a 121-unit market price the 0.5% markup quantizes away in the integer quote value,
        // so the persisted percentage no longer describes the restored quote.
        initializeAt(usdMarket, 0.0121);

        double deviationOfRestoredQuote = PriceUtil.getPercentageToMarketPrice(
                marketQuote(usdMarket), priceSelection.getPriceQuote());
        assertEquals(deviationOfRestoredQuote, priceSelection.getPricePercentage(),
                "the displayed deviation must describe the quantized authoritative quote, not the cookie");
    }

    @Test
    void lateSeededFixedQuoteDeviationIsRecomputedFromTheQuantizedQuote() {
        when(cookieStore.getUseFixPrice(usdMarket)).thenReturn(true);
        when(cookieStore.getPricePercentage(usdMarket)).thenReturn(0.005);
        // No price at initialization: the fixed quote stays null until the first tick seeds it.
        marketSelection.onSetMarket(usdMarket);
        priceSelection.initialize();
        assertNull(priceSelection.getPriceQuote());

        // At a 121-unit market price the 0.5% markup quantizes in the integer quote value, so
        // the persisted percentage no longer describes the seeded quote.
        tick(usdMarket, 0.0121);

        double deviationOfSeededQuote = PriceUtil.getPercentageToMarketPrice(
                marketQuote(usdMarket), priceSelection.getPriceQuote());
        assertEquals(deviationOfSeededQuote, priceSelection.getPricePercentage(),
                "the deviation must describe the quantized seeded quote, not the cookie");
    }

    @Test
    void modeTransitionsPreserveAndAdopt() {
        initializeAt(usdMarket, 100_000);
        priceSelection.onSetPricePercentage(0.2);
        PriceQuote floatingQuote = priceSelection.getPriceQuote();

        // Floating -> fixed preserves the resolved quote verbatim.
        priceSelection.onSetUseFixPrice(true);
        assertEquals(floatingQuote, priceSelection.getPriceQuote());
        verify(cookieStore).persistUseFixPrice(usdMarket, true);

        // The market moves; the fixed quote's deviation drifts far below the floating minimum.
        tick(usdMarket, 200_000);
        assertEquals(floatingQuote, priceSelection.getPriceQuote());

        // Fixed -> floating adopts the clamped deviation as the authoritative percentage.
        quoteEmissions.clear();
        priceSelection.onSetUseFixPrice(false);
        assertEquals(-0.1, priceSelection.getPricePercentage());
        assertEquals(PriceUtil.fromMarketPriceMarkup(marketQuote(usdMarket), -0.1), priceSelection.getPriceQuote());
        verify(cookieStore).persistPricePercentage(usdMarket, -0.1);
        assertEquals(1, quoteEmissions.size());
    }

    @Test
    void createAndGetPriceSpecUsesAuthoritativeSide() {
        initializeAt(usdMarket, 100_000);
        assertInstanceOf(MarketPriceSpec.class, priceSelection.createAndGetPriceSpec());

        priceSelection.onSetPricePercentage(0.1);
        FloatPriceSpec floatSpec = assertInstanceOf(FloatPriceSpec.class, priceSelection.createAndGetPriceSpec());
        assertEquals(0.1, floatSpec.getPercentage());

        priceSelection.onSetUseFixPrice(true);
        PriceQuote fixedQuote = priceSelection.getPriceQuote();
        tick(usdMarket, 200_000);
        FixPriceSpec fixSpec = assertInstanceOf(FixPriceSpec.class, priceSelection.createAndGetPriceSpec());
        assertEquals(fixedQuote, fixSpec.getPriceQuote());
    }

    @Test
    void marketTickAndUserInputAreSerialized() throws Exception {
        initializeAt(usdMarket, 100_000);

        CountDownLatch tickInsideTransition = new CountDownLatch(1);
        CountDownLatch releaseTick = new CountDownLatch(1);
        AtomicBoolean firstEmission = new AtomicBoolean(true);
        priceSelection.addPriceQuoteListener(quote -> {
            if (firstEmission.getAndSet(false)) {
                tickInsideTransition.countDown();
                try {
                    releaseTick.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        Thread tickThread = new Thread(() -> tick(usdMarket, 200_000));
        tickThread.start();
        assertTrue(tickInsideTransition.await(5, TimeUnit.SECONDS));

        Thread inputThread = new Thread(() -> priceSelection.onSetPricePercentage(0.1));
        inputThread.start();
        inputThread.join(300);
        assertTrue(inputThread.isAlive(), "user input must wait for the running market transition");

        releaseTick.countDown();
        tickThread.join(5_000);
        inputThread.join(5_000);
        assertFalse(inputThread.isAlive());

        // The user input formed the final coherent pair against the post-tick market price.
        assertEquals(0.1, priceSelection.getPricePercentage());
        assertEquals(PriceUtil.fromMarketPriceMarkup(marketQuote(usdMarket), 0.1), priceSelection.getPriceQuote());
    }

    @Test
    void percentageInputIsIgnoredWhileFixed() {
        initializeAt(usdMarket, 100_000);
        priceSelection.onSetUseFixPrice(true);
        PriceQuote fixedQuote = priceSelection.getPriceQuote();
        quoteEmissions.clear();

        // E.g. a UI projection replayed at step re-entry: never input while fixed.
        priceSelection.onSetPricePercentage(-0.5);

        assertEquals(fixedQuote, priceSelection.getPriceQuote());
        assertTrue(quoteEmissions.isEmpty());
        verify(cookieStore, never()).persistPricePercentage(usdMarket, -0.1);
    }

    @Test
    void fixedToFloatingIsRefusedWithoutAMarketPrice() {
        initializeAt(usdMarket, 100_000);
        priceSelection.onSetUseFixPrice(true);
        PriceQuote fixedQuote = priceSelection.getPriceQuote();

        quotes.remove(usdMarket);
        priceSelection.onSetUseFixPrice(false);

        assertTrue(priceSelection.getUseFixPrice(), "the transition is refused without a usable market context");
        assertEquals(fixedQuote, priceSelection.getPriceQuote());
        verify(cookieStore, never()).persistUseFixPrice(usdMarket, false);
    }

    @Test
    void marketSwitchToAPricelessMarketClearsTheQuoteAndSeedsOnArrival() {
        initializeAt(usdMarket, 100_000);
        PriceQuote eurQuote = PriceQuote.fromFiatPrice(90_000, "EUR");
        Market eurMarket = eurQuote.getMarket();
        // No EUR price yet.
        marketSelection.onSetMarket(eurMarket);

        assertNull(priceSelection.getPriceQuote(), "never retain a quote from another market");
        assertEquals(Optional.empty(), priceSelection.getObservedMarketPriceQuote());

        tick(eurMarket, 90_000);
        assertEquals(marketQuote(eurMarket), priceSelection.getPriceQuote());
    }

    @Test
    void fixedQuoteIsSeededWhenTheFirstPriceArrives() {
        when(cookieStore.getUseFixPrice(usdMarket)).thenReturn(true);
        when(cookieStore.getPricePercentage(usdMarket)).thenReturn(0.05);
        // No price yet at initialization.
        marketSelection.onSetMarket(usdMarket);
        priceSelection.initialize();
        priceSelection.addPriceQuoteListener(quoteEmissions::add);
        assertNull(priceSelection.getPriceQuote());

        tick(usdMarket, 100_000);

        assertEquals(PriceUtil.fromMarketPriceMarkup(marketQuote(usdMarket), 0.05), priceSelection.getPriceQuote(),
                "the fixed quote is seeded from the persisted restoration percentage");
        assertEquals(1, quoteEmissions.size());
    }
}
