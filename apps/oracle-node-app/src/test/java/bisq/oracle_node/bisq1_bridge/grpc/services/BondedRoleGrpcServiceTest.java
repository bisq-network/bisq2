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

package bisq.oracle_node.bisq1_bridge.grpc.services;

import bisq.bonded_roles.registration.BondedRoleRegistrationProtocol;
import bisq.oracle_node.bisq1_bridge.grpc.messages.BondedRoleVerificationRequest;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.Test;

import static bisq.oracle_node.TestBondedRoleRegistrations.LOCKUP_TX_ID;
import static bisq.oracle_node.TestBondedRoleRegistrations.PROPOSAL_TX_ID;
import static bisq.oracle_node.TestBondedRoleRegistrations.createCurrentRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BondedRoleGrpcServiceTest {
    @Test
    void mapsAllBindingFieldsAndTheBisqOneRoleName() {
        BondedRoleVerificationRequest request = BondedRoleGrpcService.toVerificationRequest(createCurrentRequest());

        assertThat(request.getBondUserName()).isEqualTo("bond-user");
        assertThat(request.getRoleType()).isEqualTo("MEDIATOR");
        assertThat(request.getProtocolVersion()).isEqualTo(BondedRoleRegistrationProtocol.CURRENT_VERSION);
        assertThat(request.getProposalTxId()).isEqualTo(PROPOSAL_TX_ID);
        assertThat(request.getLockupTxId()).isEqualTo(LOCKUP_TX_ID);
    }

    @Test
    void senderWhoDoesNotOwnTheProfileIdIsRejectedBeforeAnyBridgeCall() {
        // The null gRPC client guarantees that a bypassed ownership check would fail differently.
        BondedRoleGrpcService service = new BondedRoleGrpcService(null);
        var senderKeyPair = KeyGeneration.generateDefaultEcKeyPair();

        assertThatThrownBy(() -> service.requestBondedRoleVerification(createCurrentRequest(), senderKeyPair.getPublic()))
                .isInstanceOf(RuntimeException.class)
                .cause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not matching the profile ID");
    }
}
