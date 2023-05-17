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

package bisq.tor;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class TorContext {
    public enum State {
        NEW,
        STARTING,
        RUNNING,
        STOPPING,
        TERMINATED;

        public boolean isStarting() {
            return this == State.STARTING;
        }

        public boolean isStartingOrRunning() {
            return this == State.STARTING || this == State.RUNNING;
        }
    }

    private final AtomicReference<State> state = new AtomicReference<>(State.NEW);

    public State getAndUpdateStateAtomically(UnaryOperator<State> updateFunction) {
        return state.getAndUpdate(updateFunction);
    }

    public State compareAndExchangeState(State expectedValue, State newValue) {
        return state.compareAndExchange(expectedValue, newValue);
    }

    public State getState() {
        return state.get();
    }

    public void setState(State newState) {
        state.getAndUpdate(previousState -> {
            log.info("Set new state {}", newState);
            checkArgument(newState.ordinal() > previousState.ordinal(),
                    "New state %s must have a higher ordinal as the current state %s",
                    newState, state.get());
            return newState;
        });
    }
}
