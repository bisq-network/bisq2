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

package bisq.wallets.electrum.rpc.cli;

import bisq.wallets.core.exceptions.RpcCallFailureException;
import bisq.wallets.process.cli.AbstractRpcCliProcess;
import bisq.wallets.process.cli.CliProcessConfig;

import java.nio.file.Path;
import java.util.List;

public class ElectrumCli extends AbstractRpcCliProcess {
    public static final String ELECTRUM_REGTEST_ARG = "--regtest";
    public static final String ELECTRUM_DATA_DIR_ARG = "--dir";
    public static final String ELECTRUM_HELP_ARG = "--help";

    private static final String ELECTRUM_SETCONFIG_ARG = "setconfig";
    private static final String ELECTRUM_GETCONFIG_ARG = "getconfig";
    private static final String ELECTRUM_STOP_ARG = "stop";

    public ElectrumCli(Path binaryPath, Path dataDir) {
        super(CliProcessConfig.builder()
                .binaryName(
                        String.valueOf(binaryPath.toAbsolutePath())
                )
                .defaultArgs(List.of(
                        ELECTRUM_REGTEST_ARG,
                        ELECTRUM_DATA_DIR_ARG, dataDir.toAbsolutePath().toString()
                ))
                .build()
        );
    }

    public String getConfig(String configName) {
        String output = runAndGetOutput(ELECTRUM_GETCONFIG_ARG, configName);
        if (output.isEmpty()) {
            throw new RpcCallFailureException("Failed to get electrum config: " + configName);
        }
        return output;
    }

    public void setConfig(String configName, String configValue) {
        String output = runAndGetOutput(ELECTRUM_SETCONFIG_ARG, configName, configValue);
        if (!output.equals("true")) {
            throw new RpcCallFailureException("Failed to set electrum config: " + configName + " = " + configValue);
        }
    }

    public String help() {
        return runAndGetOutput(ELECTRUM_HELP_ARG);
    }

    public void stop() {
        String output = runAndGetOutput(ELECTRUM_STOP_ARG);
        boolean isStopped = output.equals("Daemon stopped");
        if (!isStopped) {
            throw new RpcCallFailureException("Cannot stop electrum daemon: " + output);
        }
    }
}
