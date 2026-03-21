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
import bisq.common.util.OptionalUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.p2p.message.ExternalNetworkMessage;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.support.mediation.MediationCaseState;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static bisq.network.p2p.services.data.storage.MetaData.HIGH_PRIORITY;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_10_DAYS;

@Slf4j
@Getter
@ToString
public final class MuSigMediationStateChangeMessage implements MailboxMessage, ExternalNetworkMessage {
    private transient final MetaData metaData = new MetaData(TTL_10_DAYS, HIGH_PRIORITY, getClass().getSimpleName());
    private final String id;
    private final String tradeId;
    private final MediationCaseState mediationCaseState;
    private final Optional<MuSigMediationResult> muSigMediationResult;
    private final Optional<byte[]> mediationResultSignature;

    public MuSigMediationStateChangeMessage(String id,
                                            String tradeId,
                                            MediationCaseState mediationCaseState,
                                            Optional<MuSigMediationResult> muSigMediationResult,
                                            Optional<byte[]> mediationResultSignature) {
        this.id = id;
        this.tradeId = tradeId;
        this.mediationCaseState = mediationCaseState;
        this.muSigMediationResult = muSigMediationResult;
        this.mediationResultSignature = mediationResultSignature.map(byte[]::clone);
        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateId(id);
        NetworkDataValidation.validateTradeId(tradeId);
        if (mediationCaseState == MediationCaseState.CLOSED && muSigMediationResult.isEmpty()) {
            throw new IllegalArgumentException("Closed mediation case state must contain MuSigMediationResult.");
        }
        if (muSigMediationResult.isPresent() && mediationResultSignature.isEmpty()) {
            throw new IllegalArgumentException("MuSig mediation result must contain mediationResultSignature.");
        }
        if (mediationResultSignature.isPresent() && muSigMediationResult.isEmpty()) {
            throw new IllegalArgumentException("MuSig mediation result signature requires muSigMediationResult.");
        }
        mediationResultSignature.ifPresent(NetworkDataValidation::validateECSignature);
    }

    @Override
    public bisq.support.protobuf.MuSigMediationStateChangeMessage.Builder getValueBuilder(boolean serializeForHash) {
        bisq.support.protobuf.MuSigMediationStateChangeMessage.Builder builder = bisq.support.protobuf.MuSigMediationStateChangeMessage.newBuilder()
                .setId(id)
                .setTradeId(tradeId)
                .setMediationCaseState(mediationCaseState.toProtoEnum());
        muSigMediationResult.ifPresent(result -> builder.setMuSigMediationResult(result.toProto(serializeForHash)));
        mediationResultSignature.ifPresent(signature -> builder.setMediationResultSignature(ByteString.copyFrom(signature)));
        return builder;
    }

    public static MuSigMediationStateChangeMessage fromProto(bisq.support.protobuf.MuSigMediationStateChangeMessage proto) {
        return new MuSigMediationStateChangeMessage(
                proto.getId(),
                proto.getTradeId(),
                MediationCaseState.fromProto(proto.getMediationCaseState()),
                proto.hasMuSigMediationResult()
                        ? Optional.of(MuSigMediationResult.fromProto(proto.getMuSigMediationResult()))
                        : Optional.empty(),
                proto.hasMediationResultSignature()
                        ? Optional.of(proto.getMediationResultSignature().toByteArray())
                        : Optional.empty()
        );
    }

    public static ProtoResolver<ExternalNetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.support.protobuf.MuSigMediationStateChangeMessage proto = any.unpack(bisq.support.protobuf.MuSigMediationStateChangeMessage.class);
                return MuSigMediationStateChangeMessage.fromProto(proto);
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    @Override
    public double getCostFactor() {
        return getCostFactor(0.1, 0.2);
    }

    public Optional<byte[]> getMediationResultSignature() {
        return mediationResultSignature.map(byte[]::clone);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MuSigMediationStateChangeMessage that)) {
            return false;
        }
        return Objects.equals(id, that.id) &&
                Objects.equals(tradeId, that.tradeId) &&
                mediationCaseState == that.mediationCaseState &&
                Objects.equals(muSigMediationResult, that.muSigMediationResult) &&
                OptionalUtils.optionalByteArrayEquals(mediationResultSignature, that.mediationResultSignature);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, tradeId, mediationCaseState, muSigMediationResult);
        result = 31 * result + mediationResultSignature.map(Arrays::hashCode).orElse(0);
        return result;
    }
}
