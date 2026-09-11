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

import bisq.common.data.Pair;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class TorrcFileParser {

    /**
     * Parses a torrc-style override file into a map of key → list of values.
     * Repeated keys (e.g. multiple {@code Bridge} lines) accumulate into a list so that
     * all entries appear in the generated torrc.
     */
    public static Map<String, List<String>> parseTorrcOverrideFile(Path filePath) throws IOException {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String line : Files.readAllLines(filePath)) {
            parseDirective(line).ifPresent(directive ->
                    result.computeIfAbsent(directive.getFirst(), key -> new ArrayList<>()).add(directive.getSecond()));
        }
        return result;
    }

    /**
     * Parses a single torrc line into its key and value. Blank lines, comment lines and keys
     * without a value yield an empty result. Key and value may be separated by any whitespace,
     * and a trailing {@code #} comment is removed unless it appears inside a quoted value.
     */
    static Optional<Pair<String, String>> parseDirective(String line) {
        String directive = stripComment(line).strip();
        if (directive.isEmpty()) {
            return Optional.empty();
        }

        int separatorIndex = indexOfFirstWhitespace(directive);
        String value = separatorIndex < 0 ? "" : directive.substring(separatorIndex + 1).strip();
        if (value.isEmpty()) {
            log.warn("Ignoring torrc line without a value: '{}'", directive);
            return Optional.empty();
        }
        return Optional.of(new Pair<>(directive.substring(0, separatorIndex), value));
    }

    private static String stripComment(String line) {
        boolean isInQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char current = line.charAt(i);
            if (isInQuotes && current == '\\') {
                i++; // Skip the escaped character so an escaped quote does not end the value
            } else if (current == '"') {
                isInQuotes = !isInQuotes;
            } else if (current == '#' && !isInQuotes) {
                return line.substring(0, i);
            }
        }
        return line;
    }

    private static int indexOfFirstWhitespace(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isWhitespace(value.charAt(i))) {
                return i;
            }
        }
        return -1;
    }
}
