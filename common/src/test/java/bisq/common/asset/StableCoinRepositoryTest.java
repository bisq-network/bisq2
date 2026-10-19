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

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StableCoinRepositoryTest {

    @Test
    @DisplayName("find usdt returns present")
    void find_usdt_returns_present() {
        assertTrue(StableCoinRepository.find("USDT").isPresent());
    }

    @Test
    @DisplayName("find usdc returns present")
    void find_usdc_returns_present() {
        assertTrue(StableCoinRepository.find("USDC").isPresent());
    }

    @Test
    @DisplayName("find unknown returns empty")
    void find_unknown_returns_empty() {
        assertTrue(StableCoinRepository.find("FAKE").isEmpty());
    }

    @Test
    @DisplayName("all with code usdt contains the usdt variants")
    void all_with_code_usdt_contains_the_usdt_variants() {
        Set<StableCoin> variants = StableCoinRepository.allWithCode("USDT");
        assertTrue(variants.stream().allMatch(coin -> coin.getCode().equals("USDT")));
        assertTrue(variants.contains(StableCoinRepository.USDT_ERC20));
        assertTrue(variants.contains(StableCoinRepository.USDT_TRC20));
        assertTrue(variants.contains(StableCoinRepository.USDT_BEP20));
    }

    @Test
    @DisplayName("all with code usdc contains the usdc variants")
    void all_with_code_usdc_contains_the_usdc_variants() {
        Set<StableCoin> variants = StableCoinRepository.allWithCode("USDC");
        assertTrue(variants.stream().allMatch(coin -> coin.getCode().equals("USDC")));
        assertTrue(variants.contains(StableCoinRepository.USDC_ERC20));
        assertTrue(variants.contains(StableCoinRepository.USDC_SPL));
    }

    @Test
    @DisplayName("all with code unknown returns empty")
    void all_with_code_unknown_returns_empty() {
        assertTrue(StableCoinRepository.allWithCode("FAKE").isEmpty());
    }

    @Test
    @DisplayName("all with peg currency code usd returns all")
    void all_with_peg_currency_code_usd_returns_all() {
        Set<StableCoin> usdPegged = StableCoinRepository.allWithPegCurrencyCode("USD");
        assertFalse(usdPegged.isEmpty());
        usdPegged.forEach(coin ->
                assertEquals("USD", coin.getPegCurrencyCode()));
    }

    @Test
    @DisplayName("usdt erc20 has correct properties")
    void usdt_erc20_has_correct_properties() {
        StableCoin coin = StableCoinRepository.USDT_ERC20;
        assertEquals("USDT", coin.getCode());
        assertEquals("Tether USD", coin.getName());
        assertEquals("USD", coin.getPegCurrencyCode());
        assertEquals(StableCoin.Network.ETHEREUM, coin.getNetwork());
        assertEquals(StableCoin.TokenStandard.ERC20, coin.getTokenStandard());
        assertEquals(StableCoin.Issuer.TETHER, coin.getIssuer());
    }

    @Test
    @DisplayName("usdc erc20 has correct properties")
    void usdc_erc20_has_correct_properties() {
        StableCoin coin = StableCoinRepository.USDC_ERC20;
        assertEquals("USDC", coin.getCode());
        assertEquals("USD Coin", coin.getName());
        assertEquals(StableCoin.Network.ETHEREUM, coin.getNetwork());
        assertEquals(StableCoin.Issuer.CIRCLE, coin.getIssuer());
    }

    @Test
    @DisplayName("usdc spl has correct network")
    void usdc_spl_has_correct_network() {
        assertEquals(StableCoin.Network.SOLANA, StableCoinRepository.USDC_SPL.getNetwork());
        assertEquals(StableCoin.TokenStandard.SPL, StableCoinRepository.USDC_SPL.getTokenStandard());
    }
}
