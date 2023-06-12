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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class DirectoryAuthorityKeyGenerationTests {
    @Test
    public void generateKeys(@TempDir Path tempDir) throws IOException, InterruptedException {
        var torDAKeyGenProcess = new DirectoryIdentityKeyGenProcess(tempDir, "127.0.0.1:8080");
        var directoryAuthorityKeyGenerator = new DirectoryAuthorityKeyGenerator(torDAKeyGenProcess);
        directoryAuthorityKeyGenerator.generate("my_passphrase");

        File tempDirFile = tempDir.toFile();
        assertThat(new File(tempDirFile, "authority_certificate"))
                .exists();
        assertThat(new File(tempDirFile, "authority_identity_key"))
                .exists();
        assertThat(new File(tempDirFile, "authority_signing_key"))
                .exists();
    }
}
