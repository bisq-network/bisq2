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

package bisq.common.application;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static bisq.common.application.LifecycleService.State.ACTIVATED;
import static bisq.common.application.LifecycleService.State.ACTIVATING;
import static bisq.common.application.LifecycleService.State.ACTIVATING_FAILED;
import static bisq.common.application.LifecycleService.State.DEACTIVATED;
import static bisq.common.application.LifecycleService.State.DEACTIVATING;
import static bisq.common.application.LifecycleService.State.DEACTIVATING_FAILED;
import static com.google.common.base.Preconditions.checkState;

@Slf4j
public abstract class LifecycleService implements Service {
    public enum State {
        ACTIVATING,
        ACTIVATING_FAILED,
        ACTIVATED,
        DEACTIVATING,
        DEACTIVATING_FAILED,
        DEACTIVATED
    }

    private final AtomicReference<State> state = new AtomicReference<>(DEACTIVATED);
    @Setter
    protected long activateTimeoutMs = 5000;
    @Setter
    protected long deactivateTimeoutMs = 5000;

    /**
     * Activates the service.
     * Allowed only if current state is DEACTIVATED or DEACTIVATING_FAILED.
     *
     * @return a CompletableFuture completing with true if activation succeeded.
     */
    public CompletableFuture<Boolean> activate() {
        checkState(state.compareAndSet(DEACTIVATED, ACTIVATING) || state.compareAndSet(DEACTIVATING_FAILED, ACTIVATING),
                "When calling activate expected state is DEACTIVATED or DEACTIVATING_FAILED, but state was {}", state.get());
        return doActivate()
                .orTimeout(activateTimeoutMs, TimeUnit.MILLISECONDS)
                .whenComplete((result, throwable) -> {
                    if (Boolean.TRUE.equals(result)) {
                        state.set(ACTIVATED);
                    } else {
                        if (throwable == null) {
                            log.error("Activation failed with result=false");
                        } else {
                            log.error("Activation failed with exception", throwable);
                        }
                        state.set(ACTIVATING_FAILED);
                    }
                });
    }

    /**
     * Deactivates the service.
     * Allowed only if current state is ACTIVATED or ACTIVATING_FAILED.
     *
     * @return a CompletableFuture completing with true if deactivation succeeded.
     */
    public CompletableFuture<Boolean> deactivate() {
        checkState(state.compareAndSet(ACTIVATED, DEACTIVATING) || state.compareAndSet(ACTIVATING_FAILED, DEACTIVATING),
                "When calling deactivate expected state is ACTIVATED or ACTIVATING_FAILED, but state was {}", state.get());
        return doDeactivate()
                .orTimeout(deactivateTimeoutMs, TimeUnit.MILLISECONDS)
                .whenComplete((result, throwable) -> {
                    if (Boolean.TRUE.equals(result)) {
                        state.set(DEACTIVATED);
                    } else {
                        if (throwable == null) {
                            log.error("Deactivation failed with result=false");
                        } else {
                            log.error("Deactivation failed with exception", throwable);
                        }
                        state.set(DEACTIVATING_FAILED);
                    }
                });
    }

    public State getState() {
        return state.get();
    }

    public boolean canRetryActivate() {
        return getState() == DEACTIVATED || getState() == DEACTIVATING_FAILED;
    }

    public void resetStateToAllowActivate() {
        state.set(DEACTIVATED);
    }

    public void resetStateToAllowDeactivate() {
        state.set(ACTIVATED);
    }

    protected abstract CompletableFuture<Boolean> doActivate();

    protected abstract CompletableFuture<Boolean> doDeactivate();
}
