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

package bisq.api.access.transport;


import bisq.api.ApiConfig;
import bisq.common.application.Service;
import bisq.common.file.FileMutatorUtils;
import bisq.common.network.Address;
import bisq.common.network.ClearnetAddress;
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.util.NetworkUtils;
import bisq.network.NetworkService;
import bisq.network.p2p.node.Node;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyBundleService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static bisq.common.threading.ExecutorFactory.commonForkJoinPool;

@Slf4j

public class ApiAccessTransportService implements Service {
    private final String IDENTITY_TAG = "ApiAccess";

    private final Path appDataDirPath;
    private final NetworkService networkService;
    private final KeyBundleService keyBundleService;
    private final String configuredBindHost;
    private final Observable<Integer> bindPort = new Observable<>(8090);
    @Getter
    private final int onionServicePort;
    private final boolean useTor;
    @Setter
    private ApiAccessTransportType apiAccessTransportType = ApiAccessTransportType.TOR;
    @Getter
    private volatile Optional<TorContext> torContext = Optional.empty();
    @Getter
    private final Observable<Address> onionAddress = new Observable<>();

    public ApiAccessTransportService(ApiConfig apiConfig,
                                     Path appDataDirPath,
                                     NetworkService networkService,
                                     KeyBundleService keyBundleService,
                                     int bindPort,
                                     int onionServicePort) {
        useTor = apiConfig.useTor();
        configuredBindHost = apiConfig.getBindHost();
        this.appDataDirPath = appDataDirPath;
        this.networkService = networkService;
        this.keyBundleService = keyBundleService;
        this.bindPort.set(bindPort);
        this.onionServicePort = onionServicePort;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        if (useTor) {
            return awaitTransport()
                    .thenCompose(ready -> publishAndGetTorAddress())
                    .thenApply(address -> true);
        } else {
            return CompletableFuture.completedFuture(true);
        }
    }

    /**
     * Completes when the transport through which clients reach the API can serve them. With the TOR transport that is
     * once the Tor transport has initialized: the onion service needs a running Tor, and the server listens where that
     * Tor connects ({@link #getServerBindHost()}). The network service completes with its first initialized transport,
     * so with several transports Tor can come later. Fails when the Tor transport fails.
     */
    public CompletableFuture<Void> awaitTransport() {
        if (!useTor) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Node> initializedTorNode = networkService.getInitializedDefaultNodeByTransport()
                .get(TransportType.TOR);
        if (initializedTorNode == null) {
            return CompletableFuture.failedFuture(new IllegalStateException(
                    "The API uses the TOR transport, but the network has no Tor transport. Check that " +
                            "network.supportedTransportTypes includes TOR in the config."));
        }
        // The Tor transport completes on a network thread; the API continues on the common pool.
        return initializedTorNode.thenApplyAsync(node -> null, commonForkJoinPool());
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return Service.super.shutdown();
    }

    /**
     * @return the host on which the API server listens. With the TOR transport, Tor or the relay of the sandbox in
     * front of Tor connects to the server of the onion service on the address which the Tor library names: the
     * loopback address, or all addresses on Whonix, where the Tor of the Gateway connects to the Workstation. The
     * configured bind host applies to the CLEAR transport. Like {@link #publishAndGetTorAddress}, it needs a running
     * Tor transport.
     */
    public String getServerBindHost() {
        if (!useTor) {
            return configuredBindHost;
        }
        return networkService.findOnionServiceBindAddress()
                .map(InetAddress::getHostAddress)
                .orElse(configuredBindHost);
    }

    public Address findLanAddress() {
        String host = NetworkUtils.findLANHostAddress(Optional.empty()).orElse("127.0.0.1");
        return new ClearnetAddress(host, bindPort.get());
    }

    public CompletableFuture<Address> publishAndGetTorAddress() {
        if (networkService.findServiceNode(TransportType.TOR).isEmpty()) {
            log.error("TOR ServiceNode not found in NetworkService. Check that network.supportedTransportTypes includes TOR in the config.");
            return CompletableFuture.failedFuture(new IllegalStateException("ApiAccessTorTransportService is initialized without TOR set as TransportType"));
        }

        return CompletableFuture.supplyAsync(() -> {
                    String keyId = keyBundleService.getKeyIdFromTag(IDENTITY_TAG);
                    KeyBundle keyBundle = keyBundleService.getOrCreateKeyBundle(keyId);
                    return keyBundle.getTorKeyPair();
                })
                .thenCompose(torKeyPair -> networkService.publishOnionService(bindPort.get(), onionServicePort, torKeyPair))
                .thenApply(onionAddress -> {
                    Address address = Address.from(onionAddress, onionServicePort);
                    log.info("{} published for {}", address, IDENTITY_TAG);
                    writeAddressToDataDir(address);
                    this.onionAddress.set(address);
                    return address;
                });
    }

    private void writeAddressToDataDir(Address address) {
        try {
            Path path = appDataDirPath.resolve(IDENTITY_TAG + "_onionAddress.txt");
            FileMutatorUtils.writeToPath(address.getFullAddress(), path);
        } catch (IOException e) {
            log.error("Error at write onionAddress", e);
        }
    }

    public void setBindPort(int bindPort) {
        this.bindPort.set(bindPort);
    }

    public ReadOnlyObservable<Integer> getBindPort() {
        return bindPort;
    }

    public void setTorContext(TorContext torContext) {
        this.torContext = Optional.of(torContext);
    }
}
