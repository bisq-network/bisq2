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

package bisq.network.tor.common.torrc;

import bisq.common.file.FileMutatorUtils;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TorrcFileGenerator {
    private final Path torrcPath;
    private final List<String> torrcConfigLines;
    private final Set<DirectoryAuthority> customDirectoryAuthorities;

    public TorrcFileGenerator(Path torrcPath,
                              List<String> torrcConfigLines,
                              Set<DirectoryAuthority> customDirectoryAuthorities) {
        this.torrcPath = torrcPath;
        this.torrcConfigLines = torrcConfigLines;
        this.customDirectoryAuthorities = customDirectoryAuthorities;
    }

    // Constructor to keep the change to List<String> smaller in scope
    public TorrcFileGenerator(Path torrcPath,
                              Map<String, String> torrcConfigMap,
                              Set<DirectoryAuthority> customDirectoryAuthorities) {
        this(torrcPath, mapToLines(torrcConfigMap), customDirectoryAuthorities);
    }

    private static List<String> mapToLines(Map<String, String> map) {
        List<String> lines = new ArrayList<>();
        map.forEach((key, value) -> lines.add(key + " " + value));
        return lines;
    }

    public void generate() {
        StringBuilder torrcStringBuilder = new StringBuilder();
        torrcConfigLines.forEach(line ->
                torrcStringBuilder.append(line).append("\n")
        );

        customDirectoryAuthorities.forEach(dirAuthority ->
                torrcStringBuilder.append("DirAuthority ").append(dirAuthority.getNickname())
                        .append(" orport=").append(dirAuthority.getOrPort())
                        .append(" v3ident=").append(dirAuthority.getV3Ident())
                        .append(" 127.0.0.1:").append(dirAuthority.getDirPort())
                        .append(" ").append(dirAuthority.getRelayFingerprint())
                        .append("\n"));


        try {
            FileMutatorUtils.writeToPath(torrcStringBuilder.toString(), torrcPath);
        } catch (IOException e) {
            throw new IllegalStateException("Couldn't create torrc file: " + torrcPath.toAbsolutePath());
        }
    }
}
