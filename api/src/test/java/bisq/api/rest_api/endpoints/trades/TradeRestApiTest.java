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

package bisq.api.rest_api.endpoints.trades;

import bisq.trade.TradeRestrictedException;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TradeRestApiTest {

    @Test
    void haltTradingErrorEntityKeepsLegacyErrorTextAndAddsErrorCode() {
        Map<String, String> entity = TradeRestApi.toTradeRestrictedErrorEntity(TradeRestrictedException.haltTrading());

        // Released mobile clients match on the "error" text; the prefix and fragments must not change
        assertThat(entity.get("error"))
                .startsWith("Invalid input: ")
                .contains("Trading is on halt");
        assertThat(entity.get("errorCode")).isEqualTo("HALT_TRADING");
        assertThat(entity).doesNotContainKey("minRequiredVersion");
    }

    @Test
    void minVersionErrorEntityCarriesVersionInTextAndField() {
        Map<String, String> entity =
                TradeRestApi.toTradeRestrictedErrorEntity(TradeRestrictedException.minVersionRequired("2.1.12"));

        assertThat(entity.get("error"))
                .startsWith("Invalid input: ")
                .contains("version 2.1.12 installed")
                .contains("min. version required for trading");
        assertThat(entity.get("errorCode")).isEqualTo("MIN_VERSION_REQUIRED");
        assertThat(entity.get("minRequiredVersion")).isEqualTo("2.1.12");
    }
}
