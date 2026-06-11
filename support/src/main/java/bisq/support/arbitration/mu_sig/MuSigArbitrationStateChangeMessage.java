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

package bisq.support.arbitration.mu_sig;

import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.common.util.OptionalUtils;
import bisq.common.validation.NetworkDataValidation;
import bisq.network.identity.NetworkId;
import bisq.network.p2p.message.ExternalNetworkMessage;
import bisq.network.p2p.message.SenderPublicKeyProvidingPayload;
import bisq.network.p2p.services.data.storage.MetaData;
import bisq.network.p2p.services.data.storage.mailbox.MailboxMessage;
import bisq.support.arbitration.ArbitrationCaseState;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

import static bisq.network.p2p.services.data.storage.MetaData.HIGH_PRIORITY;
import static bisq.network.p2p.services.data.storage.MetaData.TTL_10_DAYS;

@Slf4j
@Getter
@ToString
public final class MuSigArbitrationStateChangeMessage implements MailboxMessage, ExternalNetworkMessage, SenderPublicKeyProvidingPayload {
    private transient final MetaData metaData = new MetaData(TTL_10_DAYS, HIGH_PRIORITY, getClass().getSimpleName());
    private final String id;
    private final String tradeId;
    private final NetworkId senderNetworkId;
    private final ArbitrationCaseState arbitrationCaseState;
    private final Optional<MuSigArbitrationResult> muSigArbitrationResult;
    private final Optional<byte[]> arbitrationResultSignature;

    public MuSigArbitrationStateChangeMessage(String id,
                                              String tradeId,
                                              NetworkId senderNetworkId,
                                              ArbitrationCaseState arbitrationCaseState,
                                              Optional<MuSigArbitrationResult> muSigArbitrationResult,
                                              Optional<byte[]> arbitrationResultSignature) {
        this.id = id;
        this.tradeId = tradeId;
        this.senderNetworkId = senderNetworkId;
        this.arbitrationCaseState = arbitrationCaseState;
        this.muSigArbitrationResult = muSigArbitrationResult;
        this.arbitrationResultSignature = arbitrationResultSignature.map(byte[]::clone);
        verify();
    }

    @Override
    public void verify() {
        NetworkDataValidation.validateId(id);
        NetworkDataValidation.validateTradeId(tradeId);
        arbitrationResultSignature.ifPresent(NetworkDataValidation::validateECSignature);
    }

    @Override
    public bisq.support.protobuf.MuSigArbitrationStateChangeMessage.Builder getValueBuilder(boolean serializeForHash) {
        bisq.support.protobuf.MuSigArbitrationStateChangeMessage.Builder builder = bisq.support.protobuf.MuSigArbitrationStateChangeMessage.newBuilder()
                .setId(id)
                .setTradeId(tradeId)
                .setSenderNetworkId(senderNetworkId.toProto(serializeForHash))
                .setArbitrationCaseState(arbitrationCaseState.toProtoEnum());
        muSigArbitrationResult.ifPresent(result -> builder.setMuSigArbitrationResult(result.toProto(serializeForHash)));
        arbitrationResultSignature.ifPresent(signature -> builder.setArbitrationResultSignature(ByteString.copyFrom(signature)));
        return builder;
    }

    public static MuSigArbitrationStateChangeMessage fromProto(bisq.support.protobuf.MuSigArbitrationStateChangeMessage proto) {
        return new MuSigArbitrationStateChangeMessage(
                proto.getId(),
                proto.getTradeId(),
                NetworkId.fromProto(proto.getSenderNetworkId()),
                ArbitrationCaseState.fromProto(proto.getArbitrationCaseState()),
                proto.hasMuSigArbitrationResult()
                        ? Optional.of(MuSigArbitrationResult.fromProto(proto.getMuSigArbitrationResult()))
                        : Optional.empty(),
                proto.hasArbitrationResultSignature()
                        ? Optional.of(proto.getArbitrationResultSignature().toByteArray())
                        : Optional.empty()
        );
    }

    public static ProtoResolver<ExternalNetworkMessage> getNetworkMessageResolver() {
        return any -> {
            try {
                bisq.support.protobuf.MuSigArbitrationStateChangeMessage proto = any.unpack(bisq.support.protobuf.MuSigArbitrationStateChangeMessage.class);
                return MuSigArbitrationStateChangeMessage.fromProto(proto);
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
        return senderNetworkId.getPubKey().getPublicKey();
    }

    public Optional<byte[]> getArbitrationResultSignature() {
        return arbitrationResultSignature.map(byte[]::clone);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MuSigArbitrationStateChangeMessage that)) {
            return false;
        }
        return Objects.equals(id, that.id) &&
                Objects.equals(tradeId, that.tradeId) &&
                Objects.equals(senderNetworkId, that.senderNetworkId) &&
                arbitrationCaseState == that.arbitrationCaseState &&
                Objects.equals(muSigArbitrationResult, that.muSigArbitrationResult) &&
                OptionalUtils.optionalByteArrayEquals(arbitrationResultSignature, that.arbitrationResultSignature);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(id, tradeId, senderNetworkId, arbitrationCaseState, muSigArbitrationResult);
        result = 31 * result + arbitrationResultSignature.map(Arrays::hashCode).orElse(0);
        return result;
    }
}
