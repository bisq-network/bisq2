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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OfferAmountUtilTest {

    @Test
    @DisplayName("calculate security deposit as btc uses percentage and code")
    void calculate_security_deposit_as_btc_uses_percentage_and_code() {
        Monetary input = Coin.asBtcFromValue(100_000_000L);
        Monetary deposit = OfferAmountUtil.calculateSecurityDepositAsBTC(input, 0.25);
        assertEquals("BTC", deposit.getCode());
        assertEquals(25_000_000L, deposit.getValue());
    }

    @Test
    @DisplayName("calculate security deposit as btc rounds half up")
    void calculate_security_deposit_as_btc_rounds_half_up() {
        Monetary input = Coin.asBtcFromValue(3L);
        Monetary deposit = OfferAmountUtil.calculateSecurityDepositAsBTC(input, 0.5);
        assertEquals(2L, deposit.getValue());
    }

    @Test
    @DisplayName("calculate security deposit as btc uses base side for btc fiat market")
    void calculate_security_deposit_as_btc_uses_base_side_for_btc_fiat_market() {
        Market market = new Market("BTC", "USD", "Bitcoin", "US Dollar");
        Monetary baseSideMonetary = Coin.asBtcFromValue(100_000_000L);
        Monetary quoteSideMonetary = Fiat.fromValue(5_000_000L, "USD");

        Monetary deposit = OfferAmountUtil.calculateSecurityDepositAsBTC(market, baseSideMonetary, quoteSideMonetary, 0.10);

        assertEquals("BTC", deposit.getCode());
        assertEquals(10_000_000L, deposit.getValue());
    }

    @Test
    @DisplayName("calculate security deposit as btc uses quote side for crypto btc market")
    void calculate_security_deposit_as_btc_uses_quote_side_for_crypto_btc_market() {
        Market market = new Market("XMR", "BTC", "Monero", "Bitcoin");
        Monetary baseSideMonetary = Coin.fromValue(10_000_000_000L, "XMR");
        Monetary quoteSideMonetary = Coin.asBtcFromValue(50_000_000L);

        Monetary deposit = OfferAmountUtil.calculateSecurityDepositAsBTC(market, baseSideMonetary, quoteSideMonetary, 0.10);

        assertEquals("BTC", deposit.getCode());
        assertEquals(5_000_000L, deposit.getValue());
    }

    @Test
    @DisplayName("calculate security deposit as btc throws when code is not btc")
    void calculate_security_deposit_as_btc_throws_when_code_is_not_btc() {
        Monetary nonBtcMonetary = Fiat.fromValue(10_000L, "USD");

        assertThrows(IllegalArgumentException.class, () ->
                OfferAmountUtil.calculateSecurityDepositAsBTC(nonBtcMonetary, 0.10));
    }
}
