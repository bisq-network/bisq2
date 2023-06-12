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

import lombok.Builder;
import lombok.Getter;

import java.nio.file.Path;

@Builder
@Getter
public class DirectoryAuthority {
    private final String nickname;

    private final Path dataDir;

    private final int controlPort;
    private final int orPort;
    private final int dirPort;

    private final String v3LongTermSigningKeyFingerprint;
    private final String torKeyFingerprint;

    private final String exitPolicy = "ExitPolicy accept *:*";

    public Path getTorrcPath() {
        return dataDir.resolve("torrc");
    }
}
