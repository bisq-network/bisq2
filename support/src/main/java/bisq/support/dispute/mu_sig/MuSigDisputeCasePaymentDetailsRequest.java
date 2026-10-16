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

package bisq.support.dispute.mu_sig;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.ExternalNetworkMessage;
import bisq.network.p2p.message.SenderPublicKeyProvidingPayload;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.StoragePriority;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.security.PublicKey;

import static bisq.network.p2p.services.data.storage.MetaData.TTL_10_DAYS;

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public final class MuSigDisputeCasePaymentDetailsRequest implements MailboxMessage, ExternalNetworkMessage, SenderPublicKeyProvidingPayload {
    public static final MetaData META_DATA = new MetaData(TTL_10_DAYS, StoragePriority.HIGH, MuSigDisputeCasePaymentDetailsRequest.class.getSimpleName());
    private final String tradeId;
    private final NetworkId senderNetworkId;

    public MuSigDisputeCasePaymentDetailsRequest(String tradeId, NetworkId senderNetworkId) {
        this.tradeId = tradeId;
        this.senderNetworkId = senderNetworkId;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateTradeId(tradeId);
    }

    @Override
    public bisq.support.protobuf.MuSigDisputeCasePaymentDetailsRequest.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.support.protobuf.MuSigDisputeCasePaymentDetailsRequest.newBuilder()
                .setTradeId(tradeId)
                .setSenderNetworkId(senderNetworkId.toProto(serializeForHash));
    }

    public static MuSigDisputeCasePaymentDetailsRequest fromProto(bisq.support.protobuf.MuSigDisputeCasePaymentDetailsRequest proto)
            throws UnresolvableProtobufException {
        try {
            return new MuSigDisputeCasePaymentDetailsRequest(
                    proto.getTradeId(),
                    NetworkId.fromProto(proto.getSenderNetworkId())
            );
        } catch (RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
    }

    public static ProtoResolver<ExternalNetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.support.protobuf.MuSigDisputeCasePaymentDetailsRequest proto = any.unpack(bisq.support.protobuf.MuSigDisputeCasePaymentDetailsRequest.class);
                return MuSigDisputeCasePaymentDetailsRequest.fromProto(proto);
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
