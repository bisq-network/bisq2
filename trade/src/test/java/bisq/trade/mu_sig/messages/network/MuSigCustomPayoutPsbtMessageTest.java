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

class MuSigCustomPayoutPsbtMessageTest {
    private static final String TX_ID = "ab".repeat(32);

    @Test
    void roundTripsThroughTradeMessageResolver() {
        MuSigCustomPayoutPsbtMessage message = createMessage(new byte[]{1, 2, 3});

        bisq.trade.protobuf.TradeMessage proto = message.resolveValueProto(false);
        TradeMessage restored = TradeMessage.fromProto(proto);

        assertThat(proto.getMuSigTradeMessage().getMessageCase())
                .isEqualTo(bisq.trade.protobuf.MuSigTradeMessage.MessageCase.MUSIGCUSTOMPAYOUTPSBTMESSAGE);
        assertThat(restored).isEqualTo(message);
    }

    @Test
    void defensivelyCopiesByteArrays() {
        byte[] mediationResultHash = new byte[20];
        byte[] psbt = {1, 2, 3};
        MuSigCustomPayoutPsbtMessage message = createMessage(mediationResultHash, psbt);

        mediationResultHash[0] = 99;
        psbt[0] = 99;
        byte[] returnedHash = message.getMediationResultHash();
        byte[] returnedPsbt = message.getPsbt();
        returnedHash[1] = 99;
        returnedPsbt[1] = 99;

        assertThat(message.getMediationResultHash()).containsOnly(0);
        assertThat(message.getPsbt()).containsExactly(1, 2, 3);
    }

    @Test
    void excludesPsbtFromToString() {
        MuSigCustomPayoutPsbtMessage message = createMessage(new byte[]{1, 2, 3});

        assertThat(message.toString()).doesNotContain("psbt=");
    }

    @Test
    void enforcesPsbtSizeLimit() {
        assertThat(createMessage(new byte[MuSigCustomPayoutPsbtMessage.MAX_PSBT_SIZE]).getPsbt())
                .hasSize(MuSigCustomPayoutPsbtMessage.MAX_PSBT_SIZE);
        assertThatThrownBy(() -> createMessage(new byte[MuSigCustomPayoutPsbtMessage.MAX_PSBT_SIZE + 1]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsInvalidPayload() {
        assertThatThrownBy(() -> createMessage(new byte[19], new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> createMessage(new byte[20], "gg".repeat(32), new byte[]{1}))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> createMessage(new byte[20], new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static MuSigCustomPayoutPsbtMessage createMessage(byte[] psbt) {
        return createMessage(new byte[20], psbt);
    }

    private static MuSigCustomPayoutPsbtMessage createMessage(byte[] mediationResultHash, byte[] psbt) {
        return createMessage(mediationResultHash, TX_ID, psbt);
    }

    private static MuSigCustomPayoutPsbtMessage createMessage(byte[] mediationResultHash,
                                                              String txId,
                                                              byte[] psbt) {
        return new MuSigCustomPayoutPsbtMessage(
                "message-id",
                "trade-id",
                "1",
                createNetworkId(9998, "sender-key"),
                createNetworkId(9999, "receiver-key"),
                mediationResultHash,
                txId,
                psbt);
    }

    private static NetworkId createNetworkId(int port, String keyId) {
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, new ClearnetAddress("127.0.0.1", port)));
        PubKey pubKey = new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), keyId);
        return new NetworkId(addresses, pubKey);
    }
}
