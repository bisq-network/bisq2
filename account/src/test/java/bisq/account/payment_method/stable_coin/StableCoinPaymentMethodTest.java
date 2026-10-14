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

package bisq.account.payment_method.stable_coin;

import bisq.common.asset.StableCoin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class StableCoinPaymentMethodTest {

    @Test
    @DisplayName("from payment rail usdc erc20 returns correct code")
    void from_payment_rail_usdc_erc20_returns_correct_code() {
        StableCoinPaymentMethod method = StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_ERC20);
        assertEquals("USDC", method.getCode());
    }

    @Test
    @DisplayName("from payment rail usdc erc20 returns correct network")
    void from_payment_rail_usdc_erc20_returns_correct_network() {
        StableCoinPaymentMethod method = StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_ERC20);
        assertEquals(StableCoin.Network.ETHEREUM, method.getNetwork());
    }

    @Test
    @DisplayName("from payment rail usdc erc20 returns correct token standard")
    void from_payment_rail_usdc_erc20_returns_correct_token_standard() {
        StableCoinPaymentMethod method = StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_ERC20);
        assertEquals(StableCoin.TokenStandard.ERC20, method.getTokenStandard());
    }

    @Test
    @DisplayName("from payment rail usdt trc20 returns correct code")
    void from_payment_rail_usdt_trc20_returns_correct_code() {
        StableCoinPaymentMethod method = StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDT_TRC20);
        assertEquals("USDT", method.getCode());
    }

    @Test
    @DisplayName("from payment rail usdt trc20 returns correct network")
    void from_payment_rail_usdt_trc20_returns_correct_network() {
        StableCoinPaymentMethod method = StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDT_TRC20);
        assertEquals(StableCoin.Network.TRON, method.getNetwork());
    }

    @Test
    @DisplayName("from custom name returns null")
    void from_custom_name_returns_null() {
        assertNull(StableCoinPaymentMethod.fromCustomName("anything"));
        assertNull(StableCoinPaymentMethod.fromCustomName("USDT"));
    }

    @Test
    @DisplayName("get supported currencies returns singleton with correct coin")
    void get_supported_currencies_returns_singleton_with_correct_coin() {
        StableCoinPaymentMethod method = StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_ERC20);
        assertEquals(1, method.getSupportedCurrencies().size());
        assertEquals("USDC", method.getSupportedCurrencies().get(0).getCode());
    }

    @Test
    @DisplayName("get peg currency code is usd for all")
    void get_peg_currency_code_is_usd_for_all() {
        for (StableCoinPaymentRail rail : StableCoinPaymentRail.values()) {
            StableCoinPaymentMethod method = StableCoinPaymentMethod.fromPaymentRail(rail);
            assertEquals("USD", method.getPegCurrencyCode(),
                    "Rail " + rail.name() + " should peg to USD");
        }
    }

    @Test
    @DisplayName("get name returns human readable name")
    void get_name_returns_human_readable_name() {
        StableCoinPaymentMethod method = StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDT_ERC20);
        assertEquals("Tether USD", method.getName());
    }

    @Test
    @DisplayName("get id returns ticker code")
    void get_id_returns_ticker_code() {
        StableCoinPaymentMethod method = StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.USDC_SPL);
        assertEquals("USDC", method.getId());
    }

    @Test
    @DisplayName("payment rail accessible")
    void payment_rail_accessible() {
        StableCoinPaymentMethod method = StableCoinPaymentMethod.fromPaymentRail(StableCoinPaymentRail.DAI_ERC20);
        assertEquals(StableCoinPaymentRail.DAI_ERC20, method.getPaymentRail());
    }
}
