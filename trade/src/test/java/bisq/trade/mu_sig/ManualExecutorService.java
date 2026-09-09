/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.trade.mu_sig;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

// Queues submitted tasks instead of running them so a test decides when handler execution
// happens. Tasks run on the draining thread. Mirrors the production pool's contract: nothing is
// accepted after shutdown, and a task rejection can be forced.
class ManualExecutorService extends AbstractExecutorService {
    private static final int NEVER = Integer.MAX_VALUE;

    private final Deque<Runnable> queue = new ArrayDeque<>();
    private CountDownLatch submissionGate;
    private CountDownLatch submissionHeld;
    private boolean shutdown;
    private int submissions;
    private int heldFromSubmission = NEVER;
    private int rejectedFromSubmission = NEVER;
    private int running;

    @Override
    public void execute(Runnable command) {
        int submission;
        CountDownLatch gate = null;
        synchronized (this) {
            submission = ++submissions;
            if (submission >= heldFromSubmission) {
                gate = submissionGate;
                submissionHeld.countDown();
            }
        }
        if (gate != null) {
            await(gate);
        }
        synchronized (this) {
            if (shutdown || submission >= rejectedFromSubmission) {
                throw new RejectedExecutionException("Task rejected");
            }
            queue.add(command);
        }
    }

    // Lets a test hold a submission mid-flight to observe what the service does meanwhile. The
    // submissions are counted from this call: 1 holds the next one, 2 lets one through first.
    // The returned latch opens when a submission waits at the gate.
    synchronized CountDownLatch holdSubmissionsFrom(int submission, CountDownLatch submissionGate) {
        this.submissionGate = submissionGate;
        heldFromSubmission = submissions + submission;
        submissionHeld = new CountDownLatch(1);
        return submissionHeld;
    }

    private static void await(CountDownLatch gate) {
        try {
            if (!gate.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Submission gate was not released");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    // Counted from this call like holdSubmissionsFrom: 1 rejects the next submission.
    synchronized void rejectSubmissionsFrom(int submission) {
        rejectedFromSubmission = submissions + submission;
    }

    synchronized void acceptSubmissions() {
        rejectedFromSubmission = NEVER;
    }

    synchronized int pendingTasks() {
        return queue.size();
    }

    void drain() {
        Optional<Runnable> next;
        while ((next = take()).isPresent()) {
            try {
                next.get().run();
            } finally {
                synchronized (this) {
                    running--;
                    notifyAll();
                }
            }
        }
    }

    private synchronized Optional<Runnable> take() {
        Optional<Runnable> next = Optional.ofNullable(queue.poll());
        if (next.isPresent()) {
            running++;
        }
        return next;
    }

    @Override
    public synchronized void shutdown() {
        shutdown = true;
        notifyAll();
    }

    @Override
    public synchronized List<Runnable> shutdownNow() {
        shutdown = true;
        List<Runnable> remaining = List.copyOf(queue);
        queue.clear();
        notifyAll();
        return remaining;
    }

    @Override
    public synchronized boolean isShutdown() {
        return shutdown;
    }

    @Override
    public synchronized boolean isTerminated() {
        return shutdown && queue.isEmpty() && running == 0;
    }

    @Override
    public synchronized boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        long deadline = System.nanoTime() + unit.toNanos(timeout);
        while (!isTerminated()) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) {
                return false;
            }
            TimeUnit.NANOSECONDS.timedWait(this, remaining);
        }
        return true;
    }
}
