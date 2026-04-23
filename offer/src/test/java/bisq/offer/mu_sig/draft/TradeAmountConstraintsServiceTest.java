package bisq.offer.mu_sig.draft;

import bisq.account.payment_method.fiat.FiatPaymentRail;
import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.offer.Direction;
import bisq.offer.mu_sig.draft.dependencies.CreateOfferDraftMarketData;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TradeAmountConstraintsServiceTest {

    @Test
    public void computeUsesPaymentRailSpecificMaxLimit() {
        Market market = MarketRepository.getUSDBitcoinMarket();
        PriceQuote offerPriceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        PriceQuote marketPriceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        FakeMarketData marketData = new FakeMarketData(PriceQuote.fromFiatPrice(50000, "USD"));
        TradeAmountConstraintsService service = new TradeAmountConstraintsService(marketData);

        TradeAmountConstraints constraints = service.compute(market,
                Direction.BUY,
                offerPriceQuote,
                marketPriceQuote,
                FiatPaymentRail.ACH_TRANSFER);

        assertEquals(Fiat.fromFaceValue(5000, "USD"), constraints.tradeAmountLimits().getMax().getQuoteSideAmount());
        assertEquals(Fiat.fromFaceValue(4000, "USD"),
                constraints.userSpecificTradeAmountLimit().orElseThrow().getQuoteSideAmount());
    }

    @Test
    public void computeWithNoPaymentRailFallsBackToProtocolLimit() {
        Market market = MarketRepository.getUSDBitcoinMarket();
        PriceQuote offerPriceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        PriceQuote marketPriceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        FakeMarketData marketData = new FakeMarketData(PriceQuote.fromFiatPrice(50000, "USD"));
        TradeAmountConstraintsService service = new TradeAmountConstraintsService(marketData);

        TradeAmountConstraints constraints = service.compute(market,
                Direction.SELL,
                offerPriceQuote,
                marketPriceQuote,
                null);

        assertEquals(Fiat.fromFaceValue(10000, "USD"), constraints.tradeAmountLimits().getMax().getQuoteSideAmount());
        assertTrue(constraints.userSpecificTradeAmountLimit().isEmpty());
    }

    private static class FakeMarketData implements CreateOfferDraftMarketData {
        private final PriceQuote btcUsdPriceQuote;

        private FakeMarketData(PriceQuote btcUsdPriceQuote) {
            this.btcUsdPriceQuote = btcUsdPriceQuote;
        }

        @Override
        public PriceQuote getBtcUsdPriceQuote() {
            return btcUsdPriceQuote;
        }

        @Override
        public PriceQuote getMarketPriceQuote(Market market) {
            throw new UnsupportedOperationException("Not used in this test");
        }

        @Override
        public TradeAmount getTradeAmountFromUsd(Market market, Fiat usdAmount) {
            throw new UnsupportedOperationException("Not used in this test");
        }
    }
}
