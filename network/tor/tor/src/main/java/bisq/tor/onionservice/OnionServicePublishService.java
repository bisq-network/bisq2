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

import bisq.tor.controller.NativeTorController;
import lombok.extern.slf4j.Slf4j;
import net.freehaven.tor.control.TorControlConnection;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Slf4j
public class OnionServicePublishService {
    private static final String HS_DIR_NANE = "hiddenservice";

    private final NativeTorController nativeTorController;
    private final Path torDirPath;
    private Optional<OnionAddress> onionAddress = Optional.empty();

    public OnionServicePublishService(NativeTorController nativeTorController, Path torDirPath) {
        this.nativeTorController = nativeTorController;
        this.torDirPath = torDirPath;
    }

    public synchronized CompletableFuture<OnionAddress> initialize(String nodeId, int onionServicePort, int localPort) {
        if (onionAddress.isPresent()) {
            return CompletableFuture.completedFuture(onionAddress.get());
        }

        CompletableFuture<OnionAddress> completableFuture = new CompletableFuture<>();
        try {
            OnionServiceDataDirManager onionDataDirManager = new OnionServiceDataDirManager(
                    torDirPath.resolve(HS_DIR_NANE).resolve(nodeId)
            );
            Optional<String> privateKey = onionDataDirManager.getPrivateKey();
            TorControlConnection.CreateHiddenServiceResult jTorResult =
                    nativeTorController.createHiddenService(onionServicePort, localPort, privateKey);

            CreateHiddenServiceResult result = new CreateHiddenServiceResult(jTorResult);

            onionDataDirManager.persist(result);

            var onionAddress = new OnionAddress(jTorResult.serviceID + ".onion", onionServicePort);
            this.onionAddress = Optional.of(onionAddress);
            completableFuture.complete(onionAddress);

        } catch (IOException e) {
            log.error("Couldn't initialize onion service {}", onionAddress);
            completableFuture.completeExceptionally(e);
        }

        return completableFuture;
    }
}
