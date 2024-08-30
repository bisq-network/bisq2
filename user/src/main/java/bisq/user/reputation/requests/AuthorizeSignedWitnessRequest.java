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
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static bisq.network.p2p.services.data.storage.MetaData.MAX_MAP_SIZE_100;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_10_DAYS;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public final class AuthorizeSignedWitnessRequest implements MailboxMessage, ExternalNetworkMessage {
    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_10_DAYS, getClass().getSimpleName(), MAX_MAP_SIZE_100);
    private final String profileId;
    private final String hashAsHex;
    private final long accountAgeWitnessDate;
    private final long witnessSignDate;
    private final String pubKeyBase64;
    private final String signatureBase64;

    public AuthorizeSignedWitnessRequest(String profileId,
                                         String hashAsHex,
                                         long accountAgeWitnessDate,
                                         long witnessSignDate,
                                         String pubKeyBase64,
                                         String signatureBase64) {
        this.profileId = profileId;
        this.hashAsHex = hashAsHex;
        this.accountAgeWitnessDate = accountAgeWitnessDate;
        this.witnessSignDate = witnessSignDate;
        this.pubKeyBase64 = pubKeyBase64;
        this.signatureBase64 = signatureBase64;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateProfileId(profileId);
        NetworkDataValidation.validateHashAsHex(hashAsHex);
        NetworkDataValidation.validateDate(accountAgeWitnessDate);
        NetworkDataValidation.validateDate(witnessSignDate);
        NetworkDataValidation.validatePubKeyBase64(pubKeyBase64);
        NetworkDataValidation.validateSignatureBase64(signatureBase64);
    }

    @Override
    public bisq.user.protobuf.AuthorizeSignedWitnessRequest.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.user.protobuf.AuthorizeSignedWitnessRequest.newBuilder()
                .setProfileId(profileId)
                .setHashAsHex(hashAsHex)
                .setAccountAgeWitnessDate(accountAgeWitnessDate)
                .setWitnessSignDate(witnessSignDate)
                .setPubKeyBase64(pubKeyBase64)
                .setSignatureBase64(signatureBase64);
    }

    public static AuthorizeSignedWitnessRequest fromProto(bisq.user.protobuf.AuthorizeSignedWitnessRequest proto) {
        return new AuthorizeSignedWitnessRequest(proto.getProfileId(),
                proto.getHashAsHex(),
                proto.getAccountAgeWitnessDate(),
                proto.getWitnessSignDate(),
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
}