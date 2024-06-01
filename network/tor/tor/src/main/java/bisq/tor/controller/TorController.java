package bisq.tor.controller;

import bisq.common.observable.Observable;
import bisq.tor.TorrcClientConfigFactory;
import bisq.tor.controller.events.events.BootstrapEvent;
import bisq.tor.controller.events.listener.BootstrapEventListener;
import bisq.tor.controller.exceptions.TorBootstrapFailedException;
import bisq.tor.process.NativeTorProcess;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.freehaven.tor.control.PasswordDigest;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TorController implements BootstrapEventListener {
    private final int bootstrapTimeout;
    private final CountDownLatch isBootstrappedCountdownLatch = new CountDownLatch(1);
    @Getter
    private final Observable<BootstrapEvent> bootstrapEvent = new Observable<>();

    private Optional<TorControlProtocol> torControlProtocol = Optional.empty();

    public TorController(int bootstrapTimeout) {
        this.bootstrapTimeout = bootstrapTimeout;
    }

    public void initialize(int controlPort, PasswordDigest hashedControlPassword) throws IOException {
        var torControlProtocol = new TorControlProtocol(controlPort);
        this.torControlProtocol = Optional.of(torControlProtocol);

        torControlProtocol.initialize();
        torControlProtocol.authenticate(hashedControlPassword);
    }

    public void shutdown() {
        torControlProtocol.ifPresent(torControlProtocol -> {
            try {
                torControlProtocol.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public void bootstrapTor() throws IOException {
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

    private void bindToBisq() throws IOException {
        TorControlProtocol torControlProtocol = getTorControlProtocol();
        torControlProtocol.takeOwnership();
        torControlProtocol.resetConf(NativeTorProcess.ARG_OWNER_PID);
    }

    private void subscribeToBootstrapEvents() throws IOException {
        TorControlProtocol torControlProtocol = getTorControlProtocol();
        torControlProtocol.addBootstrapEventListener(this);
        torControlProtocol.setEvents(List.of("STATUS_CLIENT"));
    }

    private void enableNetworking() throws IOException {
        TorControlProtocol torControlProtocol = getTorControlProtocol();
        torControlProtocol.setConfig(TorrcClientConfigFactory.DISABLE_NETWORK_CONFIG_KEY, "0");
    }

    private void waitUntilBootstrapped() {
        try {
            while (true) {
                if (torControlProtocol.isEmpty()) {
                    throw new TorBootstrapFailedException("Tor is not initializing.");
                }

                boolean isSuccess = isBootstrappedCountdownLatch.await(bootstrapTimeout, TimeUnit.MILLISECONDS);

                if (isSuccess) {
                    TorControlProtocol torControlProtocol = this.torControlProtocol.get();
                    torControlProtocol.removeBootstrapEventListener(this);
                    torControlProtocol.setEvents(Collections.emptyList());
                    break;
                } else if (isBootstrapTimeoutTriggered()) {
                    throw new TorBootstrapFailedException("Tor bootstrap timeout triggered.");
                }
            }
        } catch (InterruptedException e) {
            throw new TorBootstrapFailedException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isBootstrapTimeoutTriggered() {
        BootstrapEvent bootstrapEvent = this.bootstrapEvent.get();
        Instant timestamp = bootstrapEvent.getTimestamp();
        Instant bootstrapTimeoutAgo = Instant.now().minus(bootstrapTimeout, ChronoUnit.MILLIS);
        return bootstrapTimeoutAgo.isAfter(timestamp);
    }

    private TorControlProtocol getTorControlProtocol() {
        return this.torControlProtocol.orElseThrow();
    }
}
