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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvTest {
    // Text of other users must not become a formula in a spreadsheet application
    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
            "=HYPERLINK(1)|\"'=HYPERLINK(1)\"",
            "+cmd|\"'+cmd\"",
            "-cmd|\"'-cmd\"",
            "@SUM(A1)|\"'@SUM(A1)\""})
    void cellWhichStartsLikeAFormulaBecomesText(String value, String expected) {
        assertEquals(expected, Csv.escapeSpecialCharacters(value));
    }

    @Test
    void cellWhichStartsWithTabOrCarriageReturnBecomesText() {
        assertEquals("\"'\tx\"", Csv.escapeSpecialCharacters("\tx"));
        assertEquals("\"'\rx\"", Csv.escapeSpecialCharacters("\rx"));
    }

    // Excel with the semicolon as list separator starts a new cell after each semicolon, also inside quotes (see
    // docs/specifications/desktop/csv-export.md). The cell after it must not start a formula either.
    @Test
    void formulaAfterASemicolonBecomesText() {
        assertEquals("\"x;'=1+1;\"", Csv.escapeSpecialCharacters("x;=1+1;"));
        assertEquals("\"lnbc1;'@SUM(A1)\"", Csv.escapeSpecialCharacters("lnbc1;@SUM(A1)"));
    }

    @Test
    void formulaAfterATabOrALineBreakBecomesText() {
        assertEquals("\"a\t'@SUM(A1)\"", Csv.escapeSpecialCharacters("a\t@SUM(A1)"));
        assertEquals("\"a\n'=1\"", Csv.escapeSpecialCharacters("a\n=1"));
        assertEquals("\"a\r\n'+cmd\"", Csv.escapeSpecialCharacters("a\r\n+cmd"));
    }

    // A parser which splits at semicolons keeps the cell whole
    @Test
    void cellWithASemicolonOrATabIsQuoted() {
        assertEquals("\"a;b\"", Csv.escapeSpecialCharacters("a;b"));
        assertEquals("\"a\tb\"", Csv.escapeSpecialCharacters("a\tb"));
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {"-1.00%", "+2.5%", "-0.0001", "12", "plain text"})
    void numbersAndPlainTextStayUnchanged(String value) {
        assertEquals(value, Csv.escapeSpecialCharacters(value));
    }

    @Test
    void cellWithLineBreakIsQuoted() {
        assertEquals("\"a\nb\"", Csv.escapeSpecialCharacters("a\nb"));
    }

    @Test
    void separatorsAndQuotesAreEscapedAsBefore() {
        assertEquals("\"a,b\"", Csv.escapeSpecialCharacters("a,b"));
        assertEquals("\"say \"\"hi\"\"\"", Csv.escapeSpecialCharacters("say \"hi\""));
        assertEquals("h1,h2" + System.lineSeparator() + "1,2" + System.lineSeparator(),
                Csv.toCsv(List.of("h1", "h2"), List.of(List.of("1", "2"))));
    }
}
