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
public final class AuthorizeAccountAgeRequest implements MailboxMessage, ExternalNetworkMessage {
    public static final int LEGACY_VERSION = 1;
    public static final int CURRENT_VERSION = 2;
    public static final int MAX_ACCOUNT_INPUT_LENGTH = 4096;
    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_10_DAYS, getClass().getSimpleName(), MAX_MAP_SIZE_100);
    private final String profileId;
    private final String hashAsHex;
    private final long date;
    private final String pubKeyBase64;
    private final String signatureBase64;
    private final int protocolVersion;
    @ToString.Exclude
    private final byte[] accountInputDataWithSalt;

    public AuthorizeAccountAgeRequest(String profileId,
                                      String hashAsHex,
                                      byte[] accountInputDataWithSalt,
                                      String pubKeyBase64,
                                      String signatureBase64) {
        this(CURRENT_VERSION,
                profileId,
                hashAsHex,
                0,
                accountInputDataWithSalt,
                pubKeyBase64,
                signatureBase64);
    }

    private AuthorizeAccountAgeRequest(int protocolVersion,
                                       String profileId,
                                       String hashAsHex,
                                       long date,
                                       byte[] accountInputDataWithSalt,
                                       String pubKeyBase64,
                                       String signatureBase64) {
        this.protocolVersion = protocolVersion;
        this.profileId = profileId;
        this.hashAsHex = hashAsHex;
        this.date = date;
        this.accountInputDataWithSalt = accountInputDataWithSalt.clone();
        this.pubKeyBase64 = pubKeyBase64;
        this.signatureBase64 = signatureBase64;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateProfileId(profileId);
        NetworkDataValidation.validateHashAsHex(hashAsHex);
        if (protocolVersion >= CURRENT_VERSION) {
            NetworkDataValidation.validateByteArray(accountInputDataWithSalt, 1, MAX_ACCOUNT_INPUT_LENGTH);
        } else {
            NetworkDataValidation.validateByteArray(accountInputDataWithSalt, MAX_ACCOUNT_INPUT_LENGTH);
        }
        if (protocolVersion == LEGACY_VERSION) {
            NetworkDataValidation.validateDate(date);
        }
        NetworkDataValidation.validatePubKeyBase64(pubKeyBase64);
        NetworkDataValidation.validateSignatureBase64(signatureBase64);
    }

    @Override
    public bisq.user.protobuf.AuthorizeAccountAgeRequest.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.user.protobuf.AuthorizeAccountAgeRequest.newBuilder()
                .setProfileId(profileId)
                .setHashAsHex(hashAsHex)
                .setDate(date)
                .setPubKeyBase64(pubKeyBase64)
                .setSignatureBase64(signatureBase64)
                .setProtocolVersion(protocolVersion)
                .setAccountInputDataWithSalt(ByteString.copyFrom(accountInputDataWithSalt));
    }

    @Override
    public bisq.user.protobuf.AuthorizeAccountAgeRequest toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    public static AuthorizeAccountAgeRequest fromProto(bisq.user.protobuf.AuthorizeAccountAgeRequest proto) {
        return new AuthorizeAccountAgeRequest(proto.hasProtocolVersion()
                ? proto.getProtocolVersion()
                : LEGACY_VERSION,
                proto.getProfileId(),
                proto.getHashAsHex(),
                proto.getDate(),
                proto.getAccountInputDataWithSalt().toByteArray(),
                proto.getPubKeyBase64(),
                proto.getSignatureBase64());
    }

    public static ProtoResolver<ExternalNetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.user.protobuf.AuthorizeAccountAgeRequest proto = any.unpack(bisq.user.protobuf.AuthorizeAccountAgeRequest.class);
                return AuthorizeAccountAgeRequest.fromProto(proto);
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
        if (!(o instanceof AuthorizeAccountAgeRequest that)) {
            return false;
        }
        return date == that.date &&
                protocolVersion == that.protocolVersion &&
                Objects.equals(profileId, that.profileId) &&
                Objects.equals(hashAsHex, that.hashAsHex) &&
                Objects.equals(pubKeyBase64, that.pubKeyBase64) &&
                Objects.equals(signatureBase64, that.signatureBase64) &&
                Arrays.equals(accountInputDataWithSalt, that.accountInputDataWithSalt);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(profileId, hashAsHex, date, pubKeyBase64, signatureBase64, protocolVersion);
        return 31 * result + Arrays.hashCode(accountInputDataWithSalt);
    }
}
