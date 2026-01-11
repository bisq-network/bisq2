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
import bisq.common.file.FileMutatorUtils;
import bisq.common.network.Address;
import bisq.common.network.ClearnetAddress;
import bisq.common.network.TransportType;
import bisq.common.observable.Observable;
import bisq.common.observable.ReadOnlyObservable;
import bisq.common.util.NetworkUtils;
import bisq.network.NetworkService;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyBundleService;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j

public class ApiAccessTransportService {
    private final String IDENTITY_TAG = "ApiAccess";

    private final Path appDataDirPath;
    private final NetworkService networkService;
    private final KeyBundleService keyBundleService;
    private final Observable<Integer> bindPort = new Observable<>(8090);
    @Getter
    private final int onionServicePort;

    @Setter
    private ApiAccessTransportType apiAccessTransportType = ApiAccessTransportType.TOR;
    private final boolean isTlsRequired;
    private final boolean isTorClientAuthRequired;
    @Getter
    private Optional<TlsContext> tlsContext = Optional.empty();
    @Getter
    private Optional<TorContext> torContext = Optional.empty();

    public ApiAccessTransportService(ApiConfig apiConfig,
                                     Path appDataDirPath,
                                     NetworkService networkService,
                                     KeyBundleService keyBundleService,
                                     int bindPort,
                                     int onionServicePort) {
        isTlsRequired = apiConfig.isTlsRequired();
        isTorClientAuthRequired = apiConfig.isTorClientAuthRequired();
        this.appDataDirPath = appDataDirPath;
        this.networkService = networkService;
        this.keyBundleService = keyBundleService;
        this.bindPort.set(bindPort);
        this.onionServicePort = onionServicePort;
    }

    public Address findLanAddress() {
        String host = NetworkUtils.findLANHostAddress(Optional.empty()).orElse("127.0.0.1");
        return new ClearnetAddress(host, bindPort.get());
    }

    public CompletableFuture<Address> publishAndGetTorAddress() {
        if (networkService.findServiceNode(TransportType.TOR).isEmpty()) {
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

    public void setTlsContext(TlsContext tlsContext) {
        this.tlsContext = Optional.of(tlsContext);
    }

    public void setTorContext(TorContext torContext) {
        this.torContext = Optional.of(torContext);
    }
}
