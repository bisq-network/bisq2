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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@ToString
@EqualsAndHashCode
public class FsmModel {
    private final Observable<State> state = new Observable<>();

    // Package visibility for access from Fsm mutating the collections 
    final Set<Event> eventQueue = new HashSet<>();
    final Set<Class<? extends Event>> processedEvents = new HashSet<>();

    public FsmModel(State initialState) {
        if (initialState == null) {
            throw new FsmConfigException("initialState must not be null");
        }
        state.set(initialState);
    }

    public FsmModel(State initialState, Set<Event> eventQueue, Set<Class<? extends Event>> processedEvents) {
        if (initialState == null) {
            throw new FsmConfigException("initialState must not be null");
        }
        state.set(initialState);
        this.eventQueue.addAll(eventQueue);
        this.processedEvents.addAll(processedEvents);
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

    public Set<Event> getEventQueue() {
        return Collections.unmodifiableSet(eventQueue);
    }

    public Set<Class<? extends Event>> getProcessedEvents() {
        return Collections.unmodifiableSet(processedEvents);
    }
}