package bisq.offer.mu_sig.draft;

import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.common.monetary.TradeAmountConversion;
import bisq.offer.Direction;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CreateOfferDraftWorkflowTest {
    private Market defaultMarket;
    private Market usdBtcMarket;
    private Market xmrBtcMarket;
    private PriceQuote defaultMarketPriceQuote;
    private PriceQuote usdBtcPriceQuote;
    private PriceQuote xmrBtcPriceQuote;
    private TradeAmount defaultMarketDefaultTradeAmount;
    private TradeAmount usdBtcDefaultTradeAmount;
    private TradeAmount xmrBtcDefaultTradeAmount;
    private FakeMarketData marketData;
    private FakeCookieStore cookieStore;
    private CreateOfferDraftWorkflow workflow;

    @BeforeEach
    public void setUp() {
        defaultMarket = MarketRepository.getDefaultBtcFiatMarket();
        usdBtcMarket = MarketRepository.getUSDBitcoinMarket();
        xmrBtcMarket = MarketRepository.getXmrBtcMarket();

        defaultMarketPriceQuote = PriceQuote.fromPrice(50000,
                defaultMarket.getBaseCurrencyCode(),
                defaultMarket.getQuoteCurrencyCode());
        usdBtcPriceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        xmrBtcPriceQuote = PriceQuote.fromPrice(0.005, "XMR", "BTC");

        defaultMarketDefaultTradeAmount = TradeAmountConversion.toTradeAmount(defaultMarket,
                defaultMarketPriceQuote,
                Fiat.fromFaceValue(500, defaultMarket.getQuoteCurrencyCode()));
        usdBtcDefaultTradeAmount = TradeAmountConversion.toTradeAmount(usdBtcMarket,
                usdBtcPriceQuote,
                Fiat.fromFaceValue(500, "USD"));
        xmrBtcDefaultTradeAmount = TradeAmountConversion.toTradeAmount(xmrBtcMarket,
                xmrBtcPriceQuote,
                Coin.asBtcFromFaceValue(0.01));

        marketData = new FakeMarketData(usdBtcPriceQuote);
        marketData.put(defaultMarket, defaultMarketPriceQuote, defaultMarketDefaultTradeAmount);
        marketData.put(usdBtcMarket, usdBtcPriceQuote, usdBtcDefaultTradeAmount);
        marketData.put(xmrBtcMarket, xmrBtcPriceQuote, xmrBtcDefaultTradeAmount);

        cookieStore = new FakeCookieStore(Direction.SELL, false, true, false);
        workflow = new CreateOfferDraftWorkflow(marketData, cookieStore);
        workflow.onActivate();
    }

    @Test
    public void setMarketResetsPriceAndAmountsDeterministically() {
        workflow.setMarket(xmrBtcMarket);

        assertEquals(xmrBtcMarket, workflow.getMarket());
        assertEquals(xmrBtcPriceQuote, workflow.getPriceQuote());
        assertEquals(xmrBtcDefaultTradeAmount, workflow.getFixTradeAmount());
        assertEquals(xmrBtcDefaultTradeAmount, workflow.getMinTradeAmount());
        assertEquals(xmrBtcDefaultTradeAmount, workflow.getMaxTradeAmount());
        assertNotNull(workflow.getTradeAmountLimits());
        assertNotNull(workflow.getInputAmountLimits());
    }

    @Test
    public void setPriceQuoteKeepsQuoteInputAmountConstant() {
        workflow.setMarket(usdBtcMarket);
        workflow.setUseBaseCurrencyForAmountInput(false);
        workflow.setFixTradeAmountFromInputAmount(Fiat.fromFaceValue(500, "USD"));

        TradeAmount fixTradeAmountBefore = workflow.getFixTradeAmount();
        workflow.setPriceQuote(PriceQuote.fromFiatPrice(40000, "USD"));
        TradeAmount fixTradeAmountAfter = workflow.getFixTradeAmount();

        assertEquals(fixTradeAmountBefore.getQuoteSideAmount(), fixTradeAmountAfter.getQuoteSideAmount());
        assertEquals(Coin.asBtcFromFaceValue(0.0125), fixTradeAmountAfter.getBaseSideAmount());
    }

    @Test
    public void setDirectionRecomputesUserSpecificLimitAndKeepsAmountsStable() {
        workflow.setMarket(usdBtcMarket);
        TradeAmount fixTradeAmountBefore = workflow.getFixTradeAmount();

        workflow.setDirection(Direction.BUY);
        Optional<TradeAmount> buyLimit = workflow.getUserSpecificTradeAmountLimit();

        workflow.setDirection(Direction.SELL);

        assertTrue(buyLimit.isPresent());
        assertTrue(workflow.getUserSpecificTradeAmountLimit().isEmpty());
        assertEquals(fixTradeAmountBefore, workflow.getFixTradeAmount());
        assertEquals(List.of(Direction.BUY, Direction.SELL), cookieStore.persistedDirections);
    }

    @Test
    public void settersBeforeActivationOnlyMutateInputs() {
        FakeCookieStore nonActivatedCookieStore = new FakeCookieStore(Direction.SELL, false, true, false);
        CreateOfferDraftWorkflow nonActivatedWorkflow = new CreateOfferDraftWorkflow(marketData, nonActivatedCookieStore);

        nonActivatedWorkflow.setMarket(xmrBtcMarket);
        nonActivatedWorkflow.setDirection(Direction.BUY);

        assertEquals(xmrBtcMarket, nonActivatedWorkflow.getMarket());
        assertEquals(Direction.BUY, nonActivatedWorkflow.getDirection());
        assertNull(nonActivatedWorkflow.getPriceQuote());
        assertTrue(nonActivatedCookieStore.persistedDirections.isEmpty());
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
        public PriceQuote getMarketPriceQuote(Market market) {
            return Optional.ofNullable(priceQuoteByMarket.get(market))
                    .orElseThrow(() -> new IllegalStateException("Market price quote not available for " + market));
        }

        @Override
        public PriceQuote getBtcUsdPriceQuote() {
            return btcUsdPriceQuote;
        }

        @Override
        public TradeAmount getTradeAmountFromUsd(Market market, Fiat usdAmount) {
            return Optional.ofNullable(defaultTradeAmountByMarket.get(market))
                    .orElseThrow(() -> new IllegalStateException("Default trade amount not available for " + market));
        }
    }

    private static class FakeCookieStore implements CreateOfferDraftCookieStore {
        private final Direction defaultDirection;
        private final boolean defaultUseBaseForFiatMarkets;
        private final boolean defaultUseBaseForOtherMarkets;
        private final boolean defaultUseRangeAmount;
        private final List<Direction> persistedDirections = new ArrayList<>();
        private final List<InputModePreference> persistedInputModes = new ArrayList<>();
        private final List<Boolean> persistedUseRangeAmountValues = new ArrayList<>();

        private FakeCookieStore(Direction defaultDirection,
                                boolean defaultUseBaseForFiatMarkets,
                                boolean defaultUseBaseForOtherMarkets,
                                boolean defaultUseRangeAmount) {
            this.defaultDirection = defaultDirection;
            this.defaultUseBaseForFiatMarkets = defaultUseBaseForFiatMarkets;
            this.defaultUseBaseForOtherMarkets = defaultUseBaseForOtherMarkets;
            this.defaultUseRangeAmount = defaultUseRangeAmount;
        }

        @Override
        public Direction getDirection() {
            return defaultDirection;
        }

        @Override
        public boolean getUseBaseCurrencyForAmountInput(Market market) {
            return market.isBtcFiatMarket() ? defaultUseBaseForFiatMarkets : defaultUseBaseForOtherMarkets;
        }

        @Override
        public boolean getUseRangeAmount() {
            return defaultUseRangeAmount;
        }

        @Override
        public void persistDirection(Direction direction) {
            persistedDirections.add(direction);
        }

        @Override
        public void persistUseBaseCurrencyForAmountInput(Market market, boolean useBaseCurrencyForAmountInput) {
            persistedInputModes.add(new InputModePreference(market, useBaseCurrencyForAmountInput));
        }

        @Override
        public void persistUseRangeAmount(boolean useRangeAmount) {
            persistedUseRangeAmountValues.add(useRangeAmount);
        }
    }

    private record InputModePreference(Market market, boolean useBaseCurrencyForAmountInput) {
    }
}
