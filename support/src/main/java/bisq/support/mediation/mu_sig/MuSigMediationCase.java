package bisq.support.mediation.mu_sig;

import bisq.common.observable.Observable;
import bisq.common.proto.PersistableProto;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Optional;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MuSigMediationCase implements PersistableProto {
    @EqualsAndHashCode.Include
    private final MuSigMediationRequest muSigMediationRequest;
    private final long requestDate;
    private final Observable<Boolean> isClosed = new Observable<>();
    private Optional<Long> closeCaseDate;

    public MuSigMediationCase(MuSigMediationRequest muSigMediationRequest) {
        this(muSigMediationRequest, System.currentTimeMillis(), false, Optional.empty());
    }

    private MuSigMediationCase(MuSigMediationRequest muSigMediationRequest,
                               long requestDate,
                               boolean isClosed,
                               Optional<Long> closeCaseDate) {
        this.muSigMediationRequest = muSigMediationRequest;
        this.requestDate = requestDate;
        this.isClosed.set(isClosed);
        this.closeCaseDate = closeCaseDate;
    }

    /**
     * Keep proto name for backward compatibility
     */

    @Override
    public bisq.support.protobuf.MuSigMediationCase.Builder getBuilder(boolean serializeForHash) {
        bisq.support.protobuf.MuSigMediationCase.Builder builder = bisq.support.protobuf.MuSigMediationCase.newBuilder()
                .setMuSigMediationRequest(muSigMediationRequest.toValueProto(serializeForHash))
                .setRequestDate(requestDate)
                .setIsClosed(isClosed.get());
        closeCaseDate.ifPresent(builder::setCloseCaseDate);
        return builder;
    }

    @Override
    public bisq.support.protobuf.MuSigMediationCase toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }


    public static MuSigMediationCase fromProto(bisq.support.protobuf.MuSigMediationCase proto) {
        return new MuSigMediationCase(MuSigMediationRequest.fromProto(proto.getMuSigMediationRequest()),
                proto.getRequestDate(),
                proto.getIsClosed(),
                proto.hasCloseCaseDate() ? Optional.of(proto.getCloseCaseDate()) : Optional.empty());
    }

    public boolean setClosed(boolean closed) {
        boolean changed = isClosed.set(closed);
        if (changed) {
            closeCaseDate = closed ? Optional.of(System.currentTimeMillis()) : Optional.empty();
        }
        return changed;
    }
}
