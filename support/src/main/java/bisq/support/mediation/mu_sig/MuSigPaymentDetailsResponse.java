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

import bisq.account.accounts.AccountPayload;
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
public final class MuSigPaymentDetailsResponse implements MailboxMessage, ExternalNetworkMessage {
    private transient final MetaData metaData = new MetaData(TTL_10_DAYS, HIGH_PRIORITY, getClass().getSimpleName());
    private final String tradeId;
    private final AccountPayload<?> takerAccountPayload;
    private final AccountPayload<?> makerAccountPayload;
    private final String senderUserProfileId;

    public MuSigPaymentDetailsResponse(String tradeId,
                                       AccountPayload<?> takerAccountPayload,
                                       AccountPayload<?> makerAccountPayload,
                                       String senderUserProfileId) {
        this.tradeId = tradeId;
        this.takerAccountPayload = takerAccountPayload;
        this.makerAccountPayload = makerAccountPayload;
        this.senderUserProfileId = senderUserProfileId;

        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateTradeId(tradeId);
        NetworkDataValidation.validateProfileId(senderUserProfileId);
    }

    @Override
    public bisq.support.protobuf.MuSigPaymentDetailsResponse.Builder getValueBuilder(boolean serializeForHash) {
        return bisq.support.protobuf.MuSigPaymentDetailsResponse.newBuilder()
                .setTradeId(tradeId)
                .setTakerAccountPayload(takerAccountPayload.toProto(serializeForHash))
                .setMakerAccountPayload(makerAccountPayload.toProto(serializeForHash))
                .setSenderUserProfileId(senderUserProfileId);
    }

    public static MuSigPaymentDetailsResponse fromProto(bisq.support.protobuf.MuSigPaymentDetailsResponse proto) {
        return new MuSigPaymentDetailsResponse(
                proto.getTradeId(),
                AccountPayload.fromProto(proto.getTakerAccountPayload()),
                AccountPayload.fromProto(proto.getMakerAccountPayload()),
                proto.getSenderUserProfileId()
        );
    }

    public static ProtoResolver<ExternalNetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.support.protobuf.MuSigPaymentDetailsResponse proto = any.unpack(bisq.support.protobuf.MuSigPaymentDetailsResponse.class);
                return MuSigPaymentDetailsResponse.fromProto(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.4);
    }
}
