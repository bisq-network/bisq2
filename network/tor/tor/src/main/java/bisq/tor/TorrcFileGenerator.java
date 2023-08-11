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

package bisq.tor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

public class TorrcFileGenerator {
    private final Path torrcPath;
    private final Map<String, String> torrcConfigMap;

    public TorrcFileGenerator(Path torrcPath, Map<String, String> torrcConfigMap) {
        this.torrcPath = torrcPath;
        this.torrcConfigMap = torrcConfigMap;
    }

    public void generate() {
        StringBuilder torrcStringBuilder = new StringBuilder();
        torrcConfigMap.forEach((key, value) ->
                torrcStringBuilder.append(key)
                        .append(" ")
                        .append(value)
                        .append("\n")
        );

        try {
            Files.writeString(torrcPath, torrcStringBuilder.toString());
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't create torrc file: " + torrcPath.toAbsolutePath());
        }
    }
}
