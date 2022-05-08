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

package bisq.wallets;

import bisq.wallets.bitcoind.rpc.BitcoindDaemon;
import bisq.wallets.exceptions.InvalidRpcCredentialsException;
import bisq.wallets.process.BisqProcess;
import bisq.wallets.rpc.DaemonRpcClient;
import bisq.wallets.rpc.RpcClientFactory;
import bisq.wallets.rpc.RpcConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public abstract class ConnectionFailureIntegrationTests<T extends BisqProcess, W> {

    protected abstract AbstractRegtestSetup<T, W> createRegtestSetup() throws IOException;

    @Test
    void wrongRpcCredentialsTest() throws IOException {
        AbstractRegtestSetup<T, W> regtestSetup = createRegtestSetup();
        regtestSetup.start();

        RpcConfig wrongRpcConfig = new RpcConfig.Builder(regtestSetup.getRpcConfig())
                .password("WRONG_PASSWORD")
                .build();

        DaemonRpcClient rpcClient = RpcClientFactory.createDaemonRpcClient(wrongRpcConfig);
        var minerChainBackend = new BitcoindDaemon(rpcClient);

        assertThatExceptionOfType(InvalidRpcCredentialsException.class)
                .isThrownBy(minerChainBackend::listWallets);

        regtestSetup.shutdown();
    }
}
