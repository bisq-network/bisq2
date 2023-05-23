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

import bisq.common.util.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class OnionServiceDataDirManagerTests {
    @Test
    void persistTest(@TempDir Path tempDir) {
        var dataDirManager = new OnionServiceDataDirManager(tempDir.toFile());
        var result = new CreateHiddenServiceResult("abc", "ED25519-V3 abc:def");
        dataDirManager.persist(result);

        String hostName = dataDirManager.getHostName().orElseThrow();
        assertThat(hostName).isEqualTo(result.getServiceId() + ".onion");

        String privateKey = dataDirManager.getPrivateKey().orElseThrow();
        assertThat(privateKey).isEqualTo(result.getPrivateKey());
    }

    @Test
    void persistIfHostNameFileExists(@TempDir Path tempDir) throws IOException {
        var dataDirManager = new OnionServiceDataDirManager(tempDir.toFile());
        var result = new CreateHiddenServiceResult("abc", "ED25519-V3 abc:def");
        dataDirManager.persist(result);

        File hostNameFile = new File(tempDir.toFile(), OnionServiceDataDirManager.HOST_NAME_FILENAME);
        FileUtils.makeFile(hostNameFile);

        String privateKey = dataDirManager.getPrivateKey().orElseThrow();
        assertThat(privateKey).isEqualTo(result.getPrivateKey());
    }

    @Test
    void persistIfPrivateKeyExists(@TempDir Path tempDir) throws IOException {
        var dataDirManager = new OnionServiceDataDirManager(tempDir.toFile());
        var result = new CreateHiddenServiceResult("abc", "ED25519-V3 abc:def");
        dataDirManager.persist(result);

        File privateKeyFile = new File(tempDir.toFile(), OnionServiceDataDirManager.PRIVATE_KEY_FILENAME);
        FileUtils.makeFile(privateKeyFile);

        String hostName = dataDirManager.getHostName().orElseThrow();
        assertThat(hostName).isEqualTo(result.getServiceId() + ".onion");
    }

    @Test
    void readBeforePersisting(@TempDir Path tempDir) {
        var dataDirManager = new OnionServiceDataDirManager(tempDir.toFile());
        assertThat(dataDirManager.getHostName())
                .isEmpty();
        assertThat(dataDirManager.getPrivateKey())
                .isEmpty();
    }
}
