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

package bisq.desktop.main.content.wallet.send;

import bisq.common.monetary.Coin;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WalletSendAmountValidatorTest {
    @Test
    void parsesAPositiveAmountIntoSatoshis() {
        assertEquals(Coin.asBtcFromValue(50_000_000), WalletSendAmountValidator.parse("0.5").orElseThrow());
        assertEquals(Coin.asBtcFromValue(1), WalletSendAmountValidator.parse("0.00000001").orElseThrow());
        assertEquals(Coin.asBtcFromValue(2_100_000_000_000_000L), WalletSendAmountValidator.parse("21000000").orElseThrow());
        assertEquals(Coin.asBtcFromValue(100_000_000), WalletSendAmountValidator.parse(" 1 ").orElseThrow());
        assertEquals(Coin.asBtcFromValue(10_000_000), WalletSendAmountValidator.parse("0.100000000").orElseThrow());
        assertEquals(Coin.asBtcFromValue(50_000_000), WalletSendAmountValidator.parse("0,5").orElseThrow());
    }

    @Test
    void rejectsWhatTheDaemonCannotSend() {
        for (String text : List.of("0", "0.0", "-1", "0.000000001", "21000000.00000001", "1e3", "1E-2", "abc", "", "  ")) {
            assertTrue(WalletSendAmountValidator.parse(text).isEmpty(), text);
        }
        assertTrue(WalletSendAmountValidator.parse(null).isEmpty());
    }
}
