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

package bisq.oracle_node.bisq1_bridge.grpc.messages;

import bisq.common.proto.NetworkProto;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public final class BondedRoleVerificationRequest implements NetworkProto {
    private final String bondUserName;
    private final String roleType;
    private final String profileId;
    private final String signatureBase64;

    public BondedRoleVerificationRequest(String bondUserName,
                                         String roleType,
                                         String profileId,
                                         String signatureBase64) {
        this.bondUserName = bondUserName;
        this.roleType = roleType;
        this.profileId = profileId;
        this.signatureBase64 = signatureBase64;
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateText(bondUserName, 1, 200);
        NetworkDataValidation.validateText(roleType, 1, 200);
        NetworkDataValidation.validateProfileId(profileId);
        NetworkDataValidation.validateSignatureBase64(signatureBase64);
    }

    @Override
    public bisq.bridge.protobuf.BondedRoleVerificationRequest.Builder getBuilder(boolean serializeForHash) {
        return bisq.bridge.protobuf.BondedRoleVerificationRequest.newBuilder()
                .setBondUserName(bondUserName)
                .setRoleType(roleType)
                .setProfileId(profileId)
                .setSignatureBase64(signatureBase64);
    }

    @Override
    public bisq.bridge.protobuf.BondedRoleVerificationRequest toProto(boolean serializeForHash) {
        return getBuilder(serializeForHash).build();
    }

    public static BondedRoleVerificationRequest fromProto(bisq.bridge.protobuf.BondedRoleVerificationRequest proto) {
        return new BondedRoleVerificationRequest(proto.getBondUserName(),
                proto.getRoleType(),
                proto.getProfileId(),
                proto.getSignatureBase64()
        );
    }
}
