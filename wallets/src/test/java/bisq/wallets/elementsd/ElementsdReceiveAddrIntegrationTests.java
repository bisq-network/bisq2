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

import bisq.wallets.AddressType;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ElementsdReceiveAddrIntegrationTests extends SharedElementsdInstanceTests {
    @Test
    void getNewLegacyAddress() {
        String address = elementsdMinerWallet.getNewAddress(AddressType.LEGACY, "");
        assertThat(address).startsWith("CTE");
    }

    @Test
    void getNewP2ShSegwitAddress() {
        String address = elementsdMinerWallet.getNewAddress(AddressType.P2SH_SEGWIT, "");
        assertThat(address).startsWith("Azp");
    }

    @Test
    void getNewBech32Address() {
        String address = elementsdMinerWallet.getNewAddress(AddressType.BECH32, "");
        assertThat(address)
                .startsWith("el1qq");
    }
}
