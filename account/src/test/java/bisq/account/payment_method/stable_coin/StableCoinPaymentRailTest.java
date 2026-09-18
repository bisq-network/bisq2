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

import bisq.account.payment_method.TradeDuration;
import bisq.common.asset.StableCoin;
import bisq.common.asset.StableCoinRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class StableCoinPaymentRailTest {

    @Test
    @DisplayName("all rails return hours24 trade duration")
    void all_rails_return_hours24_trade_duration() {
        for (StableCoinPaymentRail rail : StableCoinPaymentRail.values()) {
            assertEquals(TradeDuration.HOURS_24, rail.getTradeDuration(),
                    "Rail " + rail.name() + " should have HOURS_24 trade duration");
        }
    }

    @Test
    @DisplayName("enum count matches repository total coins")
    void enum_count_matches_repository_total_coins() {
        int repoCount = StableCoinRepository.getSTABLE_COIN_CURRENCY_SET_BY_CODE()
                .values().stream()
                .mapToInt(Set::size)
                .sum();
        assertEquals(repoCount, StableCoinPaymentRail.values().length);
    }

    @Test
    @DisplayName("usdt erc20 maps to correct coin")
    void usdt_erc20_maps_to_correct_coin() {
        StableCoin coin = StableCoinPaymentRail.USDT_ERC20.getStableCoin();
        assertEquals("USDT", coin.getCode());
        assertEquals(StableCoin.Network.ETHEREUM, coin.getNetwork());
        assertEquals(StableCoin.TokenStandard.ERC20, coin.getTokenStandard());
    }

    @Test
    @DisplayName("usdt trc20 maps to correct coin")
    void usdt_trc20_maps_to_correct_coin() {
        StableCoin coin = StableCoinPaymentRail.USDT_TRC20.getStableCoin();
        assertEquals("USDT", coin.getCode());
        assertEquals(StableCoin.Network.TRON, coin.getNetwork());
        assertEquals(StableCoin.TokenStandard.TRC20, coin.getTokenStandard());
    }

    @Test
    @DisplayName("usdt bep20 maps to correct coin")
    void usdt_bep20_maps_to_correct_coin() {
        StableCoin coin = StableCoinPaymentRail.USDT_BEP20.getStableCoin();
        assertEquals("USDT", coin.getCode());
        assertEquals(StableCoin.Network.BNB_SMART_CHAIN, coin.getNetwork());
        assertEquals(StableCoin.TokenStandard.BEP20, coin.getTokenStandard());
    }

    @Test
    @DisplayName("usdc erc20 maps to correct coin")
    void usdc_erc20_maps_to_correct_coin() {
        StableCoin coin = StableCoinPaymentRail.USDC_ERC20.getStableCoin();
        assertEquals("USDC", coin.getCode());
        assertEquals(StableCoin.Network.ETHEREUM, coin.getNetwork());
        assertEquals(StableCoin.TokenStandard.ERC20, coin.getTokenStandard());
    }

    @Test
    @DisplayName("usdc spl maps to correct coin")
    void usdc_spl_maps_to_correct_coin() {
        StableCoin coin = StableCoinPaymentRail.USDC_SPL.getStableCoin();
        assertEquals("USDC", coin.getCode());
        assertEquals(StableCoin.Network.SOLANA, coin.getNetwork());
        assertEquals(StableCoin.TokenStandard.SPL, coin.getTokenStandard());
    }

    @Test
    @DisplayName("dai erc20 maps to correct coin")
    void dai_erc20_maps_to_correct_coin() {
        StableCoin coin = StableCoinPaymentRail.DAI_ERC20.getStableCoin();
        assertEquals("DAI", coin.getCode());
        assertEquals(StableCoin.Network.ETHEREUM, coin.getNetwork());
    }

    @Test
    @DisplayName("each rail references non null stable coin")
    void each_rail_references_non_null_stable_coin() {
        for (StableCoinPaymentRail rail : StableCoinPaymentRail.values()) {
            assertNotNull(rail.getStableCoin(), "Rail " + rail.name() + " should have non-null StableCoin");
        }
    }
}
