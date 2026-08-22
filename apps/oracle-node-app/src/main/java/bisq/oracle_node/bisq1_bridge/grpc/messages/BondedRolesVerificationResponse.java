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

import bisq.common.proto.NetworkProto;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@EqualsAndHashCode
public final class BondedRolesVerificationResponse implements NetworkProto {
    private final int daoStateBlockHeight;
    private final List<BondedRoleVerificationResponse> verifications;

    public BondedRolesVerificationResponse(int daoStateBlockHeight,
                                           List<BondedRoleVerificationResponse> verifications) {
        this.daoStateBlockHeight = daoStateBlockHeight;
        this.verifications = List.copyOf(verifications);
        verify();
    }

    @Override
    public void verify() {
        checkArgument(daoStateBlockHeight >= 0, "DAO state block height must not be negative");
        checkArgument(verifications.size() <= BondedRolesVerificationRequest.MAX_REGISTRATIONS,
                "A bonded-role verification batch response must not exceed " +
                        BondedRolesVerificationRequest.MAX_REGISTRATIONS + " results");
        verifications.forEach(BondedRoleVerificationResponse::verify);
    }

    @Override
    public bisq.bridge.protobuf.BondedRolesVerificationResponse.Builder getBuilder(boolean serializeForHash) {
        return bisq.bridge.protobuf.BondedRolesVerificationResponse.newBuilder()
                .setDaoStateBlockHeight(daoStateBlockHeight)
                .addAllVerifications(verifications.stream()
                        .map(verification -> verification.toProto(serializeForHash))
                        .toList());
    }

    @Override
    public bisq.bridge.protobuf.BondedRolesVerificationResponse toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static BondedRolesVerificationResponse fromProto(bisq.bridge.protobuf.BondedRolesVerificationResponse proto) {
        return new BondedRolesVerificationResponse(proto.getDaoStateBlockHeight(),
                proto.getVerificationsList().stream()
                        .map(BondedRoleVerificationResponse::fromProto)
                        .toList());
    }
}
