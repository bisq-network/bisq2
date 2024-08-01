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

package bisq.tor.controller;

import bisq.common.observable.Observable;
import bisq.tor.TorrcClientConfigFactory;
import bisq.tor.controller.events.events.BootstrapEvent;
import bisq.tor.controller.events.events.EventType;
import bisq.tor.controller.events.listener.BootstrapEventListener;
import bisq.tor.controller.exceptions.TorBootstrapFailedException;
import bisq.tor.process.NativeTorProcess;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class BootstrapService extends BootstrapEventListener {
    private final TorControlProtocol torControlProtocol;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private final long timeout;
    private final Observable<BootstrapEvent> bootstrapEvent;
    @Getter
    private Optional<CompletableFuture<Void>> future = Optional.empty();

    public BootstrapService(TorControlProtocol torControlProtocol, long timeout, Observable<BootstrapEvent> bootstrapEvent) {
        super(EventType.STATUS_CLIENT);
        this.torControlProtocol = torControlProtocol;
        this.timeout = timeout;
        this.bootstrapEvent = bootstrapEvent;
    }

    public CompletableFuture<Void> bootstrap() {
        future = Optional.of(CompletableFuture.runAsync(() -> {
                    torControlProtocol.takeOwnership();
                    torControlProtocol.resetConf(NativeTorProcess.ARG_OWNER_PID);

                    torControlProtocol.addBootstrapEventListener(this);

                    torControlProtocol.setConfig(TorrcClientConfigFactory.DISABLE_NETWORK_CONFIG_KEY, "0");

                    try {
                        boolean isSuccess = countDownLatch.await(timeout, TimeUnit.MILLISECONDS);
                        if (!isSuccess) {
                            throw new TorBootstrapFailedException("Could not bootstrap Tor in " + timeout / 1000 + " seconds");
                        }
                    } catch (InterruptedException e) {
                        throw new TorBootstrapFailedException(e);
                    }
                }, MoreExecutors.directExecutor())
                .whenComplete((nil, throwable) ->
                        torControlProtocol.removeBootstrapEventListener(this)));
        return future.get();
    }

    public void shutdown() {
        torControlProtocol.removeBootstrapEventListener(this);
        if (countDownLatch.getCount() > 0) {
            countDownLatch.countDown();
        }
    }

    @Override
    public void onBootstrapStatusEvent(BootstrapEvent bootstrapEvent) {
        log.info("Tor bootstrap event: {}", bootstrapEvent);
        this.bootstrapEvent.set(bootstrapEvent);
        if (bootstrapEvent.isDoneEvent()) {
            countDownLatch.countDown();
        }
    }

    //todo Why do we check for the last timestamp at timeouts?
    private boolean isBootstrapTimeoutTriggered() {
        BootstrapEvent bootstrapEvent = this.bootstrapEvent.get();
        Instant timestamp = bootstrapEvent.getTimestamp();
        Instant bootstrapTimeoutAgo = Instant.now().minus(timeout, ChronoUnit.MILLIS);
        return bootstrapTimeoutAgo.isAfter(timestamp);
    }
}
