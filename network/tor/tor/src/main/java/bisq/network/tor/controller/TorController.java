package bisq.network.tor.controller;

import bisq.common.observable.Observable;
import bisq.network.tor.controller.events.events.TorBootstrapEvent;
import bisq.network.tor.controller.exceptions.TorBootstrapFailedException;
import bisq.security.keys.TorKeyPair;
import lombok.extern.slf4j.Slf4j;
import net.freehaven.tor.control.PasswordDigest;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class TorController {
    private final TorControlProtocol torControlProtocol = new TorControlProtocol();
    private final int bootstrapTimeout; // in ms
    private final int hsUploadTimeout; // in ms
    private final Observable<TorBootstrapEvent> bootstrapEvent;
    private final long isOnlineTimeout = TimeUnit.SECONDS.toMillis(30); // in ms
    private final Map<String, PublishOnionAddressService> publishOnionAddressServiceMap = new ConcurrentHashMap<>();
    private final Map<String, OnionServiceOnlineStateService> onionServiceOnlineStateServiceMap = new ConcurrentHashMap<>();
    private Optional<BootstrapService> bootstrapService = Optional.empty();
    private volatile boolean isShutdownInProgress;

    public TorController(int bootstrapTimeout, int hsUploadTimeout, Observable<TorBootstrapEvent> bootstrapEvent) {
        this.bootstrapTimeout = bootstrapTimeout;
        this.hsUploadTimeout = hsUploadTimeout;
        this.bootstrapEvent = bootstrapEvent;
    }

    public void initialize(int controlPort) {
        torControlProtocol.initialize(controlPort);
    }

    public void authenticate(byte[] authCookie) {
        torControlProtocol.authenticate(authCookie);
    }

    public void authenticate() {
        // No authentication required, but we still need to send an empty
        // AUTHENTICATE call to be able to send control commands
        torControlProtocol.authenticate(new byte[0]);
    }

    public void authenticate(PasswordDigest hashedControlPassword) {
        torControlProtocol.authenticate(hashedControlPassword);
    }

    public void shutdown() {
        isShutdownInProgress = true;
        bootstrapService.ifPresent(BootstrapService::shutdown);
        publishOnionAddressServiceMap.values().forEach(PublishOnionAddressService::shutdown);
        onionServiceOnlineStateServiceMap.values().forEach(OnionServiceOnlineStateService::shutdown);
        torControlProtocol.close();
    }

    public void bootstrap() {
        if (isShutdownInProgress) {
            return;
        }
        bootstrapAsync()
                .exceptionally(throwable -> {
                    if (throwable instanceof TorBootstrapFailedException) {
                        throw (TorBootstrapFailedException) throwable;
                    } else {
                        log.error("Error at bootstrap", throwable);
                        throw new TorBootstrapFailedException(throwable);
                    }
                })
                .join();
    }

    public CompletableFuture<Void> bootstrapAsync() {
        if (bootstrapService.isPresent() && bootstrapService.get().getFuture().isPresent()) {
            return bootstrapService.get().getFuture().get();
        }

        BootstrapService service = new BootstrapService(torControlProtocol, bootstrapTimeout, bootstrapEvent);
        bootstrapService = Optional.of(service);
        CompletableFuture<Void> future = service.bootstrap();
        future.whenComplete((nil, throwable) -> bootstrapService = Optional.empty());
        return future;
    }

    public void publish(TorKeyPair torKeyPair, int onionServicePort, int localPort) throws InterruptedException {
        if (isShutdownInProgress) {
            return;
        }
        publishAsync(torKeyPair, onionServicePort, localPort).join();
    }

    public CompletableFuture<Void> publishAsync(TorKeyPair torKeyPair,
                                                int onionServicePort,
                                                int localPort) {
        String onionAddress = torKeyPair.getOnionAddress();
        if (publishOnionAddressServiceMap.containsKey(onionAddress) && publishOnionAddressServiceMap.get(onionAddress).getFuture().isPresent()) {
            return publishOnionAddressServiceMap.get(onionAddress).getFuture().get();
        }

        PublishOnionAddressService publishOnionAddressService = new PublishOnionAddressService(torControlProtocol, hsUploadTimeout, torKeyPair);
        CompletableFuture<Void> future = publishOnionAddressService.publish(onionServicePort, localPort);
        future.whenComplete((nil, throwable) -> publishOnionAddressServiceMap.remove(onionAddress));
        publishOnionAddressServiceMap.put(onionAddress, publishOnionAddressService);
        return future;
    }

    public CompletableFuture<Boolean> isOnionServiceOnline(String onionAddress) {
        if (isShutdownInProgress) {
            return CompletableFuture.completedFuture(false);
        }
        if (onionServiceOnlineStateServiceMap.containsKey(onionAddress) && onionServiceOnlineStateServiceMap.get(onionAddress).getFuture().isPresent()) {
            return onionServiceOnlineStateServiceMap.get(onionAddress).getFuture().get();
        }

        OnionServiceOnlineStateService onionServiceOnlineStateService = new OnionServiceOnlineStateService(torControlProtocol, onionAddress, isOnlineTimeout);
        CompletableFuture<Boolean> future = onionServiceOnlineStateService.isOnionServiceOnline();
        future.whenComplete((r, t) -> onionServiceOnlineStateServiceMap.remove(onionAddress));
        onionServiceOnlineStateServiceMap.put(onionAddress, onionServiceOnlineStateService);
        return future;
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
}
