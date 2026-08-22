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

import bisq.bonded_roles.registration.BondedRoleRegistrationProtocol;
import bisq.common.proto.NetworkProto;
import bisq.common.validation.BitcoinTransactionValidation;
import bisq.common.validation.NetworkDataValidation;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import static com.google.common.base.Preconditions.checkArgument;

@Getter
@EqualsAndHashCode
public final class BondedRoleVerificationRequest implements NetworkProto {
    private final String bondUserName;
    private final String roleType;
    private final String profileId;
    private final String signatureBase64;
    private final String proposalTxId;
    private final String lockupTxId;
    private final int protocolVersion;

    public BondedRoleVerificationRequest(String bondUserName,
                                         String roleType,
                                         String profileId,
                                         String signatureBase64,
                                         String proposalTxId,
                                         String lockupTxId,
                                         int protocolVersion) {
        this.bondUserName = bondUserName;
        this.roleType = roleType;
        this.profileId = profileId;
        this.signatureBase64 = signatureBase64;
        this.proposalTxId = proposalTxId;
        this.lockupTxId = lockupTxId;
        this.protocolVersion = protocolVersion;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateText(bondUserName, 1, 200);
        NetworkDataValidation.validateText(roleType, 1, 200);
        NetworkDataValidation.validateProfileId(profileId);
        NetworkDataValidation.validateSignatureBase64(signatureBase64);
        checkArgument(protocolVersion >= 0,
                "Bonded-role registration protocol version must not be negative");
        checkArgument(proposalTxId.isEmpty() || BitcoinTransactionValidation.isValid(proposalTxId),
                "Proposal tx ID must be empty or a valid Bitcoin transaction ID");
        checkArgument(lockupTxId.isEmpty() || BitcoinTransactionValidation.isValid(lockupTxId),
                "Lockup tx ID must be empty or a valid Bitcoin transaction ID");
    }

    @Override
    public bisq.bridge.protobuf.BondedRoleVerificationRequest.Builder getBuilder(boolean serializeForHash) {
        return bisq.bridge.protobuf.BondedRoleVerificationRequest.newBuilder()
                .setBondUserName(bondUserName)
                .setRoleType(roleType)
                .setProfileId(profileId)
                .setSignatureBase64(signatureBase64)
                .setProposalTxId(proposalTxId)
                .setLockupTxId(lockupTxId)
                .setProtocolVersion(protocolVersion);
    }

    @Override
    public bisq.bridge.protobuf.BondedRoleVerificationRequest completeProto() {
        return toProto(false);
    }

    @Override
    public bisq.bridge.protobuf.BondedRoleVerificationRequest toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static BondedRoleVerificationRequest fromProto(bisq.bridge.protobuf.BondedRoleVerificationRequest proto) {
        return new BondedRoleVerificationRequest(proto.getBondUserName(),
                proto.getRoleType(),
                proto.getProfileId(),
                proto.getSignatureBase64(),
                proto.getProposalTxId(),
                proto.getLockupTxId(),
                BondedRoleRegistrationProtocol.versionFromProto(proto.hasProtocolVersion(), proto.getProtocolVersion())
        );
    }
}
