package bisq.tor.controller;

import bisq.common.observable.Observable;
import bisq.security.keys.TorKeyPair;
import bisq.tor.controller.events.events.BootstrapEvent;
import bisq.tor.controller.exceptions.TorBootstrapFailedException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.freehaven.tor.control.PasswordDigest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkArgument;

@Slf4j
public class TorController {
    private final TorControlProtocol torControlProtocol = new TorControlProtocol();
    private final int bootstrapTimeout; // in ms
    private final int hsUploadTimeout; // in ms
    private final long isOnlineTimeout = TimeUnit.SECONDS.toMillis(30); // in ms
    @Getter
    private final Observable<BootstrapEvent> bootstrapEvent = new Observable<>();
    private final Map<String, PublishOnionAddressService> publishOnionAddressServiceMap = new HashMap<>();
    private final Map<String, OnionServiceOnlineStateService> onionServiceOnlineStateServiceMap = new HashMap<>();
    private Optional<BootstrapService> bootstrapService = Optional.empty();

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
        checkArgument(bootstrapService.isEmpty(), "Bootstrap must be called only once");
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
        bootstrapService = Optional.of(new BootstrapService(torControlProtocol, bootstrapTimeout, bootstrapEvent));
        return bootstrapService.get().bootstrap();
    }

    public void publish(TorKeyPair torKeyPair, int onionServicePort, int localPort) throws InterruptedException {
        publishAsync(torKeyPair, onionServicePort, localPort).join();
    }

    public CompletableFuture<Void> publishAsync(TorKeyPair torKeyPair, int onionServicePort, int localPort) throws InterruptedException {
        String onionAddress = torKeyPair.getOnionAddress();
        PublishOnionAddressService publishOnionAddressService = new PublishOnionAddressService(torControlProtocol, hsUploadTimeout, torKeyPair);
        CompletableFuture<Void> future;
        synchronized (publishOnionAddressServiceMap) {
            if (publishOnionAddressServiceMap.containsKey(onionAddress)) {
                return publishOnionAddressServiceMap.get(onionAddress).getFuture().orElseThrow();
            }

            future = publishOnionAddressService.publish(onionServicePort, localPort);
            future.whenComplete((r, t) -> {
                synchronized (publishOnionAddressServiceMap) {
                    publishOnionAddressServiceMap.remove(onionAddress);
                }
            });
            publishOnionAddressServiceMap.put(onionAddress, publishOnionAddressService);
        }
        return future;
    }

    public CompletableFuture<Boolean> isOnionServiceOnline(String onionAddress) {
        OnionServiceOnlineStateService onionServiceOnlineStateService = new OnionServiceOnlineStateService(torControlProtocol, onionAddress, isOnlineTimeout);
        CompletableFuture<Boolean> future;
        synchronized (onionServiceOnlineStateServiceMap) {
            if (onionServiceOnlineStateServiceMap.containsKey(onionAddress)) {
                return onionServiceOnlineStateServiceMap.get(onionAddress).getFuture().orElseThrow();
            }
            future = onionServiceOnlineStateService.isOnionServiceOnline();
            future.whenComplete((r, t) -> {
                synchronized (onionServiceOnlineStateServiceMap) {
                    onionServiceOnlineStateServiceMap.remove(onionAddress);
                }
            });
            onionServiceOnlineStateServiceMap.put(onionAddress, onionServiceOnlineStateService);
        }
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

    private void initialize(int controlPort, Optional<PasswordDigest> hashedControlPassword) {
        torControlProtocol.initialize(controlPort);
        hashedControlPassword.ifPresent(torControlProtocol::authenticate);
    }
}
