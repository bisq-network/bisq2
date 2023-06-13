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

package bisq.tor.local_network;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

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

    public String waitUntilGenerated() throws InterruptedException, IOException {
        Process process = this.process.orElseThrow();
        process.waitFor(1, TimeUnit.MINUTES);
        return readKeyFingerprint();
    }

    private String readKeyFingerprint() throws IOException {
        File certificateFile = new File(torKeyDirPath.toFile(), "authority_certificate");
        try (var reader = new BufferedReader(new FileReader(certificateFile))) {
            String line = reader.readLine();
            while (line != null) {

                if (isFingerprintLine(line)) {
                    return extractFingerprint(line);
                }

                line = reader.readLine();
            }
        }

        throw new IllegalStateException("Authority certificate was never created.");
    }

    private boolean isFingerprintLine(String line) {
        // fingerprint A547708A712364A3782FB49E8207AF3FA2BC9713
        return line.startsWith("fingerprint ");
    }

    private String extractFingerprint(String line) {
        // fingerprint A547708A712364A3782FB49E8207AF3FA2BC9713
        return line.split(" ")[1].strip();
    }
}
