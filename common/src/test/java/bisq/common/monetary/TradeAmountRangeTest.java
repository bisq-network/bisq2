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

package bisq.common.monetary;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TradeAmountRangeTest {
    @Test
    @DisplayName("constructor accepts matching codes")
    void constructor_accepts_matching_codes() {
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1), Fiat.fromFaceValue(100, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(2), Fiat.fromFaceValue(200, "USD"));

        assertDoesNotThrow(() -> new TradeAmountRange(min, max));
    }

    @Test
    @DisplayName("constructor throws if base side codes do not match")
    void constructor_throws_if_base_side_codes_do_not_match() {
        TradeAmount min = new TradeAmount(Coin.fromFaceValue(1, "XMR"), Coin.asBtcFromFaceValue(0.01));
        TradeAmount max = new TradeAmount(Coin.fromFaceValue(2, "ETH"), Coin.asBtcFromFaceValue(0.02));

        assertThrows(IllegalArgumentException.class, () -> new TradeAmountRange(min, max));
    }

    @Test
    @DisplayName("constructor throws if quote side codes do not match")
    void constructor_throws_if_quote_side_codes_do_not_match() {
        TradeAmount min = new TradeAmount(Coin.asBtcFromFaceValue(1), Fiat.fromFaceValue(100, "USD"));
        TradeAmount max = new TradeAmount(Coin.asBtcFromFaceValue(2), Fiat.fromFaceValue(200, "EUR"));

        assertThrows(IllegalArgumentException.class, () -> new TradeAmountRange(min, max));
    }
}
