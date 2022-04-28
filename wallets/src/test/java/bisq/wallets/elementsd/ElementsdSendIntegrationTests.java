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

import bisq.wallets.model.AddressType;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;

import static org.assertj.core.api.Assertions.assertThat;

public class ElementsdSendIntegrationTests extends SharedElementsdInstanceTests {
    @Test
    public void sendOneLBtcToAddress() throws MalformedURLException {
        peginBtc(20);
        var receiverBackend = elementsdRegtestSetup.createNewWallet("receiver_wallet");

        String receiverAddress = receiverBackend.getNewAddress(AddressType.BECH32, "");
        elementsdMinerWallet.sendLBtcToAddress(receiverAddress, 1);
        elementsdRegtestSetup.mineOneBlock();

        assertThat(receiverBackend.getLBtcBalance())
                .isEqualTo(1);
    }
}
