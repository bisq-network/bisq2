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

package bisq.oracle_node.bisq1_bridge.grpc.services;

import bisq.bonded_roles.BondedRoleType;
import bisq.bonded_roles.registration.BondedRoleRegistrationRequest;
import bisq.common.application.Service;
import bisq.common.encoding.Hex;
import bisq.oracle_node.bisq1_bridge.grpc.GrpcClient;
import bisq.oracle_node.bisq1_bridge.grpc.messages.BondedRoleVerificationRequest;
import bisq.oracle_node.bisq1_bridge.grpc.messages.BondedRoleVerificationResponse;
import bisq.security.DigestUtil;
import lombok.extern.slf4j.Slf4j;

import java.security.PublicKey;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class BondedRoleGrpcService implements Service {
    private final GrpcClient grpcClient;

    public BondedRoleGrpcService(GrpcClient grpcClient) {
        this.grpcClient = grpcClient;
    }

    public BondedRoleVerificationResponse requestBondedRoleVerification(BondedRoleRegistrationRequest request,
                                                                        PublicKey senderPublicKey) {
        log.info("requestBondedRoleVerification {}", request);
        try {
            String profileId = request.getProfileId();

            // Verify if message sender is owner of the profileId
            String sendersProfileId = Hex.encode(DigestUtil.hash(senderPublicKey.getEncoded()));
            checkArgument(profileId.equals(sendersProfileId), "Senders pub key is not matching the profile ID");

            BondedRoleType bondedRoleType = request.getBondedRoleType();
            String bisq1RoleTypeName = toBisq1RoleTypeName(bondedRoleType);
            String bondUserName = request.getBondUserName();
            String signatureBase64 = request.getSignatureBase64();
            var protoRequest = new BondedRoleVerificationRequest(bondUserName, bisq1RoleTypeName, profileId, signatureBase64).toProto(true);
            var protoResponse = grpcClient.getBondedRoleGrpcService().requestBondedRoleVerification(protoRequest);
            return BondedRoleVerificationResponse.fromProto(protoResponse);
        } catch (Exception e) {
            log.warn("Error at requestBondedRoleVerification", e);
            throw new RuntimeException(e);
        }
    }

    private static String toBisq1RoleTypeName(BondedRoleType bondedRoleType) {
        String name = bondedRoleType.name();
        return switch (name) {
            case "MEDIATOR" -> "MEDIATOR"; // 5k
            case "MODERATOR" -> "YOUTUBE_ADMIN"; // 5k; repurpose unused role
            case "ARBITRATOR" -> "MOBILE_NOTIFICATIONS_RELAY_OPERATOR"; // 10k; Bisq 1 ARBITRATOR would require 100k!
            case "SECURITY_MANAGER" -> "BITCOINJ_MAINTAINER"; // 10k; repurpose unused role
            case "RELEASE_MANAGER" -> "FORUM_ADMIN"; // 10k; repurpose unused role
            case "ORACLE_NODE" -> "NETLAYER_MAINTAINER"; // 10k; repurpose unused role
            case "SEED_NODE" -> "SEED_NODE_OPERATOR"; // 10k
            case "EXPLORER_NODE" -> "BSQ_EXPLORER_OPERATOR"; // 10k; Explorer operator
            case "MARKET_PRICE_NODE" -> "DATA_RELAY_NODE_OPERATOR"; // 10k; price node
            default -> name;
        };
    }
}