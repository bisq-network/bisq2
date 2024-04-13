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
import bisq.network.p2p.message.EnvelopePayloadMessage;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.network.protobuf.ExternalNetworkMessage;
import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import static bisq.network.p2p.services.data.storage.MetaData.TTL_10_DAYS;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public final class AuthorizeTimestampRequest implements MailboxMessage {
    private final MetaData metaData = new MetaData(TTL_10_DAYS, getClass().getSimpleName());
    private final String profileId;

    public AuthorizeTimestampRequest(String profileId) {
        this.profileId = profileId;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateProfileId(profileId);
    }

    @Override
    public bisq.network.protobuf.EnvelopePayloadMessage.Builder getBuilder(boolean ignoreAnnotation) {
        return getNetworkMessageBuilder()
                .setExternalNetworkMessage(ExternalNetworkMessage.newBuilder()
                        .setAny(Any.pack(toAuthorizeTimestampRequestProto(ignoreAnnotation))));
    }

    @Override
    public bisq.network.protobuf.EnvelopePayloadMessage toProto(boolean ignoreAnnotation) {
        return buildProto(ignoreAnnotation);
    }

    private bisq.user.protobuf.AuthorizeTimestampRequest toAuthorizeTimestampRequestProto(boolean ignoreAnnotation) {
        var builder = bisq.user.protobuf.AuthorizeTimestampRequest.newBuilder()
                .setProfileId(profileId);
        return ignoreAnnotation ? builder.build() : clearAnnotatedFields(builder).build();
    }

    public static AuthorizeTimestampRequest fromProto(bisq.user.protobuf.AuthorizeTimestampRequest proto) {
        return new AuthorizeTimestampRequest(proto.getProfileId());
    }

    public static ProtoResolver<EnvelopePayloadMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.user.protobuf.AuthorizeTimestampRequest proto = any.unpack(bisq.user.protobuf.AuthorizeTimestampRequest.class);
                return AuthorizeTimestampRequest.fromProto(proto);
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