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

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public class Csv {
    private static final Pattern SIGNED_NUMBER = Pattern.compile("[-+][0-9][0-9.,]*%?");
    // A formula character after a character at which a spreadsheet application can start a new cell or row
    private static final Pattern FORMULA_AFTER_SEPARATOR = Pattern.compile("(?<=[;\\t\\r\\n])(?=[=+\\-@\\t\\r])");

    public static String toCsv(List<String> headers, List<List<String>> data) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < headers.size(); i++) {
            String header = headers.get(i);
            sb.append(escapeSpecialCharacters(header));
            if (i != headers.size() - 1) {
                sb.append(",");
            }
        }
        sb.append(System.lineSeparator());

        for (List<String> row : data) {
            for (int i = 0; i < row.size(); i++) {
                String cellData = row.get(i);
                sb.append(escapeSpecialCharacters(cellData));
                if (i != row.size() - 1) {
                    sb.append(",");
                }
            }
            sb.append(System.lineSeparator());
        }
        return sb.toString();
    }

    public static String escapeSpecialCharacters(String value) {
        value = neutralizeFormula(value);
        if (value.contains(",") || value.contains("\"") || value.contains("'") || value.contains(";") ||
                value.contains("\t") || value.contains("\n") || value.contains("\r")) {
            value = value.replace("\"", "\"\"");
            value = "\"" + value + "\"";
        }
        return value;
    }

    // Spreadsheet applications run a cell which starts with =, +, -, @, tab or carriage return as a formula. Cells can
    // contain text of trade peers, for example a payment account name or a chat text, so such a cell gets a leading
    // apostrophe, which makes it text. Numbers with a sign, for example "-1.00%", stay readable as numbers.
    // An application which splits the lines at semicolons or tabs, for example Excel with the semicolon as list
    // separator, starts a new cell after such a character, and a quote does not stop it there. So a formula character
    // after a semicolon, a tab or a line break gets an apostrophe as well (see docs/specifications/desktop/csv-export.md).
    private static String neutralizeFormula(String value) {
        if (value.isEmpty()) {
            return value;
        }
        char first = value.charAt(0);
        boolean isFormulaStart = first == '=' || first == '@' || first == '\t' || first == '\r' ||
                ((first == '-' || first == '+') && !SIGNED_NUMBER.matcher(value).matches());
        String neutralized = isFormulaStart ? "'" + value : value;
        return FORMULA_AFTER_SEPARATOR.matcher(neutralized).replaceAll("'");
    }
}