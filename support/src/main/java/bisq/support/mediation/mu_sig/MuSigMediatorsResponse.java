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

package bisq.support.mediation.mu_sig;

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

import static bisq.network.p2p.services.data.storage.MetaData.HIGH_PRIORITY;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_10_DAYS;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public final class MuSigMediatorsResponse implements MailboxMessage, ExternalNetworkMessage {
    // MetaData is transient as it will be used indirectly by low level network classes. Only some low level network classes write the metaData to their protobuf representations.
    private transient final MetaData metaData = new MetaData(TTL_10_DAYS, HIGH_PRIORITY, getClass().getSimpleName());
    private final String tradeId;

    public MuSigMediatorsResponse(String tradeId) {
        this.tradeId = tradeId;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateTradeId(tradeId);
    }

    /**
     * Keep proto name for backward compatibility
     */

    @Override
    public bisq.support.protobuf.MuSigMediatorsResponse.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.support.protobuf.MuSigMediatorsResponse.newBuilder()
                .setTradeId(tradeId);
    }

    public static MuSigMediatorsResponse fromProto(bisq.support.protobuf.MuSigMediatorsResponse proto) {
        return new MuSigMediatorsResponse(proto.getTradeId());
    }

    public static ProtoResolver<ExternalNetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.support.protobuf.MuSigMediatorsResponse proto = any.unpack(bisq.support.protobuf.MuSigMediatorsResponse.class);
                return MuSigMediatorsResponse.fromProto(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.2);
    }
}