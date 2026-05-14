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
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AmountUtilsTest {

    @Test
    @DisplayName("to input amount use base currency within limits")
    public void to_input_amount_use_base_currency_within_limits() {
        TradeAmount tradeAmount = new TradeAmount(Coin.asBtcFromFaceValue(5.0), Fiat.fromFaceValue(250000.0, "USD"));
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(50000.0, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(500000.0, "USD"));
        TradeAmountRange limits = new TradeAmountRange(min, max);

        Monetary result = AmountUtils.toInputAmount(tradeAmount, limits, true);

        assertEquals(tradeAmount.getBaseSideAmount(), result);
        assertEquals(Coin.asBtcFromFaceValue(5.0), result);
    }

    @Test
    @DisplayName("to input amount use base currency below min")
    public void to_input_amount_use_base_currency_below_min() {
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
    @DisplayName("to input amount use base currency above max")
    public void to_input_amount_use_base_currency_above_max() {
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
    @DisplayName("to input amount use quote currency within limits")
    public void to_input_amount_use_quote_currency_within_limits() {
        TradeAmount tradeAmount = new TradeAmount(Coin.asBtcFromFaceValue(5.0), Fiat.fromFaceValue(250000.0, "USD"));
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(50000.0, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(500000.0, "USD"));
        TradeAmountRange limits = new TradeAmountRange(min, max);

        Monetary result = AmountUtils.toInputAmount(tradeAmount, limits, false);

        assertEquals(tradeAmount.getQuoteSideAmount(), result);
        assertEquals(Fiat.fromFaceValue(250000.0, "USD"), result);
    }

    @Test
    @DisplayName("to input amount use quote currency below min")
    public void to_input_amount_use_quote_currency_below_min() {
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
    @DisplayName("to input amount use quote currency above max")
    public void to_input_amount_use_quote_currency_above_max() {
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
    @DisplayName("to passive amount use base currency within limits")
    public void to_passive_amount_use_base_currency_within_limits() {
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
    @DisplayName("to passive amount use base currency below min")
    public void to_passive_amount_use_base_currency_below_min() {
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
    @DisplayName("to passive amount use base currency above max")
    public void to_passive_amount_use_base_currency_above_max() {
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
    @DisplayName("to passive amount use quote currency within limits")
    public void to_passive_amount_use_quote_currency_within_limits() {
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
    @DisplayName("to passive amount use quote currency below min")
    public void to_passive_amount_use_quote_currency_below_min() {
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
    @DisplayName("to passive amount use quote currency above max")
    public void to_passive_amount_use_quote_currency_above_max() {
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
    @DisplayName("to input amount and to passive amount are inverse")
    public void to_input_amount_and_to_passive_amount_are_inverse() {
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
    @DisplayName("to slider value at min")
    public void to_slider_value_at_min() {
        Monetary inputAmount = Fiat.fromFaceValue(100.0, "USD");
        Monetary min = Fiat.fromFaceValue(100.0, "USD");
        Monetary max = Fiat.fromFaceValue(1000.0, "USD");
        MonetaryRange limits = new MonetaryRange(min, max);

        double sliderValue = AmountUtils.toSliderValue(inputAmount, limits);

        assertEquals(0.0, sliderValue, 0.0001);
    }

    @Test
    @DisplayName("to slider value at max")
    public void to_slider_value_at_max() {
        Monetary inputAmount = Fiat.fromFaceValue(1000.0, "USD");
        Monetary min = Fiat.fromFaceValue(100.0, "USD");
        Monetary max = Fiat.fromFaceValue(1000.0, "USD");
        MonetaryRange limits = new MonetaryRange(min, max);

        double sliderValue = AmountUtils.toSliderValue(inputAmount, limits);

        assertEquals(1.0, sliderValue, 0.0001);
    }

    @Test
    @DisplayName("to slider value at middle")
    public void to_slider_value_at_middle() {
        Monetary inputAmount = Fiat.fromFaceValue(550.0, "USD");
        Monetary min = Fiat.fromFaceValue(100.0, "USD");
        Monetary max = Fiat.fromFaceValue(1000.0, "USD");
        MonetaryRange limits = new MonetaryRange(min, max);

        double sliderValue = AmountUtils.toSliderValue(inputAmount, limits);

        assertEquals(0.5, sliderValue, 0.0001);
    }

    @Test
    @DisplayName("to slider value when min equals max")
    public void to_slider_value_when_min_equals_max() {
        Monetary inputAmount = Fiat.fromFaceValue(100.0, "USD");
        Monetary min = Fiat.fromFaceValue(100.0, "USD");
        Monetary max = Fiat.fromFaceValue(100.0, "USD");
        MonetaryRange limits = new MonetaryRange(min, max);

        double sliderValue = AmountUtils.toSliderValue(inputAmount, limits);

        // When min == max, should return 0 to avoid division by zero
        assertEquals(0.0, sliderValue, 0.0001);
    }

    @Test
    @DisplayName("to trade amount from slider value at min")
    public void to_trade_amount_from_slider_value_at_min() {
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
    @DisplayName("to trade amount from slider value at max")
    public void to_trade_amount_from_slider_value_at_max() {
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
    @DisplayName("to trade amount from slider value at middle")
    public void to_trade_amount_from_slider_value_at_middle() {
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
    @DisplayName("slider value round trip")
    public void slider_value_round_trip() {
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
