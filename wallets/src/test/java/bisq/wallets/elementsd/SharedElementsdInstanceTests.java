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

package bisq.wallets.elementsd;

import bisq.wallets.AbstractRegtestSetup;
import bisq.wallets.AbstractSharedRegtestInstanceTests;
import bisq.wallets.bitcoind.BitcoindRegtestSetup;
import bisq.wallets.bitcoind.rpc.BitcoindDaemon;
import bisq.wallets.elementsd.rpc.ElementsdDaemon;
import bisq.wallets.elementsd.rpc.ElementsdWallet;
import bisq.wallets.elementsd.rpc.responses.ElementsdGetPeginAddressResponse;
import bisq.wallets.process.MultiProcessCoordinator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class SharedElementsdInstanceTests
        extends AbstractSharedRegtestInstanceTests<MultiProcessCoordinator, ElementsdWallet> {

    protected BitcoindRegtestSetup bitcoindRegtestSetup;
    protected ElementsdRegtestSetup elementsdRegtestSetup;

    protected BitcoindDaemon bitcoindDaemon;
    protected ElementsdDaemon elementsdDaemon;
    protected ElementsdWallet elementsdMinerWallet;

    @Override
    public AbstractRegtestSetup<MultiProcessCoordinator, ElementsdWallet> createRegtestSetup() throws IOException {
        elementsdRegtestSetup = new ElementsdRegtestSetup();
        return elementsdRegtestSetup;
    }

    @Override
    @BeforeAll
    public void start() throws IOException {
        super.start();

        bitcoindRegtestSetup = elementsdRegtestSetup.getBitcoindRegtestSetup();
        bitcoindRegtestSetup.mineInitialRegtestBlocks();

        bitcoindDaemon = bitcoindRegtestSetup.getDaemon();
        elementsdDaemon = elementsdRegtestSetup.getDaemon();
        elementsdMinerWallet = elementsdRegtestSetup.getMinerWallet();
    }

    protected void peginBtc(double amount) {
        ElementsdGetPeginAddressResponse peginAddressResponse = elementsdMinerWallet.getPeginAddress();

        String bitcoindTxId = bitcoindRegtestSetup.fundAddress(peginAddressResponse.getMainChainAddress(), amount);
        String rawBitcoindTx = bitcoindDaemon.getRawTransaction(bitcoindTxId);

        // main chain tx needs 102 confirmations for pegin (fundAddress mines one block automatically)
        bitcoindRegtestSetup.mineBlocks(101);

        String txOutProof = bitcoindDaemon.getTxOutProof(List.of(bitcoindTxId));
        elementsdRegtestSetup.mineOneBlock();

        elementsdMinerWallet.claimPegin(rawBitcoindTx, txOutProof);
        elementsdRegtestSetup.mineOneBlock();
    }
}
