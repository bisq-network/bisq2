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

package bisq.http_api;

import bisq.common.application.Service;
import bisq.common.network.Address;
import bisq.common.facades.FacadeProvider;
import bisq.common.network.TransportType;
import bisq.network.NetworkService;
import bisq.security.SecurityService;
import bisq.security.keys.KeyBundle;
import bisq.security.keys.KeyBundleService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class ApiTorOnionService implements Service {
    private final NetworkService networkService;
    private final int port;
    private final Path appDataDirPath;
    private final KeyBundleService keyBundleService;
    private final String identityTag;
    @Getter
    private final boolean publishOnionService;
    @Getter
    private Optional<Address> publishedAddress = Optional.empty();

    public ApiTorOnionService(Path appDataDirPath,
                              SecurityService securityService,
                              NetworkService networkService,
                              int port,
                              String identityTag,
                              boolean publishOnionService) {
        this.appDataDirPath = appDataDirPath;
        keyBundleService = securityService.getKeyBundleService();
        this.identityTag = identityTag;
        this.networkService = networkService;
        this.port = port;
        this.publishOnionService = publishOnionService;
    }

    @Override
    public CompletableFuture<Boolean> initialize() {
        if (!publishOnionService || networkService.findServiceNode(TransportType.TOR).isEmpty()) {
            // If tor is not used we don't do anything
            return CompletableFuture.completedFuture(true);
        }

        return CompletableFuture.supplyAsync(() -> {
                    String keyId = keyBundleService.getKeyIdFromTag(identityTag);
                    KeyBundle keyBundle = keyBundleService.getOrCreateKeyBundle(keyId);
                    return keyBundle.getTorKeyPair();
                })
                .thenCompose(torKeyPair -> networkService.publishOnionService(port, port, torKeyPair))
                .thenApply(onionAddress -> {
                    publishedAddress = Optional.of(Address.from(onionAddress, port));
                    String onionAddressWithPort = onionAddress + ":" + port;
                    log.info("{} published for {}", onionAddressWithPort, identityTag);
                    try {
                        Path path = appDataDirPath.resolve(identityTag + "_onionAddress.txt");
                        FacadeProvider.getJdkFacade().writeString(onionAddressWithPort, path);
                        return true;
                    } catch (IOException e) {
                        log.error("Error at write onionAddress", e);
                        return false;
                    }
                });
    }

    @Override
    public CompletableFuture<Boolean> shutdown() {
        return CompletableFuture.completedFuture(true);
    }
}
