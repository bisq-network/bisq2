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
import bisq.trade.mu_sig.messages.network.mu_sig_data.PeerCustomPayoutPsbt;
import org.junit.jupiter.api.Test;

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

        assertThat(party.setMyCustomPayoutPsbt(localPsbt)).isTrue();
        assertThat(party.setMyCustomCloseTradeResponse(closeResponse)).isTrue();

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

        assertThat(party.setPeersCustomPayoutPsbt(peerPsbt)).isTrue();

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
    void rejectionAndCustomPayoutDataAreMutuallyExclusive() {
        MuSigTradeParty rejectingParty = new MuSigTradeParty(createNetworkId());

        assertThat(rejectingParty.setMediationResultRejected()).isTrue();
        assertThat(rejectingParty.setMediationResultRejected()).isFalse();
        assertThat(rejectingParty.setMyCustomPayoutPsbt(createLocalPsbt())).isFalse();
        assertThat(rejectingParty.getCustomPayoutData()).isEmpty();

        MuSigTradeParty restoredRejectingParty = MuSigTradeParty.fromProto(rejectingParty.toProto(false));
        assertThat(restoredRejectingParty.isMediationResultRejected()).isTrue();
        assertThat(restoredRejectingParty.getCustomPayoutData()).isEmpty();

        MuSigTradeParty signingParty = new MuSigTradeParty(createNetworkId());

        assertThat(signingParty.setPeersCustomPayoutPsbt(createPeerPsbt())).isTrue();
        assertThat(signingParty.setMediationResultRejected()).isFalse();
        assertThat(signingParty.isMediationResultRejected()).isFalse();
    }

    @Test
    void conflictingDataDoesNotReplaceStoredData() {
        MuSigTradeParty party = new MuSigTradeParty(createNetworkId());
        CustomPayoutPsbt firstPsbt = createLocalPsbt();
        CustomPayoutPsbt conflictingPsbt = new CustomPayoutPsbt(new byte[]{9}, TX_ID, 70_000, 30_000);
        CustomCloseTradeResponse firstCloseResponse = new CustomCloseTradeResponse(new byte[]{7});
        CustomCloseTradeResponse conflictingCloseResponse = new CustomCloseTradeResponse(new byte[]{8});

        assertThat(party.setMyCustomPayoutPsbt(firstPsbt)).isTrue();
        assertThat(party.setMyCustomPayoutPsbt(firstPsbt)).isFalse();
        assertThat(party.setMyCustomPayoutPsbt(conflictingPsbt)).isFalse();
        assertThat(party.setMyCustomCloseTradeResponse(firstCloseResponse)).isTrue();
        assertThat(party.setMyCustomCloseTradeResponse(firstCloseResponse)).isFalse();
        assertThat(party.setMyCustomCloseTradeResponse(conflictingCloseResponse)).isFalse();

        MuSigCustomPayoutPartyData storedData = party.getCustomPayoutData().orElseThrow();
        assertThat(storedData.getMyCustomPayoutPsbt()).contains(firstPsbt);
        assertThat(storedData.getMyCustomCloseTradeResponse()).contains(firstCloseResponse);
    }

    @Test
    void peerCustomPayoutPsbtDoesNotReplaceStoredData() {
        MuSigTradeParty party = new MuSigTradeParty(createNetworkId());
        PeerCustomPayoutPsbt firstPsbt = createPeerPsbt();
        PeerCustomPayoutPsbt duplicatePsbt = createPeerPsbt();
        PeerCustomPayoutPsbt conflictingPsbt = new PeerCustomPayoutPsbt(TX_ID, new byte[]{7, 8, 9});

        assertThat(party.setPeersCustomPayoutPsbt(firstPsbt)).isTrue();
        assertThat(party.setPeersCustomPayoutPsbt(duplicatePsbt)).isFalse();
        assertThat(party.setPeersCustomPayoutPsbt(conflictingPsbt)).isFalse();

        MuSigCustomPayoutPartyData storedData = party.getCustomPayoutData().orElseThrow();
        assertThat(storedData.getPeersCustomPayoutPsbt()).contains(firstPsbt);
    }

    @Test
    void peerCustomPayoutPsbtCannotBeStoredAfterRejection() {
        MuSigTradeParty party = new MuSigTradeParty(createNetworkId());

        assertThat(party.setMediationResultRejected()).isTrue();
        assertThat(party.setPeersCustomPayoutPsbt(createPeerPsbt())).isFalse();
        assertThat(party.getCustomPayoutData()).isEmpty();
    }

    @Test
    void closeResponseRequiresLocalCustomPayoutData() {
        CustomCloseTradeResponse closeResponse = new CustomCloseTradeResponse(new byte[]{7});
        MuSigTradeParty emptyParty = new MuSigTradeParty(createNetworkId());
        MuSigTradeParty peerParty = new MuSigTradeParty(createNetworkId());
        assertThat(peerParty.setPeersCustomPayoutPsbt(createPeerPsbt())).isTrue();

        assertThat(emptyParty.setMyCustomCloseTradeResponse(closeResponse)).isFalse();
        assertThat(peerParty.setMyCustomCloseTradeResponse(closeResponse)).isFalse();
    }

    @Test
    void additiveFieldsPreserveExistingPartyData() {
        bisq.trade.protobuf.TradeParty proto = bisq.trade.protobuf.TradeParty.newBuilder()
                .setNetworkId(createNetworkId().toProto(false))
                .setMuSigTradeParty(bisq.trade.protobuf.MuSigTradeParty.newBuilder()
                        .setMediationResultAccepted(true))
                .build();

        MuSigTradeParty restored = MuSigTradeParty.fromProto(proto);

        assertThat(restored.getMediationResultAccepted()).contains(true);
        assertThat(restored.getCustomPayoutData()).isEmpty();
        assertThat(restored.isMediationResultRejected()).isFalse();
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
