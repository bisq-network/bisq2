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
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.util.DateUtils;
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
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Optional;

import static bisq.network.p2p.services.data.storage.MetaData.HIGH_PRIORITY;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_10_DAYS;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public final class BisqEasyMediatorsResponse implements MailboxMessage, ExternalNetworkMessage,
        SenderPublicKeyProvidingPayload {
    private static final int VERSION = 1;

    // Can be removed after senderNetworkId is mandatory.
    public static final Date SENDER_NETWORK_ID_VERIFICATION_ACTIVATION_DATE =
            DateUtils.getUTCDate(2026, GregorianCalendar.MAY, 30);
    public static final boolean IS_SENDER_NETWORK_ID_VERIFICATION_ACTIVATED =
            new Date().after(SENDER_NETWORK_ID_VERIFICATION_ACTIVATION_DATE);

    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_10_DAYS, HIGH_PRIORITY, getClass().getSimpleName());

    private final String tradeId;
    @ExcludeForHash(excludeOnlyInVersions = {0, 1})
    @EqualsAndHashCode.Exclude
    private final Optional<NetworkId> senderNetworkId;
    @ExcludeForHash
    @EqualsAndHashCode.Exclude
    private final int version;

    public BisqEasyMediatorsResponse(String tradeId, NetworkId senderNetworkId) {
        this(VERSION, tradeId, Optional.of(senderNetworkId));
    }

    private BisqEasyMediatorsResponse(int version, String tradeId, Optional<NetworkId> senderNetworkId) {
        this.version = version;
        this.tradeId = tradeId;
        this.senderNetworkId = senderNetworkId;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateTradeId(tradeId);
        if (version >= VERSION) {
            senderNetworkId.orElseThrow(() -> new IllegalArgumentException("senderNetworkId must be present"));
        }
    }

    /**
     * Keep proto name for backward compatibility
     */

    @Override
    public bisq.support.protobuf.MediatorsResponse.Builder getValueBuilder(boolean serializeForHash) {
        bisq.support.protobuf.MediatorsResponse.Builder builder = bisq.support.protobuf.MediatorsResponse.newBuilder()
                .setVersion(version)
                .setTradeId(tradeId);
        senderNetworkId.ifPresent(networkId -> builder.setSenderNetworkId(networkId.toProto(serializeForHash)));
        return builder;
    }

    public static BisqEasyMediatorsResponse fromProto(bisq.support.protobuf.MediatorsResponse proto) {
        Optional<NetworkId> senderNetworkId = proto.hasSenderNetworkId()
                ? Optional.of(NetworkId.fromProto(proto.getSenderNetworkId()))
                : Optional.empty();
        return new BisqEasyMediatorsResponse(proto.getVersion(), proto.getTradeId(), senderNetworkId);
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
    public double getCostFactor() {
        return getCostFactor(0.1, 0.2);
    }


    @Override
    public PublicKey getSenderPublicKey() {
        return findSenderPublicKey().orElseThrow(() -> new IllegalStateException("senderNetworkId not set"));
    }

    @Override
    public Optional<PublicKey> findSenderPublicKey() {
        return senderNetworkId.map(networkId -> networkId.getPubKey().getPublicKey());
    }

    @Override
    public boolean isSenderPublicKeyRequired() {
        return version >= VERSION || IS_SENDER_NETWORK_ID_VERIFICATION_ACTIVATED;
    }
}
