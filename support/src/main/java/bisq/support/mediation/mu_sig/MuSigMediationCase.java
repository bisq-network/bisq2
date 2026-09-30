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
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.proto.PersistableProto;
import bisq.common.validation.NetworkDataValidation;
import bisq.support.mediation.MediationCaseState;
import com.google.protobuf.ByteString;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static java.lang.System.currentTimeMillis;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MuSigMediationCase implements PersistableProto {
    @EqualsAndHashCode.Include
    @Getter
    private final MuSigMediationRequest muSigMediationRequest;
    @Getter
    private final long requestDate;
    private final Observable<MediationCaseState> mediationCaseState = new Observable<>();
    private final Observable<Optional<MuSigMediationResult>> muSigMediationResult = new Observable<>();
    private Optional<byte[]> mediationResultSignature = Optional.empty();
    private Optional<byte[]> peerReportedContractHash = Optional.empty();
    private final Observable<Boolean> hasPeerReportedContractHash = new Observable<>(false);
    private final Observable<Optional<AccountPayload<?>>> takerAccountPayload = new Observable<>(Optional.empty());
    private final Observable<Optional<AccountPayload<?>>> makerAccountPayload = new Observable<>(Optional.empty());
    private final Observable<List<MuSigMediationIssue>> issues = new Observable<>(List.of());
    private final Observable<Boolean> mediatorHasLeftChat = new Observable<>(false);

    public MuSigMediationCase(MuSigMediationRequest muSigMediationRequest) {
        this(muSigMediationRequest,
                currentTimeMillis(),
                MediationCaseState.OPEN,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false,
                List.of());
    }

    private MuSigMediationCase(MuSigMediationRequest muSigMediationRequest,
                               long requestDate,
                               MediationCaseState mediationCaseState,
                               Optional<MuSigMediationResult> muSigMediationResult,
                               Optional<byte[]> mediationResultSignature,
                               Optional<byte[]> peerReportedContractHash,
                               Optional<AccountPayload<?>> takerAccountPayload,
                               Optional<AccountPayload<?>> makerAccountPayload,
                               boolean mediatorHasLeftChat,
                               List<MuSigMediationIssue> issues) {
        this.muSigMediationRequest = muSigMediationRequest;
        this.requestDate = requestDate;
        this.mediationCaseState.set(mediationCaseState);
        this.muSigMediationResult.set(muSigMediationResult);
        this.mediationResultSignature = mediationResultSignature.map(byte[]::clone);
        this.peerReportedContractHash = peerReportedContractHash.map(byte[]::clone);
        this.hasPeerReportedContractHash.set(peerReportedContractHash.isPresent());
        this.takerAccountPayload.set(takerAccountPayload);
        this.makerAccountPayload.set(makerAccountPayload);
        this.mediatorHasLeftChat.set(mediatorHasLeftChat);
        this.issues.set(issues);
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
        mediationResultSignature.ifPresent(item -> builder.setMediationResultSignature(ByteString.copyFrom(item)));
        peerReportedContractHash.ifPresent(item -> builder.setPeerReportedContractHash(ByteString.copyFrom(item)));
        takerAccountPayload.get().ifPresent(item -> builder.setTakerAccountPayload(item.toProto(serializeForHash)));
        makerAccountPayload.get().ifPresent(item -> builder.setMakerAccountPayload(item.toProto(serializeForHash)));
        builder.setMediatorHasLeftChat(mediatorHasLeftChat.get());
        builder.addAllIssues(issues.get().stream()
                .map(item -> item.toProto(serializeForHash))
                .toList());
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
                        Optional.empty(),
                proto.hasMediationResultSignature() ?
                        Optional.of(proto.getMediationResultSignature().toByteArray()) :
                        Optional.empty(),
                proto.hasPeerReportedContractHash() ?
                        Optional.of(proto.getPeerReportedContractHash().toByteArray()) :
                        Optional.empty(),
                proto.hasTakerAccountPayload() ?
                        Optional.of(AccountPayload.fromProto(proto.getTakerAccountPayload())) :
                        Optional.empty(),
                proto.hasMakerAccountPayload() ?
                        Optional.of(AccountPayload.fromProto(proto.getMakerAccountPayload())) :
                        Optional.empty(),
                proto.getMediatorHasLeftChat(),
                proto.getIssuesList().stream()
                        .map(MuSigMediationIssue::fromProto)
                        .toList());
    }

    public boolean setMediationCaseState(MediationCaseState state) {
        if (mediationCaseState.get() == state) {
            return false;
        }
        mediationCaseState.set(state);
        return true;
    }

    public Optional<byte[]> getMediationResultSignature() {
        return mediationResultSignature.map(byte[]::clone);
    }

    public Optional<byte[]> getPeerReportedContractHash() {
        return peerReportedContractHash.map(byte[]::clone);
    }

    public ReadOnlyObservable<Boolean> hasPeerReportedContractHashObservable() {
        return hasPeerReportedContractHash;
    }

    public MediationCaseState getMediationCaseState() {
        return mediationCaseState.get();
    }

    public ReadOnlyObservable<MediationCaseState> mediationCaseStateObservable() {
        return mediationCaseState;
    }

    public Optional<MuSigMediationResult> getMuSigMediationResult() {
        return muSigMediationResult.get();
    }

    public ReadOnlyObservable<Optional<MuSigMediationResult>> muSigMediationResultObservable() {
        return muSigMediationResult;
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

    public List<MuSigMediationIssue> getIssues() {
        return issues.get();
    }

    public ReadOnlyObservable<List<MuSigMediationIssue>> issuesObservable() {
        return issues;
    }

    public boolean hasMediatorLeftChat() {
        return mediatorHasLeftChat.get();
    }

    public ReadOnlyObservable<Boolean> mediatorHasLeftChatObservable() {
        return mediatorHasLeftChat;
    }

    public boolean setMediatorHasLeftChat(boolean mediatorHasLeftChat) {
        return this.mediatorHasLeftChat.set(mediatorHasLeftChat);
    }

    public boolean setSignedMuSigMediationResult(MuSigMediationResult result, byte[] signature) {
        NetworkDataValidation.validateECSignature(signature);
        byte[] signatureCopy = signature.clone();
        Optional<MuSigMediationResult> currentResult = muSigMediationResult.get();
        if (currentResult.isPresent() && !currentResult.orElseThrow().equals(result)) {
            throw new IllegalArgumentException("MuSigMediationResult cannot be changed once set.");
        }
        Optional<byte[]> currentSignature = mediationResultSignature;
        if (currentSignature.isPresent() && !Arrays.equals(currentSignature.orElseThrow(), signatureCopy)) {
            throw new IllegalArgumentException("mediationResultSignature cannot be changed once set.");
        }
        if (currentResult.isPresent() && currentSignature.isPresent()) {
            return false;
        }
        if (currentResult.isEmpty()) {
            muSigMediationResult.set(Optional.of(result));
        }
        if (currentSignature.isEmpty()) {
            mediationResultSignature = Optional.of(signatureCopy);
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

    public boolean setPeerReportedContractHash(byte[] peerReportedContractHash) {
        NetworkDataValidation.validateHash(peerReportedContractHash);
        byte[] hashCopy = peerReportedContractHash.clone();
        Optional<byte[]> currentHash = this.peerReportedContractHash;
        if (currentHash.isPresent() && !Arrays.equals(currentHash.orElseThrow(), hashCopy)) {
            throw new IllegalArgumentException("peerReportedContractHash cannot be changed once set.");
        }
        if (currentHash.isPresent()) {
            return false;
        }
        this.peerReportedContractHash = Optional.of(hashCopy);
        return hasPeerReportedContractHash.set(true);
    }

    public boolean addIssues(List<MuSigMediationIssue> newIssues) {
        if (newIssues.isEmpty()) {
            return false;
        }
        List<MuSigMediationIssue> updated = new ArrayList<>(issues.get());
        boolean changed = false;
        for (MuSigMediationIssue issue : newIssues) {
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
