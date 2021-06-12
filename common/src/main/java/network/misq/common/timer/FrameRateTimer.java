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

package network.misq.common.timer;

/**
 * We simulate a global frame rate timer similar to FXTimer to avoid creation of threads for each timer call.
 * Used only in headless apps like the seed node.
 */
public class FrameRateTimer implements MisqTimer, Runnable {
    private long interval;
    private Runnable runnable;
    private long startTs;
    private boolean isPeriodically;
    private volatile boolean stopped;

    public FrameRateTimer() {
    }

    @Override
    public void run() {
        if (!stopped) {
            try {
                long currentTimeMillis = System.currentTimeMillis();
                if ((currentTimeMillis - startTs) >= interval) {
                    runnable.run();
                    if (isPeriodically) {
                        startTs = currentTimeMillis;
                    } else {
                        stop();
                    }
                }
            } catch (Throwable t) {
                t.printStackTrace();
                stop();
                throw t;
            }
        }
    }

    @Override
    public MisqTimer runLater(long delay, Runnable runnable) {
        return doRun(delay, runnable, false);
    }

    @Override
    public MisqTimer runPeriodically(long interval, Runnable runnable) {
        return doRun(interval, runnable, true);
    }

    private FrameRateTimer doRun(long interval, Runnable runnable, boolean isPeriodically) {
        this.interval = interval;
        this.runnable = runnable;
        this.isPeriodically = isPeriodically;
        startTs = System.currentTimeMillis();
        MasterTimer.addListener(this);
        return this;
    }

    @Override
    public void stop() {
        stopped = true;
        MasterTimer.removeListener(this);
    }
}
