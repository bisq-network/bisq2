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

package bisq.offer.amount;

import bisq.common.market.Market;
import bisq.common.monetary.Coin;
import bisq.common.monetary.Fiat;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.QuoteSideFixedAmountSpec;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OfferAmountUtilTest {

    @Test
    void calculateSecurityDepositAsBTCUsesPercentageAndCode() {
        Monetary input = Coin.asBtcFromValue(100_000_000L);
        Monetary deposit = OfferAmountUtil.calculateSecurityDepositAsBTC(input, 0.25);
        assertEquals("BTC", deposit.getCode());
        assertEquals(25_000_000L, deposit.getValue());
    }

    @Test
    void calculateSecurityDepositAsBTCRoundsHalfUp() {
        Monetary input = Coin.asBtcFromValue(3L);
        Monetary deposit = OfferAmountUtil.calculateSecurityDepositAsBTC(input, 0.5);
        assertEquals(2L, deposit.getValue());
    }

    @Test
    void calculateSecurityDepositAsBTCUsesBaseSideForBtcFiatMarket() {
        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        Monetary baseSideMonetary = Coin.asBtcFromValue(100_000_000L);
        Monetary quoteSideMonetary = Fiat.fromValue(5_000_000L, "USD");

        Monetary deposit = OfferAmountUtil.calculateSecurityDepositAsBTC(market, baseSideMonetary, quoteSideMonetary, 0.10);

        assertEquals("BTC", deposit.getCode());
        assertEquals(10_000_000L, deposit.getValue());
    }

    @Test
    void calculateSecurityDepositAsBTCUsesQuoteSideForCryptoBtcMarket() {
        Market market = new Market("XMR", "BTC", "Monero", "Bitcoin");
        Monetary baseSideMonetary = Coin.fromValue(10_000_000_000L, "XMR");
        Monetary quoteSideMonetary = Coin.asBtcFromValue(50_000_000L);

        Monetary deposit = OfferAmountUtil.calculateSecurityDepositAsBTC(market, baseSideMonetary, quoteSideMonetary, 0.10);

        assertEquals("BTC", deposit.getCode());
        assertEquals(5_000_000L, deposit.getValue());
    }

    @Test
    void calculateSecurityDepositAsBTCThrowsWhenCodeIsNotBtc() {
        Monetary nonBtcMonetary = Fiat.fromValue(10_000L, "USD");

        assertThrows(IllegalArgumentException.class, () ->
                OfferAmountUtil.calculateSecurityDepositAsBTC(nonBtcMonetary, 0.10));
    }

    @Test
    void resolvedQuoteOverloadsRejectAQuoteFromAnotherMarket() {
        Market eurMarket = new Market("BTC", "EUR", "Bitcoin", "Euro");
        PriceQuote usdQuote = PriceQuote.fromFiatPrice(100_000, "USD");
        // 4,000.00 EUR quote-side fixed spec: converting it with a BTC/USD quote would silently
        // treat the EUR amount as USD.
        AmountSpec amountSpec = new QuoteSideFixedAmountSpec(40_000_000);

        assertThrows(IllegalArgumentException.class, () ->
                OfferAmountUtil.findBaseSideFixedAmount(usdQuote, amountSpec, eurMarket));
        assertThrows(IllegalArgumentException.class, () ->
                OfferAmountUtil.findBaseSideMinAmount(usdQuote, amountSpec, eurMarket));
        assertThrows(IllegalArgumentException.class, () ->
                OfferAmountUtil.findBaseSideMaxAmount(usdQuote, amountSpec, eurMarket));
        assertThrows(IllegalArgumentException.class, () ->
                OfferAmountUtil.findQuoteSideFixedAmount(usdQuote, amountSpec, eurMarket));
        assertThrows(IllegalArgumentException.class, () ->
                OfferAmountUtil.findQuoteSideMinAmount(usdQuote, amountSpec, eurMarket));
        assertThrows(IllegalArgumentException.class, () ->
                OfferAmountUtil.findQuoteSideMaxAmount(usdQuote, amountSpec, eurMarket));
    }
}
