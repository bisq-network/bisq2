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

package bisq.common.encoding;

import org.junit.jupiter.api.Test;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BitcoinURISchemeTest {
    @Test
    public void testExtractBitcoinAddress() {
        assertEquals("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy", BitcoinURIScheme.extractBitcoinAddress("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy"));
        assertEquals("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy", BitcoinURIScheme.extractBitcoinAddress("bitcoin:3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy"));
        assertEquals("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy", BitcoinURIScheme.extractBitcoinAddress("bitcoin:3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy?amount=1234"));
        assertEquals("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy", BitcoinURIScheme.extractBitcoinAddress("bitcoin:3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy?amount=1234&message=mymsg"));
        assertEquals("3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy", BitcoinURIScheme.extractBitcoinAddress("bitcoin:3J98t1WpEZ73CNmQviecrnyiWrnqRhWNEy?message=mymsg"));
        assertEquals("BC1Q6MX5CGJ3V2JSKGKPP3K4HMESGKK265WGGSLWXKWEM0ESDV2YF72QHAFCFB", BitcoinURIScheme.extractBitcoinAddress("BITCOIN:BC1Q6MX5CGJ3V2JSKGKPP3K4HMESGKK265WGGSLWXKWEM0ESDV2YF72QHAFCFB"));
    }

    @Test
    void shouldBuildUriWithOnlyAddress() {
        String uri = BitcoinURIScheme.buildBitcoinUri(
                "bc1qxyz123",
                Optional.empty(),
                Optional.empty());

        assertEquals("bitcoin:bc1qxyz123", uri);
    }

    @Test
    void shouldBuildUriWithAmountOnly() {
        String uri = BitcoinURIScheme.buildBitcoinUri(
                "bc1qxyz123",
                Optional.of("0.001"),
                Optional.empty());

        assertEquals("bitcoin:bc1qxyz123?amount=0.001", uri);
    }

    @Test
    void shouldBuildUriWithLabelOnly() {
        String uri = BitcoinURIScheme.buildBitcoinUri(
                "bc1qxyz123",
                Optional.empty(),
                Optional.of("Savings"));

        assertEquals("bitcoin:bc1qxyz123?label=Savings", uri);
    }

    @Test
    void shouldBuildUriWithAmountAndLabel() {
        String uri = BitcoinURIScheme.buildBitcoinUri(
                "bc1qxyz123",
                Optional.of("0.001"),
                Optional.of("Savings"));

        assertEquals("bitcoin:bc1qxyz123?amount=0.001&label=Savings", uri);
    }

    @Test
    void shouldPercentEncodeSpacesInLabel() {
        String uri = BitcoinURIScheme.buildBitcoinUri(
                "bc1qxyz123",
                Optional.empty(),
                Optional.of("Savings Account #1"));

        assertEquals("bitcoin:bc1qxyz123?label=Savings%20Account%20%231", uri);
    }

    @Test
    void shouldPercentEncodeSpecialCharactersInLabel() {
        String uri = BitcoinURIScheme.buildBitcoinUri(
                "bc1qxyz123",
                Optional.empty(),
                Optional.of("Coffee & Bagel?"));

        assertEquals("bitcoin:bc1qxyz123?label=Coffee%20%26%20Bagel%3F", uri);
    }

    @Test
    void shouldPercentEncodeUnicodeCharactersInLabel() {
        String uri = BitcoinURIScheme.buildBitcoinUri(
                "bc1qxyz123",
                Optional.empty(),
                Optional.of("Café"));

        assertEquals("bitcoin:bc1qxyz123?label=Caf%C3%A9", uri);
    }

    @Test
    void shouldPercentEncodePlusCharacterInLabel() {
        String uri = BitcoinURIScheme.buildBitcoinUri(
                "bc1qxyz123",
                Optional.empty(),
                Optional.of("A+B"));

        assertEquals("bitcoin:bc1qxyz123?label=A%2BB", uri);
    }

    @Test
    void shouldPlaceAmpersandBetweenAmountAndLabel() {
        String uri = BitcoinURIScheme.buildBitcoinUri(
                "bc1qxyz123",
                Optional.of("0.0005"),
                Optional.of("Test"));

        assertEquals("bitcoin:bc1qxyz123?amount=0.0005&label=Test", uri);
    }

    @Test
    void shouldSupportEmptyLabel() {
        String uri = BitcoinURIScheme.buildBitcoinUri(
                "bc1qxyz123",
                Optional.empty(),
                Optional.of(""));

        assertEquals("bitcoin:bc1qxyz123", uri);
    }

    @Test
    void shouldSupportZeroAmount() {
        String uri = BitcoinURIScheme.buildBitcoinUri(
                "bc1qxyz123",
                Optional.of("0"),
                Optional.empty());

        assertEquals("bitcoin:bc1qxyz123?amount=0", uri);
    }
}
