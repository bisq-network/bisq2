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

package bisq.resilience_test.test;

import bisq.common.timer.Scheduler;
import com.typesafe.config.Config;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;


@Slf4j
public abstract class BaseTestCase {
    @Nullable
    private Scheduler scheduler;
    @Getter
    boolean isEnabled = false;
    private int initialDelaySecs = 10;
    private int intervalDelaySecs = 30;

    public BaseTestCase(Optional<Config> optionalConfig) {
        optionalConfig.ifPresent(config -> {
            isEnabled = config.hasPath("enabled") && config.getBoolean("enabled");
            if (config.hasPath("initialDelaySecs")) {
                initialDelaySecs = config.getInt("initialDelaySecs");
                if (initialDelaySecs < 0) {
                    throw new IllegalArgumentException("initialDelaySecs must be non-negative");
                }
            }
            if (config.hasPath("intervalDelaySecs")) {
                intervalDelaySecs = config.getInt("intervalDelaySecs");
                if (intervalDelaySecs < 0) {
                    throw new IllegalArgumentException("initialDelaySecs must be non-negative");
                }
            }
        });
    }

    public void start() {
        if (!isEnabled) {
            log.info("{} is disabled, skipping start.", this.getClass().getSimpleName());
            return;
        }
        if (scheduler == null) {
            scheduler = Scheduler.run(this::run)
                    .periodically(initialDelaySecs, intervalDelaySecs, TimeUnit.SECONDS);
        }
    }

    public CompletableFuture<Boolean> shutdown() {
        if (scheduler != null) {
            scheduler.stop();
            scheduler = null;
        }
        return CompletableFuture.completedFuture(true);
    }

    protected abstract void run();
}
