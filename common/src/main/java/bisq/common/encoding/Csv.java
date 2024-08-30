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

@Slf4j
public class Csv {
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
        if (value.contains(",") || value.contains("\"") || value.contains("'")) {
            value = value.replace("\"", "\"\"");
            value = "\"" + value + "\"";
        }
        return value;
    }
}