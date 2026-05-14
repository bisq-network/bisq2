package bisq.offer.mu_sig.draft;

import bisq.common.market.Market;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.common.monetary.TradeAmount;
import bisq.common.monetary.TradeAmountRange;
import bisq.offer.Direction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TradeAmountLimitsTest {

    @Test
    @DisplayName("to trade amount limits fiat market")
    public void to_trade_amount_limits_fiat_market() {
        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        PriceQuote priceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        PriceQuote btcUsdPriceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        PriceQuote btcFiatPriceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        Fiat minUsd = Fiat.fromFaceValue(100.0, "USD");
        Fiat maxUsd = Fiat.fromFaceValue(1000.0, "USD");

        TradeAmountRange limits = TradeAmountLimits.toTradeAmountLimits(
                market, priceQuote, btcUsdPriceQuote, btcFiatPriceQuote, minUsd, maxUsd);

        // Min: 100 USD -> 100/50000 = 0.002 BTC
        assertEquals(1000000, limits.getMin().getQuoteSideAmount().getValue()); // 100.0000 USD
        assertEquals(200000, limits.getMin().getBaseSideAmount().getValue()); // 0.002 BTC

        // Max: 1000 USD -> 1000/50000 = 0.02 BTC
        assertEquals(10000000, limits.getMax().getQuoteSideAmount().getValue()); // 1000.0000 USD
        assertEquals(2000000, limits.getMax().getBaseSideAmount().getValue()); // 0.02 BTC
    }

    @Test
    @DisplayName("to trade amount limits crypto market")
    public void to_trade_amount_limits_crypto_market() {
        Market market = new Market("XMR", "BTC", "Monero", "Bitcoin");
        PriceQuote priceQuote = PriceQuote.fromPrice(0.005, "XMR", "BTC");
        PriceQuote btcUsdPriceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        PriceQuote btcFiatPriceQuote = null; // Not used for non-fiat market
        Fiat minUsd = Fiat.fromFaceValue(100.0, "USD");
        Fiat maxUsd = Fiat.fromFaceValue(1000.0, "USD");

        TradeAmountRange limits = TradeAmountLimits.toTradeAmountLimits(
                market, priceQuote, btcUsdPriceQuote, btcFiatPriceQuote, minUsd, maxUsd);

        // 100 USD at 50000 USD/BTC = 0.002 BTC (Quote side for XMR/BTC market)
        // 0.002 BTC / 0.005 BTC/XMR = 0.4 XMR (Base side)
        assertEquals(200000, limits.getMin().getQuoteSideAmount().getValue()); // 0.002 BTC
        assertEquals(400000000000L, limits.getMin().getBaseSideAmount().getValue()); // 0.4 XMR

        // 1000 USD at 50000 USD/BTC = 0.02 BTC (Quote side)
        // 0.02 BTC / 0.005 BTC/XMR = 4 XMR (Base side)
        assertEquals(2000000, limits.getMax().getQuoteSideAmount().getValue()); // 0.02 BTC
        assertEquals(4000000000000L, limits.getMax().getBaseSideAmount().getValue()); // 4 XMR
    }

    @Test
    @DisplayName("to user specific trade amount limit")
    public void to_user_specific_trade_amount_limit() {
        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        PriceQuote priceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        PriceQuote btcUsdPriceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        PriceQuote btcFiatPriceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        Fiat limitUsd = Fiat.fromFaceValue(500.0, "USD");

        // Sell direction should return empty
        assertTrue(TradeAmountLimits.toUserSpecificTradeAmountLimit(Direction.SELL, market, priceQuote, btcUsdPriceQuote, btcFiatPriceQuote, limitUsd).isEmpty());

        // Buy direction
        Optional<TradeAmount> limit = TradeAmountLimits.toUserSpecificTradeAmountLimit(Direction.BUY, market, priceQuote, btcUsdPriceQuote, btcFiatPriceQuote, limitUsd);
        assertTrue(limit.isPresent());
        // 500 USD -> 500/50000 = 0.01 BTC
        assertEquals(5000000, limit.get().getQuoteSideAmount().getValue()); // 500.0000 USD
        assertEquals(1000000, limit.get().getBaseSideAmount().getValue()); // 0.01 BTC
    }

    @Test
    @DisplayName("get clamp limits")
    public void get_clamp_limits() {
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(100.0, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(1000.0, "USD"));
        TradeAmountRange protocolLimits = new TradeAmountRange(min, max);

        TradeAmount userLimit = new TradeAmount(Coin.asBtcFromFaceValue(5.0), Fiat.fromFaceValue(500.0, "USD"));
        Optional<TradeAmount> userSpecificLimit = Optional.of(userLimit);

        // includeUserSpecificTradeAmountLimit = false
        assertEquals(protocolLimits, TradeAmountLimits.getClampLimits(protocolLimits, userSpecificLimit, false));

        // includeUserSpecificTradeAmountLimit = true
        TradeAmountRange clampedLimits = TradeAmountLimits.getClampLimits(protocolLimits, userSpecificLimit, true);
        assertEquals(min, clampedLimits.getMin());
        assertEquals(userLimit, clampedLimits.getMax());

        // includeUserSpecificTradeAmountLimit = true, but user limit is empty
        assertEquals(protocolLimits, TradeAmountLimits.getClampLimits(protocolLimits, Optional.empty(), true));
    }

    @Test
    @DisplayName("get clamp limits user limit above protocol max uses protocol max")
    public void get_clamp_limits_user_limit_above_protocol_max_uses_protocol_max() {
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(100.0, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(1000.0, "USD"));
        TradeAmountRange protocolLimits = new TradeAmountRange(min, max);

        TradeAmount userLimitAboveProtocol = new TradeAmount(Coin.asBtcFromFaceValue(15.0), Fiat.fromFaceValue(1500.0, "USD"));
        TradeAmountRange clampedLimits = TradeAmountLimits.getClampLimits(protocolLimits, Optional.of(userLimitAboveProtocol), true);

        assertEquals(min, clampedLimits.getMin());
        assertEquals(max, clampedLimits.getMax());
    }

    @Test
    @DisplayName("get clamp limits user limit below protocol min uses protocol min as max")
    public void get_clamp_limits_user_limit_below_protocol_min_uses_protocol_min_as_max() {
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(100.0, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(1000.0, "USD"));
        TradeAmountRange protocolLimits = new TradeAmountRange(min, max);

        TradeAmount userLimitBelowProtocol = new TradeAmount(Coin.asBtcFromFaceValue(0.5), Fiat.fromFaceValue(50.0, "USD"));
        TradeAmountRange clampedLimits = TradeAmountLimits.getClampLimits(protocolLimits, Optional.of(userLimitBelowProtocol), true);

        assertEquals(min, clampedLimits.getMin());
        assertEquals(min, clampedLimits.getMax());
    }

    @Test
    @DisplayName("to trade amount limits throws if min amount exceeds max amount")
    public void to_trade_amount_limits_throws_if_min_amount_exceeds_max_amount() {
        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        PriceQuote priceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        PriceQuote btcUsdPriceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        PriceQuote btcFiatPriceQuote = PriceQuote.fromFiatPrice(50000, "USD");
        Fiat minUsd = Fiat.fromFaceValue(1000.0, "USD");
        Fiat maxUsd = Fiat.fromFaceValue(100.0, "USD");

        assertThrows(IllegalArgumentException.class, () -> TradeAmountLimits.toTradeAmountLimits(
                market, priceQuote, btcUsdPriceQuote, btcFiatPriceQuote, minUsd, maxUsd));
    }

    @Test
    @DisplayName("clamp trade amount")
    public void clamp_trade_amount() {
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(100.0, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(1000.0, "USD"));
        TradeAmountRange limits = new TradeAmountRange(min, max);

        // Within limits
        TradeAmount amount1 = new TradeAmount(Coin.asBtcFromFaceValue(5.0), Fiat.fromFaceValue(500.0, "USD"));
        assertEquals(amount1, TradeAmountLimits.clampTradeAmount(limits, amount1));

        // Below min
        TradeAmount amount2 = new TradeAmount(Coin.asBtcFromFaceValue(0.5), Fiat.fromFaceValue(50.0, "USD"));
        assertEquals(min, TradeAmountLimits.clampTradeAmount(limits, amount2));

        // Above max
        TradeAmount amount3 = new TradeAmount(Coin.asBtcFromFaceValue(15.0), Fiat.fromFaceValue(1500.0, "USD"));
        assertEquals(max, TradeAmountLimits.clampTradeAmount(limits, amount3));
    }

    @Test
    @DisplayName("clamp trade amount extended")
    public void clamp_trade_amount_extended() {
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(100.0, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(1000.0, "USD"));
        TradeAmountRange protocolLimits = new TradeAmountRange(min, max);

        TradeAmount userLimit = new TradeAmount(Coin.asBtcFromFaceValue(5.0), Fiat.fromFaceValue(500.0, "USD"));
        Optional<TradeAmount> userSpecificLimit = Optional.of(userLimit);

        TradeAmount amount = new TradeAmount(Coin.asBtcFromFaceValue(7.0), Fiat.fromFaceValue(700.0, "USD"));

        // includeUserSpecificTradeAmountLimit = false -> should use max (10 BTC)
        assertEquals(amount, TradeAmountLimits.clampTradeAmount(protocolLimits, userSpecificLimit, amount, false));

        // includeUserSpecificTradeAmountLimit = true -> should use userLimit (5 BTC)
        assertEquals(userLimit, TradeAmountLimits.clampTradeAmount(protocolLimits, userSpecificLimit, amount, true));
    }

    @Test
    @DisplayName("clamp base side amount")
    public void clamp_base_side_amount() {
        TradeAmount minAmount = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(100.0, "USD"));
        TradeAmount maxAmount = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(1000.0, "USD"));
        TradeAmountRange limits = new TradeAmountRange(minAmount, maxAmount);

        Monetary min = minAmount.getBaseSideAmount();
        Monetary max = maxAmount.getBaseSideAmount();

        // Within limits
        Monetary amount1 = Coin.asBtcFromFaceValue(5.0);
        assertEquals(amount1, TradeAmountLimits.clampBaseSideAmount(limits, amount1));

        // Below min
        Monetary amount2 = Coin.asBtcFromFaceValue(0.5);
        assertEquals(min, TradeAmountLimits.clampBaseSideAmount(limits, amount2));

        // Above max
        Monetary amount3 = Coin.asBtcFromFaceValue(15.0);
        assertEquals(max, TradeAmountLimits.clampBaseSideAmount(limits, amount3));
    }

    @Test
    @DisplayName("clamp quote side amount")
    public void clamp_quote_side_amount() {
        TradeAmount minAmount = new TradeAmount(Coin.asBtcFromFaceValue(1.0), Fiat.fromFaceValue(100.0, "USD"));
        TradeAmount maxAmount = new TradeAmount(Coin.asBtcFromFaceValue(10.0), Fiat.fromFaceValue(1000.0, "USD"));
        TradeAmountRange limits = new TradeAmountRange(minAmount, maxAmount);

        Monetary min = minAmount.getQuoteSideAmount();
        Monetary max = maxAmount.getQuoteSideAmount();

        // Within limits
        Monetary amount1 = Fiat.fromFaceValue(500.0, "USD");
        assertEquals(amount1, TradeAmountLimits.clampQuoteSideAmount(limits, amount1));

        // Below min
        Monetary amount2 = Fiat.fromFaceValue(50.0, "USD");
        assertEquals(min, TradeAmountLimits.clampQuoteSideAmount(limits, amount2));

        // Above max
        Monetary amount3 = Fiat.fromFaceValue(1500.0, "USD");
        assertEquals(max, TradeAmountLimits.clampQuoteSideAmount(limits, amount3));
    }
}
