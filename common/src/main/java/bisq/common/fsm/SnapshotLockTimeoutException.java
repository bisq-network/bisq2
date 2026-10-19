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

/**
 * Thrown by {@link FsmModel#getStateAndEventQueueSnapshot()} when its bounded {@code tryLock} could not acquire
 * the model's lock in time, because a live {@link Fsm#handle(Event)}/{@link Fsm#drainEventQueue()} transition is
 * holding it. Callers taking snapshots for persistence must NOT catch this per entry - it should propagate and
 * fail the whole store write for that cycle, rather than silently dropping one entry from the snapshot while the
 * write still reports success. A later persist retries the write, by which time the transition holding the lock
 * has typically finished.
 */
public class SnapshotLockTimeoutException extends RuntimeException {
    public SnapshotLockTimeoutException(FsmModel model, long timeoutMs) {
        super("Could not acquire the FsmModel lock within " + timeoutMs + "ms while taking a state/eventQueue " +
                "snapshot for " + model.getClass().getSimpleName() + " - a live transition is likely holding it.");
    }

    public SnapshotLockTimeoutException(FsmModel model, long timeoutMs, Throwable cause) {
        super("Interrupted while waiting up to " + timeoutMs + "ms to acquire the FsmModel lock while taking a " +
                "state/eventQueue snapshot for " + model.getClass().getSimpleName(), cause);
    }
}
