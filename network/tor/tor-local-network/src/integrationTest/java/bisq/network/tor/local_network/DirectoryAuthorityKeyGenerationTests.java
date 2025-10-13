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

import bisq.network.tor.local_network.da.keygen.process.DirectoryAuthorityKeyGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class DirectoryAuthorityKeyGenerationTests {
    @Test
    public void generateKeys(@TempDir Path dataDir) throws IOException, InterruptedException {
        Path keysPath = dataDir.resolve("keys");
        Files.createDirectories(keysPath);

        var directoryAuthority = TorNode.builder()
                .type(TorNode.Type.DIRECTORY_AUTHORITY)
                .nickname("Nick")
                .dataDir(dataDir)
                .orPort(2)
                .dirPort(3)
                .build();

        DirectoryAuthorityKeyGenerator.generate(directoryAuthority, "my_passphrase");

        assertThat(dataDir.resolve("fingerprint")).exists();
        assertThat(dataDir.resolve("fingerprint-ed25519")).exists();
        assertThat(dataDir.resolve("lock")).exists();

        assertThat(keysPath.resolve("authority_certificate")).exists();
        assertThat(keysPath.resolve("authority_identity_key")).exists();
        assertThat(keysPath.resolve("authority_signing_key")).exists();

        assertThat(keysPath.resolve("ed25519_master_id_public_key")).exists();
        assertThat(keysPath.resolve("ed25519_master_id_secret_key")).exists();
        assertThat(keysPath.resolve("ed25519_signing_secret_key")).exists();

        assertThat(keysPath.resolve("secret_id_key")).exists();
        assertThat(keysPath.resolve("secret_onion_key")).exists();
        assertThat(keysPath.resolve("secret_onion_key_ntor")).exists();
    }
}
