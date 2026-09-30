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

package bisq.trade.mu_sig;

import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.ClearnetAddress;
import bisq.common.network.TransportType;
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import bisq.trade.mu_sig.messages.grpc.CustomCloseTradeResponse;
import bisq.trade.mu_sig.messages.grpc.CustomPayoutPsbt;
import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MuSigCustomPayoutPartyDataTest {
    private static final String TX_ID = "ab".repeat(32);
    private static final byte[] LOCAL_PSBT = {1, 2, 3};
    private static final byte[] PEER_PSBT = {4, 5, 6};

    @Test
    void localPartyDataRoundTripsThroughTradeParty() {
        MuSigTradeParty party = new MuSigTradeParty(createNetworkId());
        CustomPayoutPsbt localPsbt = new CustomPayoutPsbt(LOCAL_PSBT, TX_ID, 60_000, 40_000);
        CustomCloseTradeResponse closeResponse = new CustomCloseTradeResponse(new byte[]{7, 8, 9});

        party.setMyCustomPayoutPsbt(localPsbt);
        party.setMyCustomCloseTradeResponse(closeResponse);

        MuSigTradeParty restored = MuSigTradeParty.fromProto(party.toProto(false));

        assertThat(restored.getCustomPayoutData()).isPresent();
        MuSigCustomPayoutPartyData restoredData = restored.getCustomPayoutData().orElseThrow();
        assertThat(restoredData.isLocalPartyData()).isTrue();
        assertThat(restoredData.getMyCustomPayoutPsbt()).contains(localPsbt);
        assertThat(restoredData.getPeersCustomPayoutPsbt()).isEmpty();
        assertThat(restoredData.getMyCustomCloseTradeResponse()).contains(closeResponse);
        assertThat(restored.isMediationResultRejected()).isFalse();
    }

    @Test
    void peerPartyDataRoundTripsThroughTradeParty() {
        MuSigTradeParty party = new MuSigTradeParty(createNetworkId());
        PeerCustomPayoutPsbt peerPsbt = createPeerPsbt();

        party.setPeersCustomPayoutPsbt(peerPsbt);

        MuSigTradeParty restored = MuSigTradeParty.fromProto(party.toProto(false));

        assertThat(restored.getCustomPayoutData()).isPresent();
        MuSigCustomPayoutPartyData restoredData = restored.getCustomPayoutData().orElseThrow();
        assertThat(restoredData.isLocalPartyData()).isFalse();
        assertThat(restoredData.getMyCustomPayoutPsbt()).isEmpty();
        assertThat(restoredData.getPeersCustomPayoutPsbt()).contains(peerPsbt);
        assertThat(restoredData.getMyCustomCloseTradeResponse()).isEmpty();
    }

    @Test
    void peerPsbtDefensivelyCopiesByteArray() {
        byte[] psbt = PEER_PSBT.clone();
        PeerCustomPayoutPsbt peerPsbt = new PeerCustomPayoutPsbt(TX_ID, psbt);

        psbt[0] = 99;
        byte[] returnedPsbt = peerPsbt.getPsbt();
        returnedPsbt[1] = 99;

        assertThat(peerPsbt.getPsbt()).containsExactly(4, 5, 6);
    }

    @Test
    void malformedPeerPsbtDataIsRejected() {
        assertThatThrownBy(() -> new PeerCustomPayoutPsbt(
                "gg".repeat(32),
                PEER_PSBT))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PeerCustomPayoutPsbt(
                TX_ID,
                new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void peerPsbtAcceptsSizeLimitAndRejectsLargerPsbt() {
        assertThat(new PeerCustomPayoutPsbt(TX_ID, new byte[4_096]).getPsbt())
                .hasSize(4_096);
        assertThatThrownBy(() -> new PeerCustomPayoutPsbt(TX_ID, new byte[4_097]))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void peerPsbtSizeLimitIsEnforcedDuringDeserialization() {
        bisq.trade.protobuf.PeerCustomPayoutPsbt proto = bisq.trade.protobuf.PeerCustomPayoutPsbt.newBuilder()
                .setTxId(TX_ID)
                .setPsbt(ByteString.copyFrom(new byte[4_096]))
                .build();

        assertThat(PeerCustomPayoutPsbt.fromProto(proto).getPsbt()).hasSize(4_096);

        bisq.trade.protobuf.PeerCustomPayoutPsbt oversized = proto.toBuilder()
                .setPsbt(ByteString.copyFrom(new byte[4_097]))
                .build();

        assertThatThrownBy(() -> PeerCustomPayoutPsbt.fromProto(oversized))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void malformedPartyDataIsRejectedDuringDeserialization() {
        bisq.trade.protobuf.MuSigCustomPayoutPartyData empty =
                bisq.trade.protobuf.MuSigCustomPayoutPartyData.getDefaultInstance();
        bisq.trade.protobuf.MuSigCustomPayoutPartyData incompleteLocal =
                bisq.trade.protobuf.MuSigCustomPayoutPartyData.newBuilder()
                        .setMyCustomCloseTradeResponse(new CustomCloseTradeResponse(new byte[]{7}).toProto(false))
                        .build();
        bisq.trade.protobuf.MuSigCustomPayoutPartyData mixedOwnership =
                bisq.trade.protobuf.MuSigCustomPayoutPartyData.newBuilder()
                        .setMyCustomPayoutPsbt(createLocalPsbt().toProto(false))
                        .setPeersCustomPayoutPsbt(createPeerPsbt().toProto(false))
                        .build();

        assertThatThrownBy(() -> MuSigCustomPayoutPartyData.fromProto(empty))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MuSigCustomPayoutPartyData.fromProto(incompleteLocal))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> MuSigCustomPayoutPartyData.fromProto(mixedOwnership))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectionRoundTripsThroughTradeParty() {
        MuSigTradeParty rejectingParty = new MuSigTradeParty(createNetworkId());

        rejectingParty.setMediationResultRejected();
        assertThat(rejectingParty.getCustomPayoutData()).isEmpty();

        MuSigTradeParty restoredRejectingParty = MuSigTradeParty.fromProto(rejectingParty.toProto(false));
        assertThat(restoredRejectingParty.isMediationResultRejected()).isTrue();
        assertThat(restoredRejectingParty.getCustomPayoutData()).isEmpty();
    }

    @Test
    void rejectionObservableReportsInitialAndStoredState() {
        MuSigTradeParty party = new MuSigTradeParty(createNetworkId());
        List<Boolean> observedValues = new ArrayList<>();
        var pin = party.mediationResultRejectedObservable().addObserver(observedValues::add);

        try {
            party.setMediationResultRejected();
            party.setMediationResultRejected();

            assertThat(observedValues).containsExactly(false, true);
        } finally {
            pin.unbind();
        }

        MuSigTradeParty restored = MuSigTradeParty.fromProto(party.toProto(false));
        assertThat(restored.mediationResultRejectedObservable().get()).isTrue();
    }

    @Test
    void closeResponseRequiresLocalCustomPayoutData() {
        CustomCloseTradeResponse closeResponse = new CustomCloseTradeResponse(new byte[]{7});
        MuSigTradeParty emptyParty = new MuSigTradeParty(createNetworkId());
        MuSigTradeParty peerParty = new MuSigTradeParty(createNetworkId());
        peerParty.setPeersCustomPayoutPsbt(createPeerPsbt());

        assertThatThrownBy(() -> emptyParty.setMyCustomCloseTradeResponse(closeResponse))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> peerParty.setMyCustomCloseTradeResponse(closeResponse))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(emptyParty.getCustomPayoutData()).isEmpty();
        assertThat(peerParty.getCustomPayoutData())
                .flatMap(MuSigCustomPayoutPartyData::getMyCustomCloseTradeResponse)
                .isEmpty();
    }

    @Test
    void malformedPartyDecisionIsRejectedDuringDeserialization() {
        MuSigCustomPayoutPartyData customPayoutData =
                MuSigCustomPayoutPartyData.forLocalParty(createLocalPsbt());
        bisq.trade.protobuf.TradeParty proto = bisq.trade.protobuf.TradeParty.newBuilder()
                .setNetworkId(createNetworkId().toProto(false))
                .setMuSigTradeParty(bisq.trade.protobuf.MuSigTradeParty.newBuilder()
                        .setCustomPayoutData(customPayoutData.toProto(false))
                        .setMediationResultRejected(true))
                .build();

        assertThatThrownBy(() -> MuSigTradeParty.fromProto(proto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static CustomPayoutPsbt createLocalPsbt() {
        return new CustomPayoutPsbt(LOCAL_PSBT, TX_ID, 60_000, 40_000);
    }

    private static PeerCustomPayoutPsbt createPeerPsbt() {
        return new PeerCustomPayoutPsbt(
                TX_ID,
                PEER_PSBT);
    }

    private static NetworkId createNetworkId() {
        AddressByTransportTypeMap addresses = new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, new ClearnetAddress("127.0.0.1", 9999)));
        PubKey pubKey = new PubKey(KeyGeneration.generateDefaultEcKeyPair().getPublic(), "custom-payout-test-key");
        return new NetworkId(addresses, pubKey);
    }
}
