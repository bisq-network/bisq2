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

package bisq.network.tor.local_network.da.keygen.process;

import bisq.network.tor.local_network.InputStreamWaiter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DirectoryIdentityKeyGenProcess {
    private static final String PEM_PASSPHRASE_PROMPT = "Enter PEM pass phrase:";
    private static final String PEM_VERIFY_PASSPHRASE_PROMPT = "\nVerifying - Enter PEM pass phrase:";

    private final Path torKeyDirPath;
    private final String directoryAddress;

    public DirectoryIdentityKeyGenProcess(Path torKeyDirPath, String directoryAddress) {
        this.torKeyDirPath = torKeyDirPath;
        this.directoryAddress = directoryAddress;
    }

    public void generateKeys(String passphrase) throws IOException, InterruptedException {
        Process process = createAndStartKeygenProcess();
        InputStream inputStream = process.getInputStream();
        OutputStream outputStream = process.getOutputStream();

        var inputStreamWaiter = new InputStreamWaiter(inputStream);

        inputStreamWaiter.waitForString(PEM_PASSPHRASE_PROMPT);
        enterPassphrase(outputStream, passphrase);

        inputStreamWaiter.waitForString(PEM_VERIFY_PASSPHRASE_PROMPT);
        enterPassphrase(outputStream, passphrase);

        process.waitFor(1, TimeUnit.MINUTES);
    }

    private Process createAndStartKeygenProcess() throws IOException {
        var processBuilder = new ProcessBuilder(
                "tor-gencert", "--create-identity-key", "-a", directoryAddress);
        processBuilder.redirectErrorStream(true);
        processBuilder.directory(torKeyDirPath.toFile());
        return processBuilder.start();
    }

    private void enterPassphrase(OutputStream outputStream, String passphrase) throws IOException {
        String passphraseWithNewLine = passphrase + "\n";
        outputStream.write(passphraseWithNewLine.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }
}
