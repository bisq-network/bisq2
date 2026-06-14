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

package bisq.network.tor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TorrcFileParser {

    /**
     * Parses a torrc-style override file into a map of key → list of values.
     * Each non-blank, non-comment line is expected to have the form {@code Key Value}.
     * Repeated keys (e.g. multiple {@code Bridge} lines) accumulate into a list so that
     * all entries appear in the generated torrc.
     */
    public static Map<String, List<String>> parseTorrcOverrideFile(Path filePath) throws IOException {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String line : Files.readAllLines(filePath)) {
            String trimmed = line.strip();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int spaceIndex = -1;
            for (int i = 0; i < trimmed.length(); i++) {
                if (Character.isWhitespace(trimmed.charAt(i))) {
                    spaceIndex = i;
                    break;
                }
            }
            if (spaceIndex < 0) {
                continue; // bare key with no value — skip
            }
            String key = trimmed.substring(0, spaceIndex);
            String value = trimmed.substring(spaceIndex + 1).strip();
            result.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }
        return result;
    }
}
