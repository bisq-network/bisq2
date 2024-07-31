package bisq.tor.controller;

import bisq.common.observable.Observable;
import bisq.security.keys.TorKeyPair;
import bisq.tor.controller.events.events.BootstrapEvent;
import bisq.tor.controller.events.events.HsDescEvent;
import bisq.tor.controller.events.events.HsDescFailedEvent;
import bisq.tor.controller.events.listener.HsDescEventListener;
import bisq.tor.controller.exceptions.HsDescUploadFailedException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.freehaven.tor.control.PasswordDigest;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TorController implements HsDescEventListener {
    private final TorControlProtocol torControlProtocol = new TorControlProtocol();

    private final int bootstrapTimeout; // in ms
    private final int hsUploadTimeout; // in ms

    @Getter
    private final Observable<BootstrapEvent> bootstrapEvent = new Observable<>();

    private final Map<String, CountDownLatch> pendingOnionServicePublishLatchMap = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Boolean>> pendingIsOnionServiceOnlineLookupFutureMap =
            new ConcurrentHashMap<>();

    public TorController(int bootstrapTimeout, int hsUploadTimeout) {
        this.bootstrapTimeout = bootstrapTimeout;
        this.hsUploadTimeout = hsUploadTimeout;
    }

    public void initialize(int controlPort) {
        initialize(controlPort, Optional.empty());
    }

    public void initialize(int controlPort, PasswordDigest hashedControlPassword) {
        initialize(controlPort, Optional.of(hashedControlPassword));
    }

    public void shutdown() {
        torControlProtocol.close();
    }

    public void bootstrapTor() {
        BootstrapTorService bootstrapTorService = new BootstrapTorService(torControlProtocol, bootstrapTimeout, bootstrapEvent);
        bootstrapTorService.bootstrapTor();
    }

    public CompletableFuture<Boolean> isOnionServiceOnline(String onionAddress) {
        var onionServiceLookupCompletableFuture = new CompletableFuture<Boolean>();
        pendingIsOnionServiceOnlineLookupFutureMap.put(onionAddress, onionServiceLookupCompletableFuture);
        subscribeToHsDescEvents();

        String serviceId = onionAddress.replace(".onion", "");
        torControlProtocol.hsFetch(serviceId);

        onionServiceLookupCompletableFuture.thenRun(() -> {
            torControlProtocol.removeHsDescEventListener(this);
            torControlProtocol.setEvents(Collections.emptyList());
        });

        return onionServiceLookupCompletableFuture;
    }

    public void publish(TorKeyPair torKeyPair, int onionServicePort, int localPort) throws InterruptedException {
        if (!pendingOnionServicePublishLatchMap.isEmpty()) {
            pendingOnionServicePublishLatchMap.forEach((onionAddress, latch) -> {
                try {
                    log.info("A previous request for publishing {} has not completed yet.", onionAddress);
                    boolean success = latch.await(hsUploadTimeout, TimeUnit.MILLISECONDS);
                    log.info("A previous request for publishing {} has completed with success={}.", onionAddress, success);
                } catch (InterruptedException ignore) {
                }
            });
        }
        String onionAddress = torKeyPair.getOnionAddress();
        var onionServicePublishedLatch = new CountDownLatch(1);
        pendingOnionServicePublishLatchMap.put(onionAddress, onionServicePublishedLatch);

        subscribeToHsDescEvents();
        torControlProtocol.addOnion(torKeyPair, onionServicePort, localPort);

        boolean isSuccess = onionServicePublishedLatch.await(hsUploadTimeout, TimeUnit.MILLISECONDS);
        if (!isSuccess) {
            throw new HsDescUploadFailedException("HS_DESC upload timer triggered.");
        }

        torControlProtocol.removeHsDescEventListener(this);
        torControlProtocol.setEvents(Collections.emptyList());
    }

    public int getSocksPort() {
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

    private void initialize(int controlPort, Optional<PasswordDigest> hashedControlPassword) {
        torControlProtocol.initialize(controlPort);
        hashedControlPassword.ifPresent(torControlProtocol::authenticate);
    }

    private void subscribeToHsDescEvents() {
        torControlProtocol.addHsDescEventListener(this);
        torControlProtocol.setEvents(List.of("HS_DESC"));
    }
}
