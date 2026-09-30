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

package bisq.trade;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TradeRestrictedExceptionTest {

    // The message fragments are a compatibility contract: released mobile clients classify
    // trade-restriction failures by matching them (see TradeRestrictionError in bisq-mobile).

    @Test
    void haltTradingKeepsMessageContractAndCarriesRestriction() {
        TradeRestrictedException exception = TradeRestrictedException.haltTrading();

        assertThat(exception.getMessage()).contains("Trading is on halt");
        assertThat(exception.getRestriction()).isEqualTo(TradeRestrictedException.Restriction.HALT_TRADING);
        assertThat(exception.findMinRequiredVersion()).isEmpty();
    }

    @Test
    void minVersionRequiredKeepsMessageContractAndCarriesVersion() {
        TradeRestrictedException exception = TradeRestrictedException.minVersionRequired("2.1.12");

        assertThat(exception.getMessage())
                .contains("version 2.1.12 installed")
                .contains("min. version required for trading");
        assertThat(exception.getRestriction()).isEqualTo(TradeRestrictedException.Restriction.MIN_VERSION_REQUIRED);
        assertThat(exception.findMinRequiredVersion()).contains("2.1.12");
    }

    @Test
    void isCatchableAsIllegalArgumentExceptionForExistingCallSites() {
        assertThat(TradeRestrictedException.haltTrading()).isInstanceOf(IllegalArgumentException.class);
    }
}
