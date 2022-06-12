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

package bisq.wallets.electrum.regtest.electrum.cli;

import bisq.wallets.core.exceptions.RpcCallFailureException;
import bisq.wallets.electrum.regtest.AbstractRpcCliProcess;
import bisq.wallets.electrum.regtest.CliProcessConfig;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public class ElectrumCli extends AbstractRpcCliProcess {
    public static final String ELECTRUM_BINARY_NAME = "electrum";
    public static final String ELECTRUM_REGTEST_ARG = "--regtest";
    public static final String ELECTRUM_DATA_DIR_ARG = "--dir";

    private static final String ELECTRUM_SETCONFIG_ARG = "setconfig";
    private static final String ELECTRUM_GETCONFIG_ARG = "getconfig";

    public ElectrumCli(Path dataDir) {
        super(CliProcessConfig.builder()
                .binaryName(ELECTRUM_BINARY_NAME)
                .defaultArgs(List.of(
                        ELECTRUM_REGTEST_ARG,
                        ELECTRUM_DATA_DIR_ARG, dataDir.toAbsolutePath().toString()
                ))
                .build()
        );
    }

    public String getConfig(String configName) {
        try {
            Process process = runCliProcess(
                    ELECTRUM_GETCONFIG_ARG,
                    configName
            );

            String output = readProcessOutput(process);
            if (output.isEmpty()) {
                throw new RpcCallFailureException("Failed to get electrum config: " + configName);
            }

            return output;

        } catch (InterruptedException | IOException e) {
            throw new RpcCallFailureException("Failed to get electrum config: " + configName, e);
        }
    }

    public void setConfig(String configName, String configValue) {
        try {
            Process process = runCliProcess(
                    ELECTRUM_SETCONFIG_ARG,
                    configName,
                    configValue
            );

            String output = readProcessOutput(process);
            if (!output.equals("true")) {
                throw new RpcCallFailureException("Failed to set electrum config: " + configName + " = " + configValue);
            }

        } catch (InterruptedException | IOException e) {
            throw new RpcCallFailureException("Failed to set electrum config: " + configName + " = " + configValue, e);
        }
    }

    public void stop() {
        try {
            Process process = runCliProcess("stop");
            String output = readProcessOutput(process);

            boolean isStopped = output.equals("Daemon stopped");
            if (!isStopped) {
                throw new RpcCallFailureException("Cannot stop electrum daemon: " + output);
            }

        } catch (InterruptedException | IOException e) {
            throw new RpcCallFailureException("Failed to get electrum rpc password", e);
        }
    }
}
