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

package bisq.tor.local_network.da.keygen.process;

import bisq.tor.local_network.KeyFingerprintReader;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

@Slf4j
public class DirectoryIdentityKeyGenProcess {
    private final Path torKeyDirPath;
    private final String directoryAddress;

    private Optional<Process> process = Optional.empty();
    @Getter
    private Optional<InputStream> inputStream = Optional.empty();
    @Getter
    private Optional<OutputStream> outputStream = Optional.empty();

    public DirectoryIdentityKeyGenProcess(Path torKeyDirPath, String directoryAddress) {
        this.torKeyDirPath = torKeyDirPath;
        this.directoryAddress = directoryAddress;
    }

    public void start() throws IOException {
        var processBuilder = new ProcessBuilder(
                "tor-gencert", "--create-identity-key", "-a", directoryAddress);
        processBuilder.redirectErrorStream(true);
        processBuilder.directory(torKeyDirPath.toFile());

        Process process = processBuilder.start();
        this.process = Optional.of(process);

        inputStream = Optional.of(process.getInputStream());
        outputStream = Optional.of(process.getOutputStream());
    }

    public String getKeyFingerprint() throws InterruptedException {
        Process process = this.process.orElseThrow();
        process.waitFor(1, TimeUnit.MINUTES);
        return readKeyFingerprint();
    }

    private String readKeyFingerprint() {
        File certificateFile = new File(torKeyDirPath.toFile(), "authority_certificate");

        Predicate<String> lineMatcher = s -> s.startsWith("fingerprint ");
        UnaryOperator<String> dataExtractor = s -> s.split(" ")[1].strip();

        var keyFingerprintReader = new KeyFingerprintReader(certificateFile, lineMatcher, dataExtractor);
        return keyFingerprintReader.read().orElseThrow();
    }
}
