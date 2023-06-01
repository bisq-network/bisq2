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

package bisq.protocol;

import bisq.common.observable.Observable;
import bisq.common.proto.Proto;
import bisq.contract.Contract;
import bisq.network.p2p.message.NetworkMessage;
import bisq.offer.Offer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j

public abstract class ProtocolModel<T extends Offer> implements Proto {
    public enum State {
        IDLE,
        PENDING,
        COMPLETED,
        FAILED;

        public boolean isPending() {
            return this == State.PENDING;
        }

        public boolean isCompleted() {
            return this == State.COMPLETED;
        }

        public boolean isFailed() {
            return this == State.FAILED;
        }

    }

    @Getter
    protected final Contract<T> contract;
    protected final Observable<State> stateAsObservable = new Observable<>(State.IDLE);
    @Setter
    protected Class<? extends NetworkMessage> expectedNextMessageClass;

    public ProtocolModel(Contract<T> contract) {
        this.contract = contract;
    }

    public abstract bisq.protocol.protobuf.ProtocolModel toProto();

    public static ProtocolModel<?> fromProto(bisq.protocol.protobuf.ProtocolModel proto) {
        return null;
    }

    void setStateAsObservable(State newState) {
        checkArgument(getState().ordinal() < newState.ordinal(),
                "New state %s must have a higher ordinal as the current state %s", newState, stateAsObservable);
        stateAsObservable.set(newState);
    }

    public boolean isPending() {
        return getState().isPending();
    }

    public boolean isCompleted() {
        return getState().isCompleted();
    }

    public boolean isFailed() {
        return getState().isFailed();
    }

    public State getState() {
        return stateAsObservable.get();
    }

    public String getId() {
        return getSwapOffer().getId();
    }

    public T getSwapOffer() {
        return contract.getOffer();
    }

    public Observable<State> getStateAsObservable() {
        return stateAsObservable;
    }
}