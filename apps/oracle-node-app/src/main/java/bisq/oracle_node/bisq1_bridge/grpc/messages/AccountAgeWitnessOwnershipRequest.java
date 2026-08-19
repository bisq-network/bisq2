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
import bisq.user.reputation.requests.AuthorizeAccountAgeRequest;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Base64;

@Getter
@EqualsAndHashCode
public final class AccountAgeWitnessOwnershipRequest implements NetworkProto {
    private final int protocolVersion;
    private final String profileId;
    private final byte[] witnessHash;
    private final byte[] accountInputDataWithSalt;
    private final byte[] ownerPublicKey;
    private final byte[] signature;

    public AccountAgeWitnessOwnershipRequest(AuthorizeAccountAgeRequest request) {
        this.protocolVersion = request.getProtocolVersion();
        this.profileId = request.getProfileId();
        this.witnessHash = Hex.decode(request.getHashAsHex());
        this.accountInputDataWithSalt = request.getAccountInputDataWithSalt();
        this.ownerPublicKey = Base64.getDecoder().decode(request.getPubKeyBase64());
        this.signature = Base64.getDecoder().decode(request.getSignatureBase64());

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateProfileId(profileId);
        NetworkDataValidation.validateHash(witnessHash);
        NetworkDataValidation.validateByteArray(accountInputDataWithSalt,
                1,
                AuthorizeAccountAgeRequest.MAX_ACCOUNT_INPUT_LENGTH);
        NetworkDataValidation.validateByteArray(ownerPublicKey, 300, 600);
        NetworkDataValidation.validateByteArray(signature, 30, 60);
    }

    @Override
    public bisq.bridge.protobuf.AccountAgeWitnessOwnershipRequest.Builder getBuilder(boolean serializeForHash) {
        return bisq.bridge.protobuf.AccountAgeWitnessOwnershipRequest.newBuilder()
                .setProtocolVersion(protocolVersion)
                .setProfileId(profileId)
                .setWitnessHash(ByteString.copyFrom(witnessHash))
                .setAccountInputDataWithSalt(ByteString.copyFrom(accountInputDataWithSalt))
                .setOwnerPublicKey(ByteString.copyFrom(ownerPublicKey))
                .setSignature(ByteString.copyFrom(signature));
    }

    @Override
    public bisq.bridge.protobuf.AccountAgeWitnessOwnershipRequest toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    @Override
    public bisq.bridge.protobuf.AccountAgeWitnessOwnershipRequest completeProto() {
        return toProto(false);
    }
}
