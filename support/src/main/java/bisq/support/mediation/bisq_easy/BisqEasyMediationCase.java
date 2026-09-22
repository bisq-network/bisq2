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

package bisq.support.mediation.bisq_easy;

import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.proto.PersistableProto;
import bisq.common.proto.UnresolvableProtobufException;
import bisq.contract.bisq_easy.BisqEasyContract;
import lombok.Getter;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

public class BisqEasyMediationCase implements PersistableProto {
    @Getter
    private final BisqEasyMediationRequest bisqEasyMediationRequest;
    @Getter
    private final long requestDate;
    private final Observable<Boolean> isClosed = new Observable<>();
    @Getter
    private Optional<Long> closeCaseDate;
    // Request of the other trader for the same contract, kept so that both transcripts are on record
    private Optional<BisqEasyMediationRequest> peersMediationRequest;

    public BisqEasyMediationCase(BisqEasyMediationRequest bisqEasyMediationRequest) {
        this(bisqEasyMediationRequest, System.currentTimeMillis(), false, Optional.empty(), Optional.empty());
    }

    private BisqEasyMediationCase(BisqEasyMediationRequest bisqEasyMediationRequest,
                                  long requestDate,
                                  boolean isClosed,
                                  Optional<Long> closeCaseDate,
                                  Optional<BisqEasyMediationRequest> peersMediationRequest) {
        this.bisqEasyMediationRequest = bisqEasyMediationRequest;
        this.requestDate = requestDate;
        this.isClosed.set(isClosed);
        this.closeCaseDate = closeCaseDate;
        this.peersMediationRequest = peersMediationRequest;
    }

    /**
     * Keep proto name for backward compatibility
     */

    @Override
    public bisq.support.protobuf.MediationCase.Builder getBuilder(boolean serializeForHash) {
        bisq.support.protobuf.MediationCase.Builder builder = bisq.support.protobuf.MediationCase.newBuilder()
                .setMediationRequest(bisqEasyMediationRequest.toValueProto(serializeForHash))
                .setRequestDate(requestDate)
                .setIsClosed(isClosed.get());
        closeCaseDate.ifPresent(builder::setCloseCaseDate);
        peersMediationRequest.ifPresent(request -> builder.setPeersMediationRequest(request.toValueProto(serializeForHash)));
        return builder;
    }

    @Override
    public bisq.support.protobuf.MediationCase toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }


    public static BisqEasyMediationCase fromProto(bisq.support.protobuf.MediationCase proto)
            throws UnresolvableProtobufException {
        try {
            return new BisqEasyMediationCase(BisqEasyMediationRequest.fromProto(proto.getMediationRequest()),
                    proto.getRequestDate(),
                    proto.getIsClosed(),
                    proto.hasCloseCaseDate() ? Optional.of(proto.getCloseCaseDate()) : Optional.empty(),
                    proto.hasPeersMediationRequest() ? Optional.of(BisqEasyMediationRequest.fromProto(proto.getPeersMediationRequest())) : Optional.empty());
        } catch (RuntimeException e) {
            throw new UnresolvableProtobufException(e);
        }
    }

    public List<BisqEasyMediationRequest> getMediationRequests() {
        return Stream.concat(Stream.of(bisqEasyMediationRequest), peersMediationRequest.stream()).toList();
    }

    // A repeated request from a trader already on record replaces that trader's retained request, so that the
    // retained provenance covers the transcript the mediator imports.
    public BisqEasyMediationCase withRequest(BisqEasyMediationRequest request) {
        String requesterId = request.getRequester().getId();
        if (requesterId.equals(bisqEasyMediationRequest.getRequester().getId())) {
            return new BisqEasyMediationCase(request, requestDate, isClosed.get(), closeCaseDate, peersMediationRequest);
        }
        if (peersMediationRequest.map(peersRequest -> peersRequest.getRequester().getId().equals(requesterId)).orElse(false)) {
            return new BisqEasyMediationCase(bisqEasyMediationRequest, requestDate, isClosed.get(), closeCaseDate, Optional.of(request));
        }
        return this;
    }

    public boolean setPeersMediationRequest(BisqEasyMediationRequest request) {
        boolean isFromPeer = !request.getRequester().getId().equals(bisqEasyMediationRequest.getRequester().getId());
        if (!isFromPeer || peersMediationRequest.isPresent()) {
            return false;
        }
        peersMediationRequest = Optional.of(request);
        return true;
    }

    public boolean isClosed() {
        return isClosed.get();
    }

    public ReadOnlyObservable<Boolean> isClosedObservable() {
        return isClosed;
    }

    public String getTradeId() {
        return bisqEasyMediationRequest.getTradeId();
    }

    public BisqEasyContract getContract() {
        return bisqEasyMediationRequest.getContract();
    }

    // A case is identified by the trade and the contract, whichever trader requested it.
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BisqEasyMediationCase other)) {
            return false;
        }
        return getTradeId().equals(other.getTradeId()) && getContract().equals(other.getContract());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getTradeId(), getContract());
    }

    public boolean setClosed(boolean closed) {
        if (isClosed.get() == closed) {
            return false;
        }
        closeCaseDate = closed ? Optional.of(System.currentTimeMillis()) : Optional.empty();
        isClosed.set(closed);
        return true;
    }
}
