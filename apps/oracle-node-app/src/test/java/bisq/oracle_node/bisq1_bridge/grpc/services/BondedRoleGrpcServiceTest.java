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
import bisq.bonded_roles.registration.BondedRoleRegistrationRequest;
import bisq.bridge.protobuf.BondedRoleGrpcServiceGrpc;
import bisq.oracle_node.bisq1_bridge.grpc.GrpcClient;
import bisq.oracle_node.bisq1_bridge.grpc.messages.BondedRoleVerificationRequest;
import bisq.oracle_node.bisq1_bridge.grpc.messages.BondedRoleVerificationResponse;
import bisq.security.keys.KeyGeneration;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import java.security.KeyPair;

import static bisq.oracle_node.TestBondedRoleRegistrations.LOCKUP_TX_ID;
import static bisq.oracle_node.TestBondedRoleRegistrations.PROPOSAL_TX_ID;
import static bisq.oracle_node.TestBondedRoleRegistrations.createCurrentRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    // The role data contains the network ID of the request. Released clients send their network ID with the key of
    // their profile (finding D1, see docs/specifications/bonded-roles/registration.md).
    @Test
    void senderWhoDoesNotOwnTheNetworkIdIsRejectedBeforeAnyBridgeCall() {
        // The null gRPC client guarantees that a bypassed ownership check would fail differently.
        BondedRoleGrpcService service = new BondedRoleGrpcService(null);
        KeyPair senderKeyPair = KeyGeneration.generateDefaultEcKeyPair();
        BondedRoleRegistrationRequest request = createCurrentRequest(senderKeyPair,
                KeyGeneration.generateDefaultEcKeyPair());

        assertThatThrownBy(() -> service.requestBondedRoleVerification(request, senderKeyPair.getPublic()))
                .isInstanceOf(RuntimeException.class)
                .cause()
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not matching the key of the network ID");
    }

    @Test
    void senderWhoOwnsTheProfileAndTheNetworkIdIsVerifiedByTheBridge() {
        GrpcClient grpcClient = mock(GrpcClient.class);
        BondedRoleGrpcServiceGrpc.BondedRoleGrpcServiceBlockingStub blockingStub =
                mock(BondedRoleGrpcServiceGrpc.BondedRoleGrpcServiceBlockingStub.class, Answers.RETURNS_SELF);
        when(grpcClient.getBondedRoleBlockingStub()).thenReturn(blockingStub);
        when(blockingStub.requestBondedRoleVerification(any()))
                .thenReturn(bisq.bridge.protobuf.BondedRoleVerificationResponse.getDefaultInstance());
        BondedRoleGrpcService service = new BondedRoleGrpcService(grpcClient);
        KeyPair senderKeyPair = KeyGeneration.generateDefaultEcKeyPair();

        BondedRoleVerificationResponse response = service.requestBondedRoleVerification(
                createCurrentRequest(senderKeyPair, senderKeyPair), senderKeyPair.getPublic());

        assertThat(response.getErrorMessage()).isEmpty();
        verify(blockingStub).requestBondedRoleVerification(any());
    }
}
