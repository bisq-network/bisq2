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

import bisq.wallets.json_rpc.RpcConfig;
import bisq.wallets.electrum.rpc.ElectrumDaemon;
import bisq.wallets.electrum.rpc.responses.ElectrumCreateResponse;
import bisq.wallets.regtest.AbstractRegtestSetup;
import bisq.wallets.regtest.bitcoind.RemoteBitcoind;
import bisq.wallets.regtest.process.MultiProcessCoordinator;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

public abstract class ElectrumRegtestSetup extends AbstractRegtestSetup<MultiProcessCoordinator> {
    protected ElectrumRegtest electrumRegtest;

    public ElectrumRegtestSetup() throws IOException {
    }

    @Override
    public List<String> mineOneBlock() {
        throw new UnsupportedOperationException();
    }

    public String fundAddress(String address, double amount) throws InterruptedException {
        return electrumRegtest.fundAddress(address, amount);
    }

    @Override
    public RpcConfig getRpcConfig() {
        throw new UnsupportedOperationException();
    }

    public abstract RemoteBitcoind getRemoteBitcoind();

    public Path getElectrumDataDir() {
        return electrumRegtest.getElectrumProcess().getDataDir();
    }

    public ElectrumDaemon getElectrumDaemon() {
        return electrumRegtest.getElectrumDaemon();
    }

    public ElectrumCreateResponse getWalletInfo() {
        return electrumRegtest.getWalletInfo();
    }
}
