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

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

public class RelayKeyGenProcess {
    private final DirectoryAuthority directoryAuthority;

    public RelayKeyGenProcess(DirectoryAuthority directoryAuthority) {
        this.directoryAuthority = directoryAuthority;
    }

    public String generateKeys(String identityKeyFingerprint) throws IOException, InterruptedException {
        var processBuilder = new ProcessBuilder(
                "tor", "--list-fingerprint",
                "--DataDirectory", directoryAuthority.getDataDir().toAbsolutePath().toString(),
                "--ORPort", String.valueOf(directoryAuthority.getOrPort()),

                "--DirAuthority",
                directoryAuthority.getNickname() +
                        " orport=" + directoryAuthority.getOrPort() +
                        " v3ident=" + identityKeyFingerprint +
                        " 127.0.0.1:" + directoryAuthority.getDirPort() +
                        " ffffffffffffffffffffffffffffffffffffffff"
        );

        processBuilder.redirectError(ProcessBuilder.Redirect.DISCARD)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD);

        Process process = processBuilder.start();
        process.waitFor(45, TimeUnit.SECONDS);

        return readKeyFingerprint();
    }

    private String readKeyFingerprint() throws IOException {
        File dataDirFile = directoryAuthority.getDataDir().toFile();
        File fingerprintFile = new File(dataDirFile, "fingerprint");

        Predicate<String> lineMatcher = s -> s.startsWith("Unnamed ");
        UnaryOperator<String> dataExtractor = s -> s.split(" ")[1].strip();

        var keyFingerprintReader = new KeyFingerprintReader(fingerprintFile, lineMatcher, dataExtractor);
        return keyFingerprintReader.read();
    }
}
