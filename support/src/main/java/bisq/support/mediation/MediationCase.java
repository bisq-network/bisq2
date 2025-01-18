package bisq.support.mediation;

import bisq.common.observable.Observable;
import bisq.common.proto.PersistableProto;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Date;
import java.util.Optional;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MediationCase implements PersistableProto {
    @EqualsAndHashCode.Include
    private final MediationRequest mediationRequest;
    private final long requestDate;
    private final Observable<Boolean> isClosed = new Observable<>();
    private Optional<Long> closeCaseDate;

    public MediationCase(MediationRequest mediationRequest) {
        this(mediationRequest, new Date().getTime(), false, Optional.empty());
    }

    private MediationCase(MediationRequest mediationRequest,
                          long requestDate,
                          boolean isClosed,
                          Optional<Long> closeCaseDate) {
        this.mediationRequest = mediationRequest;
        this.requestDate = requestDate;
        this.isClosed.set(isClosed);
        this.closeCaseDate = closeCaseDate;
    }

    @Override
    public bisq.support.protobuf.MediationCase.Builder getBuilder(boolean serializeForHash) {
        bisq.support.protobuf.MediationCase.Builder builder = bisq.support.protobuf.MediationCase.newBuilder()
                .setMediationRequest(mediationRequest.toValueProto(serializeForHash))
                .setRequestDate(requestDate)
                .setIsClosed(isClosed.get());
        closeCaseDate.ifPresent(builder::setCloseCaseDate);
        return builder;
    }

    @Override
    public bisq.support.protobuf.MediationCase toProto(boolean serializeForHash) {
        return resolveProto(serializeForHash);
    }


    public static MediationCase fromProto(bisq.support.protobuf.MediationCase proto) {
        return new MediationCase(MediationRequest.fromProto(proto.getMediationRequest()),
                proto.getRequestDate(),
                proto.getIsClosed(),
                proto.hasCloseCaseDate() ? Optional.of(proto.getCloseCaseDate()) : Optional.empty());
    }

    public void setClosed(boolean closed) {
        closeCaseDate = closed ? Optional.of(new Date().getTime()) : Optional.empty();
        isClosed.set(closed);
    }
}
