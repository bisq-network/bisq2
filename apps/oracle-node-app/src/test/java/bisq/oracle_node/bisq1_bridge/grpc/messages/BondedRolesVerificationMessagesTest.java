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

package bisq.oracle_node.bisq1_bridge.grpc.messages;

import bisq.bonded_roles.registration.BondedRoleRegistrationProtocol;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static bisq.oracle_node.TestBondedRoleRegistrations.LOCKUP_TX_ID;
import static bisq.oracle_node.TestBondedRoleRegistrations.PROPOSAL_TX_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class BondedRolesVerificationMessagesTest {
    @Test
    void missingProtocolVersionIsReadAsLegacyVersionOne() {
        bisq.bridge.protobuf.BondedRoleVerificationRequest proto =
                bisq.bridge.protobuf.BondedRoleVerificationRequest.newBuilder()
                        .setBondUserName("legacy")
                        .setRoleType("MEDIATOR")
                        .setProfileId("0123456789abcdef0123456789abcdef01234567")
                        .setSignatureBase64("signature")
                        .build();
        assertThat(proto.hasProtocolVersion()).isFalse();

        BondedRoleVerificationRequest request = BondedRoleVerificationRequest.fromProto(proto);

        assertThat(request.getProtocolVersion()).isEqualTo(BondedRoleRegistrationProtocol.LEGACY_VERSION);
    }

    @Test
    void explicitVersionZeroRemainsUnsupported() {
        bisq.bridge.protobuf.BondedRoleVerificationRequest proto = request("unsupported")
                .completeProto()
                .toBuilder()
                .setProtocolVersion(0)
                .build();
        assertThat(proto.hasProtocolVersion()).isTrue();

        BondedRoleVerificationRequest request = BondedRoleVerificationRequest.fromProto(proto);

        assertThat(request.getProtocolVersion()).isZero();
        assertThat(BondedRoleRegistrationProtocol.isSupported(request.getProtocolVersion())).isFalse();
    }

    @Test
    void batchMessagesRoundTripInRequestOrder() {
        BondedRoleVerificationRequest first = request("first");
        BondedRoleVerificationRequest second = request("second");
        BondedRolesVerificationRequest request = new BondedRolesVerificationRequest(List.of(first, second));

        BondedRolesVerificationResponse response = new BondedRolesVerificationResponse(941001,
                List.of(new BondedRoleVerificationResponse(Optional.empty()),
                        new BondedRoleVerificationResponse(Optional.of("confiscated"))));

        assertThat(BondedRolesVerificationRequest.fromProto(request.completeProto()).getRegistrations())
                .containsExactly(first, second);
        assertThat(BondedRolesVerificationResponse.fromProto(response.toProto(false))).isEqualTo(response);
    }

    @Test
    void verificationMessageAllowsAnUnsupportedFutureProtocolAtTheStructuralBoundary() {
        BondedRoleVerificationRequest request = new BondedRoleVerificationRequest("future",
                "MEDIATOR",
                "0123456789abcdef0123456789abcdef01234567",
                "signature",
                PROPOSAL_TX_ID,
                LOCKUP_TX_ID,
                3);

        request.verify();
        assertThat(BondedRoleVerificationRequest.fromProto(request.completeProto())).isEqualTo(request);
    }

    @Test
    void verificationMessageRejectsNonHexTransactionIds() {
        assertThatIllegalArgumentException().isThrownBy(() -> new BondedRoleVerificationRequest("future",
                "MEDIATOR",
                "0123456789abcdef0123456789abcdef01234567",
                "signature",
                "z".repeat(64),
                LOCKUP_TX_ID,
                3));
    }

    private static BondedRoleVerificationRequest request(String bondUserName) {
        return new BondedRoleVerificationRequest(bondUserName,
                "MEDIATOR",
                "0123456789abcdef0123456789abcdef01234567",
                "signature",
                PROPOSAL_TX_ID,
                LOCKUP_TX_ID,
                BondedRoleRegistrationProtocol.CURRENT_VERSION);
    }
}
