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

package bisq.i18n;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class ResTest {
    @BeforeAll
    public static void setup() {
        Res.setLanguage("en");
    }

    @Test
    public void testEncodeDecode() {
        String key, argument1, argument2, expected, encoded, decoded;

        // 2 args
        argument1 = "121.12";
        argument2 = "5%";
        key = "bisqEasy.offerDetails.priceValue";
        expected = Res.get(key, argument1, argument2);
        encoded = Res.encode(key, argument1, argument2);
        decoded = Res.decode(encoded);
        assertEquals(expected, decoded);

        // 1 arg
        argument1 = "Alice";
        key = "bisqEasy.openTrades.tradeLogMessage.cancelled";
        expected = Res.get(key, argument1);
        encoded = Res.encode(key, argument1);
        decoded = Res.decode(encoded);
        assertEquals(expected, decoded);

        // no arg
        key = "bisqEasy.openTrades.cancelTrade";
        expected = Res.get(key);
        encoded = Res.encode(key);
        decoded = Res.decode(encoded);
        assertEquals(expected, decoded);
    }

}
