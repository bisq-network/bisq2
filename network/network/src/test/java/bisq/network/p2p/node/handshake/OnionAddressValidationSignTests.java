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

package bisq.network.p2p.node.handshake;

import bisq.common.network.Address;
import bisq.common.network.DefaultClearNetLocalAddressFacade;
import bisq.security.keys.TorKeyGeneration;
import bisq.security.keys.TorKeyPair;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class OnionAddressValidationSignTests {
    private final TorKeyPair myTorKeyPair = TorKeyGeneration.generateKeyPair();
    private final TorKeyPair peerTorKeyPair = TorKeyGeneration.generateKeyPair();
    private final long signatureDate = System.currentTimeMillis();

    @Test
    void testSignNonOnionAddresses() {
        Address myAddress = DefaultClearNetLocalAddressFacade.toLocalHostAddress(1234);
        Address peerAddress = DefaultClearNetLocalAddressFacade.toLocalHostAddress(4321);
        Optional<byte[]> signature = OnionAddressValidation.sign(myAddress, peerAddress, signatureDate, myTorKeyPair.getPrivateKey());
        assertThat(signature).isEmpty();
    }

    @Test
    void testSignMyNonOnionAddress() {
        Address myAddress = DefaultClearNetLocalAddressFacade.toLocalHostAddress(1234);
        Address peerAddress = new Address(peerTorKeyPair.getOnionAddress(), 8888);
        Optional<byte[]> signature = OnionAddressValidation.sign(myAddress, peerAddress, signatureDate, myTorKeyPair.getPrivateKey());
        assertThat(signature).isEmpty();
    }

    @Test
    void testSignPeerNonOnionAddress() {
        Address myAddress = new Address(myTorKeyPair.getOnionAddress(), 8888);
        Address peerAddress = DefaultClearNetLocalAddressFacade.toLocalHostAddress(4321);
        Optional<byte[]> signature = OnionAddressValidation.sign(myAddress, peerAddress, signatureDate, myTorKeyPair.getPrivateKey());
        assertThat(signature).isEmpty();
    }

    @Test
    void testSignOnionAddress() {
        Address myAddress = new Address(myTorKeyPair.getOnionAddress(), 8888);
        Address peerAddress = new Address(peerTorKeyPair.getOnionAddress(), 8888);
        Optional<byte[]> signature = OnionAddressValidation.sign(myAddress, peerAddress, signatureDate, myTorKeyPair.getPrivateKey());
        assertThat(signature).isNotEmpty();
    }
}
