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

package bisq.network.tor.local_network.da;

import bisq.network.tor.local_network.TorNode;
import bisq.network.tor.local_network.da.keygen.process.DirectoryAuthorityKeyGenerator;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class DirectoryAuthorityFactory {

    @Getter
    private final Set<TorNode> allDirectoryAuthorities = new HashSet<>();

    public void createDirectoryAuthority(TorNode directoryAuthority,
                                         String passphrase) throws IOException, InterruptedException {
        Path dataDirPath = directoryAuthority.getDataDirPath();
        createDataDirPathIfNotPresent(dataDirPath);

        Path keysPath = dataDirPath.resolve("keys");
        if (!Files.exists(keysPath)) {
            try {
                Files.createDirectories(keysPath);
            } catch (IOException e) {
                throw new IllegalStateException("Couldn't create keys folder in data directory for directory authority.", e);
            }
            DirectoryAuthorityKeyGenerator.generate(directoryAuthority, passphrase);
        }

        allDirectoryAuthorities.add(directoryAuthority);
    }

    private void createDataDirPathIfNotPresent(Path dataDirPath) {
        if(!Files.exists(dataDirPath)) {
            try {
                Files.createDirectory(dataDirPath);
            } catch (IOException e) {
                throw new IllegalStateException("Couldn't create data directory for directory authority.", e);
            }
        }
    }
}
