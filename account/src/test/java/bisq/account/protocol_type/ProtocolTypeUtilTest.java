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

package bisq.account.protocol_type;

import bisq.common.market.Market;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolTypeUtilTest {

    private static final Market BTC_USD = new Market("BTC", "USD", "Bitcoin", "US Dollar");
    private static final Market BTC_USDC = new Market("BTC", "USDC", "Bitcoin", "USD Coin");
    private static final Market XMR_BTC = new Market("XMR", "BTC", "Monero", "Bitcoin");

    @Test
    @DisplayName("btc fiat market supports bisq easy")
    void btc_fiat_market_supports_bisq_easy() {
        List<TradeProtocolType> types = ProtocolTypeUtil.getProtocols(BTC_USD);
        assertTrue(types.contains(TradeProtocolType.BISQ_EASY));
    }

    @Test
    @DisplayName("btc stablecoin market supports bisq easy")
    void btc_stablecoin_market_supports_bisq_easy() {
        List<TradeProtocolType> types = ProtocolTypeUtil.getProtocols(BTC_USDC);
        assertTrue(types.contains(TradeProtocolType.BISQ_EASY));
    }

    @Test
    @DisplayName("crypto market does not support bisq easy")
    void crypto_market_does_not_support_bisq_easy() {
        List<TradeProtocolType> types = ProtocolTypeUtil.getProtocols(XMR_BTC);
        assertFalse(types.contains(TradeProtocolType.BISQ_EASY));
    }

    @Test
    @DisplayName("btc usdt market supports bisq easy if usdt is stablecoin")
    void btc_usdt_market_supports_bisq_easy() {
        Market btcUsdt = new Market("BTC", "USDT", "Bitcoin", "Tether USD");
        List<TradeProtocolType> types = ProtocolTypeUtil.getProtocols(btcUsdt);
        assertTrue(types.contains(TradeProtocolType.BISQ_EASY),
                "BTC/USDT should support Bisq Easy if USDT is registered as stablecoin");
    }

    @Test
    @DisplayName("non-btc base fiat market does not support bisq easy")
    void non_btc_base_fiat_market_does_not_support_bisq_easy() {
        Market ethUsd = new Market("ETH", "USD", "Ethereum", "US Dollar");
        List<TradeProtocolType> types = ProtocolTypeUtil.getProtocols(ethUsd);
        assertFalse(types.contains(TradeProtocolType.BISQ_EASY),
                "Non-BTC base market should not support Bisq Easy");
    }

    @Test
    @DisplayName("protocols list always contains mu sig for any market")
    void protocols_always_contains_mu_sig() {
        assertTrue(ProtocolTypeUtil.getProtocols(BTC_USD).contains(TradeProtocolType.MU_SIG));
        assertTrue(ProtocolTypeUtil.getProtocols(BTC_USDC).contains(TradeProtocolType.MU_SIG));
        assertTrue(ProtocolTypeUtil.getProtocols(XMR_BTC).contains(TradeProtocolType.MU_SIG));
    }
}
