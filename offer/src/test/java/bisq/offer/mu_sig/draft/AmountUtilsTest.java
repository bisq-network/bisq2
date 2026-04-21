package bisq.offer.mu_sig.draft;

import bisq.common.market.Market;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.MonetaryRange;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.common.monetary.TradeAmountRange;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AmountUtilsTest {

    @Test
    public void testToInputAmount_UseBaseCurrency_WithinLimits() {
        TradeAmount tradeAmount = new TradeAmount(Coin.asBtcFromFaceValue(5.0), Fiat.fromFaceValue(250000.0, "USD"));
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(50000.0, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(500000.0, "USD"));
        TradeAmountRange limits = new TradeAmountRange(min, max);

        Monetary result = AmountUtils.toInputAmount(tradeAmount, limits, true);

        assertEquals(tradeAmount.getBaseSideAmount(), result);
        assertEquals(Coin.asBtcFromFaceValue(5.0), result);
    }

    @Test
    public void testToInputAmount_UseBaseCurrency_BelowMin() {
        TradeAmount tradeAmount = new TradeAmount(Coin.asBtcFromFaceValue(0.5), Fiat.fromFaceValue(25000.0, "USD"));
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(50000.0, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(500000.0, "USD"));
        TradeAmountRange limits = new TradeAmountRange(min, max);

        Monetary result = AmountUtils.toInputAmount(tradeAmount, limits, true);

        // Should be clamped to min
        assertEquals(min.getBaseSideAmount(), result);
        assertEquals(Coin.asBtcFromFaceValue(1.0), result);
    }

    @Test
    public void testToInputAmount_UseBaseCurrency_AboveMax() {
        TradeAmount tradeAmount = new TradeAmount(Coin.asBtcFromFaceValue(15.0), Fiat.fromFaceValue(750000.0, "USD"));
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(50000.0, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(500000.0, "USD"));
        TradeAmountRange limits = new TradeAmountRange(min, max);

        Monetary result = AmountUtils.toInputAmount(tradeAmount, limits, true);

        // Should be clamped to max
        assertEquals(max.getBaseSideAmount(), result);
        assertEquals(Coin.asBtcFromFaceValue(10.0), result);
    }

    @Test
    public void testToInputAmount_UseQuoteCurrency_WithinLimits() {
        TradeAmount tradeAmount = new TradeAmount(Coin.asBtcFromFaceValue(5.0), Fiat.fromFaceValue(250000.0, "USD"));
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(50000.0, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(500000.0, "USD"));
        TradeAmountRange limits = new TradeAmountRange(min, max);

        Monetary result = AmountUtils.toInputAmount(tradeAmount, limits, false);

        assertEquals(tradeAmount.getQuoteSideAmount(), result);
        assertEquals(Fiat.fromFaceValue(250000.0, "USD"), result);
    }

    @Test
    public void testToInputAmount_UseQuoteCurrency_BelowMin() {
        TradeAmount tradeAmount = new TradeAmount(Coin.asBtcFromFaceValue(0.5), Fiat.fromFaceValue(25000.0, "USD"));
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(50000.0, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(500000.0, "USD"));
        TradeAmountRange limits = new TradeAmountRange(min, max);

        Monetary result = AmountUtils.toInputAmount(tradeAmount, limits, false);

        // Should be clamped to min
        assertEquals(min.getQuoteSideAmount(), result);
        assertEquals(Fiat.fromFaceValue(50000.0, "USD"), result);
    }

    @Test
    public void testToInputAmount_UseQuoteCurrency_AboveMax() {
        TradeAmount tradeAmount = new TradeAmount(Coin.asBtcFromFaceValue(15.0), Fiat.fromFaceValue(750000.0, "USD"));
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(50000.0, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(500000.0, "USD"));
        TradeAmountRange limits = new TradeAmountRange(min, max);

        Monetary result = AmountUtils.toInputAmount(tradeAmount, limits, false);

        // Should be clamped to max
        assertEquals(max.getQuoteSideAmount(), result);
        assertEquals(Fiat.fromFaceValue(500000.0, "USD"), result);
    }

    @Test
    public void testToPassiveAmount_UseBaseCurrency_WithinLimits() {
        TradeAmount tradeAmount = new TradeAmount(Coin.asBtcFromFaceValue(5.0), Fiat.fromFaceValue(250000.0, "USD"));
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(50000.0, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(500000.0, "USD"));
        TradeAmountRange limits = new TradeAmountRange(min, max);

        Monetary result = AmountUtils.toPassiveAmount(tradeAmount, limits, true);

        // When useBaseCurrencyForAmountInput is true, passive amount is quote side
        assertEquals(tradeAmount.getQuoteSideAmount(), result);
        assertEquals(Fiat.fromFaceValue(250000.0, "USD"), result);
    }

    @Test
    public void testToPassiveAmount_UseBaseCurrency_BelowMin() {
        TradeAmount tradeAmount = new TradeAmount(Coin.asBtcFromFaceValue(0.5), Fiat.fromFaceValue(25000.0, "USD"));
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(50000.0, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(500000.0, "USD"));
        TradeAmountRange limits = new TradeAmountRange(min, max);

        Monetary result = AmountUtils.toPassiveAmount(tradeAmount, limits, true);

        // Should be clamped to min quote side
        assertEquals(min.getQuoteSideAmount(), result);
        assertEquals(Fiat.fromFaceValue(50000.0, "USD"), result);
    }

    @Test
    public void testToPassiveAmount_UseBaseCurrency_AboveMax() {
        TradeAmount tradeAmount = new TradeAmount(Coin.asBtcFromFaceValue(15.0), Fiat.fromFaceValue(750000.0, "USD"));
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(50000.0, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(500000.0, "USD"));
        TradeAmountRange limits = new TradeAmountRange(min, max);

        Monetary result = AmountUtils.toPassiveAmount(tradeAmount, limits, true);

        // Should be clamped to max quote side
        assertEquals(max.getQuoteSideAmount(), result);
        assertEquals(Fiat.fromFaceValue(500000.0, "USD"), result);
    }

    @Test
    public void testToPassiveAmount_UseQuoteCurrency_WithinLimits() {
        TradeAmount tradeAmount = new TradeAmount(Coin.asBtcFromFaceValue(5.0), Fiat.fromFaceValue(250000.0, "USD"));
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(50000.0, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(500000.0, "USD"));
        TradeAmountRange limits = new TradeAmountRange(min, max);

        Monetary result = AmountUtils.toPassiveAmount(tradeAmount, limits, false);

        // When useBaseCurrencyForAmountInput is false, passive amount is base side
        assertEquals(tradeAmount.getBaseSideAmount(), result);
        assertEquals(Coin.asBtcFromFaceValue(5.0), result);
    }

    @Test
    public void testToPassiveAmount_UseQuoteCurrency_BelowMin() {
        TradeAmount tradeAmount = new TradeAmount(Coin.asBtcFromFaceValue(0.5), Fiat.fromFaceValue(25000.0, "USD"));
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(50000.0, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(500000.0, "USD"));
        TradeAmountRange limits = new TradeAmountRange(min, max);

        Monetary result = AmountUtils.toPassiveAmount(tradeAmount, limits, false);

        // Should be clamped to min base side
        assertEquals(min.getBaseSideAmount(), result);
        assertEquals(Coin.asBtcFromFaceValue(1.0), result);
    }

    @Test
    public void testToPassiveAmount_UseQuoteCurrency_AboveMax() {
        TradeAmount tradeAmount = new TradeAmount(Coin.asBtcFromFaceValue(15.0), Fiat.fromFaceValue(750000.0, "USD"));
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(50000.0, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(500000.0, "USD"));
        TradeAmountRange limits = new TradeAmountRange(min, max);

        Monetary result = AmountUtils.toPassiveAmount(tradeAmount, limits, false);

        // Should be clamped to max base side
        assertEquals(max.getBaseSideAmount(), result);
        assertEquals(Coin.asBtcFromFaceValue(10.0), result);
    }

    @Test
    public void testToInputAmount_And_ToPassiveAmount_AreInverse() {
        TradeAmount tradeAmount = new TradeAmount(Coin.asBtcFromFaceValue(5.0), Fiat.fromFaceValue(250000.0, "USD"));
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(50000.0, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(500000.0, "USD"));
        TradeAmountRange limits = new TradeAmountRange(min, max);

        // When useBaseCurrencyForAmountInput is true
        Monetary inputAmountBase = AmountUtils.toInputAmount(tradeAmount, limits, true);
        Monetary passiveAmountBase = AmountUtils.toPassiveAmount(tradeAmount, limits, true);
        assertEquals(tradeAmount.getBaseSideAmount(), inputAmountBase);
        assertEquals(tradeAmount.getQuoteSideAmount(), passiveAmountBase);

        // When useBaseCurrencyForAmountInput is false
        Monetary inputAmountQuote = AmountUtils.toInputAmount(tradeAmount, limits, false);
        Monetary passiveAmountQuote = AmountUtils.toPassiveAmount(tradeAmount, limits, false);
        assertEquals(tradeAmount.getQuoteSideAmount(), inputAmountQuote);
        assertEquals(tradeAmount.getBaseSideAmount(), passiveAmountQuote);
    }

    @Test
    public void testToSliderValue_AtMin() {
        Monetary inputAmount = Fiat.fromFaceValue(100.0, "USD");
        Monetary min = Fiat.fromFaceValue(100.0, "USD");
        Monetary max = Fiat.fromFaceValue(1000.0, "USD");
        MonetaryRange limits = new MonetaryRange(min, max);

        double sliderValue = AmountUtils.toSliderValue(inputAmount, limits);

        assertEquals(0.0, sliderValue, 0.0001);
    }

    @Test
    public void testToSliderValue_AtMax() {
        Monetary inputAmount = Fiat.fromFaceValue(1000.0, "USD");
        Monetary min = Fiat.fromFaceValue(100.0, "USD");
        Monetary max = Fiat.fromFaceValue(1000.0, "USD");
        MonetaryRange limits = new MonetaryRange(min, max);

        double sliderValue = AmountUtils.toSliderValue(inputAmount, limits);

        assertEquals(1.0, sliderValue, 0.0001);
    }

    @Test
    public void testToSliderValue_AtMiddle() {
        Monetary inputAmount = Fiat.fromFaceValue(550.0, "USD");
        Monetary min = Fiat.fromFaceValue(100.0, "USD");
        Monetary max = Fiat.fromFaceValue(1000.0, "USD");
        MonetaryRange limits = new MonetaryRange(min, max);

        double sliderValue = AmountUtils.toSliderValue(inputAmount, limits);

        assertEquals(0.5, sliderValue, 0.0001);
    }

    @Test
    public void testToSliderValue_WhenMinEqualsMax() {
        Monetary inputAmount = Fiat.fromFaceValue(100.0, "USD");
        Monetary min = Fiat.fromFaceValue(100.0, "USD");
        Monetary max = Fiat.fromFaceValue(100.0, "USD");
        MonetaryRange limits = new MonetaryRange(min, max);

        double sliderValue = AmountUtils.toSliderValue(inputAmount, limits);

        // When min == max, should return 0 to avoid division by zero
        assertEquals(0.0, sliderValue, 0.0001);
    }

    @Test
    public void testToTradeAmountFromSliderValue_AtMin() {
        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        PriceQuote priceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        Monetary min = Fiat.fromFaceValue(100.0, "USD");
        Monetary max = Fiat.fromFaceValue(1000.0, "USD");
        MonetaryRange limits = new MonetaryRange(min, max);
        Monetary inputAmount = Fiat.fromFaceValue(500.0, "USD");

        TradeAmount result = AmountUtils.toTradeAmountFromSliderValue(market, priceQuote, limits, inputAmount, 0.0);

        assertEquals(1000000L, result.getQuoteSideAmount().getValue()); // 100 USD
    }

    @Test
    public void testToTradeAmountFromSliderValue_AtMax() {
        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        PriceQuote priceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        Monetary min = Fiat.fromFaceValue(100.0, "USD");
        Monetary max = Fiat.fromFaceValue(1000.0, "USD");
        MonetaryRange limits = new MonetaryRange(min, max);
        Monetary inputAmount = Fiat.fromFaceValue(500.0, "USD");

        TradeAmount result = AmountUtils.toTradeAmountFromSliderValue(market, priceQuote, limits, inputAmount, 1.0);

        assertEquals(10000000L, result.getQuoteSideAmount().getValue()); // 1000 USD
    }

    @Test
    public void testToTradeAmountFromSliderValue_AtMiddle() {
        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        PriceQuote priceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        Monetary min = Fiat.fromFaceValue(100.0, "USD");
        Monetary max = Fiat.fromFaceValue(1000.0, "USD");
        MonetaryRange limits = new MonetaryRange(min, max);
        Monetary inputAmount = Fiat.fromFaceValue(500.0, "USD");

        TradeAmount result = AmountUtils.toTradeAmountFromSliderValue(market, priceQuote, limits, inputAmount, 0.5);

        assertEquals(5500000L, result.getQuoteSideAmount().getValue()); // 550 USD
    }

    @Test
    public void testSliderValue_RoundTrip() {
        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        PriceQuote priceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        Monetary min = Fiat.fromFaceValue(100.0, "USD");
        Monetary max = Fiat.fromFaceValue(1000.0, "USD");
        MonetaryRange limits = new MonetaryRange(min, max);
        Monetary inputAmount = Fiat.fromFaceValue(750.0, "USD");

        // Convert to slider value
        double sliderValue = AmountUtils.toSliderValue(inputAmount, limits);

        // Convert back to trade amount
        TradeAmount tradeAmount = AmountUtils.toTradeAmountFromSliderValue(market, priceQuote, limits, inputAmount, sliderValue);

        // Should get the same amount back (within rounding tolerance)
        long originalValue = inputAmount.getValue();
        long roundTripValue = tradeAmount.getQuoteSideAmount().getValue();
        long diff = Math.abs(originalValue - roundTripValue);
        assertTrue(diff <= 1, "Round trip conversion should preserve value within rounding tolerance. Diff: " + diff);
    }
}
