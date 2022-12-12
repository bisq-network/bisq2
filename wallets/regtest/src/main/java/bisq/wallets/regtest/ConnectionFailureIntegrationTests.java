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

package bisq.wallets.regtest;

import bisq.wallets.bitcoind.rpc.BitcoindDaemon;
import bisq.wallets.core.RpcConfig;
import bisq.wallets.core.rpc.RpcClientFactory;
import bisq.wallets.json_rpc.JsonRpcClient;
import bisq.wallets.json_rpc.exceptions.InvalidRpcCredentialsException;
import bisq.wallets.process.BisqProcess;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class ConnectionFailureIntegrationTests<T extends BisqProcess> {

    private AbstractRegtestSetup<T> regtestSetup;

    protected abstract AbstractRegtestSetup<T> createRegtestSetup() throws IOException;

    @BeforeAll
    void setup() throws IOException, InterruptedException {
        regtestSetup = createRegtestSetup();
        regtestSetup.start();
    }

    @AfterAll
    void cleanUp() {
        regtestSetup.shutdown();
    }

    @Test
    void wrongRpcCredentialsTest() {
        RpcConfig validRpcConfig = regtestSetup.getRpcConfig();
        RpcConfig wrongRpcConfig = RpcConfig.builder()
                .hostname(validRpcConfig.getHostname())
                .port(validRpcConfig.getPort())
                .user(validRpcConfig.getUser())
                .password("WRONG_PASSWORD")
                .build();

        JsonRpcClient rpcClient = RpcClientFactory.createDaemonRpcClient(wrongRpcConfig);
        var minerChainBackend = new BitcoindDaemon(rpcClient);

        Assertions.assertThatExceptionOfType(InvalidRpcCredentialsException.class)
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
        Assertions.assertThat(isValid).isFalse();
    }

    @Test
    void verifyValidRpcConfigTest() {
        RpcConfig validRpcConfig = regtestSetup.getRpcConfig();
        boolean isValid = BitcoindDaemon.verifyRpcConfig(validRpcConfig);
        Assertions.assertThat(isValid).isTrue();
    }
}
