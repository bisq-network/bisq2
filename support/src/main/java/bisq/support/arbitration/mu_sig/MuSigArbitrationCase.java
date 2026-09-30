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

import bisq.account.accounts.AccountPayload;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.proto.PersistableProto;
import bisq.common.validation.NetworkDataValidation;
import bisq.support.arbitration.ArbitrationCaseState;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.lang.System.currentTimeMillis;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MuSigArbitrationCase implements PersistableProto {
    @EqualsAndHashCode.Include
    @Getter
    private final MuSigArbitrationRequest muSigArbitrationRequest;
    @Getter
    private final long requestDate;
    private final Observable<ArbitrationCaseState> arbitrationCaseState = new Observable<>();
    private final Observable<Optional<MuSigArbitrationResult>> muSigArbitrationResult = new Observable<>();
    private Optional<byte[]> arbitrationResultSignature = Optional.empty();
    private final Observable<Optional<AccountPayload<?>>> takerAccountPayload = new Observable<>(Optional.empty());
    private final Observable<Optional<AccountPayload<?>>> makerAccountPayload = new Observable<>(Optional.empty());
    private final Observable<List<MuSigArbitrationIssue>> issues = new Observable<>(List.of());
    private final Observable<Boolean> arbitratorHasLeftChat = new Observable<>(false);

    public MuSigArbitrationCase(MuSigArbitrationRequest muSigArbitrationRequest) {
        this(muSigArbitrationRequest,
                currentTimeMillis(),
                ArbitrationCaseState.OPEN,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false,
                List.of());
    }

    private MuSigArbitrationCase(MuSigArbitrationRequest muSigArbitrationRequest,
                                 long requestDate,
                                 ArbitrationCaseState arbitrationCaseState,
                                 Optional<MuSigArbitrationResult> muSigArbitrationResult,
                                 Optional<byte[]> arbitrationResultSignature,
                                 Optional<AccountPayload<?>> takerAccountPayload,
                                 Optional<AccountPayload<?>> makerAccountPayload,
                                 boolean arbitratorHasLeftChat,
                                 List<MuSigArbitrationIssue> issues) {
        this.muSigArbitrationRequest = muSigArbitrationRequest;
        this.requestDate = requestDate;
        this.arbitrationCaseState.set(arbitrationCaseState);
        this.muSigArbitrationResult.set(muSigArbitrationResult);
        this.arbitrationResultSignature = arbitrationResultSignature.map(byte[]::clone);
        this.takerAccountPayload.set(takerAccountPayload);
        this.makerAccountPayload.set(makerAccountPayload);
        this.arbitratorHasLeftChat.set(arbitratorHasLeftChat);
        this.issues.set(issues);
    }

    @Override
    public bisq.support.protobuf.MuSigArbitrationCase.Builder getBuilder(boolean serializeForHash) {
        bisq.support.protobuf.MuSigArbitrationCase.Builder builder = bisq.support.protobuf.MuSigArbitrationCase.newBuilder()
                .setMuSigArbitrationRequest(muSigArbitrationRequest.toValueProto(serializeForHash))
                .setRequestDate(requestDate)
                .setArbitrationCaseState(arbitrationCaseState.get().toProtoEnum());
        muSigArbitrationResult.get().ifPresent(item ->
                builder.setMuSigArbitrationResult(item.toProto(serializeForHash)));
        arbitrationResultSignature.ifPresent(item -> builder.setArbitrationResultSignature(ByteString.copyFrom(item)));
        takerAccountPayload.get().ifPresent(item -> builder.setTakerAccountPayload(item.toProto(serializeForHash)));
        makerAccountPayload.get().ifPresent(item -> builder.setMakerAccountPayload(item.toProto(serializeForHash)));
        builder.setArbitratorHasLeftChat(arbitratorHasLeftChat.get());
        builder.addAllIssues(issues.get().stream()
                .map(item -> item.toProto(serializeForHash))
                .toList());
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
                proto.hasMuSigArbitrationResult() ?
                        Optional.of(MuSigArbitrationResult.fromProto(proto.getMuSigArbitrationResult())) :
                        Optional.empty(),
                proto.hasArbitrationResultSignature() ?
                        Optional.of(proto.getArbitrationResultSignature().toByteArray()) :
                        Optional.empty(),
                proto.hasTakerAccountPayload() ?
                        Optional.of(AccountPayload.fromProto(proto.getTakerAccountPayload())) :
                        Optional.empty(),
                proto.hasMakerAccountPayload() ?
                        Optional.of(AccountPayload.fromProto(proto.getMakerAccountPayload())) :
                        Optional.empty(),
                proto.getArbitratorHasLeftChat(),
                proto.getIssuesList().stream()
                        .map(MuSigArbitrationIssue::fromProto)
                        .toList());
    }

    public boolean setArbitrationCaseState(ArbitrationCaseState state) {
        if (arbitrationCaseState.get() == state) {
            return false;
        }
        arbitrationCaseState.set(state);
        return true;
    }

    public ArbitrationCaseState getArbitrationCaseState() {
        return arbitrationCaseState.get();
    }

    public ReadOnlyObservable<ArbitrationCaseState> arbitrationCaseStateObservable() {
        return arbitrationCaseState;
    }

    public Optional<MuSigArbitrationResult> getMuSigArbitrationResult() {
        return muSigArbitrationResult.get();
    }

    public ReadOnlyObservable<Optional<MuSigArbitrationResult>> muSigArbitrationResultObservable() {
        return muSigArbitrationResult;
    }

    public Optional<byte[]> getArbitrationResultSignature() {
        return arbitrationResultSignature.map(byte[]::clone);
    }

    public Optional<AccountPayload<?>> getTakerAccountPayload() {
        return takerAccountPayload.get();
    }

    public ReadOnlyObservable<Optional<AccountPayload<?>>> takerAccountPayloadObservable() {
        return takerAccountPayload;
    }

    public Optional<AccountPayload<?>> getMakerAccountPayload() {
        return makerAccountPayload.get();
    }

    public ReadOnlyObservable<Optional<AccountPayload<?>>> makerAccountPayloadObservable() {
        return makerAccountPayload;
    }

    public List<MuSigArbitrationIssue> getIssues() {
        return issues.get();
    }

    public ReadOnlyObservable<List<MuSigArbitrationIssue>> issuesObservable() {
        return issues;
    }

    public boolean hasArbitratorLeftChat() {
        return arbitratorHasLeftChat.get();
    }

    public ReadOnlyObservable<Boolean> arbitratorHasLeftChatObservable() {
        return arbitratorHasLeftChat;
    }

    public boolean setArbitratorHasLeftChat(boolean arbitratorHasLeftChat) {
        return this.arbitratorHasLeftChat.set(arbitratorHasLeftChat);
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

    public boolean setTakerPaymentAccountPayload(AccountPayload<?> takerAccountPayload) {
        Optional<AccountPayload<?>> newValue = Optional.of(takerAccountPayload);
        if (this.takerAccountPayload.get().equals(newValue)) {
            return false;
        }
        this.takerAccountPayload.set(newValue);
        return true;
    }

    public boolean setMakerPaymentAccountPayload(AccountPayload<?> makerAccountPayload) {
        Optional<AccountPayload<?>> newValue = Optional.of(makerAccountPayload);
        if (this.makerAccountPayload.get().equals(newValue)) {
            return false;
        }
        this.makerAccountPayload.set(newValue);
        return true;
    }

    public synchronized boolean addIssues(List<MuSigArbitrationIssue> newIssues) {
        if (newIssues.isEmpty()) {
            return false;
        }
        List<MuSigArbitrationIssue> updated = new ArrayList<>(issues.get());
        boolean changed = false;
        for (MuSigArbitrationIssue issue : newIssues) {
            boolean alreadyPresent = updated.stream()
                    .anyMatch(existing -> existing.getCausingRole() == issue.getCausingRole()
                            && existing.getType() == issue.getType());
            if (!alreadyPresent) {
                updated.add(issue);
                changed = true;
            }
        }
        return changed && issues.set(List.copyOf(updated));
    }
}
