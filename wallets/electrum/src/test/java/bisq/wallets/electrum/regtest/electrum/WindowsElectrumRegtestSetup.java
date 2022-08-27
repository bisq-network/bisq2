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

package bisq.wallets.electrum.regtest.electrum;

import bisq.common.util.FileUtils;
import bisq.wallets.core.RpcConfig;
import bisq.wallets.regtest.bitcoind.BitcoindRegtestSetup;
import bisq.wallets.regtest.bitcoind.RemoteBitcoind;
import bisq.wallets.regtest.process.MultiProcessCoordinator;
import lombok.Getter;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WindowsElectrumRegtestSetup extends ElectrumRegtestSetup {

    private static final String BITCOIND_HOST_ENV_VAR = "BISQ_REGTEST_BITCOIND_HOST";
    private static final String BITCOIND_PORT_ENV_VAR = "BISQ_REGTEST_BITCOIND_PORT";
    private static final String ELECTRUMX_HOST_ENV_VAR = "BISQ_REGTEST_ELECTRUMX_HOST";
    private static final String ELECTRUMX_PORT_ENV_VAR = "BISQ_REGTEST_ELECTRUMX_PORT";

    @Getter
    private final RemoteBitcoind remoteBitcoind;

    public WindowsElectrumRegtestSetup() throws IOException {
        this(false);
    }

    public WindowsElectrumRegtestSetup(boolean doCreateWallet) throws IOException {
        RpcConfig bitcoindRpcConfig = createBitcoindRpcConfigFromEnvironmentVars();
        Path tmpDirPath = FileUtils.createTempDir();
        this.remoteBitcoind = new RemoteBitcoind(tmpDirPath, bitcoindRpcConfig, true);

        RpcConfig electrumXRpcConfig = createElectrumXRpcConfigFromEnvironmentVars();
        this.electrumRegtest = new ElectrumRegtest(
                remoteBitcoind,
                electrumXRpcConfig.getHostname(),
                electrumXRpcConfig.getPort(),
                doCreateWallet
        );
    }

    @Override
    protected MultiProcessCoordinator createProcess() {
        return new MultiProcessCoordinator(
                List.of(remoteBitcoind, electrumRegtest)
        );
    }

    public static boolean isExternalBitcoindAndElectrumXEnvironment() {
        Map<String, String> envVars = System.getenv();
        return envVars.containsKey(BITCOIND_HOST_ENV_VAR) &&
                envVars.containsKey(BITCOIND_PORT_ENV_VAR) &&
                envVars.containsKey(ELECTRUMX_HOST_ENV_VAR) &&
                envVars.containsKey(ELECTRUMX_PORT_ENV_VAR);
    }

    private static RpcConfig createBitcoindRpcConfigFromEnvironmentVars() {
        String hostname = System.getenv(BITCOIND_HOST_ENV_VAR);
        Objects.requireNonNull(
                hostname,
                String.format("Cannot find %s environment variable.", BITCOIND_HOST_ENV_VAR)
        );

        String portAsString = System.getenv(BITCOIND_PORT_ENV_VAR);
        Objects.requireNonNull(
                portAsString,
                String.format("Cannot find %s environment variable.", BITCOIND_PORT_ENV_VAR)
        );

        int port = Integer.parseInt(portAsString);
        return BitcoindRegtestSetup.createRpcConfig(hostname, port);
    }

    private static RpcConfig createElectrumXRpcConfigFromEnvironmentVars() {
        String hostname = System.getenv(ELECTRUMX_HOST_ENV_VAR);
        Objects.requireNonNull(
                hostname,
                String.format("Cannot find %s environment variable.", ELECTRUMX_HOST_ENV_VAR)
        );

        String portAsString = System.getenv(ELECTRUMX_PORT_ENV_VAR);
        Objects.requireNonNull(
                portAsString,
                String.format("Cannot find %s environment variable.", ELECTRUMX_PORT_ENV_VAR)
        );

        int port = Integer.parseInt(portAsString);
        return BitcoindRegtestSetup.createRpcConfig(hostname, port);
    }
}
