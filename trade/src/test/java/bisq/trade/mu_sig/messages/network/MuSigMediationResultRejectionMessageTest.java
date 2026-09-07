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

package bisq.trade.mu_sig.messages.network;

import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.ClearnetAddress;
import bisq.common.network.TransportType;
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.trade.protocol.messages.TradeMessage;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MuSigMediationResultRejectionMessageTest {
    @Test
    void roundTripsThroughTradeMessageResolver() {
        MuSigMediationResultRejectionMessage message = createMessage(createMediationResultHash());

        bisq.trade.protobuf.TradeMessage proto = message.resolveValueProto(false);
        TradeMessage restored = TradeMessage.fromProto(proto);

        assertThat(proto.getMuSigTradeMessage().getMessageCase())
                .isEqualTo(bisq.trade.protobuf.MuSigTradeMessage.MessageCase.MUSIGMEDIATIONRESULTREJECTIONMESSAGE);
        assertThat(restored).isEqualTo(message);
    }

    @Test
    void defensivelyCopiesMediationResultHash() {
        byte[] mediationResultHash = createMediationResultHash();
        byte[] expectedHash = mediationResultHash.clone();
        MuSigMediationResultRejectionMessage message = createMessage(mediationResultHash);

        mediationResultHash[0] = 99;
        byte[] returnedHash = message.getMediationResultHash();
        returnedHash[1] = 99;

        assertThat(message.getMediationResultHash()).containsExactly(expectedHash);
    }

    @Test
    void rejectsInvalidMediationResultHash() {
        assertThatThrownBy(() -> createMessage(new byte[19]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> createMessage(new byte[21]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static MuSigMediationResultRejectionMessage createMessage(byte[] mediationResultHash) {
        return new MuSigMediationResultRejectionMessage(
                "message-id",
                "trade-id",
                "1",
                createNetworkId(9998, "sender-key"),
                createNetworkId(9999, "receiver-key"),
                mediationResultHash);
    }

    private static byte[] createMediationResultHash() {
        return new byte[]{
                1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                11, 12, 13, 14, 15, 16, 17, 18, 19, 20
        };
    }

    private static NetworkId createNetworkId(int port, String keyId) {
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, new ClearnetAddress("127.0.0.1", port)));
        PubKey pubKey = new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), keyId);
        return new NetworkId(addresses, pubKey);
    }
}
