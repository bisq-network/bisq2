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

package bisq.trade.mu_sig;

import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.proto.PersistableProto;
import bisq.common.util.OptionalUtils;
import bisq.support.arbitration.mu_sig.MuSigArbitrationResult;
import bisq.support.mediation.mu_sig.MuSigMediationResult;
import bisq.trade.MuSigDisputeState;
import com.google.protobuf.ByteString;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

@Getter
public final class MuSigTradeDispute implements PersistableProto {
    private final Observable<MuSigDisputeState> disputeState = new Observable<>(MuSigDisputeState.NO_DISPUTE);
    private Optional<MuSigMediationResult> muSigMediationResult = Optional.empty();
    private Optional<byte[]> mediationResultSignature = Optional.empty();
    private Optional<MuSigArbitrationResult> muSigArbitrationResult = Optional.empty();
    private Optional<byte[]> arbitrationResultSignature = Optional.empty();

    public MuSigTradeDispute() {
    }

    private MuSigTradeDispute(MuSigDisputeState disputeState,
                              Optional<MuSigMediationResult> muSigMediationResult,
                              Optional<byte[]> mediationResultSignature,
                              Optional<MuSigArbitrationResult> muSigArbitrationResult,
                              Optional<byte[]> arbitrationResultSignature) {
        this.disputeState.set(disputeState);
        this.muSigMediationResult = muSigMediationResult;
        this.mediationResultSignature = mediationResultSignature.map(byte[]::clone);
        this.muSigArbitrationResult = muSigArbitrationResult;
        this.arbitrationResultSignature = arbitrationResultSignature.map(byte[]::clone);
    }

    @Override
    public bisq.trade.protobuf.MuSigTradeDispute.Builder getBuilder(boolean serializeForHash) {
        bisq.trade.protobuf.MuSigTradeDispute.Builder builder = bisq.trade.protobuf.MuSigTradeDispute.newBuilder()
                .setDisputeState(disputeState.get().toProtoEnum());
        muSigMediationResult.ifPresent(result -> builder.setMuSigMediationResult(result.toProto(serializeForHash)));
        mediationResultSignature.ifPresent(signature -> builder.setMediationResultSignature(ByteString.copyFrom(signature)));
        muSigArbitrationResult.ifPresent(result -> builder.setMuSigArbitrationResult(result.toProto(serializeForHash)));
        arbitrationResultSignature.ifPresent(signature -> builder.setArbitrationResultSignature(ByteString.copyFrom(signature)));
        return builder;
    }

    @Override
    public bisq.trade.protobuf.MuSigTradeDispute toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static MuSigTradeDispute fromProto(bisq.trade.protobuf.MuSigTradeDispute proto) {
        return new MuSigTradeDispute(
                MuSigDisputeState.fromProto(proto.getDisputeState()),
                proto.hasMuSigMediationResult()
                        ? Optional.of(MuSigMediationResult.fromProto(proto.getMuSigMediationResult()))
                        : Optional.empty(),
                proto.hasMediationResultSignature()
                        ? Optional.of(proto.getMediationResultSignature().toByteArray())
                        : Optional.empty(),
                proto.hasMuSigArbitrationResult()
                        ? Optional.of(MuSigArbitrationResult.fromProto(proto.getMuSigArbitrationResult()))
                        : Optional.empty(),
                proto.hasArbitrationResultSignature()
                        ? Optional.of(proto.getArbitrationResultSignature().toByteArray())
                        : Optional.empty()
        );
    }

    public void setDisputeState(MuSigDisputeState disputeState) {
        this.disputeState.set(disputeState);
    }

    public MuSigDisputeState getDisputeState() {
        return disputeState.get();
    }

    public ReadOnlyObservable<MuSigDisputeState> disputeStateObservable() {
        return disputeState;
    }

    public boolean setMuSigMediationResult(MuSigMediationResult muSigMediationResult) {
        if (this.muSigMediationResult.isPresent()) {
            return false;
        }
        this.muSigMediationResult = Optional.of(muSigMediationResult);
        return true;
    }

    public boolean setMediationResultSignature(byte[] mediationResultSignature) {
        if (this.mediationResultSignature.isPresent()) {
            return false;
        }
        this.mediationResultSignature = Optional.of(mediationResultSignature.clone());
        return true;
    }

    public Optional<byte[]> getMediationResultSignature() {
        return mediationResultSignature.map(byte[]::clone);
    }

    public boolean setMuSigArbitrationResult(MuSigArbitrationResult muSigArbitrationResult) {
        if (this.muSigArbitrationResult.isPresent()) {
            return false;
        }
        this.muSigArbitrationResult = Optional.of(muSigArbitrationResult);
        return true;
    }

    public boolean setArbitrationResultSignature(byte[] arbitrationResultSignature) {
        if (this.arbitrationResultSignature.isPresent()) {
            return false;
        }
        this.arbitrationResultSignature = Optional.of(arbitrationResultSignature.clone());
        return true;
    }

    public Optional<byte[]> getArbitrationResultSignature() {
        return arbitrationResultSignature.map(byte[]::clone);
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof MuSigTradeDispute that)) return false;

        return disputeState.get() == that.disputeState.get()
                && muSigMediationResult.equals(that.muSigMediationResult)
                && OptionalUtils.optionalByteArrayEquals(mediationResultSignature, that.mediationResultSignature)
                && muSigArbitrationResult.equals(that.muSigArbitrationResult)
                && OptionalUtils.optionalByteArrayEquals(arbitrationResultSignature, that.arbitrationResultSignature);
    }

    @Override
    public int hashCode() {
        int result = disputeState.get().hashCode();
        result = 31 * result + muSigMediationResult.hashCode();
        result = 31 * result + mediationResultSignature.map(Arrays::hashCode).orElse(0);
        result = 31 * result + muSigArbitrationResult.hashCode();
        result = 31 * result + arbitrationResultSignature.map(Arrays::hashCode).orElse(0);
        return result;
    }
}
