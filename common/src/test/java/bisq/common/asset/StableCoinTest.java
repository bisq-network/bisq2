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

package bisq.common.asset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.*;

class StableCoinTest {

    @Test
    @DisplayName("is stable coin true for usdt")
    void is_stable_coin_true_for_usdt() {
        assertTrue(StableCoin.isStableCoin("USDT"));
    }

    @Test
    @DisplayName("is stable coin true for usdc")
    void is_stable_coin_true_for_usdc() {
        assertTrue(StableCoin.isStableCoin("USDC"));
    }

    @Test
    @DisplayName("is stable coin true for dai")
    void is_stable_coin_true_for_dai() {
        assertTrue(StableCoin.isStableCoin("DAI"));
    }

    @Test
    @DisplayName("is stable coin false for usd")
    void is_stable_coin_false_for_usd() {
        assertFalse(StableCoin.isStableCoin("USD"));
    }

    @Test
    @DisplayName("is stable coin false for btc")
    void is_stable_coin_false_for_btc() {
        assertFalse(StableCoin.isStableCoin("BTC"));
    }

    @Test
    @DisplayName("is stable coin false for unknown")
    void is_stable_coin_false_for_unknown() {
        assertFalse(StableCoin.isStableCoin("FAKE"));
    }

    @Test
    @DisplayName("display name contains name code and network")
    void display_name_contains_name_code_and_network() {
        StableCoin usdt = StableCoinRepository.USDT_ERC20;
        String displayName = usdt.getDisplayName();
        assertEquals("Tether USD (USDT, Ethereum ERC-20)", displayName);
    }

    @Test
    @DisplayName("short display name format")
    void short_display_name_format() {
        StableCoin usdt = StableCoinRepository.USDT_ERC20;
        assertEquals("USDT (ERC-20)", usdt.getShortDisplayName());
    }

    @Test
    @DisplayName("short display name for trc20")
    void short_display_name_for_trc20() {
        StableCoin usdt = StableCoinRepository.USDT_TRC20;
        assertEquals("USDT (TRC-20)", usdt.getShortDisplayName());
    }

    @Test
    @DisplayName("short display name for spl")
    void short_display_name_for_spl() {
        StableCoin usdc = StableCoinRepository.USDC_SPL;
        assertEquals("USDC (SPL)", usdc.getShortDisplayName());
    }

    @Test
    @DisplayName("is custom false for known coin")
    void is_custom_false_for_known_coin() {
        assertFalse(StableCoinRepository.USDT_ERC20.isCustom());
        assertFalse(StableCoinRepository.USDC_ERC20.isCustom());
    }

    @Test
    @DisplayName("is custom true for unknown coin")
    void is_custom_true_for_unknown_coin() {
        StableCoin custom = new StableCoin("XYZZ", "Custom Stable", "USD",
                StableCoin.Network.ETHEREUM, StableCoin.TokenStandard.ERC20, StableCoin.Issuer.UNDEFINED);
        assertTrue(custom.isCustom());
    }

    @Test
    @DisplayName("peg currency code is usd for all known")
    void peg_currency_code_is_usd_for_all_known() {
        assertEquals("USD", StableCoinRepository.USDT_ERC20.getPegCurrencyCode());
        assertEquals("USD", StableCoinRepository.USDC_ERC20.getPegCurrencyCode());
        assertEquals("USD", StableCoinRepository.DAI_ERC20.getPegCurrencyCode());
    }

    @Test
    @DisplayName("network enum display names")
    void network_enum_display_names() {
        assertEquals("Ethereum", StableCoin.Network.ETHEREUM.getDisplayName());
        assertEquals("Tron", StableCoin.Network.TRON.getDisplayName());
        assertEquals("Solana", StableCoin.Network.SOLANA.getDisplayName());
        assertEquals("BNB Smart Chain", StableCoin.Network.BNB_SMART_CHAIN.getDisplayName());
    }

    @Test
    @DisplayName("issuer enum display names")
    void issuer_enum_display_names() {
        assertEquals("Tether Ltd.", StableCoin.Issuer.TETHER.getDisplayName());
        assertEquals("Circle Internet Financial, LLC", StableCoin.Issuer.CIRCLE.getDisplayName());
        assertEquals("MakerDAO", StableCoin.Issuer.MAKERDAO.getDisplayName());
    }

    @Test
    @DisplayName("token standard display names")
    void token_standard_display_names() {
        assertEquals("ERC-20", StableCoin.TokenStandard.ERC20.getDisplayName());
        assertEquals("TRC-20", StableCoin.TokenStandard.TRC20.getDisplayName());
        assertEquals("SPL", StableCoin.TokenStandard.SPL.getDisplayName());
        assertEquals("BEP-20", StableCoin.TokenStandard.BEP20.getDisplayName());
    }
}
