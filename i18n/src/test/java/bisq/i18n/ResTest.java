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

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class ResTest {
    private static final char ARGS_SEPARATOR = 0x1f;
    private static final char PARAM_SEPARATOR = 0x1e;

    @BeforeAll
    public static void setup() {
        Res.setAndApplyLanguageTag("en");
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

    @Test
    public void testDecodeSeparatorsOnly() {
        assertEquals(Res.get(""), Res.decode(String.valueOf(PARAM_SEPARATOR)));
        assertEquals(Res.get(""), Res.decode("" + PARAM_SEPARATOR + PARAM_SEPARATOR));
    }

    @Test
    public void testDecodeUnknownKeyWithArguments() {
        assertEquals(Res.get("a{b"), Res.decode(Res.encode("a{b", "x")));
        assertEquals(Res.get("it's"), Res.decode(Res.encode("it's", "x")));
    }

    @Test
    public void testDecodeNeverThrows() {
        String key = "bisqEasy.openTrades.tradeLogMessage.cancelled";
        for (String text : textsUpToLength(4, 'a', '{', '}', '\'', PARAM_SEPARATOR, ARGS_SEPARATOR)) {
            assertDoesNotThrow(() -> Res.decode(text), text);
            assertDoesNotThrow(() -> Res.decode(key + PARAM_SEPARATOR + text), text);
        }
    }

    private static List<String> textsUpToLength(int maxLength, char... alphabet) {
        List<String> texts = new ArrayList<>(List.of(""));
        for (int i = 0; i < texts.size(); i++) {
            String text = texts.get(i);
            if (text.length() < maxLength) {
                for (char c : alphabet) {
                    texts.add(text + c);
                }
            }
        }
        return texts;
    }
}
