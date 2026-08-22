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

import bisq.common.encoding.Hex;
import bisq.common.proto.NetworkProto;
import bisq.common.validation.NetworkDataValidation;
import bisq.user.reputation.requests.AuthorizeSignedWitnessRequest;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Base64;

@Getter
@EqualsAndHashCode
public final class SignedWitnessOwnershipRequest implements NetworkProto {
    private final int protocolVersion;
    private final String profileId;
    private final byte[] witnessHash;
    private final byte[] accountInputDataWithSalt;
    private final byte[] ownerPublicKey;
    private final byte[] signature;

    public SignedWitnessOwnershipRequest(AuthorizeSignedWitnessRequest request) {
        protocolVersion = request.getProtocolVersion();
        profileId = request.getProfileId();
        witnessHash = Hex.decode(request.getHashAsHex());
        accountInputDataWithSalt = request.getAccountInputDataWithSalt();
        ownerPublicKey = Base64.getDecoder().decode(request.getPubKeyBase64());
        signature = Base64.getDecoder().decode(request.getSignatureBase64());

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateProfileId(profileId);
        NetworkDataValidation.validateHash(witnessHash);
        NetworkDataValidation.validateByteArray(accountInputDataWithSalt,
                1,
                AuthorizeSignedWitnessRequest.MAX_ACCOUNT_INPUT_LENGTH);
        NetworkDataValidation.validateByteArray(ownerPublicKey, 300, 600);
        NetworkDataValidation.validateByteArray(signature, 30, 60);
    }

    @Override
    public bisq.bridge.protobuf.SignedWitnessOwnershipRequest.Builder getBuilder(boolean serializeForHash) {
        return bisq.bridge.protobuf.SignedWitnessOwnershipRequest.newBuilder()
                .setProtocolVersion(protocolVersion)
                .setProfileId(profileId)
                .setWitnessHash(ByteString.copyFrom(witnessHash))
                .setAccountInputDataWithSalt(ByteString.copyFrom(accountInputDataWithSalt))
                .setOwnerPublicKey(ByteString.copyFrom(ownerPublicKey))
                .setSignature(ByteString.copyFrom(signature));
    }

    @Override
    public bisq.bridge.protobuf.SignedWitnessOwnershipRequest toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    @Override
    public bisq.bridge.protobuf.SignedWitnessOwnershipRequest completeProto() {
        return toProto(false);
    }
}
