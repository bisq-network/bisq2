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

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

@Slf4j
public class DirectoryAuthorityKeyGenerator {
    private static final String PEM_PASSPHRASE_PROMPT = "Enter PEM pass phrase:";
    private static final String PEM_VERIFY_PASSPHRASE_PROMPT = "\nVerifying - Enter PEM pass phrase:";

    private final DirectoryIdentityKeyGenProcess keyGenProcess;

    public DirectoryAuthorityKeyGenerator(DirectoryIdentityKeyGenProcess keyGenProcess) {
        this.keyGenProcess = keyGenProcess;
    }

    public void generate(String passphrase) throws IOException, InterruptedException {
        keyGenProcess.start();

        InputStream inputStream = keyGenProcess.getInputStream().orElseThrow();
        var inputStreamWaiter = new InputStreamWaiter(inputStream);

        inputStreamWaiter.waitForString(PEM_PASSPHRASE_PROMPT);
        enterPassphrase(passphrase);

        inputStreamWaiter.waitForString(PEM_VERIFY_PASSPHRASE_PROMPT);
        enterPassphrase(passphrase);

        keyGenProcess.waitUntilGenerated();
    }

    private void enterPassphrase(String passphrase) throws IOException {
        OutputStream outputStream = keyGenProcess.getOutputStream().orElseThrow();
        String passphraseWithNewLine = passphrase + "\n";
        outputStream.write(passphraseWithNewLine.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }
}
