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
public final class AuthorizeAccountTimestampRequest implements MailboxMessage, ExternalNetworkMessage {
    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_10_DAYS, getClass().getSimpleName(), MAX_MAP_SIZE_100);

    private final TimestampType timestampType;
    private final AccountTimestamp accountTimestamp;
    private final byte[] saltedFingerprint;
    private final byte[] publicKey;
    private final byte[] signature;
    private final KeyAlgorithm keyAlgorithm;
    private final long creationDate;

    public AuthorizeAccountTimestampRequest(TimestampType timestampType,
                                            AccountTimestamp accountTimestamp,
                                            byte[] saltedFingerprint,
                                            byte[] publicKey,
                                            byte[] signature,
                                            KeyAlgorithm keyAlgorithm,
                                            long creationDate) {
        this.timestampType = timestampType;
        this.accountTimestamp = accountTimestamp;
        this.saltedFingerprint = saltedFingerprint;
        this.publicKey = publicKey;
        this.signature = signature;
        this.keyAlgorithm = keyAlgorithm;
        this.creationDate = creationDate;

        verify();
    }

    @Override
    public void verify() {
        //todo
        NetworkDataValidation.validateByteArray(saltedFingerprint, 1000);
        NetworkDataValidation.validateByteArray(publicKey, 1000);
        NetworkDataValidation.validateByteArray(signature, 100);
    }

    @Override
    public bisq.account.protobuf.AuthorizeAccountTimestampRequest.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.account.protobuf.AuthorizeAccountTimestampRequest.newBuilder()
                .setTimestampType(timestampType.toProtoEnum())
                .setAccountTimestamp(accountTimestamp.toProto(serializeForHash))
                .setSaltedFingerprint(ByteString.copyFrom(saltedFingerprint))
                .setPublicKey(ByteString.copyFrom(publicKey))
                .setSignature(ByteString.copyFrom(signature))
                .setKeyAlgorithm(keyAlgorithm.toProtoEnum())
                .setCreationDate(creationDate);
    }

    @Override
    public bisq.account.protobuf.AuthorizeAccountTimestampRequest toValueProto(boolean serializeForHash) {
        return resolveValueProto(serializeForHash);
    }

    public static AuthorizeAccountTimestampRequest fromProto(bisq.account.protobuf.AuthorizeAccountTimestampRequest proto) {
        return new AuthorizeAccountTimestampRequest(
                TimestampType.fromProto(proto.getTimestampType()),
                AccountTimestamp.fromProto(proto.getAccountTimestamp()),
                proto.getSaltedFingerprint().toByteArray(),
                proto.getPublicKey().toByteArray(),
                proto.getSignature().toByteArray(),
                KeyAlgorithm.fromProto(proto.getKeyAlgorithm()),
                proto.getCreationDate()
        );
    }

    public static ProtoResolver<ExternalNetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.account.protobuf.AuthorizeAccountTimestampRequest proto = any.unpack(bisq.account.protobuf.AuthorizeAccountTimestampRequest.class);
                return AuthorizeAccountTimestampRequest.fromProto(proto);
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
