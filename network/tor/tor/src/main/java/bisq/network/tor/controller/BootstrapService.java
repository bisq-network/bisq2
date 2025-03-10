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

package bisq.network.tor.controller;

import bisq.common.observable.Observable;
import bisq.network.tor.controller.events.events.TorBootstrapEvent;
import bisq.network.tor.controller.events.events.EventType;
import bisq.network.tor.controller.events.listener.BootstrapEventListener;
import bisq.network.tor.controller.exceptions.TorBootstrapFailedException;
import bisq.network.tor.process.EmbeddedTorProcess;
import com.google.common.util.concurrent.MoreExecutors;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static bisq.network.tor.common.torrc.Torrc.Keys.DISABLE_NETWORK;

@Slf4j
public class BootstrapService extends BootstrapEventListener {
    private final TorControlProtocol torControlProtocol;
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private final long timeout;
    private final Observable<TorBootstrapEvent> bootstrapEvent;
    @Getter
    private Optional<CompletableFuture<Void>> future = Optional.empty();

    public BootstrapService(TorControlProtocol torControlProtocol,
                            long timeout,
                            Observable<TorBootstrapEvent> bootstrapEvent) {
        super(EventType.STATUS_CLIENT);
        this.torControlProtocol = torControlProtocol;
        this.timeout = timeout;
        this.bootstrapEvent = bootstrapEvent;
    }

    public CompletableFuture<Void> bootstrap() {
        future = Optional.of(CompletableFuture.runAsync(() -> {
                    torControlProtocol.addBootstrapEventListener(this);

                    torControlProtocol.takeOwnership();
                    torControlProtocol.resetConf(EmbeddedTorProcess.ARG_OWNER_PID);
                    torControlProtocol.setConfig(DISABLE_NETWORK, "0");

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
    public void onBootstrapStatusEvent(TorBootstrapEvent torBootstrapEvent) {
        log.info("Tor bootstrap event: {}", torBootstrapEvent);
        this.bootstrapEvent.set(torBootstrapEvent);
        if (torBootstrapEvent.isDoneEvent()) {
            countDownLatch.countDown();
        }
    }

    //todo Why do we check for the last timestamp at timeouts?
    private boolean isBootstrapTimeoutTriggered() {
        TorBootstrapEvent torBootstrapEvent = this.bootstrapEvent.get();
        Instant timestamp = torBootstrapEvent.getTimestamp();
        Instant bootstrapTimeoutAgo = Instant.now().minus(timeout, ChronoUnit.MILLIS);
        return bootstrapTimeoutAgo.isAfter(timestamp);
    }
}
