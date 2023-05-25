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

package bisq.tor.onionservice;

import bisq.tor.Constants;
import bisq.tor.OnionAddress;
import bisq.tor.TorController;
import lombok.extern.slf4j.Slf4j;
import net.freehaven.tor.control.TorControlConnection;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class OnionServicePublishService {
    private final TorController torController;
    private final Path torDirPath;
    private Optional<OnionServiceDataDirManager> dataDirManager = Optional.empty();
    private Optional<OnionAddress> onionAddress = Optional.empty();

    public OnionServicePublishService(TorController torController, Path torDirPath) {
        this.torController = torController;
        this.torDirPath = torDirPath;
    }

    public synchronized CompletableFuture<OnionAddress> initialize(String nodeId, int onionServicePort, int localPort) {
        if (onionAddress.isPresent()) {
            return CompletableFuture.completedFuture(onionAddress.get());
        }

        CompletableFuture<OnionAddress> completableFuture = new CompletableFuture<>();
        try {
            TorControlConnection.CreateHiddenServiceResult jTorResult =
                    sendPublishRequestToTor(onionServicePort, localPort);

            CreateHiddenServiceResult result = new CreateHiddenServiceResult(jTorResult);

            dataDirManager = Optional.of(
                    new OnionServiceDataDirManager(
                            torDirPath.resolve(Constants.HS_DIR).resolve(nodeId)
                    )
            );
            dataDirManager.ifPresent(dataDirManager -> dataDirManager.persist(result));

            this.onionAddress = Optional.of(
                    new OnionAddress(jTorResult.serviceID + ".onion", onionServicePort)
            );

            log.debug("Start publishing hidden service {}", onionAddress);
            String serviceId = jTorResult.serviceID;
            torController.addHiddenServiceReadyListener(serviceId, () -> {
                torController.removeHiddenServiceReadyListener(serviceId);
                completableFuture.complete(onionAddress.orElseThrow());
            });

        } catch (IOException e) {
            log.error("Couldn't initialize onion service {}", onionAddress);
            completableFuture.completeExceptionally(e);
        }

        return completableFuture;
    }

    public synchronized void shutdown() {
        log.info("Close onionAddress={}", onionAddress);
        onionAddress.ifPresent(onionAddress -> {
            torController.removeHiddenServiceReadyListener(onionAddress.getServiceId());
            try {
                torController.destroyHiddenService(onionAddress.getServiceId());
            } catch (IOException ignore) {
            }
        });
    }

    private TorControlConnection.CreateHiddenServiceResult sendPublishRequestToTor(int onionServicePort, int localPort) throws IOException {
        Optional<String> privateKey = dataDirManager.orElseThrow().getPrivateKey();

        TorControlConnection.CreateHiddenServiceResult result;
        if (privateKey.isPresent()) {
            result = torController.createHiddenService(onionServicePort, localPort, privateKey.get());
        } else {
            result = torController.createHiddenService(onionServicePort, localPort);
        }
        return result;
    }
}
