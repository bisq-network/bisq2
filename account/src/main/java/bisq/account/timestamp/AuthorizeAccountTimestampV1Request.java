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

package bisq.account.timestamp;

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
public final class AuthorizeAccountTimestampV1Request implements MailboxMessage, ExternalNetworkMessage {
    private transient final MetaData metaData = new MetaData(TTL_10_DAYS, getClass().getSimpleName(), MAX_MAP_SIZE_100);

    private final AuthorizeAccountTimestampV1Payload payload;
    private final byte[] signature;

    public AuthorizeAccountTimestampV1Request(AuthorizeAccountTimestampV1Payload payload,
                                              byte[] signature) {
        this.payload = payload;
        this.signature = signature;

        verify();
    }

    @Override
    public void verify() {
        // DSA signature is usually around 46–48 bytes
        NetworkDataValidation.validateByteArray(signature, 30, 60);
    }

    @Override
    public bisq.account.protobuf.AuthorizeAccountTimestampV1Request.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.AuthorizeAccountTimestampV1Request.newBuilder()
                .setPayload(payload.getBuilder(serializeForHash))
                .setSignature(ByteString.copyFrom(signature));
    }

    @Override
    public bisq.account.protobuf.AuthorizeAccountTimestampV1Request toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    public static AuthorizeAccountTimestampV1Request fromProto(bisq.account.protobuf.AuthorizeAccountTimestampV1Request proto) {
        return new AuthorizeAccountTimestampV1Request(
                AuthorizeAccountTimestampV1Payload.fromProto(proto.getPayload()),
                proto.getSignature().toByteArray()
        );
    }

    public static ProtoResolver<ExternalNetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.account.protobuf.AuthorizeAccountTimestampV1Request proto = any.unpack(bisq.account.protobuf.AuthorizeAccountTimestampV1Request.class);
                return AuthorizeAccountTimestampV1Request.fromProto(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.5, 1);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof AuthorizeAccountTimestampV1Request that)) return false;

        return Objects.equals(payload, that.payload) && Arrays.equals(signature, that.signature);
    }

    @Override
    public int hashCode() {
        int result = Objects.hashCode(payload);
        result = 31 * result + Arrays.hashCode(signature);
        return result;
    }
}
