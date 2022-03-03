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

package bisq.wallets.bitcoind;

import bisq.wallets.AddressType;
import bisq.wallets.bitcoind.rpc.BitcoindWallet;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

import static org.assertj.core.api.Assertions.assertThat;

public class BitcoindSendIntegrationTests extends SharedBitcoindInstanceTests {
    @Test
    public void sendOneBtcToAddress() throws MalformedURLException {
        regtestSetup.mineInitialRegtestBlocks();
        BitcoindWallet minerWallet = regtestSetup.getMinerWallet();
        var receiverBackend = regtestSetup.createNewWallet("receiver_wallet");

        String receiverAddress = receiverBackend.getNewAddress(AddressType.BECH32, "");
        minerWallet.sendToAddress(receiverAddress, 1);
        regtestSetup.mineOneBlock();

        assertThat(receiverBackend.getBalance())
                .isEqualTo(1);
    }
}
