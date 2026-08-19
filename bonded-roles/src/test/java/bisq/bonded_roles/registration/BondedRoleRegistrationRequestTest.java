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
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.bonded_roles.registration;

import bisq.bonded_roles.BondedRoleType;
import bisq.common.network.AddressByTransportTypeMap;
import bisq.common.network.TransportType;
import bisq.common.network.clear_net_address_types.LocalHostAddressTypeFacade;
import bisq.network.identity.NetworkId;
import bisq.security.keys.KeyGeneration;
import bisq.security.keys.PubKey;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

public class BondedRoleRegistrationRequestTest {
    private static final String PROFILE_ID = "0123456789abcdef0123456789abcdef01234567";
    private static final String PROPOSAL_TX_ID = "a".repeat(64);
    private static final String LOCKUP_TX_ID = "b".repeat(64);

    @Test
    void missingProtocolVersionIsReadAsLegacyVersionOne() {
        bisq.bonded_roles.protobuf.BondedRoleRegistrationRequest legacyProto =
                bisq.bonded_roles.protobuf.BondedRoleRegistrationRequest.newBuilder()
                        .setProfileId(PROFILE_ID)
                        .setAuthorizedPublicKey("public-key")
                        .setBondedRoleType(BondedRoleType.MEDIATOR.toProtoEnum())
                        .setBondUserName("bond-user")
                        .setSignatureBase64("signature")
                        .setNetworkId(createNetworkId().toProto(false))
                        .build();
        assertThat(legacyProto.hasRegistrationProtocolVersion()).isFalse();

        BondedRoleRegistrationRequest request = BondedRoleRegistrationRequest.fromProto(legacyProto);

        assertThat(request.getRegistrationProtocolVersion()).isEqualTo(BondedRoleRegistrationProtocol.LEGACY_VERSION);
        assertThat(request.getProposalTxId()).isEmpty();
        assertThat(request.getLockupTxId()).isEmpty();
    }

    @Test
    void explicitVersionZeroRemainsUnsupported() {
        bisq.bonded_roles.protobuf.BondedRoleRegistrationRequest proto =
                createRequest(false, BondedRoleRegistrationProtocol.LEGACY_VERSION, "", "")
                        .toValueProto(false)
                        .toBuilder()
                        .setRegistrationProtocolVersion(0)
                        .build();
        assertThat(proto.hasRegistrationProtocolVersion()).isTrue();

        BondedRoleRegistrationRequest request = BondedRoleRegistrationRequest.fromProto(proto);

        assertThat(request.getRegistrationProtocolVersion()).isZero();
        assertThat(BondedRoleRegistrationProtocol.isSupported(request.getRegistrationProtocolVersion())).isFalse();
    }

    @Test
    void versionTwoRequestRoundTripsItsTransactionBinding() {
        BondedRoleRegistrationRequest request = createRequest(false,
                BondedRoleRegistrationProtocol.CURRENT_VERSION,
                PROPOSAL_TX_ID,
                LOCKUP_TX_ID);

        BondedRoleRegistrationRequest roundTrip = BondedRoleRegistrationRequest.fromProto(request.toValueProto(false));

        assertThat(roundTrip).isEqualTo(request);
        assertThat(roundTrip.isCancellationRequest()).isFalse();
        assertThat(roundTrip.getProposalTxId()).isEqualTo(PROPOSAL_TX_ID);
        assertThat(roundTrip.getLockupTxId()).isEqualTo(LOCKUP_TX_ID);
    }

    @Test
    void unsupportedProtocolRemainsResolvableAtTheMessageBoundary() {
        BondedRoleRegistrationRequest request = createRequest(false, 3, PROPOSAL_TX_ID, LOCKUP_TX_ID);

        assertThat(BondedRoleRegistrationRequest.fromProto(request.toValueProto(false))).isEqualTo(request);
        assertThat(BondedRoleRegistrationProtocol.isSupported(request.getRegistrationProtocolVersion())).isFalse();
    }

    @Test
    void messageBoundaryRejectsNonHexTransactionIds() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                createRequest(false, 3, "z".repeat(64), LOCKUP_TX_ID));
    }

    @Test
    void domainProtocolRejectsUnsupportedPartialAndNonHexProofs() {
        assertThatIllegalArgumentException().isThrownBy(() ->
                BondedRoleRegistrationProtocol.verifyProof(3, PROPOSAL_TX_ID, LOCKUP_TX_ID));
        assertThatIllegalArgumentException().isThrownBy(() ->
                BondedRoleRegistrationProtocol.verifyProof(1, PROPOSAL_TX_ID, ""));
        assertThatIllegalArgumentException().isThrownBy(() ->
                BondedRoleRegistrationProtocol.verifyProof(2, PROPOSAL_TX_ID, ""));
        assertThatIllegalArgumentException().isThrownBy(() ->
                BondedRoleRegistrationProtocol.verifyProof(2, "z".repeat(64), LOCKUP_TX_ID));
    }

    private static BondedRoleRegistrationRequest createRequest(boolean cancellation,
                                                               int protocolVersion,
                                                               String proposalTxId,
                                                               String lockupTxId) {
        return new BondedRoleRegistrationRequest(PROFILE_ID,
                "public-key",
                BondedRoleType.MEDIATOR,
                "bond-user",
                "signature",
                Optional.empty(),
                createNetworkId(),
                cancellation,
                protocolVersion,
                proposalTxId,
                lockupTxId);
    }

    public static NetworkId createNetworkId() {
        var keyPair = KeyGeneration.generateDefaultEcKeyPair();
        var addressMap = new AddressByTransportTypeMap(Map.of(
                TransportType.CLEAR, LocalHostAddressTypeFacade.toLocalHostAddress(1234)));
        return new NetworkId(addressMap, new PubKey(keyPair.getPublic(), "test-key"));
    }
}
