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

package bisq.user.reputation.requests;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.message.ExternalNetworkMessage;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Objects;

import static bisq.network.p2p.services.data.storage.MetaData.MAX_MAP_SIZE_100;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_10_DAYS;

@Slf4j
@Getter
@ToString
public final class AuthorizeSignedWitnessRequest implements MailboxMessage, ExternalNetworkMessage {
    public static final int LEGACY_VERSION = 1;
    public static final int CURRENT_VERSION = 2;
    public static final int MAX_ACCOUNT_INPUT_LENGTH = 4096;
    private transient final MetaData metaData = new MetaData(TTL_10_DAYS, getClass().getSimpleName(), MAX_MAP_SIZE_100);
    private final String profileId;
    private final String hashAsHex;
    private final long accountAgeWitnessDate;
    private final long witnessSignDate;
    private final String pubKeyBase64;
    private final String signatureBase64;
    private final int protocolVersion;
    @ToString.Exclude
    private final byte[] accountInputDataWithSalt;

    public AuthorizeSignedWitnessRequest(String profileId,
                                         String hashAsHex,
                                         byte[] accountInputDataWithSalt,
                                         String pubKeyBase64,
                                         String signatureBase64) {
        this(CURRENT_VERSION,
                profileId,
                hashAsHex,
                0,
                0,
                accountInputDataWithSalt,
                pubKeyBase64,
                signatureBase64);
    }

    private AuthorizeSignedWitnessRequest(int protocolVersion,
                                          String profileId,
                                          String hashAsHex,
                                          long accountAgeWitnessDate,
                                          long witnessSignDate,
                                          byte[] accountInputDataWithSalt,
                                          String pubKeyBase64,
                                          String signatureBase64) {
        this.protocolVersion = protocolVersion;
        this.profileId = profileId;
        this.hashAsHex = hashAsHex;
        this.accountAgeWitnessDate = accountAgeWitnessDate;
        this.witnessSignDate = witnessSignDate;
        this.accountInputDataWithSalt = accountInputDataWithSalt.clone();
        this.pubKeyBase64 = pubKeyBase64;
        this.signatureBase64 = signatureBase64;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateProfileId(profileId);
        NetworkDataValidation.validateHashAsHex(hashAsHex);
        if (accountAgeWitnessDate != 0) {
            NetworkDataValidation.validateDate(accountAgeWitnessDate);
        }
        if (witnessSignDate != 0) {
            NetworkDataValidation.validateDate(witnessSignDate);
        }
        NetworkDataValidation.validatePubKeyBase64(pubKeyBase64);
        NetworkDataValidation.validateSignatureBase64(signatureBase64);
        NetworkDataValidation.validateByteArray(accountInputDataWithSalt, MAX_ACCOUNT_INPUT_LENGTH);
    }

    @Override
    public bisq.user.protobuf.AuthorizeSignedWitnessRequest.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.user.protobuf.AuthorizeSignedWitnessRequest.newBuilder()
                .setProfileId(profileId)
                .setHashAsHex(hashAsHex)
                .setAccountAgeWitnessDate(accountAgeWitnessDate)
                .setWitnessSignDate(witnessSignDate)
                .setPubKeyBase64(pubKeyBase64)
                .setSignatureBase64(signatureBase64)
                .setProtocolVersion(protocolVersion)
                .setAccountInputDataWithSalt(ByteString.copyFrom(accountInputDataWithSalt));
    }

    public static AuthorizeSignedWitnessRequest fromProto(bisq.user.protobuf.AuthorizeSignedWitnessRequest proto) {
        return new AuthorizeSignedWitnessRequest(proto.hasProtocolVersion()
                ? proto.getProtocolVersion()
                : LEGACY_VERSION,
                proto.getProfileId(),
                proto.getHashAsHex(),
                proto.getAccountAgeWitnessDate(),
                proto.getWitnessSignDate(),
                proto.getAccountInputDataWithSalt().toByteArray(),
                proto.getPubKeyBase64(),
                proto.getSignatureBase64());
    }

    @Override
    public bisq.user.protobuf.AuthorizeSignedWitnessRequest toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    public static ProtoResolver<ExternalNetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.user.protobuf.AuthorizeSignedWitnessRequest proto = any.unpack(bisq.user.protobuf.AuthorizeSignedWitnessRequest.class);
                return AuthorizeSignedWitnessRequest.fromProto(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.5, 1);
    }

    public byte[] getAccountInputDataWithSalt() {
        return accountInputDataWithSalt.clone();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AuthorizeSignedWitnessRequest that)) {
            return false;
        }
        return accountAgeWitnessDate == that.accountAgeWitnessDate &&
                witnessSignDate == that.witnessSignDate &&
                protocolVersion == that.protocolVersion &&
                Objects.equals(profileId, that.profileId) &&
                Objects.equals(hashAsHex, that.hashAsHex) &&
                Objects.equals(pubKeyBase64, that.pubKeyBase64) &&
                Objects.equals(signatureBase64, that.signatureBase64) &&
                Arrays.equals(accountInputDataWithSalt, that.accountInputDataWithSalt);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(profileId,
                hashAsHex,
                accountAgeWitnessDate,
                witnessSignDate,
                pubKeyBase64,
                signatureBase64,
                protocolVersion);
        return 31 * result + Arrays.hashCode(accountInputDataWithSalt);
    }
}
