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

package bisq.support.mediation.bisq_easy;

import bisq.common.network.Address;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import org.junit.jupiter.api.Test;

import java.security.KeyPair;
import java.security.PublicKey;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The receiver of a confidential message compares the sender public key of the message with the key which encrypted
 * and signed it. A response of the mediator must therefore contain the network ID of the mediator.
 */
class BisqEasyMediatorsResponseTest {
    private static final String TRADE_ID = "tradeId";

    @Test
    void responseContainsTheSenderPublicKey() throws UnresolvableProtobufException {
        KeyPair keyPair = KeyGeneration.generateDefaultEcKeyPair();
        BisqEasyMediatorsResponse response = new BisqEasyMediatorsResponse(TRADE_ID, createNetworkId(keyPair.getPublic()));

        BisqEasyMediatorsResponse received = BisqEasyMediatorsResponse.fromProto(response.getValueBuilder(false).build());

        assertTrue(received.isSenderPublicKeyRequired());
        assertEquals(Optional.of(keyPair.getPublic()), received.findSenderPublicKey());
        assertEquals(keyPair.getPublic(), received.getSenderPublicKey());
    }

    // Versions before the sender network ID was added sent version 0 without it. The sender public key is required for
    // these responses as well, so the receiver does not accept them.
    @Test
    void responseOfVersion0RequiresTheSenderPublicKeyWhichItDoesNotContain() throws UnresolvableProtobufException {
        BisqEasyMediatorsResponse received = BisqEasyMediatorsResponse.fromProto(
                bisq.support.protobuf.MediatorsResponse.newBuilder()
                        .setVersion(0)
                        .setTradeId(TRADE_ID)
                        .build());

        assertTrue(received.isSenderPublicKeyRequired());
        assertEquals(Optional.empty(), received.findSenderPublicKey());
        assertThrows(IllegalStateException.class, received::getSenderPublicKey);
    }

    @Test
    void responseOfThisVersionWithoutSenderNetworkIdIsRejected() {
        bisq.support.protobuf.MediatorsResponse proto = bisq.support.protobuf.MediatorsResponse.newBuilder()
                .setVersion(1)
                .setTradeId(TRADE_ID)
                .build();

        assertInstanceOf(IllegalArgumentException.class,
                assertThrows(UnresolvableProtobufException.class,
                        () -> BisqEasyMediatorsResponse.fromProto(proto)).getCause());
    }

    private static NetworkId createNetworkId(PublicKey publicKey) {
        Address address = Address.from("127.0.0.1", 1000);
        return new NetworkId(new AddressByTransportTypeMap(Map.of(address.getTransportType(), address)),
                new PubKey(publicKey, "keyId"));
    }
}
