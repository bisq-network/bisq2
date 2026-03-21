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

import bisq.common.observable.Observable;
import bisq.common.proto.PersistableProto;
import bisq.common.validation.NetworkDataValidation;
import bisq.support.arbitration.ArbitrationCaseState;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Arrays;
import java.util.Optional;

import static java.lang.System.currentTimeMillis;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public final class MuSigArbitrationCase implements PersistableProto {
    @EqualsAndHashCode.Include
    private final MuSigArbitrationRequest muSigArbitrationRequest;
    private final long requestDate;
    private final Observable<ArbitrationCaseState> arbitrationCaseState = new Observable<>();
    private final Observable<Optional<MuSigArbitrationResult>> muSigArbitrationResult = new Observable<>();
    private Optional<byte[]> arbitrationResultSignature = Optional.empty();

    public MuSigArbitrationCase(MuSigArbitrationRequest muSigArbitrationRequest) {
        this(muSigArbitrationRequest,
                currentTimeMillis(),
                ArbitrationCaseState.OPEN,
                Optional.empty(),
                Optional.empty());
    }

    private MuSigArbitrationCase(MuSigArbitrationRequest muSigArbitrationRequest,
                                 long requestDate,
                                 ArbitrationCaseState arbitrationCaseState,
                                 Optional<MuSigArbitrationResult> muSigArbitrationResult,
                                 Optional<byte[]> arbitrationResultSignature) {
        this.muSigArbitrationRequest = muSigArbitrationRequest;
        this.requestDate = requestDate;
        this.arbitrationCaseState.set(arbitrationCaseState);
        this.muSigArbitrationResult.set(muSigArbitrationResult);
        this.arbitrationResultSignature = arbitrationResultSignature.map(byte[]::clone);
    }

    @Override
    public bisq.support.protobuf.MuSigArbitrationCase.Builder getBuilder(boolean serializeForHash) {
        bisq.support.protobuf.MuSigArbitrationCase.Builder builder = bisq.support.protobuf.MuSigArbitrationCase.newBuilder()
                .setMuSigArbitrationRequest(muSigArbitrationRequest.toValueProto(serializeForHash))
                .setRequestDate(requestDate)
                .setArbitrationCaseState(arbitrationCaseState.get().toProtoEnum());
        muSigArbitrationResult.get().ifPresent(result ->
                builder.setMuSigArbitrationResult(result.toProto(serializeForHash)));
        arbitrationResultSignature.ifPresent(signature -> builder.setArbitrationResultSignature(ByteString.copyFrom(signature)));
        return builder;
    }

    @Override
    public bisq.support.protobuf.MuSigArbitrationCase toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static MuSigArbitrationCase fromProto(bisq.support.protobuf.MuSigArbitrationCase proto) {
        return new MuSigArbitrationCase(MuSigArbitrationRequest.fromProto(proto.getMuSigArbitrationRequest()),
                proto.getRequestDate(),
                ArbitrationCaseState.fromProto(proto.getArbitrationCaseState()),
                proto.hasMuSigArbitrationResult()
                        ? Optional.of(MuSigArbitrationResult.fromProto(proto.getMuSigArbitrationResult()))
                        : Optional.empty(),
                proto.hasArbitrationResultSignature()
                        ? Optional.of(proto.getArbitrationResultSignature().toByteArray())
                        : Optional.empty());
    }

    public boolean setArbitrationCaseState(ArbitrationCaseState state) {
        if (arbitrationCaseState.get() == state) {
            return false;
        }
        arbitrationCaseState.set(state);
        return true;
    }

    public Optional<byte[]> getArbitrationResultSignature() {
        return arbitrationResultSignature.map(byte[]::clone);
    }

    public boolean setSignedMuSigArbitrationResult(MuSigArbitrationResult result, byte[] signature) {
        NetworkDataValidation.validateECSignature(signature);
        byte[] signatureCopy = signature.clone();
        Optional<MuSigArbitrationResult> currentResult = muSigArbitrationResult.get();
        if (currentResult.isPresent() && !currentResult.orElseThrow().equals(result)) {
            throw new IllegalArgumentException("MuSigArbitrationResult cannot be changed once set.");
        }
        Optional<byte[]> currentSignature = arbitrationResultSignature;
        if (currentSignature.isPresent() && !Arrays.equals(currentSignature.orElseThrow(), signatureCopy)) {
            throw new IllegalArgumentException("arbitrationResultSignature cannot be changed once set.");
        }
        if (currentResult.isPresent() && currentSignature.isPresent()) {
            return false;
        }
        if (currentResult.isEmpty()) {
            muSigArbitrationResult.set(Optional.of(result));
        }
        if (currentSignature.isEmpty()) {
            arbitrationResultSignature = Optional.of(signatureCopy);
        }
        return true;
    }
}
