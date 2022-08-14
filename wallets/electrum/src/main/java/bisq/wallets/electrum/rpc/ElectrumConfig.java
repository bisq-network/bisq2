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

package bisq.wallets.electrum.rpc;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.nio.file.Path;

@Builder
@Getter
@ToString
@EqualsAndHashCode
public final class ElectrumConfig {
    private final Path dataDir;
    private final int electrumXServerPort;
    private final String rpcHost;
    private final int rpcPort;

    public ElectrumConfig(Path dataDir, int electrumXServerPort, String rpcHost, int rpcPort) {
        this.dataDir = dataDir;
        this.electrumXServerPort = electrumXServerPort;
        this.rpcHost = rpcHost;
        this.rpcPort = rpcPort;
    }
}