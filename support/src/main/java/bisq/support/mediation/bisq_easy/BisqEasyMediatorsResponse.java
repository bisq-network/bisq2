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

package bisq.support.mediation.bisq_easy;

import bisq.common.annotation.ExcludeForHash;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.ExternalNetworkMessage;
import bisq.network.p2p.message.SenderPublicKeyProvidingPayload;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.security.PublicKey;

import static bisq.network.p2p.services.data.storage.MetaData.HIGH_PRIORITY;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_10_DAYS;
import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public final class BisqEasyMediatorsResponse implements MailboxMessage, ExternalNetworkMessage,
        SenderPublicKeyProvidingPayload {
    private static final int VERSION = 1;

    public static final MetaData META_DATA = new MetaData(TTL_10_DAYS, HIGH_PRIORITY, BisqEasyMediatorsResponse.class.getSimpleName());

    private final String tradeId;
    @ExcludeForHash(excludeOnlyInVersions = {0, 1})
    @EqualsAndHashCode.Exclude
    private final NetworkId senderNetworkId;
    @ExcludeForHash
    @EqualsAndHashCode.Exclude
    private final int version;

    public BisqEasyMediatorsResponse(String tradeId, NetworkId senderNetworkId) {
        this(VERSION, tradeId, senderNetworkId);
    }

    private BisqEasyMediatorsResponse(int version, String tradeId, NetworkId senderNetworkId) {
        this.version = version;
        this.tradeId = tradeId;
        this.senderNetworkId = senderNetworkId;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateTradeId(tradeId);
        // The receiver takes the sender public key from it (SenderPublicKeyProvidingPayload)
        checkArgument(senderNetworkId != null, "senderNetworkId must be present");
    }

    /**
     * Keep proto name for backward compatibility
     */

    @Override
    public bisq.support.protobuf.MediatorsResponse.Builder getValueBuilder(boolean serializeForHash) {
        bisq.support.protobuf.MediatorsResponse.Builder builder = bisq.support.protobuf.MediatorsResponse.newBuilder()
                .setVersion(version)
                .setTradeId(tradeId);
        builder.setSenderNetworkId(senderNetworkId.toProto(serializeForHash));
        return builder;
    }

    public static BisqEasyMediatorsResponse fromProto(bisq.support.protobuf.MediatorsResponse proto)
            throws UnresolvableProtobufException {
        try {
            // Responses of version 0 have no senderNetworkId. The receiver requires the sender public key of every
            // response, so such a response is invalid.
            checkArgument(proto.hasSenderNetworkId(), "senderNetworkId must be present");
            return new BisqEasyMediatorsResponse(proto.getVersion(),
                    proto.getTradeId(),
                    NetworkId.fromProto(proto.getSenderNetworkId()));
        } catch (RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
    }

    public static ProtoResolver<ExternalNetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.support.protobuf.MediatorsResponse proto = any.unpack(bisq.support.protobuf.MediatorsResponse.class);
                return BisqEasyMediatorsResponse.fromProto(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public MetaData getMetaData() {
        return META_DATA;
    }


    @Override
    public PublicKey getSenderPublicKey() {
        return senderNetworkId.getPubKey().getPublicKey();
    }
}
