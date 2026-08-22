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

import bisq.common.application.Service;
import bisq.oracle_node.bisq1_bridge.grpc.GrpcClient;
import bisq.oracle_node.bisq1_bridge.grpc.messages.SignedWitnessOwnershipRequest;
import bisq.oracle_node.bisq1_bridge.grpc.messages.SignedWitnessOwnershipResponse;
import bisq.user.reputation.requests.AuthorizeSignedWitnessRequest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SignedWitnessGrpcService implements Service {
    private final GrpcClient grpcClient;

    public SignedWitnessGrpcService(GrpcClient grpcClient) {
        this.grpcClient = grpcClient;
    }

    public SignedWitnessOwnershipResponse verifyAndRequestAuthorization(AuthorizeSignedWitnessRequest request) {
        log.info("verifyAndRequestAuthorization {}", request);
        try {
            var protoRequest = new SignedWitnessOwnershipRequest(request).completeProto();
            var protoResponse = GrpcClient.withInteractiveRequestDeadline(
                            grpcClient.getSignedWitnessBlockingStub())
                    .verifySignedWitnessOwnership(protoRequest);
            return SignedWitnessOwnershipResponse.fromProto(protoResponse);
        } catch (Exception e) {
            log.warn("Error at verifyAndRequestAuthorization", e);
            throw new RuntimeException(e);
        }
    }
}
