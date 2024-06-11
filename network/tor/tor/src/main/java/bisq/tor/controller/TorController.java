package bisq.tor.controller;

import bisq.common.observable.Observable;
import bisq.security.keys.TorKeyPair;
import bisq.tor.TorrcClientConfigFactory;
import bisq.tor.controller.events.events.BootstrapEvent;
import bisq.tor.controller.events.events.HsDescEvent;
import bisq.tor.controller.events.events.HsDescFailedEvent;
import bisq.tor.controller.events.listener.BootstrapEventListener;
import bisq.tor.controller.events.listener.HsDescEventListener;
import bisq.tor.controller.exceptions.HsDescUploadFailedException;
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
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TorController implements BootstrapEventListener, HsDescEventListener {
    private final int bootstrapTimeout; // in ms
    private final int hsUploadTimeout; // in ms
    private final CountDownLatch isBootstrappedCountdownLatch = new CountDownLatch(1);
    @Getter
    private final Observable<BootstrapEvent> bootstrapEvent = new Observable<>();

    private final Map<String, CountDownLatch> pendingOnionServicePublishLatchMap = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Boolean>> pendingIsOnionServiceOnlineLookupFutureMap =
            new ConcurrentHashMap<>();

    private Optional<TorControlProtocol> torControlProtocol = Optional.empty();

    public TorController(int bootstrapTimeout, int hsUploadTimeout) {
        this.bootstrapTimeout = bootstrapTimeout;
        this.hsUploadTimeout = hsUploadTimeout;
    }

    public void initialize(int controlPort) throws IOException {
        initialize(controlPort, Optional.empty());
    }

    public void initialize(int controlPort, PasswordDigest hashedControlPassword) throws IOException {
        initialize(controlPort, Optional.of(hashedControlPassword));
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

    public void bootstrapTor() {
        bindToBisq();
        subscribeToBootstrapEvents();
        enableNetworking();
        waitUntilBootstrapped();
    }

    public CompletableFuture<Boolean> isOnionServiceOnline(String onionAddress) {
        var onionServiceLookupCompletableFuture = new CompletableFuture<Boolean>();
        pendingIsOnionServiceOnlineLookupFutureMap.put(onionAddress, onionServiceLookupCompletableFuture);
        subscribeToHsDescEvents();

        TorControlProtocol torControlProtocol = getTorControlProtocol();
        String serviceId = onionAddress.replace(".onion", "");
        torControlProtocol.hsFetch(serviceId);

        onionServiceLookupCompletableFuture.thenRun(() -> {
            torControlProtocol.removeHsDescEventListener(this);
            torControlProtocol.setEvents(Collections.emptyList());
        });

        return onionServiceLookupCompletableFuture;
    }

    public void publish(TorKeyPair torKeyPair, int onionServicePort, int localPort) throws InterruptedException {
        String onionAddress = torKeyPair.getOnionAddress();
        var onionServicePublishedLatch = new CountDownLatch(1);
        pendingOnionServicePublishLatchMap.put(onionAddress, onionServicePublishedLatch);

        subscribeToHsDescEvents();
        TorControlProtocol torControlProtocol = getTorControlProtocol();
        torControlProtocol.addOnion(torKeyPair, onionServicePort, localPort);

        boolean isSuccess = onionServicePublishedLatch.await(hsUploadTimeout, TimeUnit.MILLISECONDS);
        if (!isSuccess) {
            throw new HsDescUploadFailedException("HS_DESC upload timer triggered.");
        }

        torControlProtocol.removeHsDescEventListener(this);
        torControlProtocol.setEvents(Collections.emptyList());
    }

    public int getSocksPort() {
        TorControlProtocol torControlProtocol = getTorControlProtocol();
        String socksListenersString = torControlProtocol.getInfo("net/listeners/socks");

        String socksListener;
        if (socksListenersString.contains(" ")) {
            String[] socksPorts = socksListenersString.split(" ");
            socksListener = socksPorts[0];
        } else {
            socksListener = socksListenersString;
        }

        // "127.0.0.1:12345"
        socksListener = socksListener.replace("\"", "");
        String portString = socksListener.split(":")[1];
        return Integer.parseInt(portString);
    }

    @Override
    public void onBootstrapStatusEvent(BootstrapEvent bootstrapEvent) {
        log.info("Tor bootstrap event: {}", bootstrapEvent);
        this.bootstrapEvent.set(bootstrapEvent);
        if (bootstrapEvent.isDoneEvent()) {
            isBootstrappedCountdownLatch.countDown();
        }
    }

    @Override
    public void onHsDescEvent(HsDescEvent hsDescEvent) {
        log.info("Tor HS_DESC event: {}", hsDescEvent);

        String onionAddress = hsDescEvent.getHsAddress() + ".onion";
        CompletableFuture<Boolean> completableFuture;
        switch (hsDescEvent.getAction()) {
            case FAILED:
                HsDescFailedEvent hsDescFailedEvent = (HsDescFailedEvent) hsDescEvent;
                if (hsDescFailedEvent.getReason().equals("REASON=NOT_FOUND")) {
                    completableFuture = pendingIsOnionServiceOnlineLookupFutureMap.get(onionAddress);
                    if (completableFuture != null) {
                        completableFuture.complete(false);
                        pendingIsOnionServiceOnlineLookupFutureMap.remove(onionAddress);
                    }
                }
                break;
            case RECEIVED:
                completableFuture = pendingIsOnionServiceOnlineLookupFutureMap.get(onionAddress);
                if (completableFuture != null) {
                    completableFuture.complete(true);
                    pendingIsOnionServiceOnlineLookupFutureMap.remove(onionAddress);
                }
                break;
            case UPLOADED:
                CountDownLatch countDownLatch = pendingOnionServicePublishLatchMap.get(onionAddress);
                if (countDownLatch != null) {
                    countDownLatch.countDown();
                    pendingOnionServicePublishLatchMap.remove(onionAddress);
                }
                break;
        }
    }

    private void initialize(int controlPort, Optional<PasswordDigest> hashedControlPassword) throws IOException {
        var torControlProtocol = new TorControlProtocol(controlPort);
        this.torControlProtocol = Optional.of(torControlProtocol);

        torControlProtocol.initialize();
        hashedControlPassword.ifPresent(torControlProtocol::authenticate);
    }

    private void bindToBisq() {
        TorControlProtocol torControlProtocol = getTorControlProtocol();
        torControlProtocol.takeOwnership();
        torControlProtocol.resetConf(NativeTorProcess.ARG_OWNER_PID);
    }

    private void subscribeToBootstrapEvents() {
        TorControlProtocol torControlProtocol = getTorControlProtocol();
        torControlProtocol.addBootstrapEventListener(this);
        torControlProtocol.setEvents(List.of("STATUS_CLIENT"));
    }

    private void subscribeToHsDescEvents() {
        TorControlProtocol torControlProtocol = getTorControlProtocol();
        torControlProtocol.addHsDescEventListener(this);
        torControlProtocol.setEvents(List.of("HS_DESC"));
    }

    private void enableNetworking() {
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
