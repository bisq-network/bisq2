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

package bisq.tor.local_network.torrc;

import bisq.tor.local_network.TorNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

public class TorrcFileGenerator {
    private final Path torrcPath;
    private final TorrcConfigGenerator torrcConfigGenerator;
    private final Set<TorNode> allDirAuthorities;

    public TorrcFileGenerator(Path torrcPath, TorrcConfigGenerator torrcConfigGenerator, Set<TorNode> allDirAuthorities) {
        this.torrcPath = torrcPath;
        this.torrcConfigGenerator = torrcConfigGenerator;
        this.allDirAuthorities = allDirAuthorities;
    }

    public void generate() throws IOException {
        Map<String, String> torrcConfigs = torrcConfigGenerator.generate();

        StringBuilder torrcStringBuilder = new StringBuilder();
        torrcConfigs.forEach((key, value) ->
                torrcStringBuilder.append(key)
                        .append(" ")
                        .append(value)
                        .append("\n")
        );

        allDirAuthorities.forEach(dirAuthority ->
                torrcStringBuilder.append("DirAuthority ").append(dirAuthority.getNickname())
                        .append(" orport=").append(dirAuthority.getOrPort())
                        .append(" v3ident=").append(dirAuthority.getAuthorityIdentityKeyFingerprint().orElseThrow())
                        .append(" 127.0.0.1:").append(dirAuthority.getDirPort())
                        .append(" ").append(dirAuthority.getRelayKeyFingerprint().orElseThrow())
                        .append("\n"));

        Files.writeString(torrcPath, torrcStringBuilder.toString());
    }
}
