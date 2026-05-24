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

package bisq.bonded_roles.market_price;

import bisq.common.market.Market;
import bisq.common.market.MarketRepository;
import bisq.common.monetary.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MarketBasedAmountConversionTest {

    private static final Market BTC_USD = MarketRepository.getUSDBitcoinMarket();
    private static final Market BTC_USDC = MarketRepository.getBtcUsdcMarket();

    private MarketPriceService mockPriceServiceWithUsdAndUsdc() {
        MarketPriceService service = mock(MarketPriceService.class);

        PriceQuote usdQuote = PriceQuote.fromPrice(100000.0, "BTC", "USD");
        when(service.findMarketPriceQuote(BTC_USD)).thenReturn(Optional.of(usdQuote));

        PriceQuote usdcQuote = PriceQuote.fromPrice(100000.0, "BTC", "USDC");
        when(service.findMarketPriceQuote(BTC_USDC)).thenReturn(Optional.of(usdcQuote));

        return service;
    }

    @Test
    @DisplayName("btcToFiat for BTC/USDC returns Coin not Fiat")
    void btc_to_fiat_for_usdc_returns_coin() {
        MarketPriceService service = mockPriceServiceWithUsdAndUsdc();
        Monetary btc = Coin.asBtcFromFaceValue(0.01);

        Optional<Monetary> result = MarketBasedAmountConversion.btcToFiat(service, BTC_USDC, btc);
        assertTrue(result.isPresent());
        assertEquals("USDC", result.get().getCode());
        assertTrue(result.get().getValue() > 0);
    }

    @Test
    @DisplayName("btcToFiat for BTC/USD returns Fiat")
    void btc_to_fiat_for_usd_returns_fiat() {
        MarketPriceService service = mockPriceServiceWithUsdAndUsdc();
        Monetary btc = Coin.asBtcFromFaceValue(0.01);

        Optional<Monetary> result = MarketBasedAmountConversion.btcToFiat(service, BTC_USD, btc);
        assertTrue(result.isPresent());
        assertEquals("USD", result.get().getCode());
        assertInstanceOf(Fiat.class, result.get());
    }

    @Test
    @DisplayName("tradeAmountFromUsdAmount for BTC/USDC market produces Coin quote side")
    void trade_amount_from_usd_for_usdc_market() {
        MarketPriceService service = mockPriceServiceWithUsdAndUsdc();
        Fiat usdAmount = Fiat.fromFaceValue(100, "USD");

        TradeAmount tradeAmount = MarketBasedAmountConversion.tradeAmountFromUsdAmount(service, BTC_USDC, usdAmount);
        assertNotNull(tradeAmount);
        assertEquals("USDC", tradeAmount.getQuoteSideAmount().getCode());
        assertTrue(tradeAmount.getQuoteSideAmount().getValue() > 0);
        assertTrue(tradeAmount.getBaseSideAmount().getValue() > 0);
    }

    @Test
    @DisplayName("tradeAmountFromUsdAmount for BTC/USD market produces Fiat quote side")
    void trade_amount_from_usd_for_usd_market() {
        MarketPriceService service = mockPriceServiceWithUsdAndUsdc();
        Fiat usdAmount = Fiat.fromFaceValue(100, "USD");

        TradeAmount tradeAmount = MarketBasedAmountConversion.tradeAmountFromUsdAmount(service, BTC_USD, usdAmount);
        assertNotNull(tradeAmount);
        assertEquals("USD", tradeAmount.getQuoteSideAmount().getCode());
        assertInstanceOf(Fiat.class, tradeAmount.getQuoteSideAmount());
    }

    @Test
    @DisplayName("fiatToBtc for BTC/USDC converts USDC to BTC")
    void fiat_to_btc_for_usdc() {
        MarketPriceService service = mockPriceServiceWithUsdAndUsdc();
        Monetary usdcAmount = Coin.fromFaceValue(1000.0, "USDC");

        Optional<Monetary> result = MarketBasedAmountConversion.fiatToBtc(service, BTC_USDC, usdcAmount);
        assertTrue(result.isPresent());
        assertEquals("BTC", result.get().getCode());
        assertTrue(result.get().getValue() > 0);
    }
}
