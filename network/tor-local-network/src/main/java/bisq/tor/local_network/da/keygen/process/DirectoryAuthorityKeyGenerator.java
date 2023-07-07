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

import bisq.tor.local_network.InputStreamWaiter;
import bisq.tor.local_network.da.keygen.RelayKeyGenProcess;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
public class DirectoryAuthorityKeyGenerator {
    private static final String PEM_PASSPHRASE_PROMPT = "Enter PEM pass phrase:";
    private static final String PEM_VERIFY_PASSPHRASE_PROMPT = "\nVerifying - Enter PEM pass phrase:";

    private final DirectoryIdentityKeyGenProcess identityKeyGenProcess;
    private final RelayKeyGenProcess relayKeyGenProcess;

    @Getter
    private Optional<String> identityKeyFingerprint = Optional.empty();
    @Getter
    private Optional<String> relayKeyFingerprint = Optional.empty();

    public DirectoryAuthorityKeyGenerator(DirectoryIdentityKeyGenProcess identityKeyGenProcess,
                                          RelayKeyGenProcess relayKeyGenProcess) {
        this.identityKeyGenProcess = identityKeyGenProcess;
        this.relayKeyGenProcess = relayKeyGenProcess;
    }

    public void generate(String passphrase) throws IOException, InterruptedException {
        String identityKeyFingerprint = generateIdentityKeys(passphrase);
        this.identityKeyFingerprint = Optional.of(identityKeyFingerprint);

        String relayKeyFingerprint = relayKeyGenProcess.generateKeys(identityKeyFingerprint);
        this.relayKeyFingerprint = Optional.of(relayKeyFingerprint);
    }

    private String generateIdentityKeys(String passphrase) throws IOException, InterruptedException {
        identityKeyGenProcess.start();

        InputStream inputStream = identityKeyGenProcess.getInputStream().orElseThrow();
        var inputStreamWaiter = new InputStreamWaiter(inputStream);

        inputStreamWaiter.waitForString(PEM_PASSPHRASE_PROMPT);
        enterPassphrase(passphrase);

        inputStreamWaiter.waitForString(PEM_VERIFY_PASSPHRASE_PROMPT);
        enterPassphrase(passphrase);

        return identityKeyGenProcess.getKeyFingerprint();
    }

    private void enterPassphrase(String passphrase) throws IOException {
        OutputStream outputStream = identityKeyGenProcess.getOutputStream().orElseThrow();
        String passphraseWithNewLine = passphrase + "\n";
        outputStream.write(passphraseWithNewLine.getBytes(StandardCharsets.UTF_8));
        outputStream.flush();
    }
}
