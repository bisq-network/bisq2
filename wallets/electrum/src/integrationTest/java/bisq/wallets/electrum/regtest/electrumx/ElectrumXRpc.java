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

import bisq.wallets.json_rpc.RpcCallFailureException;
import bisq.wallets.process.cli.AbstractRpcCliProcess;
import bisq.wallets.process.cli.CliProcessConfig;

import java.util.List;

public class ElectrumXRpc extends AbstractRpcCliProcess {
    public ElectrumXRpc(int rpcPort) {
        super(CliProcessConfig.builder()
                .binaryName("electrumx_rpc")
                .defaultArgs(List.of(
                        "--port", String.valueOf(rpcPort)
                ))
                .build());
    }

    public void stop() {
        String output = runAndGetOutput("stop");
        if (!output.equals("\"stopping\"")) {
            throw new RpcCallFailureException("Failed to stop get electrumx_server");
        }
    }
}
