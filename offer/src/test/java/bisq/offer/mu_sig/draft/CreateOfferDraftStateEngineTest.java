package bisq.offer.mu_sig.draft;

import bisq.account.payment_method.PaymentRail;
import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.common.monetary.TradeAmountConversion;
import bisq.offer.Direction;
import bisq.offer.mu_sig.draft.dependencies.CreateOfferDraftMarketData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreateOfferDraftStateEngineTest {
    private Market usdBtcMarket;
    private PriceQuote usdBtcPriceQuote;
    private TradeAmount usdBtcDefaultTradeAmount;
    private CreateOfferDraft offerDraft;
    private FakeMarketData marketData;
    private CreateOfferDraftStateEngine stateEngine;
    private AtomicInteger paymentMethodUpdateCalls;
    private AtomicReference<PaymentRail> selectedPaymentRail;

    @BeforeEach
    public void setUp() {
        usdBtcMarket = MarketRepository.getUSDBitcoinMarket();
        usdBtcPriceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        usdBtcDefaultTradeAmount = TradeAmountConversion.toTradeAmount(usdBtcMarket,
                usdBtcPriceQuote,
                Fiat.fromFaceValue(500, "USD"));

        offerDraft = new CreateOfferDraft();
        marketData = new FakeMarketData(usdBtcPriceQuote);
        marketData.put(usdBtcMarket, usdBtcPriceQuote, usdBtcDefaultTradeAmount);

        paymentMethodUpdateCalls = new AtomicInteger();
        selectedPaymentRail = new AtomicReference<>();

        stateEngine = new CreateOfferDraftStateEngine(offerDraft,
                marketData,
                new TradeAmountConstraintsService(marketData),
                new AmountMappingService(),
                selectedPaymentRail::get,
                paymentMethodUpdateCalls::incrementAndGet,
                CreateOfferDraftWorkflow.DEFAULT_TRADE_AMOUNT_IN_USD);
    }

    @Test
    public void initializeSetsDerivedStateAndCallsPaymentMethodUpdater() {
        stateEngine.initialize(usdBtcMarket, Direction.SELL, false, true);

        assertEquals(usdBtcMarket, offerDraft.getMarket());
        assertEquals(Direction.SELL, offerDraft.getDirection());
        assertEquals(usdBtcPriceQuote, offerDraft.getPriceQuote());
        assertEquals(usdBtcDefaultTradeAmount, offerDraft.getFixTradeAmount());
        assertEquals(usdBtcDefaultTradeAmount, offerDraft.getMinTradeAmount());
        assertEquals(usdBtcDefaultTradeAmount, offerDraft.getMaxTradeAmount());
        assertNotNull(offerDraft.getTradeAmountLimits());
        assertNotNull(offerDraft.getInputAmountLimits());
        assertEquals(1, paymentMethodUpdateCalls.get());
    }

    @Test
    public void applyDirectionChangedReturnsFalseWithoutPricingContext() {
        boolean recalculated = stateEngine.applyDirectionChanged(Direction.BUY);

        assertFalse(recalculated);
        assertEquals(Direction.BUY, offerDraft.getDirection());
    }

    @Test
    public void applyDirectionChangedReturnsTrueWithPricingContext() {
        stateEngine.initialize(usdBtcMarket, Direction.SELL, false, false);

        boolean recalculated = stateEngine.applyDirectionChanged(Direction.BUY);

        assertTrue(recalculated);
        Optional<TradeAmount> userSpecificTradeAmountLimit = offerDraft.getUserSpecificTradeAmountLimit();
        assertTrue(userSpecificTradeAmountLimit.isPresent());
    }

    @Test
    public void applyUseBaseCurrencyForAmountInputChangedDependsOnDerivedStateInitialization() {
        assertFalse(stateEngine.applyUseBaseCurrencyForAmountInputChanged(true));

        stateEngine.initialize(usdBtcMarket, Direction.SELL, false, false);

        assertTrue(stateEngine.applyUseBaseCurrencyForAmountInputChanged(true));
        assertTrue(offerDraft.getUseBaseCurrencyForAmountInput());
    }

    @Test
    public void recalculateTradeAmountConstraintsForSelectedPaymentRailClampsExistingAmounts() {
        stateEngine.initialize(usdBtcMarket, Direction.SELL, false, false);

        TradeAmount nineThousandUsd = TradeAmountConversion.toTradeAmount(usdBtcMarket,
                usdBtcPriceQuote,
                Fiat.fromFaceValue(9000, "USD"));
        stateEngine.setFixTradeAmount(nineThousandUsd);
        assertEquals(Fiat.fromFaceValue(9000, "USD"), offerDraft.getFixTradeAmount().getQuoteSideAmount());

        selectedPaymentRail.set(FiatPaymentRail.ACH_TRANSFER);
        stateEngine.recalculateTradeAmountConstraintsForSelectedPaymentRail();

        assertEquals(Fiat.fromFaceValue(5000, "USD"), offerDraft.getFixTradeAmount().getQuoteSideAmount());
    }

    private static class FakeMarketData implements CreateOfferDraftMarketData {
        private final Map<Market, PriceQuote> priceQuoteByMarket = new HashMap<>();
        private final Map<Market, TradeAmount> defaultTradeAmountByMarket = new HashMap<>();
        private final PriceQuote btcUsdPriceQuote;

        private FakeMarketData(PriceQuote btcUsdPriceQuote) {
            this.btcUsdPriceQuote = btcUsdPriceQuote;
        }

        private void put(Market market, PriceQuote priceQuote, TradeAmount defaultTradeAmount) {
            priceQuoteByMarket.put(market, priceQuote);
            defaultTradeAmountByMarket.put(market, defaultTradeAmount);
        }

        @Override
        public PriceQuote getBtcUsdPriceQuote() {
            return btcUsdPriceQuote;
        }

        @Override
        public PriceQuote getMarketPriceQuote(Market market) {
            return Optional.ofNullable(priceQuoteByMarket.get(market))
                    .orElseThrow(() -> new IllegalStateException("Market price quote not available for " + market));
        }

        @Override
        public TradeAmount getTradeAmountFromUsd(Market market, Fiat usdAmount) {
            return Optional.ofNullable(defaultTradeAmountByMarket.get(market))
                    .orElseThrow(() -> new IllegalStateException("Default trade amount not available for " + market));
        }
    }
}
