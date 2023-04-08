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

package bisq.wallets.electrum.regtest.electrumx;

import bisq.wallets.json_rpc.RpcConfig;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.nio.file.Path;

@Builder
@Getter
@ToString
@EqualsAndHashCode
public final class ElectrumXServerConfig {
    private final Path dataDir;
    private final int port;
    private final int rpcPort;
    private final RpcConfig bitcoindRpcConfig;

    public ElectrumXServerConfig(
            Path dataDir,
            int port,
            int rpcPort,
            RpcConfig bitcoindRpcConfig
    ) {
        this.dataDir = dataDir;
        this.port = port;
        this.rpcPort = rpcPort;
        this.bitcoindRpcConfig = bitcoindRpcConfig;
    }
}
