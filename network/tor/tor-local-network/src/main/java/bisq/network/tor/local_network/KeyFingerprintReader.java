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

package bisq.network.tor.local_network;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

@Slf4j
public class KeyFingerprintReader {
    private final File fingerprintFile;
    private final Predicate<String> lineMatcher;
    private final UnaryOperator<String> dataExtractor;

    public KeyFingerprintReader(File fingerprintFile,
                                Predicate<String> lineMatcher,
                                UnaryOperator<String> dataExtractor) {
        this.fingerprintFile = fingerprintFile;
        this.lineMatcher = lineMatcher;
        this.dataExtractor = dataExtractor;
    }


    public Optional<String> read() {
        try (var reader = new BufferedReader(new FileReader(fingerprintFile))) {
            String line = reader.readLine();
            while (line != null) {

                if (lineMatcher.test(line)) {
                    return Optional.of(
                            dataExtractor.apply(line)
                    );
                }

                line = reader.readLine();
            }
        } catch (IOException e) {
            log.error("Cannot read {}", fingerprintFile.getAbsolutePath(), e);
        }

        return Optional.empty();
    }
}
