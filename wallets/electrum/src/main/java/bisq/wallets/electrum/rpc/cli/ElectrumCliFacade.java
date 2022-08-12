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

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ElectrumCliFacade {

    private final ElectrumCli electrumCli;

    public ElectrumCliFacade(ElectrumCli electrumCli) {
        this.electrumCli = electrumCli;
    }

    public void enableLoggingToFile() {
        electrumCli.setConfig("log_to_file", "true");
    }

    public void setRpcHost(String host) {
        electrumCli.setConfig("rpchost", host);
    }

    public void setRpcPort(int port) {
        electrumCli.setConfig("rpcport", String.valueOf(port));
    }

    public int getRpcPort() {
        String rpcPort = electrumCli.getConfig("rpcport");
        return Integer.parseInt(rpcPort);
    }

    public String getRpcUser() {
        return electrumCli.getConfig("rpcuser");
    }

    public String getRpcPassword() {
        return electrumCli.getConfig("rpcpassword");
    }

    public void stop() {
        electrumCli.stop();
    }
}
