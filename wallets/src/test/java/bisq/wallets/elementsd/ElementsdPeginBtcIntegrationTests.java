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

import bisq.wallets.elementsd.rpc.responses.ElementsdGetPeginAddressResponse;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class ElementsdPeginBtcIntegrationTests extends SharedElementsdInstanceTests {

    @Test
    void getPeginAddress() {
        elementsdMinerWallet.getPeginAddress();
    }

    @Test
    void claimPegin() {
        ElementsdGetPeginAddressResponse peginAddressResponse = elementsdMinerWallet.getPeginAddress();

        String bitcoindTxId = bitcoindRegtestSetup.fundAddress(peginAddressResponse.getMainChainAddress(), 20);
        String rawBitcoindTx = bitcoindDaemon.getRawTransaction(bitcoindTxId);

        // main chain tx needs 102 confirmations for pegin (fundAddress mines one block automatically)
        bitcoindRegtestSetup.mineBlocks(101);

        String txOutProof = bitcoindDaemon.getTxOutProof(List.of(bitcoindTxId));
        elementsdRegtestSetup.mineOneBlock();

        elementsdMinerWallet.claimPegin(rawBitcoindTx, txOutProof);
        elementsdRegtestSetup.mineOneBlock();

        assertThat(elementsdMinerWallet.getLBtcBalance())
                .isGreaterThan(19);  // Not 20 because of mining fees
    }
}
