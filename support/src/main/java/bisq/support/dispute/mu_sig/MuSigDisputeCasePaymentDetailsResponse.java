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

import bisq.account.accounts.AccountPayload;
import bisq.common.proto.ProtoResolver;
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

@Slf4j
@Getter
@ToString
@EqualsAndHashCode
public final class MuSigDisputeCasePaymentDetailsResponse implements MailboxMessage, ExternalNetworkMessage, SenderPublicKeyProvidingPayload {
    private transient final MetaData metaData = new MetaData(TTL_10_DAYS, HIGH_PRIORITY, getClass().getSimpleName());
    private final String tradeId;
    private final NetworkId senderNetworkId;
    private final AccountPayload<?> takerAccountPayload;
    private final AccountPayload<?> makerAccountPayload;

    public MuSigDisputeCasePaymentDetailsResponse(String tradeId,
                                                  NetworkId senderNetworkId,
                                                  AccountPayload<?> takerAccountPayload,
                                                  AccountPayload<?> makerAccountPayload) {
        this.tradeId = tradeId;
        this.senderNetworkId = senderNetworkId;
        this.takerAccountPayload = takerAccountPayload;
        this.makerAccountPayload = makerAccountPayload;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateTradeId(tradeId);
    }

    @Override
    public bisq.support.protobuf.MuSigDisputeCasePaymentDetailsResponse.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.support.protobuf.MuSigDisputeCasePaymentDetailsResponse.newBuilder()
                .setTradeId(tradeId)
                .setSenderNetworkId(senderNetworkId.toProto(serializeForHash))
                .setTakerAccountPayload(takerAccountPayload.toProto(serializeForHash))
                .setMakerAccountPayload(makerAccountPayload.toProto(serializeForHash));
    }

    public static MuSigDisputeCasePaymentDetailsResponse fromProto(bisq.support.protobuf.MuSigDisputeCasePaymentDetailsResponse proto) {
        return new MuSigDisputeCasePaymentDetailsResponse(
                proto.getTradeId(),
                NetworkId.fromProto(proto.getSenderNetworkId()),
                AccountPayload.fromProto(proto.getTakerAccountPayload()),
                AccountPayload.fromProto(proto.getMakerAccountPayload())
        );
    }

    public static ProtoResolver<ExternalNetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.support.protobuf.MuSigDisputeCasePaymentDetailsResponse proto = any.unpack(bisq.support.protobuf.MuSigDisputeCasePaymentDetailsResponse.class);
                return MuSigDisputeCasePaymentDetailsResponse.fromProto(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.4);
    }

    @Override
    public PublicKey getSenderPublicKey() {
        return senderNetworkId.getPubKey().getPublicKey();
    }
}
