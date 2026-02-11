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
import bisq.common.proto.PersistableProto;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.util.Optional;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class BisqEasyMediationCase implements PersistableProto {
    @EqualsAndHashCode.Include
    private final BisqEasyMediationRequest bisqEasyMediationRequest;
    private final long requestDate;
    private final Observable<Boolean> isClosed = new Observable<>();
    private Optional<Long> closeCaseDate;

    public BisqEasyMediationCase(BisqEasyMediationRequest bisqEasyMediationRequest) {
        this(bisqEasyMediationRequest, System.currentTimeMillis(), false, Optional.empty());
    }

    private BisqEasyMediationCase(BisqEasyMediationRequest bisqEasyMediationRequest,
                                  long requestDate,
                                  boolean isClosed,
                                  Optional<Long> closeCaseDate) {
        this.bisqEasyMediationRequest = bisqEasyMediationRequest;
        this.requestDate = requestDate;
        this.isClosed.set(isClosed);
        this.closeCaseDate = closeCaseDate;
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
        return builder;
    }

    @Override
    public bisq.support.protobuf.MediationCase toProto(boolean serializeForHash) {
        return unsafeToProto(serializeForHash);
    }


    public static BisqEasyMediationCase fromProto(bisq.support.protobuf.MediationCase proto) {
        return new BisqEasyMediationCase(BisqEasyMediationRequest.fromProto(proto.getMediationRequest()),
                proto.getRequestDate(),
                proto.getIsClosed(),
                proto.hasCloseCaseDate() ? Optional.of(proto.getCloseCaseDate()) : Optional.empty());
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
