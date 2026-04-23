package bisq.offer.mu_sig.draft;

import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.MonetaryRange;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.common.monetary.TradeAmountConversion;
import bisq.common.monetary.TradeAmountRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AmountMappingServiceTest {
    private final AmountMappingService service = new AmountMappingService();

    @Test
    public void toTradeAmountFromInputAmountClampsToLimits() {
        Market market = MarketRepository.getUSDBitcoinMarket();
        PriceQuote priceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        TradeAmountRange limits = createTradeAmountLimits(market, priceQuote, 10, 5000);

        TradeAmount tradeAmount = service.toTradeAmountFromInputAmount(market,
                priceQuote,
                Fiat.fromFaceValue(9000, "USD"),
                limits);

        assertEquals(Fiat.fromFaceValue(5000, "USD"), tradeAmount.getQuoteSideAmount());
    }

    @Test
    public void toSliderValueAndBackIsStable() {
        Market market = MarketRepository.getUSDBitcoinMarket();
        PriceQuote priceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        TradeAmountRange limits = createTradeAmountLimits(market, priceQuote, 10, 5000);
        TradeAmount tradeAmount = TradeAmountConversion.toTradeAmount(market,
                priceQuote,
                Fiat.fromFaceValue(800, "USD"));
        MonetaryRange inputAmountLimits = service.toInputAmountLimits(limits, false);

        double sliderValue = service.toSliderValue(tradeAmount, limits, inputAmountLimits, false);
        TradeAmount fromSliderValue = service.toTradeAmountFromSliderValue(market,
                priceQuote,
                tradeAmount,
                limits,
                inputAmountLimits,
                false,
                sliderValue);

        assertEquals(tradeAmount, fromSliderValue);
    }

    @Test
    public void toUpdatedPassiveAmountKeepsQuoteInputAmount() {
        Market market = MarketRepository.getUSDBitcoinMarket();
        PriceQuote oldPriceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        PriceQuote newPriceQuote = PriceQuote.fromFiatPrice(40000, "USD");
        TradeAmountRange limits = createTradeAmountLimits(market, oldPriceQuote, 10, 10000);
        TradeAmount tradeAmount = TradeAmountConversion.toTradeAmount(market,
                oldPriceQuote,
                Fiat.fromFaceValue(500, "USD"));

        TradeAmount updatedTradeAmount = service.toUpdatedPassiveAmount(market,
                newPriceQuote,
                tradeAmount,
                limits,
                limits,
                false);

        assertEquals(Fiat.fromFaceValue(500, "USD"), updatedTradeAmount.getQuoteSideAmount());
        assertEquals(Coin.asBtcFromFaceValue(0.0125), updatedTradeAmount.getBaseSideAmount());
    }

    private static TradeAmountRange createTradeAmountLimits(Market market,
                                                            PriceQuote priceQuote,
                                                            long minUsd,
                                                            long maxUsd) {
        TradeAmount min = TradeAmountConversion.toTradeAmount(market,
                priceQuote,
                Fiat.fromFaceValue(minUsd, "USD"));
        TradeAmount max = TradeAmountConversion.toTradeAmount(market,
                priceQuote,
                Fiat.fromFaceValue(maxUsd, "USD"));
        return new TradeAmountRange(min, max);
    }
}
