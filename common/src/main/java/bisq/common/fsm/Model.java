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

package bisq.common.fsm;

import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ToString
@EqualsAndHashCode
public class Model {
    private final Observable<State> state = new Observable<>();

    public Model(State initialState) {
        if (initialState == null) {
            throw new FsmException("InitialState must not be null at Model constructor");
        }
        state.set(initialState);
    }

    public ReadOnlyObservable<State> stateObservable() {
        return state;
    }

    public State getState() {
        return state.get();
    }

    // Only called from FSM
    void setNewState(State newState) {
        state.set(newState);
    }
}