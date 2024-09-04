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
import bisq.wallets.json_rpc.RpcConfig;
import bisq.wallets.json_rpc.RpcClientFactory;
import bisq.wallets.json_rpc.JsonRpcClient;
import bisq.wallets.json_rpc.exceptions.InvalidRpcCredentialsException;
import bisq.wallets.regtest.process.BisqProcess;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

public abstract class ConnectionFailureIntegrationTests<T extends BisqProcess> {

    private final AbstractRegtestSetup<T> regtestSetup;

    public ConnectionFailureIntegrationTests(AbstractRegtestSetup<T> regtestSetup) {
        this.regtestSetup = regtestSetup;
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
