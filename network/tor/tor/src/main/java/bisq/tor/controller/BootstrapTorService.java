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
import bisq.tor.controller.events.listener.BootstrapEventListener;
import bisq.tor.controller.exceptions.TorBootstrapFailedException;
import bisq.tor.process.NativeTorProcess;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class BootstrapTorService implements BootstrapEventListener {
    private final TorControlProtocol torControlProtocol;
    private final CountDownLatch isBootstrappedCountdownLatch = new CountDownLatch(1);
    private final int bootstrapTimeout; // in ms
    private final Observable<BootstrapEvent> bootstrapEvent;

    public BootstrapTorService(TorControlProtocol torControlProtocol, int bootstrapTimeout, Observable<BootstrapEvent> bootstrapEvent) {
        this.torControlProtocol = torControlProtocol;
        this.bootstrapTimeout = bootstrapTimeout;
        this.bootstrapEvent = bootstrapEvent;
    }

    public void bootstrapTor() {
        bindToBisq();
        subscribeToBootstrapEvents();
        enableNetworking();
        waitUntilBootstrapped();
    }

    @Override
    public void onBootstrapStatusEvent(BootstrapEvent bootstrapEvent) {
        log.info("Tor bootstrap event: {}", bootstrapEvent);
        this.bootstrapEvent.set(bootstrapEvent);
        if (bootstrapEvent.isDoneEvent()) {
            isBootstrappedCountdownLatch.countDown();
        }
    }

    private void bindToBisq() {
        torControlProtocol.takeOwnership();
        torControlProtocol.resetConf(NativeTorProcess.ARG_OWNER_PID);
    }

    private void subscribeToBootstrapEvents() {
        torControlProtocol.addBootstrapEventListener(this);
        torControlProtocol.setEvents(List.of("STATUS_CLIENT"));
    }

    private void enableNetworking() {
        torControlProtocol.setConfig(TorrcClientConfigFactory.DISABLE_NETWORK_CONFIG_KEY, "0");
    }

    private void waitUntilBootstrapped() {
        try {
            while (true) {
                boolean isSuccess = isBootstrappedCountdownLatch.await(bootstrapTimeout, TimeUnit.MILLISECONDS);
                if (isSuccess) {
                    torControlProtocol.removeBootstrapEventListener(this);
                    torControlProtocol.setEvents(Collections.emptyList());
                    break;
                } else if (isBootstrapTimeoutTriggered()) {
                    throw new TorBootstrapFailedException("Tor bootstrap timeout triggered.");
                }
            }
        } catch (InterruptedException e) {
            throw new TorBootstrapFailedException(e);
        }
    }

    private boolean isBootstrapTimeoutTriggered() {
        BootstrapEvent bootstrapEvent = this.bootstrapEvent.get();
        Instant timestamp = bootstrapEvent.getTimestamp();
        Instant bootstrapTimeoutAgo = Instant.now().minus(bootstrapTimeout, ChronoUnit.MILLIS);
        return bootstrapTimeoutAgo.isAfter(timestamp);
    }
}
