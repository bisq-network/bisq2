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

package bisq.oracle.daobridge.model;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.network.protobuf.ExternalNetworkMessage;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.concurrent.TimeUnit;

@Getter
@ToString
@EqualsAndHashCode
public final class AuthorizeAccountAgeRequest implements MailboxMessage {
    private final String profileId;
    private final String hashAsHex;
    private final long date;
    private final String pubKeyBase64;
    private final String signatureBase64;
    private final MetaData metaData = new MetaData(TimeUnit.DAYS.toMillis(5),
            100000,
            AuthorizeAccountAgeRequest.class.getSimpleName());

    public AuthorizeAccountAgeRequest(String profileId,
                                      String hashAsHex,
                                      long date,
                                      String pubKeyBase64,
                                      String signatureBase64) {
        this.profileId = profileId;
        this.hashAsHex = hashAsHex;
        this.date = date;
        this.pubKeyBase64 = pubKeyBase64;
        this.signatureBase64 = signatureBase64;
    }

    @Override
    public bisq.network.protobuf.NetworkMessage toProto() {
        return getNetworkMessageBuilder()
                .setExternalNetworkMessage(ExternalNetworkMessage.newBuilder()
                        .setAny(Any.pack(toAuthorizeAccountAgeRequestProto())))
                .build();
    }

    private bisq.oracle.protobuf.AuthorizeAccountAgeRequest toAuthorizeAccountAgeRequestProto() {
        return bisq.oracle.protobuf.AuthorizeAccountAgeRequest.newBuilder()
                .setProfileId(profileId)
                .setHashAsHex(hashAsHex)
                .setDate(date)
                .setPubKeyBase64(pubKeyBase64)
                .setSignatureBase64(signatureBase64)
                .build();
    }

    public static AuthorizeAccountAgeRequest fromProto(bisq.oracle.protobuf.AuthorizeAccountAgeRequest proto) {
        return new AuthorizeAccountAgeRequest(proto.getProfileId(),
                proto.getHashAsHex(),
                proto.getDate(),
                proto.getPubKeyBase64(),
                proto.getSignatureBase64());
    }

    public static ProtoResolver<bisq.network.p2p.message.NetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.oracle.protobuf.AuthorizeAccountAgeRequest proto = any.unpack(bisq.oracle.protobuf.AuthorizeAccountAgeRequest.class);
                return AuthorizeAccountAgeRequest.fromProto(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }
}