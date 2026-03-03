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

import bisq.common.observable.Observable;
import bisq.common.proto.PersistableProto;
import bisq.support.mediation.MediationCaseState;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Optional;

import static java.lang.System.currentTimeMillis;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MuSigMediationCase implements PersistableProto {
    @EqualsAndHashCode.Include
    private final MuSigMediationRequest muSigMediationRequest;
    private final long requestDate;
    private final Observable<MediationCaseState> mediationCaseState = new Observable<>();
    private final Observable<Optional<MuSigMediationResult>> muSigMediationResult = new Observable<>();


    public MuSigMediationCase(MuSigMediationRequest muSigMediationRequest) {
        this(muSigMediationRequest, currentTimeMillis(), MediationCaseState.OPEN, Optional.empty());
    }

    private MuSigMediationCase(MuSigMediationRequest muSigMediationRequest,
                               long requestDate,
                               MediationCaseState mediationCaseState,
                               Optional<MuSigMediationResult> muSigMediationResult) {
        this.muSigMediationRequest = muSigMediationRequest;
        this.requestDate = requestDate;
        this.mediationCaseState.set(mediationCaseState);
        this.muSigMediationResult.set(muSigMediationResult);
    }

    /**
     * Keep proto name for backward compatibility
     */

    @Override
    public bisq.support.protobuf.MuSigMediationCase.Builder getBuilder(boolean serializeForHash) {
        bisq.support.protobuf.MuSigMediationCase.Builder builder = bisq.support.protobuf.MuSigMediationCase.newBuilder()
                .setMuSigMediationRequest(muSigMediationRequest.toValueProto(serializeForHash))
                .setRequestDate(requestDate)
                .setMediationCaseState(mediationCaseState.get().toProtoEnum());
        muSigMediationResult.get().ifPresent(item ->
                builder.setMuSigMediationResult(item.toProto(serializeForHash)));
        return builder;
    }

    @Override
    public bisq.support.protobuf.MuSigMediationCase toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }

    public static MuSigMediationCase fromProto(bisq.support.protobuf.MuSigMediationCase proto) {
        return new MuSigMediationCase(MuSigMediationRequest.fromProto(proto.getMuSigMediationRequest()),
                proto.getRequestDate(),
                MediationCaseState.fromProto(proto.getMediationCaseState()),
                proto.hasMuSigMediationResult() ?
                        Optional.of(MuSigMediationResult.fromProto(proto.getMuSigMediationResult())) :
                        Optional.empty());
    }

    public boolean setMediationCaseState(MediationCaseState state) {
        if (mediationCaseState.get() == state) {
            return false;
        }
        mediationCaseState.set(state);
        return true;
    }

    public boolean setMuSigMediationResult(MuSigMediationResult result) {
        Optional<MuSigMediationResult> currentResult = muSigMediationResult.get();
        if (currentResult.isPresent() && !currentResult.get().equals(result)) {
            throw new IllegalArgumentException("MuSigMediationResult cannot be changed once set.");
        }
        var newResult = Optional.of(result);
        if (currentResult.equals(newResult)) {
            return false;
        }
        muSigMediationResult.set(newResult);
        return true;
    }
}
