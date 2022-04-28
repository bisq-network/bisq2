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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ConnectionFailureIntegrationTests<T extends BisqProcess, W> {

    private AbstractRegtestSetup<T, W> regtestSetup;

    protected abstract AbstractRegtestSetup<T, W> createRegtestSetup() throws IOException;

    @BeforeAll
    void setup() throws IOException {
        regtestSetup = createRegtestSetup();
        regtestSetup.start();
    }

    @AfterAll
    void cleanUp() {
        regtestSetup.shutdown();
    }

    @Test
    void wrongRpcCredentialsTest() throws IOException {
        RpcConfig validRpcConfig = regtestSetup.getRpcConfig();
        RpcConfig wrongRpcConfig = RpcConfig.builder()
                .hostname(validRpcConfig.getHostname())
                .port(validRpcConfig.getPort())
                .user(validRpcConfig.getUser())
                .password("WRONG_PASSWORD")
                .build();

        DaemonRpcClient rpcClient = RpcClientFactory.createDaemonRpcClient(wrongRpcConfig);
        var minerChainBackend = new BitcoindDaemon(rpcClient);

        assertThatExceptionOfType(InvalidRpcCredentialsException.class)
                .isThrownBy(minerChainBackend::listWallets);
    }

    @Test
    void verifyInvalidRpcConfigTest() {
        RpcConfig validRpcConfig = regtestSetup.getRpcConfig();
        RpcConfig wrongRpcConfig = RpcConfig.builder()
                .hostname(validRpcConfig.getHostname())
                .port(validRpcConfig.getPort())
                .user(validRpcConfig.getUser())
                .password("WRONG_PASSWORD")
                .build();

        boolean isValid = BitcoindDaemon.verifyRpcConfig(wrongRpcConfig);
        assertThat(isValid).isFalse();
    }

    @Test
    void verifyValidRpcConfigTest() {
        RpcConfig validRpcConfig = regtestSetup.getRpcConfig();
        boolean isValid = BitcoindDaemon.verifyRpcConfig(validRpcConfig);
        assertThat(isValid).isTrue();
    }
}
