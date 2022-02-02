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

import bisq.contract.Contract;
import bisq.network.p2p.message.Message;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
@Getter
public abstract class ProtocolModel implements Serializable {
    public enum State {
        IDLE,
        PENDING,
        COMPLETED,
        FAILED
    }

    protected Contract contract;

    protected State state = State.IDLE;
    @Setter
    protected Class<? extends Message> expectedNextMessageClass;

    public ProtocolModel(Contract contract) {
        this.contract = contract;
    }

  /*  public void applyPersisted(ProtocolModel persisted) {
        log.error("applyPersisted {}", persisted);
        contract = persisted.getContract();
        state = persisted.getState();
        expectedNextMessageClass = persisted.getExpectedNextMessageClass();
    }*/

    void setState(State newState) {
        checkArgument(state.ordinal() < newState.ordinal(),
                "New state %s must have a higher ordinal as the current state %s", newState, state);
        state = newState;
    }

    public boolean isPending() {
        return state == State.PENDING;
    }

    public boolean isCompleted() {
        return state == State.COMPLETED;
    }

    public boolean isFailed() {
        return state == State.FAILED;
    }

    public String getId() {
        return contract.getOffer().getId();
    }
}