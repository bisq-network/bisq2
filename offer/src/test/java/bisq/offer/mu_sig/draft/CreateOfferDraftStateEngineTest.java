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
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("initialize sets derived state and calls payment method updater")
    public void initialize_sets_derived_state_and_calls_payment_method_updater() {
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
    @DisplayName("apply direction changed returns false without pricing context")
    public void apply_direction_changed_returns_false_without_pricing_context() {
        boolean recalculated = stateEngine.applyDirectionChanged(Direction.BUY);

        assertFalse(recalculated);
        assertEquals(Direction.BUY, offerDraft.getDirection());
    }

    @Test
    @DisplayName("apply direction changed returns true with pricing context")
    public void apply_direction_changed_returns_true_with_pricing_context() {
        stateEngine.initialize(usdBtcMarket, Direction.SELL, false, false);

        boolean recalculated = stateEngine.applyDirectionChanged(Direction.BUY);

        assertTrue(recalculated);
        Optional<TradeAmount> userSpecificTradeAmountLimit = offerDraft.getUserSpecificTradeAmountLimit();
        assertTrue(userSpecificTradeAmountLimit.isPresent());
    }

    @Test
    @DisplayName("apply use base currency for amount input changed depends on derived state initialization")
    public void apply_use_base_currency_for_amount_input_changed_depends_on_derived_state_initialization() {
        assertFalse(stateEngine.applyUseBaseCurrencyForAmountInputChanged(true));

        stateEngine.initialize(usdBtcMarket, Direction.SELL, false, false);

        assertTrue(stateEngine.applyUseBaseCurrencyForAmountInputChanged(true));
        assertTrue(offerDraft.getUseBaseCurrencyForAmountInput());
    }

    @Test
    @DisplayName("recalculate trade amount constraints for selected payment rail clamps existing amounts")
    public void recalculate_trade_amount_constraints_for_selected_payment_rail_clamps_existing_amounts() {
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
